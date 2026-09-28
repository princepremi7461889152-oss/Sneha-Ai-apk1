package com.example.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.engine.BackgroundSpeaker
import com.example.engine.CallAssistantManager
import com.example.engine.LockScreenHelper

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        try {
            val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
            val rawNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            Log.d("CallReceiver", "Phone state changed: $stateStr for rawNumber=$rawNumber")

            when (stateStr) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    // Resolve contact name from device contacts in real-time
                    val (resolvedName, cleanNumber) = resolveRealContactName(context, rawNumber)

                    // 1. Check AI Spam Detection & Auto-Block
                    val (isSpam, spamReport) = com.example.engine.SpamCallerManager.handleIncomingCallCheck(context, cleanNumber)
                    if (isSpam && spamReport.wasBlocked) {
                        BackgroundSpeaker.speak("चेतावनी! स्पैम कॉल ब्लॉक कर दिया गया: ${spamReport.callerName}")
                        return
                    }

                    // 2. Check if Class Mode is active to customize auto-reply
                    if (com.example.engine.ClassTimetableManager.isCurrentClassActive(context)) {
                        val activeClass = com.example.engine.ClassTimetableManager.getActiveClass(context)
                        if (activeClass != null) {
                            CallAssistantManager.setDefaultMessage(
                                "नमस्ते, मास्टर अभी ${activeClass.subject} की क्लास में हैं (${activeClass.startTime} से ${activeClass.endTime})। क्लास के बाद संपर्क करें।"
                            )
                        }
                    }

                    val callerDisplayName = if (isSpam) {
                        "⚠️ स्पैम: ${spamReport.callerName} ($cleanNumber)"
                    } else if (resolvedName != null) {
                        "$resolvedName ($cleanNumber)"
                    } else if (cleanNumber.isNotBlank() && cleanNumber != "अज्ञात नंबर") {
                        cleanNumber
                    } else {
                        "इनकमिंग कॉल"
                    }

                    CallAssistantManager.setRealIncomingCall(callerDisplayName)
                    LockScreenHelper.wakeUpScreen(context, 8000L)
                    
                    if (CallAssistantManager.autoAttendantEnabled.value) {
                        val announcement = if (isSpam) {
                            "सावधान मास्टर! स्पैम कॉल आ रही है: ${spamReport.callerName}!"
                        } else if (resolvedName != null) {
                            "मास्टर, $resolvedName का फोन आ रहा है!"
                        } else if (cleanNumber.isNotBlank() && cleanNumber != "अज्ञात नंबर") {
                            "मास्टर, $cleanNumber से फोन आ रहा है!"
                        } else {
                            "मास्टर, फोन आ रहा है!"
                        }
                        BackgroundSpeaker.speak(announcement)
                    }
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    CallAssistantManager.dismissIncomingCallPrompt()
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    // Call is now active or answered
                }
            }
        } catch (e: Exception) {
            Log.e("CallReceiver", "Error processing phone state", e)
        }
    }

    private fun resolveRealContactName(context: Context, rawNumber: String?): Pair<String?, String> {
        if (rawNumber.isNullOrBlank()) {
            return Pair(null, "अज्ञात नंबर")
        }
        val cleanNumber = rawNumber.trim()
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(cleanNumber))
                context.contentResolver.query(
                    uri,
                    arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                        if (nameIdx != -1) {
                            val name = cursor.getString(nameIdx)
                            if (!name.isNullOrBlank()) {
                                return Pair(name, cleanNumber)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("CallReceiver", "Error looking up contact name", e)
        }
        return Pair(null, cleanNumber)
    }
}
