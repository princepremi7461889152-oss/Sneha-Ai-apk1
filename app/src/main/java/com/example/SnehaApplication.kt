package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.local.EmergencyContactEntity
import com.example.data.local.SnehaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SnehaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        createChannels()
        seedEmergencyContacts()
        com.example.engine.BackgroundSpeaker.initialize(this)
        com.example.engine.AiCloudConnectorManager.init(this)
        com.example.engine.AntiTheftManager.init(this)
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

    companion object {
        const val EMERGENCY_CHANNEL_ID = "sneha_emergency_channel"
        lateinit var instance: SnehaApplication
            private set
    }
}
