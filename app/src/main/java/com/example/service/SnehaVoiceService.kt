package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.local.SnehaDatabase
import com.example.data.local.VoicePreferences
import com.example.engine.BackgroundSpeaker
import com.example.engine.LockScreenHelper
import com.example.engine.OtpSafetyGuard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SnehaVoiceService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningForWakeWord = false
    private var toneGenerator: ToneGenerator? = null
    private var wakeLock: android.os.PowerManager.WakeLock? = null
    private var activeWakeSessionUntilMs: Long = 0L

    companion object {
        const val CHANNEL_ID = "sneha_voice_channel_v2"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_SERVICE = "com.example.sneha.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.sneha.STOP_SERVICE"
        const val ACTION_TRIGGER_VOICE = "com.example.sneha.TRIGGER_VOICE"
        const val ACTION_TRIGGER_SOS = "com.example.sneha.TRIGGER_SOS"
        const val ACTION_READ_MESSAGES = "com.example.sneha.READ_MESSAGES"
        const val ACTION_START_SCREEN_SHARE = "com.example.sneha.START_SCREEN_SHARE"
        const val ACTION_PAUSE_FOR_FOREGROUND = "com.example.sneha.PAUSE_FOR_FOREGROUND"
        const val ACTION_RESUME_FROM_FOREGROUND = "com.example.sneha.RESUME_FROM_FOREGROUND"

        const val EXTRA_START_MIC = "extra_start_mic"
        const val EXTRA_TRIGGER_SOS = "extra_trigger_sos"
        const val EXTRA_SCREEN_SHARE = "extra_screen_share"

        @Volatile
        var instance: SnehaVoiceService? = null
            internal set

        val isServiceRunning: Boolean
            get() = instance != null

        fun pauseListeningForForeground(context: Context) {
            // Never start a foreground service just to pause!
            // If the service is running, pause listening directly in memory.
            if (!isServiceRunning) return
            instance?.stopWakeWordListening()
        }

        fun resumeListeningFromForeground(context: Context) {
            val hasMicPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasMicPermission) return
            if (!VoicePreferences.isBackgroundVoiceActive(context)) return

            if (instance != null) {
                instance?.startWakeWordListening()
            } else {
                startService(context)
            }
        }

        fun startService(context: Context) {
            val hasMicPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasMicPermission) {
                Log.w("SnehaVoiceService", "Skipping startService until RECORD_AUDIO is granted by user")
                return
            }

            val intent = Intent(context, SnehaVoiceService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("SnehaVoiceService", "startForegroundService exception", e)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, SnehaVoiceService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            try {
                context.startService(intent)
            } catch (ignored: Exception) {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()

        // Immediate startForeground call in onCreate satisfies Android OS requirement without delay
        try {
            val notification = buildForegroundNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e("SnehaVoiceService", "Error calling startForeground in onCreate", e)
        }

        BackgroundSpeaker.initialize(this)
        BackgroundSpeaker.onSpeechDone = {
            if (isListeningForWakeWord) {
                mainHandler.postDelayed({ restartListeningInternal() }, 800L)
            }
        }
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            wakeLock = pm?.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "Sneha:VoiceServiceWakeLock")
        } catch (ignored: Exception) {}
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
        } catch (ignored: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Unconditionally attach foreground notification immediately to satisfy Android OS requirements
        try {
            val notification = buildForegroundNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e("SnehaVoiceService", "startForeground error", e)
        }

        val action = intent?.action
        if (action == ACTION_STOP_SERVICE) {
            stopWakeWordListening()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val hasMicPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            Log.w("SnehaVoiceService", "RECORD_AUDIO not granted, stopping service gracefully")
            stopWakeWordListening()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        when (action) {
            ACTION_PAUSE_FOR_FOREGROUND -> {
                stopWakeWordListening()
                return START_STICKY
            }
            ACTION_READ_MESSAGES -> {
                readRecentMessagesAloud()
                startWakeWordListening()
                return START_STICKY
            }
            ACTION_RESUME_FROM_FOREGROUND,
            ACTION_START_SERVICE,
            null -> {
                startWakeWordListening()
                return START_STICKY
            }
            else -> {
                startWakeWordListening()
                return START_STICKY
            }
        }
    }

    private fun startWakeWordListening() {
        val hasMic = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasMic || !SpeechRecognizer.isRecognitionAvailable(this)) return
        isListeningForWakeWord = true

        // Hold partial wake lock so CPU continues listening in sleep/lockscreen
        if (wakeLock?.isHeld != true) {
            try {
                wakeLock?.acquire(12 * 60 * 60 * 1000L)
            } catch (ignored: Exception) {}
        }

        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (ignored: Exception) {}

            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {}

                        override fun onError(error: Int) {
                            if (!isListeningForWakeWord) return
                            when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH,
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                                    // Immediate polite loop restart
                                    mainHandler.postDelayed({ restartListeningInternal() }, 100L)
                                }
                                SpeechRecognizer.ERROR_CLIENT,
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                                SpeechRecognizer.ERROR_SERVER,
                                11, // SpeechRecognizer.ERROR_SERVER_DISCONNECTED
                                SpeechRecognizer.ERROR_AUDIO -> {
                                    try {
                                        speechRecognizer?.cancel()
                                        speechRecognizer?.destroy()
                                    } catch (ignored: Exception) {}
                                    speechRecognizer = null
                                    mainHandler.postDelayed({
                                        if (isListeningForWakeWord) startWakeWordListening()
                                    }, 250L)
                                }
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                                    isListeningForWakeWord = false
                                }
                                else -> {
                                    mainHandler.postDelayed({ restartListeningInternal() }, 200L)
                                }
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()?.trim() ?: ""

                            if (text.isNotBlank()) {
                                // 1. Strict self-echo & speaking check
                                if (BackgroundSpeaker.isSpeaking.value || BackgroundSpeaker.isSelfEcho(text)) {
                                    Log.d("SnehaVoiceService", "Ignored self-echo in background: '$text'")
                                    if (isListeningForWakeWord) {
                                        mainHandler.postDelayed({ restartListeningInternal() }, 600L)
                                    }
                                    return
                                }
                                handleBackgroundCommand(text)
                            } else if (isListeningForWakeWord) {
                                mainHandler.postDelayed({ restartListeningInternal() }, 100L)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {}

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
                restartListeningInternal()
            } catch (e: Exception) {
                Log.e("SnehaVoiceService", "Error setting up background voice listener", e)
            }
        }
    }

    private fun restartListeningInternal() {
        if (!isListeningForWakeWord) return
        if (BackgroundSpeaker.isSpeaking.value) {
            // Wait for speaking to finish
            return
        }
        val timeSinceSpeech = System.currentTimeMillis() - BackgroundSpeaker.lastSpeechEndTimeMs
        if (timeSinceSpeech < 800L) {
            mainHandler.postDelayed({ restartListeningInternal() }, 800L - timeSinceSpeech)
            return
        }
        mainHandler.post {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-IN", "en-US"))
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.w("SnehaVoiceService", "restartListeningInternal exception", e)
                try {
                    speechRecognizer?.destroy()
                } catch (ignored: Exception) {}
                speechRecognizer = null
                mainHandler.postDelayed({
                    if (isListeningForWakeWord) startWakeWordListening()
                }, 300L)
            }
        }
    }

    private fun handleBackgroundCommand(text: String) {
        val lower = text.lowercase().trim()
        val customWake = com.example.data.local.VoicePreferences.getCustomWakeWord(this).lowercase().trim()

        // 1. Strict self-echo check
        if (BackgroundSpeaker.isSelfEcho(text)) {
            Log.d("SnehaVoiceService", "handleBackgroundCommand: Ignored self-echo: '$text'")
            if (isListeningForWakeWord) {
                mainHandler.postDelayed({ restartListeningInternal() }, 600L)
            }
            return
        }

        val baseWakeKeywords = listOf(
            "हे स्नेहा", "hey sneha", "सुनो स्नेहा", "hello sneha", "hi sneha", "ok sneha",
            "स्नेहा", "sneha", "हेलो स्नेहा", "हे सुनो स्नेहा", "सुनो"
        )
        val allWakeKeywords = if (customWake.isNotBlank() && customWake != "स्नेहा" && customWake != "sneha") {
            baseWakeKeywords + listOf(customWake, "hey $customWake", "हे $customWake", "hello $customWake", "hi $customWake")
        } else {
            baseWakeKeywords
        }

        // 2. Strict Wake-Word requirement:
        // Must contain "स्नेहा" (wake word) OR be inside an active 10-second conversation session
        val containsWakeWord = allWakeKeywords.any { lower.contains(it) }
        val isWithinActiveSession = System.currentTimeMillis() < activeWakeSessionUntilMs

        if (!containsWakeWord && !isWithinActiveSession) {
            Log.d("SnehaVoiceService", "Ignored ambient speech without wake word: '$text'")
            if (isListeningForWakeWord) {
                mainHandler.postDelayed({ restartListeningInternal() }, 150L)
            }
            return
        }

        // Renew active session window for 10 seconds!
        activeWakeSessionUntilMs = System.currentTimeMillis() + 10000L

        val isJustCalling = allWakeKeywords.any { lower == it } ||
                lower == "siri" || lower == "alexa"

        try {
            com.example.engine.MicChimeManager.playChime(applicationContext)
        } catch (ignored: Exception) {}

        if (isJustCalling) {
            LockScreenHelper.wakeUpScreen(applicationContext, 8000L)
            BackgroundSpeaker.speak("जी मास्टर! मैं हाजिर हूँ, आज्ञा दीजिए!")
            return
        }

        // Strip wake prefix if command was e.g. "स्नेहा टॉर्च जलाओ"
        var cleanInput = text
        for (wp in allWakeKeywords) {
            if (wp.isNotBlank() && cleanInput.startsWith(wp, ignoreCase = true)) {
                cleanInput = cleanInput.substring(wp.length).trim()
                break
            }
            val idx = cleanInput.indexOf(wp, ignoreCase = true)
            if (idx >= 0) {
                val candidate = (cleanInput.substring(0, idx) + " " + cleanInput.substring(idx + wp.length)).trim()
                if (candidate.isNotBlank()) {
                    cleanInput = candidate
                    break
                }
            }
        }
        if (cleanInput.isBlank()) {
            cleanInput = text
        }

        serviceScope.launch(Dispatchers.IO) {
            try {
                val db = SnehaDatabase.getInstance(applicationContext)
                // 1. Record user command in chat history
                db.chatMessageDao().insertMessage(
                    com.example.data.local.ChatMessageEntity(text = text, isUser = true)
                )

                // 2. Process command using SnehaCommandEngine in background
                val cmdResult = com.example.engine.SnehaCommandEngine.processCommand(
                    context = applicationContext,
                    activity = null,
                    rawInput = cleanInput
                )

                // 3. Record Sneha response in chat history
                db.chatMessageDao().insertMessage(
                    com.example.data.local.ChatMessageEntity(
                        text = cmdResult.spokenResponse,
                        isUser = false,
                        isOtpMasked = cmdResult.isOtpRefusal
                    )
                )

                // 4. Wake up screen so user sees action result
                LockScreenHelper.wakeUpScreen(applicationContext, 8000L)
                when (cmdResult.actionTaken) {
                    is com.example.data.model.SnehaAction.UnlockPhone,
                    is com.example.data.model.SnehaAction.OpenSecurityUnlock,
                    is com.example.data.model.SnehaAction.EmergencyUnlockAndSos -> {
                        LockScreenHelper.launchOverLockScreen(applicationContext, startVoiceMic = false)
                    }
                    else -> {}
                }

                // 5. Speak response aloud in background
                if (cmdResult.spokenResponse.isNotBlank()) {
                    BackgroundSpeaker.speak(cmdResult.spokenResponse)
                } else {
                    mainHandler.postDelayed({ restartListeningInternal() }, 400L)
                }
            } catch (e: Exception) {
                Log.e("SnehaVoiceService", "Error executing background command", e)
                BackgroundSpeaker.speak("मास्टर, आदेश का पालन करने में समस्या हुई।")
            }
        }
    }

    private fun stopWakeWordListening() {
        isListeningForWakeWord = false
        mainHandler.removeCallbacksAndMessages(null)
        if (wakeLock?.isHeld == true) {
            try {
                wakeLock?.release()
            } catch (ignored: Exception) {}
        }
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (ignored: Exception) {}
    }

    private fun readRecentMessagesAloud() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                LockScreenHelper.wakeUpScreen(applicationContext, 6000L)
                val db = SnehaDatabase.getInstance(applicationContext)
                val dao = db.notificationLogDao()
                val recent = dao.getRecentNotificationList(5)

                if (recent.isEmpty()) {
                    BackgroundSpeaker.speak("मास्टर, आपके पास कोई हालिया संदेश नहीं मिला है।")
                    return@launch
                }

                BackgroundSpeaker.speak("मास्टर, स्नेहा आपके हाल के संदेश पढ़ रही है। सुरक्षा कारणों से OTP सुरक्षित रखा गया है।")
                delay(3500)

                recent.forEachIndexed { index, item ->
                    val safeSpeech = OtpSafetyGuard.sanitizeForSpeech(item.sender, item.originalText)
                    BackgroundSpeaker.speak("संदेश ${index + 1}: $safeSpeech")
                    delay(4500)
                }
            } catch (e: Exception) {
                BackgroundSpeaker.speak("मास्टर, संदेश प्राप्त करने में समस्या हुई।")
            }
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val voiceIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_TRIGGER_VOICE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_START_MIC, true)
        }
        val voicePendingIntent = PendingIntent.getActivity(
            this, 1, voiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val readIntent = Intent(this, SnehaVoiceService::class.java).apply {
            action = ACTION_READ_MESSAGES
        }
        val readPendingIntent = PendingIntent.getService(
            this, 2, readIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sosIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_TRIGGER_SOS
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TRIGGER_SOS, true)
        }
        val sosPendingIntent = PendingIntent.getActivity(
            this, 3, sosIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("स्नेहा AI वॉयस असिस्टेंट 🎙️")
            .setContentText("Siri की तरह सक्रिय • 'हे स्नेहा' बोलें • हमेशा हाजिर")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_btn_speak_now, "बोलें", voicePendingIntent)
            .addAction(android.R.drawable.ic_menu_send, "मैसेज सुनें", readPendingIntent)
            .addAction(android.R.drawable.ic_dialog_alert, "इमरजेंसी", sosPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "स्नेहा AI असिस्टेंट सेवा"
            val descriptionText = "बैकग्राउंड वॉयस और वेक-वर्ड डिटेक्शन चैनल"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        stopWakeWordListening()
        try {
            toneGenerator?.release()
        } catch (ignored: Exception) {}
    }
}
