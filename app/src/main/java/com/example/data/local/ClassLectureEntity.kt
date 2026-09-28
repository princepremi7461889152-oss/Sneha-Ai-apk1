package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "class_lectures")
data class ClassLectureEntity(
    @PrimaryKey
    val id: String,
    val dayOfWeek: String, // "MONDAY", "TUESDAY", etc.
    val subject: String,
    val professor: String,
    val room: String,
    val startTime: String, // "09:00"
    val endTime: String,   // "10:30"
    val timestamp: Long = System.currentTimeMillis()
)
