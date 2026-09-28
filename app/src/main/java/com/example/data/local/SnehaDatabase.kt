package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChatMessageEntity::class,
        EmergencyContactEntity::class,
        NotificationLogEntity::class,
        ClassLectureEntity::class,
        WhatsAppReplyLogEntity::class,
        CallTranslationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SnehaDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun classLectureDao(): ClassLectureDao
    abstract fun whatsAppReplyLogDao(): WhatsAppReplyLogDao
    abstract fun callTranslationDao(): CallTranslationDao

    companion object {
        @Volatile
        private var INSTANCE: SnehaDatabase? = null

        fun getInstance(context: Context): SnehaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SnehaDatabase::class.java,
                    "sneha_voice_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
