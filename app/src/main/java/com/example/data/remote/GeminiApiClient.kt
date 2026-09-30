package com.example.data.remote

import android.util.Log
import com.example.SnehaApplication
import com.example.engine.AiCloudConnectorManager
import com.example.engine.AiProvider
import com.sneha.ai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import java.util.concurrent.TimeUnit

object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
    private const val MODEL = "gemini-2.5-flash"
    private const val SYSTEM_PROMPT =
        "आप स्नेहा (Sneha) हैं - एक बुद्धिमान, निष्ठावान, विनम्र और सशक्त भारतीय AI वॉयस व फोन असिस्टेंट। नियम 1 (अनिवार्य): आप हमेशा यूजर को आदरपूर्वक 'मास्टर' (Master) या 'मास्टर जी' कहकर ही संबोधित करेंगी। नियम 2: हर उत्तर में संक्षिप्त, 2-4 पंक्तियों का सरल व स्पष्ट हिंदी/हिंग्लिश उत्तर दें। नियम 3: सुरक्षा कारणों से बैंक OTP कभी उजागर न करें।"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun getSnehaAiResponse(userPrompt: String): String = withContext(Dispatchers.IO) {
        val context = SnehaApplication.instance
        try {
            val response = AiCloudConnectorManager.generateResponse(context, "$SYSTEM_PROMPT\n\nयूजर का सवाल: $userPrompt")
            if (response.isNotBlank()) return@withContext response
        } catch (e: Exception) {
            Log.w(TAG, "AiCloudConnectorManager failed: ${e.message}, trying direct Gemini")
        }

        val direct = generateContent(userPrompt)
        if (direct.isNotBlank()) direct else getOfflineSnehaResponse(userPrompt)
    }

    suspend fun generateContent(prompt: String): String = withContext(Dispatchers.IO) {
        val context = SnehaApplication.instance
        var apiKey = AiCloudConnectorManager.getApiKey(context, AiProvider.GEMINI)
        if (apiKey.isBlank()) {
            apiKey = BuildConfig.GEMINI_API_KEY
        }
        val cleanKey = AiCloudConnectorManager.sanitizeKey(apiKey)
        if (cleanKey.isBlank()) {
            return@withContext getOfflineSnehaResponse(prompt)
        }

        val modelsToTry = listOf(MODEL, "gemini-2.5-flash", "gemini-flash-latest").distinct()

        for (targetModel in modelsToTry) {
            try {
                val url = "$BASE_URL$targetModel:generateContent?key=$cleanKey"
                val jsonBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", "$SYSTEM_PROMPT\n\nयूजर सवाल: $prompt"))
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.4)
                        put("maxOutputTokens", 500)
                    })
                }

                val request = Request.Builder()
                    .url(url)
                    .post(jsonBody.toString().toRequestBody(mediaType))
                    .build()

                val response = client.newCall(request).execute()
                val bodyStr = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    Log.w(TAG, "Gemini $targetModel error ${response.code}: $bodyStr")
                    if (response.code == 404 && targetModel != modelsToTry.last()) {
                        continue
                    }
                    return@withContext getOfflineSnehaResponse(prompt)
                }

                val resJson = JSONObject(bodyStr)
                val candidates = resJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text", "").trim()
                        if (text.isNotBlank()) return@withContext text
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini error on $targetModel: ${e.message}")
            }
        }
        getOfflineSnehaResponse(prompt)
    }

    fun getOfflineSnehaResponse(query: String): String {
        val q = query.lowercase(Locale.ROOT).trim()

        if (q.contains("हेलो") || q.contains("hello") || q.contains("hey") || q.contains("स्नेहा") ||
            q.contains("sneha") || q.contains("नमस्ते") || q.contains("सुनो") || q.contains("hi") || q == "siri"
        ) {
            return "जी मास्टर! मैं हाजिर हूँ, आज्ञा दीजिए! मैं आपके लिए क्या करूँ?"
        }

        if (q.contains("कौन") || q.contains("who")) {
            return "नमस्ते मास्टर जी! मैं स्नेहा हूँ, आपकी निजी AI वॉयस और फोन असिस्टेंट। मैं आपके आदेश पर ऐप्स खोल सकती हूँ, व्हाट्सएप मैसेज व कॉल कर सकती हूँ, एसएमएस भेज सकती हूँ, स्क्रीन शेयर और फोन कंट्रोल कर सकती हूँ।"
        }

        if (q.contains("kaise ho") || q.contains("कैसी हो") || q.contains("how are you")) {
            return "मैं बहुत अच्छी हूँ मास्टर जी, और आपकी हर आज्ञा का पालन करने के लिए पूरी तरह तैयार हूँ! आप कैसे हैं?"
        }

        if (q.contains("screen share") || q.contains("स्क्रीन शेयर")) {
            return "जी मास्टर! रियल-टाइम स्क्रीन शेयर शुरू किया जा रहा है। अब आप अपनी स्क्रीन लाइव देख और साझा कर सकते हैं।"
        }

        if ((q.contains("call") || q.contains("कॉल")) && (q.contains("pick") || q.contains("पिक") || q.contains("उठा") || q.contains("bolo") || q.contains("बोलो"))) {
            return "मास्टर, कॉल पिक कर ली गई है और कॉलर को बता दिया गया है: 'नमस्ते, मास्टर अभी व्यस्त हैं, वे आपसे बाद में संपर्क करेंगे।'"
        }

        if (q.contains("unlock") || q.contains("अनलॉक")) {
            return "जी मास्टर, मैं फोन अनलॉक मोड में सहायता कर रही हूँ। कृपया स्क्रीन चालू रखें।"
        }

        if (q.contains("kya kar sakti ho") || q.contains("क्या कर सकती हो") || q.contains("features") || q.contains("help") || q.contains("मदद")) {
            return "मास्टर, मैं आपके लिए ये सब कर सकती हूँ:\n1. व्हाट्सएप पर मैसेज व कॉल\n2. एसएमएस और डायरेक्ट कॉलिंग\n3. स्मार्ट होम और फोन सेटिंग्स कंट्रोल\n4. कॉल स्पैम प्रोटेक्शन व लाइव ट्रांसलेटर\n5. टाइमटेबल, क्लास नोट्स और एआई उत्तर।"
        }

        if (q.contains("date") || q.contains("तारीख") || q.contains("din") || q.contains("दिन")) {
            val now = LocalDate.now()
            return "मास्टर, आज की तारीख ${now.dayOfMonth} ${now.month} ${now.year} है।"
        }

        if (q.contains("time") || q.contains("समय") || q.contains("samay") || q.contains("बजा")) {
            val now = LocalTime.now()
            return "मास्टर, अभी समय ${now.hour} बजकर ${now.minute} मिनट हुआ है।"
        }

        if (q.contains("thank") || q.contains("धन्यवाद") || q.contains("shukriya")) {
            return "आपका बहुत-बहुत धन्यवाद मास्टर जी! आपके साथ बात करके मुझे हमेशा खुशी होती है।"
        }

        return "जी मास्टर! आज्ञा दीजिए, आपकी स्नेहा हमेशा आपकी सेवा में तत्पर है।"
    }
}
