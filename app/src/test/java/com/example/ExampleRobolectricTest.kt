package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Sneha", appName)
  }

  @Test
  fun `test voice preferences persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    
    // Test persona saving & loading
    val proPersona = com.example.data.model.VoicePersona.fromId("pro")
    com.example.data.local.VoicePreferences.savePersona(context, proPersona)
    val loadedPersona = com.example.data.local.VoicePreferences.getPersona(context)
    assertEquals(com.example.data.model.VoicePersonaId.PRO, loadedPersona.id)

    // Test speed rate saving & loading
    com.example.data.local.VoicePreferences.saveSpeechRate(context, 1.35f)
    val loadedRate = com.example.data.local.VoicePreferences.getSpeechRate(context)
    assertEquals(1.35f, loadedRate, 0.01f)

    // Test pitch saving & loading
    com.example.data.local.VoicePreferences.saveSpeechPitch(context, 1.22f)
    val loadedPitch = com.example.data.local.VoicePreferences.getSpeechPitch(context)
    assertEquals(1.22f, loadedPitch, 0.01f)

    // Test voice output toggle
    com.example.data.local.VoicePreferences.saveVoiceOutputEnabled(context, false)
    assertEquals(false, com.example.data.local.VoicePreferences.isVoiceOutputEnabled(context))
    com.example.data.local.VoicePreferences.saveVoiceOutputEnabled(context, true)
    assertEquals(true, com.example.data.local.VoicePreferences.isVoiceOutputEnabled(context))
  }
}
