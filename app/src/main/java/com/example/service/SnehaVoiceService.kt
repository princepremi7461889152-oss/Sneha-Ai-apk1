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

        fun pauseListeningForForeground(context: Context) {
            try {
                val intent = Intent(context, SnehaVoiceService::class.java).apply {
                    action = ACTION_PAUSE_FOR_FOREGROUND
                }
                context.startService(intent)
            } catch (ignored: Exception) {}
        }

        fun resumeListeningFromForeground(context: Context) {
            val hasMicPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasMicPermission) return
            try {
                val intent = Intent(context, SnehaVoiceService::class.java).apply {
                    action = ACTION_RESUME_FROM_FOREGROUND
                }
                context.startService(intent)
            } catch (ignored: Exception) {}
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
        createNotificationChannel()
        BackgroundSpeaker.initialize(this)
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
        } catch (ignored: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_STOP_SERVICE -> {
                stopWakeWordListening()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE_FOR_FOREGROUND -> {
                stopWakeWordListening()
                return START_STICKY
            }
            ACTION_RESUME_FROM_FOREGROUND -> {
                val hasMicPermission = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (hasMicPermission && !isListeningForWakeWord) {
                    startWakeWordListening()
                }
                return START_STICKY
            }
            ACTION_READ_MESSAGES -> {
                readRecentMessagesAloud()
            }
        }

        val hasMicPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            Log.w("SnehaVoiceService", "RECORD_AUDIO not granted, stopping service gracefully")
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildForegroundNotification()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e("SnehaVoiceService", "startForeground error", e)
            stopSelf()
            return START_NOT_STICKY
        }

        // Start Siri-like background wake-word listening
        startWakeWordListening()

        return START_STICKY
    }

    private fun startWakeWordListening() {
        val hasMic = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasMic || !SpeechRecognizer.isRecognitionAvailable(this)) return
        isListeningForWakeWord = true

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
                            if (isListeningForWakeWord) {
                                // Restart listening in a polite loop
                                mainHandler.postDelayed({ restartListeningInternal() }, 1500)
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()?.lowercase()?.trim() ?: ""

                            val hasWakeWord = text.contains("स्नेहा") || text.contains("sneha") ||
                                    text.contains("hey sneha") || text.contains("सुनो स्नेहा") ||
                                    text.contains("सुनो") || text.contains("siri") ||
                                    text.contains("हेलो स्नेहा") || text.contains("नमस्ते स्नेहा")

                            if (hasWakeWord) {
                                onWakeWordTriggered()
                            } else if (isListeningForWakeWord) {
                                mainHandler.postDelayed({ restartListeningInternal() }, 800)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()?.lowercase()?.trim() ?: ""
                            if (text.contains("स्नेहा") || text.contains("sneha") || text.contains("सुनो स्नेहा")) {
                                onWakeWordTriggered()
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
                restartListeningInternal()
            } catch (e: Exception) {
                Log.e("SnehaVoiceService", "Error setting up wake word listener", e)
            }
        }
    }

    private fun restartListeningInternal() {
        if (!isListeningForWakeWord) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            mainHandler.postDelayed({ restartListeningInternal() }, 2000)
        }
    }

    private fun onWakeWordTriggered() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (ignored: Exception) {}

        // 1. Wake screen and bring activity to front over lockscreen
        LockScreenHelper.wakeUpScreen(applicationContext, 10000L)
        LockScreenHelper.launchOverLockScreen(applicationContext, startVoiceMic = true)

        // 2. Announce ready to Master
        BackgroundSpeaker.speak("जी मास्टर! मैं हाजिर हूँ, आज्ञा दीजिए!")
    }

    private fun stopWakeWordListening() {
        isListeningForWakeWord = false
        mainHandler.removeCallbacksAndMessages(null)
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
        stopWakeWordListening()
        try {
            toneGenerator?.release()
        } catch (ignored: Exception) {}
    }
}
