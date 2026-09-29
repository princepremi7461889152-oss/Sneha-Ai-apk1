package com.example.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object OfflineVoiceManager {

    private const val PREFS_NAME = "sneha_offline_prefs"
    private const val KEY_FORCE_OFFLINE = "force_offline_mode"

    private val _isOfflineForced = MutableStateFlow(false)
    val isOfflineForced: StateFlow<Boolean> = _isOfflineForced.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isOfflineForced.value = prefs.getBoolean(KEY_FORCE_OFFLINE, false)
    }

    fun setForceOffline(context: Context, enabled: Boolean) {
        _isOfflineForced.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_FORCE_OFFLINE, enabled).apply()
    }

    /**
     * Checks if device is currently offline or offline mode is forced by user
     */
    fun isOfflineActive(context: Context): Boolean {
        init(context)
        if (_isOfflineForced.value) return true

        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return true
            val capabilities = cm.getNetworkCapabilities(network) ?: return true
            !(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Executes offline command using zero-internet local components.
     * Returns spoken response string or null if not an offline-supported command.
     */
    fun processOfflineCommand(context: Context, rawInput: String): String? {
        val lower = rawInput.lowercase(Locale.ROOT).trim()

        // 1. Dialect / Language switch offline
        RegionalLanguageManager.checkVoiceLanguageSwitch(context, rawInput)?.let { (_, reply) ->
            return reply
        }

        // 2. Torch / Flashlight (100% Offline)
        if (lower.contains("टॉर्च") || lower.contains("torch") || lower.contains("फ्लैशलाइट") || lower.contains("flashlight") || lower.contains("जरावा") || lower.contains("जराउ")) {
            val isOff = lower.contains("बंद") || lower.contains("off") || lower.contains("बुतावा") || lower.contains("बन्ह")
            PhoneControlManager.toggleTorch(context, !isOff)
            return RegionalLanguageManager.localizeTorchResponse(context, !isOff)
        }

        // 3. Time, Date, Day (100% Offline)
        if (lower.contains("समय") || lower.contains("टाइम") || lower.contains("time") || lower.contains("बजे") ||
            lower.contains("तारीख") || lower.contains("date") || lower.contains("दिन") || lower.contains("भेल") || lower.contains("भईल")
        ) {
            val now = Calendar.getInstance()
            val timeFormat = SimpleDateFormat("hh:mm a", Locale("hi", "IN"))
            val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("hi", "IN"))
            return "मास्टर, अभी समय ${timeFormat.format(now.time)} हुआ है और आज ${dateFormat.format(now.time)} है।"
        }

        // 4. Battery Level (100% Offline)
        if (lower.contains("बैटरी") || lower.contains("battery") || lower.contains("चार्ज") || lower.contains("charge")) {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val pct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            return if (pct >= 0) {
                "मास्टर, फोन की बैटरी $pct% बची हुई है।"
            } else {
                "मास्टर, बैटरी का स्तर सामान्य है।"
            }
        }

        // 5. Volume Adjustments (100% Offline)
        if (lower.contains("वॉल्यूम") || lower.contains("आवाज") || lower.contains("volume") || lower.contains("sound")) {
            if (lower.contains("बढ़ाओ") || lower.contains("up") || lower.contains("तेज") || lower.contains("increase")) {
                PhoneControlManager.adjustVolume(context, true)
                return "मास्टर, आवाज बढ़ा दी गई है।"
            }
            if (lower.contains("घटाओ") || lower.contains("down") || lower.contains("धीमी") || lower.contains("कम") || lower.contains("decrease")) {
                PhoneControlManager.adjustVolume(context, false)
                return "मास्टर, आवाज कम कर दी गई है।"
            }
            if (lower.contains("म्यूट") || lower.contains("शांत") || lower.contains("silent") || lower.contains("mute")) {
                PhoneControlManager.muteVolume(context)
                return "मास्टर, फोन म्यूट कर दिया गया है।"
            }
        }

        // 6. Direct Phone Call (100% Offline via Telephony Intent)
        if ((lower.contains("कॉल") || lower.contains("call") || lower.contains("फोन") || lower.contains("डायल") || lower.contains("लगावा")) &&
            (lower.contains("करो") || lower.contains("लगाओ") || lower.contains("मिलाओ") || lower.contains("लगावा") || lower.startsWith("call "))
        ) {
            val target = rawInput
                .replace("कॉल", "", ignoreCase = true)
                .replace("call", "", ignoreCase = true)
                .replace("फोन", "")
                .replace("करो", "")
                .replace("लगाओ", "")
                .replace("लगावा", "")
                .replace("को", "")
                .replace("मिलाओ", "")
                .trim()
            val (_, msg) = WhatsAppManager.makeDirectPhoneCall(context, target)
            return msg
        }

        // 7. Voice Math & Currency (100% Offline on-device math)
        VoiceMathConverter.processMathOrConversion(rawInput)?.let { mathResp ->
            return mathResp
        }

        // 8. Voice Notes & Quick Reminders (100% Offline JSON storage)
        if (VoiceNotesManager.isNoteQuery(lower)) {
            return VoiceNotesManager.processNoteVoiceCommand(context, rawInput)
        }

        // 9. Emergency SOS (Offline SMS + GPS coordinates)
        if (EmergencySosManager.isEmergencySosQuery(lower)) {
            val (_, msg) = EmergencySosManager.triggerEmergencySos(context)
            return msg
        }

        // 10. Open Local Installed Apps (Camera, Calculator, Settings, Clock)
        if (lower.contains("खोलो") || lower.contains("ओपन") || lower.contains("open")) {
            if (lower.contains("कैमरा") || lower.contains("camera")) {
                PhoneControlManager.openAppByName(context, "camera")
                return "मास्टर, ऑफलाइन कैमरा खोला जा रहा है।"
            }
            if (lower.contains("कैलकुलेटर") || lower.contains("calculator")) {
                PhoneControlManager.openAppByName(context, "calculator")
                return "मास्टर, कैलकुलेटर खोला जा रहा है।"
            }
            if (lower.contains("घड़ी") || lower.contains("अलार्म") || lower.contains("clock") || lower.contains("alarm")) {
                PhoneControlManager.openAppByName(context, "clock")
                return "मास्टर, घड़ी और अलार्म खोला जा रहा है।"
            }
            if (lower.contains("सेटिंग") || lower.contains("settings")) {
                PhoneControlManager.openAppByName(context, "settings")
                return "मास्टर, फोन की सेटिंग्स खोली जा रही हैं।"
            }
        }

        // 11. Anti-Theft Siren Stop
        if (lower.contains("सायरन") && (lower.contains("बंद") || lower.contains("stop"))) {
            AntiTheftManager.stopAlarm(context)
            return "मास्टर, इमरजेंसी सायरन तुरंत बंद कर दिया गया है।"
        }

        // If offline and unrecognized query
        return "मास्टर, वर्तमान में इंटरनेट उपलब्ध नहीं है (ऑफलाइन मोड)। आप बिना इंटरनेट के टॉर्च, फोन कॉल, समय, बैटरी, वॉल्यूम, अलार्म, कैलकुलेटर, नोट्स और इमरजेंसी SOS का उपयोग कर सकते हैं।"
    }
}
