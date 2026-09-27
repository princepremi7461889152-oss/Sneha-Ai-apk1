package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_logs")
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val packageName: String,
    val appName: String,
    val originalText: String,
    val safeText: String,
    val containsOtp: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
