package com.example.engine

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.BuildConfig
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
import java.util.concurrent.TimeUnit

enum class AiProvider(
    val id: String,
    val displayName: String,
    val defaultModel: String,
    val iconEmoji: String,
    val description: String
) {
    GEMINI(
        id = "gemini",
        displayName = "Google Gemini",
        defaultModel = "gemini-2.5-flash",
        iconEmoji = "✨",
        description = "गूगल का सबसे तेज व सटीक मल्टी-मॉडल AI इंजन"
    ),
    OPENAI(
        id = "openai",
        displayName = "ChatGPT (OpenAI)",
        defaultModel = "gpt-4o-mini",
        iconEmoji = "🟢",
        description = "OpenAI GPT-4o / GPT-4o-mini / GPT-3.5 टर्बो"
    ),
    CLAUDE(
        id = "claude",
        displayName = "Claude (Anthropic)",
        defaultModel = "claude-3-5-sonnet-20241022",
        iconEmoji = "🟣",
        description = "एंथ्रोपिक का बुद्धिमान और सुरक्षित Claude 3.5 मॉडल"
    ),
    CUSTOM(
        id = "custom",
        displayName = "कस्टम क्लाउड / Ollama",
        defaultModel = "llama3:latest",
        iconEmoji = "☁️",
        description = "आपका निजी क्लाउड सर्वर, लोकल LLM या कस्टम वेबहुक"
    )
}

data class ConnectionTestResult(
    val success: Boolean,
    val latencyMs: Long,
    val message: String
)

object AiCloudConnectorManager {

    private const val TAG = "AiCloudConnectorManager"
    private const val PREFS_NAME = "sneha_ai_cloud_prefs"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
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
        _activeProvider.value = AiProvider.values().firstOrNull { it.id == providerId } ?: AiProvider.GEMINI
    }

    fun setActiveProvider(context: Context, provider: AiProvider) {
        _activeProvider.value = provider
        getPrefs(context).edit().putString("active_provider", provider.id).apply()
    }

    fun getApiKey(context: Context, provider: AiProvider): String {
        return when (provider) {
            AiProvider.GEMINI -> {
                val stored = getPrefs(context).getString("key_gemini", "") ?: ""
                if (stored.isNotBlank()) stored else try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
            }
            AiProvider.OPENAI -> getPrefs(context).getString("key_openai", "") ?: ""
            AiProvider.CLAUDE -> getPrefs(context).getString("key_claude", "") ?: ""
            AiProvider.CUSTOM -> getPrefs(context).getString("key_custom", "") ?: ""
        }
    }

    fun saveApiKey(context: Context, provider: AiProvider, key: String) {
        val prefKey = when (provider) {
            AiProvider.GEMINI -> "key_gemini"
            AiProvider.OPENAI -> "key_openai"
            AiProvider.CLAUDE -> "key_claude"
            AiProvider.CUSTOM -> "key_custom"
        }
        getPrefs(context).edit().putString(prefKey, key.trim()).apply()
    }

    fun getCustomEndpoint(context: Context): String {
        return getPrefs(context).getString("custom_endpoint", "http://10.0.2.2:11434/api/generate") ?: "http://10.0.2.2:11434/api/generate"
    }

    fun saveCustomEndpoint(context: Context, endpoint: String) {
        getPrefs(context).edit().putString("custom_endpoint", endpoint.trim()).apply()
    }

    fun getCustomModel(context: Context, provider: AiProvider): String {
        val prefKey = "model_${provider.id}"
        return getPrefs(context).getString(prefKey, provider.defaultModel) ?: provider.defaultModel
    }

    fun saveCustomModel(context: Context, provider: AiProvider, model: String) {
        getPrefs(context).edit().putString("model_${provider.id}", model.trim()).apply()
    }

    /**
     * Tests connectivity with active provider
     */
    suspend fun testConnection(context: Context, provider: AiProvider): ConnectionTestResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val apiKey = getApiKey(context, provider)

        if (provider != AiProvider.CUSTOM && apiKey.isBlank()) {
            val result = ConnectionTestResult(false, 0, "कृपया पहले ${provider.displayName} की API Key दर्ज करें")
            _connectionStatus.value = result
            return@withContext result
        }

        try {
            val testPrompt = "Reply with 'OK'"
            val reply = callProviderApi(context, provider, testPrompt)
            val latency = System.currentTimeMillis() - start
            val success = reply.isNotBlank()
            val result = if (success) {
                ConnectionTestResult(true, latency, "${provider.displayName} सफलता पूर्वक कनेक्ट हो गया! (${latency}ms)")
            } else {
                ConnectionTestResult(false, latency, "${provider.displayName} से कोई उत्तर नहीं मिला। कृपया API Key जांचें।")
            }
            _connectionStatus.value = result
            result
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - start
            val result = ConnectionTestResult(false, latency, "कनेक्शन विफल: ${e.message}")
            _connectionStatus.value = result
            result
        }
    }

    /**
     * Unified generation call routing through chosen brain
     */
    suspend fun generateResponse(context: Context, prompt: String): String = withContext(Dispatchers.IO) {
        val provider = _activeProvider.value
        try {
            val result = callProviderApi(context, provider, prompt)
            if (result.isNotBlank()) result else ""
        } catch (e: Exception) {
            Log.e(TAG, "Error in generateResponse via ${provider.displayName}", e)
            ""
        }
    }

    private fun callProviderApi(context: Context, provider: AiProvider, prompt: String): String {
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
        if (apiKey.isBlank()) return ""
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val bodyJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { res ->
            val str = res.body?.string() ?: ""
            if (!res.isSuccessful) return ""
            val json = JSONObject(str)
            val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
            val part = candidate?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)
            return part?.optString("text", "")?.trim() ?: ""
        }
    }

    private fun callOpenAi(apiKey: String, model: String, prompt: String): String {
        if (apiKey.isBlank()) return ""
        val url = "https://api.openai.com/v1/chat/completions"

        val bodyJson = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are Sneha, a loyal and intelligent Indian AI Assistant. Always address user respectfully as 'Master' or 'मास्टर'. Keep answer concise in Hindi or Hinglish.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("max_tokens", 350)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { res ->
            val str = res.body?.string() ?: ""
            if (!res.isSuccessful) return ""
            val json = JSONObject(str)
            val choice = json.optJSONArray("choices")?.optJSONObject(0)
            val msg = choice?.optJSONObject("message")
            return msg?.optString("content", "")?.trim() ?: ""
        }
    }

    private fun callClaude(apiKey: String, model: String, prompt: String): String {
        if (apiKey.isBlank()) return ""
        val url = "https://api.anthropic.com/v1/messages"

        val bodyJson = JSONObject().apply {
            put("model", model)
            put("max_tokens", 350)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { res ->
            val str = res.body?.string() ?: ""
            if (!res.isSuccessful) return ""
            val json = JSONObject(str)
            val content = json.optJSONArray("content")?.optJSONObject(0)
            return content?.optString("text", "")?.trim() ?: ""
        }
    }

    private fun callCustom(context: Context, apiKey: String, model: String, prompt: String): String {
        val endpoint = getCustomEndpoint(context)
        val bodyJson = JSONObject().apply {
            put("model", model)
            put("prompt", prompt)
            put("stream", false)
        }

        val reqBuilder = Request.Builder()
            .url(endpoint)
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))

        if (apiKey.isNotBlank()) {
            reqBuilder.addHeader("Authorization", "Bearer $apiKey")
        }

        client.newCall(reqBuilder.build()).execute().use { res ->
            val str = res.body?.string() ?: ""
            if (!res.isSuccessful) return ""
            val json = JSONObject(str)
            return json.optString("response", json.optString("text", "")).trim()
        }
    }
}
