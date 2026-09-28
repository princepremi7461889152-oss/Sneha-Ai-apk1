package com.example.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CallSummaryItem(
    val id: Long = System.currentTimeMillis(),
    val callerName: String,
    val callerNumber: String,
    val durationText: String,
    val category: String, // "कॉलेज / पढ़ाई", "ऑफिस / काम", "दोस्त / परिवार", "सामान्य"
    val timestamp: String,
    val summaryPoints: List<String>,
    val actionItems: List<String>,
    val urgencyLevel: String, // "उच्च (High) 🔴", "मध्यम (Medium) 🟡", "सामान्य (Low) 🟢"
    val rawSnippet: String,
    var isActionCompleted: Boolean = false
)

object CallSummaryManager {

    private const val TAG = "CallSummaryManager"

    private val _summaries = MutableStateFlow<List<CallSummaryItem>>(emptyList())
    val summaries: StateFlow<List<CallSummaryItem>> = _summaries.asStateFlow()

    fun toggleActionItem(id: Long) {
        _summaries.value = _summaries.value.map {
            if (it.id == id) it.copy(isActionCompleted = !it.isActionCompleted) else it
        }
    }

    fun deleteSummary(id: Long) {
        _summaries.value = _summaries.value.filter { it.id != id }
    }

    /**
     * Creates summary from caller transcript using AI Cloud Connector (Gemini or ChatGPT or Claude)
     */
    suspend fun createSummaryFromTranscript(
        context: Context,
        callerName: String,
        callerNumber: String,
        category: String,
        transcript: String
    ): CallSummaryItem = withContext(Dispatchers.IO) {
        val nowStr = SimpleDateFormat("आज, hh:mm a", Locale.getDefault()).format(Date())

        val prompt = """
            You are Sneha AI Call Summarizer. Analyze this phone conversation transcript:
            "$transcript"
            
            Return a JSON object with:
            {
              "summary": ["Point 1 in Hindi", "Point 2 in Hindi"],
              "actions": ["Task 1 to do in Hindi", "Task 2 to do in Hindi"],
              "urgency": "High or Medium or Low"
            }
        """.trimIndent()

        val aiResponse = AiCloudConnectorManager.generateResponse(context, prompt)

        var points = listOf("कॉल पर संक्षिप्त बातचीत पूरी हुई।", transcript.take(80))
        var actions = listOf("कॉल संदर्भ नोट करें")
        var urgency = "मध्यम (Medium) 🟡"

        if (aiResponse.isNotBlank()) {
            try {
                val clean = aiResponse.substringAfter("{").substringBeforeLast("}")
                val fullJson = org.json.JSONObject("{$clean}")
                val pts = fullJson.optJSONArray("summary")
                if (pts != null && pts.length() > 0) {
                    points = (0 until pts.length()).map { pts.getString(it) }
                }
                val acts = fullJson.optJSONArray("actions")
                if (acts != null && acts.length() > 0) {
                    actions = (0 until acts.length()).map { acts.getString(it) }
                }
                val urg = fullJson.optString("urgency", "Medium")
                urgency = if (urg.contains("High", ignoreCase = true)) "उच्च (High) 🔴"
                else if (urg.contains("Low", ignoreCase = true)) "सामान्य (Low) 🟢"
                else "मध्यम (Medium) 🟡"
            } catch (e: Exception) {
                Log.w(TAG, "Parsing AI summary failed, using fallback", e)
            }
        }

        val newSummary = CallSummaryItem(
            callerName = if (callerName.isNotBlank()) callerName else "अज्ञात कॉलर",
            callerNumber = if (callerNumber.isNotBlank()) callerNumber else "कॉलर",
            durationText = "1 मिनट 15 सेकंड",
            category = category,
            timestamp = nowStr,
            summaryPoints = points,
            actionItems = actions,
            urgencyLevel = urgency,
            rawSnippet = transcript
        )

        _summaries.value = listOf(newSummary) + _summaries.value
        newSummary
    }
}
