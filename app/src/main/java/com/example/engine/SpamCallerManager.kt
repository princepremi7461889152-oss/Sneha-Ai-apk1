package com.example.engine

import android.content.Context
import android.telecom.TelecomManager
import android.util.Log
import com.example.data.local.VoicePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CallerCategory(val hindiLabel: String, val badgeColorHex: Long) {
    SAFE_CONTACT("सुरक्षित संपर्क (Verified Contact)", 0xFF00E676),
    TELEMARKETER("टेलीमार्केटर / लोन / क्रेडिट कार्ड", 0xFFFFB300),
    SUSPECTED_FRAUD("संदिग्ध धोखाधड़ी / लॉटरी स्कैम", 0xFFFF1744),
    DELIVERY("कूरियर / जोमैटो / स्विगी डिलीवरी", 0xFF00B0FF),
    UNKNOWN("अज्ञात नंबर (Unknown)", 0xFF9E9E9E)
}

data class SpamReport(
    val phoneNumber: String,
    val callerName: String,
    val category: CallerCategory,
    val spamScorePercentage: Int, // 0 - 100
    val reportsCount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val wasBlocked: Boolean = false
)

object SpamCallerManager {

    private const val TAG = "SpamCallerManager"

    private val _blockedSpamLogs = MutableStateFlow<List<SpamReport>>(
        listOf(
            SpamReport(
                phoneNumber = "+91 14098-76543",
                callerName = "बजाज लोन टेलीमार्केटिंग",
                category = CallerCategory.TELEMARKETER,
                spamScorePercentage = 95,
                reportsCount = 1420,
                wasBlocked = true
            ),
            SpamReport(
                phoneNumber = "+91 91234-56789",
                callerName = "फर्जी लॉटरी / बिजली बिल स्कैम",
                category = CallerCategory.SUSPECTED_FRAUD,
                spamScorePercentage = 98,
                reportsCount = 3890,
                wasBlocked = true
            ),
            SpamReport(
                phoneNumber = "+91 98765-43210",
                callerName = "ब्लूडार्ट कूरियर डिलीवरी बॉय",
                category = CallerCategory.DELIVERY,
                spamScorePercentage = 5,
                reportsCount = 2,
                wasBlocked = false
            )
        )
    )
    val blockedSpamLogs: StateFlow<List<SpamReport>> = _blockedSpamLogs.asStateFlow()

    private val userCustomBlacklist = mutableSetOf<String>()

    /**
     * AI Truecaller Caller ID & Spam Risk Analyzer
     */
    fun analyzeCaller(phoneNumber: String): SpamReport {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")

        // Check user custom blacklist first
        if (userCustomBlacklist.contains(cleanNumber) || userCustomBlacklist.any { cleanNumber.endsWith(it) }) {
            return SpamReport(
                phoneNumber = phoneNumber,
                callerName = "मास्टर द्वारा ब्लॉक नंबर",
                category = CallerCategory.SUSPECTED_FRAUD,
                spamScorePercentage = 100,
                reportsCount = 999
            )
        }

        // 140 prefix is reserved for telemarketing in India by TRAI
        if (cleanNumber.contains("140") || cleanNumber.startsWith("+91140") || cleanNumber.startsWith("140")) {
            return SpamReport(
                phoneNumber = phoneNumber,
                callerName = "प्रमोशनल टेलीमार्केटर (TRAI 140)",
                category = CallerCategory.TELEMARKETER,
                spamScorePercentage = 96,
                reportsCount = 2150
            )
        }

        // Suspicious International prefixes (Nigeria +234, Ivory Coast +225, etc. known for missed call fraud)
        if (cleanNumber.startsWith("+234") || cleanNumber.startsWith("+225") || cleanNumber.startsWith("+232")) {
            return SpamReport(
                phoneNumber = phoneNumber,
                callerName = "अंतरराष्ट्रीय फ्रॉड / वांगिरी स्कैम",
                category = CallerCategory.SUSPECTED_FRAUD,
                spamScorePercentage = 99,
                reportsCount = 5300
            )
        }

        // Toll free marketing
        if (cleanNumber.startsWith("1800") || cleanNumber.startsWith("+911800") || cleanNumber.startsWith("1860")) {
            return SpamReport(
                phoneNumber = phoneNumber,
                callerName = "क्रेडिट कार्ड / बैंक मार्केटिंग",
                category = CallerCategory.TELEMARKETER,
                spamScorePercentage = 75,
                reportsCount = 640
            )
        }

        // Repetitive digit scam patterns (e.g. 9999999999, 1234567890)
        val digitsOnly = cleanNumber.filter { it.isDigit() }
        if (digitsOnly.length >= 10) {
            val uniqueDigits = digitsOnly.toSet().size
            if (uniqueDigits <= 2) {
                return SpamReport(
                    phoneNumber = phoneNumber,
                    callerName = "कंप्यूटर जनरेटेड स्पैम नंबर",
                    category = CallerCategory.SUSPECTED_FRAUD,
                    spamScorePercentage = 92,
                    reportsCount = 1890
                )
            }
        }

        // Normal unknown caller
        return SpamReport(
            phoneNumber = phoneNumber,
            callerName = "अज्ञात कॉलर",
            category = CallerCategory.UNKNOWN,
            spamScorePercentage = 15,
            reportsCount = 0
        )
    }

    /**
     * Handle incoming call: check if spam and auto-block if enabled.
     * Returns Pair(isSpam, report)
     */
    fun handleIncomingCallCheck(
        context: Context,
        phoneNumber: String
    ): Pair<Boolean, SpamReport> {
        val report = analyzeCaller(phoneNumber)
        val isSpam = report.spamScorePercentage >= 70
        val isAutoBlockEnabled = VoicePreferences.isSpamBlockEnabled(context)

        if (isSpam && isAutoBlockEnabled) {
            try {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    telecomManager?.endCall()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to auto-end spam call", e)
            }

            val blockedReport = report.copy(wasBlocked = true)
            _blockedSpamLogs.value = listOf(blockedReport) + _blockedSpamLogs.value
            return Pair(true, blockedReport)
        }

        return Pair(isSpam, report)
    }

    fun addNumberToBlacklist(number: String) {
        val clean = number.replace(Regex("[^0-9+]"), "")
        if (clean.isNotBlank()) {
            userCustomBlacklist.add(clean)
            val newReport = SpamReport(
                phoneNumber = number,
                callerName = "ब्लॉक नंबर",
                category = CallerCategory.SUSPECTED_FRAUD,
                spamScorePercentage = 100,
                reportsCount = 1,
                wasBlocked = true
            )
            _blockedSpamLogs.value = listOf(newReport) + _blockedSpamLogs.value
        }
    }

    fun removeNumberFromBlacklist(number: String) {
        val clean = number.replace(Regex("[^0-9+]"), "")
        userCustomBlacklist.remove(clean)
        _blockedSpamLogs.value = _blockedSpamLogs.value.filter { it.phoneNumber != number }
    }
}
