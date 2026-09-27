package com.example.service

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.local.NotificationLogEntity
import com.example.data.local.SnehaDatabase
import com.example.engine.BackgroundSpeaker
import com.example.engine.ClassTimetableManager
import com.example.engine.LockScreenHelper
import com.example.engine.OtpSafetyGuard
import com.example.engine.WhatsAppManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SnehaNotificationListener : NotificationListenerService() {

    private val tag = "SnehaNotificationListener"
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkgName = sbn.packageName ?: return
        // Ignore self-notifications from Sneha
        if (pkgName == packageName) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        val fullMessage = if (title.isNotBlank() && text.isNotBlank()) "$title: $text" else (title + text)
        val hasOtp = OtpSafetyGuard.containsOtp(fullMessage)
        val safeText = OtpSafetyGuard.redactForDisplay(fullMessage)

        val appName = try {
            val appInfo = packageManager.getApplicationInfo(pkgName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            pkgName
        }

        val senderName = if (title.isNotBlank()) title else appName

        // Check Smart Class Mode
        ClassTimetableManager.applyClassSilenceIfActive(applicationContext)

        // Process WhatsApp Call & Message Auto-Reply
        if (pkgName.contains("whatsapp", ignoreCase = true)) {
            WhatsAppManager.processNotification(applicationContext, sbn) { alertMsg ->
                LockScreenHelper.wakeUpScreen(applicationContext, 4000L)
                BackgroundSpeaker.speak(alertMsg)
            }
        }

        // Log to Room Database
        scope.launch {
            try {
                val db = SnehaDatabase.getInstance(applicationContext)
                db.notificationLogDao().insertLog(
                    NotificationLogEntity(
                        sender = senderName,
                        packageName = pkgName,
                        appName = appName,
                        originalText = fullMessage,
                        safeText = safeText,
                        containsOtp = hasOtp
                    )
                )
            } catch (e: Exception) {
                Log.e(tag, "Failed to log notification", e)
            }
        }

        // Auto-Read Aloud if enabled (even when locked in background)
        if (BackgroundSpeaker.isAutoReadMessagesEnabled) {
            // Check if it's a communication or message app
            val isMessageApp = isMessagingApp(pkgName, fullMessage)
            if (isMessageApp) {
                LockScreenHelper.wakeUpScreen(applicationContext, 5000L)
                val messageToRead = if (text.isNotBlank()) text else fullMessage
                val speech = OtpSafetyGuard.sanitizeForSpeech(senderName, messageToRead)
                BackgroundSpeaker.speak(speech)
            }
        }
    }

    private fun isMessagingApp(pkgName: String, text: String): Boolean {
        val lowerPkg = pkgName.lowercase()
        val messagingKeywords = listOf(
            "sms", "mms", "message", "whatsapp", "telegram", "signal",
            "viber", "messenger", "chat", "inbox", "mail", "gmail", "outlook"
        )
        if (messagingKeywords.any { lowerPkg.contains(it) }) return true

        // If content contains common message indicators or OTP
        if (OtpSafetyGuard.containsOtp(text)) return true

        return false
    }

    companion object {
        fun isNotificationServiceEnabled(context: Context): Boolean {
            val pkgName = context.packageName
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return flat != null && flat.contains(pkgName)
        }

        fun openNotificationAccessSettings(context: Context) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
