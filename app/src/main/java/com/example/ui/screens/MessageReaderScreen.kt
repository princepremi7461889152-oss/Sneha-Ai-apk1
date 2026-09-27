package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.NotificationLogEntity
import com.example.engine.OtpSafetyGuard
import com.example.ui.theme.SnehaAmber
import com.example.ui.theme.SnehaCyan
import com.example.ui.theme.SnehaDarkSurface
import com.example.ui.theme.SnehaDarkSurfaceVariant
import com.example.ui.theme.SnehaEmerald
import com.example.ui.theme.SnehaTextPrimary
import com.example.ui.theme.SnehaTextSecondary

@Composable
fun MessageReaderScreen(
    isNotificationAccessEnabled: Boolean,
    isAutoReadEnabled: Boolean = true,
    onToggleAutoRead: (Boolean) -> Unit = {},
    notifications: List<NotificationLogEntity>,
    onOpenNotificationSettings: () -> Unit,
    onSpeakMessage: (String) -> Unit,
    onSimulateTestMessage: (sender: String, text: String, appName: String) -> Unit,
    onReadAllMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Header
        item {
            Text(
                text = "सुरक्षित संदेश वाचक (OTP सुरक्षा) 🛡️",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SnehaTextPrimary
            )
            Text(
                text = "स्नेहा आपके नए संदेश बोलकर सुनाएगी, लेकिन किसी भी OTP को कभी नहीं पढ़ेगी।",
                fontSize = 13.sp,
                color = SnehaTextSecondary
            )
        }

        // Auto-Read Aloud Toggle Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, if (isAutoReadEnabled) SnehaCyan else Color(0xFF2C3252))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(SnehaCyan.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = SnehaCyan
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "आने वाले संदेश स्वतः जोर से पढ़ें",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SnehaTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "बैकग्राउंड और लॉकस्क्रीन पर मैसेज आते ही स्नेहा पढ़कर सुनाएगी (OTP छोड़कर)",
                                fontSize = 11.sp,
                                color = SnehaTextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Switch(
                        checked = isAutoReadEnabled,
                        onCheckedChange = onToggleAutoRead,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SnehaCyan,
                            checkedTrackColor = SnehaCyan.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("switch_auto_read_messages")
                    )
                }
            }
        }

        // Security Shield Highlight Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F2622)
                ),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, SnehaEmerald)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(SnehaEmerald.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security Shield",
                            tint = SnehaEmerald,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "OTP गोपनीयता सुरक्षा सक्रिय है 🔒",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnehaEmerald
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "बैंक, यूपीआई, लॉगिन व 4-8 अंकों के पासवर्ड कोड को ऑडियो में नहीं बोला जाता और सुरक्षित रूप से ब्लॉक रखा जाता है।",
                            fontSize = 12.sp,
                            color = Color(0xFFA7F3D0),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Notification Access Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF2C3252))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isNotificationAccessEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isNotificationAccessEnabled) SnehaEmerald else SnehaAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isNotificationAccessEnabled) "नोटिफिकेशन लिसनर चालू है" else "नोटिफिकेशन अनुमति आवश्यक",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SnehaTextPrimary
                            )
                        }

                        if (!isNotificationAccessEnabled) {
                            Button(
                                onClick = onOpenNotificationSettings,
                                colors = ButtonDefaults.buttonColors(containerColor = SnehaAmber),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("btn_grant_notif_perm")
                            ) {
                                Text("अनुमति दें", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onReadAllMessages,
                            colors = ButtonDefaults.buttonColors(containerColor = SnehaCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("btn_read_all_messages")
                        ) {
                            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("स्नेहा, हाल के संदेश पढ़ो", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Test Simulator Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF2C3252))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(
                        text = "सुरक्षा टेस्ट सिमुलेटर (OTP फ़िल्टर जांचें) 🧪",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SnehaCyan
                    )
                    Text(
                        text = "टेस्ट संदेश भेजकर देखें कि स्नेहा कैसे OTP को छिपाती है और सामान्य संदेश पढ़ती है:",
                        fontSize = 12.sp,
                        color = SnehaTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onSimulateTestMessage(
                                    "SBI Bank",
                                    "Dear Customer, Your OTP for Rs 4,999 is 728491. Do not share.",
                                    "SBI YONO"
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("btn_test_bank_otp")
                        ) {
                            Text("1. बैंक OTP SMS", fontSize = 11.sp, color = SnehaAmber)
                        }

                        OutlinedButton(
                            onClick = {
                                onSimulateTestMessage(
                                    "Google Auth",
                                    "G-948102 is your Google verification security code.",
                                    "Google"
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("btn_test_google_otp")
                        ) {
                            Text("2. Google कोड", fontSize = 11.sp, color = SnehaCyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = {
                            onSimulateTestMessage(
                                "Amit (WhatsApp)",
                                "Bhai shaam ko milte hain 7 baje chai pe!",
                                "WhatsApp"
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_test_normal_msg")
                    ) {
                        Text("3. सामान्य संदेश (जो स्नेहा पूरा पढ़ेगी)", fontSize = 11.sp, color = SnehaEmerald)
                    }
                }
            }
        }

        // Received Notifications List
        item {
            Text(
                text = "हाल के संदेश एवं अलर्ट (${notifications.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = SnehaCyan
            )
        }

        if (notifications.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "अभी कोई नया संदेश नहीं है। ऊपर दिए टेस्ट बटन दबाकर OTP सुरक्षा देखें!",
                            color = SnehaTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        items(notifications) { notif ->
            NotificationItemCard(
                item = notif,
                onSpeak = {
                    val safeSpeech = OtpSafetyGuard.sanitizeForSpeech(notif.sender, notif.originalText)
                    onSpeakMessage(safeSpeech)
                }
            )
        }
    }
}

@Composable
private fun NotificationItemCard(
    item: NotificationLogEntity,
    onSpeak: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (item.containsOtp) SnehaAmber.copy(alpha = 0.5f) else Color(0xFF242A44))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (item.containsOtp) SnehaAmber.copy(alpha = 0.2f) else SnehaCyan.copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (item.containsOtp) Icons.Default.Lock else Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (item.containsOtp) SnehaAmber else SnehaCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = item.sender,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnehaTextPrimary
                        )
                        Text(
                            text = item.appName,
                            fontSize = 11.sp,
                            color = SnehaTextSecondary
                        )
                    }
                }

                if (item.containsOtp) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SnehaAmber.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "OTP प्रोटेक्टेड 🔒",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnehaAmber,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.safeText,
                fontSize = 13.sp,
                color = SnehaTextPrimary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onSpeak,
                    colors = ButtonDefaults.buttonColors(containerColor = SnehaDarkSurfaceVariant),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "सुनाएं",
                        tint = SnehaCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (item.containsOtp) "सुरक्षित रूप से सुनें" else "सुनें",
                        fontSize = 11.sp,
                        color = SnehaCyan
                    )
                }
            }
        }
    }
}
