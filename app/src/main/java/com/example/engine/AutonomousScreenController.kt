package com.example.engine

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import com.example.service.SnehaAccessibilityService
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale

object AutonomousScreenController {

    /**
     * Checks if the user command is an autonomous screen / OS control action.
     * Returns a spoken confirmation if handled, or null otherwise.
     */
    suspend fun handleAutonomousCommand(context: Context, rawQuery: String): String? {
        val query = rawQuery.lowercase(Locale.ROOT).trim()
        val service = SnehaAccessibilityService.instance

        // 1. Home / Back / Recents / Notifications navigation
        if (query == "home" || query.contains("home जाओ") || query.contains("होम जाओ") || query == "go home") {
            if (service != null && service.goHome()) {
                return "होम स्क्रीन पर आ गए, मास्टर!"
            } else {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return "होम स्क्रीन खोल दी गई है, मास्टर!"
            }
        }

        if (query == "back" || query.contains("back जाओ") || query.contains("बैक जाओ") || query.contains("पीछे जाओ")) {
            return if (service != null && service.goBack()) {
                "वापस बैक हो गए, मास्टर!"
            } else {
                "मास्टर, स्क्रीन बैक करने के लिए कृपया एक्सेसिबिलिटी सेवा चालू करें।"
            }
        }

        if (query.contains("recent") || query.contains("रीसेंट ऐप्स") || query.contains("हाल के ऐप्स") || query.contains("recent apps")) {
            return if (service != null && service.openRecents()) {
                "रीसेंट ऐप्स खोल दिए गए हैं, मास्टर!"
            } else {
                "मास्टर, रीसेंट ऐप्स खोलने के लिए एक्सेसिबिलिटी सेवा चालू करें।"
            }
        }

        if (query.contains("notification panel") || query.contains("नोटिफिकेशन खोलो") || query.contains("नोटिफिकेशन दिखाओ")) {
            return if (service != null && service.openNotifications()) {
                "नोटिफिकेशन पैनल खोल दिया गया है, मास्टर!"
            } else {
                "नोटिफिकेशन देखने के लिए एक्सेसिबिलिटी सक्षम करें।"
            }
        }

        // 2. Screenshot
        if (query.contains("screenshot") || query.contains("स्क्रीनशॉट") || query.contains("स्क्रीन शॉट")) {
            return if (service != null && service.captureScreenshot()) {
                "स्क्रीनशॉट ले लिया गया है, मास्टर!"
            } else {
                "स्क्रीनशॉट लेने के लिए कृपया स्नेहा एक्सेसिबिलिटी सेवा की अनुमति दें।"
            }
        }

        // 3. Screen Reading (Screen का content पढ़कर समझना)
        if (query.contains("स्क्रीन पढ़ो") || query.contains("स्क्रीन पढ़कर बताओ") || query.contains("read screen") || query.contains("स्क्रीन पर क्या लिखा है") || query.contains("screen read")) {
            if (service == null) {
                return "मास्टर, स्क्रीन पढ़ने के लिए सेटिंग्स से स्नेहा एक्सेसिबिलिटी सेवा सक्रिय करें।"
            }
            val text = service.readScreenContent()
            return "मास्टर, स्क्रीन पर यह सामग्री दिख रही है: $text"
        }

        // 4. Gestures: Swipe & Scroll (Swipe up/down/left/right)
        if (query.contains("नीचे स्क्रॉल") || query.contains("scroll down") || query.contains("नीचे जाओ")) {
            if (service != null) {
                service.scrollDown()
                return "स्क्रीन नीचे स्क्रॉल कर दी गई है।"
            }
            return "स्क्रॉल करने के लिए एक्सेसिबिलिटी चालू करें।"
        }

        if (query.contains("ऊपर स्क्रॉल") || query.contains("scroll up") || query.contains("ऊपर जाओ")) {
            if (service != null) {
                service.scrollUp()
                return "स्क्रीन ऊपर स्क्रॉल कर दी गई है।"
            }
            return "स्क्रॉल करने के लिए एक्सेसिबिलिटी चालू करें।"
        }

        if (query.contains("दाएं स्वाइप") || query.contains("swipe right") || query.contains("right swipe")) {
            if (service != null) {
                service.swipeRight()
                return "दाएं स्वाइप किया गया।"
            }
            return "स्वाइप करने के लिए एक्सेसिबिलिटी चालू करें।"
        }

        if (query.contains("बाएं स्वाइप") || query.contains("swipe left") || query.contains("left swipe")) {
            if (service != null) {
                service.swipeLeft()
                return "बाएं स्वाइप किया गया।"
            }
            return "स्वाइप करने के लिए एक्सेसिबिलिटी चालू करें।"
        }

        // 5. Visual Context Buttons ("नीचे वाला button दबाओ", "ऊपर वाला button दबाओ", "पहला बटन", "बीच वाला")
        val isPositionalButton = (query.contains("बटन") || query.contains("button")) &&
                (query.contains("दबाओ") || query.contains("क्लिक") || query.contains("press") || query.contains("click")) &&
                (query.contains("नीचे") || query.contains("ऊपर") || query.contains("पहला") || query.contains("आखिरी") || query.contains("बीच"))
        if (isPositionalButton) {
            if (service != null) {
                val success = service.clickButtonByPosition(query)
                return if (success) "बटन दबा दिया गया है, मास्टर!" else "स्क्रीन पर संबंधित बटन नहीं मिला।"
            }
            return "बटन दबाने के लिए एक्सेसिबिलिटी चालू करें।"
        }

        // 6. Tap / Click by text label ("Search पर tap करो", "Next पर क्लिक करो", "Submit दबाओ")
        val isClickCommand = (query.contains("पर टैप करो") || query.contains("पर क्लिक करो") || query.contains("दबाओ") || query.startsWith("tap ") || query.startsWith("click ")) &&
                !query.contains("फोटो") && !query.contains("कॉल")
        if (isClickCommand) {
            val target = query
                .replace("पर टैप करो", "")
                .replace("पर क्लिक करो", "")
                .replace("दबाओ", "")
                .replace("tap on", "")
                .replace("click on", "")
                .replace("tap", "")
                .replace("click", "")
                .trim()
            if (target.isNotBlank()) {
                if (service != null) {
                    val clicked = service.clickByText(target)
                    return if (clicked) "'$target' पर क्लिक कर दिया गया है!" else "मास्टर, स्क्रीन पर '$target' नहीं मिला।"
                }
                return "स्क्रीन पर क्लिक करने के लिए एक्सेसिबिलिटी चालू करें।"
            }
        }

        // 7. Text Typing and Deleting
        if (query.startsWith("लिखो ") || query.startsWith("टाइप करो ") || query.startsWith("type ")) {
            val textToType = rawQuery
                .replace("लिखो ", "")
                .replace("टाइप करो ", "")
                .replace("type ", "")
                .trim()
            if (textToType.isNotBlank()) {
                if (service != null) {
                    val typed = service.typeText(textToType)
                    return if (typed) "टेक्स्ट लिख दिया गया है: '$textToType'" else "मास्टर, स्क्रीन पर कोई सक्रिय इनपुट बॉक्स नहीं मिला।"
                }
                return "टेक्स्ट लिखने के लिए एक्सेसिबिलिटी चालू करें।"
            }
        }

        if (query.contains("डिलीट करो") || query.contains("टेक्स्ट हटाओ") || query.contains("क्लियर करो") || query.contains("clear text")) {
            if (service != null) {
                val cleared = service.clearText()
                return if (cleared) "टेक्स्ट डिलीट कर दिया गया है।" else "डिलीट करने के लिए कोई टेक्स्ट बॉक्स नहीं मिला।"
            }
            return "टेक्स्ट डिलीट करने के लिए एक्सेसिबिलिटी चालू करें।"
        }

        // 8. Clipboard Read / Write
        if (query.contains("clipboard") || query.contains("क्लिपबोर्ड")) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (query.contains("पढ़ो") || query.contains("read") || query.contains("सुनाओ") || query.contains("क्या है")) {
                val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                return if (!clipText.isNullOrBlank()) {
                    "क्लिपबोर्ड में यह कॉपी है: $clipText"
                } else {
                    "क्लिपबोर्ड खाली है, कोई टेक्स्ट कॉपी नहीं है।"
                }
            }
            if (query.contains("कॉपी करो") || query.contains("लिखो") || query.contains("copy")) {
                val toCopy = rawQuery.substringAfter("कॉपी करो").substringAfter("copy").trim()
                if (toCopy.isNotBlank()) {
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Sneha Copied", toCopy))
                    return "क्लिपबोर्ड में सुरक्षित कॉपी कर लिया गया है: '$toCopy'"
                }
            }
        }

        // 9. Files & Folders
        if (query.contains("file manager") || query.contains("फाइल मैनेजर") || query.contains("files खोलो") || query.contains("माई फाइल्स") || query.contains("downloads खोलो")) {
            return try {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                "फ़ाइल मैनेजर खोल दिया गया है, मास्टर!"
            } catch (e: Exception) {
                launchAppByName(context, "files") ?: "फ़ाइल मैनेजर खोलने में असमर्थ।"
            }
        }

        // 10. Camera Photo & Video
        if (query.contains("फोटो खींचो") || query.contains("take photo") || query.contains("कैमरा खोलो") || query.contains("तस्वीर लो")) {
            return try {
                val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                "कैमरा खोल दिया गया है, मास्टर! मुस्कुराइए!"
            } catch (e: Exception) {
                launchAppByName(context, "camera") ?: "कैमरा खोलने में असमर्थ।"
            }
        }

        if (query.contains("वीडियो बनाओ") || query.contains("record video") || query.contains("वीडियो रिकॉर्ड")) {
            return try {
                val intent = Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                "वीडियो कैमरा खोल दिया गया है, मास्टर!"
            } catch (e: Exception) {
                "वीडियो कैमरा खोलने में असमर्थ।"
            }
        }

        // 11. Gallery Photo Search
        if (query.contains("gallery") || query.contains("गैलरी") || query.contains("photos खोलो") || query.contains("फोटो दिखाओ")) {
            return try {
                val intent = Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                "गैलरी खोल दी गई है, मास्टर!"
            } catch (e: Exception) {
                launchAppByName(context, "gallery") ?: "गैलरी खोलने में असमर्थ।"
            }
        }

        // 12. Alarm & Timer
        if (query.contains("अलार्म") || query.contains("alarm")) {
            val hour = extractNumber(query) ?: 6
            return try {
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, 0)
                    putExtra(AlarmClock.EXTRA_MESSAGE, "स्नेहा अलार्म")
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                "मास्टर, $hour बजे का अलार्म सेट कर दिया गया है!"
            } catch (e: Exception) {
                "अलार्म सेट करने के लिए क्लॉक ऐप खोला जा रहा है।"
            }
        }

        if (query.contains("टाइमर") || query.contains("timer")) {
            val minutes = extractNumber(query) ?: 5
            val seconds = minutes * 60
            return try {
                val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                    putExtra(AlarmClock.EXTRA_MESSAGE, "स्नेहा टाइमर")
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                "मास्टर, $minutes मिनट का टाइमर शुरू कर दिया गया है!"
            } catch (e: Exception) {
                "टाइमर सेट करने में असमर्थ।"
            }
        }

        // 13. Calendar Event
        if (query.contains("कैलेंडर") || query.contains("calendar") || query.contains("मीटिंग शेड्यूल") || query.contains("इवेंट बनाओ")) {
            val title = rawQuery
                .replace("कैलेंडर में इवेंट बनाओ", "")
                .replace("कैलेंडर खोलो", "")
                .replace("मीटिंग शेड्यूल करो", "")
                .replace("इवेंट बनाओ", "")
                .trim()
                .ifBlank { "महत्वपूर्ण बैठक" }
            return try {
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                    putExtra(CalendarContract.Events.TITLE, title)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                "कैलेंडर में '$title' का इवेंट बनाने की स्क्रीन खोल दी गई है, मास्टर!"
            } catch (e: Exception) {
                "कैलेंडर खोलने में असमर्थ।"
            }
        }

        // 14. App Settings Navigation
        if (query.contains("सेटिंग्स") || query.contains("settings")) {
            val targetIntent = when {
                query.contains("wifi") || query.contains("वाईफाई") -> Intent(Settings.ACTION_WIFI_SETTINGS)
                query.contains("bluetooth") || query.contains("ब्लूटूथ") -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                query.contains("accessibility") || query.contains("एक्सेसिबिलिटी") -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                query.contains("display") || query.contains("डिस्प्ले") -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
                query.contains("sound") || query.contains("साउंड") || query.contains("आवाज") -> Intent(Settings.ACTION_SOUND_SETTINGS)
                query.contains("app") || query.contains("ऐप") -> Intent(Settings.ACTION_APPLICATION_SETTINGS)
                else -> Intent(Settings.ACTION_SETTINGS)
            }
            targetIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            return try {
                context.startActivity(targetIntent)
                "सेटिंग्स स्क्रीन खोल दी गई है, मास्टर!"
            } catch (e: Exception) {
                "सेटिंग्स खोलने में असमर्थ।"
            }
        }

        // 15. In-App Search: "YouTube में search करो [query]", "Google में search करो [query]"
        val isAppSearch = (query.contains("में search करो") || query.contains("में सर्च करो") || query.contains("पर search करो") || query.contains("पर सर्च करो"))
        if (isAppSearch) {
            val parts = query.split(Regex("में search करो|में सर्च करो|पर search करो|पर सर्च करो"))
            if (parts.size >= 2) {
                val appPart = parts[0].trim()
                val searchQuery = parts[1].trim()
                if (appPart.contains("youtube") || appPart.contains("यूट्यूब")) {
                    val intent = Intent(Intent.ACTION_SEARCH).apply {
                        setPackage("com.google.android.youtube")
                        putExtra("query", searchQuery)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    return try {
                        context.startActivity(intent)
                        "YouTube पर '$searchQuery' सर्च किया जा रहा है!"
                    } catch (e: Exception) {
                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$searchQuery")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(webIntent)
                        "YouTube पर '$searchQuery' सर्च किया गया!"
                    }
                } else if (appPart.contains("google") || appPart.contains("गूगल")) {
                    val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                        putExtra("query", searchQuery)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    return "Google पर '$searchQuery' खोजा जा रहा है!"
                }
            }
        }

        // 16. App Opening / Launching ("WhatsApp खोलो", "Instagram खोलो", "YouTube खोलो")
        if (query.endsWith("खोलो") || query.startsWith("open ") || query.endsWith("open")) {
            val appQuery = query
                .replace("खोलो", "")
                .replace("open", "")
                .replace("app", "")
                .replace("ऐप", "")
                .trim()
            if (appQuery.isNotBlank()) {
                val launchResult = launchAppByName(context, appQuery)
                if (launchResult != null) {
                    return launchResult
                }
            }
        }

        // 17. App Closing: "App बंद करो", "close app"
        if (query.contains("app बंद करो") || query.contains("ऐप बंद करो") || query.contains("close app") || query.contains("बंद करो")) {
            if (service != null && service.goBack()) {
                return "सक्रिय ऐप बंद कर दिया गया है।"
            } else {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return "ऐप बंद करके होम पर आ गए हैं।"
            }
        }

        return null
    }

    private fun launchAppByName(context: Context, name: String): String? {
        val pm = context.packageManager
        val cleanName = name.lowercase().trim()

        // Known quick packages
        val quickMap = mapOf(
            "youtube" to "com.google.android.youtube",
            "यूट्यूब" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "व्हाट्सएप" to "com.whatsapp",
            "वाट्सएप" to "com.whatsapp",
            "instagram" to "com.instagram.android",
            "इंस्टाग्राम" to "com.instagram.android",
            "chrome" to "com.android.chrome",
            "क्रोम" to "com.android.chrome",
            "camera" to "com.android.camera",
            "कैमरा" to "com.android.camera",
            "gallery" to "com.google.android.apps.photos",
            "गैलरी" to "com.google.android.apps.photos",
            "settings" to "com.android.settings",
            "सेटिंग्स" to "com.android.settings",
            "calculator" to "com.google.android.calculator",
            "कैलकुलेटर" to "com.google.android.calculator",
            "maps" to "com.google.android.apps.maps",
            "मैप्स" to "com.google.android.apps.maps",
            "gmail" to "com.google.android.gm",
            "जीमेल" to "com.google.android.gm"
        )

        quickMap[cleanName]?.let { pkg ->
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return "$name ऐप खोल दिया गया है, मास्टर!"
            }
        }

        // Search through all installed apps
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(mainIntent, 0)
        for (info in apps) {
            val label = info.loadLabel(pm).toString().lowercase()
            if (label.contains(cleanName) || cleanName.contains(label)) {
                val launchIntent = pm.getLaunchIntentForPackage(info.activityInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return "${info.loadLabel(pm)} खोल दिया गया है, मास्टर!"
                }
            }
        }
        return null
    }

    private fun extractNumber(text: String): Int? {
        val regex = Regex("""\b(\d+)\b""")
        return regex.find(text)?.groupValues?.get(1)?.toIntOrNull()
    }
}
