package com.example.ui

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChatMessageEntity
import com.example.data.local.EmergencyContactEntity
import com.example.data.local.NotificationLogEntity
import com.example.data.local.SnehaDatabase
import com.example.data.model.InstalledApp
import com.example.data.model.SnehaAction
import com.example.data.model.SnehaScreen
import com.example.data.model.VoiceState
import com.example.engine.CallAssistantManager
import com.example.engine.OtpSafetyGuard
import com.example.engine.PhoneControlManager
import com.example.engine.ScreenShareManager
import com.example.engine.SnehaCommandEngine
import com.example.engine.VoiceSpeechManager
import com.example.service.SnehaNotificationListener
import com.example.service.SnehaVoiceService
import com.example.ui.screens.AntiTheftScreen
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.CallAssistantScreen
import com.example.ui.screens.CallSummaryScreen
import com.example.ui.screens.ClassTimetableScreen
import com.example.ui.screens.CloudConnectorScreen
import com.example.ui.screens.EmergencyScreen
import com.example.ui.screens.LiveCallTranslatorScreen
import com.example.ui.screens.MessageReaderScreen
import com.example.ui.screens.PhoneControlScreen
import com.example.ui.screens.ScreenShareScreen
import com.example.ui.screens.SecurityUnlockScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SpamCallerScreen
import com.example.ui.screens.WhatsAppAutoReplyScreen
import com.example.ui.theme.SnehaCyan
import com.example.ui.theme.SnehaDarkSurface
import com.example.ui.theme.SnehaDarkSurfaceVariant
import com.example.ui.theme.SnehaPink
import com.example.ui.theme.SnehaTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NavItem(
    val screen: SnehaScreen,
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun MainScreen(
    voiceManager: VoiceSpeechManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentScreen by remember { mutableStateOf(SnehaScreen.ASSISTANT) }
    val db = remember { SnehaDatabase.getInstance(context) }

    val messages by db.chatMessageDao().getAllMessages().collectAsState(initial = emptyList())
    val contacts by db.emergencyContactDao().getAllContacts().collectAsState(initial = emptyList())
    val notifications by db.notificationLogDao().getRecentNotifications().collectAsState(initial = emptyList())

    val voiceState by voiceManager.voiceState.collectAsState()
    val audioAmplitude by voiceManager.audioAmplitude.collectAsState()
    val spokenTextLive by voiceManager.spokenTextLive.collectAsState()

    var isVoiceOutputEnabled by remember { mutableStateOf(voiceManager.isVoiceOutputEnabled) }
    var isAutoReadEnabled by remember { mutableStateOf(com.example.engine.BackgroundSpeaker.isAutoReadMessagesEnabled) }
    var speechRate by remember { mutableFloatStateOf(voiceManager.speechRate) }
    var speechPitch by remember { mutableFloatStateOf(voiceManager.speechPitch) }
    var currentPersona by remember { mutableStateOf(voiceManager.currentPersona) }
    var isServiceRunning by remember { mutableStateOf(true) }
    var isTorchActive by remember { mutableStateOf(PhoneControlManager.isTorchActive()) }
    var isAlarmPlaying by remember { mutableStateOf(PhoneControlManager.isEmergencyAlarmActive()) }
    var isStrobePlaying by remember { mutableStateOf(PhoneControlManager.isStrobeActive()) }
    var batteryInfo by remember { mutableStateOf(PhoneControlManager.getBatteryInfo(context)) }

    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }

    // Load installed apps once in background
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val apps = PhoneControlManager.getInstalledApps(context)
            installedApps = apps
        }
    }

    // Refresh battery and system states periodically
    LaunchedEffect(Unit) {
        batteryInfo = PhoneControlManager.getBatteryInfo(context)
        isTorchActive = PhoneControlManager.isTorchActive()
        isAlarmPlaying = PhoneControlManager.isEmergencyAlarmActive()
        isStrobePlaying = PhoneControlManager.isStrobeActive()
    }

    // Core Command Processor callback
    val executeUserQuery: (String) -> Unit = { query ->
        scope.launch {
            // Save User message
            db.chatMessageDao().insertMessage(
                ChatMessageEntity(text = query, isUser = true)
            )

            val cmdResult = SnehaCommandEngine.processCommand(
                context = context,
                activity = activity,
                rawInput = query,
                onReadMessagesRequest = {
                    val notifList = notifications
                    if (notifList.isEmpty()) {
                        "मास्टर, आपके पास कोई नया संदेश नहीं मिला है।"
                    } else {
                        val sb = StringBuilder("मास्टर, आपके पास ${notifList.size} नए संदेश हैं। ")
                        notifList.take(3).forEachIndexed { idx, it ->
                            sb.append("संदेश ${idx + 1}: ${OtpSafetyGuard.sanitizeForSpeech(it.sender, it.originalText)} ")
                        }
                        sb.toString()
                    }
                }
            )

            // Save Sneha's response
            db.chatMessageDao().insertMessage(
                ChatMessageEntity(
                    text = cmdResult.spokenResponse,
                    isUser = false,
                    isOtpMasked = cmdResult.isOtpRefusal
                )
            )

            // Speak response via TTS reliably
            if (isVoiceOutputEnabled) {
                voiceManager.speak(cmdResult.spokenResponse)
            } else {
                voiceManager.scheduleContinuousListenResume(100L)
            }

            // Handle actions that change screen or hardware state
            when (cmdResult.actionTaken) {
                is SnehaAction.OpenSecurityUnlock -> {
                    currentScreen = SnehaScreen.SECURITY_UNLOCK
                }
                is SnehaAction.UnlockPhone -> {
                    currentScreen = SnehaScreen.SECURITY_UNLOCK
                    if (activity != null) {
                        PhoneControlManager.requestEmergencyUnlock(activity) {}
                    }
                }
                is SnehaAction.StartScreenShare -> {
                    currentScreen = SnehaScreen.SCREEN_SHARE
                    ScreenShareManager.toggleScreenShareDirectly(true)
                }
                is SnehaAction.StopScreenShare -> {
                    ScreenShareManager.toggleScreenShareDirectly(false)
                }
                is SnehaAction.EmergencyUnlockAndSos -> {
                    currentScreen = SnehaScreen.EMERGENCY
                }
                is SnehaAction.PickCallAndSpeak -> {
                    currentScreen = SnehaScreen.CALL_ASSISTANT
                    val action = cmdResult.actionTaken as SnehaAction.PickCallAndSpeak
                    CallAssistantManager.pickCallAndSpeak(context, action.customMessage) { textToSpeak ->
                        voiceManager.speak(textToSpeak)
                    }
                }
                is SnehaAction.RejectCall -> {
                    currentScreen = SnehaScreen.CALL_ASSISTANT
                    CallAssistantManager.endOrRejectCall(context) { textToSpeak ->
                        voiceManager.speak(textToSpeak)
                    }
                }
                is SnehaAction.OpenCallAssistant -> {
                    currentScreen = SnehaScreen.CALL_ASSISTANT
                }
                is SnehaAction.OpenSettings -> {
                    currentScreen = SnehaScreen.SETTINGS
                }
                is SnehaAction.OpenSpamBlocker -> {
                    currentScreen = SnehaScreen.SPAM_BLOCKER
                }
                is SnehaAction.OpenWhatsAppAutoReply -> {
                    currentScreen = SnehaScreen.WHATSAPP_AUTO_REPLY
                }
                is SnehaAction.OpenCallTranslator -> {
                    currentScreen = SnehaScreen.CALL_TRANSLATOR
                }
                is SnehaAction.OpenClassTimetable -> {
                    currentScreen = SnehaScreen.CLASS_TIMETABLE
                }
                is SnehaAction.OpenAntiTheft -> {
                    currentScreen = SnehaScreen.ANTI_THEFT
                }
                is SnehaAction.OpenCallSummary -> {
                    currentScreen = SnehaScreen.CALL_SUMMARY
                }
                is SnehaAction.OpenAiConnector -> {
                    currentScreen = SnehaScreen.AI_CONNECTOR
                }
                is SnehaAction.OpenSmartHome -> {
                    currentScreen = SnehaScreen.SMART_HOME
                }
                is SnehaAction.OpenCreativeStudio -> {
                    currentScreen = SnehaScreen.CREATIVE_STUDIO
                }
                is SnehaAction.SetCustomWakeWord -> {
                    val act = cmdResult.actionTaken as SnehaAction.SetCustomWakeWord
                    com.example.data.local.VoicePreferences.saveCustomWakeWord(context, act.newName)
                }
                else -> {}
            }

            // Refresh toggles
            isTorchActive = PhoneControlManager.isTorchActive()
            isAlarmPlaying = PhoneControlManager.isEmergencyAlarmActive()
            isStrobePlaying = PhoneControlManager.isStrobeActive()
            batteryInfo = PhoneControlManager.getBatteryInfo(context)
        }
    }

    // Wire speech recognizer callback
    LaunchedEffect(voiceManager) {
        voiceManager.onSpeechRecognized = { recognizedText ->
            executeUserQuery(recognizedText)
        }
        voiceManager.onSpeechError = { errorMsg ->
            scope.launch {
                snackbarHostState.showSnackbar(errorMsg)
            }
        }
    }

    BackHandler(enabled = currentScreen != SnehaScreen.ASSISTANT) {
        currentScreen = SnehaScreen.ASSISTANT
    }

    val navItems = listOf(
        NavItem(SnehaScreen.ASSISTANT, "स्नेहा", Icons.Default.AutoAwesome, "nav_assistant"),
        NavItem(SnehaScreen.SMART_HOME, "स्मार्ट होम", Icons.Default.Home, "nav_smart_home"),
        NavItem(SnehaScreen.SPAM_BLOCKER, "स्पैम गार्ड", Icons.Default.Shield, "nav_spam_blocker"),
        NavItem(SnehaScreen.WHATSAPP_AUTO_REPLY, "व्हाट्सएप", Icons.Default.Sms, "nav_whatsapp"),
        NavItem(SnehaScreen.CALL_TRANSLATOR, "ट्रांसलेटर", Icons.Default.Translate, "nav_translator"),
        NavItem(SnehaScreen.SETTINGS, "सेटिंग्स", Icons.Default.Settings, "nav_settings")
    )

    val configuration = LocalConfiguration.current
    val isExpanded = configuration.screenWidthDp >= 600

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!isExpanded) {
                NavigationBar(
                    containerColor = SnehaDarkSurface,
                    contentColor = SnehaCyan
                ) {
                    navItems.forEach { item ->
                        val selected = currentScreen == item.screen
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentScreen = item.screen },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = { Text(item.title, fontSize = 9.sp, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SnehaCyan,
                                selectedTextColor = SnehaCyan,
                                unselectedIconColor = SnehaTextSecondary,
                                unselectedTextColor = SnehaTextSecondary,
                                indicatorColor = SnehaDarkSurfaceVariant
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isExpanded) {
                NavigationRail(
                    containerColor = SnehaDarkSurface,
                    contentColor = SnehaCyan,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    navItems.forEach { item ->
                        val selected = currentScreen == item.screen
                        NavigationRailItem(
                            selected = selected,
                            onClick = { currentScreen = item.screen },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text(item.title, fontSize = 10.sp) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = SnehaCyan,
                                selectedTextColor = SnehaCyan,
                                unselectedIconColor = SnehaTextSecondary,
                                unselectedTextColor = SnehaTextSecondary,
                                indicatorColor = SnehaDarkSurfaceVariant
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (currentScreen) {
                    SnehaScreen.ASSISTANT -> {
                        AssistantScreen(
                            voiceState = voiceState,
                            audioAmplitude = audioAmplitude,
                            spokenTextLive = spokenTextLive,
                            messages = messages,
                            isVoiceOutputEnabled = isVoiceOutputEnabled,
                            currentPersona = currentPersona,
                            onOpenVoiceSettings = {
                                currentScreen = SnehaScreen.SETTINGS
                            },
                            onOpenSecurityUnlock = {
                                currentScreen = SnehaScreen.SECURITY_UNLOCK
                            },
                            onOpenScreenShare = {
                                currentScreen = SnehaScreen.SCREEN_SHARE
                            },
                            onOpenCallAssistant = {
                                currentScreen = SnehaScreen.CALL_ASSISTANT
                            },
                            onOpenSpamBlocker = {
                                currentScreen = SnehaScreen.SPAM_BLOCKER
                            },
                            onOpenWhatsAppAutoReply = {
                                currentScreen = SnehaScreen.WHATSAPP_AUTO_REPLY
                            },
                            onOpenCallTranslator = {
                                currentScreen = SnehaScreen.CALL_TRANSLATOR
                            },
                            onOpenClassTimetable = {
                                currentScreen = SnehaScreen.CLASS_TIMETABLE
                            },
                            onOpenAntiTheft = {
                                currentScreen = SnehaScreen.ANTI_THEFT
                            },
                            onOpenCallSummary = {
                                currentScreen = SnehaScreen.CALL_SUMMARY
                            },
                            onOpenAiConnector = {
                                currentScreen = SnehaScreen.AI_CONNECTOR
                            },
                            onMicClick = {
                                if (voiceState == VoiceState.SPEAKING) {
                                    voiceManager.stopSpeaking()
                                    voiceManager.setContinuousListening(true)
                                    voiceManager.startListening(playChime = true)
                                } else {
                                    voiceManager.setContinuousListening(true)
                                    voiceManager.startListening(playChime = true)
                                }
                            },
                            onSendMessage = { query -> executeUserQuery(query) },
                            onSpeakMessage = { text -> voiceManager.speak(text) },
                            onToggleVoiceOutput = {
                                isVoiceOutputEnabled = !isVoiceOutputEnabled
                                voiceManager.setVoiceOutput(isVoiceOutputEnabled)
                            },
                            onClearChat = {
                                scope.launch { db.chatMessageDao().clearAll() }
                            }
                        )
                    }

                    SnehaScreen.SPAM_BLOCKER -> {
                        SpamCallerScreen(
                            onTestCall = { testCaller ->
                                CallAssistantManager.triggerSimulatedIncomingCall(testCaller)
                                currentScreen = SnehaScreen.CALL_ASSISTANT
                            }
                        )
                    }

                    SnehaScreen.WHATSAPP_AUTO_REPLY -> {
                        WhatsAppAutoReplyScreen(
                            onSpeakAnnouncement = { voiceManager.speak(it) }
                        )
                    }

                    SnehaScreen.CALL_TRANSLATOR -> {
                        LiveCallTranslatorScreen(
                            onSpeak = { voiceManager.speak(it) }
                        )
                    }

                    SnehaScreen.CLASS_TIMETABLE -> {
                        ClassTimetableScreen(
                            onSpeakAnnouncement = { voiceManager.speak(it) }
                        )
                    }

                    SnehaScreen.SECURITY_UNLOCK -> {
                        SecurityUnlockScreen(
                            onBack = { currentScreen = SnehaScreen.ASSISTANT },
                            onSpeakAnnouncement = { voiceManager.speak(it) }
                        )
                    }

                    SnehaScreen.SCREEN_SHARE -> {
                        ScreenShareScreen(
                            onBack = { currentScreen = SnehaScreen.ASSISTANT },
                            onSpeakAnnouncement = { voiceManager.speak(it) }
                        )
                    }

                    SnehaScreen.CALL_ASSISTANT -> {
                        CallAssistantScreen(
                            onBack = { currentScreen = SnehaScreen.ASSISTANT },
                            onSpeakAnnouncement = { voiceManager.speak(it) }
                        )
                    }

                    SnehaScreen.PHONE_CONTROL -> {
                        PhoneControlScreen(
                            installedApps = installedApps,
                            isTorchActive = isTorchActive,
                            batteryInfo = batteryInfo,
                            onToggleTorch = {
                                isTorchActive = !isTorchActive
                                PhoneControlManager.toggleTorch(context, isTorchActive)
                            },
                            onVolumeUp = {
                                PhoneControlManager.adjustVolume(context, true)
                            },
                            onVolumeDown = {
                                PhoneControlManager.adjustVolume(context, false)
                            },
                            onVolumeMute = {
                                PhoneControlManager.muteVolume(context)
                            },
                            onLaunchApp = { pkg ->
                                PhoneControlManager.launchApp(context, pkg)
                            },
                            onSearchInsideApp = { appName, q ->
                                PhoneControlManager.searchInsideApp(context, appName, q)
                            },
                            onSearchYouTube = { q ->
                                PhoneControlManager.searchYouTube(context, q)
                            },
                            onSearchWeb = { q ->
                                PhoneControlManager.searchWeb(context, q)
                            },
                            onOpenDialer = {
                                PhoneControlManager.openDialer(context)
                            },
                            onOpenSettings = {
                                PhoneControlManager.openAppByName(context, "settings")
                            },
                            onOpenWifi = {
                                PhoneControlManager.openAppByName(context, "wifi")
                            },
                            onOpenCamera = {
                                PhoneControlManager.openAppByName(context, "camera")
                            }
                        )
                    }

                    SnehaScreen.SAFE_MESSAGES -> {
                        MessageReaderScreen(
                            isNotificationAccessEnabled = SnehaNotificationListener.isNotificationServiceEnabled(context),
                            isAutoReadEnabled = isAutoReadEnabled,
                            onToggleAutoRead = { enabled ->
                                isAutoReadEnabled = enabled
                                com.example.engine.BackgroundSpeaker.isAutoReadMessagesEnabled = enabled
                            },
                            notifications = notifications,
                            onOpenNotificationSettings = {
                                SnehaNotificationListener.openNotificationAccessSettings(context)
                            },
                            onSpeakMessage = { text ->
                                voiceManager.speak(text)
                            },
                            onSimulateTestMessage = { sender, text, appName ->
                                scope.launch {
                                    val hasOtp = OtpSafetyGuard.containsOtp(text)
                                    val safeText = OtpSafetyGuard.redactForDisplay(text)
                                    db.notificationLogDao().insertLog(
                                        NotificationLogEntity(
                                            sender = sender,
                                            packageName = "com.test.simulated",
                                            appName = appName,
                                            originalText = text,
                                            safeText = safeText,
                                            containsOtp = hasOtp
                                        )
                                    )
                                    // Announce that new message arrived safely
                                    val spoken = OtpSafetyGuard.sanitizeForSpeech(sender, text)
                                    if (isVoiceOutputEnabled) {
                                        voiceManager.speak("मास्टर, नया संदेश आया है: $spoken")
                                    }
                                }
                            },
                            onReadAllMessages = {
                                scope.launch {
                                    if (notifications.isEmpty()) {
                                        voiceManager.speak("मास्टर, कोई नया संदेश नहीं है।")
                                    } else {
                                        val sb = StringBuilder("मास्टर, आपके पास कुल ${notifications.size} संदेश हैं। ")
                                        notifications.take(4).forEachIndexed { index, notif ->
                                            sb.append("संदेश ${index + 1}: ${OtpSafetyGuard.sanitizeForSpeech(notif.sender, notif.originalText)} ")
                                        }
                                        voiceManager.speak(sb.toString())
                                    }
                                }
                            }
                        )
                    }

                    SnehaScreen.EMERGENCY -> {
                        EmergencyScreen(
                            contacts = contacts,
                            isAlarmPlaying = isAlarmPlaying,
                            isStrobePlaying = isStrobePlaying,
                            onRequestEmergencyUnlock = {
                                if (activity != null) {
                                    PhoneControlManager.requestEmergencyUnlock(activity) { success ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (success) "मास्टर, लॉकस्क्रीन हटाने का अनुरोध सफल!" else "मास्टर, लॉकस्क्रीन अनुरोध अस्वीकार या रद्द हुआ।"
                                            )
                                        }
                                    }
                                    if (isVoiceOutputEnabled) {
                                        voiceManager.speak("मास्टर, इमरजेंसी अनलॉक अनुरोध भेजा गया है।")
                                    }
                                }
                            },
                            onToggleAlarm = {
                                if (isAlarmPlaying) {
                                    PhoneControlManager.stopEmergencyAlarm()
                                    isAlarmPlaying = false
                                } else {
                                    PhoneControlManager.startEmergencyAlarm(context)
                                    isAlarmPlaying = true
                                }
                            },
                            onToggleStrobe = {
                                if (isStrobePlaying) {
                                    PhoneControlManager.stopEmergencyStrobe(context)
                                    isStrobePlaying = false
                                } else {
                                    PhoneControlManager.startEmergencyStrobe(context, scope)
                                    isStrobePlaying = true
                                }
                            },
                            onCallNumber = { number ->
                                PhoneControlManager.openDialer(context, number)
                            },
                            onAddContact = { name, num, rel ->
                                scope.launch {
                                    db.emergencyContactDao().insertContact(
                                        EmergencyContactEntity(name = name, phoneNumber = num, relationship = rel)
                                    )
                                }
                            },
                            onDeleteContact = { id ->
                                scope.launch {
                                    db.emergencyContactDao().deleteContact(id)
                                }
                            }
                        )
                    }

                    SnehaScreen.SETTINGS -> {
                        SettingsScreen(
                            isServiceRunning = isServiceRunning,
                            isVoiceOutputEnabled = isVoiceOutputEnabled,
                            isAutoReadEnabled = isAutoReadEnabled,
                            speechRate = speechRate,
                            speechPitch = speechPitch,
                            currentPersona = currentPersona,
                            isSpeaking = voiceState == VoiceState.SPEAKING,
                            onToggleService = { enable ->
                                isServiceRunning = enable
                                if (enable) {
                                    SnehaVoiceService.startService(context)
                                } else {
                                    SnehaVoiceService.stopService(context)
                                }
                            },
                            onToggleVoiceOutput = { enable ->
                                isVoiceOutputEnabled = enable
                                voiceManager.setVoiceOutput(enable)
                            },
                            onToggleAutoRead = { enable ->
                                isAutoReadEnabled = enable
                                com.example.data.local.VoicePreferences.saveAutoReadEnabled(context, enable)
                                com.example.engine.BackgroundSpeaker.isAutoReadMessagesEnabled = enable
                            },
                            onRateChange = { rate ->
                                speechRate = rate
                                voiceManager.setSpeechRateValue(rate)
                            },
                            onPitchChange = { pitch ->
                                speechPitch = pitch
                                voiceManager.setSpeechPitchValue(pitch)
                            },
                            onSelectPersona = { persona ->
                                currentPersona = persona
                                voiceManager.applyPersona(persona, applyPresetValues = true)
                                speechRate = voiceManager.speechRate
                                speechPitch = voiceManager.speechPitch
                                voiceManager.speak(persona.sampleSpeech)
                            },
                            onResetToPersonaDefaults = {
                                voiceManager.resetToCurrentPersonaDefaults()
                                speechRate = voiceManager.speechRate
                                speechPitch = voiceManager.speechPitch
                                voiceManager.speak("मास्टर, पर्सोना डिफ़ॉल्ट सेटिंग्स लागू हो गई हैं।")
                            },
                            onTestVoice = {
                                if (voiceState == VoiceState.SPEAKING) {
                                    voiceManager.stopSpeaking()
                                } else {
                                    voiceManager.speak(currentPersona.sampleSpeech)
                                }
                            },
                            onTestCustomPhrase = { phrase ->
                                if (voiceState == VoiceState.SPEAKING) {
                                    voiceManager.stopSpeaking()
                                } else {
                                    val userTitle = com.example.data.local.VoicePreferences.getUserName(context)
                                    val greeting = if (phrase.startsWith(userTitle) || phrase.startsWith("जी $userTitle") || phrase.startsWith("नमस्ते")) phrase else "$userTitle, $phrase"
                                    voiceManager.speak(greeting)
                                }
                            },
                            onNavigateToScreen = { targetScreen ->
                                currentScreen = targetScreen
                            }
                        )
                    }

                    SnehaScreen.ANTI_THEFT -> {
                        AntiTheftScreen()
                    }

                    SnehaScreen.CALL_SUMMARY -> {
                        CallSummaryScreen()
                    }

                    SnehaScreen.AI_CONNECTOR -> {
                        CloudConnectorScreen()
                    }

                    SnehaScreen.SMART_HOME -> {
                        com.example.ui.screens.SmartHomeScreen(
                            onBack = { currentScreen = SnehaScreen.ASSISTANT },
                            onVoiceCommand = { executeUserQuery(it) }
                        )
                    }

                    SnehaScreen.CREATIVE_STUDIO -> {
                        com.example.ui.screens.GeminiCreativeStudioScreen(
                            onBack = { currentScreen = SnehaScreen.ASSISTANT }
                        )
                    }
                }
            }
        }
    }
}
