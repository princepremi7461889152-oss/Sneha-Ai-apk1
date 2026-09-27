package com.example.engine

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.local.VoicePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WhatsAppAutoReplyLog(
    val id: Long = System.currentTimeMillis(),
    val senderName: String,
    val incomingMessage: String,
    val repliedText: String,
    val time: String,
    val isCall: Boolean = false
)

object WhatsAppManager {

    private const val TAG = "WhatsAppManager"

    private val _replyLogs = MutableStateFlow<List<WhatsAppAutoReplyLog>>(
        listOf(
            WhatsAppAutoReplyLog(
                senderName = "रोहित शर्मा",
                incomingMessage = "भाई नोट्स भेज दे जल्दी",
                repliedText = "नमस्ते! मैं मास्टर की AI असिस्टेंट स्नेहा बोल रही हूँ। मास्टर अभी क्लास में हैं, जल्द ही आपको रिप्लाई करेंगे। 🙏",
                time = "आज, 10:45 AM"
            ),
            WhatsAppAutoReplyLog(
                senderName = "मम्मी ❤️",
                incomingMessage = "घर कब आओगे बेटा?",
                repliedText = "मास्टर अभी व्यस्त हैं, जल्द ही कॉल बैक करेंगे।",
                time = "आज, 09:30 AM"
            )
        )
    )
    val replyLogs: StateFlow<List<WhatsAppAutoReplyLog>> = _replyLogs.asStateFlow()

    private val _lastWhatsAppCallAlert = MutableStateFlow<String?>(null)
    val lastWhatsAppCallAlert: StateFlow<String?> = _lastWhatsAppCallAlert.asStateFlow()

    /**
     * Inspect incoming notification from NotificationListenerService
     * Checks if it's WhatsApp message or call and performs auto-reply or voice announcement
     */
    fun processNotification(
        context: Context,
        sbn: StatusBarNotification,
        onCallAlert: (String) -> Unit
    ) {
        val pkg = sbn.packageName ?: return
        if (!pkg.contains("com.whatsapp", ignoreCase = true)) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""

        if (title.isBlank() && text.isBlank()) return

        // 1. Detect WhatsApp Call
        val isCallNotification = sbn.notification.category == Notification.CATEGORY_CALL ||
                text.contains("incoming voice call", ignoreCase = true) ||
                text.contains("incoming video call", ignoreCase = true) ||
                text.contains("कॉल आ रही है", ignoreCase = true) ||
                text.contains("व्हॉट्सऐप कॉल", ignoreCase = true)

        if (isCallNotification) {
            val caller = if (title.isNotBlank()) title else "अज्ञात व्यक्ति"
            val alertMessage = "मास्टर, व्हाट्सएप पर $caller की कॉल आ रही है!"
            _lastWhatsAppCallAlert.value = alertMessage
            onCallAlert(alertMessage)
            return
        }

        // 2. WhatsApp Message Auto-Reply
        if (!VoicePreferences.isWhatsAppAutoReplyEnabled(context)) return

        // Prevent infinite loop if the notification text looks like our own reply
        if (text.startsWith("नमस्ते! मैं मास्टर") || text.contains("AI असिस्टेंट")) return

        // Check if smart class mode is currently active
        val replyText = if (ClassTimetableManager.isCurrentClassActive(context)) {
            val activeClass = ClassTimetableManager.getActiveClass(context)
            if (activeClass != null) {
                "नमस्ते! मैं मास्टर की AI असिस्टेंट बोल रही हूँ। मास्टर अभी ${activeClass.subject} की क्लास में हैं (${activeClass.startTime} से ${activeClass.endTime})। क्लास के बाद संपर्क करेंगे।"
            } else {
                VoicePreferences.getWhatsAppReplyText(context)
            }
        } else {
            VoicePreferences.getWhatsAppReplyText(context)
        }

        // Attempt Quick Reply via RemoteInput
        val repliedSuccessfully = sendQuickReplyViaRemoteInput(sbn.notification, replyText, context)

        // Log the auto reply
        val now = java.time.LocalTime.now()
        val timeStr = "${now.hour}:${String.format("%02d", now.minute)}"
        val logEntry = WhatsAppAutoReplyLog(
            senderName = if (title.isNotBlank()) title else "WhatsApp User",
            incomingMessage = text,
            repliedText = replyText,
            time = "अभी, $timeStr"
        )
        _replyLogs.value = listOf(logEntry) + _replyLogs.value
        Log.i(TAG, "WhatsApp auto-reply processed. Success=$repliedSuccessfully to $title")
    }

    /**
     * Extracts RemoteInput from Notification actions and fires the PendingIntent
     */
    private fun sendQuickReplyViaRemoteInput(
        notification: Notification,
        replyMessage: String,
        context: Context
    ): Boolean {
        val actions = notification.actions ?: return false
        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            for (remoteInput in remoteInputs) {
                val bundle = Bundle().apply {
                    putCharSequence(remoteInput.resultKey, replyMessage)
                }
                val intent = Intent()
                RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)
                try {
                    action.actionIntent.send(context, 0, intent)
                    return true
                } catch (e: Exception) {
                    Log.w(TAG, "RemoteInput send exception", e)
                }
            }
        }
        return false
    }

    fun clearLogs() {
        _replyLogs.value = emptyList()
    }
}
