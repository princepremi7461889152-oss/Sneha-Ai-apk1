package com.example.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CallVoiceAvatar(
    val id: String,
    val title: String,
    val description: String,
    val pitch: Float,
    val speed: Float,
    val emoji: String,
    val prefixIntro: String
) {
    SNEHA_NATURAL(
        "sneha",
        "स्नेहा (मधुर महिला AI)",
        "सौम्य, प्राकृतिक एवं विनम्र भारतीय स्वर",
        1.15f,
        1.0f,
        "🌸",
        "नमस्ते! मैं मास्टर की AI असिस्टेंट स्नेहा बोल रही हूँ। "
    ),
    VIKRAM_MALE(
        "vikram",
        "विक्रम (गंभीर पुरुष आवाज)",
        "शांत, परिपक्व और पेशेवर पुरुष स्वर",
        0.75f,
        0.95f,
        "👔",
        "हेलो! मैं मास्टर का अधिकृत प्रतिनिधि बोल रहा हूँ। "
    ),
    SECURITY_GUARD(
        "security",
        "सिक्योरिटी / पुलिस टोन",
        "कड़क चेतावनी देने वाला रक्षक स्वर (स्पैमर्स हेतु सर्वोत्तम)",
        0.65f,
        1.05f,
        "🛡️",
        "सावधान! यह कॉल मास्टर के स्वचालित सुरक्षा सिस्टम द्वारा मॉनिटर की जा रही है। "
    ),
    PROFESSOR(
        "professor",
        "प्रोफेसर / फॉर्मल टोन",
        "अकादमिक व व्यस्त शिक्षक शैली",
        0.9f,
        0.9f,
        "🎓",
        "नमस्कार! मास्टर अभी अकादमिक लेक्चर और क्लास में व्यस्त हैं। "
    ),
    ROBOTIC(
        "robot",
        "साइबर रोबोट अवतार",
        "फ्यूचरिस्टिक हाई-टेक वॉयस टोन",
        0.55f,
        1.15f,
        "🤖",
        "सिस्टम अलर्ट: यूजर वर्तमान में ऑफलाइन है। "
    )
}

data class CallLogEntry(
    val id: Long = System.currentTimeMillis(),
    val caller: String,
    val time: String,
    val messageSpoken: String,
    val status: String // "अटेंड किया गया (Answered & Spoken)", "कटा गया (Ended)"
)

object CallAssistantManager {

    private const val TAG = "CallAssistantManager"
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isCallRinging = MutableStateFlow(false)
    val isCallRinging: StateFlow<Boolean> = _isCallRinging.asStateFlow()

    private val _currentCaller = MutableStateFlow("अज्ञात कॉलर (+91 98765-XXXXX)")
    val currentCaller: StateFlow<String> = _currentCaller.asStateFlow()

    private val _isCallActive = MutableStateFlow(false)
    val isCallActive: StateFlow<Boolean> = _isCallActive.asStateFlow()

    private val _autoAttendantEnabled = MutableStateFlow(true)
    val autoAttendantEnabled: StateFlow<Boolean> = _autoAttendantEnabled.asStateFlow()

    private val _activeVoiceAvatar = MutableStateFlow(CallVoiceAvatar.SNEHA_NATURAL)
    val activeVoiceAvatar: StateFlow<CallVoiceAvatar> = _activeVoiceAvatar.asStateFlow()

    private val _defaultAttendantMessage = MutableStateFlow("मास्टर अभी क्लास में हैं, कृपया बाद में कॉल करें।")
    val defaultAttendantMessage: StateFlow<String> = _defaultAttendantMessage.asStateFlow()

    private val _callLogs = MutableStateFlow<List<CallLogEntry>>(
        listOf(
            CallLogEntry(
                caller = "अमित शर्मा (+91 98112-34567)",
                time = "आज, 10:15 AM",
                messageSpoken = "नमस्ते, मास्टर अभी क्लास में हैं, वे आपसे बाद में संपर्क करेंगे।",
                status = "स्नेहा ने कॉल अटेंड किया"
            )
        )
    )
    val callLogs: StateFlow<List<CallLogEntry>> = _callLogs.asStateFlow()

    fun setActiveVoiceAvatar(avatar: CallVoiceAvatar) {
        _activeVoiceAvatar.value = avatar
    }

    fun setDefaultMessage(message: String) {
        _defaultAttendantMessage.value = message
    }

    fun setAutoAttendantEnabled(enabled: Boolean) {
        _autoAttendantEnabled.value = enabled
    }

    /**
     * Pick up call and speak the given message aloud to the caller through the phone.
     */
    fun pickCallAndSpeak(
        context: Context,
        customMessage: String? = null,
        onAnnouncement: (String) -> Unit
    ): Pair<Boolean, String> {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

        var telecomAnswerSuccess = false

        // 1. Android TelecomManager call accept (if physical device has a ringing call)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ANSWER_PHONE_CALLS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPerm && telecomManager != null) {
                try {
                    telecomManager.acceptRingingCall()
                    telecomAnswerSuccess = true
                } catch (e: Exception) {
                    Log.w(TAG, "TelecomManager acceptRingingCall exception", e)
                }
            }
        }

        // 2. Route audio to speakerphone so caller can hear Sneha's voice clearly
        try {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isSpeakerphoneOn = true
        } catch (ignored: Exception) {}

        // 3. Format the polite attendant message using chosen Voice Avatar
        val transformedMessage = transformUserPromptToMasterThirdPerson(customMessage ?: _defaultAttendantMessage.value)
        val avatar = _activeVoiceAvatar.value
        val fullCallerSpeech = "${avatar.prefixIntro}$transformedMessage"

        _isCallRinging.value = false
        _isCallActive.value = true

        // 4. Log the attendant action
        val newLog = CallLogEntry(
            caller = _currentCaller.value,
            time = "अभी, ${java.time.LocalTime.now().hour}:${String.format("%02d", java.time.LocalTime.now().minute)}",
            messageSpoken = fullCallerSpeech,
            status = "${avatar.title} द्वारा उत्तर दिया गया"
        )
        _callLogs.value = listOf(newLog) + _callLogs.value

        // 5. Speak via TTS
        onAnnouncement(fullCallerSpeech)

        // 6. Automatically reset active call state after announcement
        scope.launch {
            delay(8000)
            _isCallActive.value = false
        }

        val resultSummary = "मास्टर, कॉल पिक कर ली गई है और कॉलर को बता दिया गया है: '$fullCallerSpeech'"
        return Pair(true, resultSummary)
    }

    /**
     * Reject or End Call
     */
    fun endOrRejectCall(context: Context, onAnnouncement: ((String) -> Unit)? = null): String {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ANSWER_PHONE_CALLS
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
                try {
                    telecomManager?.endCall()
                } catch (ignored: Exception) {}
            }
        }

        _isCallRinging.value = false
        _isCallActive.value = false

        val msg = "मास्टर, कॉल काट दी गई है।"
        onAnnouncement?.invoke(msg)

        val activeCaller = _currentCaller.value
        val spokenMsg = _defaultAttendantMessage.value
        scope.launch {
            try {
                CallSummaryManager.createSummaryFromTranscript(
                    context = context,
                    callerName = activeCaller.substringBefore("(").trim(),
                    callerNumber = activeCaller,
                    category = "कॉलेज / पढ़ाई",
                    transcript = "कॉल अटेंडेंट ने कॉलर को सूचित किया: $spokenMsg"
                )
            } catch (ignored: Exception) {}
        }

        return msg
    }

    /**
     * Converts first-person user commands ("मैं क्लास में हूँ", "मैं मीटिंग में हूँ")
     * into respectful third-person for the caller: "मास्टर अभी क्लास में हैं।"
     */
    fun transformUserPromptToMasterThirdPerson(input: String): String {
        var clean = input.trim()
            .removePrefix("कि")
            .removePrefix("की")
            .removePrefix("bol do")
            .removePrefix("bolo")
            .removePrefix("बोलो")
            .removePrefix("बोल दो")
            .trim()

        if (clean.isBlank()) {
            return "मास्टर अभी व्यस्त हैं, वे आपसे बाद में संपर्क करेंगे।"
        }

        // Replace first person "मैं" / "mai" / "i am" with "मास्टर"
        clean = clean.replace(Regex("(?i)\\bmai\\s+hu\\b"), "मास्टर अभी व्यस्त हैं")
        clean = clean.replace(Regex("(?i)\\bmai\\b"), "मास्टर")
        clean = clean.replace(Regex("(?i)\\bmain\\b"), "मास्टर")
        clean = clean.replace("मैं", "मास्टर")
        clean = clean.replace("हूँ", "हैं")
        clean = clean.replace("हु", "हैं")

        // If it does not contain "मास्टर", ensure it's framed respectfully
        if (!clean.contains("मास्टर")) {
            clean = "मास्टर $clean"
        }

        if (!clean.endsWith("।") && !clean.endsWith(".")) {
            clean += ", वे आपसे बाद में संपर्क करेंगे।"
        }

        return clean
    }

    /**
     * Simulate an incoming call for testing in the emulator or without a SIM card.
     */
    fun triggerSimulatedIncomingCall(callerName: String = "प्रोफेसर वर्मा (+91 94321-78900)") {
        _currentCaller.value = callerName
        _isCallRinging.value = true
        _isCallActive.value = false
    }

    fun dismissIncomingCallPrompt() {
        _isCallRinging.value = false
    }
}
