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

    private val _summaries = MutableStateFlow<List<CallSummaryItem>>(
        listOf(
            CallSummaryItem(
                id = 1L,
                callerName = "डॉ. शर्मा (एचओडी, कंप्यूटर साइंस)",
                callerNumber = "+91 98112-45678",
                durationText = "2 मिनट 45 सेकंड",
                category = "कॉलेज / पढ़ाई",
                timestamp = "आज, 10:30 AM",
                summaryPoints = listOf(
                    "फाइनल ईयर प्रोजेक्ट प्रेजेंटेशन की तारीख 15 तारीख तय हुई है।",
                    "डॉ. शर्मा ने पीपीटी (PPT) और सिनॉप्सिस की हार्ड कॉपी मांगी है।",
                    "ग्रुप के सभी 3 सदस्यों की उपस्थिति अनिवार्य है।"
                ),
                actionItems = listOf(
                    "कल दोपहर 12 बजे तक फाइनल सिनॉप्सिस प्रिंट निकालना",
                    "प्रोजेक्ट कोड गिटहब (GitHub) पर पुश करना",
                    "साथियों को कल सुबह 9 बजे मिलने को बोलना"
                ),
                urgencyLevel = "उच्च (High) 🔴",
                rawSnippet = "नमस्ते बेटा, 15 तारीख को आपका प्रोजेक्ट प्रेजेंटेशन है। सभी 3 सदस्य हार्ड कॉपी के साथ आएं।"
            ),
            CallSummaryItem(
                id = 2L,
                callerName = "रोहित (कॉलेज फ्रेंड)",
                callerNumber = "+91 98765-11223",
                durationText = "4 मिनट 10 सेकंड",
                category = "दोस्त / परिवार",
                timestamp = "आज, 09:15 AM",
                summaryPoints = listOf(
                    "डेटा स्ट्रक्चर्स के यूनिट 3 के नोट्स मांगे।",
                    "कल की कैंटीन मीटिंग और लैब असाइनमेंट पर चर्चा हुई।"
                ),
                actionItems = listOf(
                    "रोहित को व्हाट्सएप पर यूनिट 3 की पीडीएफ भेजना"
                ),
                urgencyLevel = "मध्यम (Medium) 🟡",
                rawSnippet = "भाई कल लैब में कौन सा प्रोग्राम दिखाना है? यूनिट 3 के नोट्स पीडीएफ भेज दे प्लीज।"
            ),
            CallSummaryItem(
                id = 3L,
                callerName = "मम्मी ❤️",
                callerNumber = "+91 99887-76655",
                durationText = "1 मिनट 30 सेकंड",
                category = "दोस्त / परिवार",
                timestamp = "कल, 08:00 PM",
                summaryPoints = listOf(
                    "घर समय पर आने और शाम को आते समय मेडिकल स्टोर से दवा लाने को कहा।"
                ),
                actionItems = listOf(
                    "शाम को घर लौटते समय मेडिकल स्टोर से दवा लेना"
                ),
                urgencyLevel = "उच्च (High) 🔴",
                rawSnippet = "बेटा क्लास खत्म करके सीधे घर आना और रास्ते में मेडिकल से पापा की दवा ले आना।"
            )
        )
    )
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
