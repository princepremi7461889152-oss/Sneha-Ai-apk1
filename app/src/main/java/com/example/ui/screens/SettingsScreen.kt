package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VoicePersona
import com.example.data.model.VoicePersonaId
import com.example.ui.theme.SnehaAmber
import com.example.ui.theme.SnehaCardBorder
import com.example.ui.theme.SnehaCyan
import com.example.ui.theme.SnehaDarkBg
import com.example.ui.theme.SnehaDarkSurface
import com.example.ui.theme.SnehaDarkSurfaceVariant
import com.example.ui.theme.SnehaEmerald
import com.example.ui.theme.SnehaPink
import com.example.ui.theme.SnehaPurple
import com.example.ui.theme.SnehaTextPrimary
import com.example.ui.theme.SnehaTextSecondary
import com.example.ui.theme.SnehaTextTertiary
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    isServiceRunning: Boolean,
    isVoiceOutputEnabled: Boolean,
    isAutoReadEnabled: Boolean,
    speechRate: Float,
    speechPitch: Float,
    onToggleService: (Boolean) -> Unit,
    onToggleVoiceOutput: (Boolean) -> Unit,
    onToggleAutoRead: (Boolean) -> Unit,
    onRateChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onTestVoice: () -> Unit,
    currentPersona: VoicePersona = VoicePersona.ALL[0],
    onSelectPersona: (VoicePersona) -> Unit = {},
    onResetToPersonaDefaults: () -> Unit = {},
    onTestCustomPhrase: (String) -> Unit = { onTestVoice() },
    isSpeaking: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var customTestSentence by remember { mutableStateOf("नमस्ते! मैं स्नेहा हूँ। मैं आपके सारे काम चुटकियों में कर सकती हूँ!") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Dedicated Settings Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "स्नेहा सेटिंग्स ⚙️",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SnehaTextPrimary
                    )
                    Text(
                        text = "वॉयस पर्सोना, गति, सुर एवं सिस्टम सेटिंग्स",
                        fontSize = 12.sp,
                        color = SnehaTextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SnehaDarkSurfaceVariant,
                    border = BorderStroke(1.dp, SnehaCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentPersona.iconEmoji,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentPersona.nameHindi,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SnehaCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dedicated Navigation Tabs: Voice Persona & Tuning vs System & Security
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SnehaDarkSurface,
                contentColor = SnehaCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = SnehaCyan,
                        height = 3.dp
                    )
                },
                divider = {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SnehaCardBorder))
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedTab == 0) SnehaCyan else SnehaTextSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "वॉयस पर्सोना & ट्यूनिंग",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) SnehaCyan else SnehaTextSecondary
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_voice_settings")
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedTab == 1) SnehaPurple else SnehaTextSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "सिस्टम & बैकग्राउंड",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) SnehaPurple else SnehaTextSecondary
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_system_settings")
                )
            }
        }

        // Tab Content
        if (selectedTab == 0) {
            VoicePersonaSettingsTab(
                currentPersona = currentPersona,
                speechRate = speechRate,
                speechPitch = speechPitch,
                isVoiceOutputEnabled = isVoiceOutputEnabled,
                isSpeaking = isSpeaking,
                customTestSentence = customTestSentence,
                onCustomSentenceChange = { customTestSentence = it },
                onSelectPersona = onSelectPersona,
                onRateChange = onRateChange,
                onPitchChange = onPitchChange,
                onToggleVoiceOutput = onToggleVoiceOutput,
                onTestVoice = onTestVoice,
                onTestCustomPhrase = onTestCustomPhrase,
                onResetToPersonaDefaults = onResetToPersonaDefaults
            )
        } else {
            SystemSecuritySettingsTab(
                isServiceRunning = isServiceRunning,
                isAutoReadEnabled = isAutoReadEnabled,
                onToggleService = onToggleService,
                onToggleAutoRead = onToggleAutoRead
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoicePersonaSettingsTab(
    currentPersona: VoicePersona,
    speechRate: Float,
    speechPitch: Float,
    isVoiceOutputEnabled: Boolean,
    isSpeaking: Boolean,
    customTestSentence: String,
    onCustomSentenceChange: (String) -> Unit,
    onSelectPersona: (VoicePersona) -> Unit,
    onRateChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onToggleVoiceOutput: (Boolean) -> Unit,
    onTestVoice: () -> Unit,
    onTestCustomPhrase: (String) -> Unit,
    onResetToPersonaDefaults: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var customWakeWord by remember { mutableStateOf(com.example.data.local.VoicePreferences.getCustomWakeWord(context)) }
    var wakeWordInput by remember { mutableStateOf("") }

    val wakeWordPresets = listOf(
        "स्नेहा",
        "जार्विस",
        "प्रिंस",
        "सिरी",
        "रूबी",
        "दोस्त",
        "मास्टर"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Custom Wake Word Setup Card (अपनी मर्जी का नाम)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_custom_wake_word"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                border = BorderStroke(1.5.dp, SnehaCyan)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(SnehaCyan.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = SnehaCyan)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "कस्टम वेक-वर्ड (अपनी मर्जी का नाम) ✨",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SnehaTextPrimary
                                )
                                Text(
                                    text = "वर्तमान नाम: \"$customWakeWord\"",
                                    fontSize = 12.sp,
                                    color = SnehaCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "आप स्नेहा को अपनी पसंद के किसी भी नाम से पुकार सकते हैं। नीचे से तुरंत चुनें या नया नाम टाइप करें:",
                        fontSize = 12.sp,
                        color = SnehaTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset Chips
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        wakeWordPresets.forEach { preset ->
                            androidx.compose.material3.FilterChip(
                                selected = customWakeWord.equals(preset, ignoreCase = true),
                                onClick = {
                                    customWakeWord = preset
                                    com.example.data.local.VoicePreferences.saveCustomWakeWord(context, preset)
                                    onTestCustomPhrase("जी मास्टर! अब मैं '$preset' पुकारने पर हाजिर होऊंगी!")
                                },
                                label = { Text(preset, fontSize = 12.sp) },
                                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SnehaCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = SnehaCyan,
                                    labelColor = SnehaTextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Custom Name Input Field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = wakeWordInput,
                            onValueChange = { wakeWordInput = it },
                            placeholder = { Text("उदा. सिकंदर, अलेक्सा, बॉस...", color = SnehaTextSecondary, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_wake_word_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SnehaCyan,
                                unfocusedBorderColor = SnehaDarkSurfaceVariant,
                                focusedTextColor = SnehaTextPrimary,
                                unfocusedTextColor = SnehaTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (wakeWordInput.isNotBlank()) {
                                    val newName = wakeWordInput.trim()
                                    customWakeWord = newName
                                    com.example.data.local.VoicePreferences.saveCustomWakeWord(context, newName)
                                    onTestCustomPhrase("जी मास्टर! अब मैं '$newName' पुकारने पर हाजिर होऊंगी!")
                                    wakeWordInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SnehaCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("save_wake_word_button")
                        ) {
                            Text("सेव", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Voice Input Error 5 & 11 Explainer & Auto-Healer Guide
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_voice_error_guide"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13222E)),
                border = BorderStroke(1.dp, SnehaCyan.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = SnehaCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "वॉयस इनपुट एरर 5 और 11 क्या हैं?",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnehaCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• एरर 5 (Client Error): यह तब होता है जब एक कमांड खत्म होने से पहले दूसरा शुरू हो या बैकग्राउंड सर्विस रीसेट हो रही हो।",
                        fontSize = 11.sp,
                        color = SnehaTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• एरर 11 (Server Disconnected): यह तब आता है जब गूगल स्पीच सर्विसेज इंजन बैकग्राउंड में कनेक्शन री-एस्टैब्लिश कर रहा हो।",
                        fontSize = 11.sp,
                        color = SnehaTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "स्नेहा में ऑटो-साइलेंट हीलिंग सक्रिय है: अब ये एरर्स स्क्रीन पर एरर पॉपअप नहीं देंगे और खुद 1 सेकंड में बैकग्राउंड में रिकवर हो जाएंगे।",
                            fontSize = 11.sp,
                            color = Color(0xFF00E676),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Active Persona Hero Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_active_persona_banner"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(SnehaCyan, SnehaPurple)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        brush = Brush.linearGradient(listOf(SnehaCyan.copy(alpha = 0.3f), SnehaPurple.copy(alpha = 0.3f))),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentPersona.iconEmoji,
                                    fontSize = 22.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currentPersona.nameHindi,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SnehaTextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = SnehaCyan.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "सक्रिय पर्सोना",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SnehaCyan,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${currentPersona.nameEnglish} • ${currentPersona.tagHindi}",
                                    fontSize = 12.sp,
                                    color = SnehaTextSecondary
                                )
                            }
                        }

                        // Master Voice Output Switch
                        Switch(
                            checked = isVoiceOutputEnabled,
                            onCheckedChange = onToggleVoiceOutput,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SnehaCyan,
                                checkedTrackColor = SnehaCyan.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.testTag("switch_voice_output")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = currentPersona.descriptionHindi,
                        fontSize = 12.sp,
                        color = SnehaTextSecondary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Current tuning stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = SnehaDarkSurfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = SnehaCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("गति (Speed)", fontSize = 10.sp, color = SnehaTextTertiary)
                                    Text("${String.format("%.2f", speechRate)}x", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SnehaTextPrimary)
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = SnehaDarkSurfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = SnehaPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("सुर (Pitch)", fontSize = 10.sp, color = SnehaTextTertiary)
                                    Text(String.format("%.2f", speechPitch), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SnehaTextPrimary)
                                }
                            }
                        }

                        Button(
                            onClick = onTestVoice,
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSpeaking) SnehaPink else SnehaCyan),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_hero_test_voice")
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSpeaking) "रुकें" else "सुनें",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }

        // Section 1: Choose Sneha's Voice Persona
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "1. स्नेहा का वॉयस पर्सोना चुनें 🎭",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SnehaTextPrimary
                    )
                    Text(
                        text = "व्यक्तित्व के अनुसार आवाज़ का स्वभाव और सुर बदलें",
                        fontSize = 11.sp,
                        color = SnehaTextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SnehaDarkSurfaceVariant
                ) {
                    Text(
                        text = "${VoicePersona.ALL.size} विकल्प",
                        fontSize = 10.sp,
                        color = SnehaCyan,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // 5 Dedicated Persona Cards
        items(VoicePersona.ALL) { persona ->
            val isSelected = persona.id == currentPersona.id
            val borderColor by animateColorAsState(
                if (isSelected) SnehaCyan else SnehaCardBorder,
                label = "persona_border"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectPersona(persona) }
                    .testTag("card_persona_${persona.id.id}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) SnehaDarkSurfaceVariant else SnehaDarkSurface
                ),
                border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        if (isSelected) SnehaCyan.copy(alpha = 0.2f) else Color(0xFF22283A),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = persona.iconEmoji, fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = persona.nameHindi,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) SnehaCyan else SnehaTextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${persona.nameEnglish})",
                                        fontSize = 11.sp,
                                        color = SnehaTextSecondary
                                    )
                                }
                                Text(
                                    text = persona.tagHindi,
                                    fontSize = 11.sp,
                                    color = if (isSelected) SnehaTextPrimary else SnehaTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (isSelected) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SnehaCyan.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, SnehaCyan)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = SnehaCyan,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("सक्रिय", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SnehaCyan)
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onSelectPersona(persona) },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, SnehaCardBorder),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                            Text("चुनें", fontSize = 11.sp, color = SnehaTextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = persona.descriptionHindi,
                    fontSize = 11.sp,
                    color = SnehaTextSecondary,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Persona characteristics & Direct audition button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1E2235)
                        ) {
                            Text(
                                text = "डिफ़ॉल्ट गति: ${persona.defaultSpeed}x",
                                fontSize = 10.sp,
                                color = SnehaCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1E2235)
                        ) {
                            Text(
                                text = "डिफ़ॉल्ट सुर: ${persona.defaultPitch}",
                                fontSize = 10.sp,
                                color = SnehaPurple,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { onTestCustomPhrase(persona.sampleSpeech) },
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, SnehaCyan.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("btn_sample_speech_${persona.id.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = SnehaCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("नमूना सुनें", fontSize = 10.sp, color = SnehaCyan)
                    }
                }
            }
        }
    }

    // Section 2: Fine-tune Speech Speed
    item {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_speed_tuning"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            border = BorderStroke(1.dp, SnehaCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(SnehaCyan.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = SnehaCyan, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "2. बोलने की गति (Speech Speed)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SnehaTextPrimary
                            )
                            Text(
                                text = "धीमी स्पष्टता से लेकर तेज़ उत्पादकता तक",
                                fontSize = 11.sp,
                                color = SnehaTextSecondary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SnehaCyan.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, SnehaCyan)
                    ) {
                        Text(
                            text = "${String.format("%.2f", speechRate)}x",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnehaCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stepper + Slider Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newRate = ((speechRate - 0.05f) * 100).roundToInt() / 100f
                            onRateChange(newRate.coerceIn(0.5f, 2.0f))
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = SnehaDarkSurfaceVariant),
                        modifier = Modifier.size(34.dp).testTag("btn_rate_minus")
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease Speed", tint = SnehaCyan, modifier = Modifier.size(16.dp))
                    }

                    Slider(
                        value = speechRate,
                        onValueChange = { onRateChange(((it * 100).roundToInt() / 100f).coerceIn(0.5f, 2.0f)) },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = SnehaCyan,
                            activeTrackColor = SnehaCyan,
                            inactiveTrackColor = SnehaDarkSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                            .testTag("slider_speech_rate")
                    )

                    IconButton(
                        onClick = {
                            val newRate = ((speechRate + 0.05f) * 100).roundToInt() / 100f
                            onRateChange(newRate.coerceIn(0.5f, 2.0f))
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = SnehaDarkSurfaceVariant),
                        modifier = Modifier.size(34.dp).testTag("btn_rate_plus")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase Speed", tint = SnehaCyan, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Speed Preset Chips
                Text("त्वरित गति प्रीसेट:", fontSize = 11.sp, color = SnehaTextTertiary)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val speedPresets = listOf(
                        0.80f to "0.8x धीमी",
                        1.00f to "1.0x सामान्य",
                        1.18f to "1.18x तेज़",
                        1.40f to "1.4x बहुत तेज़"
                    )
                    speedPresets.forEach { (presetRate, label) ->
                        val isPresetActive = kotlin.math.abs(speechRate - presetRate) < 0.03f
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPresetActive) SnehaCyan.copy(alpha = 0.25f) else SnehaDarkSurfaceVariant,
                            border = BorderStroke(1.dp, if (isPresetActive) SnehaCyan else SnehaCardBorder),
                            modifier = Modifier.clickable { onRateChange(presetRate) }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = if (isPresetActive) SnehaCyan else SnehaTextSecondary,
                                fontWeight = if (isPresetActive) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Section 3: Fine-tune Voice Pitch / Tone
    item {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_pitch_tuning"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            border = BorderStroke(1.dp, SnehaCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(SnehaPurple.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, tint = SnehaPurple, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "3. आवाज का सुर (Voice Pitch)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SnehaTextPrimary
                            )
                            Text(
                                text = "गंभीर बेस टोन से मधुर चुलबुली पिच तक",
                                fontSize = 11.sp,
                                color = SnehaTextSecondary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SnehaPurple.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, SnehaPurple)
                    ) {
                        Text(
                            text = String.format("%.2f", speechPitch),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnehaPurple,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Pitch Stepper + Slider Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newPitch = ((speechPitch - 0.05f) * 100).roundToInt() / 100f
                            onPitchChange(newPitch.coerceIn(0.60f, 1.50f))
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = SnehaDarkSurfaceVariant),
                        modifier = Modifier.size(34.dp).testTag("btn_pitch_minus")
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease Pitch", tint = SnehaPurple, modifier = Modifier.size(16.dp))
                    }

                    Slider(
                        value = speechPitch,
                        onValueChange = { onPitchChange(((it * 100).roundToInt() / 100f).coerceIn(0.60f, 1.50f)) },
                        valueRange = 0.60f..1.50f,
                        colors = SliderDefaults.colors(
                            thumbColor = SnehaPurple,
                            activeTrackColor = SnehaPurple,
                            inactiveTrackColor = SnehaDarkSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                            .testTag("slider_speech_pitch")
                    )

                    IconButton(
                        onClick = {
                            val newPitch = ((speechPitch + 0.05f) * 100).roundToInt() / 100f
                            onPitchChange(newPitch.coerceIn(0.60f, 1.50f))
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = SnehaDarkSurfaceVariant),
                        modifier = Modifier.size(34.dp).testTag("btn_pitch_plus")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase Pitch", tint = SnehaPurple, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("गंभीर (Bass)", fontSize = 10.sp, color = SnehaTextTertiary)
                    Text("स्वाभाविक (Normal)", fontSize = 10.sp, color = SnehaTextTertiary)
                    Text("मधुर / तीखी (Treble)", fontSize = 10.sp, color = SnehaTextTertiary)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Pitch Preset Chips
                Text("सुर के त्वरित प्रीसेट:", fontSize = 11.sp, color = SnehaTextTertiary)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val pitchPresets = listOf(
                        0.76f to "गंभीर बेस (0.76)",
                        0.95f to "प्रो क्रिस्प (0.95)",
                        1.05f to "स्वाभाविक (1.05)",
                        1.25f to "मधुर चुलबुली (1.25)"
                    )
                    pitchPresets.forEach { (presetPitch, label) ->
                        val isPresetActive = kotlin.math.abs(speechPitch - presetPitch) < 0.03f
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPresetActive) SnehaPurple.copy(alpha = 0.25f) else SnehaDarkSurfaceVariant,
                            border = BorderStroke(1.dp, if (isPresetActive) SnehaPurple else SnehaCardBorder),
                            modifier = Modifier.clickable { onPitchChange(presetPitch) }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = if (isPresetActive) SnehaPurple else SnehaTextSecondary,
                                fontWeight = if (isPresetActive) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Section 4: Dedicated Voice Preview & Testing Studio
    item {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_voice_preview_studio"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            border = BorderStroke(1.dp, SnehaCyan.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(SnehaCyan.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = SnehaCyan, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "4. वॉयस टेस्ट स्टूडियो 🎧",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SnehaTextPrimary
                            )
                            Text(
                                text = "नई सेटिंग्स को तुरंत बोलकर परखें",
                                fontSize = 11.sp,
                                color = SnehaTextSecondary
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onResetToPersonaDefaults,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SnehaCardBorder),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("btn_reset_persona_defaults")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", tint = SnehaTextSecondary, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("डिफ़ॉल्ट", fontSize = 10.sp, color = SnehaTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom phrase input
                OutlinedTextField(
                    value = customTestSentence,
                    onValueChange = onCustomSentenceChange,
                    modifier = Modifier.fillMaxWidth().testTag("input_test_phrase"),
                    label = { Text("टेस्ट वाक्य", fontSize = 11.sp, color = SnehaCyan) },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SnehaTextPrimary,
                        unfocusedTextColor = SnehaTextPrimary,
                        focusedBorderColor = SnehaCyan,
                        unfocusedBorderColor = SnehaCardBorder,
                        focusedContainerColor = SnehaDarkSurfaceVariant,
                        unfocusedContainerColor = SnehaDarkSurfaceVariant
                    ),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Suggested test phrases
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val samplePhrases = listOf(
                        "नमस्ते! मैं स्नेहा हूँ।",
                        "आज का मौसम 28 डिग्री है।",
                        "क्या मैं आपके मैसेज पढ़ूँ?"
                    )
                    samplePhrases.forEach { phrase ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SnehaDarkSurfaceVariant,
                            modifier = Modifier.clickable {
                                onCustomSentenceChange(phrase)
                                onTestCustomPhrase(phrase)
                            }
                        ) {
                            Text(
                                text = phrase,
                                fontSize = 10.sp,
                                color = SnehaTextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Play / Speak current settings button
                Button(
                    onClick = { onTestCustomPhrase(customTestSentence) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSpeaking) SnehaPink else SnehaCyan
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_test_sneha_voice")
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSpeaking) "आवाज बंद करें ⏹️" else "स्नेहा की नई आवाज टेस्ट करें 🔊",
                        fontSize = 13.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
}

@Composable
private fun SystemSecuritySettingsTab(
    isServiceRunning: Boolean,
    isAutoReadEnabled: Boolean,
    onToggleService: (Boolean) -> Unit,
    onToggleAutoRead: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Background Foreground Service Toggle Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isServiceRunning) SnehaEmerald else Color(0xFF2C3252))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isServiceRunning) SnehaEmerald.copy(alpha = 0.2f) else Color(0xFF334155),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (isServiceRunning) SnehaEmerald else SnehaTextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "बैकग्राउंड वॉयस सर्विस",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SnehaTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isServiceRunning) SnehaEmerald.copy(alpha = 0.2f) else Color(0xFF334155)
                                ) {
                                    Text(
                                        text = if (isServiceRunning) "सक्रिय" else "बंद",
                                        fontSize = 10.sp,
                                        color = if (isServiceRunning) SnehaEmerald else SnehaTextSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "फोन लॉक होने पर भी स्नेहा नोटिफिकेशन व त्वरित माइक से काम करेगी",
                                fontSize = 11.sp,
                                color = SnehaTextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = onToggleService,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SnehaEmerald,
                            checkedTrackColor = SnehaEmerald.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("switch_background_service")
                    )
                }
            }
        }

        // Auto-Read Messages Toggle Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isAutoReadEnabled) SnehaCyan else Color(0xFF2C3252))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isAutoReadEnabled) SnehaCyan.copy(alpha = 0.2f) else Color(0xFF334155),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = if (isAutoReadEnabled) SnehaCyan else SnehaTextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "संदेश स्वतः बोलकर सुनाएं (Auto-Read)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SnehaTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "SMS व WhatsApp संदेश आते ही बोलकर पढ़ें (OTP हमेशा सुरक्षित रूप से छोड़ दिया जाएगा)",
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
                        modifier = Modifier.testTag("switch_settings_auto_read")
                    )
                }
            }
        }

        // Lockscreen & Emergency Mode Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF2C3252))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, tint = SnehaPurple)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "लॉकस्क्रीन & इमरजेंसी अनलॉक क्षमताएं",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnehaTextPrimary
                        )
                    }
                    Text(
                        text = "1. लॉक फोन पर कार्य: जब फोन लॉक होता है, स्नेहा की स्क्रीन ऑटोमैटिकली ऑन हो जाती है।\n" +
                                "2. इमरजेंसी अनलॉक: 'स्नेहा, लॉक खोलो' आदेश से एंड्रॉइड कीगार्ड को हटाने का सिस्टम अनुरोध भेजा जाता है।\n" +
                                "3. SOS सायरन: फोन लॉक होने पर भी लाउड सायरन और स्ट्रोब लाइट बजती है।",
                        fontSize = 12.sp,
                        color = SnehaTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Language & AI Engine Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF2C3252))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = SnehaPurple)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "भाषा प्राथमिकता: हिंदी + English (Bilingual)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnehaTextPrimary
                        )
                    }
                    Text(
                        text = "स्नेहा हिंदी, हिंग्लिश और अंग्रेजी तीनों भाषाओं में सहजता से समझती और उत्तर देती है।",
                        fontSize = 12.sp,
                        color = SnehaTextSecondary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = SnehaCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI इंजन: Google Gemini 3.5 Flash",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnehaTextPrimary
                        )
                    }
                    Text(
                        text = "सामान्य ज्ञान, सवालों और बातचीत के लिए अत्याधुनिक Gemini मॉडल से संचालित।",
                        fontSize = 12.sp,
                        color = SnehaTextSecondary
                    )
                }
            }
        }

        // Security Commitment Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF2C3252))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = SnehaEmerald)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "सुरक्षा एवं गोपनीयता वचन 🛡️",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnehaEmerald
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "स्नेहा आपकी वित्तीय सुरक्षा का पूरा सम्मान करती है। कोई भी OTP या बैंक कोड कभी भी जोर से नहीं बोला जाता और न ही साझा किया जाता है।",
                            fontSize = 12.sp,
                            color = SnehaTextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
