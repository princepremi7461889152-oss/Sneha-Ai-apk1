package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.local.CallTranslationEntity
import com.example.data.local.ClassLectureEntity
import com.example.data.local.EmergencyContactEntity
import com.example.data.local.SnehaDatabase
import com.example.data.local.WhatsAppReplyLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SnehaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Global crash guard to prevent app force-closes ("kick" out)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("SnehaCrashGuard", "Intercepted uncaught exception in thread ${thread.name}", throwable)
            try {
                // If it's a non-fatal service/coroutine exception, log without killing process
                if (throwable !is OutOfMemoryError) {
                    android.util.Log.w("SnehaCrashGuard", "Safely recovered from exception: ${throwable.message}")
                    return@setDefaultUncaughtExceptionHandler
                }
            } catch (ignored: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }

        com.example.engine.ContextValHolder.appContext = applicationContext
        createChannels()
        seedEmergencyContacts()
        seedTimetableLectures()
        seedWhatsAppLogs()
        seedCallTranslations()
        com.example.engine.BackgroundSpeaker.initialize(this)
        com.example.engine.AiCloudConnectorManager.init(this)
        com.example.engine.AntiTheftManager.init(this)
        com.example.engine.ClassTimetableManager.init(this)
        com.example.engine.WhatsAppManager.init(this)
        com.example.engine.CallTranslatorManager.init(this)
        com.example.engine.EarphoneControlManager.init(this)
        com.example.engine.RegionalLanguageManager.init(this)
        com.example.engine.OfflineVoiceManager.init(this)
        com.example.engine.SmartHomeManager.init(this)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val emergencyChannel = NotificationChannel(
                EMERGENCY_CHANNEL_ID,
                "Sneha Emergency & Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "स्नेहा इमरजेंसी और सुरक्षा अलर्ट चैनल"
                enableVibration(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(emergencyChannel)
        }
    }

    private fun seedEmergencyContacts() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = SnehaDatabase.getInstance(applicationContext)
            val count = db.emergencyContactDao().getCount()
            if (count == 0) {
                db.emergencyContactDao().insertContact(
                    EmergencyContactEntity(
                        name = "राष्ट्रीय आपातकालीन सेवा (National Emergency)",
                        phoneNumber = "112",
                        relationship = "Police / Ambulance / Fire",
                        isPrimary = true
                    )
                )
                db.emergencyContactDao().insertContact(
                    EmergencyContactEntity(
                        name = "महिला हेल्पलाइन (Women Helpline)",
                        phoneNumber = "1091",
                        relationship = "Emergency Support",
                        isPrimary = false
                    )
                )
            }
        }
    }

    private fun seedTimetableLectures() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = SnehaDatabase.getInstance(applicationContext)
            val count = db.classLectureDao().getCount()
            if (count == 0) {
                val seedList = listOf(
                    // MONDAY (matching Screenshot 3)
                    ClassLectureEntity("mon_1", "MONDAY", "डेटा स्ट्रक्चर्स व एल्गोरिदम", "डॉ. शर्मा", "कमरा 204", "09:00", "10:30"),
                    ClassLectureEntity("mon_2", "MONDAY", "कंप्यूटर नेटवर्क", "प्रो. गुप्ता", "लैब 3", "11:00", "12:30"),
                    ClassLectureEntity("mon_3", "MONDAY", "ऑपरेटिंग सिस्टम", "डॉ. वर्मा", "कमरा 105", "14:00", "15:30"),

                    // TUESDAY
                    ClassLectureEntity("tue_1", "TUESDAY", "डेटाबेस मैनेजमेंट सिस्टम (DBMS)", "प्रो. मिश्रा", "लैब 2", "09:30", "11:00"),
                    ClassLectureEntity("tue_2", "TUESDAY", "सॉफ्टवेयर इंजीनियरिंग", "डॉ. सिंह", "कमरा 201", "11:30", "13:00"),
                    ClassLectureEntity("tue_3", "TUESDAY", "कंप्यूटर आर्किटेक्चर", "प्रो. देशपांडे", "कमरा 102", "14:00", "15:30"),

                    // WEDNESDAY
                    ClassLectureEntity("wed_1", "WEDNESDAY", "वेब डेवलपमेंट व क्लाउड", "प्रो. अग्रवाल", "लैब 4", "09:00", "10:30"),
                    ClassLectureEntity("wed_2", "WEDNESDAY", "साइबर सुरक्षा व क्रिप्टोग्राफी", "डॉ. कपूर", "कमरा 302", "11:00", "12:30"),

                    // THURSDAY
                    ClassLectureEntity("thu_1", "THURSDAY", "आर्टिफिशियल इंटेलिजेंस व मशीन लर्निंग", "डॉ. राव", "लैब 1", "10:00", "11:30"),
                    ClassLectureEntity("thu_2", "THURSDAY", "थ्योरी ऑफ कंप्यूटेशन", "प्रो. मेहता", "कमरा 204", "12:00", "13:30"),

                    // FRIDAY
                    ClassLectureEntity("fri_1", "FRIDAY", "मोबाइल ऐप डेवलपमेंट", "प्रो. पटेल", "लैब 5", "09:00", "10:30"),
                    ClassLectureEntity("fri_2", "FRIDAY", "कंपाइलर डिजाइन", "डॉ. त्रिवेदी", "कमरा 108", "11:00", "12:30"),

                    // SATURDAY
                    ClassLectureEntity("sat_1", "SATURDAY", "प्रोजेक्ट लैब व सेमिनार", "एचओडी डॉ. शर्मा", "कॉन्फ्रेंस हॉल", "10:00", "12:00")
                )
                db.classLectureDao().insertLectures(seedList)
            }
        }
    }

    private fun seedWhatsAppLogs() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = SnehaDatabase.getInstance(applicationContext)
            val count = db.whatsAppReplyLogDao().getCount()
            if (count == 0) {
                val seedLogs = listOf(
                    WhatsAppReplyLogEntity(
                        senderName = "विकास वर्मा",
                        incomingMessage = "भाई क्या हाल है? फ्री है क्या?",
                        repliedText = "नमस्ते! मैं मास्टर की AI असिस्टेंट स्नेहा बोल रही हूँ। मास्टर अभी व्यस्त हैं, जल्द ही आपको रिप्लाई करेंगे। 🙏",
                        timeStr = "आज, 10:45 AM",
                        isCall = false,
                        timestamp = System.currentTimeMillis() - 3600000
                    ),
                    WhatsAppReplyLogEntity(
                        senderName = "रोहित शर्मा (कॉलेज)",
                        incomingMessage = "कल के डेटा स्ट्रक्चर्स असाइनमेंट के नोट्स भेज देना भाई।",
                        repliedText = "मास्टर अभी क्लास में हैं, बाद में रिप्लाई करेंगे।",
                        timeStr = "आज, 09:15 AM",
                        isCall = false,
                        timestamp = System.currentTimeMillis() - 7200000
                    ),
                    WhatsAppReplyLogEntity(
                        senderName = "अंकुर जैन",
                        incomingMessage = "प्रोजेक्ट की मीटिंग कितने बजे है?",
                        repliedText = "नमस्ते! मैं मास्टर की AI असिस्टेंट बोल रही हूँ। मास्टर अभी क्लास में हैं (09:00 से 10:30), क्लास के बाद संपर्क करेंगे।",
                        timeStr = "आज, 09:05 AM",
                        isCall = false,
                        timestamp = System.currentTimeMillis() - 8000000
                    ),
                    WhatsAppReplyLogEntity(
                        senderName = "मम्मी ❤️",
                        incomingMessage = "बेटा कॉलेज पहुँच गए?",
                        repliedText = "नमस्ते! मैं मास्टर की AI असिस्टेंट स्नेहा बोल रही हूँ। मास्टर अभी क्लास में हैं, जल्द कॉल बैक करेंगे।",
                        timeStr = "कल, 01:20 PM",
                        isCall = false,
                        timestamp = System.currentTimeMillis() - 86400000
                    )
                )
                db.whatsAppReplyLogDao().insertLogs(seedLogs)
            }
        }
    }

    private fun seedCallTranslations() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = SnehaDatabase.getInstance(applicationContext)
            val count = db.callTranslationDao().getCount()
            if (count == 0) {
                val seedTrans = listOf(
                    CallTranslationEntity(
                        originalText = "Hello, where are you right now?",
                        translatedText = "नमस्ते, आप अभी कहाँ हैं?",
                        sourceLang = "en",
                        targetLang = "hi",
                        isUserSpeaking = false,
                        timestamp = System.currentTimeMillis() - 120000
                    ),
                    CallTranslationEntity(
                        originalText = "मैं अभी कॉलेज की लाइब्रेरी में हूँ।",
                        translatedText = "I am currently in the college library.",
                        sourceLang = "hi",
                        targetLang = "en",
                        isUserSpeaking = true,
                        timestamp = System.currentTimeMillis() - 80000
                    ),
                    CallTranslationEntity(
                        originalText = "Okay, can you send the assignment file by 5 PM?",
                        translatedText = "ठीक है, क्या आप शाम 5 बजे तक असाइनमेंट फाइल भेज सकते हैं?",
                        sourceLang = "en",
                        targetLang = "hi",
                        isUserSpeaking = false,
                        timestamp = System.currentTimeMillis() - 40000
                    ),
                    CallTranslationEntity(
                        originalText = "हाँ, मैं अभी कमरे पर जाकर तुरंत भेजता हूँ।",
                        translatedText = "Yes, I will go to my room and send it right away.",
                        sourceLang = "hi",
                        targetLang = "en",
                        isUserSpeaking = true,
                        timestamp = System.currentTimeMillis() - 10000
                    )
                )
                db.callTranslationDao().insertTranslations(seedTrans)
            }
        }
    }

    companion object {
        const val EMERGENCY_CHANNEL_ID = "sneha_emergency_channel"
        lateinit var instance: SnehaApplication
            private set
    }
}
