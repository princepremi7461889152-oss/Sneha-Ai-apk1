package com.example.engine

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class DeviceType(val iconEmoji: String, val category: String) {
    LIGHT("💡", "लाइट्स"),
    FAN("🌀", "पंखा"),
    AC("❄️", "एयर कंडीशनर"),
    TV("📺", "स्मार्ट टीवी"),
    PLUG("🔌", "स्मार्ट प्लग / गीजर")
}

data class SmartDevice(
    val id: String,
    val name: String,
    val room: String,
    val type: DeviceType,
    var isOn: Boolean = false,
    var value: Int = 50, // Brightness (0-100), Speed (1-5), Temp (16-30), Volume (0-100)
    var colorName: String = "Warm White"
)

object SmartHomeManager {

    private const val PREFS_NAME = "sneha_smarthome_prefs"
    private const val KEY_DEVICES = "saved_iot_devices_json"

    private val defaultDevices = listOf(
        SmartDevice("light_living", "लिविंग रूम मुख्य लाइट", "लिविंग रूम", DeviceType.LIGHT, isOn = true, value = 85, colorName = "Cyber Cyan"),
        SmartDevice("fan_living", "लिविंग रूम पंखा", "लिविंग रूम", DeviceType.FAN, isOn = true, value = 4),
        SmartDevice("ac_living", "हॉल इन्वर्टर एसी", "लिविंग रूम", DeviceType.AC, isOn = false, value = 24),
        SmartDevice("tv_living", "सोनी स्मार्ट टीवी", "लिविंग रूम", DeviceType.TV, isOn = true, value = 22),
        SmartDevice("light_bedroom", "मास्टर बेडरूम लाइट", "बेडरूम", DeviceType.LIGHT, isOn = false, value = 60, colorName = "Warm White"),
        SmartDevice("fan_bedroom", "बेडरूम सीलिंग फैन", "बेडरूम", DeviceType.FAN, isOn = false, value = 3),
        SmartDevice("light_balcony", "बालकनी डेकोर लाइट", "बालकनी", DeviceType.LIGHT, isOn = true, value = 100, colorName = "Sunset Gold"),
        SmartDevice("geyser_bath", "बाथरूम गीजर", "किचन व बाथ", DeviceType.PLUG, isOn = false, value = 15)
    )

    private val _devices = MutableStateFlow<List<SmartDevice>>(defaultDevices)
    val devices: StateFlow<List<SmartDevice>> = _devices.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_DEVICES, null)
        if (json != null) {
            try {
                val array = JSONArray(json)
                val list = mutableListOf<SmartDevice>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        SmartDevice(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            room = obj.getString("room"),
                            type = DeviceType.valueOf(obj.getString("type")),
                            isOn = obj.getBoolean("isOn"),
                            value = obj.getInt("value"),
                            colorName = obj.optString("colorName", "Warm White")
                        )
                    )
                }
                _devices.value = list
                return
            } catch (ignored: Exception) {}
        }
        _devices.value = defaultDevices
    }

    fun toggleDevice(context: Context, deviceId: String): Boolean {
        val updated = _devices.value.map { dev ->
            if (dev.id == deviceId) {
                dev.copy(isOn = !dev.isOn)
            } else dev
        }
        _devices.value = updated
        saveDevices(context, updated)
        return updated.find { it.id == deviceId }?.isOn ?: false
    }

    fun setDevicePower(context: Context, deviceId: String, on: Boolean) {
        val updated = _devices.value.map { dev ->
            if (dev.id == deviceId) {
                dev.copy(isOn = on)
            } else dev
        }
        _devices.value = updated
        saveDevices(context, updated)
    }

    fun setDeviceValue(context: Context, deviceId: String, newValue: Int) {
        val updated = _devices.value.map { dev ->
            if (dev.id == deviceId) {
                dev.copy(value = newValue)
            } else dev
        }
        _devices.value = updated
        saveDevices(context, updated)
    }

    fun setDeviceColor(context: Context, deviceId: String, colorName: String) {
        val updated = _devices.value.map { dev ->
            if (dev.id == deviceId) {
                dev.copy(colorName = colorName, isOn = true)
            } else dev
        }
        _devices.value = updated
        saveDevices(context, updated)
    }

    fun allOff(context: Context) {
        val updated = _devices.value.map { it.copy(isOn = false) }
        _devices.value = updated
        saveDevices(context, updated)
    }

    private fun saveDevices(context: Context, list: List<SmartDevice>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        for (dev in list) {
            val obj = JSONObject().apply {
                put("id", dev.id)
                put("name", dev.name)
                put("room", dev.room)
                put("type", dev.type.name)
                put("isOn", dev.isOn)
                put("value", dev.value)
                put("colorName", dev.colorName)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_DEVICES, array.toString()).apply()
    }

    /**
     * Executes natural language voice commands for smart home devices:
     * e.g. "लाइट जलाओ", "कमरे की लाइट बंद करो", "पंखा 4 पर करो", "एसी 24 डिग्री करो", "टीवी बंद करो"
     */
    fun processSmartHomeVoiceCommand(context: Context, input: String): Pair<Boolean, String>? {
        init(context)
        val lower = input.lowercase().trim()

        val isSmartHome = lower.contains("लाइट") || lower.contains("light") ||
                lower.contains("पंखा") || lower.contains("fan") ||
                lower.contains("एसी") || lower.contains("ac") ||
                lower.contains("टीवी") || lower.contains("tv") ||
                lower.contains("गीजर") || lower.contains("स्मार्ट होम") ||
                lower.contains("स्मार्ट लाइट")

        if (!isSmartHome) return null

        val isOff = lower.contains("बंद") || lower.contains("off") || lower.contains("बुझा") || lower.contains("रोक")

        // 1. All devices off: "सब लाइट बंद करो" / "सब कुछ बंद करो"
        if (lower.contains("सब") && isOff) {
            allOff(context)
            return Pair(true, "मास्टर, घर की सभी स्मार्ट लाइट्स, पंखे और एसी बंद कर दिए गए हैं।")
        }

        // 2. Lights
        if (lower.contains("लाइट") || lower.contains("light") || lower.contains("बल्ब")) {
            val dev = _devices.value.find { it.type == DeviceType.LIGHT } ?: _devices.value.first()
            setDevicePower(context, dev.id, !isOff)
            val actionWord = if (isOff) "बंद कर दी गई है।" else "जला दी गई है।"
            return Pair(true, "मास्टर, ${dev.name} $actionWord")
        }

        // 3. Fan
        if (lower.contains("पंखा") || lower.contains("fan")) {
            val dev = _devices.value.find { it.type == DeviceType.FAN } ?: _devices.value.first()
            val speedMatch = Regex("""(?:स्पीड|speed|नंबर|पर)\s*([1-5])""").find(lower)
            if (speedMatch != null) {
                val speed = speedMatch.groupValues[1].toInt()
                setDeviceValue(context, dev.id, speed)
                setDevicePower(context, dev.id, true)
                return Pair(true, "मास्टर, ${dev.name} चालू है और स्पीड $speed पर सेट कर दी गई है।")
            }
            setDevicePower(context, dev.id, !isOff)
            val actionWord = if (isOff) "बंद कर दिया गया है।" else "चालू कर दिया गया है।"
            return Pair(true, "मास्टर, ${dev.name} $actionWord")
        }

        // 4. AC
        if (lower.contains("एसी") || lower.contains("ac") || lower.contains("एयर कंडीशनर")) {
            val dev = _devices.value.find { it.type == DeviceType.AC } ?: _devices.value.first()
            val tempMatch = Regex("""(\d{2})\s*(?:डिग्री|degree)?""").find(lower)
            if (tempMatch != null) {
                val temp = tempMatch.groupValues[1].toInt().coerceIn(16, 30)
                setDeviceValue(context, dev.id, temp)
                setDevicePower(context, dev.id, true)
                return Pair(true, "मास्टर, एसी चालू कर दिया गया है और तापमान $temp°C पर सेट कर दिया गया है।")
            }
            setDevicePower(context, dev.id, !isOff)
            val actionWord = if (isOff) "बंद कर दिया गया है।" else "24°C पर चालू कर दिया गया है।"
            return Pair(true, "मास्टर, ${dev.name} $actionWord")
        }

        // 5. TV
        if (lower.contains("टीवी") || lower.contains("tv")) {
            val dev = _devices.value.find { it.type == DeviceType.TV } ?: _devices.value.first()
            setDevicePower(context, dev.id, !isOff)
            val actionWord = if (isOff) "बंद कर दिया गया है।" else "चालू कर दिया गया है।"
            return Pair(true, "मास्टर, ${dev.name} $actionWord")
        }

        // 6. Geyser
        if (lower.contains("गीजर") || lower.contains("geyser")) {
            val dev = _devices.value.find { it.type == DeviceType.PLUG } ?: _devices.value.first()
            setDevicePower(context, dev.id, !isOff)
            val actionWord = if (isOff) "बंद कर दिया गया है।" else "चालू कर दिया गया है (15 मिनट ऑटो-ऑफ टाइमर)।"
            return Pair(true, "मास्टर, ${dev.name} $actionWord")
        }

        return null
    }
}
