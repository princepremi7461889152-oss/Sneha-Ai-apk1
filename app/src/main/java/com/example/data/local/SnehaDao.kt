package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}

@Dao
interface EmergencyContactDao {
    @Query("SELECT * FROM emergency_contacts ORDER BY id ASC")
    fun getAllContacts(): Flow<List<EmergencyContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContactEntity): Long

    @Query("DELETE FROM emergency_contacts WHERE id = :id")
    suspend fun deleteContact(id: Long)

    @Query("SELECT COUNT(*) FROM emergency_contacts")
    suspend fun getCount(): Int
}

@Dao
interface NotificationLogDao {
    @Query("SELECT * FROM notification_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentNotifications(): Flow<List<NotificationLogEntity>>

    @Query("SELECT * FROM notification_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentNotificationList(limit: Int = 10): List<NotificationLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: NotificationLogEntity): Long

    @Query("DELETE FROM notification_logs")
    suspend fun clearLogs()
}

@Dao
interface ClassLectureDao {
    @Query("SELECT * FROM class_lectures ORDER BY startTime ASC")
    fun getAllLectures(): Flow<List<ClassLectureEntity>>

    @Query("SELECT * FROM class_lectures ORDER BY startTime ASC")
    suspend fun getAllLecturesList(): List<ClassLectureEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLecture(lecture: ClassLectureEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLectures(lectures: List<ClassLectureEntity>)

    @Query("DELETE FROM class_lectures WHERE id = :id")
    suspend fun deleteLecture(id: String)

    @Query("SELECT COUNT(*) FROM class_lectures")
    suspend fun getCount(): Int

    @Query("DELETE FROM class_lectures")
    suspend fun clearAll()
}

@Dao
interface WhatsAppReplyLogDao {
    @Query("SELECT * FROM whatsapp_reply_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<WhatsAppReplyLogEntity>>

    @Query("SELECT * FROM whatsapp_reply_logs ORDER BY timestamp DESC LIMIT 100")
    suspend fun getAllLogsList(): List<WhatsAppReplyLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WhatsAppReplyLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<WhatsAppReplyLogEntity>)

    @Query("DELETE FROM whatsapp_reply_logs WHERE id = :id")
    suspend fun deleteLog(id: Long)

    @Query("SELECT COUNT(*) FROM whatsapp_reply_logs")
    suspend fun getCount(): Int

    @Query("DELETE FROM whatsapp_reply_logs")
    suspend fun clearAll()
}

@Dao
interface CallTranslationDao {
    @Query("SELECT * FROM call_translations ORDER BY timestamp DESC")
    fun getAllTranslations(): Flow<List<CallTranslationEntity>>

    @Query("SELECT * FROM call_translations ORDER BY timestamp DESC LIMIT 50")
    suspend fun getAllTranslationsList(): List<CallTranslationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslation(translation: CallTranslationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslations(translations: List<CallTranslationEntity>)

    @Query("DELETE FROM call_translations WHERE id = :id")
    suspend fun deleteTranslation(id: Long)

    @Query("SELECT COUNT(*) FROM call_translations")
    suspend fun getCount(): Int

    @Query("DELETE FROM call_translations")
    suspend fun clearAll()
}

