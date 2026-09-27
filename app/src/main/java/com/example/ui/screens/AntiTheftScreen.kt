package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.engine.AntiTheftManager
import com.example.ui.theme.SnehaBgDark
import com.example.ui.theme.SnehaCyan
import com.example.ui.theme.SnehaDarkCard
import com.example.ui.theme.SnehaDarkSurface
import com.example.ui.theme.SnehaDarkSurfaceVariant
import com.example.ui.theme.SnehaPink
import com.example.ui.theme.SnehaTextPrimary
import com.example.ui.theme.SnehaTextSecondary

@Composable
fun AntiTheftScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPocketArmed by AntiTheftManager.isPocketGuardArmed.collectAsState()
    val isMotionArmed by AntiTheftManager.isMotionGuardArmed.collectAsState()
    val isWrongPinEnabled by AntiTheftManager.isWrongPinAlertEnabled.collectAsState()
    val isAlarmActive by AntiTheftManager.isAlarmActive.collectAsState()
    val intruderLogs by AntiTheftManager.intruderLogs.collectAsState()

    val isAnyArmed = isPocketArmed || isMotionArmed || isWrongPinEnabled

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SnehaBgDark)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SnehaDarkCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    1.5.dp,
                    if (isAlarmActive) SnehaPink else if (isAnyArmed) SnehaCyan else Color.Gray.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        brush = Brush.linearGradient(
                                            if (isAlarmActive) listOf(SnehaPink, Color.Red)
                                            else listOf(SnehaCyan, Color(0xFF00796B))
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAlarmActive) Icons.Default.Warning else Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "एंटी-थेफ्ट व इंट्रूडर गार्ड",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SnehaTextPrimary
                                )
                                Text(
                                    text = if (isAlarmActive) "⚠️ सायरन बज रहा है!" else if (isAnyArmed) "सुरक्षा सक्रिय है (Armed) 🛡️" else "सुरक्षा निष्क्रिय है",
                                    fontSize = 12.sp,
                                    color = if (isAlarmActive) SnehaPink else if (isAnyArmed) SnehaCyan else SnehaTextSecondary
                                )
                            }
                        }

                        if (isAlarmActive) {
                            Button(
                                onClick = { AntiTheftManager.stopAlarm(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = SnehaPink),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("btn_stop_alarm")
                            ) {
                                Icon(Icons.Default.StopCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("बंद करें", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "पॉकेट से फोन खींचने, गलत पिन दर्ज करने या टेबल से फोन उठाने पर स्नेहा तुरंत अलार्म बजाकर घुसपैठिए की गुप्त जानकारी सेव कर लेती है।",
                        fontSize = 12.sp,
                        color = SnehaTextSecondary,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                if (isAlarmActive) {
                                    AntiTheftManager.stopAlarm(context)
                                } else {
                                    AntiTheftManager.triggerTheftAlarm(context, "मास्टर! एंटी-थेफ्ट परीक्षण सायरन चालू किया गया है!")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAlarmActive) SnehaPink else SnehaDarkSurfaceVariant
                            ),
                            border = BorderStroke(1.dp, SnehaPink.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_test_theft_siren")
                        ) {
                            Icon(
                                imageVector = if (isAlarmActive) Icons.Default.StopCircle else Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = if (isAlarmActive) Color.White else SnehaPink,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAlarmActive) "सायरन रोकें" else "सायरन टेस्ट करें",
                                fontSize = 11.sp,
                                color = if (isAlarmActive) Color.White else SnehaPink,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Toggles Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "सुरक्षा मॉनिटरिंग ऑप्शन्स",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SnehaCyan
                    )

                    // 1. Pocket Pickpocket Guard
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SnehaCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Sensors, contentDescription = null, tint = SnehaCyan, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("पॉकेट पिकपॉकेट गार्ड", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SnehaTextPrimary)
                                Text("जेब से फोन खींचते ही सायरन बजाएं (Proximity Sensor)", fontSize = 11.sp, color = SnehaTextSecondary)
                            }
                        }
                        Switch(
                            checked = isPocketArmed,
                            onCheckedChange = { AntiTheftManager.setPocketGuard(context, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SnehaCyan,
                                checkedTrackColor = SnehaCyan.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.testTag("switch_pocket_guard")
                        )
                    }

                    // 2. Motion / Table Pick Guard
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFFB300).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("डोंट टच माय फोन (टेबल मोशन)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SnehaTextPrimary)
                                Text("टेबल या डेस्क से फोन हिलाने पर अलार्म (Accelerometer)", fontSize = 11.sp, color = SnehaTextSecondary)
                            }
                        }
                        Switch(
                            checked = isMotionArmed,
                            onCheckedChange = { AntiTheftManager.setMotionGuard(context, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFFB300),
                                checkedTrackColor = Color(0xFFFFB300).copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.testTag("switch_motion_guard")
                        )
                    }

                    // 3. Wrong PIN Intruder Alert
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SnehaPink.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = SnehaPink, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("गलत पिन इंट्रूडर सेल्फी व अलर्ट", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SnehaTextPrimary)
                                Text("गलत पिन पर फ्रंट कैमरे से फोटो व अलर्ट लॉग", fontSize = 11.sp, color = SnehaTextSecondary)
                            }
                        }
                        Switch(
                            checked = isWrongPinEnabled,
                            onCheckedChange = { AntiTheftManager.setWrongPinAlert(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SnehaPink,
                                checkedTrackColor = SnehaPink.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.testTag("switch_wrong_pin_guard")
                        )
                    }
                }
            }
        }

        // Intruder Logbook Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🚨 इंट्रूडर व चोरी प्रयास लॉग्स (${intruderLogs.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SnehaTextPrimary
                )
                if (intruderLogs.isNotEmpty()) {
                    IconButton(
                        onClick = { AntiTheftManager.clearLogs() },
                        modifier = Modifier.testTag("btn_clear_intruder_logs")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "लॉग साफ़ करें", tint = SnehaTextSecondary)
                    }
                }
            }
        }

        // Intruder Log Items
        if (intruderLogs.isEmpty()) {
            item {
                Surface(
                    color = SnehaDarkSurface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "कोई संदिग्ध गतिविधि दर्ज नहीं हुई है। आपका डिवाइस पूरी तरह सुरक्षित है।",
                        fontSize = 12.sp,
                        color = SnehaTextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(intruderLogs, key = { it.id }) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SnehaPink.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SnehaPink.copy(alpha = 0.2f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(log.avatarEmoji, fontSize = 20.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = log.triggerReason,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SnehaTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = SnehaPink.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = log.threatLevel,
                                        fontSize = 10.sp,
                                        color = SnehaPink,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = log.timeFormatted,
                                    fontSize = 11.sp,
                                    color = SnehaTextSecondary
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "कैप्चर की गई फोटो",
                            tint = SnehaCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
