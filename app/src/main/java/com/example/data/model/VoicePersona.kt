package com.example.data.model

enum class VoicePersonaId(val id: String) {
    CLASSIC("classic"),
    PRO("pro"),
    CHEERFUL("cheerful"),
    CALM("calm"),
    BOLD("bold")
}

data class VoicePersona(
    val id: VoicePersonaId,
    val nameHindi: String,
    val nameEnglish: String,
    val tagHindi: String,
    val descriptionHindi: String,
    val defaultPitch: Float,
    val defaultSpeed: Float,
    val sampleSpeech: String,
    val iconEmoji: String
) {
    companion object {
        val ALL: List<VoicePersona> = listOf(
            VoicePersona(
                id = VoicePersonaId.CLASSIC,
                nameHindi = "स्नेहा क्लासिक",
                nameEnglish = "Sneha Classic",
                tagHindi = "दोस्ताना और स्वाभाविक",
                descriptionHindi = "दैनिक बातचीत के लिए संतुलित, मधुर और स्पष्ट आवाज़।",
                defaultPitch = 1.05f,
                defaultSpeed = 1.0f,
                sampleSpeech = "नमस्ते! मैं स्नेहा हूँ। मैं आपके सारे काम प्यार और समझदारी से कर सकती हूँ।",
                iconEmoji = "🌸"
            ),
            VoicePersona(
                id = VoicePersonaId.PRO,
                nameHindi = "स्नेहा प्रो",
                nameEnglish = "Sneha Pro",
                tagHindi = "तेज़, स्पष्ट & पेशेवर",
                descriptionHindi = "कामकाजी और तकनीकी आदेशों के लिए फुर्तीली, गंभीर और क्रिस्प आवाज़।",
                defaultPitch = 0.95f,
                defaultSpeed = 1.18f,
                sampleSpeech = "नमस्कार। स्नेहा प्रो तैयार है। बताइए आज आपके लिए क्या कार्य निष्पादित करना है?",
                iconEmoji = "💼"
            ),
            VoicePersona(
                id = VoicePersonaId.CHEERFUL,
                nameHindi = "स्नेहा चुलबुली",
                nameEnglish = "Sneha Cheerful",
                tagHindi = "ऊर्जावान & उत्साही",
                descriptionHindi = "हंसमुख, जीवंत और उच्च सुर वाली प्रेरणादायक व दोस्ताना आवाज़।",
                defaultPitch = 1.25f,
                defaultSpeed = 1.06f,
                sampleSpeech = "अरे वाह! नमस्ते जी! मैं स्नेहा हूँ, पूरी ऊर्जा के साथ तैयार। चलिए कुछ नया करते हैं!",
                iconEmoji = "✨"
            ),
            VoicePersona(
                id = VoicePersonaId.CALM,
                nameHindi = "स्नेहा सौम्य",
                nameEnglish = "Sneha Calm",
                tagHindi = "शांत, कोमल & ध्यानमग्न",
                descriptionHindi = "धीमी गति और शांत सुर में सुकून व ध्यान देने वाली आवाज़।",
                defaultPitch = 0.90f,
                defaultSpeed = 0.85f,
                sampleSpeech = "नमस्ते। मैं स्नेहा। बिना किसी जल्दबाज़ी के, आराम से बताइए, मैं आपकी क्या सहायता करूँ?",
                iconEmoji = "🍃"
            ),
            VoicePersona(
                id = VoicePersonaId.BOLD,
                nameHindi = "स्नेहा प्रखर",
                nameEnglish = "Sneha Bold",
                tagHindi = "गंभीर, आत्मविश्वासी & मजबूत",
                descriptionHindi = "गहरे बेस टोन और दृढ़ शैली वाली औपचारिक व सुरक्षा-केन्द्रित आवाज़।",
                defaultPitch = 0.76f,
                defaultSpeed = 0.98f,
                sampleSpeech = "नमस्कार। मैं स्नेहा। सुरक्षा, अलर्ट और आपके आवश्यक कार्यों के लिए सदैव मुस्तैद।",
                iconEmoji = "🛡️"
            )
        )

        fun fromId(id: String?): VoicePersona {
            return ALL.firstOrNull { it.id.id.equals(id, ignoreCase = true) } ?: ALL[0]
        }
    }
}
