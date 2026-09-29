package com.example.engine

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class VoiceNoteItem(
    val id: String,
    val text: String,
    val timestamp: Long,
    val type: String = "NOTE" // "NOTE" or "REMINDER"
) {
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd MMM, h:mm a", Locale("hi", "IN"))
        return sdf.format(Date(timestamp))
    }
}

object VoiceNotesManager {

    private const val PREFS_NAME = "sneha_voice_notes_prefs"
    private const val KEY_NOTES = "saved_voice_notes_json"

    fun getAllNotes(context: Context): List<VoiceNoteItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_NOTES, "[]") ?: "[]"
        val list = mutableListOf<VoiceNoteItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    VoiceNoteItem(
                        id = obj.getString("id"),
                        text = obj.getString("text"),
                        timestamp = obj.getLong("timestamp"),
                        type = obj.optString("type", "NOTE")
                    )
                )
            }
        } catch (ignored: Exception) {}
        return list.sortedByDescending { it.timestamp }
    }

    fun addNote(context: Context, text: String, type: String = "NOTE"): VoiceNoteItem {
        val notes = getAllNotes(context).toMutableList()
        val newItem = VoiceNoteItem(
            id = UUID.randomUUID().toString(),
            text = text.trim(),
            timestamp = System.currentTimeMillis(),
            type = type
        )
        notes.add(0, newItem)
        saveAllNotes(context, notes)
        return newItem
    }

    fun deleteNote(context: Context, id: String) {
        val notes = getAllNotes(context).filter { it.id != id }
        saveAllNotes(context, notes)
    }

    private fun saveAllNotes(context: Context, list: List<VoiceNoteItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("text", item.text)
                put("timestamp", item.timestamp)
                put("type", item.type)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_NOTES, array.toString()).apply()
    }

    fun isNoteQuery(query: String): Boolean {
        val lower = query.lowercase().trim()
        return lower.startsWith("नोट") ||
                lower.contains("नोट लिखो") ||
                lower.contains("नोट बनाओ") ||
                lower.contains("याद रखना") ||
                lower.contains("remind me") ||
                lower.contains("रिमाइंडर लगाओ") ||
                lower.contains("नोट्स सुनाओ") ||
                lower.contains("मेरे नोट्स")
    }

    fun processNoteVoiceCommand(context: Context, rawQuery: String): String {
        val lower = rawQuery.lowercase().trim()

        // Read out notes
        if (lower.contains("नोट्स सुनाओ") || lower.contains("मेरे नोट्स") || lower.contains("नोट्स दिखाओ") || lower.contains("read notes")) {
            val notes = getAllNotes(context)
            if (notes.isEmpty()) {
                return "मास्टर, अभी आपकी डायरी में कोई नोट या रिमाइंडर सेव नहीं है।"
            }
            val recent = notes.take(4)
            val joined = recent.mapIndexed { idx, n -> "${idx + 1}. ${n.text}" }.joinToString("। ")
            return "मास्टर, आपके हाल के नोट्स ये हैं: $joined।"
        }

        // Add note / reminder
        var cleanText = rawQuery
        val prefixes = listOf(
            "नोट लिखो कि", "नोट लिखो", "नोट बनाओ कि", "नोट बनाओ",
            "याद रखना कि", "याद रखना", "रिमाइंडर लगाओ कि", "रिमाइंडर लगाओ",
            "write note", "take note", "remind me that", "remind me"
        )
        for (p in prefixes) {
            if (cleanText.lowercase().startsWith(p)) {
                cleanText = cleanText.substring(p.length).trim()
                break
            }
        }

        if (cleanText.isNotBlank()) {
            val isReminder = lower.contains("याद") || lower.contains("रिमाइंडर") || lower.contains("remind")
            val type = if (isReminder) "REMINDER" else "NOTE"
            addNote(context, cleanText, type)
            val typeName = if (isReminder) "रिमाइंडर" else "नोट"
            return "मास्टर, आपका $typeName सुरक्षित रूप से सेव कर लिया गया है: \"$cleanText\"।"
        }

        return "मास्टर, आप क्या नोट या रिमाइंडर लिखवाना चाहते हैं? कृपया बोलिए।"
    }
}
