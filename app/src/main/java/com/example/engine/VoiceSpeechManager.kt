package com.example.engine

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.local.VoicePreferences
import com.example.data.model.VoicePersona
import com.example.data.model.VoiceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class VoiceSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val tag = "VoiceSpeechManager"
    private val scope = CoroutineScope(Dispatchers.Main)

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private val pendingSpeechQueue = mutableListOf<String>()

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _spokenTextLive = MutableStateFlow("")
    val spokenTextLive: StateFlow<String> = _spokenTextLive.asStateFlow()

    private val _isContinuousListening = MutableStateFlow(VoicePreferences.isContinuousListeningEnabled(context))
    val isContinuousListening: StateFlow<Boolean> = _isContinuousListening.asStateFlow()

    var onSpeechRecognized: ((String) -> Unit)? = null
    var onSpeechError: ((String) -> Unit)? = null
    var onWakeWordDetected: (() -> Unit)? = null

    var isVoiceOutputEnabled: Boolean = VoicePreferences.isVoiceOutputEnabled(context)
    var speechRate: Float = VoicePreferences.getSpeechRate(context)
    var speechPitch: Float = VoicePreferences.getSpeechPitch(context)
    var currentPersona: VoicePersona = VoicePreferences.getPersona(context)

    private var restartListeningJob: Job? = null
    private var toneGenerator: ToneGenerator? = null
    private var lastSpokenText: String = ""
    private var lastSpokenTime: Long = 0L

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (ignored: Exception) {}

        try {
            textToSpeech = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true

            // Try Hindi (India), fallback to English (India), US, or default locale
            val hindi = Locale("hi", "IN")
            val result = textToSpeech?.setLanguage(hindi)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                val englishIn = Locale("en", "IN")
                val resEn = textToSpeech?.setLanguage(englishIn)
                if (resEn == TextToSpeech.LANG_MISSING_DATA || resEn == TextToSpeech.LANG_NOT_SUPPORTED) {
                    textToSpeech?.setLanguage(Locale.getDefault())
                }
            }

            // Find best high-quality natural female voice available in the engine
            try {
                val availableVoices = textToSpeech?.voices
                if (!availableVoices.isNullOrEmpty()) {
                    fun isFemale(name: String): Boolean {
                        val n = name.lowercase()
                        val isExplicitMale = (n.contains("male") && !n.contains("female")) ||
                                n.contains("man") ||
                                n.contains("-hie") ||
                                n.contains("#male") ||
                                n.contains("_male")
                        if (isExplicitMale) return false
                        return n.contains("female") ||
                                n.contains("woman") ||
                                n.contains("f00") ||
                                n.contains("-hid") ||
                                n.contains("-hia") ||
                                n.contains("-hic") ||
                                n.contains("-cfn") ||
                                n.contains("zira") ||
                                n.contains("eva")
                    }

                    // 1. Prioritize Hindi female voice
                    val bestFemaleVoice = availableVoices.firstOrNull { v ->
                        v.locale.language == "hi" && isFemale(v.name)
                    } ?: availableVoices.firstOrNull { v ->
                        v.locale.country.equals("IN", ignoreCase = true) && isFemale(v.name)
                    } ?: availableVoices.firstOrNull { v ->
                        isFemale(v.name)
                    } ?: availableVoices.firstOrNull { v ->
                        val n = v.name.lowercase()
                        v.locale.language == "hi" && !((n.contains("male") && !n.contains("female")) || n.contains("-hie") || n.contains("man"))
                    }

                    if (bestFemaleVoice != null) {
                        textToSpeech?.voice = bestFemaleVoice
                        Log.d(tag, "Selected female TTS voice: ${bestFemaleVoice.name}")
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Voice engine selection note", e)
            }

            // Ensure pitch is feminine and sweet (1.18f)
            val effectivePitch = if (speechPitch < 1.05f) 1.18f else speechPitch
            speechPitch = effectivePitch
            textToSpeech?.setSpeechRate(speechRate)
            textToSpeech?.setPitch(effectivePitch)

            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _voiceState.value = VoiceState.SPEAKING
                    lastSpokenTime = System.currentTimeMillis()
                    try {
                        speechRecognizer?.cancel()
                    } catch (ignored: Exception) {}
                    isRecognizerBusy = false
                }

                override fun onDone(utteranceId: String?) {
                    lastSpokenTime = System.currentTimeMillis()
                    _voiceState.value = if (_isContinuousListening.value) VoiceState.LISTENING else VoiceState.IDLE
                    _audioAmplitude.value = 0f
                    // Delay 650ms so speaker audio vibrations and room echo completely die down
                    scheduleContinuousListenResume(650L)
                }

                override fun onError(utteranceId: String?) {
                    lastSpokenTime = System.currentTimeMillis()
                    _voiceState.value = if (_isContinuousListening.value) VoiceState.LISTENING else VoiceState.IDLE
                    _audioAmplitude.value = 0f
                    scheduleContinuousListenResume(400L)
                }
            })

            // Flush pending speech
            synchronized(pendingSpeechQueue) {
                while (pendingSpeechQueue.isNotEmpty()) {
                    val queued = pendingSpeechQueue.removeAt(0)
                    speak(queued)
                }
            }
        } else {
            Log.e(tag, "TTS init failed with status: $status")
        }
    }

    private var isRecognizerBusy = false

    fun playWakeBeep() {
        try {
            MicChimeManager.playChime(context)
        } catch (ignored: Exception) {}
    }

    @Synchronized
    private fun getOrCreateRecognizer(): SpeechRecognizer? {
        if (speechRecognizer != null) return speechRecognizer

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(tag, "Speech recognition is not available on this device")
            return null
        }

        return try {
            SpeechRecognizer.createSpeechRecognizer(context.applicationContext).also { recognizer ->
                speechRecognizer = recognizer
                recognizer.setRecognitionListener(createRecognitionListener())
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to create SpeechRecognizer", e)
            null
        }
    }

    private fun recreateRecognizer() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (ignored: Exception) {}
        speechRecognizer = null
        isRecognizerBusy = false
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _voiceState.value = VoiceState.LISTENING
                _spokenTextLive.value = ""
                isRecognizerBusy = true
            }

            override fun onBeginningOfSpeech() {
                _voiceState.value = VoiceState.LISTENING
            }

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                _audioAmplitude.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                if (_spokenTextLive.value.isNotBlank()) {
                    _voiceState.value = VoiceState.THINKING
                }
                _audioAmplitude.value = 0f
            }

            override fun onError(error: Int) {
                _audioAmplitude.value = 0f
                isRecognizerBusy = false

                // Keep state in LISTENING in continuous mode so UI doesn't flicker or switch off
                if (_isContinuousListening.value) {
                    _voiceState.value = VoiceState.LISTENING
                } else {
                    _voiceState.value = VoiceState.IDLE
                }

                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        // User paused speaking or brief silence - immediately resume without turning off
                        if (_isContinuousListening.value) {
                            scheduleContinuousListenResume(60L)
                        }
                    }
                    SpeechRecognizer.ERROR_CLIENT -> { // Code 5
                        recreateRecognizer()
                        if (_isContinuousListening.value) {
                            scheduleContinuousListenResume(120L)
                        }
                    }
                    11, // SpeechRecognizer.ERROR_SERVER_DISCONNECTED
                    SpeechRecognizer.ERROR_SERVER -> {
                        recreateRecognizer()
                        if (_isContinuousListening.value) {
                            scheduleContinuousListenResume(200L)
                        }
                    }
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> { // Code 8
                        try {
                            speechRecognizer?.cancel()
                        } catch (ignored: Exception) {}
                        isRecognizerBusy = false
                        if (_isContinuousListening.value) {
                            scheduleContinuousListenResume(120L)
                        }
                    }
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> { // Code 9
                        _voiceState.value = VoiceState.IDLE
                        onSpeechError?.invoke("माइक की अनुमति (Microphone Permission) आवश्यक है। कृपया सेटिंग्स से अनुमति दें।")
                    }
                    SpeechRecognizer.ERROR_AUDIO -> { // Code 3
                        recreateRecognizer()
                        if (_isContinuousListening.value) {
                            scheduleContinuousListenResume(200L)
                        }
                    }
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                        if (_isContinuousListening.value) {
                            scheduleContinuousListenResume(800L)
                        } else {
                            onSpeechError?.invoke("इंटरनेट या वॉयस सर्विस कनेक्शन जांचें।")
                        }
                    }
                    else -> {
                        if (_isContinuousListening.value) {
                            scheduleContinuousListenResume(120L)
                        }
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                _audioAmplitude.value = 0f
                isRecognizerBusy = false

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim() ?: ""
                _spokenTextLive.value = text

                if (text.isNotBlank()) {
                    _voiceState.value = VoiceState.THINKING
                    handleRecognizedText(text)
                } else if (_isContinuousListening.value) {
                    _voiceState.value = VoiceState.LISTENING
                    scheduleContinuousListenResume(60L)
                } else {
                    _voiceState.value = VoiceState.IDLE
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                _spokenTextLive.value = text
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun startListening(playChime: Boolean = false) {
        if (playChime) {
            try {
                MicChimeManager.playChime(context)
            } catch (ignored: Exception) {}
        }
        restartListeningJob?.cancel()
        if (isSpeaking()) {
            stopSpeaking()
        }

        // Check RECORD_AUDIO permission safely
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            onSpeechError?.invoke("माइक्रोफ़ोन की अनुमति नहीं है।")
            return
        }

        val recognizer = getOrCreateRecognizer()
        if (recognizer == null) {
            onSpeechError?.invoke("डिवाइस में वॉयस पहचान उपलब्ध नहीं है।")
            return
        }

        // Keep VoiceState in LISTENING so the mic UI stays continuously active
        _voiceState.value = VoiceState.LISTENING

        // Only cancel previous active recognition session if it is actively running
        if (isRecognizerBusy) {
            try {
                recognizer.cancel()
            } catch (ignored: Exception) {}
            isRecognizerBusy = false
        }

        val customWake = VoicePreferences.getCustomWakeWord(context)
        val promptText = if (customWake.equals("स्नेता", ignoreCase = true) || customWake.equals("स्नेहा", ignoreCase = true)) {
            "स्नेहा सुन रही है, बोलिए मास्टर..."
        } else {
            "$customWake सुन रही है, बोलिए मास्टर..."
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-IN", "en-US"))
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, promptText)
        }

        try {
            recognizer.startListening(intent)
            isRecognizerBusy = true
        } catch (e: Exception) {
            isRecognizerBusy = false
            Log.w(tag, "SpeechRecognizer startListening failed: ${e.message}")
            recreateRecognizer()
            if (_isContinuousListening.value) {
                scheduleContinuousListenResume(250L)
            } else {
                _voiceState.value = VoiceState.IDLE
            }
        }
    }

    private fun handleRecognizedText(text: String) {
        val lower = text.lowercase().trim()

        // 1. Strict Echo & Self-Voice Suppression:
        // If TTS is currently speaking or just finished within 1400ms, check if recognized text is an echo
        val timeSinceSpeech = System.currentTimeMillis() - lastSpokenTime
        if (isSpeaking() || timeSinceSpeech < 1400L) {
            val lastClean = lastSpokenText.lowercase().trim()
            val isEcho = (lastClean.isNotBlank() && (lastClean.contains(lower) || lower.contains(lastClean))) ||
                    lower.contains("जी मास्टर") || lower.contains("हाजिर हूँ") ||
                    lower.contains("बोल रही हूँ") || lower.contains("सुन रही हूँ") ||
                    lower.contains("आज्ञा दीजिए") || lower.contains("मेरे जानू") ||
                    (lastClean.length > 6 && lower.length > 6 && (lastClean.startsWith(lower.take(6)) || lower.startsWith(lastClean.take(6))))

            if (isEcho) {
                Log.d(tag, "Suppressed acoustic feedback / self-voice echo: '$text'")
                if (_isContinuousListening.value) {
                    scheduleContinuousListenResume(500L)
                }
                return
            }
        }

        val customWake = VoicePreferences.getCustomWakeWord(context).lowercase().trim()

        val defaultWakeList = listOf(
            "स्नेहा", "sneha", "hey sneha", "हे स्नेहा", "सुनो स्नेहा",
            "सुनो", "hello sneha", "hi sneha", "ok sneha", "siri", "alexa"
        )

        val customWakeList = if (customWake.isNotBlank() && customWake != "स्नेहा" && customWake != "sneha") {
            listOf(
                customWake,
                "hey $customWake",
                "हे $customWake",
                "सुनो $customWake",
                "hello $customWake",
                "hi $customWake",
                "ok $customWake"
            )
        } else {
            emptyList()
        }

        val isJustCalling = defaultWakeList.contains(lower) ||
                customWakeList.contains(lower) ||
                (customWake.isNotBlank() && lower == customWake)

        if (isJustCalling) {
            playWakeBeep()
            onWakeWordDetected?.invoke()
            if (currentPersona.id == com.example.data.model.VoicePersonaId.GIRLFRIEND) {
                speak("हाँ मेरे जानू! मैं हाजिर हूँ, आज्ञा दीजिए! ❤️")
            } else {
                speak("जी मास्टर! मैं हाजिर हूँ, आज्ञा दीजिए!")
            }
        } else {
            // Full command recognized
            onSpeechRecognized?.invoke(text)
        }
    }

    fun scheduleContinuousListenResume(delayMs: Long = 100L) {
        if (!_isContinuousListening.value) return
        restartListeningJob?.cancel()
        restartListeningJob = scope.launch {
            delay(delayMs)
            if (!isSpeaking() && _isContinuousListening.value) {
                startListening()
            }
        }
    }

    fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking == true || _voiceState.value == VoiceState.SPEAKING
    }

    fun setContinuousListening(enabled: Boolean) {
        _isContinuousListening.value = enabled
        VoicePreferences.saveContinuousListeningEnabled(context, enabled)
        if (!enabled) {
            restartListeningJob?.cancel()
            stopListening()
        } else {
            if (!isSpeaking()) {
                startListening()
            }
        }
    }

    fun stopListening() {
        restartListeningJob?.cancel()
        try {
            speechRecognizer?.stopListening()
            _voiceState.value = VoiceState.IDLE
            _audioAmplitude.value = 0f
        } catch (ignored: Exception) {}
    }

    fun speak(text: String) {
        if (!isVoiceOutputEnabled) return

        val cleanText = text
            .replace("*", "")
            .replace("#", "")
            .replace("`", "")
            .trim()

        if (cleanText.isBlank()) return

        if (!isTtsReady || textToSpeech == null) {
            synchronized(pendingSpeechQueue) {
                pendingSpeechQueue.add(cleanText)
            }
            return
        }

        stopSpeaking()
        restartListeningJob?.cancel()

        // Mute recognizer immediately while speaking to prevent self-voice capture
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (ignored: Exception) {}
        isRecognizerBusy = false

        lastSpokenText = cleanText
        lastSpokenTime = System.currentTimeMillis()

        _voiceState.value = VoiceState.SPEAKING
        textToSpeech?.setSpeechRate(speechRate)
        textToSpeech?.setPitch(speechPitch)

        val utteranceId = "sneha_${System.currentTimeMillis()}"
        textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun applyPersona(persona: VoicePersona, applyPresetValues: Boolean = true) {
        currentPersona = persona
        VoicePreferences.savePersona(context, persona)
        if (applyPresetValues) {
            speechRate = persona.defaultSpeed
            speechPitch = persona.defaultPitch
            VoicePreferences.saveSpeechRate(context, speechRate)
            VoicePreferences.saveSpeechPitch(context, speechPitch)
            BackgroundSpeaker.speechRate = speechRate
            BackgroundSpeaker.speechPitch = speechPitch
        }
        textToSpeech?.setSpeechRate(speechRate)
        textToSpeech?.setPitch(speechPitch)
    }

    fun setSpeechRateValue(rate: Float) {
        speechRate = rate
        VoicePreferences.saveSpeechRate(context, rate)
        BackgroundSpeaker.speechRate = rate
        textToSpeech?.setSpeechRate(rate)
    }

    fun setSpeechPitchValue(pitch: Float) {
        speechPitch = pitch
        VoicePreferences.saveSpeechPitch(context, pitch)
        BackgroundSpeaker.speechPitch = pitch
        textToSpeech?.setPitch(pitch)
    }

    fun setVoiceOutput(enabled: Boolean) {
        isVoiceOutputEnabled = enabled
        VoicePreferences.saveVoiceOutputEnabled(context, enabled)
        if (!enabled) {
            stopSpeaking()
        }
    }

    fun resetToCurrentPersonaDefaults() {
        applyPersona(currentPersona, applyPresetValues = true)
    }

    fun stopSpeaking() {
        try {
            if (textToSpeech?.isSpeaking == true) {
                textToSpeech?.stop()
            }
            if (_voiceState.value == VoiceState.SPEAKING) {
                _voiceState.value = VoiceState.IDLE
            }
        } catch (ignored: Exception) {}
    }

    fun destroy() {
        restartListeningJob?.cancel()
        try {
            toneGenerator?.release()
            speechRecognizer?.destroy()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (ignored: Exception) {}
    }
}
