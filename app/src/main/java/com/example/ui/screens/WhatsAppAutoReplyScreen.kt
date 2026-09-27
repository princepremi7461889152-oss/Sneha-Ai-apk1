package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.VoicePreferences
import com.example.engine.ClassTimetableManager
import com.example.engine.WhatsAppAutoReplyLog
import com.example.engine.WhatsAppManager
import com.example.service.SnehaNotificationListener
import com.example.ui.theme.SnehaBgDark
import com.example.ui.theme.SnehaCyan
import com.example.ui.theme.SnehaDarkCard
import com.example.ui.theme.SnehaDarkSurface
import com.example.ui.theme.SnehaDarkSurfaceVariant
import com.example.ui.theme.SnehaTextPrimary
import com.example.ui.theme.SnehaTextSecondary

@Composable
fun WhatsAppAutoReplyScreen(
    modifier: Modifier = Modifier,
    onSpeakAnnouncement: (String) -> Unit
) {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(VoicePreferences.isWhatsAppAutoReplyEnabled(context)) }
    var replyText by remember { mutableStateOf(VoicePreferences.getWhatsAppReplyText(context)) }
    val replyLogs by WhatsAppManager.replyLogs.collectAsState()
    val isClassActive = ClassTimetableManager.isCurrentClassActive(context)
    val activeClass = ClassTimetableManager.getActiveClass(context)

    val isNotifListenerGranted = remember {
        SnehaNotificationListener.isNotificationServiceEnabled(context)
    }

    val presetMessages = listOf(
        "मास्टर अभी क्लास में हैं, बाद में रिप्लाई करेंगे।",
        "मास्टर अभी ड्राइविंग कर रहे हैं, जरूरी हो तो कॉल करें।",
        "नमस्ते! मैं स्नेहा AI हूँ। मास्टर अभी व्यस्त हैं, जल्द संपर्क करेंगे।",
        "अभी मीटिंग में हूँ, 1 घंटे बाद बात होगी।"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SnehaBgDark)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SnehaDarkCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color(0xFF25D366).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = "WhatsApp",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "व्हाट्सएप कॉल व मैसेज ऑटो-रिप्लाई",
                            color = SnehaTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isEnabled) "सक्रिय: मैसेज आने पर खुद जवाब जाएगा" else "बंद है",
                            color = if (isEnabled) Color(0xFF25D366) else Color.Red,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { checked ->
                            isEnabled = checked
                            VoicePreferences.saveWhatsAppAutoReplyEnabled(context, checked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF25D366),
                            checkedTrackColor = Color(0xFF25D366).copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("whatsapp_auto_reply_switch")
                    )
                }
            }
        }

        // Notification Access Banner (if missing)
        if (!isNotifListenerGranted) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF332014)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFFFB300))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "नोटिफिकेशन एक्सेस अनुमति आवश्यक",
                                color = SnehaTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "व्हाट्सएप मैसेज पढ़ने व ऑटो-रिप्लाई करने के लिए नोटिफिकेशन अनुमति चालू करें।",
                                color = SnehaTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = { SnehaNotificationListener.openNotificationAccessSettings(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("अनुमति दें", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Smart Class Mode Override Status
        if (isClassActive && activeClass != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14243A)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎓", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "स्मार्ट क्लास मोड सक्रिय: ${activeClass.subject}",
                                color = SnehaCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "व्हाट्सएप रिप्लाई अपने-आप सेट है: 'मास्टर अभी ${activeClass.subject} क्लास में हैं (${activeClass.startTime}-${activeClass.endTime})'",
                                color = SnehaTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Custom Reply Message Editor
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "✍️ सामान्य ऑटो-रिप्लाई संदेश",
                        color = SnehaCyan,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset Quick Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetMessages.forEach { preset ->
                            FilterChip(
                                selected = replyText == preset,
                                onClick = {
                                    replyText = preset
                                    VoicePreferences.saveWhatsAppReplyText(context, preset)
                                },
                                label = { Text(preset, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SnehaCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = SnehaCyan,
                                    labelColor = SnehaTextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = {
                            replyText = it
                            VoicePreferences.saveWhatsAppReplyText(context, it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("whatsapp_reply_textfield"),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SnehaCyan,
                            unfocusedBorderColor = SnehaDarkSurfaceVariant,
                            focusedTextColor = SnehaTextPrimary,
                            unfocusedTextColor = SnehaTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                // Simulate an incoming WhatsApp message and verify auto-reply
                                val testLog = WhatsAppAutoReplyLog(
                                    senderName = "विकास वर्मा",
                                    incomingMessage = "भाई क्या हाल है? फ्री है क्या?",
                                    repliedText = replyText,
                                    time = "अभी टेस्ट"
                                )
                                WhatsAppManager.clearLogs()
                                onSpeakAnnouncement("व्हाट्सएप टेस्ट: विकास को ऑटो-रिप्लाई भेज दिया गया है!")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("whatsapp_simulate_test_button")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("टेस्ट रिप्लाई भेजें", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                onSpeakAnnouncement("मास्टर, व्हाट्सएप पर अंकुर जैन की वॉइस कॉल आ रही है!")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SnehaDarkSurfaceVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = SnehaCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("कॉल अलर्ट टेस्ट", color = SnehaCyan, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Auto-Replied History Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 हाल ही में दिए गए ऑटो-रिप्लाई लॉग",
                    color = SnehaTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "${replyLogs.size} संदेश",
                    color = SnehaTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // History items
        items(replyLogs) { log ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = log.senderName,
                            color = SnehaCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = log.time,
                            color = SnehaTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "आया संदेश: \"${log.incomingMessage}\"",
                        color = SnehaTextPrimary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "स्नेहा ने भेजा: \"${log.repliedText}\"",
                        color = Color(0xFF25D366),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
