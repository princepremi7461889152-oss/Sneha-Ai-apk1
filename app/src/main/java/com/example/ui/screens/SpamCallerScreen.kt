package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.engine.CallerCategory
import com.example.engine.SpamCallerManager
import com.example.engine.SpamReport
import com.example.ui.theme.SnehaBgDark
import com.example.ui.theme.SnehaCyan
import com.example.ui.theme.SnehaDarkCard
import com.example.ui.theme.SnehaDarkSurface
import com.example.ui.theme.SnehaDarkSurfaceVariant
import com.example.ui.theme.SnehaPink
import com.example.ui.theme.SnehaTextPrimary
import com.example.ui.theme.SnehaTextSecondary

@Composable
fun SpamCallerScreen(
    modifier: Modifier = Modifier,
    onTestCall: (String) -> Unit
) {
    val context = LocalContext.current
    var isAutoBlockEnabled by remember { mutableStateOf(VoicePreferences.isSpamBlockEnabled(context)) }
    val blockedLogs by SpamCallerManager.blockedSpamLogs.collectAsState()

    var lookupQuery by remember { mutableStateOf("") }
    var lookupResult by remember { mutableStateOf<SpamReport?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SnehaBgDark)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
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
                            .background(
                                if (isAutoBlockEnabled) SnehaCyan.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAutoBlockEnabled) Icons.Default.Shield else Icons.Default.Warning,
                            contentDescription = "Spam Shield",
                            tint = if (isAutoBlockEnabled) SnehaCyan else Color.Red,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI स्पैम कॉलर गार्ड (Truecaller)",
                            color = SnehaTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = if (isAutoBlockEnabled) "सक्रिय: स्पैम व फ्रॉड कॉल खुद ब्लॉक होंगे" else "सुरक्षा बंद है",
                            color = if (isAutoBlockEnabled) Color(0xFF00E676) else Color.Red,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = isAutoBlockEnabled,
                        onCheckedChange = { checked ->
                            isAutoBlockEnabled = checked
                            VoicePreferences.saveSpamBlockEnabled(context, checked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SnehaCyan,
                            checkedTrackColor = SnehaCyan.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("spam_block_switch")
                    )
                }
            }
        }

        // Live AI Number Scanner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔍 AI नंबर लुकअप व स्पैम चेकर",
                        color = SnehaCyan,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = lookupQuery,
                            onValueChange = { lookupQuery = it },
                            placeholder = { Text("उदा. +91 1409823451 या 9876543210", color = SnehaTextSecondary, fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("spam_number_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SnehaCyan,
                                unfocusedBorderColor = SnehaDarkSurfaceVariant,
                                focusedTextColor = SnehaTextPrimary,
                                unfocusedTextColor = SnehaTextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (lookupQuery.isNotBlank()) {
                                    lookupResult = SpamCallerManager.analyzeCaller(lookupQuery)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SnehaCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("spam_check_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Scan", tint = Color.Black)
                        }
                    }

                    // Result Display
                    lookupResult?.let { result ->
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (result.spamScorePercentage >= 70) Color(0xFF3E1A24) else Color(0xFF142E25)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = result.callerName,
                                        color = SnehaTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "${result.spamScorePercentage}% स्पैम रिस्क",
                                        color = if (result.spamScorePercentage >= 70) Color(0xFFFF5252) else Color(0xFF00E676),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = result.category.hindiLabel,
                                    color = Color(result.category.badgeColorHex),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "समुदाय रिपोर्ट्स: ${result.reportsCount}+ बार रिपोर्ट किया गया",
                                    color = SnehaTextSecondary,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            SpamCallerManager.addNumberToBlacklist(result.phoneNumber)
                                            lookupResult = null
                                            lookupQuery = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Block, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ब्लॉक करें", fontSize = 12.sp, color = Color.White)
                                    }

                                    Button(
                                        onClick = {
                                            onTestCall("⚠️ स्पैम: ${result.callerName} (${result.phoneNumber})")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SnehaDarkSurfaceVariant),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = SnehaCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("कॉल टेस्ट", fontSize = 12.sp, color = SnehaCyan)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Blocked / Analyzed Log Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🛡️ हाल ही में रोके गए / पहचाने गए कॉल",
                    color = SnehaTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "${blockedLogs.size} लॉग",
                    color = SnehaTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // List of Logs
        items(blockedLogs) { log ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                if (log.wasBlocked) Color(0xFFFF1744).copy(alpha = 0.2f) else Color(0xFF00E676).copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (log.wasBlocked) Icons.Default.Block else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (log.wasBlocked) Color(0xFFFF1744) else Color(0xFF00E676),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = log.callerName,
                                color = SnehaTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            if (log.wasBlocked) {
                                Text(
                                    text = "ऑटो-ब्लॉक किया गया",
                                    color = Color(0xFFFF5252),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = log.phoneNumber,
                            color = SnehaTextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${log.category.hindiLabel} • ${log.spamScorePercentage}% रिस्क",
                            color = Color(log.category.badgeColorHex),
                            fontSize = 11.sp
                        )
                    }
                    IconButton(
                        onClick = { SpamCallerManager.removeNumberFromBlacklist(log.phoneNumber) }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SnehaTextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
