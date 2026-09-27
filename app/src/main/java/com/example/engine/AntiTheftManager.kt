package com.example.engine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

data class IntruderLogEntry(
    val id: Long = System.currentTimeMillis(),
    val timeFormatted: String,
    val triggerReason: String, // "2 बार गलत पिन दर्ज किया गया", "जेब से अनधिकृत निकाला गया", "टेबल से फोन उठाया गया"
    val avatarEmoji: String = "🚨",
    val threatLevel: String = "गंभीर (High Threat)",
    val wasPhotoCaptured: Boolean = true
)

object AntiTheftManager : SensorEventListener {

    private const val TAG = "AntiTheftManager"

    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private var toneGenerator: ToneGenerator? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isPocketGuardArmed = MutableStateFlow(false)
    val isPocketGuardArmed: StateFlow<Boolean> = _isPocketGuardArmed.asStateFlow()

    private val _isMotionGuardArmed = MutableStateFlow(false)
    val isMotionGuardArmed: StateFlow<Boolean> = _isMotionGuardArmed.asStateFlow()

    private val _isWrongPinAlertEnabled = MutableStateFlow(true)
    val isWrongPinAlertEnabled: StateFlow<Boolean> = _isWrongPinAlertEnabled.asStateFlow()

    private val _isAlarmActive = MutableStateFlow(false)
    val isAlarmActive: StateFlow<Boolean> = _isAlarmActive.asStateFlow()

    private val _intruderLogs = MutableStateFlow<List<IntruderLogEntry>>(
        listOf(
            IntruderLogEntry(
                timeFormatted = "आज, 11:20 AM",
                triggerReason = "गलत पिन (2 बार असफल प्रयास)",
                avatarEmoji = "📸",
                threatLevel = "चेतावनी (Suspicious)",
                wasPhotoCaptured = true
            ),
            IntruderLogEntry(
                timeFormatted = "कल, 04:15 PM",
                triggerReason = "पॉकेट पिकपॉकेट मोशन डिटेक्ट",
                avatarEmoji = "🏃‍♂️",
                threatLevel = "गंभीर (Pocket Theft)",
                wasPhotoCaptured = true
            )
        )
    )
    val intruderLogs: StateFlow<List<IntruderLogEntry>> = _intruderLogs.asStateFlow()

    private var wasInPocket = false
    private var lastAccelX = 0f
    private var lastAccelY = 0f
    private var lastAccelZ = 0f
    private var motionArmedTime = 0L

    private var alarmLoopRunnable: Runnable? = null

    fun init(context: Context) {
        if (sensorManager == null) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        } catch (ignored: Exception) {}
    }

    fun setPocketGuard(context: Context, armed: Boolean) {
        init(context)
        _isPocketGuardArmed.value = armed
        updateSensorRegistrations()
        if (armed) {
            BackgroundSpeaker.speak("मास्टर, पॉकेट एंटी-थेफ्ट गार्ड सक्रिय है। फोन जेब में सुरक्षित है।")
        }
    }

    fun setMotionGuard(context: Context, armed: Boolean) {
        init(context)
        _isMotionGuardArmed.value = armed
        motionArmedTime = System.currentTimeMillis() + 3000L // 3s grace period to set phone down
        updateSensorRegistrations()
        if (armed) {
            BackgroundSpeaker.speak("मास्टर, मोशन गार्ड चालू है। फोन को हिलाने पर तेज अलार्म बजेगा।")
        }
    }

    fun setWrongPinAlert(enabled: Boolean) {
        _isWrongPinAlertEnabled.value = enabled
    }

    private fun updateSensorRegistrations() {
        sensorManager?.unregisterListener(this)

        if (_isPocketGuardArmed.value && proximitySensor != null) {
            sensorManager?.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        if ((_isMotionGuardArmed.value || _isPocketGuardArmed.value) && accelerometer != null) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    /**
     * Called when wrong PIN or pattern is entered in lock/unlock screen
     */
    fun onWrongPinAttempt(context: Context, failedAttemptCount: Int) {
        if (!_isWrongPinAlertEnabled.value) return

        val nowStr = SimpleDateFormat("आज, hh:mm a", Locale.getDefault()).format(Date())
        val entry = IntruderLogEntry(
            timeFormatted = nowStr,
            triggerReason = "गलत पिन ($failedAttemptCount बार असफल प्रयास)",
            avatarEmoji = "📸",
            threatLevel = "अनाधिकृत अनलॉकिंग प्रयास",
            wasPhotoCaptured = true
        )
        _intruderLogs.value = listOf(entry) + _intruderLogs.value

        if (failedAttemptCount >= 2) {
            triggerTheftAlarm(context, "सावधान! किसी अज्ञात व्यक्ति ने गलत पिन डालकर फोन खोलने की कोशिश की है!")
        }
    }

    fun triggerTheftAlarm(context: Context, speechAlert: String) {
        _isAlarmActive.value = true
        LockScreenHelper.wakeUpScreen(context, 10000L)
        BackgroundSpeaker.speak(speechAlert)

        alarmLoopRunnable?.let { mainHandler.removeCallbacks(it) }
        alarmLoopRunnable = object : Runnable {
            override fun run() {
                if (_isAlarmActive.value) {
                    try {
                        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 700)
                    } catch (ignored: Exception) {}
                    mainHandler.postDelayed(this, 1200L)
                }
            }
        }
        mainHandler.post(alarmLoopRunnable!!)
    }

    fun stopAlarm(context: Context) {
        _isAlarmActive.value = false
        alarmLoopRunnable?.let { mainHandler.removeCallbacks(it) }
        try {
            toneGenerator?.stopTone()
        } catch (ignored: Exception) {}
        BackgroundSpeaker.speak("मास्टर, एंटी-थेफ्ट अलार्म बंद कर दिया गया है।")
    }

    fun clearLogs() {
        _intruderLogs.value = emptyList()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_PROXIMITY && _isPocketGuardArmed.value) {
            val distance = event.values[0]
            val maxRange = event.sensor.maximumRange
            val isNear = distance < maxRange && distance < 4f

            if (isNear) {
                wasInPocket = true
            } else if (wasInPocket && !_isAlarmActive.value) {
                // Pulled out of pocket!
                wasInPocket = false
                val nowStr = SimpleDateFormat("आज, hh:mm a", Locale.getDefault()).format(Date())
                val entry = IntruderLogEntry(
                    timeFormatted = nowStr,
                    triggerReason = "पॉकेट पिकपॉकेट अलार्म: फोन जेब से निकाला गया",
                    avatarEmoji = "🚨",
                    threatLevel = "चोरी का खतरा (High)",
                    wasPhotoCaptured = true
                )
                _intruderLogs.value = listOf(entry) + _intruderLogs.value
                // Trigger Alarm
                _isAlarmActive.value = true
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 2000)
                } catch (ignored: Exception) {}
                BackgroundSpeaker.speak("अलर्ट! फोन मास्टर की जेब से निकाला गया है! कृपया पिन दर्ज करें!")
            }
        }

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER && _isMotionGuardArmed.value) {
            if (System.currentTimeMillis() < motionArmedTime) {
                // Still in grace period
                lastAccelX = event.values[0]
                lastAccelY = event.values[1]
                lastAccelZ = event.values[2]
                return
            }

            val dx = abs(event.values[0] - lastAccelX)
            val dy = abs(event.values[1] - lastAccelY)
            val dz = abs(event.values[2] - lastAccelZ)
            val movement = sqrt((dx * dx + dy * dy + dz * dz).toDouble())

            lastAccelX = event.values[0]
            lastAccelY = event.values[1]
            lastAccelZ = event.values[2]

            // If phone was moved significantly
            if (movement > 4.5 && !_isAlarmActive.value) {
                val nowStr = SimpleDateFormat("आज, hh:mm a", Locale.getDefault()).format(Date())
                val entry = IntruderLogEntry(
                    timeFormatted = nowStr,
                    triggerReason = "डोंट टच माय फोन: टेबल से फोन उठाया गया",
                    avatarEmoji = "📱",
                    threatLevel = "अनधिकृत स्पर्श डिटेक्ट",
                    wasPhotoCaptured = true
                )
                _intruderLogs.value = listOf(entry) + _intruderLogs.value
                _isAlarmActive.value = true
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 2000)
                } catch (ignored: Exception) {}
                BackgroundSpeaker.speak("मास्टर! किसी ने आपका फोन छुआ है! सायरन बज रहा है!")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
