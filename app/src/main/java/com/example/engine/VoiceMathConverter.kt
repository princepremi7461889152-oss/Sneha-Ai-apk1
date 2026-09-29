package com.example.engine

import java.text.DecimalFormat
import java.util.Locale

object VoiceMathConverter {

    private val df = DecimalFormat("#,##0.##")

    // Approximate baseline exchange rates to INR
    private const val USD_TO_INR = 83.50
    private const val EUR_TO_INR = 91.20
    private const val GBP_TO_INR = 106.80
    private const val AED_TO_INR = 22.75
    private const val SAR_TO_INR = 22.25
    private const val CAD_TO_INR = 61.30

    /**
     * Attempts to parse voice queries related to calculations, percentages, currency, or units.
     * Returns a spoken Hindi response if matched, or null otherwise.
     */
    fun processMathOrConversion(rawQuery: String): String? {
        val query = rawQuery.lowercase(Locale.ROOT).trim()

        // 1. Percentage check: e.g. "2500 का 18%", "5000 ka 10 percent", "500 ka 20 pratishat"
        val percentRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:का|ka|of)\s*(\d+(?:\.\d+)?)\s*(?:%|प्रतिशत|pratishat|percent)""")
        percentRegex.find(query)?.let { match ->
            val total = match.groupValues[1].toDoubleOrNull()
            val rate = match.groupValues[2].toDoubleOrNull()
            if (total != null && rate != null) {
                val result = (total * rate) / 100.0
                return "मास्टर, ${df.format(total)} का ${df.format(rate)}% होगा ${df.format(result)}।"
            }
        }

        // Percentage reverse form: "18% of 2500" or "18 percent of 500"
        val percentReverseRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:%|percent|प्रतिशत)\s*(?:of|का|ka)\s*(\d+(?:\.\d+)?)""")
        percentReverseRegex.find(query)?.let { match ->
            val rate = match.groupValues[1].toDoubleOrNull()
            val total = match.groupValues[2].toDoubleOrNull()
            if (rate != null && total != null) {
                val result = (total * rate) / 100.0
                return "मास्टर, ${df.format(total)} का ${df.format(rate)}% होगा ${df.format(result)}।"
            }
        }

        // 2. Currency Conversions
        // USD to INR: "50 डॉलर", "100 dollar in inr", "50 usd"
        val usdRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:डॉलर|dollar|usd|dollars)""")
        if (query.contains("रुपये") || query.contains("rupee") || query.contains("inr") || query.contains("kitna") || query.contains("कितना")) {
            usdRegex.find(query)?.let { match ->
                val amount = match.groupValues[1].toDoubleOrNull()
                if (amount != null) {
                    val inr = amount * USD_TO_INR
                    return "$amount डॉलर लगभग ${df.format(inr)} भारतीय रुपये के बराबर है।"
                }
            }
        }

        // EUR to INR: "50 यूरो", "100 euro in inr"
        val euroRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:यूरो|euro|euros|eur)""")
        if (query.contains("रुपये") || query.contains("rupee") || query.contains("inr") || query.contains("kitna") || query.contains("कितना")) {
            euroRegex.find(query)?.let { match ->
                val amount = match.groupValues[1].toDoubleOrNull()
                if (amount != null) {
                    val inr = amount * EUR_TO_INR
                    return "$amount यूरो लगभग ${df.format(inr)} भारतीय रुपये के बराबर है।"
                }
            }
        }

        // AED to INR (Dirham): "500 दिरहम"
        val dirhamRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:दिरहम|dirham|aed)""")
        if (query.contains("रुपये") || query.contains("rupee") || query.contains("inr") || query.contains("kitna") || query.contains("कितना")) {
            dirhamRegex.find(query)?.let { match ->
                val amount = match.groupValues[1].toDoubleOrNull()
                if (amount != null) {
                    val inr = amount * AED_TO_INR
                    return "$amount दिरहम लगभग ${df.format(inr)} भारतीय रुपये के बराबर है।"
                }
            }
        }

        // SAR to INR (Riyal): "500 रियाल"
        val riyalRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:रियाल|riyal|sar)""")
        if (query.contains("रुपये") || query.contains("rupee") || query.contains("inr") || query.contains("kitna") || query.contains("कितना")) {
            riyalRegex.find(query)?.let { match ->
                val amount = match.groupValues[1].toDoubleOrNull()
                if (amount != null) {
                    val inr = amount * SAR_TO_INR
                    return "$amount रियाल लगभग ${df.format(inr)} भारतीय रुपये के बराबर है।"
                }
            }
        }

        // 3. Unit Conversions
        // Kilometer to Miles: "100 किलोमीटर में कितने मील"
        val kmRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:किलोमीटर|km|kilometre|kilometer)""")
        if (query.contains("मील") || query.contains("mile") || query.contains("miles")) {
            kmRegex.find(query)?.let { match ->
                val km = match.groupValues[1].toDoubleOrNull()
                if (km != null) {
                    val miles = km * 0.621371
                    return "$km किलोमीटर में लगभग ${df.format(miles)} मील (Miles) होते हैं।"
                }
            }
        }

        // Miles to Kilometer: "50 मील में कितने किलोमीटर"
        val milesRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:मील|miles|mile)""")
        if (query.contains("किलोमीटर") || query.contains("km") || query.contains("kilometre")) {
            milesRegex.find(query)?.let { match ->
                val miles = match.groupValues[1].toDoubleOrNull()
                if (miles != null) {
                    val km = miles * 1.60934
                    return "$miles मील में लगभग ${df.format(km)} किलोमीटर होते हैं।"
                }
            }
        }

        // KG to Pounds
        val kgRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:किलो|किलोग्राम|kg|kilo)""")
        if (query.contains("पाउंड") || query.contains("pound") || query.contains("lbs")) {
            kgRegex.find(query)?.let { match ->
                val kg = match.groupValues[1].toDoubleOrNull()
                if (kg != null) {
                    val pounds = kg * 2.20462
                    return "$kg किलोग्राम में लगभग ${df.format(pounds)} पाउंड (Pounds) होते हैं।"
                }
            }
        }

        // Temperature: Celsius to Fahrenheit
        val tempRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:डिग्री|degree)?\s*(?:सेल्सियस|celsius)""")
        if (query.contains("फॉरेनहाइट") || query.contains("fahrenheit")) {
            tempRegex.find(query)?.let { match ->
                val c = match.groupValues[1].toDoubleOrNull()
                if (c != null) {
                    val f = (c * 9.0 / 5.0) + 32.0
                    return "$c डिग्री सेल्सियस बराबर ${df.format(f)} डिग्री फॉरेनहाइट होता है।"
                }
            }
        }

        // 4. Basic Arithmetic Operations (जोड़, घटाव, गुणा, भाग)
        // Multiplication: "25 गुणा 4", "25 into 4", "25 * 4", "25 multiply by 4"
        val multRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:गुणा|गुना|into|\*|times|multiply|multiplied by)\s*(\d+(?:\.\d+)?)""")
        multRegex.find(query)?.let { match ->
            val a = match.groupValues[1].toDoubleOrNull()
            val b = match.groupValues[2].toDoubleOrNull()
            if (a != null && b != null) {
                val res = a * b
                return "$a गुणा $b बराबर ${df.format(res)} होगा।"
            }
        }

        // Division: "5000 भाग 6", "5000 divide by 6", "5000 / 6", "5000 ko 6 logo me banto"
        val divRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:भाग|divide|divided by|\/|बटे)\s*(\d+(?:\.\d+)?)""")
        divRegex.find(query)?.let { match ->
            val a = match.groupValues[1].toDoubleOrNull()
            val b = match.groupValues[2].toDoubleOrNull()
            if (a != null && b != null) {
                if (b == 0.0) return "शून्य (0) से भाग नहीं दिया जा सकता।"
                val res = a / b
                return "$a भाग $b बराबर ${df.format(res)} होगा।"
            }
        }

        // Split / Distribute: "5000 को 6 लोगों में बांटो"
        val splitRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:को|ko)\s*(\d+(?:\.\d+)?)\s*(?:लोगों|लोग|logon|hisson|हिस्सों)\s*(?:में|me)\s*(?:बांटो|baanto|divide)""")
        splitRegex.find(query)?.let { match ->
            val total = match.groupValues[1].toDoubleOrNull()
            val people = match.groupValues[2].toDoubleOrNull()
            if (total != null && people != null && people > 0) {
                val each = total / people
                return "प्रत्येक व्यक्ति के हिस्से में ${df.format(each)} रुपये आएंगे।"
            }
        }

        // Addition: "450 + 350", "450 प्लस 350", "450 aur 350 kitna hoga", "450 जोड़ 350"
        val addRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:\+|प्लस|plus|जोड़)\s*(\d+(?:\.\d+)?)""")
        addRegex.find(query)?.let { match ->
            val a = match.groupValues[1].toDoubleOrNull()
            val b = match.groupValues[2].toDoubleOrNull()
            if (a != null && b != null) {
                val res = a + b
                return "$a प्लस $b बराबर ${df.format(res)} होगा।"
            }
        }

        // Subtraction: "500 - 120", "500 माइनस 120", "500 घटाव 120"
        val subRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:\-|माइनस|minus|घटाव)\s*(\d+(?:\.\d+)?)""")
        subRegex.find(query)?.let { match ->
            val a = match.groupValues[1].toDoubleOrNull()
            val b = match.groupValues[2].toDoubleOrNull()
            if (a != null && b != null) {
                val res = a - b
                return "$a माइनस $b बराबर ${df.format(res)} होगा।"
            }
        }

        return null
    }
}
