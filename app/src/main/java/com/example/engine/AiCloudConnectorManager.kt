package com.example.engine

import android.content.Context
import android.content.SharedPreferences
import com.sneha.ai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object AiCloudConnectorManager {
    private const val PREFS_NAME = "sneha_ai_cloud_prefs"
    private const val TAG = "AiCloudConnectorManager"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _activeProvider = MutableStateFlow(AiProvider.GEMINI)
    val activeProvider: StateFlow<AiProvider> = _activeProvider.asStateFlow()

    private val _connectionStatus = MutableStateFlow<ConnectionTestResult?>(null)
    val connectionStatus: StateFlow<ConnectionTestResult?> = _connectionStatus.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun init(context: Context) {
        val providerId = getPrefs(context).getString("active_provider", AiProvider.GEMINI.id)
        val provider = AiProvider.values().find { it.id == providerId } ?: AiProvider.GEMINI
        _activeProvider.value = provider
    }

    fun setActiveProvider(context: Context, provider: AiProvider) {
        _activeProvider.value = provider
        getPrefs(context).edit().putString("active_provider", provider.id).apply()
    }

    fun getApiKey(context: Context, provider: AiProvider): String {
        val saved = getPrefs(context).getString("api_key_${provider.id}", "")?.trim() ?: ""
        if (saved.isNotEmpty()) return sanitizeKey(saved)
        // If Gemini has no custom key, fallback to BuildConfig GEMINI_API_KEY
        if (provider == AiProvider.GEMINI && BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            return sanitizeKey(BuildConfig.GEMINI_API_KEY)
        }
        return ""
    }

    fun saveApiKey(context: Context, provider: AiProvider, key: String) {
        val cleanKey = sanitizeKey(key)
        getPrefs(context).edit().putString("api_key_${provider.id}", cleanKey).apply()
    }

    fun sanitizeKey(key: String): String {
        return key.trim()
            .replace("\n", "")
            .replace("\r", "")
            .replace("\t", "")
            .replace("\"", "")
            .replace("'", "")
            .replace(" ", "")
    }

    fun getCustomEndpoint(context: Context): String {
        return getPrefs(context).getString("custom_endpoint", "https://api.openai.com/v1/chat/completions") ?: ""
    }

    fun saveCustomEndpoint(context: Context, endpoint: String) {
        getPrefs(context).edit().putString("custom_endpoint", endpoint.trim()).apply()
    }

    fun getCustomModel(context: Context, provider: AiProvider): String {
        return getPrefs(context).getString("custom_model_${provider.id}", provider.defaultModel) ?: provider.defaultModel
    }

    fun saveCustomModel(context: Context, provider: AiProvider, model: String) {
        getPrefs(context).edit().putString("custom_model_${provider.id}", model.trim()).apply()
    }

    suspend fun testConnection(context: Context, provider: AiProvider): ConnectionTestResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val apiKey = getApiKey(context, provider)
        val model = getCustomModel(context, provider)

        if (provider != AiProvider.CUSTOM && apiKey.isBlank()) {
            val res = ConnectionTestResult(
                success = false,
                latencyMs = 0L,
                message = "API Key दर्ज नहीं है! कृपया पहले अपनी ${provider.displayName} Key पेस्ट करके सेव करें।"
            )
            _connectionStatus.value = res
            return@withContext res
        }

        // Help user if they pasted wrong vendor key
        if (provider == AiProvider.GEMINI && apiKey.startsWith("sk-")) {
            val res = ConnectionTestResult(
                success = false,
                latencyMs = 0L,
                message = "यह OpenAI की API Key है! Gemini के लिए Google AI Studio (AIzaSy...) वाली Key दर्ज करें।"
            )
            _connectionStatus.value = res
            return@withContext res
        }

        if (provider == AiProvider.OPENAI && apiKey.startsWith("AIzaSy")) {
            val res = ConnectionTestResult(
                success = false,
                latencyMs = 0L,
                message = "यह Google Gemini Key है! OpenAI के लिए sk-... वाली Key दर्ज करें।"
            )
            _connectionStatus.value = res
            return@withContext res
        }

        try {
            val reply = callProviderApi(context, provider, "Reply with: OK")
            val latency = System.currentTimeMillis() - start
            val success = reply.isNotBlank()
            val res = if (success) {
                ConnectionTestResult(
                    success = true,
                    latencyMs = latency,
                    message = "${provider.displayName} सफलतापूर्वक कनेक्टेड! (${latency}ms)"
                )
            } else {
                ConnectionTestResult(
                    success = false,
                    latencyMs = latency,
                    message = "${provider.displayName} से कोई उत्तर नहीं मिला। कृपया API Key दोबारा जांचें।"
                )
            }
            _connectionStatus.value = res
            res
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - start
            val errorMsg = parseFriendlyError(e.message ?: "अज्ञात त्रुटि")
            val res = ConnectionTestResult(
                success = false,
                latencyMs = latency,
                message = "कनेक्शन त्रुटि: $errorMsg"
            )
            _connectionStatus.value = res
            res
        }
    }

    private fun parseFriendlyError(raw: String): String {
        return when {
            raw.contains("API_KEY_INVALID", ignoreCase = true) || raw.contains("API key not valid", ignoreCase = true) ->
                "अमान्य API Key! कृपया Google AI Studio (aistudio.google.com) से नई Key लेकर पेस्ट करें।"
            raw.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || raw.contains("quota", ignoreCase = true) ->
                "API Key मान्य है, परंतु इसका Free Quota समाप्त हो चुका है। कृपया थोड़ी देर बाद प्रयास करें या नई Key उपयोग करें।"
            raw.contains("404", ignoreCase = true) ->
                "चुना गया मॉडल उपलब्ध नहीं है, डिफ़ॉल्ट gemini-2.5-flash का उपयोग किया जा रहा है।"
            raw.contains("Unable to resolve host", ignoreCase = true) || raw.contains("Failed to connect", ignoreCase = true) ->
                "इंटरनेट कनेक्शन उपलब्ध नहीं है। कृपया मोबाइल डेटा या वाई-फ़ाई ऑन करें।"
            else -> raw
        }
    }

    suspend fun generateResponse(context: Context, prompt: String): String = withContext(Dispatchers.IO) {
        val provider = _activeProvider.value
        callProviderApi(context, provider, prompt)
    }

    fun callProviderApi(context: Context, provider: AiProvider, prompt: String): String {
        val apiKey = getApiKey(context, provider)
        val model = getCustomModel(context, provider)
        return when (provider) {
            AiProvider.GEMINI -> callGemini(apiKey, model, prompt)
            AiProvider.OPENAI -> callOpenAi(apiKey, model, prompt)
            AiProvider.CLAUDE -> callClaude(apiKey, model, prompt)
            AiProvider.CUSTOM -> callCustom(context, apiKey, model, prompt)
        }
    }

    private fun callGemini(apiKey: String, model: String, prompt: String): String {
        val cleanKey = sanitizeKey(apiKey)
        if (cleanKey.isBlank()) return ""

        val modelsToTry = listOf(
            model.ifBlank { "gemini-2.5-flash" },
            "gemini-2.5-flash",
            "gemini-flash-latest"
        ).distinct()

        var lastException: Exception? = null

        for (targetModel in modelsToTry) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$targetModel:generateContent?key=$cleanKey"
                val json = JSONObject().apply {
                    val contents = JSONArray().apply {
                        val userObj = JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", prompt))
                            })
                        }
                        put(userObj)
                    }
                    put("contents", contents)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val request = Request.Builder()
                    .url(url)
                    .post(json.toString().toRequestBody(mediaType))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val code = response.code
                    if (code == 404 && targetModel != modelsToTry.last()) {
                        continue // Try next model
                    }
                    val friendly = try {
                        val errObj = JSONObject(responseBody).optJSONObject("error")
                        errObj?.optString("message") ?: "HTTP $code"
                    } catch (_: Exception) {
                        "HTTP $code"
                    }
                    throw IOException(friendly)
                }

                val resJson = JSONObject(responseBody)
                val candidates = resJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val contentObj = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return parts.getJSONObject(0).optString("text", "")
                    }
                }
                return ""
            } catch (e: Exception) {
                lastException = e
                if (e.message?.contains("API key not valid", ignoreCase = true) == true) {
                    throw e // Do not retry on invalid key
                }
            }
        }
        throw lastException ?: IOException("Gemini API से कोई प्रतिक्रिया नहीं मिली")
    }

    private fun callOpenAi(apiKey: String, model: String, prompt: String): String {
        val cleanKey = sanitizeKey(apiKey)
        if (cleanKey.isBlank()) return ""

        val url = "https://api.openai.com/v1/chat/completions"
        val json = JSONObject().apply {
            put("model", model.ifBlank { "gpt-4o-mini" })
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("max_tokens", 500)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $cleanKey")
            .post(json.toString().toRequestBody(mediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw IOException("OpenAI Error HTTP ${response.code}: $responseBody")
        }

        val resJson = JSONObject(responseBody)
        val choices = resJson.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val message = choices.getJSONObject(0).optJSONObject("message")
            return message?.optString("content", "") ?: ""
        }
        return ""
    }

    private fun callClaude(apiKey: String, model: String, prompt: String): String {
        val cleanKey = sanitizeKey(apiKey)
        if (cleanKey.isBlank()) return ""

        val url = "https://api.anthropic.com/v1/messages"
        val json = JSONObject().apply {
            put("model", model.ifBlank { "claude-3-haiku-20240307" })
            put("max_tokens", 500)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", cleanKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(json.toString().toRequestBody(mediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw IOException("Claude Error HTTP ${response.code}: $responseBody")
        }

        val resJson = JSONObject(responseBody)
        val content = resJson.optJSONArray("content")
        if (content != null && content.length() > 0) {
            return content.getJSONObject(0).optString("text", "")
        }
        return ""
    }

    private fun callCustom(context: Context, apiKey: String, model: String, prompt: String): String {
        val endpoint = getCustomEndpoint(context)
        if (endpoint.isBlank()) throw IOException("Custom Endpoint URL खाली है")

        val json = JSONObject().apply {
            put("model", model)
            put("prompt", prompt)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val builder = Request.Builder().url(endpoint)
        if (apiKey.isNotBlank()) {
            builder.addHeader("Authorization", "Bearer ${sanitizeKey(apiKey)}")
        }

        val request = builder.post(json.toString().toRequestBody(mediaType)).build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw IOException("Custom API HTTP ${response.code}: $responseBody")
        }
        return responseBody
    }
}
