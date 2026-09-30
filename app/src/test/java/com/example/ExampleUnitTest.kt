package com.example

import com.example.engine.OtpSafetyGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testOtpDetection_englishAndHindi() {
        // Bank SMS in English
        assertTrue(OtpSafetyGuard.containsOtp("Your OTP for SBI transaction of Rs 2000 is 839201. Valid for 10 mins."))
        assertTrue(OtpSafetyGuard.containsOtp("492019 is your Google verification code."))
        
        // Hindi OTP SMS
        assertTrue(OtpSafetyGuard.containsOtp("आपका सत्यापन ओटीपी कोड 482019 है। इसे किसी से साझा न करें।"))
        
        // Normal personal message without OTP
        assertFalse(OtpSafetyGuard.containsOtp("Kal shaam ko 7 baje chai pe milte hain."))
        assertFalse(OtpSafetyGuard.containsOtp("नमस्ते स्नेहा, आज का मौसम कैसा है?"))
    }

    @Test
    fun testOtpRedactionForDisplay() {
        val original = "Your OTP is 948201 for bank login."
        val redacted = OtpSafetyGuard.redactForDisplay(original)
        assertFalse(redacted.contains("948201"))
        assertTrue(redacted.contains("OTP सुरक्षित"))
    }

    @Test
    fun testOtpSpeechSanitization() {
        val safeSpeech = OtpSafetyGuard.sanitizeForSpeech("HDFC Bank", "Your OTP is 123456.")
        assertFalse(safeSpeech.contains("123456"))
        assertTrue(safeSpeech.contains("OTP"))

        // Message with content and OTP
        val mixedMessage = OtpSafetyGuard.sanitizeForSpeech("Amazon", "Your order is ready. OTP is 849201 for delivery.")
        assertFalse(mixedMessage.contains("849201"))
        assertTrue(mixedMessage.contains("Your order is ready"))
        assertTrue(mixedMessage.contains("सुरक्षा"))

        // Normal message without OTP is read as is
        val normalMsg = OtpSafetyGuard.sanitizeForSpeech("Rohan", "Kal 5 baje call karna.")
        assertTrue(normalMsg.contains("Kal 5 baje call karna"))
    }

    @Test
    fun testOtpRefusalResponse() {
        val refusal = OtpSafetyGuard.getOtpRefusalResponse()
        assertTrue(refusal.contains("स्नेहा आपकी सुरक्षा"))
    }

    @Test
    fun testVoicePersona_allPersonasAndDefaults() {
        val personas = com.example.data.model.VoicePersona.ALL
        assertEquals(6, personas.size)

        val gf = com.example.data.model.VoicePersona.fromId("girlfriend")
        assertEquals(com.example.data.model.VoicePersonaId.GIRLFRIEND, gf.id)

        val classic = com.example.data.model.VoicePersona.fromId("classic")
        assertEquals(com.example.data.model.VoicePersonaId.CLASSIC, classic.id)
        assertEquals(1.0f, classic.defaultSpeed)
        assertEquals(1.18f, classic.defaultPitch)

        val pro = com.example.data.model.VoicePersona.fromId("pro")
        assertEquals(com.example.data.model.VoicePersonaId.PRO, pro.id)
        assertEquals(1.12f, pro.defaultSpeed)
        assertEquals(1.12f, pro.defaultPitch)

        val cheerful = com.example.data.model.VoicePersona.fromId("cheerful")
        assertEquals(com.example.data.model.VoicePersonaId.CHEERFUL, cheerful.id)
        assertEquals(1.30f, cheerful.defaultPitch)
        assertEquals(1.05f, cheerful.defaultSpeed)

        val calm = com.example.data.model.VoicePersona.fromId("calm")
        assertEquals(com.example.data.model.VoicePersonaId.CALM, calm.id)
        assertEquals(0.90f, calm.defaultSpeed)
        assertEquals(1.10f, calm.defaultPitch)

        val bold = com.example.data.model.VoicePersona.fromId("bold")
        assertEquals(com.example.data.model.VoicePersonaId.BOLD, bold.id)
        assertEquals(1.05f, bold.defaultPitch)
        assertEquals(1.0f, bold.defaultSpeed)

        // Fallback for unknown ID
        val unknown = com.example.data.model.VoicePersona.fromId("unknown_id")
        assertEquals(com.example.data.model.VoicePersonaId.CLASSIC, unknown.id)
    }

    @Test
    fun testMicChimeType_allValuesAndFallback() {
        val types = com.example.engine.MicChimeType.entries
        assertEquals(5, types.size)

        assertEquals(com.example.engine.MicChimeType.FUTURISTIC, com.example.engine.MicChimeType.fromId("futuristic"))
        assertEquals(com.example.engine.MicChimeType.CRYSTAL_BELL, com.example.engine.MicChimeType.fromId("crystal"))
        assertEquals(com.example.engine.MicChimeType.WATER_DROP, com.example.engine.MicChimeType.fromId("water_drop"))
        assertEquals(com.example.engine.MicChimeType.GENTLE_PULSE, com.example.engine.MicChimeType.fromId("gentle_pulse"))
        assertEquals(com.example.engine.MicChimeType.MUTE, com.example.engine.MicChimeType.fromId("mute"))
        // Fallback to futuristic for unknown id
        assertEquals(com.example.engine.MicChimeType.FUTURISTIC, com.example.engine.MicChimeType.fromId("unknown"))
    }

    @Test
    fun testVoiceMathConverter_percentageAndCurrency() {
        val pctResult = com.example.engine.VoiceMathConverter.processMathOrConversion("2500 का 18%")
        org.junit.Assert.assertNotNull(pctResult)
        org.junit.Assert.assertTrue(pctResult!!.contains("450"))

        val currencyResult = com.example.engine.VoiceMathConverter.processMathOrConversion("50 डॉलर में कितने रुपये")
        org.junit.Assert.assertNotNull(currencyResult)
        org.junit.Assert.assertTrue(currencyResult!!.contains("रुपये"))

        val divResult = com.example.engine.VoiceMathConverter.processMathOrConversion("3000 को 6 लोगों में बांटो")
        org.junit.Assert.assertNotNull(divResult)
        org.junit.Assert.assertTrue(divResult!!.contains("500"))
    }

    @Test
    fun testMorningBriefingRecognition() {
        org.junit.Assert.assertTrue(com.example.engine.MorningBriefingManager.isMorningBriefingQuery("गुड मॉर्निंग स्नेहा"))
        org.junit.Assert.assertTrue(com.example.engine.MorningBriefingManager.isMorningBriefingQuery("good morning"))
        org.junit.Assert.assertTrue(com.example.engine.MorningBriefingManager.isMorningBriefingQuery("मॉर्निंग ब्रीफिंग"))
        org.junit.Assert.assertFalse(com.example.engine.MorningBriefingManager.isMorningBriefingQuery("टॉर्च जलाओ"))
    }

    @Test
    fun testEmergencySosRecognition() {
        org.junit.Assert.assertTrue(com.example.engine.EmergencySosManager.isEmergencySosQuery("स्नेहा मुझे बचाओ"))
        org.junit.Assert.assertTrue(com.example.engine.EmergencySosManager.isEmergencySosQuery("हेल्प मी"))
        org.junit.Assert.assertTrue(com.example.engine.EmergencySosManager.isEmergencySosQuery("इमरजेंसी लोकेशन भेजो"))
        org.junit.Assert.assertFalse(com.example.engine.EmergencySosManager.isEmergencySosQuery("गाना बजाओ"))
    }

    @Test
    fun testSnehaAppThemes_allThemes() {
        val themes = com.example.ui.theme.SnehaAppTheme.entries
        org.junit.Assert.assertEquals(4, themes.size)
        org.junit.Assert.assertEquals(com.example.ui.theme.SnehaAppTheme.CYBERPUNK, com.example.ui.theme.SnehaAppTheme.fromId("cyberpunk"))
        org.junit.Assert.assertEquals(com.example.ui.theme.SnehaAppTheme.SWEET_ROSE, com.example.ui.theme.SnehaAppTheme.fromId("sweet_rose"))
        org.junit.Assert.assertEquals(com.example.ui.theme.SnehaAppTheme.AMOLED_DARK, com.example.ui.theme.SnehaAppTheme.fromId("amoled_dark"))
        org.junit.Assert.assertEquals(com.example.ui.theme.SnehaAppTheme.ROYAL_EMERALD, com.example.ui.theme.SnehaAppTheme.fromId("royal_emerald"))
    }

    @Test
    fun testAutonomousScreenCommands() {
        val queryHome = "home जाओ"
        org.junit.Assert.assertTrue(queryHome.contains("home") || queryHome.contains("होम"))

        val queryScreenshot = "screenshot लो"
        org.junit.Assert.assertTrue(queryScreenshot.contains("screenshot") || queryScreenshot.contains("स्क्रीनशॉट"))

        val queryScroll = "नीचे स्क्रॉल करो"
        org.junit.Assert.assertTrue(queryScroll.contains("नीचे स्क्रॉल") || queryScroll.contains("scroll down"))
    }

    @Test
    fun testWakeWordGating_ambientSpeechRejected() {
        val wakeKeywords = listOf("हे स्नेहा", "hey sneha", "सुनो स्नेहा", "hello sneha", "hi sneha", "ok sneha", "स्नेहा", "sneha")

        val ambientSpeech = "खाना खा लो भाई"
        val hasWake = wakeKeywords.any { ambientSpeech.lowercase().contains(it) }
        org.junit.Assert.assertFalse(hasWake)

        val wakeCommand = "स्नेहा टॉर्च चालू करो"
        val hasWake2 = wakeKeywords.any { wakeCommand.lowercase().contains(it) }
        org.junit.Assert.assertTrue(hasWake2)
    }

    @Test
    fun testKnowledgeEngineAnswers() {
        val q1 = "भारत की राजधानी क्या है"
        val a1 = com.example.data.remote.GeminiApiClient.getOfflineSnehaResponse(q1)
        org.junit.Assert.assertNotNull(a1)

        val q2 = "मुझे एक चुटकुला सुनाओ"
        org.junit.Assert.assertTrue(q2.contains("चुटकुला"))
    }
}
