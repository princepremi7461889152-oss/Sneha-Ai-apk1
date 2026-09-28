package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.VoicePersona
import com.example.data.model.VoicePersonaId

object VoicePreferences {

    private const val PREF_NAME = "sneha_voice_settings"
    private const val KEY_PERSONA_ID = "voice_persona_id"
    private const val KEY_SPEECH_RATE = "voice_speech_rate"
    private const val KEY_SPEECH_PITCH = "voice_speech_pitch"
    private const val KEY_VOICE_GENDER = "voice_gender_selected"
    private const val KEY_VOICE_OUTPUT_ENABLED = "voice_output_enabled"
    private const val KEY_AUTO_READ_ENABLED = "auto_read_messages_enabled"
    private const val KEY_CUSTOM_WAKE_WORD = "custom_wake_word"
    private const val KEY_CUSTOM_USER_NAME = "custom_user_name"
    private const val KEY_SPAM_BLOCK_ENABLED = "spam_block_enabled"
    private const val KEY_WHATSAPP_AUTO_REPLY = "whatsapp_auto_reply_enabled"
    private const val KEY_WHATSAPP_REPLY_TEXT = "whatsapp_reply_text"
    private const val KEY_CLASS_TIMETABLE_MODE = "class_timetable_mode_enabled"
    private const val KEY_TRANSLATOR_SOURCE_LANG = "translator_source_lang"
    private const val KEY_TRANSLATOR_TARGET_LANG = "translator_target_lang"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getCustomWakeWord(context: Context): String {
        return getPrefs(context).getString(KEY_CUSTOM_WAKE_WORD, "स्नेहा") ?: "स्नेहा"
    }

    fun saveCustomWakeWord(context: Context, wakeWord: String) {
        val word = if (wakeWord.isBlank()) "स्नेहा" else wakeWord.trim()
        getPrefs(context).edit().putString(KEY_CUSTOM_WAKE_WORD, word).apply()
    }

    fun getUserName(context: Context): String {
        return getPrefs(context).getString(KEY_CUSTOM_USER_NAME, "मास्टर") ?: "मास्टर"
    }

    fun saveUserName(context: Context, userName: String) {
        val name = if (userName.isBlank()) "मास्टर" else userName.trim()
        getPrefs(context).edit().putString(KEY_CUSTOM_USER_NAME, name).apply()
    }

    fun isSpamBlockEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SPAM_BLOCK_ENABLED, true)
    }

    fun saveSpamBlockEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SPAM_BLOCK_ENABLED, enabled).apply()
    }

    fun isWhatsAppAutoReplyEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_WHATSAPP_AUTO_REPLY, true)
    }

    fun saveWhatsAppAutoReplyEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_WHATSAPP_AUTO_REPLY, enabled).apply()
    }

    fun getWhatsAppReplyText(context: Context): String {
        return getPrefs(context).getString(
            KEY_WHATSAPP_REPLY_TEXT,
            "नमस्ते! मैं मास्टर की AI असिस्टेंट स्नेहा बोल रही हूँ। मास्टर अभी व्यस्त हैं, जल्द ही आपको रिप्लाई करेंगे। 🙏"
        ) ?: "नमस्ते! मैं मास्टर की AI असिस्टेंट स्नेहा बोल रही हूँ। मास्टर अभी व्यस्त हैं।"
    }

    fun saveWhatsAppReplyText(context: Context, text: String) {
        getPrefs(context).edit().putString(KEY_WHATSAPP_REPLY_TEXT, text).apply()
    }

    fun isClassTimetableModeEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_CLASS_TIMETABLE_MODE, true)
    }

    fun saveClassTimetableModeEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_CLASS_TIMETABLE_MODE, enabled).apply()
    }

    fun getTranslatorSourceLang(context: Context): String {
        return getPrefs(context).getString(KEY_TRANSLATOR_SOURCE_LANG, "hi") ?: "hi"
    }

    fun saveTranslatorSourceLang(context: Context, lang: String) {
        getPrefs(context).edit().putString(KEY_TRANSLATOR_SOURCE_LANG, lang).apply()
    }

    fun getTranslatorTargetLang(context: Context): String {
        return getPrefs(context).getString(KEY_TRANSLATOR_TARGET_LANG, "en") ?: "en"
    }

    fun saveTranslatorTargetLang(context: Context, lang: String) {
        getPrefs(context).edit().putString(KEY_TRANSLATOR_TARGET_LANG, lang).apply()
    }

    fun getPersona(context: Context): VoicePersona {
        val id = getPrefs(context).getString(KEY_PERSONA_ID, VoicePersonaId.CLASSIC.id)
        return VoicePersona.fromId(id)
    }

    fun savePersona(context: Context, persona: VoicePersona) {
        getPrefs(context).edit()
            .putString(KEY_PERSONA_ID, persona.id.id)
            .apply()
    }

    fun getSpeechRate(context: Context): Float {
        return getPrefs(context).getFloat(KEY_SPEECH_RATE, 1.0f)
    }

    fun saveSpeechRate(context: Context, rate: Float) {
        getPrefs(context).edit()
            .putFloat(KEY_SPEECH_RATE, rate)
            .apply()
    }

    fun getSpeechPitch(context: Context): Float {
        return getPrefs(context).getFloat(KEY_SPEECH_PITCH, 1.18f)
    }

    fun saveSpeechPitch(context: Context, pitch: Float) {
        getPrefs(context).edit()
            .putFloat(KEY_SPEECH_PITCH, pitch)
            .apply()
    }

    fun getVoiceGender(context: Context): String {
        return getPrefs(context).getString(KEY_VOICE_GENDER, "FEMALE") ?: "FEMALE"
    }

    fun saveVoiceGender(context: Context, gender: String) {
        getPrefs(context).edit().putString(KEY_VOICE_GENDER, gender).apply()
    }

    fun isVoiceOutputEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_VOICE_OUTPUT_ENABLED, true)
    }

    fun saveVoiceOutputEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit()
            .putBoolean(KEY_VOICE_OUTPUT_ENABLED, enabled)
            .apply()
    }

    fun isAutoReadEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_READ_ENABLED, true)
    }

    fun saveAutoReadEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit()
            .putBoolean(KEY_AUTO_READ_ENABLED, enabled)
            .apply()
    }
}
