package com.example.engine

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Singleton speaker capable of playing speech announcements in both
 * foreground and background, including when the device is locked.
 */
object BackgroundSpeaker : TextToSpeech.OnInitListener {

    private const val TAG = "BackgroundSpeaker"
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private val pendingQueue = mutableListOf<Pair<String, Int>>()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    var isAutoReadMessagesEnabled = true
    var speechRate = 1.0f
    var speechPitch = 1.05f

    fun initialize(context: Context) {
        speechRate = com.example.data.local.VoicePreferences.getSpeechRate(context)
        speechPitch = com.example.data.local.VoicePreferences.getSpeechPitch(context)
        isAutoReadMessagesEnabled = com.example.data.local.VoicePreferences.isAutoReadEnabled(context)

        if (textToSpeech == null) {
            try {
                textToSpeech = TextToSpeech(context.applicationContext, this)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing background TTS", e)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            val hindi = Locale("hi", "IN")
            val result = textToSpeech?.setLanguage(hindi)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                val englishIn = Locale("en", "IN")
                val resEn = textToSpeech?.setLanguage(englishIn)
                if (resEn == TextToSpeech.LANG_MISSING_DATA || resEn == TextToSpeech.LANG_NOT_SUPPORTED) {
                    textToSpeech?.setLanguage(Locale.getDefault())
                }
            }
            textToSpeech?.setSpeechRate(speechRate)
            textToSpeech?.setPitch(speechPitch)

            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
            Log.d(TAG, "Background TTS initialized successfully")

            synchronized(pendingQueue) {
                while (pendingQueue.isNotEmpty()) {
                    val (text, queueMode) = pendingQueue.removeAt(0)
                    speak(text, queueMode)
                }
            }
        } else {
            Log.e(TAG, "Background TTS init failed with status: $status")
        }
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        val clean = text.replace("*", "").replace("#", "").replace("`", "").trim()
        if (clean.isBlank()) return

        if (textToSpeech == null || !isInitialized) {
            synchronized(pendingQueue) {
                pendingQueue.add(Pair(clean, queueMode))
            }
            return
        }

        try {
            textToSpeech?.setSpeechRate(speechRate)
            textToSpeech?.setPitch(speechPitch)
            val utteranceId = "sneha_bg_${System.currentTimeMillis()}"
            textToSpeech?.speak(clean, queueMode, null, utteranceId)
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking in background", e)
        }
    }

    fun stop() {
        try {
            if (textToSpeech?.isSpeaking == true) {
                textToSpeech?.stop()
            }
            _isSpeaking.value = false
        } catch (ignored: Exception) {}
    }

    fun shutdown() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            isInitialized = false
        } catch (ignored: Exception) {}
    }
}
