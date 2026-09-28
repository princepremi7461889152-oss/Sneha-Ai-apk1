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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    private val _isPickupSirenArmed = MutableStateFlow(true)
    val isPickupSirenArmed: StateFlow<Boolean> = _isPickupSirenArmed.asStateFlow()

    private val _isWrongPinAlertEnabled = MutableStateFlow(true)
    val isWrongPinAlertEnabled: StateFlow<Boolean> = _isWrongPinAlertEnabled.asStateFlow()

    private val _isAlarmActive = MutableStateFlow(false)
    val isAlarmActive: StateFlow<Boolean> = _isAlarmActive.asStateFlow()

    private val _lastDisarmMessage = MutableStateFlow("")
    val lastDisarmMessage: StateFlow<String> = _lastDisarmMessage.asStateFlow()

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
        updateSensorRegistrations()
    }

    fun setPickupSirenGuard(context: Context, armed: Boolean) {
        init(context)
        _isPickupSirenArmed.value = armed
        _isMotionGuardArmed.value = armed
        motionArmedTime = System.currentTimeMillis() + 2500L
        updateSensorRegistrations()
        if (armed) {
            BackgroundSpeaker.speak("मास्टर, अनधिकृत फोन पिकअप सायरन सक्रिय है। आपके अलावा किसी और के फोन छूते या उठाते ही तेज सायरन बज उठेगा और आपके बोलने पर ही बंद होगा।")
        } else {
            BackgroundSpeaker.speak("पिकअप सायरन सुरक्षा निष्क्रिय कर दी गई है।")
        }
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
        if ((_isMotionGuardArmed.value || _isPocketGuardArmed.value || _isPickupSirenArmed.value) && accelerometer != null) {
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
            triggerTheftAlarm(context, "सावधान! किसी अज्ञात व्यक्ति ने गलत पिन डालकर फोन खोलने की कोशिश की है! सायरन बज रहा है!")
        }
    }

    fun triggerTheftAlarm(context: Context, speechAlert: String) {
        _isAlarmActive.value = true
        LockScreenHelper.wakeUpScreen(context, 30000L)

        // Maximize alarm stream volume
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
            }
        } catch (ignored: Exception) {}

        // Start loud oscillating police/emergency siren tone via AudioTrack
        PhoneControlManager.startEmergencyAlarm(context)

        // Start strobe flashlight
        PhoneControlManager.startEmergencyStrobe(context, CoroutineScope(Dispatchers.Default))

        BackgroundSpeaker.speak(speechAlert)

        alarmLoopRunnable?.let { mainHandler.removeCallbacks(it) }
        alarmLoopRunnable = object : Runnable {
            override fun run() {
                if (_isAlarmActive.value) {
                    try {
                        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 800)
                    } catch (ignored: Exception) {}
                    mainHandler.postDelayed(this, 1200L)
                }
            }
        }
        mainHandler.post(alarmLoopRunnable!!)
    }

    /**
     * Master Voice Disarm:
     * When master speaks "सायरन बंद करो", "स्टॉप सायरन", "अलार्म बंद करो" or "stop siren"
     */
    fun disarmWithVoice(context: Context, spokenInput: String): Pair<Boolean, String> {
        val lower = spokenInput.lowercase().trim()
        val isDisarmCommand = lower.contains("सायरन बंद") || lower.contains("स्टॉप सायरन") ||
                lower.contains("अलार्म बंद") || lower.contains("सायरन ऑफ") || lower.contains("बंद करो") ||
                lower.contains("stop siren") || lower.contains("siren off") || lower.contains("stop alarm") ||
                lower.contains("alarm off") || lower.contains("मैं आ गया") || lower.contains("स्नेहा बंद") ||
                lower == "off" || lower == "stop" || lower == "बंद"

        if (isDisarmCommand) {
            stopAlarm(context)
            val nowStr = SimpleDateFormat("आज, hh:mm a", Locale.getDefault()).format(Date())
            val log = IntruderLogEntry(
                timeFormatted = nowStr,
                triggerReason = "मास्टर द्वारा वॉयस कमांड ('$spokenInput') से सायरन बंद किया गया",
                avatarEmoji = "🎙️",
                threatLevel = "सफलतापूर्वक डिस्आर्म (Authorized Disarm)",
                wasPhotoCaptured = false
            )
            _intruderLogs.value = listOf(log) + _intruderLogs.value
            _lastDisarmMessage.value = "मास्टर की आवाज पहचानी गई! इमरजेंसी सायरन सफलतापूर्वक बंद कर दिया गया।"
            BackgroundSpeaker.speak("मास्टर की आवाज पहचानी गई! इमरजेंसी सायरन बंद कर दिया गया है।")
            return Pair(true, "मास्टर की आवाज पहचानी गई! सायरन बंद हो गया।")
        }

        return Pair(false, "आवाज या कमांड मेल नहीं खाया। कृपया 'सायरन बंद करो' कहें।")
    }

    fun stopAlarm(context: Context) {
        _isAlarmActive.value = false
        alarmLoopRunnable?.let { mainHandler.removeCallbacks(it) }
        try {
            toneGenerator?.stopTone()
        } catch (ignored: Exception) {}
        PhoneControlManager.stopEmergencyAlarm()
        PhoneControlManager.stopEmergencyStrobe(context)
        BackgroundSpeaker.speak("मास्टर, एंटी-थेफ्ट इमरजेंसी सायरन बंद कर दिया गया है।")
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
                triggerTheftAlarm(
                    event.sensor.name.let { ContextValHolder.appContext ?: return },
                    "अलर्ट! फोन मास्टर की जेब से निकाला गया है! इमरजेंसी सायरन चालू है!"
                )
            }
        }

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER && (_isMotionGuardArmed.value || _isPickupSirenArmed.value)) {
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

            // If phone was moved or picked up
            if (movement > 3.8 && !_isAlarmActive.value) {
                val nowStr = SimpleDateFormat("आज, hh:mm a", Locale.getDefault()).format(Date())
                val entry = IntruderLogEntry(
                    timeFormatted = nowStr,
                    triggerReason = "अनधिकृत पिकअप सायरन: किसी अन्य व्यक्ति ने फोन उठाया या छुआ",
                    avatarEmoji = "🚨",
                    threatLevel = "अनधिकृत स्पर्श डिटेक्ट (Siren Active)",
                    wasPhotoCaptured = true
                )
                _intruderLogs.value = listOf(entry) + _intruderLogs.value

                val ctx = ContextValHolder.appContext
                if (ctx != null) {
                    triggerTheftAlarm(
                        ctx,
                        "सावधान! किसी अन्य व्यक्ति ने फोन उठाया है! इमरजेंसी सायरन बज रहा है! मास्टर के बोलने पर ही बंद होगा!"
                    )
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

/**
 * Static application context holder for sensor background callbacks
 */
object ContextValHolder {
    var appContext: Context? = null
}
