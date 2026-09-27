package com.example.engine

import java.util.regex.Pattern

object OtpSafetyGuard {

    // Regex to detect OTP related keywords in English and Hindi
    private val OTP_KEYWORDS = listOf(
        "otp", "one time password", "verification code", "security code",
        "secret code", "passcode", "v-code", "auth code", "pin",
        "ओटीपी", "सत्यापन कोड", "पासवर्ड", "पिन", "गुप्त कोड", "सुरक्षा कोड"
    )

    // Regex for standalone 4-8 digit numeric codes
    private val DIGIT_CODE_PATTERN = Pattern.compile("(?i)\\b(\\d{4,8})\\b")
    
    // Regex for alphanumeric codes like A-123456 or G-849201
    private val PREFIXED_CODE_PATTERN = Pattern.compile("(?i)\\b([A-Z]{1,3}[-\\s]?\\d{4,8})\\b")

    /**
     * Checks if the message text contains an OTP or verification code.
     */
    fun containsOtp(text: String): Boolean {
        if (text.isBlank()) return false
        val lower = text.lowercase()

        val hasKeyword = OTP_KEYWORDS.any { lower.contains(it) }
        val hasDigits = DIGIT_CODE_PATTERN.matcher(text).find()

        // If it explicitly mentions OTP/verification keywords and has numbers, it's definitely OTP
        if (hasKeyword && hasDigits) return true

        // If it comes with bank or service markers (SBI, HDFC, Google, WhatsApp code, etc.)
        val bankingOrAuthMarkers = listOf("bank", "a/c", "card", "login", "transaction", "debit", "credit", "खाता", "बैंक")
        val hasBankMarker = bankingOrAuthMarkers.any { lower.contains(it) }
        if (hasBankMarker && hasDigits) return true

        // If text specifically says "code is 123456" or "OTP is 1234"
        if (lower.contains("is ") || lower.contains("hai ") || lower.contains("है ")) {
            if (hasKeyword) return true
        }

        return false
    }

    /**
     * Replaces OTP digits and codes with a safe masked placeholder for visual UI display.
     */
    fun redactForDisplay(text: String): String {
        if (!containsOtp(text)) return text

        var sanitized = text
        // Replace digit sequences if OTP keywords exist
        val matcher = DIGIT_CODE_PATTERN.matcher(sanitized)
        sanitized = matcher.replaceAll("[🔒 OTP सुरक्षित]")
        
        val prefixedMatcher = PREFIXED_CODE_PATTERN.matcher(sanitized)
        sanitized = prefixedMatcher.replaceAll("[🔒 कोड सुरक्षित]")

        return sanitized
    }

    /**
     * Produces audio-safe spoken text: skips any secret numbers and explicitly warns that OTP was redacted.
     */
    fun sanitizeForSpeech(sender: String, originalText: String): String {
        if (containsOtp(originalText)) {
            // Check if there is meaningful text besides the OTP digits
            var textWithoutOtp = originalText
            val matcher = DIGIT_CODE_PATTERN.matcher(textWithoutOtp)
            textWithoutOtp = matcher.replaceAll("")
            val prefixedMatcher = PREFIXED_CODE_PATTERN.matcher(textWithoutOtp)
            textWithoutOtp = prefixedMatcher.replaceAll("")
            
            val cleaned = textWithoutOtp.replace(Regex("\\s+"), " ").trim()
            return if (cleaned.length > 8) {
                "संदेश $sender की तरफ से आया है: $cleaned। ध्यान दें कि सुरक्षा कारणों से गुप्त OTP नहीं पढ़ा गया है।"
            } else {
                "संदेश $sender की तरफ से आया है। इस संदेश में एक गोपनीय OTP या सुरक्षा कोड है, जिसे सुरक्षा कारणों से नहीं पढ़ा जाएगा।"
            }
        }
        return "संदेश $sender की तरफ से आया है: $originalText"
    }

    /**
     * Guard response if user directly asks Sneha to read an OTP.
     */
    fun getOtpRefusalResponse(): String {
        return "माफ़ कीजिये! स्नेहा आपकी सुरक्षा और गोपनीयता का पूरा सम्मान करती है। बैंकिंग और सुरक्षा कारणों से मैं कोई भी OTP या गुप्त पासवर्ड नहीं पढ़ सकती।"
    }
}
