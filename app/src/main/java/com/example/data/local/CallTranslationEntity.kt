package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_translations")
data class CallTranslationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val isUserSpeaking: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
