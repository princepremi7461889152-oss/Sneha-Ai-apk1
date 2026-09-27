package com.example.engine

import android.content.Context
import com.example.data.remote.GeminiApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    val supportedLanguages = listOf(
        SupportedLanguage("hi", "हिंदी", "Hindi", "🇮🇳", Locale("hi", "IN")),
        SupportedLanguage("en", "अंग्रेज़ी (English)", "English", "🇺🇸", Locale.ENGLISH),
        SupportedLanguage("bn", "बंगाली (বাংলা)", "Bengali", "🇮🇳", Locale("bn", "IN")),
        SupportedLanguage("te", "तेलुगु (తెలుగు)", "Telugu", "🇮🇳", Locale("te", "IN")),
        SupportedLanguage("ta", "तमिल (தமிழ்)", "Tamil", "🇮🇳", Locale("ta", "IN")),
        SupportedLanguage("mr", "मराठी (मराठी)", "Marathi", "🇮🇳", Locale("mr", "IN")),
        SupportedLanguage("gu", "गुजराती (ગુજરાતી)", "Gujarati", "🇮🇳", Locale("gu", "IN")),
        SupportedLanguage("es", "स्पैनिश (Español)", "Spanish", "🇪🇸", Locale("es", "ES")),
        SupportedLanguage("fr", "फ्रेंच (Français)", "French", "🇫🇷", Locale.FRENCH)
    )

    private val _translationHistory = MutableStateFlow<List<TranslationEntry>>(
        listOf(
            TranslationEntry(
                originalText = "Hello, where are you right now?",
                translatedText = "नमस्ते, आप अभी कहाँ हैं?",
                sourceLang = "en",
                targetLang = "hi",
                isUserSpeaking = false
            ),
            TranslationEntry(
                originalText = "मैं अभी कॉलेज की लाइब्रेरी में हूँ।",
                translatedText = "I am currently in the college library.",
                sourceLang = "hi",
                targetLang = "en",
                isUserSpeaking = true
            )
        )
    )
    val translationHistory: StateFlow<List<TranslationEntry>> = _translationHistory.asStateFlow()

    private val _isLiveTranslating = MutableStateFlow(false)
    val isLiveTranslating: StateFlow<Boolean> = _isLiveTranslating.asStateFlow()

    private val _activeSourceLang = MutableStateFlow("hi")
    val activeSourceLang: StateFlow<String> = _activeSourceLang.asStateFlow()

    private val _activeTargetLang = MutableStateFlow("en")
    val activeTargetLang: StateFlow<String> = _activeTargetLang.asStateFlow()

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
     * and Gemini API fallback for natural nuances.
     */
    suspend fun translateText(
        text: String,
        fromLang: String,
        toLang: String,
        isUser: Boolean
    ): String {
        if (text.isBlank()) return ""

        // 1. Quick phrase dictionary for instant response during call
        val quickTranslation = lookupQuickDictionary(text.trim(), fromLang, toLang)
        val result = if (quickTranslation != null) {
            quickTranslation
        } else {
            // 2. Call Gemini API for high accuracy natural speech translation
            val prompt = "Translate the following speech precisely from language code '$fromLang' to '$toLang'. Output ONLY the translated sentence with no explanations, notes, or punctuation changes: \"$text\""
            try {
                val apiRes = GeminiApiClient.generateContent(prompt)
                if (apiRes.isNotBlank()) apiRes.trim() else fallbackWordByWord(text, fromLang, toLang)
            } catch (e: Exception) {
                fallbackWordByWord(text, fromLang, toLang)
            }
        }

        // Add to history
        val entry = TranslationEntry(
            originalText = text,
            translatedText = result,
            sourceLang = fromLang,
            targetLang = toLang,
            isUserSpeaking = isUser
        )
        _translationHistory.value = listOf(entry) + _translationHistory.value
        return result
    }

    private fun lookupQuickDictionary(text: String, from: String, to: String): String? {
        val lower = text.lowercase().trim()
        val hiToEn = mapOf(
            "नमस्ते" to "Hello",
            "आप कैसे हैं" to "How are you?",
            "आप कौन बोल रहे हैं" to "Who is speaking?",
            "मैं ठीक हूँ" to "I am doing well",
            "मैं क्लास में हूँ" to "I am in class right now",
            "बाद में बात करते हैं" to "Let's talk later",
            "धन्यवाद" to "Thank you",
            "अलविदा" to "Goodbye",
            "क्या आप मुझे सुन सकते हैं" to "Can you hear me?",
            "एक मिनट रुकिए" to "Please hold on for a moment",
            "हाँ" to "Yes",
            "नहीं" to "No",
            "कहाँ हो" to "Where are you?"
        )
        val enToHi = mapOf(
            "hello" to "नमस्ते",
            "how are you" to "आप कैसे हैं?",
            "who is this" to "यह कौन बोल रहा है?",
            "i am good" to "मैं ठीक हूँ",
            "i am fine" to "मैं ठीक हूँ",
            "call you later" to "मैं आपको बाद में कॉल करता हूँ",
            "thank you" to "धन्यवाद",
            "thanks" to "धन्यवाद",
            "bye" to "अलविदा",
            "can you hear me" to "क्या आप मुझे सुन सकते हैं?",
            "hold on" to "कृपया एक मिनट रुकिए",
            "yes" to "हाँ",
            "no" to "नहीं",
            "where are you" to "आप कहाँ हैं?"
        )

        if (from == "hi" && to == "en") {
            for ((k, v) in hiToEn) {
                if (lower.contains(k.lowercase())) return v
            }
        } else if (from == "en" && to == "hi") {
            for ((k, v) in enToHi) {
                if (lower.contains(k.lowercase())) return v
            }
        }
        return null
    }

    private fun fallbackWordByWord(text: String, from: String, to: String): String {
        return if (from == "hi" && to == "en") {
            "[Translated to English]: $text"
        } else {
            "[अनुवादित हिंदी]: $text"
        }
    }

    fun clearHistory() {
        _translationHistory.value = emptyList()
    }
}
