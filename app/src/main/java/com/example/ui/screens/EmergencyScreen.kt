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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.EmergencyContactEntity
import com.example.ui.theme.SnehaAmber
import com.example.ui.theme.SnehaCyan
import com.example.ui.theme.SnehaDarkSurface
import com.example.ui.theme.SnehaDarkSurfaceVariant
import com.example.ui.theme.SnehaEmerald
import com.example.ui.theme.SnehaPink
import com.example.ui.theme.SnehaTextPrimary
import com.example.ui.theme.SnehaTextSecondary

@Composable
fun EmergencyScreen(
    contacts: List<EmergencyContactEntity>,
    isAlarmPlaying: Boolean,
    isStrobePlaying: Boolean,
    onRequestEmergencyUnlock: () -> Unit,
    onToggleAlarm: () -> Unit,
    onToggleStrobe: () -> Unit,
    onCallNumber: (String) -> Unit,
    onAddContact: (name: String, number: String, relation: String) -> Unit,
    onDeleteContact: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newNumber by remember { mutableStateOf("") }
    var newRelation by remember { mutableStateOf("") }

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
                text = "इमरजेंसी एवं लॉकस्क्रीन कंट्रोल 🚨",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SnehaTextPrimary
            )
            Text(
                text = "लॉक फोन अनलॉक करने का अनुरोध, तेज SOS सायरन एवं आपातकालीन संपर्क",
                fontSize = 13.sp,
                color = SnehaTextSecondary
            )
        }

        // Lockscreen Unlock Trigger Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1430)),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, Color(0xFFA855F7))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color(0xFFA855F7).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "इमरजेंसी में लॉक खोलें",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SnehaTextPrimary
                            )
                            Text(
                                text = "Keyguard Dismiss & Screen Wakeup",
                                fontSize = 11.sp,
                                color = Color(0xFFE9D5FF)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "स्नेहा फोन लॉक होने पर भी स्क्रीन पर काम करती है। नीचे दिए बटन से या 'स्नेहा, लॉक खोलो' बोलकर आप लॉकस्क्रीन हटाने का त्वरित अनुरोध कर सकते हैं।",
                        fontSize = 12.sp,
                        color = Color(0xFFD8B4FE),
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onRequestEmergencyUnlock,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_request_emergency_unlock")
                    ) {
                        Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "🔓 लॉकस्क्रीन हटाएं (Unlock Request)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // SOS Siren & Flash Strobe Controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Siren Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAlarmPlaying) Color(0xFF4C0519) else SnehaDarkSurface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isAlarmPlaying) SnehaPink else Color(0xFF2C3252))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isAlarmPlaying) Icons.Default.Stop else Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = SnehaPink,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isAlarmPlaying) "सायरन चालू है!" else "SOS सायरन",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnehaTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onToggleAlarm,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAlarmPlaying) Color(0xFFE11D48) else SnehaPink.copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("btn_toggle_sos_siren")
                        ) {
                            Text(
                                text = if (isAlarmPlaying) "बंद करें ⏹" else "अलार्म बजाएं 🚨",
                                fontSize = 11.sp,
                                color = if (isAlarmPlaying) Color.White else SnehaPink,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Strobe Flash Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isStrobePlaying) Color(0xFF451A03) else SnehaDarkSurface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isStrobePlaying) SnehaAmber else Color(0xFF2C3252))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = SnehaAmber,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isStrobePlaying) "स्ट्रोब चालू है!" else "SOS स्ट्रोब लाइट",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnehaTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onToggleStrobe,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isStrobePlaying) Color(0xFFD97706) else SnehaAmber.copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("btn_toggle_strobe")
                        ) {
                            Text(
                                text = if (isStrobePlaying) "लाइट बंद ⏹" else "फ्लैश चमकाएं ⚡",
                                fontSize = 11.sp,
                                color = if (isStrobePlaying) Color.White else SnehaAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Direct National SOS Hotline
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1515)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFDC2626))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFDC2626).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneInTalk,
                                contentDescription = null,
                                tint = Color(0xFFEF4444)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "राष्ट्रीय इमरजेंसी हेल्पलाइन 112",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "पुलिस / एम्बुलेंस / फायर ब्रिगेड",
                                fontSize = 11.sp,
                                color = Color(0xFFFCA5A5)
                            )
                        }
                    }

                    Button(
                        onClick = { onCallNumber("112") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_call_112")
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("112 कॉल", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Emergency Contacts List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "आपातकालीन संपर्क सूची (${contacts.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SnehaCyan
                )

                Button(
                    onClick = { showAddDialog = !showAddDialog },
                    colors = ButtonDefaults.buttonColors(containerColor = SnehaDarkSurfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("btn_add_emergency_contact")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = SnehaCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("नंबर जोड़ें", fontSize = 11.sp, color = SnehaCyan)
                }
            }
        }

        if (showAddDialog) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SnehaCyan)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("नया इमरजेंसी संपर्क जोड़ें:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SnehaCyan)

                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("नाम (जैसे माँ, पापा, भाई)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SnehaTextPrimary,
                                unfocusedTextColor = SnehaTextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = newNumber,
                            onValueChange = { newNumber = it },
                            label = { Text("फोन नंबर") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SnehaTextPrimary,
                                unfocusedTextColor = SnehaTextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = newRelation,
                            onValueChange = { newRelation = it },
                            label = { Text("संबंध (रिलेशन)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SnehaTextPrimary,
                                unfocusedTextColor = SnehaTextPrimary
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    if (newName.isNotBlank() && newNumber.isNotBlank()) {
                                        onAddContact(newName, newNumber, newRelation)
                                        newName = ""
                                        newNumber = ""
                                        newRelation = ""
                                        showAddDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SnehaCyan)
                            ) {
                                Text("सेव करें", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        items(contacts) { contact ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF2C3252))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contact.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnehaTextPrimary
                        )
                        Text(
                            text = "${contact.relationship} • ${contact.phoneNumber}",
                            fontSize = 12.sp,
                            color = SnehaTextSecondary
                        )
                    }

                    Row {
                        IconButton(
                            onClick = { onCallNumber(contact.phoneNumber) },
                            modifier = Modifier.testTag("call_contact_${contact.id}")
                        ) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = "कॉल करें", tint = SnehaEmerald)
                        }

                        if (!contact.isPrimary) {
                            IconButton(onClick = { onDeleteContact(contact.id) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "हटाएं", tint = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            }
        }

        // Lockscreen & Background info card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF242A44))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = SnehaCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "स्नेहा का बैकग्राउंड सर्विस नोटिफिकेशन चालू रखें ताकि जब भी फोन लॉक हो या स्क्रीन ऑफ हो, स्नेहा वॉयस और नोटिफिकेशन से तुरंत एक्टिवेट हो सके।",
                        fontSize = 12.sp,
                        color = SnehaTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
