package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.example.engine.BackgroundSpeaker
import com.example.engine.CallAssistantManager
import com.example.engine.LockScreenHelper

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        try {
            val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: "अज्ञात नंबर"

            Log.d("CallReceiver", "Phone state changed: $stateStr for $incomingNumber")

            when (stateStr) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    // 1. Check AI Truecaller Spam Detection & Auto-Block
                    val (isSpam, spamReport) = com.example.engine.SpamCallerManager.handleIncomingCallCheck(context, incomingNumber)
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
                        "⚠️ स्पैम: ${spamReport.callerName} ($incomingNumber)"
                    } else {
                        "इनकमिंग कॉल ($incomingNumber)"
                    }

                    CallAssistantManager.triggerSimulatedIncomingCall(callerDisplayName)
                    LockScreenHelper.wakeUpScreen(context, 8000L)
                    
                    if (CallAssistantManager.autoAttendantEnabled.value) {
                        val announcement = if (isSpam) {
                            "सावधान मास्टर! स्पैम कॉल आ रही है: ${spamReport.callerName}!"
                        } else {
                            "मास्टर, $incomingNumber से फोन आ रहा है!"
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
}
