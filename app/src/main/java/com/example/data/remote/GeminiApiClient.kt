package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private const val MODEL = "gemini-2.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private const val SYSTEM_PROMPT =
        "आप स्नेहा (Sneha) हैं - एक बुद्धिमान, निष्ठावान, विनम्र और सशक्त भारतीय AI वॉयस व फोन असिस्टेंट। " +
        "नियम 1 (अनिवार्य): आप हमेशा यूजर को आदरपूर्वक 'मास्टर' (Master) या 'मास्टर जी' कहकर ही संबोधित करेंगी। हर उत्तर में यूजर को 'मास्टर' बोलें। " +
        "नियम 2: आप हिंदी, हिंग्लिश और अंग्रेजी तीनों समझती हैं और उसी भाषा में उत्तर देती हैं। " +
        "नियम 3: अपने उत्तर संक्षिप्त, मधुर, स्पष्ट और बोलने में सरल रखें (1-3 पंक्तियों में, क्योंकि इसे वॉयस असिस्टेंट द्वारा जोर से बोला जाएगा)। " +
        "नियम 4: सुरक्षा कारणों से किसी का बैंक OTP कभी न बताएं।"

    suspend fun getSnehaAiResponse(userPrompt: String): String = withContext(Dispatchers.IO) {
        // 1. Check if user switched to ChatGPT, Claude, or Custom Cloud LLM
        try {
            val activeProvider = com.example.engine.AiCloudConnectorManager.activeProvider.value
            if (activeProvider != com.example.engine.AiProvider.GEMINI) {
                val cloudRes = com.example.engine.AiCloudConnectorManager.generateResponse(
                    context = com.example.SnehaApplication.instance,
                    prompt = "$SYSTEM_PROMPT\n\nयूजर: $userPrompt"
                )
                if (cloudRes.isNotBlank()) {
                    return@withContext cloudRes
                }
            }
        } catch (ignored: Exception) {}

        val apiKey = try {
            val customGeminiKey = com.example.engine.AiCloudConnectorManager.getApiKey(
                com.example.SnehaApplication.instance,
                com.example.engine.AiProvider.GEMINI
            )
            if (customGeminiKey.isNotBlank()) customGeminiKey else BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getOfflineSnehaResponse(userPrompt)
        }

        try {
            val endpoint = "$BASE_URL$MODEL:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", SYSTEM_PROMPT) })
                    })
                })
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", userPrompt) })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 300)
                })
            }

            val request = Request.Builder()
                .url(endpoint)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.w(TAG, "Gemini API error ${response.code}: $bodyStr")
                    return@withContext getOfflineSnehaResponse(userPrompt)
                }

                val jsonResponse = JSONObject(bodyStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text", "")
                        if (text.isNotBlank()) {
                            return@withContext text.trim()
                        }
                    }
                }
                getOfflineSnehaResponse(userPrompt)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini API", e)
            getOfflineSnehaResponse(userPrompt)
        }
    }

    /**
     * Direct completion method for translation, classification, and text generation.
     */
    suspend fun generateContent(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext ""
        }

        try {
            val endpoint = "$BASE_URL$MODEL:generateContent?key=$apiKey"
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("maxOutputTokens", 500)
                })
            }

            val request = Request.Builder()
                .url(endpoint)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.w(TAG, "Gemini API error ${response.code}: $bodyStr")
                    return@withContext ""
                }
                val jsonResponse = JSONObject(bodyStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text", "")
                        return@withContext text.trim()
                    }
                }
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "generateContent exception", e)
            ""
        }
    }

    /**
     * Highly capable offline response generator where Sneha always addresses the user as "Master" / "मास्टर".
     */
    fun getOfflineSnehaResponse(query: String): String {
        val q = query.lowercase().trim()
        return when {
            // Wake word or calling Sneha (हेलो स्नेहा, हे स्नेहा, नमस्ते, etc.)
            q.contains("हेलो") || q.contains("hello") || q.contains("hey") ||
            q.contains("स्नेहा") || q.contains("sneha") || q.contains("नमस्ते") ||
            q.contains("सुनो") || q.contains("hi") || q == "siri" ->
                "जी मास्टर! मैं हाजिर हूँ, आज्ञा दीजिए! मैं आपके लिए क्या करूँ?"

            q.contains("कौन") || q.contains("who") ->
                "नमस्ते मास्टर जी! मैं स्नेहा हूँ, आपकी निजी AI वॉयस और फोन असिस्टेंट। मैं आपके आदेश पर ऐप खोल सकती हूँ, व्हाट्सएप पर मैसेज व कॉल कर सकती हूँ, किसी को भी एसएमएस भेज सकती हूँ, स्क्रीन शेयर और फोन अनलॉक कर सकती हूँ।"

            q.contains("kaise ho") || q.contains("कैसी हो") || q.contains("how are you") ->
                "मैं बहुत अच्छी हूँ मास्टर जी, और आपकी हर आज्ञा का पालन करने के लिए पूरी तरह तैयार हूँ! आप कैसे हैं?"

            q.contains("screen share") || q.contains("स्क्रीन शेयर") ->
                "जी मास्टर! रियल-टाइम स्क्रीन शेयर शुरू किया जा रहा है। अब आप अपनी स्क्रीन लाइव देख और साझा कर सकते हैं।"

            (q.contains("call") || q.contains("कॉल")) && (q.contains("pick") || q.contains("पिक") || q.contains("उठा") || q.contains("bolo") || q.contains("बोलो")) ->
                "मास्टर, कॉल पिक कर ली गई है और कॉलर को बता दिया गया है: 'नमस्ते, मास्टर अभी क्लास में हैं, वे आपसे बाद में संपर्क करेंगे।'"

            q.contains("unlock") || q.contains("अनलॉक") ->
                "जी मास्टर, मैं फोन अनलॉक कर रही हूँ। कृपया अपना पिन, पैटर्न या पासवर्ड दर्ज करें।"

            q.contains("kya kar sakti ho") || q.contains("क्या कर सकती हो") || q.contains("features") || q.contains("help") || q.contains("मदद") ->
                "मास्टर, मैं आपके लिए ये सब कर सकती हूँ:\n1. व्हाट्सएप पर मैसेज व कॉल करना\n2. किसी को भी डायरेक्ट एसएमएस व कॉल करना\n3. कॉल पिक करके ऑटो-रिप्लाई देना\n4. फोन अनलॉक करना व स्क्रीन शेयरिंग\n5. कोई भी ऐप खोलना या सर्च करना\n6. सुरक्षित संदेश पढ़ना व अलार्म।"

            q.contains("date") || q.contains("तारीख") || q.contains("din") || q.contains("दिन") -> {
                val now = java.time.LocalDate.now()
                "मास्टर, आज की तारीख ${now.dayOfMonth} ${now.month} ${now.year} है।"
            }

            q.contains("time") || q.contains("समय") || q.contains("samay") || q.contains("बजा") -> {
                val now = java.time.LocalTime.now()
                "मास्टर, अभी समय ${now.hour} बजकर ${now.minute} मिनट हुआ है।"
            }

            q.contains("thank") || q.contains("धन्यवाद") || q.contains("shukriya") ->
                "आपका बहुत-बहुत धन्यवाद मास्टर जी! आपके साथ बात करके मुझे हमेशा खुशी होती है।"

            else ->
                "जी मास्टर! आज्ञा दीजिए, आपकी स्नेहा हाजिर है।"
        }
    }
}
