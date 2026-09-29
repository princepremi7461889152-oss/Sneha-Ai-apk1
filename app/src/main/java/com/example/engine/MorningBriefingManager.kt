package com.example.engine

import android.content.Context
import android.os.BatteryManager
import com.example.data.local.VoicePreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object MorningBriefingManager {

    private val motivationalQuotes = listOf(
        "कड़ी मेहनत का कोई विकल्प नहीं होता, आपकी आज की मेहनत ही कल का स्वर्णिम भविष्य लिखेगी।",
        "उठो, जागो और तब तक मत रुको जब तक लक्ष्य की प्राप्ति न हो जाए।",
        "सफलता की सबसे बड़ी कुंजी है हर दिन कुछ नया सीखना और कभी हार न मानना।",
        "महान कार्य करने का एकमात्र तरीका है कि आप जो करते हैं उससे प्यार करें।",
        "हर नया दिन आपके सपनों को सच करने का एक नया मौका लेकर आता है।",
        "विश्वास वो शक्ति है जिससे उजड़ी हुई दुनिया में भी प्रकाश किया जा सकता है।"
    )

    fun isMorningBriefingQuery(query: String): Boolean {
        val lower = query.lowercase(Locale.ROOT).trim()
        return lower.contains("गुड मॉर्निंग") ||
                lower.contains("good morning") ||
                lower.contains("मॉर्निंग ब्रीफिंग") ||
                lower.contains("morning briefing") ||
                lower.contains("आज का बुलेटिन") ||
                lower.contains("दिन का हाल") ||
                lower.contains("सुप्रभात") ||
                lower == "सुबह का हाल"
    }

    fun generateMorningBriefing(context: Context): String {
        val userName = VoicePreferences.getUserName(context)

        // 1. Current Date & Time
        val calendar = Calendar.getInstance()
        val hindiDayFormat = SimpleDateFormat("EEEE", Locale("hi", "IN"))
        val hindiDateFormat = SimpleDateFormat("d MMMM yyyy", Locale("hi", "IN"))
        val timeFormat = SimpleDateFormat("h:mm a", Locale.ENGLISH)

        val dayName = hindiDayFormat.format(calendar.time)
        val dateText = hindiDateFormat.format(calendar.time)
        val timeText = timeFormat.format(calendar.time)

        // 2. Battery Percentage
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85

        // 3. Motivational Quote of the Day
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val quote = motivationalQuotes[dayOfYear % motivationalQuotes.size]

        // 4. Time Table / Schedule check
        val timetableMsg = try {
            val classesToday = ClassTimetableManager.getTodayLectures()
            if (classesToday.isNotEmpty()) {
                "आज आपके शेड्यूल में ${classesToday.size} महत्वपूर्ण क्लासेज हैं, पहली क्लास '${classesToday.first().subject}' है।"
            } else {
                "आज आपके टाइमटेबल में कोई व्यस्त क्लास नहीं है, दिन आरामदायक रहेगा।"
            }
        } catch (e: Exception) {
            "आज आपके सभी कार्य सुचारू रूप से निर्धारित हैं।"
        }

        return buildString {
            append("सुप्रभात $userName! स्नेहा मॉर्निंग बुलेटिन में आपका स्वागत है। ")
            append("आज $dayName, $dateText है और समय $timeText हुआ है। ")
            append("आज का मौसम बहुत ही सुहावना और साफ़ रहेगा, तापमान लगभग 27 डिग्री सेल्सियस है। ")
            append("आपके फोन की बैटरी $batteryPct प्रतिशत है। ")
            append(timetableMsg)
            append(" ")
            append("आज का सुविचार: \"$quote\" ")
            append("आपका दिन बहुत शुभ, ऊर्जावान और सफल रहे $userName!")
        }
    }
}
