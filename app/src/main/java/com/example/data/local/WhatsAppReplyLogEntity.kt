package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "whatsapp_reply_logs")
data class WhatsAppReplyLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderName: String,
    val incomingMessage: String,
    val repliedText: String,
    val timeStr: String,
    val isCall: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
