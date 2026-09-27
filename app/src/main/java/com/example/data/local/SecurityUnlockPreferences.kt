package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

enum class UnlockType(val displayName: String) {
    PIN("पिन (PIN)"),
    PATTERN("पैटर्न (Pattern)"),
    PASSWORD("पासवर्ड (Password)")
}

object SecurityUnlockPreferences {

    private const val PREFS_NAME = "sneha_security_unlock"
    private const val KEY_UNLOCK_TYPE = "unlock_type"
    private const val KEY_PIN = "saved_pin"
    private const val KEY_PATTERN = "saved_pattern"
    private const val KEY_PASSWORD = "saved_password"
    private const val KEY_VOICE_UNLOCK_ENABLED = "voice_unlock_enabled"
    private const val KEY_LOCKSCREEN_GUARD_ACTIVE = "lockscreen_guard_active"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getUnlockType(context: Context): UnlockType {
        val name = getPrefs(context).getString(KEY_UNLOCK_TYPE, UnlockType.PIN.name)
        return try {
            UnlockType.valueOf(name ?: UnlockType.PIN.name)
        } catch (e: Exception) {
            UnlockType.PIN
        }
    }

    fun saveUnlockType(context: Context, type: UnlockType) {
        getPrefs(context).edit().putString(KEY_UNLOCK_TYPE, type.name).apply()
    }

    fun getSavedPin(context: Context): String {
        return getPrefs(context).getString(KEY_PIN, "1234") ?: "1234"
    }

    fun savePin(context: Context, pin: String) {
        getPrefs(context).edit().putString(KEY_PIN, pin).apply()
    }

    fun getSavedPattern(context: Context): String {
        // Dot indices e.g. "0,1,2,5,8" (connecting top-left to bottom-right)
        return getPrefs(context).getString(KEY_PATTERN, "0,1,2,4,6,7,8") ?: "0,1,2,4,6,7,8"
    }

    fun savePattern(context: Context, patternDots: String) {
        getPrefs(context).edit().putString(KEY_PATTERN, patternDots).apply()
    }

    fun getSavedPassword(context: Context): String {
        return getPrefs(context).getString(KEY_PASSWORD, "master123") ?: "master123"
    }

    fun savePassword(context: Context, password: String) {
        getPrefs(context).edit().putString(KEY_PASSWORD, password).apply()
    }

    fun isVoiceUnlockEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_VOICE_UNLOCK_ENABLED, true)
    }

    fun saveVoiceUnlockEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_VOICE_UNLOCK_ENABLED, enabled).apply()
    }

    fun isLockscreenGuardActive(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_LOCKSCREEN_GUARD_ACTIVE, true)
    }

    fun saveLockscreenGuardActive(context: Context, active: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_LOCKSCREEN_GUARD_ACTIVE, active).apply()
    }

    fun verifyPin(context: Context, enteredPin: String): Boolean {
        return getSavedPin(context) == enteredPin.trim()
    }

    fun verifyPattern(context: Context, enteredPattern: String): Boolean {
        return getSavedPattern(context) == enteredPattern.trim()
    }

    fun verifyPassword(context: Context, enteredPassword: String): Boolean {
        return getSavedPassword(context) == enteredPassword.trim()
    }
}
