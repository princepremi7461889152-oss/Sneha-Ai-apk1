package com.example.engine

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.local.VoicePreferences

object EmergencySosManager {

    private const val TAG = "EmergencySosManager"
    private const val PREF_KEY_EMERGENCY_PHONE = "emergency_contact_phone"
    private const val PREF_KEY_EMERGENCY_NAME = "emergency_contact_name"

    fun isEmergencySosQuery(query: String): Boolean {
        val lower = query.lowercase().trim()
        return lower.contains("बचाओ") ||
                lower.contains("हेल्प मी") ||
                lower.contains("help me") ||
                lower.contains("इमरजेंसी") ||
                lower.contains("emergency") ||
                lower.contains("sos") ||
                lower.contains("लोकेशन भेजो") ||
                lower.contains("send location")
    }

    fun getEmergencyContactPhone(context: Context): String {
        val prefs = context.getSharedPreferences("sneha_sos_prefs", Context.MODE_PRIVATE)
        return prefs.getString(PREF_KEY_EMERGENCY_PHONE, "112") ?: "112"
    }

    fun getEmergencyContactName(context: Context): String {
        val prefs = context.getSharedPreferences("sneha_sos_prefs", Context.MODE_PRIVATE)
        return prefs.getString(PREF_KEY_EMERGENCY_NAME, "आपातकालीन सेवा (112)") ?: "आपातकालीन सेवा (112)"
    }

    fun saveEmergencyContact(context: Context, name: String, phone: String) {
        val prefs = context.getSharedPreferences("sneha_sos_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString(PREF_KEY_EMERGENCY_NAME, name)
            .putString(PREF_KEY_EMERGENCY_PHONE, phone)
            .apply()
    }

    @SuppressLint("MissingPermission")
    fun triggerEmergencySos(context: Context): Pair<Boolean, String> {
        val userName = VoicePreferences.getUserName(context)
        val emergencyPhone = getEmergencyContactPhone(context)
        val emergencyName = getEmergencyContactName(context)

        // 1. Get GPS or Network coordinates
        var loc: Location? = null
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

            if (lm != null && (hasFine || hasCoarse)) {
                loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Location fetch failed", e)
        }

        val mapUrl = if (loc != null) {
            "https://maps.google.com/?q=${loc.latitude},${loc.longitude}"
        } else {
            "Google Maps लाइव लोकेशन उपलब्ध नहीं है"
        }

        val sosMessage = "⚠️ आपातकालीन SOS अलर्ट! $userName मुसीबत में हैं और सहायता की आवश्यकता है। वर्तमान लोकेशन: $mapUrl - स्नेहा AI सुरक्षा गार्ड"

        // 2. Send SMS if permission granted
        val hasSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

        return if (hasSmsPermission && emergencyPhone.isNotBlank()) {
            try {
                @Suppress("DEPRECATION")
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(emergencyPhone, null, sosMessage, null, null)
                Pair(true, "मास्टर $userName! घबराएं नहीं, आपकी आपातकालीन स्थिति और लाइव लोकेशन $emergencyName ($emergencyPhone) को SMS द्वारा तुरंत भेज दी गई है!")
            } catch (e: Exception) {
                fallbackSmsIntent(context, emergencyPhone, sosMessage)
                Pair(true, "मास्टर $userName! आपातकालीन SMS तैयार कर दिया गया है, कृपया तुरंत सेंड बटन दबाएं!")
            }
        } else {
            fallbackSmsIntent(context, emergencyPhone, sosMessage)
            Pair(true, "मास्टर $userName! आपातकालीन SMS तैयार कर दिया गया है, कृपया तुरंत सेंड करें!")
        }
    }

    private fun fallbackSmsIntent(context: Context, phone: String, message: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phone")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (ignored: Exception) {}
    }
}
