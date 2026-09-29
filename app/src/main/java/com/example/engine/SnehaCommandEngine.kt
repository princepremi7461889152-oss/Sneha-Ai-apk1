package com.example.engine

import android.app.Activity
import android.content.Context
import com.example.data.local.SecurityUnlockPreferences
import com.example.data.local.VoicePreferences
import com.example.data.model.SnehaAction
import com.example.data.model.VoicePersona
import com.example.data.model.VoicePersonaId
import com.example.data.remote.GeminiApiClient

data class CommandResult(
    val spokenResponse: String,
    val actionTaken: SnehaAction? = null,
    val isOtpRefusal: Boolean = false
)

object SnehaCommandEngine {

    suspend fun processCommand(
        context: Context,
        activity: Activity?,
        rawInput: String,
        onReadMessagesRequest: (suspend () -> String)? = null
    ): CommandResult {
        val input = rawInput.trim()
        val lower = input.lowercase()
        val userTitle = VoicePreferences.getUserName(context)

        // 1. Calling by name / Wake words ("स्नेहा", "hey sneha", "सुनो स्नेहा", "हेलो स्नेहा", "नमस्ते स्नेहा", etc.)
        val isJustCalling = lower == "स्नेहा" || lower == "sneha" || lower == "hey sneha" ||
                lower == "हे स्नेहा" || lower == "सुनो स्नेहा" || lower == "सुनो" ||
                lower == "hello sneha" || lower == "hi sneha" || lower == "ok sneha" || lower == "siri" ||
                lower == "हेलो स्नेहा" || lower == "नमस्ते स्नेहा" || lower == "हाय स्नेहा" ||
                lower == "हेलो" || lower == "नमस्ते" || lower == "hello" || lower == "hi"
        if (isJustCalling) {
            return CommandResult(
                spokenResponse = "जी $userTitle! मैं हाजिर हूँ, आज्ञा दीजिए! मैं आपके लिए क्या करूँ?",
                actionTaken = SnehaAction.ConversationalAnswer("जी $userTitle! मैं हाजिर हूँ, आज्ञा दीजिए!")
            )
        }

        // 0a. Regional Language / Dialect Switch ("भोजपुरी में बोलो", "मैथिली में बात करो", "पंजाबी बोलो", "english me bolo")
        RegionalLanguageManager.checkVoiceLanguageSwitch(context, input)?.let { (_, langSpeech) ->
            return CommandResult(
                spokenResponse = langSpeech,
                actionTaken = SnehaAction.ConversationalAnswer(langSpeech)
            )
        }

        // 0b. Open Smart Home Hub or IoT Controls ("स्मार्ट होम खोलो", "लाइट जलाओ", "पंखा 4 पर करो", "एसी 24 डिग्री")
        if (lower.contains("स्मार्ट होम खोलो") || lower.contains("smart home") || lower == "स्मार्ट होम") {
            return CommandResult(
                spokenResponse = "मास्टर, IoT स्मार्ट होम हब खोला जा रहा है।",
                actionTaken = SnehaAction.OpenSmartHome
            )
        }
        SmartHomeManager.processSmartHomeVoiceCommand(context, input)?.let { (handled, iotSpeech) ->
            if (handled) {
                return CommandResult(
                    spokenResponse = iotSpeech,
                    actionTaken = SnehaAction.ConversationalAnswer(iotSpeech)
                )
            }
        }

        // 0c. Offline Mode Force Toggle ("ऑफलाइन मोड ऑन करो", "ऑफलाइन मोड बंद करो")
        if (lower.contains("ऑफलाइन मोड") || lower.contains("offline mode")) {
            val isOff = lower.contains("बंद") || lower.contains("off") || lower.contains("हटाओ")
            OfflineVoiceManager.setForceOffline(context, !isOff)
            val reply = if (isOff) {
                "मास्टर, ऑफलाइन मोड बंद कर दिया गया है। स्नेहा अब ऑनलाइन इंटरनेट और Gemini AI से कनेक्टेड है।"
            } else {
                "मास्टर, ऑफलाइन मोड चालू कर दिया गया है। बिना इंटरनेट के भी टॉर्च, कॉलिंग, अलार्म, वॉल्यूम, नोट्स और इमरजेंसी SOS सक्रिय हैं।"
            }
            return CommandResult(spokenResponse = reply, actionTaken = SnehaAction.ConversationalAnswer(reply))
        }

        // 0d. Zero-Internet Offline Voice Processor (If no internet or offline mode active)
        if (OfflineVoiceManager.isOfflineActive(context)) {
            OfflineVoiceManager.processOfflineCommand(context, input)?.let { offlineResp ->
                return CommandResult(
                    spokenResponse = offlineResp,
                    actionTaken = SnehaAction.ConversationalAnswer(offlineResp)
                )
            }
        }

        // 1a. WhatsApp Call Command ("व्हाट्सएप कॉल करो राहुल", "whatsapp call to rahul", "व्हाट्सएप पर कॉल लगाओ")
        val isWhatsAppCall = (lower.contains("whatsapp") || lower.contains("व्हाट्सएप") || lower.contains("वाट्सएप")) &&
                (lower.contains("call") || lower.contains("कॉल") || lower.contains("फोन") || lower.contains("लगाओ") || lower.contains("मिलाओ"))
        if (isWhatsAppCall) {
            val target = extractContactTarget(input, listOf("व्हाट्सएप पर कॉल करो", "व्हाट्सएप कॉल करो", "व्हाट्सएप पर", "व्हाट्सएप कॉल", "whatsapp call to", "whatsapp call", "कॉल करो", "कॉल लगाओ", "को कॉल करो", "को फोन लगाओ"))
            val (success, speech) = WhatsAppManager.makeWhatsAppCall(context, target)
            return CommandResult(
                spokenResponse = speech,
                actionTaken = SnehaAction.ConversationalAnswer(speech)
            )
        }

        // 1b. WhatsApp Message Command ("व्हाट्सएप पर राहुल को मैसेज भेजो मैं 10 मिनट में आ रहा हूँ")
        val isWhatsAppMsg = (lower.contains("whatsapp") || lower.contains("व्हाट्सएप") || lower.contains("वाट्सएप")) &&
                (lower.contains("मैसेज") || lower.contains("संदेश") || lower.contains("message") || lower.contains("msg") || lower.contains("भेजो") || lower.contains("send"))
        if (isWhatsAppMsg && !lower.contains("ऑटो") && !lower.contains("auto") && !lower.contains("रिप्लाई") && !lower.contains("reply")) {
            val (target, message) = extractContactAndMessage(input, isWhatsApp = true)
            val (success, speech) = WhatsAppManager.sendWhatsAppMessage(context, target, message)
            return CommandResult(
                spokenResponse = speech,
                actionTaken = SnehaAction.ConversationalAnswer(speech)
            )
        }

        // 1c. Direct SMS to anyone ("मैसेज भेजो राहुल को मैं आ रहा हूँ", "send message to 9876543210")
        val isGeneralMsg = (lower.contains("मैसेज भेजो") || lower.contains("sms भेजो") || lower.contains("संदेश भेजो") ||
                lower.contains("send message") || lower.contains("send sms") || lower.contains("message to") || lower.contains("sms to"))
        if (isGeneralMsg) {
            val (target, message) = extractContactAndMessage(input, isWhatsApp = false)
            val (success, speech) = WhatsAppManager.sendDirectSms(context, target, message)
            return CommandResult(
                spokenResponse = speech,
                actionTaken = SnehaAction.ConversationalAnswer(speech)
            )
        }

        // 1d. Direct Phone Call to anyone ("कॉल करो राहुल", "call 9876543210", "फोन लगाओ राहुल को", "कॉल लगाओ", "डायल करो")
        val isDirectCall = (lower.contains("कॉल") || lower.contains("call") || lower.contains("फोन") || lower.contains("डायल") || lower.contains("dial")) &&
                (lower.contains("लगा") || lower.contains("करो") || lower.contains("मिला") || lower.contains("dial") || lower.startsWith("call ") || lower.contains("to ")) &&
                !lower.contains("pick") && !lower.contains("पिक") && !lower.contains("उठा") && !lower.contains("काट") && !lower.contains("cut") && !lower.contains("reject") && !lower.contains("रिजेक्ट") && !lower.contains("whatsapp") && !lower.contains("व्हाट्सएप")
        if (isDirectCall) {
            val target = extractContactTarget(input, listOf("कॉल करो", "कॉल लगाओ", "फोन लगाओ", "फोन करो", "कॉल मिलाओ", "डायल करो", "call to", "call", "को कॉल करो", "को फोन लगाओ", "को फोन करो", "को कॉल लगाओ"))
            val (success, speech) = WhatsAppManager.makeDirectPhoneCall(context, target)
            return CommandResult(
                spokenResponse = speech,
                actionTaken = SnehaAction.ConversationalAnswer(speech)
            )
        }

        // 1e. Emergency SOS Location Share ("स्नेहा मुझे बचाओ", "हेल्प मी", "इमरजेंसी लोकेशन भेजो")
        if (EmergencySosManager.isEmergencySosQuery(lower)) {
            val (success, speech) = EmergencySosManager.triggerEmergencySos(context)
            return CommandResult(
                spokenResponse = speech,
                actionTaken = SnehaAction.ConversationalAnswer(speech)
            )
        }

        // 1f. Morning Briefing Bulletin ("गुड मॉर्निंग स्नेहा", "morning briefing", "दिन का हाल")
        if (MorningBriefingManager.isMorningBriefingQuery(lower)) {
            val speech = MorningBriefingManager.generateMorningBriefing(context)
            return CommandResult(
                spokenResponse = speech,
                actionTaken = SnehaAction.ConversationalAnswer(speech)
            )
        }

        // 1g. Voice Math & Currency / Unit Converter ("2500 का 18%", "50 डॉलर", "5000 भाग 6")
        VoiceMathConverter.processMathOrConversion(input)?.let { mathAnswer ->
            return CommandResult(
                spokenResponse = mathAnswer,
                actionTaken = SnehaAction.ConversationalAnswer(mathAnswer)
            )
        }

        // 1h. Voice Notes & Quick Reminders ("नोट लिखो", "याद रखना कि", "मेरे नोट्स सुनाओ")
        if (VoiceNotesManager.isNoteQuery(lower)) {
            val noteSpeech = VoiceNotesManager.processNoteVoiceCommand(context, input)
            return CommandResult(
                spokenResponse = noteSpeech,
                actionTaken = SnehaAction.ConversationalAnswer(noteSpeech)
            )
        }

        // 2. Strict Security Guard: Direct request for private OTP
        if ((lower.contains("otp") || lower.contains("ओटीपी") || lower.contains("पासवर्ड") || lower.contains("password")) &&
            (lower.contains("पढ़ो") || lower.contains("पढ़ो") || lower.contains("read") || lower.contains("बताओ") || lower.contains("tell") || lower.contains("क्या है"))
        ) {
            return CommandResult(
                spokenResponse = "माफ़ कीजिये मास्टर, सुरक्षा कारणों से मैं कोई भी गोपनीय बैंक OTP या पासवर्ड जोर से नहीं पढ़ सकती।",
                isOtpRefusal = true
            )
        }

        // 3. Voice Call Pick & Auto-Speak (e.g. "कॉल पिक करके बोलो कि मैं क्लास में हूँ", "call pic karke bolo ki mai class me hu")
        val isCallPickCommand = (lower.contains("call") || lower.contains("कॉल") || lower.contains("फोन")) &&
                (lower.contains("pick") || lower.contains("पिक") || lower.contains("उठा") || lower.contains("रिसीव") || lower.contains("receive") || lower.contains("attend"))

        if (isCallPickCommand) {
            var messageToCaller: String? = null
            val boloKeywords = listOf("बोलो कि", "बोलो", "बोल दो", "bolo ki", "bolo", "bol do", "say that", "say")
            for (kw in boloKeywords) {
                val idx = lower.indexOf(kw)
                if (idx != -1) {
                    messageToCaller = input.substring(idx + kw.length).trim()
                    break
                }
            }

            if (messageToCaller.isNullOrBlank()) {
                messageToCaller = "मास्टर अभी क्लास में हैं, कृपया बाद में कॉल करें।"
            }

            val (success, summary) = CallAssistantManager.pickCallAndSpeak(context, messageToCaller) {}

            return CommandResult(
                spokenResponse = summary,
                actionTaken = SnehaAction.PickCallAndSpeak(messageToCaller)
            )
        }

        // Cut / Reject Call Command ("कॉल काटो", "cut call", "कॉल रिजेक्ट करो", "reject call", "disconnect")
        val isCallCutCommand = (lower.contains("call") || lower.contains("कॉल") || lower.contains("फोन")) &&
                (lower.contains("काट") || lower.contains("cut") || lower.contains("reject") || lower.contains("रिजेक्ट") || lower.contains("disconnect"))
        if (isCallCutCommand) {
            val cutMsg = CallAssistantManager.endOrRejectCall(context)
            return CommandResult(
                spokenResponse = cutMsg,
                actionTaken = SnehaAction.RejectCall
            )
        }

        // 3a. AI Truecaller / Spam Blocker Command
        if (lower.contains("स्पैम") || lower.contains("spam") || lower.contains("truecaller") || lower.contains("ट्रूकॉलर")) {
            return CommandResult(
                spokenResponse = "जी मास्टर! AI स्पैम कॉलर गार्ड सक्रिय है। ज्ञात स्पैम और फ्रॉड नंबर खुद ब्लॉक हो रहे हैं।",
                actionTaken = SnehaAction.OpenSpamBlocker
            )
        }

        // Custom Wake-Word Command ("वेक वर्ड बदलो", "अपना नाम जार्विस रखो", etc.)
        if (lower.contains("वेक वर्ड") || lower.contains("wake word") || (lower.contains("अपना नाम") && (lower.contains("रखो") || lower.contains("बदलो")))) {
            var newName = ""
            val prefixes = listOf("वेक वर्ड बदलकर", "वेक वर्ड रखो", "वेक वर्ड", "wake word to", "wake word", "अपना नाम")
            for (p in prefixes) {
                val idx = lower.indexOf(p)
                if (idx != -1) {
                    val candidate = input.substring(idx + p.length).replace("रखो", "").replace("करो", "").replace("set", "").replace("to", "").replace("बदलो", "").trim()
                    if (candidate.isNotBlank()) {
                        newName = candidate
                        break
                    }
                }
            }
            if (newName.isNotBlank()) {
                com.example.data.local.VoicePreferences.saveCustomWakeWord(context, newName)
                return CommandResult(
                    spokenResponse = "जी मास्टर! अब से मैं '$newName' पुकारने पर हाजिर होऊंगी!",
                    actionTaken = SnehaAction.SetCustomWakeWord(newName)
                )
            } else {
                return CommandResult(
                    spokenResponse = "मास्टर, सेटिंग्स में कस्टम वेक-वर्ड विकल्प खोला गया है। आप अपनी पसंद का नाम चुन सकते हैं।",
                    actionTaken = SnehaAction.OpenSettings
                )
            }
        }

        // 3b. WhatsApp Auto-Reply Command
        if ((lower.contains("व्हाट्सएप") || lower.contains("whatsapp")) &&
            (lower.contains("रिप्लाई") || lower.contains("reply") || lower.contains("ऑटो") || lower.contains("auto"))
        ) {
            return CommandResult(
                spokenResponse = "जी मास्टर! व्हाट्सएप ऑटो-रिप्लाई खोला गया है। आपके आने वाले मैसेज पर निर्धारित उत्तर खुद चला जाएगा।",
                actionTaken = SnehaAction.OpenWhatsAppAutoReply
            )
        }

        // 3c. Live Call Translator Command
        if (lower.contains("ट्रांसलेटर") || lower.contains("translator") || lower.contains("अनुवाद") ||
            lower.contains("translate") || lower.contains("अंग्रेजी") || lower.contains("english")
        ) {
            return CommandResult(
                spokenResponse = "जी मास्टर! लाइव कॉल व वॉयस ट्रांसलेटर शुरू किया गया है। अब आप हिंदी और अंग्रेजी में सीधे बात कर सकते हैं।",
                actionTaken = SnehaAction.OpenCallTranslator
            )
        }

        // 3d. Smart Class Timetable Command
        if (lower.contains("टाइमटेबल") || lower.contains("timetable") || lower.contains("मेरी क्लास") ||
            lower.contains("अगली क्लास") || lower.contains("lecture") || lower.contains("पीरियड")
        ) {
            val next = ClassTimetableManager.getNextLecture()
            val spoken = if (next != null) {
                "मास्टर, आपकी अगली क्लास ${next.subject} की है, जो ${next.startTime} बजे ${next.room} में शुरू होगी।"
            } else {
                "मास्टर, स्मार्ट क्लास टाइमटेबल खोला गया है।"
            }
            return CommandResult(
                spokenResponse = spoken,
                actionTaken = SnehaAction.OpenClassTimetable
            )
        }

        // 3e. Anti-Theft Guard Command ("एंटी थेफ्ट ऑन करो", "पॉकेट अलार्म चालू करो", "डोंट टच माय फोन", etc.)
        if (lower.contains("एंटी थेफ्ट") || lower.contains("anti theft") || lower.contains("चोरी") ||
            lower.contains("पॉकेट अलार्म") || lower.contains("pocket alarm") || lower.contains("डोंट टच") || lower.contains("मोशन अलार्म")
        ) {
            if (lower.contains("पॉकेट") || lower.contains("pocket")) {
                AntiTheftManager.setPocketGuard(context, true)
                return CommandResult(
                    spokenResponse = "मास्टर, पॉकेट एंटी-थेफ्ट गार्ड चालू कर दिया गया है। जेब से फोन निकाले जाने पर सायरन बजेगा।",
                    actionTaken = SnehaAction.OpenAntiTheft
                )
            }
            if (lower.contains("मोशन") || lower.contains("motion") || lower.contains("टेबल") || lower.contains("टच")) {
                AntiTheftManager.setMotionGuard(context, true)
                return CommandResult(
                    spokenResponse = "मास्टर, मोशन गार्ड सक्रिय है। फोन हिलाने या छूने पर तुरंत सायरन बजेगा।",
                    actionTaken = SnehaAction.OpenAntiTheft
                )
            }
            return CommandResult(
                spokenResponse = "जी मास्टर! एंटी-थेफ्ट गार्ड व इंट्रूडर स्क्रीन खोली गई है। आप पॉकेट और मोशन गार्ड ऑन कर सकते हैं।",
                actionTaken = SnehaAction.OpenAntiTheft
            )
        }

        // 3f. AI Call Summary Command ("कॉल समरी", "कॉल का सारांश", "कॉल नोट्स", "call summary")
        if (lower.contains("समरी") || lower.contains("summary") || lower.contains("सारांश") || lower.contains("कॉल नोट्स")) {
            val list = CallSummaryManager.summaries.value
            val first = list.firstOrNull()
            val spoken = if (first != null) {
                "मास्टर, आपकी पिछली कॉल ${first.callerName} से हुई थी। मुख्य बात: ${first.summaryPoints.firstOrNull() ?: ""}।"
            } else {
                "मास्टर, AI कॉल सारांश व कार्य सूची खोली गई है।"
            }
            return CommandResult(
                spokenResponse = spoken,
                actionTaken = SnehaAction.OpenCallSummary
            )
        }

        // 3g. AI Cloud Connector / ChatGPT Command ("चैटजीपीटी कनेक्ट करो", "क्लाउड कनेक्टर", "chatgpt", "claude")
        if (lower.contains("chatgpt") || lower.contains("चैटजीपीटी") || lower.contains("क्लाउड कनेक्टर") ||
            lower.contains("connector") || lower.contains("claude") || lower.contains("क्लॉड") || lower.contains("ollama")
        ) {
            return CommandResult(
                spokenResponse = "जी मास्टर! मल्टी-मॉडल AI क्लाउड कनेक्टर खोला गया है। आप ChatGPT, Gemini या Claude को जोड़ सकते हैं।",
                actionTaken = SnehaAction.OpenAiConnector
            )
        }

        // 3h. Call Voice Avatar Command ("कॉल की आवाज बदलो", "विक्रम की आवाज", "सिक्योरिटी गार्ड आवाज")
        if (lower.contains("कॉल आवाज") || lower.contains("कॉल आवाज़") || lower.contains("वॉइस अवतार") || lower.contains("voice avatar")) {
            if (lower.contains("विक्रम") || lower.contains("vikram") || lower.contains("पुरुष")) {
                CallAssistantManager.setActiveVoiceAvatar(CallVoiceAvatar.VIKRAM_MALE)
                return CommandResult(
                    spokenResponse = "जी मास्टर! कॉल अटेंडेंट के लिए विक्रम (गंभीर पुरुष आवाज) सेट कर दी गई है।",
                    actionTaken = SnehaAction.OpenCallAssistant
                )
            }
            if (lower.contains("सिक्योरिटी") || lower.contains("security") || lower.contains("पुलिस")) {
                CallAssistantManager.setActiveVoiceAvatar(CallVoiceAvatar.SECURITY_GUARD)
                return CommandResult(
                    spokenResponse = "जी मास्टर! सुरक्षा गार्ड / पुलिस टोन एक्टिवेट कर दी गई है। स्पैमर्स अब तुरंत अलर्ट हो जाएंगे।",
                    actionTaken = SnehaAction.OpenCallAssistant
                )
            }
            return CommandResult(
                spokenResponse = "मास्टर, कॉल असिस्टेंट में वॉयस चेंजर मेनू खोला गया है। आप स्नेहा, विक्रम, सिक्योरिटी गार्ड या रोबोट अवतार चुन सकते हैं।",
                actionTaken = SnehaAction.OpenCallAssistant
            )
        }

        // 4. Spoken Voice PIN Unlock (e.g. "मेरा पिन 1234 है फोन अनलॉक करो" or "पिन 1234 अनलॉक")
        if ((lower.contains("पिन") || lower.contains("pin")) && (lower.contains("अनलॉक") || lower.contains("unlock") || lower.contains("खोलो"))) {
            val digitsMatch = Regex("(\\d{4,6})").find(input)
            if (digitsMatch != null) {
                val spokenPin = digitsMatch.value
                val isCorrect = SecurityUnlockPreferences.verifyPin(context, spokenPin)
                if (isCorrect) {
                    if (activity != null) {
                        PhoneControlManager.requestEmergencyUnlock(activity) {}
                    }
                    return CommandResult(
                        spokenResponse = "मास्टर, आपका पिन $spokenPin सत्यापित हो गया है! फोन सफलतापूर्वक अनलॉक कर दिया गया है।",
                        actionTaken = SnehaAction.UnlockPhone(verifiedWithPin = true)
                    )
                } else {
                    return CommandResult(
                        spokenResponse = "मास्टर, दर्ज किया गया पिन $spokenPin गलत है। कृपया सही पिन बोलें या स्क्रीन पर डालें।",
                        actionTaken = SnehaAction.OpenSecurityUnlock
                    )
                }
            }
        }

        // 4. Phone Unlock Request ("फोन अनलॉक करो", "unlock phone", "स्क्रीन खोलो", "अनलॉक करो")
        if (lower.contains("फोन अनलॉक") || lower.contains("phone unlock") || lower.contains("स्क्रीन अनलॉक") ||
            lower.contains("डिवाइस अनलॉक") || lower == "अनलॉक करो" || lower == "unlock" || lower == "unlock phone"
        ) {
            if (activity != null) {
                PhoneControlManager.requestEmergencyUnlock(activity) {}
            }
            return CommandResult(
                spokenResponse = "जी मास्टर, मैं फोन अनलॉक कर रही हूँ। कृपया अपना पिन, पैटर्न या पासवर्ड दर्ज करें या बोलें।",
                actionTaken = SnehaAction.OpenSecurityUnlock
            )
        }

        // 5. Real-time Screen Share ("स्क्रीन शेयर करो", "start screen share", "स्क्रीन दिखाओ", "स्क्रीन शेयर बंद करो")
        if (lower.contains("screen share") || lower.contains("स्क्रीन शेयर") || lower.contains("स्क्रीन साझा")) {
            if (lower.contains("बंद") || lower.contains("stop") || lower.contains("हटाओ") || lower.contains("रोक")) {
                return CommandResult(
                    spokenResponse = "मास्टर, स्क्रीन शेयर सफलतापूर्वक रोक दिया गया है।",
                    actionTaken = SnehaAction.StopScreenShare
                )
            } else {
                return CommandResult(
                    spokenResponse = "जी मास्टर! रियल-टाइम स्क्रीन शेयर शुरू किया जा रहा है। अब आप अपनी स्क्रीन लाइव साझा कर सकते हैं।",
                    actionTaken = SnehaAction.StartScreenShare
                )
            }
        }

        // 6. Emergency Unlock & SOS Alarm
        if (lower.contains("emergency") || lower.contains("इमरजेंसी") || lower.contains("sos") || lower.contains("अलार्म बजाओ")) {
            if (activity != null) {
                PhoneControlManager.requestEmergencyUnlock(activity) {}
            }
            PhoneControlManager.startEmergencyAlarm(context)
            return CommandResult(
                spokenResponse = "मास्टर, इमरजेंसी मोड सक्रिय कर दिया गया है! लॉकस्क्रीन हटाने का प्रयास किया गया और SOS सायरन चालू है।",
                actionTaken = SnehaAction.EmergencyUnlockAndSos
            )
        }

        // 7. Stop Siren / Emergency Disarm ("सायरन बंद करो", "स्टॉप सायरन", "सायरन ऑफ", "stop siren", "siren off", etc.)
        val isStopSiren = (lower.contains("सायरन") || lower.contains("siren") || lower.contains("अलार्म") || lower.contains("alarm") || lower.contains("sos")) &&
                (lower.contains("बंद") || lower.contains("stop") || lower.contains("off") || lower.contains("रोक") || lower.contains("चुप") || lower.contains("शांत"))
        if (isStopSiren) {
            AntiTheftManager.stopAlarm(context)
            PhoneControlManager.stopEmergencyAlarm()
            PhoneControlManager.stopEmergencyStrobe(context)
            return CommandResult(
                spokenResponse = "मास्टर की आवाज पहचानी गई! इमरजेंसी सायरन और अलार्म बंद कर दिया गया है।",
                actionTaken = SnehaAction.StopSiren
            )
        }

        // 7a. Start Emergency Siren ("सायरन बजाओ", "सायरन ऑन करो", "start siren", "emergency siren")
        val isStartSiren = (lower.contains("सायरन") || lower.contains("siren")) &&
                (lower.contains("बजाओ") || lower.contains("ऑन") || lower.contains("चालू") || lower.contains("start") || lower.contains("on"))
        if (isStartSiren) {
            AntiTheftManager.triggerTheftAlarm(context, "मास्टर! इमरजेंसी सायरन चालू कर दिया गया है!")
            return CommandResult(
                spokenResponse = "मास्टर! इमरजेंसी सायरन और फ्लैशलाइट स्ट्रोब चालू कर दिया गया है। बंद करने के लिए 'सायरन बंद करो' कहें।",
                actionTaken = SnehaAction.EmergencyUnlockAndSos
            )
        }

        // 7b. Anti-Intruder Pickup Siren Notice (Shake siren removed by user request)
        val isPickupSirenCommand = (lower.contains("कोई और") || lower.contains("फोन ले") || lower.contains("फोन उठाए") || lower.contains("पिकअप सायरन") || lower.contains("pickup siren") || lower.contains("डोंट टच") || lower.contains("हिलाने पर सायरन") || lower.contains("मोशन सायरन"))
        if (isPickupSirenCommand) {
            AntiTheftManager.setPickupSirenGuard(context, false)
            return CommandResult(
                spokenResponse = "मास्टर, फोन हिलने या उठाने पर सायरन बजने का फीचर आपके निर्देशानुसार स्थायी रूप से हटा दिया गया है ताकि फोन हिलाने या जेब में रखने पर कभी भी गलती से सायरन न बजे।",
                actionTaken = SnehaAction.OpenAntiTheft
            )
        }

        // 7c. Bluetooth Voice Control ("ब्लूटूथ ऑन करो", "ब्लूटूथ बंद करो", "bluetooth on", "bluetooth off")
        if (lower.contains("bluetooth") || lower.contains("ब्लूटूथ") || lower.contains("बूलूटूथ")) {
            val isOff = lower.contains("बंद") || lower.contains("off") || lower.contains("disable") || lower.contains("हटा")
            val isOn = lower.contains("चालू") || lower.contains("ऑन") || lower.contains("on") || lower.contains("enable") || lower.contains("खोलो")
            val targetState = if (isOff) false else if (isOn) true else !PhoneControlManager.isBluetoothEnabled(context)

            val (success, msg) = PhoneControlManager.toggleBluetooth(context, targetState)
            return CommandResult(
                spokenResponse = "मास्टर, $msg",
                actionTaken = SnehaAction.ToggleBluetooth(targetState)
            )
        }

        // 7d. Hotspot Voice Control ("हॉटस्पॉट ऑन करो", "हॉटस्पॉट चालू करो", "हॉटस्पॉट बंद करो", "hotspot on", "hotspot off")
        if (lower.contains("hotspot") || lower.contains("हॉटस्पॉट") || lower.contains("हॉट स्पाट") || lower.contains("tethering") || lower.contains("टेदरिंग")) {
            val isOff = lower.contains("बंद") || lower.contains("off") || lower.contains("disable")
            val isOn = lower.contains("चालू") || lower.contains("ऑन") || lower.contains("on") || lower.contains("enable") || lower.contains("खोलो")
            val targetState = if (isOff) false else if (isOn) true else !PhoneControlManager.isHotspotActive.value

            val (success, msg) = PhoneControlManager.toggleHotspot(context, targetState)
            return CommandResult(
                spokenResponse = msg,
                actionTaken = SnehaAction.ToggleHotspot(targetState)
            )
        }

        // 7e. Wi-Fi Voice Control ("वाईफाई ऑन करो", "वाईफाई चालू करो", "वाईफाई बंद करो", "wifi on", "wifi off")
        if (lower.contains("wifi") || lower.contains("वाईफाई") || lower.contains("वाई-फाई") || lower.contains("wi-fi")) {
            val isOff = lower.contains("बंद") || lower.contains("off") || lower.contains("disable")
            val isOn = lower.contains("चालू") || lower.contains("ऑन") || lower.contains("on") || lower.contains("enable") || lower.contains("खोलो")
            val targetState = if (isOff) false else if (isOn) true else !PhoneControlManager.isWifiEnabled(context)

            val (success, msg) = PhoneControlManager.toggleWifi(context, targetState)
            return CommandResult(
                spokenResponse = "मास्टर, $msg",
                actionTaken = SnehaAction.ToggleWifi(targetState)
            )
        }

        // 8. Torch / Flashlight Control
        if (lower.contains("torch") || lower.contains("टॉर्च") || lower.contains("flashlight") || lower.contains("फ्लैशलाइट")) {
            val isOff = lower.contains("बंद") || lower.contains("off") || lower.contains("हटा")
            val isOn = lower.contains("चालू") || lower.contains("ऑन") || lower.contains("on") || lower.contains("जला")
            val targetState = if (isOff) false else if (isOn) true else !PhoneControlManager.isTorchActive()

            val (success, msg) = PhoneControlManager.toggleTorch(context, targetState)
            val prefix = if (targetState) "मास्टर, टॉर्च चालू कर दी गई है।" else "मास्टर, टॉर्च बंद कर दी गई है।"
            return CommandResult(
                spokenResponse = "$prefix $msg",
                actionTaken = SnehaAction.ToggleTorch(targetState)
            )
        }

        // 9. Volume Adjustments
        if (lower.contains("volume") || lower.contains("वॉल्यूम") || lower.contains("आवाज") || lower.contains("साउंड")) {
            if (lower.contains("बढ़ा") || lower.contains("up") || lower.contains("तेज") || lower.contains("high")) {
                val msg = PhoneControlManager.adjustVolume(context, true)
                return CommandResult(spokenResponse = "मास्टर, $msg", actionTaken = SnehaAction.AdjustVolume(true))
            }
            if (lower.contains("कम") || lower.contains("down") || lower.contains("धीम") || lower.contains("low")) {
                val msg = PhoneControlManager.adjustVolume(context, false)
                return CommandResult(spokenResponse = "मास्टर, $msg", actionTaken = SnehaAction.AdjustVolume(false))
            }
            if (lower.contains("म्यूट") || lower.contains("mute") || lower.contains("शांत")) {
                val msg = PhoneControlManager.muteVolume(context)
                return CommandResult(spokenResponse = "मास्टर, $msg", actionTaken = SnehaAction.MuteVolume)
            }
        }

        // 10. Battery Status
        if (lower.contains("battery") || lower.contains("बैटरी") || lower.contains("चार्ज") || lower.contains("charging")) {
            val msg = PhoneControlManager.getBatteryInfo(context)
            return CommandResult(spokenResponse = "मास्टर, $msg", actionTaken = SnehaAction.CheckBattery)
        }

        // 11. Read Messages & Notifications (Safe with OTP masking)
        if (lower.contains("मैसेज") || lower.contains("संदेश") || lower.contains("message") || lower.contains("notification") || lower.contains("sms") || lower.contains("एसएमएस")) {
            if (lower.contains("पढ़ो") || lower.contains("पढ़ो") || lower.contains("read") || lower.contains("सुनाओ") || lower.contains("check") || lower.contains("बताओ")) {
                val customSpeech = onReadMessagesRequest?.invoke() ?: run {
                    val db = com.example.data.local.SnehaDatabase.getInstance(context)
                    val list = db.notificationLogDao().getRecentNotificationList(4)
                    if (list.isEmpty()) {
                        "मास्टर, आपके पास कोई नया संदेश या एसएमएस नहीं मिला है।"
                    } else {
                        val sb = StringBuilder("मास्टर, आपके पास नए संदेश हैं: ")
                        list.forEachIndexed { idx, it ->
                            sb.append("संदेश ${idx + 1}: ${OtpSafetyGuard.sanitizeForSpeech(it.sender, it.originalText)} ")
                        }
                        sb.toString()
                    }
                }
                return CommandResult(
                    spokenResponse = customSpeech,
                    actionTaken = SnehaAction.ReadMessagesSafe
                )
            }
        }

        // 12. In-App Searches: YouTube, WhatsApp, Maps
        if (lower.contains("youtube") || lower.contains("यूट्यूब")) {
            if (lower.contains("सर्च") || lower.contains("search") || lower.contains("चलाओ") || lower.contains("play")) {
                val query = extractQuery(input, listOf("यूट्यूब पर सर्च करो", "यूट्यूब में सर्च करो", "यूट्यूब पर", "यूट्यूब में", "search on youtube for", "youtube search", "youtube par search", "youtube"))
                PhoneControlManager.searchYouTube(context, query)
                return CommandResult(
                    spokenResponse = "मास्टर, यूट्यूब पर '$query' खोजा जा रहा है।",
                    actionTaken = SnehaAction.SearchYouTube(query)
                )
            }
        }

        if (lower.contains("whatsapp") || lower.contains("व्हाट्सएप") || lower.contains("वाट्सएप")) {
            if (lower.contains("सर्च") || lower.contains("search") || lower.contains("भेजो") || lower.contains("send") || lower.contains("ढूंढो")) {
                val query = extractQuery(input, listOf("व्हाट्सएप पर सर्च करो", "व्हाट्सएप में सर्च करो", "व्हाट्सएप पर", "व्हाट्सएप में", "search on whatsapp for", "whatsapp search", "whatsapp par", "whatsapp"))
                PhoneControlManager.searchWhatsApp(context, query)
                return CommandResult(
                    spokenResponse = "मास्टर, व्हाट्सएप पर '$query' खोजा जा रहा है।",
                    actionTaken = SnehaAction.SearchInsideApp("WhatsApp", query)
                )
            }
        }

        // 13. Timer / Alarm
        if (lower.contains("timer") || lower.contains("टाइमर")) {
            val minutesRegex = Regex("(\\d+)")
            val match = minutesRegex.find(input)
            val minutes = match?.value?.toIntOrNull() ?: 5
            val (success, msg) = PhoneControlManager.setTimer(context, minutes)
            return CommandResult(spokenResponse = "मास्टर, $msg", actionTaken = SnehaAction.SetTimer(minutes))
        }

        // 14. Open App Command ("खोलो", "open", "चलाओ")
        if (lower.contains("खोलो") || lower.contains("open") || lower.contains("चलाओ") || lower.contains("launch") || lower.contains("app")) {
            val (success, msg) = PhoneControlManager.openAppByName(context, input)
            if (success) {
                return CommandResult(spokenResponse = "मास्टर, $msg", actionTaken = SnehaAction.OpenApp(input, ""))
            }
        }

        // 14a. Time & Date Commands ("समय बताओ", "टाइम क्या है", "आज की तारीख क्या है", "आज कौन सा दिन है")
        if (lower.contains("समय") || lower.contains("टाइम") || lower.contains("time") || lower.contains("कितने बजे") ||
            lower.contains("तारीख") || lower.contains("date") || lower.contains("आज का दिन") || lower.contains("कौन सा दिन")
        ) {
            val now = java.util.Calendar.getInstance()
            val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale("hi", "IN"))
            val dateFormat = java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale("hi", "IN"))
            val currentTime = timeFormat.format(now.time)
            val currentDate = dateFormat.format(now.time)
            val responseText = "मास्टर, अभी समय $currentTime हुआ है, और आज $currentDate है।"
            return CommandResult(
                spokenResponse = responseText,
                actionTaken = SnehaAction.ConversationalAnswer(responseText)
            )
        }

        // 14b. Camera Quick Action ("कैमरा खोलो", "कैमरा चालू करो", "फोटो खींचो", "open camera")
        if (lower.contains("camera") || lower.contains("कैमरा") || lower.contains("फोटो खींचो") || lower.contains("सेल्फी")) {
            val (success, msg) = PhoneControlManager.openAppByName(context, "camera")
            val resp = if (success) "मास्टर, $msg" else "मास्टर, कैमरा खोला जा रहा है।"
            return CommandResult(spokenResponse = resp, actionTaken = SnehaAction.OpenApp("Camera", ""))
        }

        // 14c. Music & Songs ("गाना बजाओ", "गाना चलाओ", "म्यूजिक चलाओ", "play music", "play song")
        if (lower.contains("गाना") || lower.contains("music") || lower.contains("म्यूजिक") || lower.contains("song") || lower.contains("गीत")) {
            val query = extractQuery(input, listOf("गाना बजाओ", "गाना चलाओ", "म्यूजिक चलाओ", "play music", "play song", "play"))
            val finalQuery = if (query.isNotBlank()) query else "Top Bollywood Songs"
            PhoneControlManager.searchYouTube(context, finalQuery)
            return CommandResult(
                spokenResponse = "मास्टर, यूट्यूब पर '$finalQuery' बजाया जा रहा है।",
                actionTaken = SnehaAction.SearchYouTube(finalQuery)
            )
        }

        // 14d. Jokes & Fun ("जोक सुनाओ", "चुटकुला सुनाओ", "tell a joke", "हंसाओ")
        if (lower.contains("जोक") || lower.contains("चुटकुला") || lower.contains("हंसाओ") || lower.contains("joke")) {
            val jokes = listOf(
                "मास्टर, एक बार पिंटू ने डॉक्टर से पूछा - क्या आप मेरी बीमारी ठीक कर देंगे? डॉक्टर बोला - जरूर, लेकिन जो दवाई लिखूँगा उसे रोज समय पर खाइएगा। पिंटू बोला - डॉक्टर साहब, मैं तो यहाँ चाय बिस्किट खाने आया था!",
                "मास्टर, संता ने भगवान से प्रार्थना की - हे भगवान, मुझे इतनी शक्ति दो कि जब मेरा फोन गिरे तो दिल न धड़के, बल्कि फोन मुस्कुराए!",
                "मास्टर, टीचर ने गोलू से पूछा - बताओ न्यूटन का चौथा नियम क्या है? गोलू बोला - सर, जब परीक्षा में प्रश्न समझ न आए, तो आँखें बंद करके 'हे प्रभु' बोलो!"
            )
            val selectedJoke = jokes.random()
            return CommandResult(
                spokenResponse = selectedJoke,
                actionTaken = SnehaAction.ConversationalAnswer(selectedJoke)
            )
        }

        // 14e. Calculator Quick Action ("कैलकुलेटर खोलो", "open calculator", "हिसाब किताब")
        if (lower.contains("कैलकुलेटर") || lower.contains("calculator") || lower.contains("हिसाब")) {
            PhoneControlManager.openAppByName(context, "calculator")
            return CommandResult(
                spokenResponse = "मास्टर, कैलकुलेटर खोला जा रहा है।",
                actionTaken = SnehaAction.OpenApp("Calculator", "")
            )
        }

        // 14f. Google Web Search ("गूगल पर सर्च करो", "सर्च करो", "search on google")
        if (lower.contains("गूगल पर सर्च") || lower.contains("गूगल में सर्च") || lower.contains("google search") || lower.startsWith("सर्च करो ") || lower.startsWith("search ")) {
            val query = extractQuery(input, listOf("गूगल पर सर्च करो", "गूगल में सर्च करो", "गूगल सर्च", "search on google for", "google search", "सर्च करो", "search"))
            val searchIntent = android.content.Intent(android.content.Intent.ACTION_WEB_SEARCH).apply {
                putExtra(android.app.SearchManager.QUERY, query)
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(searchIntent)
            } catch (ignored: Exception) {}
            return CommandResult(
                spokenResponse = "मास्टर, गूगल पर '$query' खोजा जा रहा है।",
                actionTaken = SnehaAction.ConversationalAnswer("मास्टर, गूगल पर '$query' खोजा जा रहा है।")
            )
        }

        // 15. General Conversational / AI Query with Gemini API (Always addressing as user's chosen title)
        val aiResponse = GeminiApiClient.getSnehaAiResponse(input)
        val formattedResponse = if (!aiResponse.contains(userTitle, ignoreCase = true) && !aiResponse.contains("मास्टर")) {
            "$userTitle, $aiResponse"
        } else {
            aiResponse.replace("मास्टर", userTitle)
        }

        return CommandResult(
            spokenResponse = formattedResponse,
            actionTaken = SnehaAction.ConversationalAnswer(formattedResponse)
        )
    }

    private fun extractQuery(fullText: String, prefixes: List<String>): String {
        var result = fullText
        for (p in prefixes) {
            val idx = result.indexOf(p, ignoreCase = true)
            if (idx != -1) {
                result = result.substring(idx + p.length).trim()
                break
            }
        }
        return result.replace("पर", "").replace("में", "").replace("करो", "").replace("for", "").trim()
    }

    private fun extractContactTarget(fullText: String, prefixes: List<String>): String {
        var clean = fullText
        for (p in prefixes) {
            val idx = clean.indexOf(p, ignoreCase = true)
            if (idx != -1) {
                clean = clean.substring(idx + p.length).trim()
                break
            }
        }
        return clean.replace("को", "")
            .replace("पर", "")
            .replace("करो", "")
            .replace("लगाओ", "")
            .replace("लगा", "")
            .replace("मिलाओ", "")
            .replace("मिला", "")
            .replace("डायल", "")
            .replace("dial", "", ignoreCase = true)
            .replace("call", "", ignoreCase = true)
            .replace("please", "", ignoreCase = true)
            .trim()
    }

    private fun extractContactAndMessage(fullText: String, isWhatsApp: Boolean): Pair<String, String> {
        var text = fullText
        val removePrefixes = listOf(
            "व्हाट्सएप पर", "व्हाट्सएप में", "व्हाट्सएप", "whatsapp par", "whatsapp per", "whatsapp to", "whatsapp",
            "मैसेज भेजो", "संदेश भेजो", "sms भेजो", "send message to", "send sms to", "message to", "sms to"
        )
        for (p in removePrefixes) {
            val idx = text.indexOf(p, ignoreCase = true)
            if (idx != -1) {
                text = text.substring(idx + p.length).trim()
                break
            }
        }

        val koIdx = text.indexOf(" को ")
        if (koIdx != -1) {
            val target = text.substring(0, koIdx).trim()
            var msg = text.substring(koIdx + 4).trim()
            msg = msg.replace("मैसेज", "").replace("संदेश", "").replace("भेजो", "").replace("send", "").replace("लिखो", "").trim()
            if (msg.isBlank()) msg = "नमस्ते!"
            return Pair(target, msg)
        }

        val parts = text.split(" ", limit = 2)
        if (parts.size == 2) {
            val target = parts[0].trim()
            val msg = parts[1].replace("मैसेज", "").replace("भेजो", "").replace("send", "").trim()
            return Pair(target, if (msg.isNotBlank()) msg else "नमस्ते!")
        }

        return Pair(text.trim(), "नमस्ते! मैं स्नेहा के माध्यम से मैसेज कर रहा हूँ।")
    }
}
