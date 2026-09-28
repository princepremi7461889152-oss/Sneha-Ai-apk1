package com.example.engine

import android.app.Activity
import android.app.KeyguardManager
import android.app.SearchManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import com.example.data.model.InstalledApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

object PhoneControlManager {

    private var isTorchOn = false
    private var alarmTrack: AudioTrack? = null
    private var isAlarmPlaying = false
    private var strobeJob: Job? = null

    private val _isHotspotActive = MutableStateFlow(false)
    val isHotspotActive: StateFlow<Boolean> = _isHotspotActive.asStateFlow()

    private val _isBluetoothActive = MutableStateFlow(false)
    val isBluetoothActive: StateFlow<Boolean> = _isBluetoothActive.asStateFlow()

    private val _isWifiActive = MutableStateFlow(false)
    val isWifiActive: StateFlow<Boolean> = _isWifiActive.asStateFlow()

    /**
     * Lists all launchable apps on the device.
     */
    fun getInstalledApps(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val list = mutableListOf<InstalledApp>()

        for (resolveInfo in resolveInfos) {
            val appName = resolveInfo.loadLabel(pm).toString()
            val packageName = resolveInfo.activityInfo.packageName
            val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val icon = resolveInfo.loadIcon(pm)
            list.add(InstalledApp(name = appName, packageName = packageName, isSystemApp = isSystem, icon = icon))
        }

        return list.sortedBy { it.name.lowercase() }
    }

    /**
     * Finds and opens an app matching voice input.
     */
    fun openAppByName(context: Context, appQuery: String): Pair<Boolean, String> {
        val cleanQuery = appQuery.lowercase().trim()
            .replace("खोलो", "")
            .replace("open", "")
            .replace("app", "")
            .replace("ऐप", "")
            .trim()

        val apps = getInstalledApps(context)
        
        // Exact or strong contains match
        val matchedApp = apps.find { it.name.lowercase() == cleanQuery }
            ?: apps.find { it.name.lowercase().contains(cleanQuery) }
            ?: findKnownAppAlias(cleanQuery, apps)

        if (matchedApp != null) {
            val launched = launchApp(context, matchedApp.packageName)
            return if (launched) {
                Pair(true, "${matchedApp.name} खोला जा रहा है...")
            } else {
                Pair(false, "${matchedApp.name} को खोलने में समस्या हुई।")
            }
        }

        // Direct fallback intents for common system apps
        val systemFallback = handleSystemAppShortcuts(context, cleanQuery)
        if (systemFallback != null) {
            return systemFallback
        }

        return Pair(false, "माफ़ कीजिये, मुझे फोन में '$appQuery' ऐप नहीं मिला।")
    }

    private fun findKnownAppAlias(query: String, apps: List<InstalledApp>): InstalledApp? {
        val aliases = mapOf(
            "whatsapp" to listOf("whatsapp", "व्हाट्सएप", "वाट्सएप"),
            "youtube" to listOf("youtube", "yt", "यूट्यूब"),
            "chrome" to listOf("chrome", "browser", "इंटरनेट", "गूगल क्रोम"),
            "camera" to listOf("camera", "कैमरा"),
            "gallery" to listOf("gallery", "photos", "गैलरी", "फोटो"),
            "settings" to listOf("settings", "सेटिंग्स"),
            "instagram" to listOf("instagram", "insta", "इंस्टाग्राम"),
            "facebook" to listOf("facebook", "fb", "फेसबुक"),
            "calculator" to listOf("calculator", "कैलकुलेटर"),
            "maps" to listOf("maps", "गूगल मैप्स", "नेविगेशन")
        )

        for ((pkgKeyword, aliasList) in aliases) {
            if (aliasList.any { query.contains(it) }) {
                val found = apps.find { it.packageName.lowercase().contains(pkgKeyword) || it.name.lowercase().contains(pkgKeyword) }
                if (found != null) return found
            }
        }
        return null
    }

    private fun handleSystemAppShortcuts(context: Context, query: String): Pair<Boolean, String>? {
        return try {
            when {
                query.contains("setting") || query.contains("सेटिंग") -> {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    Pair(true, "सेटिंग्स खोली जा रही है।")
                }
                query.contains("camera") || query.contains("कैमरा") -> {
                    context.startActivity(Intent("android.media.action.IMAGE_CAPTURE").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    Pair(true, "कैमरा खोला जा रहा है।")
                }
                query.contains("call") || query.contains("phone") || query.contains("डायल") -> {
                    context.startActivity(Intent(Intent.ACTION_DIAL).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    Pair(true, "फोन डायलर खोला जा रहा है।")
                }
                query.contains("wifi") || query.contains("वाईफाई") -> {
                    context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    Pair(true, "वाईफाई सेटिंग्स खोली जा रही है।")
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun launchApp(context: Context, packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName) ?: return false
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun searchWeb(context: Context, query: String) {
        try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        }
    }

    fun searchYouTube(context: Context, query: String) {
        try {
            val appIntent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(appIntent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    fun searchPlayStore(context: Context, query: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    fun searchMaps(context: Context, query: String) {
        try {
            val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        }
    }

    fun searchWhatsApp(context: Context, query: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("whatsapp://send?text=${Uri.encode(query)}")
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            launchApp(context, "com.whatsapp")
        }
    }

    fun searchSpotify(context: Context, query: String) {
        try {
            val intent = Intent("android.media.action.MEDIA_PLAY_FROM_SEARCH").apply {
                putExtra(SearchManager.QUERY, query)
                setPackage("com.spotify.music")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            searchYouTube(context, query)
        }
    }

    fun searchAmazon(context: Context, query: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.amazon.in/s?k=${Uri.encode(query)}")).apply {
                setPackage("in.amazon.mShop.android.shopping")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.amazon.in/s?k=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    fun searchFlipkart(context: Context, query: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.flipkart.com/search?q=${Uri.encode(query)}")).apply {
                setPackage("com.flipkart.android")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.flipkart.com/search?q=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    fun searchTwitter(context: Context, query: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("twitter://search?query=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/search?q=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    fun searchInstagram(context: Context, query: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/explore/tags/${Uri.encode(query)}/")).apply {
                setPackage("com.instagram.android")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            launchApp(context, "com.instagram.android")
        }
    }

    /**
     * Universal In-App Search: Launches specified application and triggers
     * a search within it.
     */
    fun searchInsideApp(context: Context, appQuery: String, searchQuery: String): Pair<Boolean, String> {
        val lowerApp = appQuery.lowercase().trim()

        when {
            lowerApp.contains("youtube") || lowerApp.contains("यूट्यूब") -> {
                searchYouTube(context, searchQuery)
                return Pair(true, "यूट्यूब में '$searchQuery' खोजा जा रहा है।")
            }
            lowerApp.contains("whatsapp") || lowerApp.contains("व्हाट्सएप") || lowerApp.contains("वाट्सएप") -> {
                searchWhatsApp(context, searchQuery)
                return Pair(true, "व्हाट्सएप पर '$searchQuery' खोला जा रहा है।")
            }
            lowerApp.contains("maps") || lowerApp.contains("मैप्स") || lowerApp.contains("नेविगेशन") -> {
                searchMaps(context, searchQuery)
                return Pair(true, "गूगल मैप्स पर '$searchQuery' सर्च किया जा रहा है।")
            }
            lowerApp.contains("spotify") || lowerApp.contains("स्पॉटिफाई") || lowerApp.contains("music") || lowerApp.contains("म्यूजिक") -> {
                searchSpotify(context, searchQuery)
                return Pair(true, "स्पॉटिफाई पर '$searchQuery' खोजा जा रहा है।")
            }
            lowerApp.contains("play store") || lowerApp.contains("प्ले स्टोर") || lowerApp.contains("playstore") -> {
                searchPlayStore(context, searchQuery)
                return Pair(true, "प्ले स्टोर में '$searchQuery' खोजा जा रहा है।")
            }
            lowerApp.contains("amazon") || lowerApp.contains("अमेज़न") || lowerApp.contains("अमेजन") -> {
                searchAmazon(context, searchQuery)
                return Pair(true, "अमेज़न पर '$searchQuery' खोजा जा रहा है।")
            }
            lowerApp.contains("flipkart") || lowerApp.contains("फ्लिपकार्ट") -> {
                searchFlipkart(context, searchQuery)
                return Pair(true, "फ्लिपकार्ट पर '$searchQuery' खोजा जा रहा है।")
            }
            lowerApp.contains("twitter") || lowerApp.contains("ट्विटर") || lowerApp == "x" -> {
                searchTwitter(context, searchQuery)
                return Pair(true, "ट्विटर पर '$searchQuery' खोजा जा रहा है।")
            }
            lowerApp.contains("instagram") || lowerApp.contains("इंस्टाग्राम") || lowerApp.contains("इन्स्टा") -> {
                searchInstagram(context, searchQuery)
                return Pair(true, "इंस्टाग्राम पर '$searchQuery' खोजा जा रहा है।")
            }
            lowerApp.contains("google") || lowerApp.contains("गूगल") || lowerApp.contains("chrome") || lowerApp.contains("क्रोम") -> {
                searchWeb(context, searchQuery)
                return Pair(true, "गूगल पर '$searchQuery' खोजा जा रहा है।")
            }
        }

        // Generic installed app matching
        val apps = getInstalledApps(context)
        val matchedApp = apps.find { it.name.lowercase().contains(lowerApp) }
            ?: apps.find { it.packageName.lowercase().contains(lowerApp) }

        if (matchedApp != null) {
            try {
                // Try ACTION_SEARCH inside the app
                val searchIntent = Intent(Intent.ACTION_SEARCH).apply {
                    setPackage(matchedApp.packageName)
                    putExtra(SearchManager.QUERY, searchQuery)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(searchIntent)
                return Pair(true, "${matchedApp.name} में '$searchQuery' सर्च किया जा रहा है।")
            } catch (e: Exception) {
                // Fallback: Launch app
                launchApp(context, matchedApp.packageName)
                return Pair(true, "${matchedApp.name} खोला गया है ('$searchQuery' सर्च करने के लिए)।")
            }
        }

        // Global fallback to web search with app context
        searchWeb(context, "$appQuery $searchQuery")
        return Pair(true, "$appQuery के लिए '$searchQuery' वेब पर खोजा जा रहा है।")
    }

    fun toggleTorch(context: Context, enable: Boolean): Pair<Boolean, String> {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            if (cameraManager == null) return Pair(false, "कैमरा फ्लैश उपलब्ध नहीं है।")

            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cameraManager.cameraIdList.firstOrNull()

            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, enable)
                isTorchOn = enable
                val msg = if (enable) "फ्लैशलाइट ऑन कर दी गई है।" else "फ्लैशलाइट बंद कर दी गई है।"
                Pair(true, msg)
            } else {
                Pair(false, "टॉर्च हार्डवेयर नहीं मिला।")
            }
        } catch (e: CameraAccessException) {
            Pair(false, "टॉर्च एरर: ${e.message}")
        } catch (e: Exception) {
            Pair(false, "फ्लैशलाइट नियंत्रित नहीं हो सकी।")
        }
    }

    fun isTorchActive(): Boolean = isTorchOn

    fun adjustVolume(context: Context, isIncrease: Boolean): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return "ऑडियो सर्विस अनुपलब्ध है।"
        val direction = if (isIncrease) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return if (isIncrease) "वॉल्यूम बढ़ाया गया ($current/$max)" else "वॉल्यूम कम किया गया ($current/$max)"
    }

    fun muteVolume(context: Context): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return "ऑडियो सर्विस अनुपलब्ध है।"
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
        return "फोन म्यूट कर दिया गया है।"
    }

    /**
     * Bluetooth Control
     */
    fun isBluetoothEnabled(context: Context): Boolean {
        return try {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bm?.adapter ?: BluetoothAdapter.getDefaultAdapter()
            val state = adapter?.isEnabled == true
            _isBluetoothActive.value = state
            state
        } catch (e: Exception) {
            _isBluetoothActive.value
        }
    }

    fun toggleBluetooth(context: Context, enable: Boolean): Pair<Boolean, String> {
        return try {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bm?.adapter ?: BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) {
                return Pair(false, "डिवाइस में ब्लूटूथ हार्डवेयर उपलब्ध नहीं है।")
            }

            _isBluetoothActive.value = enable

            if (enable) {
                if (adapter.isEnabled) {
                    return Pair(true, "ब्लूटूथ पहले से ही ऑन है।")
                }
                @Suppress("DEPRECATION")
                val success = try { adapter.enable() } catch (e: Exception) { false }
                if (!success) {
                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        openBluetoothSettings(context)
                    }
                }
                Pair(true, "ब्लूटूथ ऑन कर दिया गया है।")
            } else {
                if (!adapter.isEnabled) {
                    return Pair(true, "ब्लूटूथ पहले से ही बंद है।")
                }
                @Suppress("DEPRECATION")
                val success = try { adapter.disable() } catch (e: Exception) { false }
                if (!success) {
                    openBluetoothSettings(context)
                }
                Pair(true, "ब्लूटूथ बंद कर दिया गया है।")
            }
        } catch (e: Exception) {
            openBluetoothSettings(context)
            Pair(true, "ब्लूटूथ सेटिंग्स खोली गई हैं।")
        }
    }

    fun openBluetoothSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    /**
     * Wi-Fi Control
     */
    fun isWifiEnabled(context: Context): Boolean {
        return try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val state = wm?.isWifiEnabled == true
            _isWifiActive.value = state
            state
        } catch (e: Exception) {
            _isWifiActive.value
        }
    }

    fun toggleWifi(context: Context, enable: Boolean): Pair<Boolean, String> {
        return try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            _isWifiActive.value = enable

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val panelIntent = Intent(Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(panelIntent)
                } catch (e: Exception) {
                    openWifiSettings(context)
                }
                val msg = if (enable) "वाईफाई चालू करने के लिए पैनल खोला गया है।" else "वाईफाई बंद करने के लिए पैनल खोला गया है।"
                Pair(true, msg)
            } else {
                @Suppress("DEPRECATION")
                wm?.isWifiEnabled = enable
                val msg = if (enable) "वाईफाई ऑन कर दिया गया है।" else "वाईफाई बंद कर दिया गया है।"
                Pair(true, msg)
            }
        } catch (e: Exception) {
            openWifiSettings(context)
            Pair(true, "वाईफाई सेटिंग्स खोली गई हैं।")
        }
    }

    fun openWifiSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    /**
     * Hotspot Control
     */
    fun toggleHotspot(context: Context, enable: Boolean): Pair<Boolean, String> {
        _isHotspotActive.value = enable
        return try {
            openHotspotSettings(context)
            val actionText = if (enable) "हॉटस्पॉट चालू (ON)" else "हॉटस्पॉट बंद (OFF)"
            Pair(true, "मास्टर, $actionText करने के लिए पोर्टेबल हॉटस्पॉट सेटिंग्स खोली गई है।")
        } catch (e: Exception) {
            Pair(false, "हॉटस्पॉट सेटिंग्स खोलने में समस्या हुई।")
        }
    }

    fun openHotspotSettings(context: Context) {
        try {
            val intent = Intent().apply {
                action = "android.settings.TETHER_SETTINGS"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
    }

    fun openMobileDataSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    fun getBatteryInfo(context: Context): String {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val isCharging = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val status = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        } else false

        return if (batteryPct >= 0) {
            val chargingText = if (isCharging) " (चार्जिंग चालू है ⚡)" else ""
            "आपकी बैटरी $batteryPct प्रतिशत है$chargingText।"
        } else {
            "बैटरी जानकारी प्राप्त नहीं हो सकी।"
        }
    }

    fun openDialer(context: Context, number: String = "") {
        val uri = if (number.isNotBlank()) Uri.parse("tel:${Uri.encode(number)}") else Uri.parse("tel:")
        val intent = Intent(Intent.ACTION_DIAL, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun setTimer(context: Context, minutes: Int): Pair<Boolean, String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, minutes * 60)
                putExtra(AlarmClock.EXTRA_MESSAGE, "स्नेहा टाइमर ($minutes मिनट)")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "$minutes मिनट का टाइमर सेट कर दिया गया है।")
        } catch (e: Exception) {
            Pair(false, "टाइमर सेट करने में समस्या हुई।")
        }
    }

    /**
     * Emergency Keyguard Unlock request.
     * Uses Android KeyguardManager to request dismiss of lock screen.
     */
    fun requestEmergencyUnlock(activity: Activity, onResult: (Boolean) -> Unit) {
        val km = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (km == null) {
            onResult(false)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            km.requestDismissKeyguard(activity, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    super.onDismissSucceeded()
                    onResult(true)
                }

                override fun onDismissCancelled() {
                    super.onDismissCancelled()
                    onResult(false)
                }

                override fun onDismissError() {
                    super.onDismissError()
                    onResult(false)
                }
            })
        } else {
            @Suppress("DEPRECATION")
            km.newKeyguardLock("SnehaKeyguardLock")?.disableKeyguard()
            onResult(true)
        }
    }

    /**
     * Synthesizes and plays a loud emergency SOS siren tone directly via AudioTrack.
     */
    fun startEmergencyAlarm(context: Context) {
        if (isAlarmPlaying) return
        isAlarmPlaying = true

        CoroutineScope(Dispatchers.Default).launch {
            val sampleRate = 44100
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufSize.coerceAtLeast(sampleRate * 2))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            alarmTrack = track
            track.play()

            // Oscillating dual-tone emergency police/ambulance siren (700Hz to 1200Hz)
            val buffer = ShortArray(sampleRate / 4)
            var phase = 0.0

            while (isActive && isAlarmPlaying) {
                for (cycle in 0..15) {
                    if (!isActive || !isAlarmPlaying) break
                    val freq = 700.0 + (500.0 * (cycle / 15.0))
                    for (i in buffer.indices) {
                        phase += 2.0 * Math.PI * freq / sampleRate
                        buffer[i] = (sin(phase) * 32767).toInt().toShort()
                    }
                    track.write(buffer, 0, buffer.size)
                }
                for (cycle in 15 downTo 0) {
                    if (!isActive || !isAlarmPlaying) break
                    val freq = 700.0 + (500.0 * (cycle / 15.0))
                    for (i in buffer.indices) {
                        phase += 2.0 * Math.PI * freq / sampleRate
                        buffer[i] = (sin(phase) * 32767).toInt().toShort()
                    }
                    track.write(buffer, 0, buffer.size)
                }
            }

            try {
                track.stop()
                track.release()
            } catch (ignored: Exception) {}
        }
    }

    fun stopEmergencyAlarm() {
        isAlarmPlaying = false
        try {
            alarmTrack?.stop()
            alarmTrack?.release()
            alarmTrack = null
        } catch (ignored: Exception) {}
    }

    fun isEmergencyAlarmActive(): Boolean = isAlarmPlaying

    /**
     * Rapid strobe flashlight for emergency visibility.
     */
    fun startEmergencyStrobe(context: Context, scope: CoroutineScope) {
        stopEmergencyStrobe(context)
        strobeJob = scope.launch(Dispatchers.Default) {
            var state = false
            while (isActive) {
                state = !state
                toggleTorch(context, state)
                delay(180)
            }
            toggleTorch(context, false)
        }
    }

    fun stopEmergencyStrobe(context: Context) {
        strobeJob?.cancel()
        strobeJob = null
        toggleTorch(context, false)
    }

    fun isStrobeActive(): Boolean = strobeJob?.isActive == true
}
