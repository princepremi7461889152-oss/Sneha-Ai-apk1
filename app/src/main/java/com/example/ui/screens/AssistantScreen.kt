package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChatMessageEntity
import com.example.data.model.VoicePersona
import com.example.data.model.VoiceState
import com.example.engine.CallAssistantManager
import com.example.ui.components.MicButton
import com.example.ui.components.VoiceWaveVisualizer
import com.example.ui.theme.SnehaCardBorder
import com.example.ui.theme.SnehaCyan
import com.example.ui.theme.SnehaDarkSurface
import com.example.ui.theme.SnehaDarkSurfaceVariant
import com.example.ui.theme.SnehaGreen
import com.example.ui.theme.SnehaPink
import com.example.ui.theme.SnehaPurple
import com.example.ui.theme.SnehaTextPrimary
import com.example.ui.theme.SnehaTextSecondary

@Composable
fun AssistantScreen(
    voiceState: VoiceState,
    audioAmplitude: Float,
    spokenTextLive: String,
    messages: List<ChatMessageEntity>,
    isVoiceOutputEnabled: Boolean,
    onMicClick: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSpeakMessage: (String) -> Unit,
    onToggleVoiceOutput: () -> Unit,
    onClearChat: () -> Unit,
    currentPersona: VoicePersona = VoicePersona.ALL[0],
    onOpenVoiceSettings: () -> Unit = {},
    onOpenSecurityUnlock: () -> Unit = {},
    onOpenScreenShare: () -> Unit = {},
    onOpenCallAssistant: () -> Unit = {},
    onOpenSpamBlocker: () -> Unit = {},
    onOpenWhatsAppAutoReply: () -> Unit = {},
    onOpenCallTranslator: () -> Unit = {},
    onOpenClassTimetable: () -> Unit = {},
    onOpenAntiTheft: () -> Unit = {},
    onOpenCallSummary: () -> Unit = {},
    onOpenAiConnector: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showFeatureGuideDialog by remember { mutableStateOf(false) }

    val currentWakeWord = com.example.data.local.VoicePreferences.getCustomWakeWord(context)
    val isCallRinging by CallAssistantManager.isCallRinging.collectAsState()
    val isCallActive by CallAssistantManager.isCallActive.collectAsState()
    val callerName by CallAssistantManager.currentCaller.collectAsState()

    LaunchedEffect(messages.size, spokenTextLive) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickChips = listOf(
        "हे $currentWakeWord!",
        "🛡️ स्पैम ब्लॉकर खोलो",
        "💬 व्हाट्सएप ऑटो-रिप्लाई खोलो",
        "🌐 लाइव ट्रांसलेटर खोलो",
        "🎓 मेरी अगली क्लास कब है?",
        "वेक वर्ड बदलो",
        "कॉल पिक करके बोलो मैं क्लास में हूँ",
        "फोन अनलॉक करो",
        "स्क्रीन शेयर करो",
        "यूट्यूब खोलो",
        "टॉर्च चालू करो",
        "सुरक्षित मैसेज पढ़ो",
        "बैटरी कितनी है?"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            brush = Brush.linearGradient(listOf(SnehaCyan, SnehaPurple)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Sneha AI",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (currentWakeWord.equals("स्नेहा", ignoreCase = true)) "स्नेहा AI" else "$currentWakeWord AI",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnehaTextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SnehaDarkSurfaceVariant,
                            border = BorderStroke(0.8.dp, SnehaCyan.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "वेक: $currentWakeWord",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SnehaCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = when (voiceState) {
                            VoiceState.LISTENING -> "मास्टर, सुन रही हूँ... 🎙️"
                            VoiceState.SPEAKING -> "मास्टर, बोल रही हूँ... 🔊"
                            VoiceState.THINKING -> "मास्टर, सोच रही हूँ... ⚡"
                            VoiceState.ERROR -> "मास्टर, वॉयस एरर ठीक हुआ"
                            VoiceState.IDLE -> "मास्टर, मैं हाजिर हूँ • बोलिए"
                        },
                        fontSize = 11.sp,
                        color = when (voiceState) {
                            VoiceState.LISTENING -> SnehaCyan
                            VoiceState.SPEAKING -> SnehaPurple
                            VoiceState.THINKING -> SnehaPink
                            else -> SnehaTextSecondary
                        }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Info & Guide Dialog Trigger
                IconButton(
                    onClick = { showFeatureGuideDialog = true },
                    modifier = Modifier.testTag("btn_header_feature_info")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "फीचर्स व एरर जानकारी",
                        tint = SnehaCyan
                    )
                }

                // Quick Shortcut for Call Assistant
                IconButton(
                    onClick = onOpenCallAssistant,
                    modifier = Modifier.testTag("btn_header_call_assistant")
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = "कॉल अटेंडेंट",
                        tint = SnehaGreen
                    )
                }

                // Quick Shortcut for Security Unlock
                IconButton(
                    onClick = onOpenSecurityUnlock,
                    modifier = Modifier.testTag("btn_header_security_unlock")
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "फोन अनलॉक",
                        tint = SnehaCyan
                    )
                }

                // Quick Shortcut for Screen Share
                IconButton(
                    onClick = onOpenScreenShare,
                    modifier = Modifier.testTag("btn_header_screen_share")
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenShare,
                        contentDescription = "स्क्रीन शेयर",
                        tint = SnehaPurple
                    )
                }

                IconButton(
                    onClick = onOpenVoiceSettings,
                    modifier = Modifier.testTag("btn_open_voice_settings")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "वॉयस कस्टमाइज़ेशन",
                        tint = SnehaCyan
                    )
                }

                IconButton(
                    onClick = onToggleVoiceOutput,
                    modifier = Modifier.testTag("toggle_voice_output_btn")
                ) {
                    Icon(
                        imageVector = if (isVoiceOutputEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = "वॉयस आउटपुट",
                        tint = if (isVoiceOutputEnabled) SnehaCyan else SnehaTextSecondary
                    )
                }

                IconButton(
                    onClick = onClearChat,
                    modifier = Modifier.testTag("clear_chat_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "चैट साफ़ करें",
                        tint = SnehaTextSecondary
                    )
                }
            }
        }

        // Comprehensive Feature & Error 11/5 Diagnostics Dialog
        if (showFeatureGuideDialog) {
            AlertDialog(
                onDismissRequest = { showFeatureGuideDialog = false },
                containerColor = SnehaDarkSurface,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💡 स्नेहा AI फीचर्स व गाइड", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SnehaCyan)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Section 1: Error 5 and 11 explanation
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SnehaDarkSurfaceVariant,
                            border = BorderStroke(1.dp, SnehaPink.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("🔧 वॉयस इनपुट Error 5 और 11 क्या हैं?", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SnehaPink)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("• Error 5 (ERROR_CLIENT): यह एंड्रॉइड स्पीच रिकग्नाइज़र का क्लाइंट स्टेट टकराव था जब रिकग्नाइज़र बार-बार री-क्रिएट हो रहा था।", fontSize = 11.sp, color = SnehaTextPrimary)
                                Text("• Error 11 (ERROR_SERVER_DISCONNECTED): यह Google Speech Service का बैकग्राउंड कनेक्शन टूटने पर आता है।", fontSize = 11.sp, color = SnehaTextPrimary)
                                Text("✅ समाधान: सिंगल-इंस्टेंस कनेक्शन व साइलेंट ऑटो-रिकवरी लागू कर दी गई है। अब यह एरर नहीं आएगा!", fontSize = 11.sp, color = SnehaGreen, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Section 2: 5 Main Features shortcuts
                        Text("🚀 5 नए सशक्त फीचर्स:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SnehaTextPrimary)

                        Button(
                            onClick = {
                                showFeatureGuideDialog = false
                                onOpenSpamBlocker()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SnehaDarkSurfaceVariant),
                            border = BorderStroke(1.dp, SnehaPink.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("🛡️ 1. स्मार्ट स्पैम कॉलर ब्लॉकर (AI Truecaller)", fontSize = 11.sp, color = SnehaPink)
                        }

                        Button(
                            onClick = {
                                showFeatureGuideDialog = false
                                onOpenVoiceSettings()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SnehaDarkSurfaceVariant),
                            border = BorderStroke(1.dp, SnehaCyan.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("🎙️ 2. कस्टम वेक-वर्ड (अपनी मर्जी का नाम)", fontSize = 11.sp, color = SnehaCyan)
                        }

                        Button(
                            onClick = {
                                showFeatureGuideDialog = false
                                onOpenWhatsAppAutoReply()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SnehaDarkSurfaceVariant),
                            border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("💬 3. व्हाट्सएप कॉल व मैसेज ऑटो-रिप्लाई", fontSize = 11.sp, color = Color(0xFF25D366))
                        }

                        Button(
                            onClick = {
                                showFeatureGuideDialog = false
                                onOpenCallTranslator()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SnehaDarkSurfaceVariant),
                            border = BorderStroke(1.dp, SnehaCyan.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("🌐 4. कॉल के दौरान लाइव ट्रांसलेटर", fontSize = 11.sp, color = SnehaCyan)
                        }

                        Button(
                            onClick = {
                                showFeatureGuideDialog = false
                                onOpenClassTimetable()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SnehaDarkSurfaceVariant),
                            border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("🎓 5. स्मार्ट क्लास टाइमटेबल मोड", fontSize = 11.sp, color = Color(0xFFFFB300))
                        }

                        Button(
                            onClick = {
                                showFeatureGuideDialog = false
                                onOpenAntiTheft()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SnehaDarkSurfaceVariant),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("🚨 6. एंटी-थेफ्ट गार्ड व इंट्रूडर सेल्फी", fontSize = 11.sp, color = Color(0xFFFF5252))
                        }

                        Button(
                            onClick = {
                                showFeatureGuideDialog = false
                                onOpenCallSummary()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SnehaDarkSurfaceVariant),
                            border = BorderStroke(1.dp, SnehaCyan.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("📝 7. AI कॉल सारांश व कार्य सूची (Notes)", fontSize = 11.sp, color = SnehaCyan)
                        }

                        Button(
                            onClick = {
                                showFeatureGuideDialog = false
                                onOpenAiConnector()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SnehaDarkSurfaceVariant),
                            border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("☁️ 8. AI क्लाउड कनेक्टर (ChatGPT / Claude)", fontSize = 11.sp, color = Color(0xFF00E676))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFeatureGuideDialog = false }) {
                        Text("समझ गया, धन्यवाद", color = SnehaCyan, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Active Incoming Call Quick Alert Banner
        AnimatedVisibility(visible = isCallRinging || isCallActive) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("assistant_incoming_call_banner"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCallActive) SnehaGreen.copy(alpha = 0.2f) else SnehaPink.copy(alpha = 0.2f)
                ),
                border = BorderStroke(1.5.dp, if (isCallActive) SnehaGreen else SnehaPink),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCallActive) Icons.Default.PhoneInTalk else Icons.Default.Call,
                        contentDescription = null,
                        tint = if (isCallActive) SnehaGreen else SnehaPink,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isCallActive) "स्नेहा कॉल पर बात कर रही है 📞" else "इनकमिंग कॉल आ रही है... 📲",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnehaTextPrimary
                        )
                        Text(
                            text = callerName,
                            fontSize = 11.sp,
                            color = SnehaCyan
                        )
                    }

                    if (isCallRinging) {
                        Button(
                            onClick = {
                                val (ok, msg) = CallAssistantManager.pickCallAndSpeak(context, "मास्टर अभी क्लास में हैं, वे आपसे बाद में बात करेंगे।") { phrase ->
                                    onSpeakMessage(phrase)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SnehaGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_quick_pick_class")
                        ) {
                            Text("पिक करें (क्लास)", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Voice Wave Visualizer & Live speech card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            shape = RoundedCornerShape(20.dp),
            border = borderStroke()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VoiceWaveVisualizer(
                    voiceState = voiceState,
                    amplitude = audioAmplitude,
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(visible = spokenTextLive.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SnehaDarkSurfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "\"$spokenTextLive\"",
                            color = SnehaCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Quick Feature Banner (Spam Blocker, WhatsApp Auto-Reply, Live Translator, Class Timetable, Unlock, Screen Share & Call Assistant)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier
                        .clickable { onOpenSpamBlocker() }
                        .testTag("chip_shortcut_spam_blocker"),
                    color = SnehaDarkSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SnehaPink.copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🛡️", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "स्पैम ब्लॉकर",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnehaPink
                        )
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .clickable { onOpenWhatsAppAutoReply() }
                        .testTag("chip_shortcut_whatsapp"),
                    color = SnehaDarkSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💬", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "व्हाट्सएप ऑटो",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF25D366)
                        )
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .clickable { onOpenCallTranslator() }
                        .testTag("chip_shortcut_translator"),
                    color = SnehaDarkSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SnehaCyan.copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🌐", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "लाइव अनुवादक",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnehaCyan
                        )
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .clickable { onOpenClassTimetable() }
                        .testTag("chip_shortcut_class_timetable"),
                    color = SnehaDarkSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎓", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "क्लास टाइमटेबल",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB300)
                        )
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .clickable { onOpenCallAssistant() }
                        .testTag("chip_shortcut_call_assistant"),
                    color = SnehaDarkSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SnehaGreen.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = SnehaGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "कॉल असिस्टेंट",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SnehaGreen
                        )
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .clickable { onOpenSecurityUnlock() }
                        .testTag("chip_shortcut_unlock"),
                    color = SnehaDarkSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SnehaCyan.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = SnehaCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "फोन अनलॉक",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SnehaCyan
                        )
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .clickable { onOpenScreenShare() }
                        .testTag("chip_shortcut_screenshare"),
                    color = SnehaDarkSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SnehaPurple.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScreenShare,
                            contentDescription = null,
                            tint = SnehaPurple,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "स्क्रीन शेयर",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SnehaPurple
                        )
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .clickable { onOpenAntiTheft() }
                        .testTag("chip_shortcut_antitheft"),
                    color = SnehaDarkSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🚨", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "एंटी-थेफ्ट गार्ड",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5252)
                        )
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .clickable { onOpenCallSummary() }
                        .testTag("chip_shortcut_call_summary"),
                    color = SnehaDarkSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SnehaCyan.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📝", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AI कॉल सारांश",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnehaCyan
                        )
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .clickable { onOpenAiConnector() }
                        .testTag("chip_shortcut_ai_connector"),
                    color = SnehaDarkSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("☁️", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AI कनेक्टर",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676)
                        )
                    }
                }
            }
        }

        // Quick Suggestions Bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickChips) { chip ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SnehaDarkSurfaceVariant,
                    border = borderStroke(),
                    modifier = Modifier
                        .clickable { onSendMessage(chip) }
                        .testTag("quick_chip_${chip.hashCode()}")
                ) {
                    Text(
                        text = chip,
                        fontSize = 12.sp,
                        color = SnehaCyan,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Chat Message Feed
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    EmptyChatGreeting(onSuggest = onSendMessage)
                }
            }

            items(messages) { msg ->
                ChatMessageBubble(
                    message = msg,
                    onReplay = { onSpeakMessage(msg.text) }
                )
            }
        }

        // Bottom Input Dock
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SnehaDarkSurface,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = {
                        Text(
                            "मास्टर, स्नेहा से कुछ भी कहें या आदेश दें...",
                            fontSize = 13.sp,
                            color = SnehaTextSecondary
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_text_input"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SnehaDarkSurfaceVariant,
                        unfocusedContainerColor = SnehaDarkSurfaceVariant,
                        focusedBorderColor = SnehaCyan,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = SnehaTextPrimary,
                        unfocusedTextColor = SnehaTextPrimary
                    ),
                    maxLines = 3,
                    trailingIcon = {
                        if (textInput.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val q = textInput.trim()
                                    if (q.isNotBlank()) {
                                        onSendMessage(q)
                                        textInput = ""
                                    }
                                },
                                modifier = Modifier.testTag("send_message_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "भेजें",
                                    tint = SnehaCyan
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                MicButton(
                    voiceState = voiceState,
                    onClick = onMicClick
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessageEntity,
    onReplay: () -> Unit
) {
    val isUser = message.isUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(SnehaCyan, SnehaPurple)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) SnehaPurple.copy(alpha = 0.8f) else SnehaDarkSurfaceVariant
            ),
            border = BorderStroke(1.dp, if (isUser) SnehaPurple else SnehaCardBorder),
            modifier = Modifier
                .widthIn(max = 280.dp)
                .testTag(if (isUser) "user_message_bubble" else "sneha_message_bubble")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.isOtpMasked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "OTP Masked",
                            tint = SnehaPink,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "OTP सुरक्षा सक्रिय",
                            color = SnehaPink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = message.text,
                    color = SnehaTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                if (!isUser) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onReplay,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "सुनाएं",
                                tint = SnehaCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFF334155), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyChatGreeting(onSuggest: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
        shape = RoundedCornerShape(20.dp),
        border = borderStroke()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "नमस्ते मास्टर जी! मैं स्नेha हूँ 🌸",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SnehaCyan
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "आपकी निजी AI वॉयस व फोन असिस्टेंट। Siri की तरह लॉकस्क्रीन पर भी 'हे स्नेहा' बोलते ही तुरंत सक्रिय हो जाऊँगी।",
                fontSize = 13.sp,
                color = SnehaTextSecondary,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "मास्टर, आप कह सकते हैं:\n• 'स्नेहा, कॉल पिक करके बोलो कि मैं क्लास में हूँ'\n• 'स्नेहा, फोन अनलॉक करो' (पिन/पैटर्न से)\n• 'स्नेहा, स्क्रीन शेयर करो' (रियल-टाइम लाइव)\n• 'यूट्यूब पर गाने चलाओ'\n• 'टॉर्च चालू करो'\n• 'सुरक्षित मैसेज पढ़ो' (OTP गोपनीय)\n• 'मेरा पिन 1234 है फोन अनलॉक करो'",
                fontSize = 12.sp,
                color = Color(0xFFCBD5E1),
                lineHeight = 19.sp
            )
        }
    }
}

private fun borderStroke() = BorderStroke(1.dp, Color(0xFF2C3252))
