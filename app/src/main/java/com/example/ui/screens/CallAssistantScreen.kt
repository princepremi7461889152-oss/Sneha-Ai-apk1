package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CallAssistantManager
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
fun CallAssistantScreen(
    onBack: () -> Unit,
    onSpeakAnnouncement: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isRinging by CallAssistantManager.isCallRinging.collectAsState()
    val isCallActive by CallAssistantManager.isCallActive.collectAsState()
    val currentCaller by CallAssistantManager.currentCaller.collectAsState()
    val autoAttendantEnabled by CallAssistantManager.autoAttendantEnabled.collectAsState()
    val defaultMessage by CallAssistantManager.defaultAttendantMessage.collectAsState()
    val callLogs by CallAssistantManager.callLogs.collectAsState()

    var customMessageInput by remember { mutableStateOf(defaultMessage) }
    var actionFeedback by remember { mutableStateOf<String?>(null) }

    val presetMessages = listOf(
        "मास्टर अभी क्लास में हैं, कृपया बाद में कॉल करें।",
        "मास्टर अभी मीटिंग में व्यस्त हैं, वे बाद में बात करेंगे।",
        "मास्टर अभी ड्राइविंग कर रहे हैं, बाद में संपर्क करें।",
        "मास्टर अभी आराम कर रहे हैं, जरूरी हो तो संदेश भेजें।"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ringing_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("call_assistant_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "वापस",
                    tint = SnehaTextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "AI कॉल अटेंडेंट",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SnehaTextPrimary
                )
                Text(
                    text = "कॉल पिक करके कॉलर को बोलें: 'मास्टर क्लास में हैं'",
                    fontSize = 12.sp,
                    color = SnehaCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Incoming Call Banner (If Ringing or Active)
        AnimatedVisibility(visible = isRinging || isCallActive) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("incoming_call_card"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCallActive) SnehaGreen.copy(alpha = 0.15f) else SnehaPink.copy(alpha = 0.15f)
                ),
                border = BorderStroke(1.5.dp, if (isCallActive) SnehaGreen else SnehaPink),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .scale(if (isRinging) pulseScale else 1f)
                            .background(
                                if (isCallActive) SnehaGreen.copy(alpha = 0.3f) else SnehaPink.copy(alpha = 0.3f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCallActive) Icons.Default.PhoneInTalk else Icons.Default.Call,
                            contentDescription = "Call Status",
                            tint = if (isCallActive) SnehaGreen else SnehaPink,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isCallActive) "📞 कॉल लाइव अटेंड हो रही है (Sneha Active)" else "📲 इनकमिंग कॉल आ रही है...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SnehaTextPrimary
                    )

                    Text(
                        text = currentCaller,
                        fontSize = 13.sp,
                        color = SnehaCyan,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val (ok, summary) = CallAssistantManager.pickCallAndSpeak(
                                    context = context,
                                    customMessage = customMessageInput
                                ) { phrase ->
                                    onSpeakAnnouncement(phrase)
                                }
                                actionFeedback = summary
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_pick_and_speak"),
                            colors = ButtonDefaults.buttonColors(containerColor = SnehaGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("पिक करें व बोलें", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val msg = CallAssistantManager.endOrRejectCall(context) { phrase ->
                                    onSpeakAnnouncement(phrase)
                                }
                                actionFeedback = msg
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_reject_call"),
                            colors = ButtonDefaults.buttonColors(containerColor = SnehaPink),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CallEnd, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("कॉल काटें", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Auto Attendant Status & Toggle Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            border = BorderStroke(1.dp, SnehaCardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = SnehaCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "स्नेहा ऑटो कॉल अटेंडेंट",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SnehaTextPrimary
                            )
                            Text(
                                text = if (autoAttendantEnabled) "सक्रिय • आपके आदेश पर कॉल पिक करके बोलेगी" else "निष्क्रिय",
                                fontSize = 11.sp,
                                color = if (autoAttendantEnabled) SnehaGreen else SnehaTextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = autoAttendantEnabled,
                        onCheckedChange = { CallAssistantManager.setAutoAttendantEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = SnehaCyan)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Voice Avatar / Voice Changer Card
        val activeAvatar by CallAssistantManager.activeVoiceAvatar.collectAsState()
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            border = BorderStroke(1.dp, SnehaCyan.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎭", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "वॉयस चेंजर कॉल अटेंडेंट (Voice Avatar)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnehaCyan
                        )
                        Text(
                            text = "कॉल पिक करते समय कॉलर से किस आवाज में बात करनी है चुनें (क्लिक करके सुनें)",
                            fontSize = 11.sp,
                            color = SnehaTextSecondary
                        )
                    }
                }

                com.example.engine.CallVoiceAvatar.values().forEach { avatar ->
                    val isSelected = activeAvatar == avatar
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) SnehaDarkSurfaceVariant else SnehaDarkSurface,
                        border = BorderStroke(1.dp, if (isSelected) SnehaCyan else Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                CallAssistantManager.setActiveVoiceAvatar(avatar)
                                onSpeakAnnouncement("${avatar.prefixIntro}मास्टर, यह मेरी आवाज का पूर्वावलोकन है।")
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text(avatar.emoji, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(avatar.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SnehaTextPrimary)
                                    Text(avatar.description, fontSize = 10.sp, color = SnehaTextSecondary)
                                }
                            }
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SnehaCyan, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preset Quick Messages for Caller
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            border = BorderStroke(1.dp, SnehaCardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "कॉलर के लिए पूर्वनिर्धारित संदेश (Quick Presets)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SnehaTextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                presetMessages.forEach { msg ->
                    val isSelected = customMessageInput == msg
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                customMessageInput = msg
                                CallAssistantManager.setDefaultMessage(msg)
                            },
                        color = if (isSelected) SnehaPurple.copy(alpha = 0.25f) else SnehaDarkSurfaceVariant,
                        border = BorderStroke(1.dp, if (isSelected) SnehaCyan else Color.Transparent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (msg.contains("क्लास")) Icons.Default.School else Icons.Default.Work,
                                contentDescription = null,
                                tint = if (isSelected) SnehaCyan else SnehaTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = msg,
                                fontSize = 12.sp,
                                color = if (isSelected) SnehaCyan else SnehaTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Text Input
                Text(
                    text = "कस्टम संदेश संपादित करें:",
                    fontSize = 12.sp,
                    color = SnehaTextSecondary
                )
                OutlinedTextField(
                    value = customMessageInput,
                    onValueChange = { customMessageInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("custom_attendant_message_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SnehaTextPrimary,
                        unfocusedTextColor = SnehaTextPrimary,
                        focusedBorderColor = SnehaCyan
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val sample = "नमस्ते, ${CallAssistantManager.transformUserPromptToMasterThirdPerson(customMessageInput)}"
                            onSpeakAnnouncement(sample)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("आवाज सुनें (Test TTS)", fontSize = 11.sp, color = SnehaCyan)
                    }

                    Button(
                        onClick = {
                            CallAssistantManager.setDefaultMessage(customMessageInput)
                            onSpeakAnnouncement("मास्टर, डिफ़ॉल्ट कॉल संदेश सहेज लिया गया है।")
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SnehaCyan)
                    ) {
                        Text("सहेजें (Save)", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Simulator Card for Testing
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            border = BorderStroke(1.dp, SnehaCardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🧪 कॉल सिमुलेटर (Test Emulator Call)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SnehaCyan
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ब्राउज़र एमुलेटर या बिना सिम के टेस्ट करने के लिए नीचे टैप करें। इसके बाद बोलें: 'स्नेहा कॉल पिक करके बोलो कि मैं क्लास में हूँ'।",
                    fontSize = 12.sp,
                    color = SnehaTextSecondary,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        CallAssistantManager.triggerSimulatedIncomingCall("प्रोफेसर वर्मा (+91 94321-78900)")
                        onSpeakAnnouncement("मास्टर, प्रोफेसर वर्मा की कॉल आ रही है!")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_trigger_simulated_call"),
                    colors = ButtonDefaults.buttonColors(containerColor = SnehaPurple)
                ) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("नकली इनकमिंग कॉल ट्रिगर करें", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Voice Command Tips Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            border = BorderStroke(1.dp, SnehaCardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🎙️ वॉयस कमांड्स (Voice Commands)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SnehaCyan
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• \"स्नेहा, कॉल पिक करके बोलो कि मैं क्लास में हूँ\"\n• \"स्नेहा, कॉल उठाओ और बोलो कि मैं मीटिंग में हूँ\"\n• \"स्नेहा, कॉल रिसीव करो\"\n• \"स्नेहा, कॉल काटो\" या \"कॉल रिजेक्ट करो\"",
                    fontSize = 12.sp,
                    color = SnehaTextSecondary,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent Call Attendant Logs
        Text(
            text = "हाल के कॉल रिकॉर्ड्स (Call Logs)",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = SnehaTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (callLogs.isEmpty()) {
            Text("कोई कॉल रिकॉर्ड नहीं मिला।", fontSize = 12.sp, color = SnehaTextSecondary)
        } else {
            callLogs.take(5).forEach { log ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = SnehaDarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = log.caller, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SnehaTextPrimary)
                            Text(text = log.time, fontSize = 10.sp, color = SnehaTextSecondary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = log.messageSpoken, fontSize = 11.sp, color = SnehaCyan)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SnehaGreen, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = log.status, fontSize = 10.sp, color = SnehaGreen)
                        }
                    }
                }
            }
        }
    }
}
