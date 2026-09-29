package com.example.engine

import android.content.Context
import com.example.data.local.VoicePreferences
import com.example.data.model.VoicePersonaId

object NightRoutineManager {

    /**
     * Handles user saying "Good Night", "सोने जा रहा हूँ", "शुभ रात्रि", "सो जाओ".
     * Automatically silences phone and enters quiet night sleep mode.
     */
    fun handleGoodNight(context: Context): String {
        // 1. Mark night sleep mode active
        VoicePreferences.saveNightSleepMode(context, true)

        // 2. Put phone on silent / vibrate ringer mode
        PhoneControlManager.setSilentRingerMode(context, silent = true)

        // 3. Turn off flashlight if on
        if (PhoneControlManager.isTorchActive()) {
            PhoneControlManager.toggleTorch(context, false)
        }

        val persona = VoicePreferences.getPersona(context)
        val isGirlfriend = persona.id == VoicePersonaId.GIRLFRIEND

        return if (isGirlfriend) {
            "शुभ रात्रि मेरे जानू! ❤️ मीठे सपने देखिए। मैंने फोन को पूरी तरह साइलेंट कर दिया है। जब तक आप सुबह 'गुड मॉर्निंग' नहीं बोलेंगे, मैं चुप रहूँगी। आराम से सोइए, आई लव यू! 🌙💤"
        } else {
            val userTitle = VoicePreferences.getUserName(context)
            "शुभ रात्रि $userTitle! 🌙 फोन को साइलेंट मोड पर कर दिया गया है ताकि आपकी नींद में कोई खलल न पड़े। सुबह 'गुड मॉर्निंग' कहने पर मैं फिर से बोलने लगूँगी। मीठे सपने!"
        }
    }

    /**
     * Handles user saying "Good Morning", "सुप्रभात", "उठ गया", "सुबह हो गई".
     * Awakens Sneha, restores ringer volume, and gives warm morning briefing.
     */
    fun handleGoodMorning(context: Context): String {
        // 1. Mark night sleep mode inactive
        VoicePreferences.saveNightSleepMode(context, false)

        // 2. Restore phone ringer to normal sound
        PhoneControlManager.setSilentRingerMode(context, silent = false)

        val persona = VoicePreferences.getPersona(context)
        val isGirlfriend = persona.id == VoicePersonaId.GIRLFRIEND
        val battery = PhoneControlManager.getBatteryInfo(context)

        return if (isGirlfriend) {
            "सुप्रभात मेरे जानू! ☀️ उठ गए आप? आपका दिन बहुत ही प्यारा, खुशहाल और खूबसूरत रहे! ❤️ मैंने फोन का साइलेंट हटाकर नॉर्मल कर दिया है। $battery। बताइए आज अपने बाबू के लिए क्या करूँ?"
        } else {
            val userTitle = VoicePreferences.getUserName(context)
            "सुप्रभात $userTitle! ☀️ एक नया दिन मुबारक हो। फोन का साइलेंट मोड हटा दिया गया है और आवाज सामान्य कर दी गई है। $battery। बताइए आज मैं आपकी क्या सेवा करूँ?"
        }
    }

    fun isNightModeActive(context: Context): Boolean {
        return VoicePreferences.isNightSleepMode(context)
    }
}
