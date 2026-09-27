package com.example.engine

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.example.data.local.VoicePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class ClassLecture(
    val id: String,
    val dayOfWeek: DayOfWeek,
    val subject: String,
    val professor: String,
    val room: String,
    val startTime: String, // "09:00"
    val endTime: String    // "10:00"
)

object ClassTimetableManager {

    private const val TAG = "ClassTimetableManager"

    private val _lectures = MutableStateFlow<List<ClassLecture>>(
        listOf(
            ClassLecture(
                id = "1",
                dayOfWeek = DayOfWeek.MONDAY,
                subject = "डेटा स्ट्रक्चर्स व एल्गोरिदम",
                professor = "डॉ. शर्मा",
                room = "कमरा 204",
                startTime = "09:00",
                endTime = "10:30"
            ),
            ClassLecture(
                id = "2",
                dayOfWeek = DayOfWeek.MONDAY,
                subject = "कंप्यूटर नेटवर्क",
                professor = "प्रो. गुप्ता",
                room = "लैब 3",
                startTime = "11:00",
                endTime = "12:30"
            ),
            ClassLecture(
                id = "3",
                dayOfWeek = DayOfWeek.TUESDAY,
                subject = "आर्टिफिशियल इंटेलिजेंस",
                professor = "डॉ. वर्मा",
                room = "ऑडिटोरियम",
                startTime = "10:00",
                endTime = "11:30"
            ),
            ClassLecture(
                id = "4",
                dayOfWeek = DayOfWeek.WEDNESDAY,
                subject = "ऑपरेटिंग सिस्टम",
                professor = "प्रो. सिंह",
                room = "कमरा 105",
                startTime = "09:30",
                endTime = "11:00"
            ),
            ClassLecture(
                id = "5",
                dayOfWeek = DayOfWeek.THURSDAY,
                subject = "डेटाबेस मैनेजमेंट सिस्टम (DBMS)",
                professor = "डॉ. पटेल",
                room = "कमरा 302",
                startTime = "11:30",
                endTime = "01:00"
            ),
            ClassLecture(
                id = "6",
                dayOfWeek = DayOfWeek.FRIDAY,
                subject = "वेब व मोबाइल ऐप डेवलपमेंट",
                professor = "प्रो. मेहता",
                room = "कंप्यूटर लैब 1",
                startTime = "10:00",
                endTime = "12:00"
            )
        )
    )
    val lectures: StateFlow<List<ClassLecture>> = _lectures.asStateFlow()

    private val _isClassDndActive = MutableStateFlow(false)
    val isClassDndActive: StateFlow<Boolean> = _isClassDndActive.asStateFlow()

    /**
     * Checks if a class is active right now
     */
    fun isCurrentClassActive(context: Context): Boolean {
        if (!VoicePreferences.isClassTimetableModeEnabled(context)) return false
        val active = getActiveClass(context)
        val isActiveNow = active != null
        _isClassDndActive.value = isActiveNow
        return isActiveNow
    }

    /**
     * Returns the currently active lecture, if any
     */
    fun getActiveClass(context: Context): ClassLecture? {
        val today = LocalDate.now().dayOfWeek
        val now = LocalTime.now()

        val todayLectures = _lectures.value.filter { it.dayOfWeek == today }
        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        for (lecture in todayLectures) {
            try {
                val start = LocalTime.parse(lecture.startTime, formatter)
                val end = LocalTime.parse(lecture.endTime, formatter)
                if (!now.isBefore(start) && now.isBefore(end)) {
                    return lecture
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing lecture time", e)
            }
        }
        return null
    }

    /**
     * Next upcoming lecture today
     */
    fun getNextLecture(): ClassLecture? {
        val today = LocalDate.now().dayOfWeek
        val now = LocalTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        return _lectures.value
            .filter { it.dayOfWeek == today }
            .sortedBy { it.startTime }
            .firstOrNull {
                try {
                    val start = LocalTime.parse(it.startTime, formatter)
                    now.isBefore(start)
                } catch (e: Exception) {
                    false
                }
            }
    }

    /**
     * Enforces silent/vibrate mode if active class is happening
     */
    fun applyClassSilenceIfActive(context: Context) {
        if (!VoicePreferences.isClassTimetableModeEnabled(context)) return

        val active = getActiveClass(context)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        if (active != null) {
            _isClassDndActive.value = true
            try {
                // If DND policy access is granted, set priority/none
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notifManager?.isNotificationPolicyAccessGranted == true) {
                    notifManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                } else {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                }
            } catch (e: Exception) {
                Log.w(TAG, "Silent mode apply error", e)
            }
        } else {
            _isClassDndActive.value = false
        }
    }

    fun addLecture(lecture: ClassLecture) {
        _lectures.value = _lectures.value + lecture
    }

    fun removeLecture(id: String) {
        _lectures.value = _lectures.value.filter { it.id != id }
    }
}
