package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.local.NotificationLogEntity
import com.example.data.local.SnehaDatabase
import com.example.engine.BackgroundSpeaker
import com.example.engine.LockScreenHelper
import com.example.engine.OtpSafetyGuard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val sender = messages[0].displayOriginatingAddress ?: "अज्ञात प्रेषक"
            val bodyBuilder = StringBuilder()
            for (msg in messages) {
                bodyBuilder.append(msg.displayMessageBody)
            }
            val fullBody = bodyBuilder.toString()
            val hasOtp = OtpSafetyGuard.containsOtp(fullBody)
            val safeDisplay = OtpSafetyGuard.redactForDisplay(fullBody)

            Log.d("SmsReceiver", "SMS received from $sender (hasOtp=$hasOtp)")

            // Save to database
            scope.launch {
                try {
                    val db = SnehaDatabase.getInstance(context)
                    db.notificationLogDao().insertLog(
                        NotificationLogEntity(
                            sender = sender,
                            packageName = "com.android.mms",
                            appName = "SMS संदेश",
                            originalText = fullBody,
                            safeText = safeDisplay,
                            containsOtp = hasOtp
                        )
                    )
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Error saving SMS log", e)
                }
            }

            // Speak aloud in background if Auto-Read is enabled (even when locked)
            if (BackgroundSpeaker.isAutoReadMessagesEnabled) {
                LockScreenHelper.wakeUpScreen(context, 5000L)
                val speechText = OtpSafetyGuard.sanitizeForSpeech(sender, fullBody)
                BackgroundSpeaker.speak(speechText)
            }
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error handling incoming SMS", e)
        }
    }
}
