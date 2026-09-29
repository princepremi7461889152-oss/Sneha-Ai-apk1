package com.example.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.SnehaApplication
import com.example.engine.AiCloudConnectorManager
import com.example.engine.AiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class SearchSource(val title: String, val url: String)

data class GroundedResponse(
    val text: String,
    val searchSources: List<SearchSource> = emptyList(),
    val mapsPlaces: List<String> = emptyList(),
    val searchQueries: List<String> = emptyList()
)

data class ChatTurn(val role: String, val text: String)

data class GeneratedImageResult(
    val bitmap: Bitmap?,
    val textDescription: String,
    val rawBase64: String? = null
)

data class GeneratedMusicResult(
    val audioBase64: String?,
    val mimeType: String,
    val description: String
)

data class GeneratedVideoResult(
    val operationName: String,
    val videoUrl: String?,
    val status: String,
    val description: String
)

object GeminiCreativeClient {
    private const val TAG = "GeminiCreativeClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getEffectiveApiKey(): String {
        return try {
            val custom = AiCloudConnectorManager.getApiKey(SnehaApplication.instance, AiProvider.GEMINI)
            if (custom.isNotBlank() && custom != "MY_GEMINI_API_KEY") custom else BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            BuildConfig.GEMINI_API_KEY
        }
    }

    // ---------------------------------------------------------
    // 1. GEMINI CHATBOT (gemini-3.5-flash, gemini-3.1-pro-preview, gemini-3.1-flash-lite)
    // ---------------------------------------------------------
    suspend fun chatMultiTurn(
        model: String, // "gemini-3.5-flash", "gemini-3.1-pro-preview", "gemini-3.1-flash-lite"
        history: List<ChatTurn>,
        systemInstruction: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "कृपया सेटिंग्स में अपनी मान्य Gemini API Key दर्ज करें।"
        }

        try {
            val endpoint = "$BASE_URL$model:generateContent?key=$apiKey"
            val contentsArr = JSONArray()
            for (turn in history) {
                contentsArr.put(JSONObject().apply {
                    put("role", turn.role)
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", turn.text) })
                    })
                })
            }

            val jsonBody = JSONObject().apply {
                if (systemInstruction.isNotBlank()) {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", systemInstruction) })
                        })
                    })
                }
                put("contents", contentsArr)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 2048)
                })
            }

            val request = Request.Builder()
                .url(endpoint)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext "त्रुटि (${response.code}): $bodyStr"
                }
                parseTextFromResponse(bodyStr)
            }
        } catch (e: Exception) {
            Log.e(TAG, "chatMultiTurn error", e)
            "नेटवर्क त्रुटि: ${e.message}"
        }
    }

    // ---------------------------------------------------------
    // 2. GOOGLE SEARCH GROUNDING (gemini-3.5-flash with googleSearch tool)
    // ---------------------------------------------------------
    suspend fun searchGrounding(prompt: String): GroundedResponse = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GroundedResponse(text = "कृपया सेटिंग्स में अपनी मान्य Gemini API Key दर्ज करें।")
        }

        try {
            val endpoint = "${BASE_URL}gemini-3.5-flash:generateContent?key=$apiKey"
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
                put("tools", JSONArray().apply {
                    put(JSONObject().apply {
                        put("googleSearch", JSONObject())
                    })
                })
            }

            val request = Request.Builder()
                .url(endpoint)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext GroundedResponse(text = "खोज त्रुटि (${response.code}): $bodyStr")
                }
                parseGroundedResponse(bodyStr)
            }
        } catch (e: Exception) {
            Log.e(TAG, "searchGrounding error", e)
            GroundedResponse(text = "खोज में त्रुटि: ${e.message}")
        }
    }

    // ---------------------------------------------------------
    // 3. GOOGLE MAPS GROUNDING (gemini-3.5-flash with googleMaps tool)
    // ---------------------------------------------------------
    suspend fun mapsGrounding(prompt: String): GroundedResponse = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GroundedResponse(text = "कृपया सेटिंग्स में अपनी मान्य Gemini API Key दर्ज करें।")
        }

        try {
            val endpoint = "${BASE_URL}gemini-3.5-flash:generateContent?key=$apiKey"
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
                put("tools", JSONArray().apply {
                    put(JSONObject().apply {
                        put("googleMaps", JSONObject())
                    })
                })
            }

            val request = Request.Builder()
                .url(endpoint)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext GroundedResponse(text = "मैप्स त्रुटि (${response.code}): $bodyStr")
                }
                parseGroundedResponse(bodyStr)
            }
        } catch (e: Exception) {
            Log.e(TAG, "mapsGrounding error", e)
            GroundedResponse(text = "मैप्स डेटा प्राप्त करने में त्रुटि: ${e.message}")
        }
    }

    // ---------------------------------------------------------
    // 4. CREATE & EDIT IMAGES (gemini-3.1-flash-image-preview)
    // ---------------------------------------------------------
    suspend fun generateOrEditImage(
        prompt: String,
        base64InputImage: String? = null,
        aspectRatio: String = "1:1"
    ): GeneratedImageResult = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeneratedImageResult(
                bitmap = null,
                textDescription = "कृपया इमेज जनरेट करने के लिए अपनी Gemini API Key सेट करें।"
            )
        }

        try {
            val endpoint = "${BASE_URL}gemini-3.1-flash-image-preview:generateContent?key=$apiKey"
            val partsArr = JSONArray().apply {
                put(JSONObject().apply { put("text", prompt) })
                if (!base64InputImage.isNullOrBlank()) {
                    put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", "image/jpeg")
                            put("data", base64InputImage)
                        })
                    })
                }
            }

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", partsArr)
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().apply {
                        put("TEXT")
                        put("IMAGE")
                    })
                    put("imageConfig", JSONObject().apply {
                        put("aspectRatio", aspectRatio)
                        put("imageSize", "1K")
                    })
                })
            }

            val request = Request.Builder()
                .url(endpoint)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext GeneratedImageResult(
                        bitmap = null,
                        textDescription = "इमेज जनरेशन त्रुटि (${response.code}): $bodyStr"
                    )
                }

                val json = JSONObject(bodyStr)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                    var textDesc = ""
                    var foundBitmap: Bitmap? = null
                    var foundBase64: String? = null

                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            if (part.has("text")) {
                                textDesc += part.optString("text") + " "
                            }
                            if (part.has("inlineData")) {
                                val inline = part.getJSONObject("inlineData")
                                val data = inline.optString("data")
                                if (data.isNotBlank()) {
                                    foundBase64 = data
                                    val bytes = Base64.decode(data, Base64.DEFAULT)
                                    foundBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                }
                            }
                        }
                    }

                    GeneratedImageResult(
                        bitmap = foundBitmap,
                        textDescription = if (textDesc.isNotBlank()) textDesc.trim() else "इमेज सफलतापूर्वक जनरेट की गई!",
                        rawBase64 = foundBase64
                    )
                } else {
                    GeneratedImageResult(bitmap = null, textDescription = "कोई इमेज परिणाम नहीं मिला।")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "generateOrEditImage error", e)
            GeneratedImageResult(bitmap = null, textDescription = "इमेज बनाने में त्रुटि: ${e.message}")
        }
    }

    // ---------------------------------------------------------
    // 5. GENERATE MUSIC (lyria-3-clip-preview & lyria-3-pro-preview)
    // ---------------------------------------------------------
    suspend fun generateMusic(
        prompt: String,
        isFullLength: Boolean = false
    ): GeneratedMusicResult = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeneratedMusicResult(
                audioBase64 = null,
                mimeType = "audio/mp3",
                description = "कृपया म्यूजिक जनरेशन के लिए अपनी Gemini API Key सेट करें।"
            )
        }

        val model = if (isFullLength) "lyria-3-pro-preview" else "lyria-3-clip-preview"

        try {
            val endpoint = "$BASE_URL$model:generateContent?key=$apiKey"
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
                    put("responseModalities", JSONArray().apply { put("AUDIO") })
                })
            }

            val request = Request.Builder()
                .url(endpoint)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext GeneratedMusicResult(
                        audioBase64 = null,
                        mimeType = "audio/mp3",
                        description = "म्यूजिक जनरेशन त्रुटि (${response.code}): $bodyStr"
                    )
                }

                val json = JSONObject(bodyStr)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                    var textDesc = ""
                    var audioB64: String? = null
                    var mime = "audio/mp3"

                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            if (part.has("text")) {
                                textDesc += part.optString("text") + " "
                            }
                            if (part.has("inlineData")) {
                                val inline = part.getJSONObject("inlineData")
                                mime = inline.optString("mimeType", "audio/mp3")
                                audioB64 = inline.optString("data")
                            }
                        }
                    }

                    GeneratedMusicResult(
                        audioBase64 = audioB64,
                        mimeType = mime,
                        description = if (textDesc.isNotBlank()) textDesc.trim() else "म्यूजिक ट्रैक सफलतापूर्वक जनरेट हुआ ($model)"
                    )
                } else {
                    GeneratedMusicResult(
                        audioBase64 = null,
                        mimeType = "audio/mp3",
                        description = "कोई ऑडियो डेटा प्राप्त नहीं हुआ।"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "generateMusic error", e)
            GeneratedMusicResult(
                audioBase64 = null,
                mimeType = "audio/mp3",
                description = "म्यूजिक जनरेशन में त्रुटि: ${e.message}"
            )
        }
    }

    // ---------------------------------------------------------
    // 6. VEO VIDEO GENERATION (veo-3.1-fast-generate-preview)
    // ---------------------------------------------------------
    suspend fun generateVeoVideo(
        prompt: String,
        base64Image: String? = null,
        aspectRatio: String = "16:9" // "16:9" or "9:16"
    ): GeneratedVideoResult = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeneratedVideoResult(
                operationName = "",
                videoUrl = null,
                status = "FAILED",
                description = "कृपया Veo वीडियो जनरेशन के लिए अपनी Gemini API Key सेट करें।"
            )
        }

        try {
            val endpoint = "${BASE_URL}veo-3.1-fast-generate-preview:generateVideos?key=$apiKey"
            val jsonBody = JSONObject().apply {
                put("prompt", prompt)
                put("config", JSONObject().apply {
                    put("numberOfVideos", 1)
                    put("resolution", "720p")
                    put("aspectRatio", aspectRatio)
                })
                if (!base64Image.isNullOrBlank()) {
                    put("image", JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", "image/jpeg")
                            put("data", base64Image)
                        })
                    })
                }
            }

            val request = Request.Builder()
                .url(endpoint)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext GeneratedVideoResult(
                        operationName = "",
                        videoUrl = null,
                        status = "ERROR",
                        description = "Veo वीडियो जनरेशन त्रुटि (${response.code}): $bodyStr"
                    )
                }

                val json = JSONObject(bodyStr)
                val opName = json.optString("name", "veo_op_${System.currentTimeMillis()}")
                GeneratedVideoResult(
                    operationName = opName,
                    videoUrl = null,
                    status = "PROCESSING",
                    description = "Veo 3.1 वीडियो जनरेशन शुरू हो गया है (Aspect Ratio: $aspectRatio)। ऑपरेशन आईडी: $opName"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "generateVeoVideo error", e)
            GeneratedVideoResult(
                operationName = "",
                videoUrl = null,
                status = "FAILED",
                description = "Veo वीडियो प्रोसेस करने में त्रुटि: ${e.message}"
            )
        }
    }

    // ---------------------------------------------------------
    // 7. TRANSCRIBE AUDIO (gemini-3.5-transcribe)
    // ---------------------------------------------------------
    suspend fun transcribeAudio(
        audioBase64: String,
        mimeType: String = "audio/mp3",
        instruction: String = "Transcribe this audio recording accurately verbatim in the original language."
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "कृपया ऑडियो ट्रांसक्रिप्शन के लिए Gemini API Key दर्ज करें।"
        }

        try {
            val endpoint = "${BASE_URL}gemini-3.5-transcribe:generateContent?key=$apiKey"
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", instruction) })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", mimeType)
                                    put("data", audioBase64)
                                })
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(endpoint)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext "ट्रांसक्रिप्शन त्रुटि (${response.code}): $bodyStr"
                }
                parseTextFromResponse(bodyStr)
            }
        } catch (e: Exception) {
            Log.e(TAG, "transcribeAudio error", e)
            "ऑडियो ट्रांसक्राइब करने में त्रुटि: ${e.message}"
        }
    }

    // ---------------------------------------------------------
    // 8. LIVE VOICE CONVERSATIONS (gemini-3.8-live)
    // ---------------------------------------------------------
    suspend fun liveVoiceTurn(
        userAudioBase64: String? = null,
        userText: String? = null,
        systemInstruction: String = "You are Sneha, a loyal and intelligent real-time conversational AI. Answer concisely in 1-2 spoken sentences."
    ): Pair<String, String?> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Pair("कृपया Live Voice के लिए Gemini API Key दर्ज करें।", null)
        }

        try {
            val endpoint = "${BASE_URL}gemini-3.8-live:generateContent?key=$apiKey"
            val partsArr = JSONArray()
            if (!userText.isNullOrBlank()) {
                partsArr.put(JSONObject().apply { put("text", userText) })
            }
            if (!userAudioBase64.isNullOrBlank()) {
                partsArr.put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", "audio/mp3")
                        put("data", userAudioBase64)
                    })
                })
            }

            val jsonBody = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                })
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", partsArr)
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().apply {
                        put("TEXT")
                        put("AUDIO")
                    })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", "Kore")
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(endpoint)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Pair("Live API त्रुटि (${response.code}): $bodyStr", null)
                }

                val json = JSONObject(bodyStr)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                    var textResp = ""
                    var audioB64: String? = null

                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            if (part.has("text")) {
                                textResp += part.optString("text") + " "
                            }
                            if (part.has("inlineData")) {
                                audioB64 = part.getJSONObject("inlineData").optString("data")
                            }
                        }
                    }
                    Pair(if (textResp.isNotBlank()) textResp.trim() else "Live response received", audioB64)
                } else {
                    Pair("कोई उत्तर प्राप्त नहीं हुआ।", null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "liveVoiceTurn error", e)
            Pair("Live सेशन में त्रुटि: ${e.message}", null)
        }
    }

    // Helper parsers
    private fun parseTextFromResponse(bodyStr: String): String {
        return try {
            val json = JSONObject(bodyStr)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val sb = StringBuilder()
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        if (part.has("text")) {
                            sb.append(part.getString("text"))
                        }
                    }
                    if (sb.isNotBlank()) return sb.toString().trim()
                }
            }
            "कोई उत्तर उपलब्ध नहीं।"
        } catch (e: Exception) {
            "पार्सिंग त्रुटि: ${e.message}"
        }
    }

    private fun parseGroundedResponse(bodyStr: String): GroundedResponse {
        return try {
            val json = JSONObject(bodyStr)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val cand = candidates.getJSONObject(0)
                val parts = cand.optJSONObject("content")?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text", "") ?: ""

                val sources = mutableListOf<SearchSource>()
                val places = mutableListOf<String>()
                val searchQueries = mutableListOf<String>()

                val groundingMetadata = cand.optJSONObject("groundingMetadata")
                if (groundingMetadata != null) {
                    val queries = groundingMetadata.optJSONArray("webSearchQueries")
                    if (queries != null) {
                        for (i in 0 until queries.length()) {
                            searchQueries.add(queries.getString(i))
                        }
                    }

                    val chunks = groundingMetadata.optJSONArray("groundingChunks")
                    if (chunks != null) {
                        for (i in 0 until chunks.length()) {
                            val chunk = chunks.getJSONObject(i)
                            val web = chunk.optJSONObject("web")
                            if (web != null) {
                                val title = web.optString("title", "वेब सोर्स")
                                val uri = web.optString("uri", "")
                                if (uri.isNotBlank()) {
                                    sources.add(SearchSource(title, uri))
                                }
                            }
                            val maps = chunk.optJSONObject("maps")
                            if (maps != null) {
                                val name = maps.optString("title", "स्थान")
                                places.add(name)
                            }
                        }
                    }
                }

                GroundedResponse(
                    text = text,
                    searchSources = sources,
                    mapsPlaces = places,
                    searchQueries = searchQueries
                )
            } else {
                GroundedResponse(text = "कोई परिणाम नहीं मिला।")
            }
        } catch (e: Exception) {
            GroundedResponse(text = "पार्सिंग त्रुटि: ${e.message}")
        }
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
