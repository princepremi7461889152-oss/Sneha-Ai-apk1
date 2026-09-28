package com.example.engine

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.example.data.local.ClassLectureEntity
import com.example.data.local.SnehaDatabase
import com.example.data.local.VoicePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    val endTime: String    // "10:30"
)

object ClassTimetableManager {

    private const val TAG = "ClassTimetableManager"
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _lectures = MutableStateFlow<List<ClassLecture>>(emptyList())
    val lectures: StateFlow<List<ClassLecture>> = _lectures.asStateFlow()

    private val _isClassDndActive = MutableStateFlow(false)
    val isClassDndActive: StateFlow<Boolean> = _isClassDndActive.asStateFlow()

    fun init(context: Context) {
        scope.launch {
            try {
                val db = SnehaDatabase.getInstance(context)
                db.classLectureDao().getAllLectures().collect { entityList ->
                    val domainList = entityList.map { entity ->
                        ClassLecture(
                            id = entity.id,
                            dayOfWeek = try {
                                DayOfWeek.valueOf(entity.dayOfWeek.uppercase())
                            } catch (e: Exception) {
                                DayOfWeek.MONDAY
                            },
                            subject = entity.subject,
                            professor = entity.professor,
                            room = entity.room,
                            startTime = entity.startTime,
                            endTime = entity.endTime
                        )
                    }
                    _lectures.value = domainList
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load lectures from Room", e)
            }
        }
    }

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

        val todayLectures = _lectures.value
            .filter { it.dayOfWeek == today }
            .sortedBy { it.startTime }

        val upcomingToday = todayLectures.firstOrNull {
            try {
                val start = LocalTime.parse(it.startTime, formatter)
                now.isBefore(start)
            } catch (e: Exception) {
                false
            }
        }

        // If no more classes today, find first class for tomorrow or upcoming day
        if (upcomingToday != null) return upcomingToday

        // Check if there are any classes today at all
        if (todayLectures.isNotEmpty()) {
            return null // Today's classes have finished
        }

        // Fallback to Monday's first class if weekend or no classes today
        return _lectures.value
            .filter { it.dayOfWeek == DayOfWeek.MONDAY }
            .minByOrNull { it.startTime }
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

    fun addLecture(context: Context? = null, lecture: ClassLecture) {
        _lectures.value = _lectures.value + lecture
        if (context != null) {
            scope.launch {
                try {
                    val db = SnehaDatabase.getInstance(context)
                    db.classLectureDao().insertLecture(
                        ClassLectureEntity(
                            id = lecture.id,
                            dayOfWeek = lecture.dayOfWeek.name,
                            subject = lecture.subject,
                            professor = lecture.professor,
                            room = lecture.room,
                            startTime = lecture.startTime,
                            endTime = lecture.endTime
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to persist lecture to Room", e)
                }
            }
        }
    }

    fun removeLecture(context: Context? = null, id: String) {
        _lectures.value = _lectures.value.filter { it.id != id }
        if (context != null) {
            scope.launch {
                try {
                    val db = SnehaDatabase.getInstance(context)
                    db.classLectureDao().deleteLecture(id)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete lecture from Room", e)
                }
            }
        }
    }
}
