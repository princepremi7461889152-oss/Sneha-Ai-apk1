package com.example.engine

import android.content.Context
import com.example.data.local.VoicePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class RegionalLanguage(
    val id: String,
    val displayName: String,
    val nativeGreeting: String,
    val wakeReply: String,
    val flagEmoji: String,
    val ttsLocale: Locale
) {
    HINDI(
        id = "hi",
        displayName = "हिंदी (Hindi)",
        nativeGreeting = "नमस्ते मास्टर! मैं स्नेहा हूँ, आज्ञा दीजिए!",
        wakeReply = "जी मास्टर! मैं हाजिर हूँ, बोलिए!",
        flagEmoji = "🇮🇳",
        ttsLocale = Locale("hi", "IN")
    ),
    BHOJPURI(
        id = "bho",
        displayName = "भोजपुरी (Bhojpuri)",
        nativeGreeting = "प्रणाम मास्टर! का हाल बा? हम स्नेहा हईं, बताईं का हुकुम बा!",
        wakeReply = "हाँ मास्टर! हम सुनत बानी, बताईं का काम बा!",
        flagEmoji = "🌾",
        ttsLocale = Locale("hi", "IN")
    ),
    MAITHILI(
        id = "mai",
        displayName = "मैथिली (Maithili)",
        nativeGreeting = "प्रणाम मास्टर! अहाँ के की हाल अछि? हम स्नेहा छी, आज्ञा करू!",
        wakeReply = "हँ मास्टर! हम सुनि रहल छी, कहू की करबाक अछि!",
        flagEmoji = "🪷",
        ttsLocale = Locale("hi", "IN")
    ),
    PUNJABI(
        id = "pa",
        displayName = "ਪੰਜਾਬੀ (Punjabi)",
        nativeGreeting = "ਸਤਿ ਸ੍ਰੀ ਅਕਾਲ ਮਾਸਟਰ ਜੀ! ਕੀ ਹਾਲ ਚਾਲ? ਮੈਂ ਸਨੇਹਾ ਹਾਂ, ਹੁਕਮ ਕਰੋ ਜੀ!",
        wakeReply = "ਹਾਂਜੀ ਮਾਸਟਰ ਜੀ! ਮੈਂ ਸੁਣ ਰਹੀ ਹਾਂ, ਦੱਸੋ ਕੀ ਸੇਵਾ ਕਰਾਂ!",
        flagEmoji = "🚜",
        ttsLocale = Locale("pa", "IN")
    ),
    BENGALI(
        id = "bn",
        displayName = "বাংলা (Bengali)",
        nativeGreeting = "নমস্কার মাস্টার! কেমন আছেন? আমি স্নেহা, আপনার কী সাহায্য করতে পারি?",
        wakeReply = "হ্যাঁ মাস্টার! আমি শুনছি, বলুন কী আদেশ?",
        flagEmoji = "🌊",
        ttsLocale = Locale("bn", "IN")
    ),
    ENGLISH(
        id = "en",
        displayName = "English / Hinglish",
        nativeGreeting = "Hello Master! I am Sneha, at your command. How can I assist you?",
        wakeReply = "Yes Master! I am listening, please command me.",
        flagEmoji = "🌐",
        ttsLocale = Locale("en", "IN")
    );

    companion object {
        fun fromId(id: String): RegionalLanguage {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: HINDI
        }
    }
}

object RegionalLanguageManager {

    private const val PREFS_NAME = "sneha_language_prefs"
    private const val KEY_LANG = "selected_language_id"

    private val _currentLanguage = MutableStateFlow(RegionalLanguage.HINDI)
    val currentLanguage: StateFlow<RegionalLanguage> = _currentLanguage.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val id = prefs.getString(KEY_LANG, RegionalLanguage.HINDI.id) ?: RegionalLanguage.HINDI.id
        _currentLanguage.value = RegionalLanguage.fromId(id)
    }

    fun setLanguage(context: Context, lang: RegionalLanguage) {
        _currentLanguage.value = lang
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, lang.id).apply()
    }

    fun getGreeting(context: Context): String {
        init(context)
        return _currentLanguage.value.nativeGreeting
    }

    fun getWakeReply(context: Context): String {
        init(context)
        return _currentLanguage.value.wakeReply
    }

    /**
     * Checks if user requested to switch dialect via voice:
     * e.g. "भोजपुरी में बोलो", "मैथिली में बात करो", "पंजाबी बोलो", "speak in english"
     */
    fun checkVoiceLanguageSwitch(context: Context, input: String): Pair<Boolean, String>? {
        val lower = input.lowercase().trim()

        if (lower.contains("भोजपुरी में") || lower.contains("bhojpuri me") || lower.contains("भोजपुरी बोलो")) {
            setLanguage(context, RegionalLanguage.BHOJPURI)
            return Pair(true, "मास्टर, अब से हम आपसे भोजपुरी में बात करब! का हुकुम बा बताईं?")
        }
        if (lower.contains("मैथिली में") || lower.contains("maithili me") || lower.contains("मैथिली बोलो")) {
            setLanguage(context, RegionalLanguage.MAITHILI)
            return Pair(true, "मास्टर, आब हम अहाँ सँ मैथिली में बात करब! कहू की आदेश अछि?")
        }
        if (lower.contains("पंजाबी में") || lower.contains("punjabi me") || lower.contains("ਪੰਜਾਬੀ")) {
            setLanguage(context, RegionalLanguage.PUNJABI)
            return Pair(true, "ਸਤਿ ਸ੍ਰੀ ਅਕਾਲ ਮਾਸਟਰ ਜੀ! ਹੁਣ ਮੈਂ ਤੁਹਾਡੇ ਨਾਲ ਪੰਜਾਬੀ ਵਿੱਚ ਗੱਲ ਕਰਾਂਗੀ।")
        }
        if (lower.contains("बंगाली में") || lower.contains("bangla me") || lower.contains("বাংলা")) {
            setLanguage(context, RegionalLanguage.BENGALI)
            return Pair(true, "হ্যাঁ মাস্টার! এখন থেকে আমি আপনার সাথে বাংলায় কথা বলব।")
        }
        if (lower.contains("इंग्लिश में") || lower.contains("english me") || lower.contains("speak in english")) {
            setLanguage(context, RegionalLanguage.ENGLISH)
            return Pair(true, "Sure Master! I will now interact with you in English and Hinglish.")
        }
        if (lower.contains("हिंदी में बोलो") || lower.contains("hindi me bolo") || lower.contains("हिंदी में बात करो")) {
            setLanguage(context, RegionalLanguage.HINDI)
            return Pair(true, "मास्टर, अब मैं आपसे शुद्ध हिंदी में बात करूँगी। आज्ञा दीजिए!")
        }

        return null
    }

    /**
     * Formats action responses based on selected dialect
     */
    fun localizeTorchResponse(context: Context, on: Boolean): String {
        init(context)
        return when (_currentLanguage.value) {
            RegionalLanguage.BHOJPURI -> if (on) "मास्टर, फोन के टॉर्च जरा दिहल गईल बा!" else "मास्टर, टॉर्च बुता दिहल गईल बा!"
            RegionalLanguage.MAITHILI -> if (on) "मास्टर, टॉर्च जरा देल गेल अछि!" else "मास्टर, टॉर्च बन्ह कऽ देल गेल अछि!"
            RegionalLanguage.PUNJABI -> if (on) "ਮਾਸਟਰ ਜੀ, ਫ਼ੋਨ ਦੀ ਟਾਰਚ ਜਲਾ ਦਿੱਤੀ ਗਈ ਹੈ!" else "ਮਾਸਟਰ ਜੀ, ਟਾਰਚ ਬੰਦ ਕਰ ਦਿੱਤੀ ਗਈ ਹੈ!"
            RegionalLanguage.BENGALI -> if (on) "মাস্টার, ফোনের টর্চ জ্বালানো হয়েছে!" else "মাস্টার, টর্চ বন্ধ করা হয়েছে!"
            RegionalLanguage.ENGLISH -> if (on) "Master, flashlight turned ON!" else "Master, flashlight turned OFF!"
            RegionalLanguage.HINDI -> if (on) "मास्टर, फ्लैशलाइट टॉर्च ऑन कर दी गई है।" else "मास्टर, फ्लैशलाइट टॉर्च बंद कर दी गई है।"
        }
    }
}
