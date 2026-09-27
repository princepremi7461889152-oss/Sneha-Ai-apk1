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
        assertEquals(5, personas.size)

        val classic = com.example.data.model.VoicePersona.fromId("classic")
        assertEquals(com.example.data.model.VoicePersonaId.CLASSIC, classic.id)
        assertEquals(1.0f, classic.defaultSpeed)
        assertEquals(1.05f, classic.defaultPitch)

        val pro = com.example.data.model.VoicePersona.fromId("pro")
        assertEquals(com.example.data.model.VoicePersonaId.PRO, pro.id)
        assertEquals(1.18f, pro.defaultSpeed)

        val cheerful = com.example.data.model.VoicePersona.fromId("cheerful")
        assertEquals(com.example.data.model.VoicePersonaId.CHEERFUL, cheerful.id)
        assertEquals(1.25f, cheerful.defaultPitch)

        val calm = com.example.data.model.VoicePersona.fromId("calm")
        assertEquals(com.example.data.model.VoicePersonaId.CALM, calm.id)
        assertEquals(0.85f, calm.defaultSpeed)

        val bold = com.example.data.model.VoicePersona.fromId("bold")
        assertEquals(com.example.data.model.VoicePersonaId.BOLD, bold.id)
        assertEquals(0.76f, bold.defaultPitch)

        // Fallback for unknown ID
        val unknown = com.example.data.model.VoicePersona.fromId("unknown_id")
        assertEquals(com.example.data.model.VoicePersonaId.CLASSIC, unknown.id)
    }
}
