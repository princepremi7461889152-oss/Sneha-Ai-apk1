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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.text.input.ImeAction
import com.example.engine.EarphoneControlManager
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

    val isEarphoneConnected by EarphoneControlManager.isEarphoneConnected.collectAsState()
    val earphoneStatus by EarphoneControlManager.earphoneStatusText.collectAsState()

    val currentWakeWord = com.example.data.local.VoicePreferences.getCustomWakeWord(context)
    val currentUserName = com.example.data.local.VoicePreferences.getUserName(context)
    val isCallRinging by CallAssistantManager.isCallRinging.collectAsState()
    val isCallActive by CallAssistantManager.isCallActive.collectAsState()
    val callerName by CallAssistantManager.currentCaller.collectAsState()

    LaunchedEffect(messages.size, spokenTextLive) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

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
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Brush.linearGradient(listOf(SnehaCyan, SnehaPink)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.sneha_girl_character_1790568612380),
                        contentDescription = "Sneha AI",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
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

                        if (isEarphoneConnected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SnehaPurple.copy(alpha = 0.25f),
                                border = BorderStroke(0.8.dp, SnehaPurple)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Headset,
                                        contentDescription = null,
                                        tint = SnehaCyan,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = earphoneStatus,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SnehaCyan
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = when (voiceState) {
                            VoiceState.LISTENING -> "$currentUserName, सुन रही हूँ... 🎙️"
                            VoiceState.SPEAKING -> "$currentUserName, बोल रही हूँ... 🔊"
                            VoiceState.THINKING -> "$currentUserName, सोच रही हूँ... ⚡"
                            VoiceState.ERROR -> "$currentUserName, सुन रही हूँ... 🎙️"
                            VoiceState.IDLE -> "$currentUserName, माइक लगातार चालू है • बोलिए 🎙️"
                        },
                        fontSize = 11.sp,
                        color = when (voiceState) {
                            VoiceState.LISTENING -> SnehaCyan
                            VoiceState.SPEAKING -> SnehaPurple
                            VoiceState.THINKING -> SnehaPink
                            else -> SnehaCyan
                        }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
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
                .padding(horizontal = 16.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            shape = RoundedCornerShape(20.dp),
            border = borderStroke()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VoiceWaveVisualizer(
                    voiceState = voiceState,
                    amplitude = audioAmplitude,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SnehaCyan.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(SnehaCyan, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "माइक लगातार ऑन • बैकग्राउंड व लॉकस्क्रीन में भी सक्रिय",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SnehaCyan
                    )
                }

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
                    EmptyChatGreeting(userName = currentUserName, onSuggest = onSendMessage)
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
            Column(modifier = Modifier.fillMaxWidth()) {
                // Quick Command Chips Strip
                val quickChips = listOf(
                    "🔦 टॉर्च जलाओ",
                    "⏰ समय क्या है",
                    "🔋 बैटरी कितनी है",
                    "💡 लाइट जलाओ",
                    "🌀 पंखा 4 पर करो",
                    "📞 कॉल लगाओ",
                    "📝 नया नोट लिखो",
                    "🚨 इमरजेंसी SOS",
                    "🌾 भोजपुरी बोलो",
                    "🧮 2500 का 18%"
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(quickChips) { chip ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SnehaDarkSurfaceVariant,
                            border = BorderStroke(1.dp, SnehaCyan.copy(alpha = 0.35f)),
                            modifier = Modifier.clickable {
                                onSendMessage(chip)
                            }
                        ) {
                            Text(
                                text = chip,
                                fontSize = 11.sp,
                                color = SnehaCyan,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                "$currentUserName, यहाँ लिखकर या बोलकर आदेश दें...",
                                fontSize = 13.sp,
                                color = SnehaTextSecondary
                            )
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                val q = textInput.trim()
                                if (q.isNotBlank()) {
                                    onSendMessage(q)
                                    textInput = ""
                                }
                            }
                        ),
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
                            IconButton(
                                onClick = {
                                    val q = textInput.trim()
                                    if (q.isNotBlank()) {
                                        onSendMessage(q)
                                        textInput = ""
                                    }
                                },
                                enabled = textInput.isNotBlank(),
                                modifier = Modifier.testTag("send_message_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "भेजें",
                                    tint = if (textInput.isNotBlank()) SnehaCyan else SnehaTextSecondary.copy(alpha = 0.3f)
                                )
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
private fun EmptyChatGreeting(userName: String = "मास्टर", onSuggest: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Elegant Sneha character portrait
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .border(2.5.dp, Brush.linearGradient(listOf(SnehaCyan, SnehaPink)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.sneha_girl_character_1790568612380),
                contentDescription = "स्नेहा AI",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "नमस्ते $userName जी! कहिए क्या सेवा करूँ?",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = SnehaTextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "माइक ऑन है, बस बोलिए या टाइप कीजिए...",
            fontSize = 13.sp,
            color = SnehaCyan
        )
    }
}

private fun borderStroke() = BorderStroke(1.dp, Color(0xFF2C3252))
