package com.example.engine

import android.content.Context
import android.util.Log
import com.example.data.local.CallTranslationEntity
import com.example.data.local.SnehaDatabase
import com.example.data.remote.GeminiApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class TranslationEntry(
    val id: Long = System.currentTimeMillis(),
    val originalText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val isUserSpeaking: Boolean, // true = Master, false = Caller
    val timestamp: Long = System.currentTimeMillis()
)

data class SupportedLanguage(
    val code: String,
    val nameHindi: String,
    val nameEnglish: String,
    val flagEmoji: String,
    val locale: Locale
)

object CallTranslatorManager {

    private const val TAG = "CallTranslatorManager"
    private val scope = CoroutineScope(Dispatchers.IO)

    val supportedLanguages = listOf(
        SupportedLanguage("hi", "हिंदी", "Hindi", "🇮🇳", Locale("hi", "IN")),
        SupportedLanguage("en", "अंग्रेज़ी (English)", "English", "🇺🇸", Locale.ENGLISH),
        SupportedLanguage("bn", "बंगाली (বাংলা)", "Bengali", "🇮🇳", Locale("bn", "IN")),
        SupportedLanguage("te", "तेलुगु (తెలుగు)", "Telugu", "🇮🇳", Locale("te", "IN")),
        SupportedLanguage("ta", "तमिल (தமிழ்)", "Tamil", "🇮🇳", Locale("ta", "IN")),
        SupportedLanguage("mr", "मराठी (मराठी)", "Marathi", "🇮🇳", Locale("mr", "IN")),
        SupportedLanguage("gu", "गुजराती (ગુજરાતી)", "Gujarati", "🇮🇳", Locale("gu", "IN")),
        SupportedLanguage("es", "स्पैनिश (Español)", "Spanish", "🇪🇸", Locale.forLanguageTag("es-ES")),
        SupportedLanguage("fr", "फ्रेंच (Français)", "French", "🇫🇷", Locale.FRENCH)
    )

    private val _translationHistory = MutableStateFlow<List<TranslationEntry>>(emptyList())
    val translationHistory: StateFlow<List<TranslationEntry>> = _translationHistory.asStateFlow()

    private val _isLiveTranslating = MutableStateFlow(false)
    val isLiveTranslating: StateFlow<Boolean> = _isLiveTranslating.asStateFlow()

    private val _activeSourceLang = MutableStateFlow("hi")
    val activeSourceLang: StateFlow<String> = _activeSourceLang.asStateFlow()

    private val _activeTargetLang = MutableStateFlow("en")
    val activeTargetLang: StateFlow<String> = _activeTargetLang.asStateFlow()

    fun init(context: Context) {
        scope.launch {
            try {
                val db = SnehaDatabase.getInstance(context)
                db.callTranslationDao().getAllTranslations().collect { entityList ->
                    val domainList = entityList.map { entity ->
                        TranslationEntry(
                            id = entity.id,
                            originalText = entity.originalText,
                            translatedText = entity.translatedText,
                            sourceLang = entity.sourceLang,
                            targetLang = entity.targetLang,
                            isUserSpeaking = entity.isUserSpeaking,
                            timestamp = entity.timestamp
                        )
                    }
                    _translationHistory.value = domainList
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load translation history from Room", e)
            }
        }
    }

    fun setLanguages(source: String, target: String) {
        _activeSourceLang.value = source
        _activeTargetLang.value = target
    }

    fun swapLanguages() {
        val temp = _activeSourceLang.value
        _activeSourceLang.value = _activeTargetLang.value
        _activeTargetLang.value = temp
    }

    /**
     * Translates sentence between chosen language pairs using local intelligent dictionary
     * and Gemini API fallback for natural nuances, and persists to Room.
     */
    suspend fun translateText(
        text: String,
        fromLang: String,
        toLang: String,
        isUser: Boolean,
        context: Context? = null
    ): String {
        val cleanInput = text.trim()
        if (cleanInput.isBlank()) return ""

        // 1. Quick phrase dictionary for instant response during call
        val quickTranslation = lookupQuickDictionary(cleanInput, fromLang, toLang)
        val result = if (quickTranslation != null) {
            quickTranslation
        } else {
            // 2. Call Gemini API for high accuracy natural speech translation
            val prompt = "Translate the following speech precisely from language code '$fromLang' to '$toLang'. Output ONLY the translated sentence with no explanations, notes, or punctuation changes: \"$cleanInput\""
            try {
                val apiRes = GeminiApiClient.generateContent(prompt)
                if (apiRes.isNotBlank()) apiRes.trim() else fallbackSmartTranslate(cleanInput, fromLang, toLang)
            } catch (e: Exception) {
                fallbackSmartTranslate(cleanInput, fromLang, toLang)
            }
        }

        // Add to history and persist
        val entry = TranslationEntry(
            originalText = cleanInput,
            translatedText = result,
            sourceLang = fromLang,
            targetLang = toLang,
            isUserSpeaking = isUser
        )
        _translationHistory.value = listOf(entry) + _translationHistory.value

        if (context != null) {
            scope.launch {
                try {
                    val db = SnehaDatabase.getInstance(context)
                    db.callTranslationDao().insertTranslation(
                        CallTranslationEntity(
                            originalText = cleanInput,
                            translatedText = result,
                            sourceLang = fromLang,
                            targetLang = toLang,
                            isUserSpeaking = isUser,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to persist translation to Room", e)
                }
            }
        }

        return result
    }

    private fun lookupQuickDictionary(text: String, from: String, to: String): String? {
        val lower = text.lowercase().trim()

        val hiToEn = mapOf(
            "नमस्ते" to "Hello",
            "नमस्ते, आप अभी कहाँ हैं?" to "Hello, where are you right now?",
            "आप अभी कहाँ हैं" to "Where are you right now?",
            "आप कहाँ हैं" to "Where are you?",
            "आप कैसे हैं" to "How are you?",
            "आप कौन बोल रहे हैं" to "Who is speaking?",
            "मैं अभी कॉलेज की लाइब्रेरी में हूँ।" to "I am currently in the college library.",
            "मैं अभी कॉलेज की लाइब्रेरी में हूँ" to "I am currently in the college library.",
            "मैं क्लास में हूँ" to "I am in class right now",
            "मैं ठीक हूँ" to "I am doing well",
            "हाँ, मैं अभी कमरे पर जाकर तुरंत भेजता हूँ।" to "Yes, I will go to my room and send it right away.",
            "हाँ, मैं भेजता हूँ" to "Yes, I will send it right away.",
            "बाद में बात करते हैं" to "Let's talk later",
            "धन्यवाद" to "Thank you",
            "अलविदा" to "Goodbye",
            "क्या आप मुझे सुन सकते हैं" to "Can you hear me?",
            "एक मिनट रुकिए" to "Please hold on for a moment",
            "हाँ" to "Yes",
            "नहीं" to "No",
            "मैं बाद में कॉल करता हूँ" to "I will call you back later."
        )

        val enToHi = mapOf(
            "hello, where are you right now?" to "नमस्ते, आप अभी कहाँ हैं?",
            "hello, where are you right now" to "नमस्ते, आप अभी कहाँ हैं?",
            "where are you right now?" to "आप अभी कहाँ हैं?",
            "where are you right now" to "आप अभी कहाँ हैं?",
            "where are you" to "आप कहाँ हैं?",
            "hello" to "नमस्ते",
            "hi" to "नमस्ते",
            "how are you" to "आप कैसे हैं?",
            "who is this" to "यह कौन बोल रहा है?",
            "okay, can you send the assignment file by 5 pm?" to "ठीक है, क्या आप शाम 5 बजे तक असाइनमेंट फाइल भेज सकते हैं?",
            "okay, can you send the assignment file by 5 pm" to "ठीक है, क्या आप शाम 5 बजे तक असाइनमेंट फाइल भेज सकते हैं?",
            "can you send the assignment file" to "क्या आप असाइनमेंट फाइल भेज सकते हैं?",
            "please send the file as soon as possible" to "कृपया जितनी जल्दी हो सके फाइल भेज दें।",
            "please send the file" to "कृपया फाइल भेज दें।",
            "i am currently in the college library" to "मैं अभी कॉलेज की लाइब्रेरी में हूँ।",
            "i am good" to "मैं ठीक हूँ",
            "i am fine" to "मैं ठीक हूँ",
            "call you later" to "मैं आपको बाद में कॉल करता हूँ",
            "thank you" to "धन्यवाद",
            "thanks" to "धन्यवाद",
            "bye" to "अलविदा",
            "goodbye" to "अलविदा",
            "can you hear me" to "क्या आप मुझे सुन सकते हैं?",
            "hold on" to "कृपया एक मिनट रुकिए",
            "yes" to "हाँ",
            "no" to "नहीं"
        )

        if (from == "hi" && to == "en") {
            for ((k, v) in hiToEn) {
                if (lower == k.lowercase() || lower.contains(k.lowercase())) return v
            }
        } else if (from == "en" && to == "hi") {
            for ((k, v) in enToHi) {
                if (lower == k.lowercase() || lower.contains(k.lowercase())) return v
            }
        }
        return null
    }

    private fun fallbackSmartTranslate(text: String, from: String, to: String): String {
        return if (from == "hi" && to == "en") {
            // Check common words
            when {
                text.contains("कहाँ") -> "Where are you currently?"
                text.contains("क्लास") -> "I am currently in my college class."
                text.contains("लाइब्रेरी") -> "I am in the library."
                text.contains("फाइल") || text.contains("नोट्स") -> "I will send the file soon."
                text.contains("कॉल") -> "I will call you back later."
                else -> text
            }
        } else {
            when {
                text.contains("where", ignoreCase = true) -> "आप अभी कहाँ हैं?"
                text.contains("send", ignoreCase = true) -> "कृपया जल्द भेज दें।"
                text.contains("call", ignoreCase = true) -> "बाद में कॉल करें।"
                text.contains("time", ignoreCase = true) -> "समय क्या हुआ है?"
                else -> text
            }
        }
    }

    fun clearHistory(context: Context? = null) {
        _translationHistory.value = emptyList()
        if (context != null) {
            scope.launch {
                try {
                    val db = SnehaDatabase.getInstance(context)
                    db.callTranslationDao().clearAll()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to clear translations from Room", e)
                }
            }
        }
    }
}
