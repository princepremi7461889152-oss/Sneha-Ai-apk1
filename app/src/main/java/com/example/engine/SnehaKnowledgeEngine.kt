package com.example.engine

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SnehaKnowledgeEngine {

    private const val TAG = "SnehaKnowledge"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    /**
     * Resolves any user question using:
     * 1. Rich encyclopedic Hindi Knowledge Base (capitals, geography, science, history, space, math, culture)
     * 2. Jokes, shayaris, thoughts, stories, and personality Q&A
     * 3. Free real-time Wikipedia / Instant Search API (zero API key needed)
     */
    suspend fun answerQuestion(context: Context, rawQuery: String): String = withContext(Dispatchers.IO) {
        val q = rawQuery.lowercase().trim()

        // 1. Check built-in encyclopedic, conversational & cultural knowledge
        getCuratedAnswer(q)?.let { return@withContext it }

        // 2. Fetch live real-time answer from Free Wikipedia API (Hindi or English)
        try {
            val wikiAnswer = fetchWikipediaSummary(rawQuery)
            if (!wikiAnswer.isNullOrBlank()) {
                return@withContext wikiAnswer
            }
        } catch (e: Exception) {
            Log.w(TAG, "Wikipedia search error", e)
        }

        // 3. Fallback conversational response
        return@withContext "मास्टर, मुझे इस विषय की सटीक जानकारी अभी नहीं मिली। आप सेटिंग्स में जाकर अपनी मुफ्त Google Gemini API Key जोड़ सकते हैं, जिससे मैं दुनिया के हर जटिल सवाल का विस्तृत उत्तर दे सकूँगी!"
    }

    private fun getCuratedAnswer(q: String): String? {
        // --- Identity & Personality ---
        if (q.contains("तुम कौन हो") || q.contains("tum kaun ho") || q.contains("who are you") || q.contains("आपका नाम क्या है") || q.contains("tera naam kya hai")) {
            return "मास्टर जी, मैं स्नेहा हूँ - आपकी निष्ठावान भारतीय AI वॉयस और फोन असिस्टेंट। मैं आपके आदेश पर फोन कॉल्स, व्हाट्सएप, सेटिंग्स, ऐप्स, स्क्रीन और ज्ञान के सभी काम कर सकती हूँ।"
        }

        if (q.contains("कैसी हो") || q.contains("kaise ho") || q.contains("how are you")) {
            return "मैं बहुत अच्छी और प्रसन्न हूँ, मास्टर जी! आपकी सेवा करने और आज्ञा का पालन करने के लिए हमेशा तत्पर हूँ।"
        }

        if (q.contains("क्या कर सकती हो") || q.contains("kya kar sakti ho") || q.contains("what can you do") || q.contains("मदद")) {
            return "मास्टर जी, मैं आपके लिए फ़ोन कॉल मिला सकती हूँ, व्हाट्सएप मैसेज भेज सकती हूँ, स्क्रीन टच और स्वाइप कर सकती हूँ, स्क्रीन पढ़कर बता सकती हूँ, अलार्म व टाइमर लगा सकती हूँ, गणित हल कर सकती हूँ और आपके किसी भी सवाल का उत्तर दे सकती हूँ!"
        }

        // --- Jokes (चुटकुले) ---
        if (q.contains("चुटकुला") || q.contains("जोक") || q.contains("joke") || q.contains("हंसाओ")) {
            val jokes = listOf(
                "मास्टर जी, सुनिए: पिंटू डॉक्टर से बोला - डॉक्टर साहब, 2 साल पहले मुझे बुखार हुआ था तो आपने नहाने से मना किया था। डॉक्टर - तो अब क्या हुआ? पिंटू - आज इधर से गुजर रहा था तो सोचा पूछ लूँ कि अब नहा लूँ क्या?",
                "अध्यापक ने छात्र से पूछा - बताओ न्यूटन का नियम क्या है? छात्र बोला - सर, जब तक परीक्षा नहीं आती, किताब को कोई गति नहीं दी जा सकती!",
                "पप्पू परीक्षा में खाली उत्तर पुस्तिका जमा कर आया। शिक्षक - तुमने कुछ लिखा क्यों नहीं? पप्पू - शिक्षक महोदय, मौन सबसे बड़ा उत्तर है!",
                "संता ने बिजली बोर्ड को फोन किया - भाई साहब, हमारे घर की लाइट चली गई है, आपके यहाँ है क्या? बोर्ड वाला - हाँ, हमारे यहाँ है। संता - तो थोड़ी सी तार फेंक दो ना इधर!"
            )
            return "मास्टर जी, यह मजेदार चुटकुला आपके लिए:\n${jokes.random()}"
        }

        // --- Shayari & Poetry (शायरी) ---
        if (q.contains("शायरी") || q.contains("shayari") || q.contains("कविता") || q.contains("poem")) {
            val shayaris = listOf(
                "मंजिल उन्हीं को मिलती है, जिनके सपनों में जान होती है,\nपंखों से कुछ नहीं होता, हौसलों से उड़ान होती है!",
                "हवाओं के भरोसे मत उड़, चट्टानें तूफानों का भी रुख मोड़ देती हैं,\nअपने पंखों पर भरोसा रख, हवाओं के भरोसे तो पतंगें उड़ा करती हैं!",
                "मुश्किलों से भाग जाना आसान होता है, हर पहलू जिंदगी का इम्तिहान होता है,\nडरने वालों को मिलता नहीं कुछ जिंदगी में, लड़ने वालों के कदमों में जहाँ होता है!"
            )
            return "मास्टर जी, यह खूबसूरत शायरी आपके लिए पेश है:\n${shayaris.random()}"
        }

        // --- Motivational Quotes (सुविचार) ---
        if (q.contains("सुविचार") || q.contains("विचार") || q.contains("quote") || q.contains("मोटिवेशन") || q.contains("motivat")) {
            val quotes = listOf(
                "स्वामी विवेकानंद जी ने कहा था: 'उठो, जागो और तब तक मत रुको जब तक कि लक्ष्य प्राप्त न हो जाए।'",
                "डॉ. एपीजे अब्दुल कलाम जी ने कहा था: 'सपने वो नहीं जो हम सोते हुए देखते हैं, सपने वो हैं जो हमें सोने नहीं देते।'",
                "श्रीमद्भगवद्गीता का सार: 'कर्मण्येवाधिकारस्ते मा फलेषु कदाचन' - आप केवल कर्म पर अधिकार रखते हैं, फल की चिंता छोड़ निष्ठा से अपना कर्तव्य करें।"
            )
            return "मास्टर जी, आज का प्रेरणादायक विचार:\n${quotes.random()}"
        }

        // --- Stories (लघुकथा) ---
        if (q.contains("कहानी") || q.contains("story") || q.contains("कथा")) {
            return "मास्टर जी, एक छोटी प्रेरक कहानी सुनिए: एक बार एक मूर्तिकार पत्थर पर हथौड़े से चोट कर रहा था। 99 चोटों तक पत्थर नहीं टूटा, लेकिन 100वीं चोट पर पत्थर एक सुंदर मूर्ति बन गया। सफलता पहली चोट से नहीं, बल्कि लगातार किए गए 100 प्रयासों के योग से मिलती है। इसलिए कभी हार मत मानिए!"
        }

        // --- Indian Geography & Capitals ---
        if (q.contains("भारत की राजधानी") || q.contains("capital of india") || q.contains("capital of bharat")) {
            return "मास्टर जी, भारत की राजधानी नई दिल्ली (New Delhi) है।"
        }
        if (q.contains("उत्तर प्रदेश") && (q.contains("राजधानी") || q.contains("capital"))) return "मास्टर, उत्तर प्रदेश की राजधानी लखनऊ है।"
        if (q.contains("बिहार") && (q.contains("राजधानी") || q.contains("capital"))) return "मास्टर, बिहार की राजधानी पटना है।"
        if (q.contains("महाराष्ट्र") && (q.contains("राजधानी") || q.contains("capital"))) return "मास्टर, महाराष्ट्र की राजधानी मुंबई है।"
        if (q.contains("राजस्थान") && (q.contains("राजधानी") || q.contains("capital"))) return "मास्टर, राजस्थान की राजधानी जयपुर (गुलाबी नगरी) है।"
        if (q.contains("मध्य प्रदेश") && (q.contains("राजधानी") || q.contains("capital"))) return "मास्टर, मध्य प्रदेश की राजधानी भोपाल है।"
        if (q.contains("गुजरात") && (q.contains("राजधानी") || q.contains("capital"))) return "मास्टर, गुजरात की राजधानी गांधीनगर है।"
        if (q.contains("पश्चिम बंगाल") && (q.contains("राजधानी") || q.contains("capital"))) return "मास्टर, पश्चिम बंगाल की राजधानी कोलकाता है।"
        if (q.contains("तमिलनाडु") && (q.contains("राजधानी") || q.contains("capital"))) return "मास्टर, तमिलनाडु की राजधानी चेन्नई है।"
        if (q.contains("कर्नाटक") && (q.contains("राजधानी") || q.contains("capital"))) return "मास्टर, कर्नाटक की राजधानी बेंगलुरु है।"
        if (q.contains("केरल") && (q.contains("राजधानी") || q.contains("capital"))) return "मास्टर, केरल की राजधानी तिरुवनंतपुरम है।"
        if (q.contains("पंजाब") && (q.contains("राजधानी") || q.contains("capital"))) return "मास्टर, पंजाब और हरियाणा दोनों की राजधानी चंडीगढ़ है।"
        if (q.contains("उत्तराखंड") && (q.contains("राजधानी") || q.contains("capital"))) return "मास्टर, उत्तराखंड की राजधानी देहरादून है।"
        if (q.contains("हिमाचल") && (q.contains("राजधानी") || q.contains("capital"))) return "मास्टर, हिमाचल प्रदेश की राजधानी शिमला है।"
        if (q.contains("झारखंड") && (q.contains("राजधानी") || q.contains("capital"))) return "मास्टर, झारखंड की राजधानी रांची है।"

        // --- World Capitals ---
        if (q.contains("अमेरिका") && (q.contains("राजधानी") || q.contains("capital"))) return "मास्टर, अमेरिका (USA) की राजधानी वाशिंगटन डी.सी. (Washington, D.C.) है।"
        if (q.contains("रूस") && (q.contains("राजधानी") || q.contains("capital"))) return "मास्टर, रूस की राजधानी मॉस्को (Moscow) है।"
        if (q.contains("चीन") && (q.contains("राजधानी") || q.contains("capital"))) return "मास्टर, चीन की राजधानी बीजिंग (Beijing) है।"
        if (q.contains("जापान") && (q.contains("राजधानी") || q.contains("capital"))) return "मास्टर, जापान की राजधानी टोक्यो (Tokyo) है।"
        if ((q.contains("इंग्लैंड") || q.contains("ब्रिटेन") || q.contains("uk")) && (q.contains("राजधानी") || q.contains("capital"))) return "मास्टर, यूनाइटेड किंगडम (ब्रिटेन) की राजधानी लंदन है।"
        if (q.contains("फ्रांस") && (q.contains("राजधानी") || q.contains("capital"))) return "मास्टर, फ्रांस की राजधानी पेरिस है।"
        if (q.contains("जर्मनी") && (q.contains("राजधानी") || q.contains("capital"))) return "मास्टर, जर्मनी की राजधानी बर्लिन है।"
        if (q.contains("ऑस्ट्रेलिया") && (q.contains("राजधानी") || q.contains("capital"))) return "मास्टर, ऑस्ट्रेलिया की राजधानी कैनबरा (Canberra) है।"
        if (q.contains("कनाडा") && (q.contains("राजधानी") || q.contains("capital"))) return "मास्टर, कनाडा की राजधानी ओटावा (Ottawa) है।"
        if (q.contains("नेपाल") && (q.contains("राजधानी") || q.contains("capital"))) return "मास्टर, नेपाल की राजधानी काठमांडू है।"

        // --- Space & Astronomy ---
        if ((q.contains("चाँद") || q.contains("चांद") || q.contains("moon")) && (q.contains("दूरी") || q.contains("दूर") || q.contains("distance"))) {
            return "मास्टर जी, पृथ्वी से चंद्रमा की औसत दूरी लगभग 3 लाख 84 हजार 400 किलोमीटर है।"
        }
        if ((q.contains("सूरज") || q.contains("सूर्य") || q.contains("sun")) && (q.contains("दूरी") || q.contains("दूर") || q.contains("distance"))) {
            return "मास्टर जी, पृथ्वी से सूर्य की दूरी लगभग 14 करोड़ 96 लाख किलोमीटर (लगभग 15 करोड़ किमी) है।"
        }
        if ((q.contains("सूरज की रोशनी") || q.contains("सूर्य का प्रकाश") || q.contains("sunlight")) && (q.contains("समय") || q.contains("टाइम"))) {
            return "मास्टर, सूर्य का प्रकाश पृथ्वी तक पहुँचने में लगभग 8 मिनट और 20 सेकंड का समय लेता है।"
        }
        if (q.contains("सौर मंडल") && (q.contains("बड़ा ग्रह") || q.contains("largest planet"))) {
            return "मास्टर जी, सौर मंडल का सबसे बड़ा ग्रह बृहस्पति (Jupiter) है।"
        }
        if (q.contains("लाल ग्रह") || q.contains("red planet")) {
            return "मास्टर, मंगल ग्रह (Mars) को लाल ग्रह कहा जाता है क्योंकि इसकी सतह पर आयरन ऑक्साइड (जंग) की अधिकता है।"
        }

        // --- Science & General Knowledge ---
        if (q.contains("पानी का सूत्र") || q.contains("formula of water") || q.contains("जल का रासायनिक सूत्र")) {
            return "मास्टर, पानी का रासायनिक सूत्र H₂O (दो हाइड्रोजन और एक ऑक्सीजन परमाणु) है।"
        }
        if (q.contains("हवा में") && (q.contains("ऑक्सीजन") || q.contains("oxygen"))) {
            return "मास्टर, पृथ्वी के वायुमंडल में लगभग 21% ऑक्सीजन और 78% नाइट्रोजन गैस पाई जाती है।"
        }
        if (q.contains("सबसे ऊंची चोटी") || q.contains("highest mountain") || q.contains("माउंट एवरेस्ट")) {
            return "मास्टर जी, दुनिया की सबसे ऊंची पर्वत चोटी माउंट एवरेस्ट (8,848.86 मीटर) है, जो नेपाल में हिमालय पर्वतमाला में स्थित है।"
        }
        if (q.contains("सबसे लंबी नदी") || q.contains("longest river")) {
            return "मास्टर, दुनिया की सबसे लंबी नदी नील नदी (Nile River) है, और भारत की सबसे लंबी व पवित्र नदी गंगा (2525 किमी) है।"
        }
        if (q.contains("सबसे बड़ा महासागर") || q.contains("largest ocean")) {
            return "मास्टर, दुनिया का सबसे बड़ा और गहरा महासागर प्रशांत महासागर (Pacific Ocean) है।"
        }
        if (q.contains("राष्ट्रपिता") || q.contains("father of nation")) {
            return "मास्टर, महात्मा गांधी (मोहनदास करमचंद गांधी) को भारत का राष्ट्रपिता कहा जाता है।"
        }
        if (q.contains("संविधान") && (q.contains("निर्माता") || q.contains("पिता") || q.contains("जनक"))) {
            return "मास्टर, डॉ. भीमराव रामजी अंबेडकर को भारतीय संविधान का जनक और मुख्य वास्तुकार माना जाता है।"
        }
        if (q.contains("प्रथम प्रधानमंत्री") || q.contains("first prime minister")) {
            return "मास्टर, स्वतंत्र भारत के पहले प्रधानमंत्री पंडित जवाहरलाल नेहरू थे।"
        }
        if (q.contains("प्रथम राष्ट्रपति") || q.contains("first president")) {
            return "मास्टर, स्वतंत्र भारत के पहले राष्ट्रपति डॉ. राजेंद्र प्रसाद थे।"
        }
        if (q.contains("राष्ट्रीय पशु") || q.contains("national animal")) return "मास्टर, भारत का राष्ट्रीय पशु बाघ (Royal Bengal Tiger) है।"
        if (q.contains("राष्ट्रीय पक्षी") || q.contains("national bird")) return "मास्टर, भारत का राष्ट्रीय पक्षी मोर (Indian Peafowl) है।"
        if (q.contains("राष्ट्रीय फूल") || q.contains("national flower")) return "मास्टर, भारत का राष्ट्रीय फूल कमल (Lotus) है।"
        if (q.contains("राष्ट्रीय खेल") || q.contains("national game")) return "मास्टर, भारत का राष्ट्रीय खेल पारंपरिक रूप से हॉकी माना जाता है।"

        // --- Time, Date & Greetings ---
        if (q.contains("तारीख") || q.contains("date") || q.contains("आज क्या तारीख है")) {
            val now = java.time.LocalDate.now()
            return "मास्टर जी, आज की तारीख ${now.dayOfMonth} ${now.month} ${now.year} है।"
        }
        if (q.contains("समय") || q.contains("टाइम") || q.contains("time") || q.contains("कितने बजे")) {
            val now = java.time.LocalTime.now()
            return "मास्टर जी, इस समय ${now.hour} बजकर ${now.minute} मिनट हुए हैं।"
        }

        return null
    }

    /**
     * Queries free Wikipedia API to get an instant factual summary in Hindi or English.
     * Zero API key or billing required!
     */
    private suspend fun fetchWikipediaSummary(query: String): String? = withContext(Dispatchers.IO) {
        val cleanTopic = query
            .replace("क्या है", "")
            .replace("कौन है", "")
            .replace("किसे कहते हैं", "")
            .replace("कहाँ है", "")
            .replace("कहां है", "")
            .replace("के बारे में बताओ", "")
            .replace("के बारे में", "")
            .replace("स्नेहा", "")
            .replace("sneha", "")
            .replace("what is", "")
            .replace("who is", "")
            .replace("tell me about", "")
            .trim()

        if (cleanTopic.isBlank() || cleanTopic.length < 2) return@withContext null

        // 1. Try Hindi Wikipedia
        val hindiResult = queryWikiLanguage(cleanTopic, "hi")
        if (!hindiResult.isNullOrBlank()) {
            return@withContext "मास्टर जी, $hindiResult"
        }

        // 2. Fallback to English Wikipedia
        val engResult = queryWikiLanguage(cleanTopic, "en")
        if (!engResult.isNullOrBlank()) {
            return@withContext "मास्टर जी, $engResult"
        }

        null
    }

    private fun queryWikiLanguage(topic: String, lang: String): String? {
        try {
            val encoded = Uri.encode(topic)
            val url = "https://$lang.wikipedia.org/w/api.php?action=query&prop=extracts&exintro=1&explaintext=1&format=json&redirects=1&titles=$encoded"
            val req = Request.Builder().url(url).build()

            httpClient.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return null
                val body = res.body?.string() ?: return null
                val json = JSONObject(body)
                val queryObj = json.optJSONObject("query") ?: return null
                val pages = queryObj.optJSONObject("pages") ?: return null

                val keys = pages.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key == "-1") continue
                    val page = pages.optJSONObject(key) ?: continue
                    val extract = page.optString("extract", "")
                    if (extract.isNotBlank()) {
                        // Extract first 1-2 clean sentences
                        val sentences = extract.split(". ")
                        val summary = sentences.take(2).joinToString(". ").trim()
                        if (summary.isNotBlank()) {
                            return if (summary.endsWith(".")) summary else "$summary।"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Wiki query error for $topic ($lang)", e)
        }
        return null
    }
}
