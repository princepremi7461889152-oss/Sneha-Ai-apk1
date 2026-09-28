package com.example.engine

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.local.SnehaDatabase
import com.example.data.local.VoicePreferences
import com.example.data.local.WhatsAppReplyLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _replyLogs = MutableStateFlow<List<WhatsAppAutoReplyLog>>(emptyList())
    val replyLogs: StateFlow<List<WhatsAppAutoReplyLog>> = _replyLogs.asStateFlow()

    private val _lastWhatsAppCallAlert = MutableStateFlow<String?>(null)
    val lastWhatsAppCallAlert: StateFlow<String?> = _lastWhatsAppCallAlert.asStateFlow()

    fun init(context: Context) {
        scope.launch {
            try {
                val db = SnehaDatabase.getInstance(context)
                db.whatsAppReplyLogDao().getAllLogs().collect { entityList ->
                    val domainLogs = entityList.map { entity ->
                        WhatsAppAutoReplyLog(
                            id = entity.id,
                            senderName = entity.senderName,
                            incomingMessage = entity.incomingMessage,
                            repliedText = entity.repliedText,
                            time = entity.timeStr,
                            isCall = entity.isCall
                        )
                    }
                    _replyLogs.value = domainLogs
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load WhatsApp logs from Room", e)
            }
        }
    }

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
        addReplyLog(context, logEntry)
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

    /**
     * Resolves a phone number from target string, checking if it is already digits or searching device contacts.
     */
    fun resolveContactPhone(context: Context, target: String): Pair<String?, String> {
        val cleanTarget = target.trim()
        val digitOnly = cleanTarget.replace(Regex("[^0-9+]"), "")
        if (digitOnly.length >= 10) {
            val formatted = if (digitOnly.startsWith("+")) digitOnly else if (digitOnly.length == 10) "+91$digitOnly" else "+$digitOnly"
            return Pair(formatted, cleanTarget)
        }

        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
                val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
                val selectionArgs = arrayOf("%$cleanTarget%")
                context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val foundName = if (nameIdx != -1) cursor.getString(nameIdx) else cleanTarget
                        val foundNum = if (numIdx != -1) cursor.getString(numIdx) else null
                        if (!foundNum.isNullOrBlank()) {
                            val numDigits = foundNum.replace(Regex("[^0-9+]"), "")
                            val formatted = if (numDigits.startsWith("+")) numDigits else if (numDigits.length == 10) "+91$numDigits" else "+$numDigits"
                            return Pair(formatted, foundName)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Contacts query exception", e)
        }

        return Pair(null, cleanTarget)
    }

    /**
     * Sends WhatsApp message to a specific contact or phone number.
     */
    fun sendWhatsAppMessage(context: Context, target: String, message: String): Pair<Boolean, String> {
        val (phone, resolvedName) = resolveContactPhone(context, target)
        return try {
            if (phone != null) {
                val cleanPhone = phone.replace("+", "")
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallbackIntent)
                }
                Pair(true, "मास्टर, व्हाट्सएप पर $resolvedName को संदेश भेजा जा रहा है: '$message'")
            } else {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage("com.whatsapp")
                    putExtra(Intent.EXTRA_TEXT, message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "व्हाट्सएप संदेश भेजें").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
                Pair(true, "मास्टर, व्हाट्सएप खोला गया है। कृपया $target को चुनकर संदेश भेजें: '$message'")
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendWhatsAppMessage error", e)
            Pair(false, "मास्टर, व्हाट्सएप संदेश भेजने में समस्या हुई। कृपया जांचें कि व्हाट्सएप इंस्टॉल है या नहीं।")
        }
    }

    /**
     * Initiates a WhatsApp Voice Call to a contact or phone number.
     */
    fun makeWhatsAppCall(context: Context, target: String): Pair<Boolean, String> {
        val (phone, resolvedName) = resolveContactPhone(context, target)
        return try {
            var callTriggered = false
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                val cursor = context.contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(ContactsContract.Data._ID),
                    "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                    arrayOf("vnd.android.cursor.item/vnd.com.whatsapp.voip.call", "%$target%"),
                    null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val dataId = it.getLong(0)
                        val callIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(
                                Uri.parse("content://com.android.contacts/data/$dataId"),
                                "vnd.android.cursor.item/vnd.com.whatsapp.voip.call"
                            )
                            setPackage("com.whatsapp")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(callIntent)
                        callTriggered = true
                    }
                }
            }

            if (callTriggered) {
                Pair(true, "मास्टर, व्हाट्सएप पर $resolvedName को वॉयस कॉल लगाई जा रही है...")
            } else if (phone != null) {
                val cleanPhone = phone.replace("+", "")
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
                Pair(true, "मास्टर, व्हाट्सएप पर $resolvedName की चैट खोली गई है। आप सीधे कॉल आइकन दबाकर बात कर सकते हैं।")
            } else {
                val pm = context.packageManager
                val launchIntent = pm.getLaunchIntentForPackage("com.whatsapp")
                    ?: pm.getLaunchIntentForPackage("com.whatsapp.w4b")
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    Pair(true, "मास्टर, $target को कॉल करने के लिए व्हाट्सएप खोला गया है।")
                } else {
                    Pair(false, "मास्टर, डिवाइस में व्हाट्सएप नहीं मिला।")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "makeWhatsAppCall error", e)
            Pair(false, "मास्टर, व्हाट्सएप कॉल लगाने में समस्या हुई।")
        }
    }

    /**
     * Sends SMS to anyone directly or via SMS composer.
     */
    fun sendDirectSms(context: Context, target: String, message: String): Pair<Boolean, String> {
        val (phone, resolvedName) = resolveContactPhone(context, target)
        if (phone.isNullOrBlank()) {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply {
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return Pair(true, "मास्टर, संदेश भेजने के लिए मैसेज ऐप खोला गया है। कृपया $target को चुनें।")
        }

        val cleanPhone = phone.replace(" ", "").replace("-", "")

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(android.telephony.SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    android.telephony.SmsManager.getDefault()
                }
                val parts = smsManager.divideMessage(message)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(cleanPhone, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(cleanPhone, null, message, null, null)
                }
                return Pair(true, "मास्टर, $resolvedName ($cleanPhone) को एसएमएस सफलतापूर्वक भेज दिया गया है: '$message'")
            } catch (e: Exception) {
                Log.e(TAG, "Direct SMS send failed, falling back to intent", e)
            }
        }

        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$cleanPhone")).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(smsIntent)
        return Pair(true, "मास्टर, $resolvedName के लिए एसएमएस ड्राफ्ट खोल दिया गया है।")
    }

    /**
     * Makes a standard direct phone call or opens dialer.
     */
    fun makeDirectPhoneCall(context: Context, target: String): Pair<Boolean, String> {
        val (phone, resolvedName) = resolveContactPhone(context, target)
        if (phone.isNullOrBlank()) {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            return Pair(true, "मास्टर, फोन डायलर खोला गया है।")
        }

        val cleanPhone = phone.replace(" ", "").replace("-", "")
        val hasCallPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

        return try {
            if (hasCallPerm) {
                val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanPhone")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(callIntent)
                Pair(true, "मास्टर, $resolvedName ($cleanPhone) को कॉल लगाई जा रही है...")
            } else {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
                Pair(true, "मास्टर, $resolvedName का नंबर ($cleanPhone) डायलर में लगा दिया गया है।")
            }
        } catch (e: Exception) {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            Pair(true, "मास्टर, डायलर खोला गया है।")
        }
    }

    fun addReplyLog(context: Context? = null, log: WhatsAppAutoReplyLog) {
        _replyLogs.value = listOf(log) + _replyLogs.value.filter { it.id != log.id }
        if (context != null) {
            scope.launch {
                try {
                    val db = SnehaDatabase.getInstance(context)
                    db.whatsAppReplyLogDao().insertLog(
                        WhatsAppReplyLogEntity(
                            id = if (log.id > 1000000000L) 0 else log.id,
                            senderName = log.senderName,
                            incomingMessage = log.incomingMessage,
                            repliedText = log.repliedText,
                            timeStr = log.time,
                            isCall = log.isCall,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to persist WhatsApp log to Room", e)
                }
            }
        }
    }

    fun clearLogs(context: Context? = null) {
        _replyLogs.value = emptyList()
        if (context != null) {
            scope.launch {
                try {
                    val db = SnehaDatabase.getInstance(context)
                    db.whatsAppReplyLogDao().clearAll()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to clear WhatsApp logs in Room", e)
                }
            }
        }
    }
}
