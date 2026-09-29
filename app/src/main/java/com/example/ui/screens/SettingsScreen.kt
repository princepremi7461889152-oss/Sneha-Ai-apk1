package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SecurityUnlockPreferences
import com.example.data.local.UnlockType
import com.example.data.local.VoicePreferences
import com.example.data.model.SnehaScreen
import com.example.data.model.VoicePersona
import com.example.engine.AiCloudConnectorManager
import com.example.engine.AiProvider
import com.example.ui.theme.SnehaCardBorder
import com.example.ui.theme.SnehaCyan
import com.example.ui.theme.SnehaDarkSurface
import com.example.ui.theme.SnehaDarkSurfaceVariant
import com.example.ui.theme.SnehaEmerald
import com.example.ui.theme.SnehaGreen
import com.example.ui.theme.SnehaPink
import com.example.ui.theme.SnehaPurple
import com.example.ui.theme.SnehaTextPrimary
import com.example.ui.theme.SnehaTextSecondary
import kotlinx.coroutines.launch
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
    onNavigateToScreen: (SnehaScreen) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val currentUserName = VoicePreferences.getUserName(context)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
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
                        text = "$currentUserName की व्यक्तिगत सुरक्षा, वॉयस व AI सेटिंग्स",
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

            // 4 Navigation Tabs: Voice & User | Security | Cloud Connector | All Features Directory
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SnehaDarkSurface,
                contentColor = SnehaCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = when (selectedTab) {
                            0 -> SnehaCyan
                            1 -> SnehaPurple
                            2 -> SnehaPink
                            else -> SnehaEmerald
                        },
                        height = 3.dp
                    )
                },
                divider = {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SnehaCardBorder))
                }
            ) {
                // Tab 0: Voice & User
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "वॉयस & यूज़र",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) SnehaCyan else SnehaTextSecondary
                        )
                    },
                    modifier = Modifier.testTag("tab_voice_user")
                )

                // Tab 1: Security (Password, PIN, Pattern)
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "सुरक्षा & पिन",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) SnehaPurple else SnehaTextSecondary
                        )
                    },
                    modifier = Modifier.testTag("tab_security_pin")
                )

                // Tab 2: AI Cloud Connector
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            text = "क्लाउड AI",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 2) SnehaPink else SnehaTextSecondary
                        )
                    },
                    modifier = Modifier.testTag("tab_cloud_ai")
                )

                // Tab 3: All Features Directory
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Text(
                            text = "सभी फीचर्स",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 3) SnehaEmerald else SnehaTextSecondary
                        )
                    },
                    modifier = Modifier.testTag("tab_all_features")
                )
            }
        }

        // Tab Content
        when (selectedTab) {
            0 -> {
                VoiceAndUserTab(
                    currentPersona = currentPersona,
                    speechRate = speechRate,
                    speechPitch = speechPitch,
                    isVoiceOutputEnabled = isVoiceOutputEnabled,
                    isSpeaking = isSpeaking,
                    onSelectPersona = onSelectPersona,
                    onRateChange = onRateChange,
                    onPitchChange = onPitchChange,
                    onToggleVoiceOutput = onToggleVoiceOutput,
                    onTestVoice = onTestVoice,
                    onTestCustomPhrase = onTestCustomPhrase,
                    onResetToPersonaDefaults = onResetToPersonaDefaults
                )
            }
            1 -> {
                SecurityPinPasswordTab(
                    onSpeak = onTestCustomPhrase,
                    onOpenUnlockScreen = { onNavigateToScreen(SnehaScreen.SECURITY_UNLOCK) }
                )
            }
            2 -> {
                CloudConnectorTab(
                    onOpenFullConnector = { onNavigateToScreen(SnehaScreen.AI_CONNECTOR) },
                    onSpeak = onTestCustomPhrase
                )
            }
            3 -> {
                AllFeaturesDirectoryTab(
                    isServiceRunning = isServiceRunning,
                    isAutoReadEnabled = isAutoReadEnabled,
                    onToggleService = onToggleService,
                    onToggleAutoRead = onToggleAutoRead,
                    onNavigate = onNavigateToScreen
                )
            }
        }
    }
}

/**
 * TAB 0: Voice Gender Selection (Strict Female Voice), User Nickname Changer (Master, Prince, Baby, etc.),
 * Persona Selection, Speed & Pitch Sliders, and Voice Test Studio.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoiceAndUserTab(
    currentPersona: VoicePersona,
    speechRate: Float,
    speechPitch: Float,
    isVoiceOutputEnabled: Boolean,
    isSpeaking: Boolean,
    onSelectPersona: (VoicePersona) -> Unit,
    onRateChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onToggleVoiceOutput: (Boolean) -> Unit,
    onTestVoice: () -> Unit,
    onTestCustomPhrase: (String) -> Unit,
    onResetToPersonaDefaults: () -> Unit
) {
    val context = LocalContext.current
    var customUserName by remember { mutableStateOf(VoicePreferences.getUserName(context)) }
    var userNameInput by remember { mutableStateOf("") }
    var activeGender by remember { mutableStateOf(VoicePreferences.getVoiceGender(context)) }

    var customWakeWord by remember { mutableStateOf(VoicePreferences.getCustomWakeWord(context)) }
    var wakeWordInput by remember { mutableStateOf("") }
    var activeMicChime by remember { mutableStateOf(VoicePreferences.getMicChimeType(context)) }
    var activeTheme by remember { mutableStateOf(com.example.ui.theme.SnehaAppTheme.getSavedTheme(context)) }
    var isBubbleRunning by remember { mutableStateOf(com.example.service.SnehaFloatingBubbleService.isRunning) }
    var sosName by remember { mutableStateOf(com.example.engine.EmergencySosManager.getEmergencyContactName(context)) }
    var sosPhone by remember { mutableStateOf(com.example.engine.EmergencySosManager.getEmergencyContactPhone(context)) }
    var notesList by remember { mutableStateOf(com.example.engine.VoiceNotesManager.getAllNotes(context)) }
    var newNoteInput by remember { mutableStateOf("") }

    var customTestSentence by remember { mutableStateOf("नमस्ते $customUserName! मैं स्नेहा हूँ, आपकी महिला AI असिस्टेंट।") }

    val userNamePresets = listOf(
        "मास्टर",
        "प्रिंस",
        "बेबी",
        "बॉस",
        "सर",
        "स्वीटू",
        "दोस्त",
        "भैया"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Voice Gender Selection (FEMALE VOICE ENFORCEMENT)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_voice_gender"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                border = BorderStroke(1.5.dp, if (activeGender == "FEMALE") SnehaCyan else SnehaCardBorder)
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
                                    .size(40.dp)
                                    .background(SnehaCyan.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👧", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "आवाज़ का लिंग (Voice Gender)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SnehaTextPrimary
                                )
                                Text(
                                    text = if (activeGender == "FEMALE") "सक्रिय: मधुर महिला आवाज़ (स्नेहा) 🌸" else "सक्रिय: पुरुष आवाज़",
                                    fontSize = 12.sp,
                                    color = if (activeGender == "FEMALE") SnehaCyan else SnehaTextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (activeGender == "FEMALE") {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SnehaCyan.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, SnehaCyan)
                            ) {
                                Text(
                                    text = "सक्रिय महिला",
                                    fontSize = 10.sp,
                                    color = SnehaCyan,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "स्नेहा की आवाज़ को महिला (Female) या पुरुष (Male) में तुरंत बदलें:",
                        fontSize = 12.sp,
                        color = SnehaTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Female Voice Button (Recommended)
                        Button(
                            onClick = {
                                activeGender = "FEMALE"
                                VoicePreferences.saveVoiceGender(context, "FEMALE")
                                onPitchChange(1.18f)
                                onTestCustomPhrase("नमस्ते $customUserName! मैं स्नेहा बोल रही हूँ, आपकी मधुर महिला AI असिस्टेंट।")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeGender == "FEMALE") SnehaCyan else SnehaDarkSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_select_female_voice")
                        ) {
                            Text(
                                text = "👧 महिला आवाज़ (स्नेहा)",
                                color = if (activeGender == "FEMALE") Color.Black else SnehaTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        // Male Voice Button
                        Button(
                            onClick = {
                                activeGender = "MALE"
                                VoicePreferences.saveVoiceGender(context, "MALE")
                                onPitchChange(0.90f)
                                onTestCustomPhrase("नमस्कार $customUserName। पुरुष आवाज़ सक्रिय की गई है।")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeGender == "MALE") SnehaPurple else SnehaDarkSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_select_male_voice")
                        ) {
                            Text(
                                text = "👦 पुरुष आवाज़",
                                color = if (activeGender == "MALE") Color.White else SnehaTextSecondary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. User Nickname / Title Setup Card ("स्नेहा आपको क्या कहकर पुकारे")
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_custom_user_name"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                border = BorderStroke(1.5.dp, SnehaPurple)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(SnehaPurple.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = SnehaPurple)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "स्नेहा आपको क्या कहकर पुकारे? 👑",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SnehaTextPrimary
                            )
                            Text(
                                text = "वर्तमान नाम: \"$customUserName\"",
                                fontSize = 12.sp,
                                color = SnehaPurple,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "स्नेहा आपको क्या कहकर बुलाए (जैसे मास्टर, प्रिंस, बेबी, बॉस आदि)? नीचे से चुनें या अपना नाम लिखें:",
                        fontSize = 12.sp,
                        color = SnehaTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset Chips: Master, Prince, Baby, Boss, Sir, Sweetu, Dost, Bhaiya
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        userNamePresets.forEach { preset ->
                            FilterChip(
                                selected = customUserName.equals(preset, ignoreCase = true),
                                onClick = {
                                    customUserName = preset
                                    VoicePreferences.saveUserName(context, preset)
                                    customTestSentence = "नमस्ते $preset! मैं आपकी AI असिस्टेंट स्नेहा बोल रही हूँ।"
                                    onTestCustomPhrase("जी $preset! अब से मैं आपको $preset कहकर ही पुकारूँगी!")
                                },
                                label = { Text(preset, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SnehaPurple.copy(alpha = 0.25f),
                                    selectedLabelColor = SnehaPurple,
                                    labelColor = SnehaTextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Custom User Name Input Field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = userNameInput,
                            onValueChange = { userNameInput = it },
                            placeholder = { Text("उदा. मास्टर, प्रिंस, बेबी, अमन...", color = SnehaTextSecondary, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_user_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SnehaPurple,
                                unfocusedBorderColor = SnehaDarkSurfaceVariant,
                                focusedTextColor = SnehaTextPrimary,
                                unfocusedTextColor = SnehaTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (userNameInput.isNotBlank()) {
                                    val newTitle = userNameInput.trim()
                                    customUserName = newTitle
                                    VoicePreferences.saveUserName(context, newTitle)
                                    customTestSentence = "नमस्ते $newTitle! मैं आपकी AI असिस्टेंट स्नेहा बोल रही हूँ।"
                                    onTestCustomPhrase("जी $newTitle! अब से मैं आपको $newTitle कहकर ही पुकारूँगी!")
                                    userNameInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SnehaPurple),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("save_user_name_button")
                        ) {
                            Text("सेव", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 3. Custom Wake Word
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                border = BorderStroke(1.dp, SnehaCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "वेक वर्ड (Wake Word) कस्टमाइज़ेशन 🗣️",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SnehaTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "वर्तमान वेक वर्ड: \"$customWakeWord\" (बोलने पर स्नेहा जागती है)",
                        fontSize = 12.sp,
                        color = SnehaCyan
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = wakeWordInput,
                            onValueChange = { wakeWordInput = it },
                            placeholder = { Text("नया वेक वर्ड (उदा. स्नेहा, जार्विस, सिरी)...", color = SnehaTextSecondary, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
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
                                    val word = wakeWordInput.trim()
                                    customWakeWord = word
                                    VoicePreferences.saveCustomWakeWord(context, word)
                                    onTestCustomPhrase("नमस्ते $customUserName! अब आप मुझे '$word' कहकर बुला सकते हैं।")
                                    wakeWordInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SnehaCyan),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("सेव", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 3b. Mic Activation Sound Selection Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_mic_chime_sound"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                border = BorderStroke(1.5.dp, SnehaCyan)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(SnehaCyan.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔔", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "माइक ओपन साउंड (Mic Activation Sound) 🎙️",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SnehaTextPrimary
                            )
                            Text(
                                text = "सक्रिय: ${activeMicChime.displayName} ${activeMicChime.emoji}",
                                fontSize = 12.sp,
                                color = SnehaCyan,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "माइक ऑन होने पर बजने वाला साउंड चुनें। सुनने और सेट करने के लिए किसी भी विकल्प पर टैप करें:",
                        fontSize = 11.sp,
                        color = SnehaTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.engine.MicChimeType.entries.forEach { chime ->
                            val isSelected = activeMicChime == chime
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    activeMicChime = chime
                                    com.example.data.local.VoicePreferences.saveMicChimeType(context, chime)
                                    com.example.engine.MicChimeManager.playSpecificChime(chime)
                                    Toast.makeText(context, "${chime.displayName} सेट किया गया", Toast.LENGTH_SHORT).show()
                                },
                                label = {
                                    Text(
                                        text = "${chime.emoji} ${chime.displayName}",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SnehaCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = SnehaCyan,
                                    containerColor = SnehaDarkSurfaceVariant,
                                    labelColor = SnehaTextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = SnehaDarkSurfaceVariant,
                                    selectedBorderColor = SnehaCyan
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3c. Custom App Themes Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_app_themes"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                border = BorderStroke(1.5.dp, SnehaPurple)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(SnehaPurple.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎨", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "कस्टम ऐप थीम्स (Color Themes) 🎨",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SnehaTextPrimary
                            )
                            Text(
                                text = "सक्रिय: ${activeTheme.displayName} ${activeTheme.emoji}",
                                fontSize = 12.sp,
                                color = SnehaPurple,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "स्नेहा का रूप और रंग बदलें। अपनी पसंद की थीम पर टैप करें:",
                        fontSize = 11.sp,
                        color = SnehaTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.ui.theme.SnehaAppTheme.entries.forEach { theme ->
                            val isSelected = activeTheme == theme
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    activeTheme = theme
                                    com.example.ui.theme.SnehaAppTheme.saveTheme(context, theme)
                                    Toast.makeText(context, "${theme.displayName} थीम लागू की गई", Toast.LENGTH_SHORT).show()
                                },
                                label = {
                                    Text(
                                        text = "${theme.emoji} ${theme.displayName}",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SnehaPurple.copy(alpha = 0.25f),
                                    selectedLabelColor = SnehaPurple,
                                    containerColor = SnehaDarkSurfaceVariant,
                                    labelColor = SnehaTextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = SnehaDarkSurfaceVariant,
                                    selectedBorderColor = SnehaPurple
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3d. Floating Voice Bubble Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_floating_bubble"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                border = BorderStroke(1.5.dp, SnehaCyan)
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
                                    .size(40.dp)
                                    .background(SnehaCyan.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🫧", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "फ्लोटिंग वॉयस बबल (Bubble) 🫧",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SnehaTextPrimary
                                )
                                Text(
                                    text = if (isBubbleRunning) "सक्रिय: स्क्रीन पर तैर रहा है" else "निष्क्रिय (बंद)",
                                    fontSize = 12.sp,
                                    color = if (isBubbleRunning) SnehaCyan else SnehaTextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = isBubbleRunning,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    if (android.provider.Settings.canDrawOverlays(context)) {
                                        com.example.service.SnehaFloatingBubbleService.start(context)
                                        isBubbleRunning = true
                                        Toast.makeText(context, "फ्लोटिंग बबल चालू हो गया!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        try {
                                            val intent = android.content.Intent(
                                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                android.net.Uri.parse("package:${context.packageName}")
                                            ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                                            context.startActivity(intent)
                                            Toast.makeText(context, "कृपया 'अन्य ऐप्स के ऊपर दिखाएं' अनुमति दें", Toast.LENGTH_LONG).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "स्क्रीन ओवरले अनुमति की आवश्यकता है", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    com.example.service.SnehaFloatingBubbleService.stop(context)
                                    isBubbleRunning = false
                                    Toast.makeText(context, "फ्लोटिंग बबल बंद किया गया", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = SnehaCyan
                            ),
                            modifier = Modifier.testTag("switch_floating_bubble")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "YouTube, WhatsApp या गेम खेलते समय स्क्रीन पर स्नेहा का तैरता हुआ बबल रहेगा। टैप करते ही स्नेहा बात सुनेगी।",
                        fontSize = 11.sp,
                        color = SnehaTextSecondary
                    )
                }
            }
        }

        // 3e. Emergency SOS Location Setup Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_emergency_sos"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                border = BorderStroke(1.5.dp, SnehaPink)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(SnehaPink.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📍", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "इमरजेंसी SOS लोकेशन शेयर 🚨",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SnehaTextPrimary
                            )
                            Text(
                                text = "संपर्क: $sosName ($sosPhone)",
                                fontSize = 12.sp,
                                color = SnehaPink
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "मुसीबत में 'स्नेहा मुझे बचाओ' या 'हेल्प मी' बोलने पर इस नंबर पर लाइव Google Maps लोकेशन SMS जाएगी:",
                        fontSize = 11.sp,
                        color = SnehaTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = sosPhone,
                            onValueChange = { sosPhone = it },
                            label = { Text("इमरजेंसी फोन नंबर", color = SnehaTextSecondary, fontSize = 11.sp) },
                            placeholder = { Text("उदा. 9876543210 या 112", color = SnehaTextSecondary, fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("emergency_phone_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SnehaPink,
                                unfocusedBorderColor = SnehaDarkSurfaceVariant,
                                focusedTextColor = SnehaTextPrimary,
                                unfocusedTextColor = SnehaTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (sosPhone.isNotBlank()) {
                                    com.example.engine.EmergencySosManager.saveEmergencyContact(context, sosName, sosPhone.trim())
                                    Toast.makeText(context, "इमरजेंसी संपर्क सेव हो गया!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SnehaPink),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("save_emergency_sos_btn")
                        ) {
                            Text("सेव", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 3f. Voice Notes & Quick Reminders Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_voice_notes"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                border = BorderStroke(1.5.dp, SnehaEmerald)
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
                                    .size(40.dp)
                                    .background(SnehaEmerald.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📝", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "वॉयस नोट्स & रिमाइंडर डायरी 📝",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SnehaTextPrimary
                                )
                                Text(
                                    text = "कुल नोट्स: ${notesList.size}",
                                    fontSize = 12.sp,
                                    color = SnehaEmerald
                                )
                            }
                        }

                        if (notesList.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    val joined = notesList.take(3).mapIndexed { idx, it -> "${idx + 1}. ${it.text}" }.joinToString(". ")
                                    onTestCustomPhrase("मास्टर, आपकी डायरी के हाल के नोट्स हैं: $joined")
                                },
                                modifier = Modifier.testTag("btn_read_notes")
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "नोट्स सुनाओ", tint = SnehaEmerald)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "बोलें: 'नोट लिखो [बात]' या 'याद रखना कि [काम]'\nस्नेहा तुरंत तारीख और समय के साथ याद रखेगी।",
                        fontSize = 11.sp,
                        color = SnehaTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newNoteInput,
                            onValueChange = { newNoteInput = it },
                            placeholder = { Text("त्वरित नया नोट टाइप करें...", color = SnehaTextSecondary, fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_note_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SnehaEmerald,
                                unfocusedBorderColor = SnehaDarkSurfaceVariant,
                                focusedTextColor = SnehaTextPrimary,
                                unfocusedTextColor = SnehaTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newNoteInput.isNotBlank()) {
                                    com.example.engine.VoiceNotesManager.addNote(context, newNoteInput.trim())
                                    notesList = com.example.engine.VoiceNotesManager.getAllNotes(context)
                                    newNoteInput = ""
                                    Toast.makeText(context, "नोट सुरक्षित सेव हो गया!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SnehaEmerald),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("save_quick_note_btn")
                        ) {
                            Text("जोड़ें", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    if (notesList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        notesList.take(5).forEach { note ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(SnehaDarkSurfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = note.text,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SnehaTextPrimary
                                    )
                                    Text(
                                        text = "${if (note.type == "REMINDER") "⏰ रिमाइंडर" else "📌 नोट"} • ${note.getFormattedDate()}",
                                        fontSize = 10.sp,
                                        color = SnehaTextSecondary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        com.example.engine.VoiceNotesManager.deleteNote(context, note.id)
                                        notesList = com.example.engine.VoiceNotesManager.getAllNotes(context)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = "हटाएं",
                                        tint = SnehaPink,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Voice Personas
        item {
            Text(
                text = "स्नेहा के 5 वॉयस अवतार (Voice Personas) 🌸",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SnehaTextPrimary
            )
        }

        items(VoicePersona.ALL) { persona ->
            val isSelected = currentPersona.id == persona.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectPersona(persona) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF142434) else SnehaDarkSurface
                ),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) SnehaCyan else SnehaCardBorder
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(persona.iconEmoji, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = persona.nameHindi,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) SnehaCyan else SnehaTextPrimary
                                )
                                Text(
                                    text = persona.tagHindi,
                                    fontSize = 11.sp,
                                    color = SnehaTextSecondary
                                )
                            }
                        }

                        if (isSelected) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SnehaCyan.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, SnehaCyan)
                            ) {
                                Text(
                                    text = "चयनित",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SnehaCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onSelectPersona(persona) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "सुर: ${persona.defaultPitch} • गति: ${persona.defaultSpeed}x",
                            fontSize = 10.sp,
                            color = SnehaTextSecondary
                        )
                        OutlinedButton(
                            onClick = { onTestCustomPhrase(persona.sampleSpeech) },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SnehaCyan, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("नमूना सुनें", fontSize = 10.sp, color = SnehaCyan)
                        }
                    }
                }
            }
        }

        // 5. Speed & Pitch Sliders
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                border = BorderStroke(1.dp, SnehaCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "आवाज़ की गति व सुर (Speed & Pitch)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SnehaTextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("बोलने की गति (Speech Speed):", fontSize = 12.sp, color = SnehaTextSecondary)
                        Text("${((speechRate * 100).roundToInt()) / 100.0}x", fontSize = 12.sp, color = SnehaCyan, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = speechRate,
                        onValueChange = onRateChange,
                        valueRange = 0.7f..1.6f,
                        steps = 8,
                        colors = SliderDefaults.colors(thumbColor = SnehaCyan, activeTrackColor = SnehaCyan)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("आवाज़ का सुर (Pitch - ऊँचा सुर = मीठी आवाज़):", fontSize = 12.sp, color = SnehaTextSecondary)
                        Text("${((speechPitch * 100).roundToInt()) / 100.0}", fontSize = 12.sp, color = SnehaPurple, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = speechPitch,
                        onValueChange = onPitchChange,
                        valueRange = 0.8f..1.6f,
                        steps = 8,
                        colors = SliderDefaults.colors(thumbColor = SnehaPurple, activeTrackColor = SnehaPurple)
                    )
                }
            }
        }

        // 6. Voice Testing Studio
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                border = BorderStroke(1.dp, SnehaCyan.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = SnehaCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("वॉयस टेस्ट स्टूडियो 🎧", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SnehaTextPrimary)
                        }
                        OutlinedButton(
                            onClick = onResetToPersonaDefaults,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("डिफ़ॉल्ट", fontSize = 10.sp, color = SnehaTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customTestSentence,
                        onValueChange = { customTestSentence = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("टेस्ट वाक्य", fontSize = 11.sp, color = SnehaCyan) },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SnehaTextPrimary,
                            unfocusedTextColor = SnehaTextPrimary,
                            focusedBorderColor = SnehaCyan
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onTestCustomPhrase(customTestSentence) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSpeaking) SnehaPink else SnehaCyan
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSpeaking) "आवाज़ बंद करें ⏹️" else "स्नेहा की नई आवाज़ टेस्ट करें 🔊",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * TAB 1: Complete Security: Password, PIN (Custom Any Digit Count), Pattern Connector Dots,
 * Biometric Connector, Voice Unlock, and Security Test.
 */
@Composable
private fun SecurityPinPasswordTab(
    onSpeak: (String) -> Unit,
    onOpenUnlockScreen: () -> Unit
) {
    val context = LocalContext.current
    val userTitle = VoicePreferences.getUserName(context)

    var selectedType by remember { mutableStateOf(SecurityUnlockPreferences.getUnlockType(context)) }
    var pinLength by remember { mutableIntStateOf(SecurityUnlockPreferences.getPinLength(context).coerceIn(4, 16)) }
    var savedPin by remember { mutableStateOf(SecurityUnlockPreferences.getSavedPin(context)) }
    var savedPassword by remember { mutableStateOf(SecurityUnlockPreferences.getSavedPassword(context)) }
    val savedPattern = SecurityUnlockPreferences.getSavedPattern(context)

    var pinInput by remember { mutableStateOf(savedPin) }
    var passwordInput by remember { mutableStateOf(savedPassword) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val newPatternDots = remember {
        val initial = if (savedPattern.isNotBlank()) {
            savedPattern.split(",").mapNotNull { it.trim().toIntOrNull() }
        } else listOf(0, 1, 2, 4, 6, 7, 8)
        mutableStateListOf<Int>().apply { addAll(initial) }
    }

    var isBiometricActive by remember { mutableStateOf(SecurityUnlockPreferences.isBiometricConnectorEnabled(context)) }
    var isAppLockActive by remember { mutableStateOf(SecurityUnlockPreferences.isAppLockEnabled(context)) }
    var isVoiceUnlockActive by remember { mutableStateOf(SecurityUnlockPreferences.isVoiceUnlockEnabled(context)) }
    var isLockscreenGuardActive by remember { mutableStateOf(SecurityUnlockPreferences.isLockscreenGuardActive(context)) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                border = BorderStroke(1.5.dp, SnehaPurple)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(SnehaPurple.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = SnehaPurple, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$userTitle, फोन लॉक सुरक्षा केंद्र 🔐",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnehaTextPrimary
                        )
                        Text(
                            text = "सक्रिय विधि: ${selectedType.displayName} • पिन: $pinLength अंक",
                            fontSize = 12.sp,
                            color = SnehaCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 1. Unlock Type Selector (PIN, Pattern, Password)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                border = BorderStroke(1.dp, SnehaCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. प्राथमिक अनलॉक विधि चुनें:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SnehaTextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UnlockType.values().forEach { type ->
                            val isSelected = selectedType == type
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedType = type
                                    SecurityUnlockPreferences.saveUnlockType(context, type)
                                },
                                label = { Text(type.displayName, fontSize = 12.sp) },
                                leadingIcon = {
                                    val icon = when (type) {
                                        UnlockType.PIN -> Icons.Default.Pin
                                        UnlockType.PATTERN -> Icons.Default.GridOn
                                        UnlockType.PASSWORD -> Icons.Default.Key
                                    }
                                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SnehaCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = SnehaCyan
                                )
                            )
                        }
                    }
                }
            }
        }

        // 2. PIN Digit Length Customization (Kitne bhi digit ka add karein)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                border = BorderStroke(1.dp, if (selectedType == UnlockType.PIN) SnehaCyan else SnehaCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Pin, contentDescription = null, tint = SnehaCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "2. पिन की लंबाई चुनें (PIN Digit Length):",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnehaTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "आप कितने भी डिजिटल (अंक) का पिन सेट कर सकते हैं (4, 6, 8, 10, 12 अंक आदि):",
                        fontSize = 12.sp,
                        color = SnehaTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(4, 6, 8, 10, 12).forEach { len ->
                            FilterChip(
                                selected = pinLength == len,
                                onClick = {
                                    pinLength = len
                                    if (pinInput.length > len) {
                                        pinInput = pinInput.take(len)
                                    }
                                },
                                label = { Text("$len अंक", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SnehaCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = SnehaCyan
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }
                            if (digitsOnly.length <= pinLength) {
                                pinInput = digitsOnly
                            }
                        },
                        placeholder = { Text("$pinLength अंकों का नया पिन लिखें (उदा. ${"123456789012".take(pinLength)})...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SnehaCyan,
                            unfocusedBorderColor = SnehaDarkSurfaceVariant,
                            focusedTextColor = SnehaTextPrimary,
                            unfocusedTextColor = SnehaTextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // 3. Pattern Connector Dots (Connect pattern dots)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                border = BorderStroke(1.dp, if (selectedType == UnlockType.PATTERN) SnehaPurple else SnehaCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GridOn, contentDescription = null, tint = SnehaPurple)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "3. पैटर्न कनेक्टर सेटअप:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SnehaTextPrimary
                            )
                        }

                        if (newPatternDots.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { newPatternDots.clear() },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("रीसेट", fontSize = 10.sp, color = SnehaPink)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (newPatternDots.isEmpty()) "नीचे 3x3 बिंदुओं को जोड़कर अपना नया पैटर्न बनाएं:" else "नया पाथ: ${newPatternDots.map { it + 1 }.joinToString(" ➔ ")}",
                        fontSize = 12.sp,
                        color = if (newPatternDots.isEmpty()) SnehaTextSecondary else SnehaPink,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3x3 Dots Matrix
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (r in 0..2) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                for (c in 0..2) {
                                    val idx = r * 3 + c
                                    val isDotSelected = newPatternDots.contains(idx)
                                    val dotOrder = newPatternDots.indexOf(idx)

                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(if (isDotSelected) SnehaPurple.copy(alpha = 0.35f) else SnehaDarkSurfaceVariant)
                                            .border(1.5.dp, if (isDotSelected) SnehaPink else SnehaCardBorder, CircleShape)
                                            .clickable {
                                                if (!newPatternDots.contains(idx)) {
                                                    newPatternDots.add(idx)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isDotSelected) {
                                            Text("${dotOrder + 1}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SnehaPink)
                                        } else {
                                            Box(modifier = Modifier.size(8.dp).background(SnehaCyan, CircleShape))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Password Setup
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                border = BorderStroke(1.dp, if (selectedType == UnlockType.PASSWORD) SnehaPink else SnehaCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = SnehaPink)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "4. टेक्स्ट पासवर्ड सेटअप (Alphanumeric Password):",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnehaTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "अक्षरों, अंकों व सिंबल्स का सुरक्षित पासवर्ड सेट करें:",
                        fontSize = 12.sp,
                        color = SnehaTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        placeholder = { Text("नया पासवर्ड (उदा. Prince@2026, Master#123)...") },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = SnehaTextSecondary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SnehaPink,
                            unfocusedBorderColor = SnehaDarkSurfaceVariant,
                            focusedTextColor = SnehaTextPrimary,
                            unfocusedTextColor = SnehaTextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // 5. Extra Security Connectors (Biometric, App Lock, Voice Unlock, Lockscreen Guard)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                border = BorderStroke(1.dp, SnehaCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "अतिरिक्त सुरक्षा कनेक्टर्स & मोड्स 🛡️",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SnehaTextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Biometric Fingerprint Connector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = SnehaGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("बायोमेट्रिक फिंगरप्रिंट कनेक्टर", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SnehaTextPrimary)
                                Text("फिंगरप्रिंट से सुपर-फास्ट अनलॉक", fontSize = 11.sp, color = SnehaTextSecondary)
                            }
                        }
                        Switch(
                            checked = isBiometricActive,
                            onCheckedChange = {
                                isBiometricActive = it
                                SecurityUnlockPreferences.saveBiometricConnectorEnabled(context, it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = SnehaGreen)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // App Lock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = SnehaPurple)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("ऐप लॉक गार्ड (App Lock)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SnehaTextPrimary)
                                Text("स्नेहा ऐप खोलने से पहले सुरक्षा लॉक मांगें", fontSize = 11.sp, color = SnehaTextSecondary)
                            }
                        }
                        Switch(
                            checked = isAppLockActive,
                            onCheckedChange = {
                                isAppLockActive = it
                                SecurityUnlockPreferences.saveAppLockEnabled(context, it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = SnehaPurple)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Voice Command Unlock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = SnehaCyan)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("वॉयस अनलॉक ('स्नेहा फोन खोलो')", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SnehaTextPrimary)
                                Text("आवाज़ पहचानकर आपातकालीन अनलॉक अनुरोध", fontSize = 11.sp, color = SnehaTextSecondary)
                            }
                        }
                        Switch(
                            checked = isVoiceUnlockActive,
                            onCheckedChange = {
                                isVoiceUnlockActive = it
                                SecurityUnlockPreferences.saveVoiceUnlockEnabled(context, it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = SnehaCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Lockscreen Guard Auto-Active
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = SnehaEmerald)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("लॉकस्क्रीन गार्ड ऑटो-सक्रिय", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SnehaTextPrimary)
                                Text("स्क्रीन बंद होते ही सुरक्षा लॉक लागू करें", fontSize = 11.sp, color = SnehaTextSecondary)
                            }
                        }
                        Switch(
                            checked = isLockscreenGuardActive,
                            onCheckedChange = {
                                isLockscreenGuardActive = it
                                SecurityUnlockPreferences.saveLockscreenGuardActive(context, it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = SnehaEmerald)
                        )
                    }
                }
            }
        }

        // Save All Button
        item {
            Button(
                onClick = {
                    if (pinInput.isNotBlank()) {
                        SecurityUnlockPreferences.savePin(context, pinInput.trim())
                        SecurityUnlockPreferences.savePinLength(context, pinLength)
                    }
                    if (newPatternDots.size >= 4) {
                        SecurityUnlockPreferences.savePattern(context, newPatternDots.joinToString(","))
                    }
                    if (passwordInput.isNotBlank()) {
                        SecurityUnlockPreferences.savePassword(context, passwordInput.trim())
                    }
                    SecurityUnlockPreferences.saveUnlockType(context, selectedType)

                    Toast.makeText(context, "$userTitle, सभी सुरक्षा सेटिंग्स सफलतापूर्वक सहेज ली गईं!", Toast.LENGTH_SHORT).show()
                    onSpeak("$userTitle, आपकी $pinLength-अंकीय पिन, पैटर्न व पासवर्ड सेटिंग्स सुरक्षित रूप से सहेज ली गई हैं!")
                },
                colors = ButtonDefaults.buttonColors(containerColor = SnehaCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_all_security_settings_button")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("सभी सुरक्षा सेटिंग्स सहेजें (Save All)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        // Test Unlock Screen Button
        item {
            OutlinedButton(
                onClick = onOpenUnlockScreen,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("open_unlock_test_button")
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null, tint = SnehaPurple)
                Spacer(modifier = Modifier.width(8.dp))
                Text("सुरक्षा अनलॉक स्क्रीन का टेस्ट करें 🔓", color = SnehaPurple, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

/**
 * TAB 2: AI Cloud Connector Tab (ChatGPT, Gemini, Claude, Ollama, API Key Management & Live Ping Test)
 */
@Composable
private fun CloudConnectorTab(
    onOpenFullConnector: () -> Unit,
    onSpeak: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activeProvider by AiCloudConnectorManager.activeProvider.collectAsState()
    val connectionStatus by AiCloudConnectorManager.connectionStatus.collectAsState()

    var apiKeyInput by remember { mutableStateOf(AiCloudConnectorManager.getApiKey(context, activeProvider)) }
    var customEndpointInput by remember { mutableStateOf(AiCloudConnectorManager.getCustomEndpoint(context)) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cloud Status Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                border = BorderStroke(1.5.dp, SnehaPink)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(SnehaPink.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Cloud, contentDescription = null, tint = SnehaPink)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "मल्टी-मॉडल AI क्लाउड कनेक्टर 🤖",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SnehaTextPrimary
                            )
                            Text(
                                text = "सक्रिय इंजन: ${activeProvider.displayName} (${activeProvider.defaultModel})",
                                fontSize = 12.sp,
                                color = SnehaPink,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "स्नेहा ChatGPT (OpenAI), Google Gemini, Claude या आपके कस्टम प्राइवेट LLM से सीधे जुड़कर उत्तर देती है।",
                        fontSize = 12.sp,
                        color = SnehaTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Active Provider Selection
        item {
            Text(
                text = "एक्टिव AI प्रोवाइडर चुनें:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SnehaTextPrimary
            )
        }

        items(AiProvider.values()) { provider ->
            val isSelected = activeProvider == provider
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        AiCloudConnectorManager.setActiveProvider(context, provider)
                        apiKeyInput = AiCloudConnectorManager.getApiKey(context, provider)
                    },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF281C30) else SnehaDarkSurface
                ),
                border = BorderStroke(1.dp, if (isSelected) SnehaPink else SnehaCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(provider.iconEmoji, fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = provider.displayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) SnehaPink else SnehaTextPrimary
                            )
                            Text(
                                text = "${provider.defaultModel} • ${provider.description}",
                                fontSize = 11.sp,
                                color = SnehaTextSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    if (isSelected) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SnehaPink.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, SnehaPink)
                        ) {
                            Text(
                                text = "सक्रिय",
                                fontSize = 10.sp,
                                color = SnehaPink,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // API Key Management Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                border = BorderStroke(1.dp, SnehaCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${activeProvider.displayName} API Key दर्ज करें 🔑",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SnehaTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (activeProvider == AiProvider.GEMINI) "Google Gemini API Key ऑटो-इंजेक्टेड है, आप कस्टम की भी डाल सकते हैं" else "आपकी सुरक्षित API Key डिवाइस पर एन्क्रिप्टेड रहती है",
                        fontSize = 11.sp,
                        color = SnehaTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        placeholder = { Text("sk-... या API Key यहाँ पेस्ट करें") },
                        singleLine = true,
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(
                                    imageVector = if (isApiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = SnehaTextSecondary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SnehaPink,
                            unfocusedBorderColor = SnehaDarkSurfaceVariant,
                            focusedTextColor = SnehaTextPrimary,
                            unfocusedTextColor = SnehaTextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (activeProvider == AiProvider.CUSTOM) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("कस्टम Ollama / LLM एंडपॉइंट URL:", fontSize = 12.sp, color = SnehaTextSecondary)
                        OutlinedTextField(
                            value = customEndpointInput,
                            onValueChange = { customEndpointInput = it },
                            placeholder = { Text("http://10.0.2.2:11434/api/generate") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Save API Key Button
                        Button(
                            onClick = {
                                AiCloudConnectorManager.saveApiKey(context, activeProvider, apiKeyInput)
                                if (activeProvider == AiProvider.CUSTOM) {
                                    AiCloudConnectorManager.saveCustomEndpoint(context, customEndpointInput)
                                }
                                Toast.makeText(context, "${activeProvider.displayName} API Key सहेजी गई!", Toast.LENGTH_SHORT).show()
                                onSpeak("${activeProvider.displayName} API Key सहेज ली गई है!")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SnehaPink),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("API Key सहेजें", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // Live Connection Test Button
                        Button(
                            onClick = {
                                scope.launch {
                                    isTestingConnection = true
                                    val result = AiCloudConnectorManager.testConnection(context, activeProvider)
                                    isTestingConnection = false
                                    onSpeak(if (result.success) "क्लाउड कनेक्शन सफल! लेटेंसी ${result.latencyMs} मिलीसेकंड है।" else "कनेक्शन विफल! कृपया API Key जांचें।")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SnehaCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("कनेक्शन टेस्ट", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    // Connection Test Result Badge
                    connectionStatus?.let { status ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (status.success) SnehaGreen.copy(alpha = 0.15f) else SnehaPink.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, if (status.success) SnehaGreen else SnehaPink),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (status.success) "✅" else "❌", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (status.success) "कनेक्शन सफल! लेटेंसी: ${status.latencyMs}ms" else "कनेक्शन असफल",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (status.success) SnehaGreen else SnehaPink
                                    )
                                    Text(
                                        text = status.message,
                                        fontSize = 11.sp,
                                        color = SnehaTextSecondary,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Full Screen Shortcut
        item {
            OutlinedButton(
                onClick = onOpenFullConnector,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = SnehaCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("विस्तृत AI क्लाउड कनेक्टर स्क्रीन खोलें ⚡", color = SnehaCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

/**
 * TAB 3: All Features Directory & Extra Settings (Spam Blocker, Anti-Theft Guard, WhatsApp Auto-Reply,
 * Live Call Translator, Class Timetable, Call Summary, Screen Share, Emergency SOS, Phone Control,
 * Background Service, Auto-Read).
 */
data class FeatureDirectoryItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconColor: Color,
    val statusText: String,
    val targetScreen: SnehaScreen
)

@Composable
private fun AllFeaturesDirectoryTab(
    isServiceRunning: Boolean,
    isAutoReadEnabled: Boolean,
    onToggleService: (Boolean) -> Unit,
    onToggleAutoRead: (Boolean) -> Unit,
    onNavigate: (SnehaScreen) -> Unit
) {
    val context = LocalContext.current
    val features = listOf(
        FeatureDirectoryItem(
            title = "🛡️ एंटी-थेफ्ट गार्ड व अलार्म",
            description = "मोशन अलर्ट, पॉकेट डिटेक्शन, गलत पासवर्ड पर फोटो खींचना व तेज सायरन बजाना।",
            icon = Icons.Default.Shield,
            iconColor = SnehaEmerald,
            statusText = "सुरक्षा सक्रिय",
            targetScreen = SnehaScreen.ANTI_THEFT
        ),
        FeatureDirectoryItem(
            title = "🚫 स्पैम कॉलर व एसएमएस ब्लॉकर",
            description = "कॉल आने पर कॉलर आईडी स्कोर जांचना, फ्रॉड नंबर ब्लॉक करना व AI ऑटो-स्क्रीनिंग।",
            icon = Icons.Default.Block,
            iconColor = SnehaPink,
            statusText = "स्मार्ट फ़िल्टर ऑन",
            targetScreen = SnehaScreen.SPAM_BLOCKER
        ),
        FeatureDirectoryItem(
            title = "💬 व्हाट्सएप कॉल व मैसेज ऑटो-रिप्लाई",
            description = "कॉल या मैसेज आने पर स्नेहा द्वारा स्वतः क्लास/व्यस्तता अनुसार ऑटो-रिप्लाई व अलर्ट।",
            icon = Icons.Default.Chat,
            iconColor = SnehaGreen,
            statusText = "ऑटो-रिप्लाई ऑन",
            targetScreen = SnehaScreen.WHATSAPP_AUTO_REPLY
        ),
        FeatureDirectoryItem(
            title = "🌐 कॉल के दौरान लाइव ट्रांसलेटर",
            description = "द्विभाषी रियल-टाइम बातचीत अनुवाद (हिंदी ⇄ अंग्रेज़ी, बंगाली, मराठी, तमिल आदि)।",
            icon = Icons.Default.Translate,
            iconColor = SnehaCyan,
            statusText = "लाइव अनुवादक तैयार",
            targetScreen = SnehaScreen.CALL_TRANSLATOR
        ),
        FeatureDirectoryItem(
            title = "🎓 स्मार्ट क्लास टाइमटेबल व ऑटो-साइलेंट",
            description = "कॉलेज टाइमटेबल अनुसार क्लास के दौरान फोन स्वतः साइलेंट व ऑटो-रिप्लाई भेजना।",
            icon = Icons.Default.School,
            iconColor = SnehaCyan,
            statusText = "कॉलेज शेड्यूल सिंक",
            targetScreen = SnehaScreen.CLASS_TIMETABLE
        ),
        FeatureDirectoryItem(
            title = "📝 AI कॉल सारांश व ट्रांसक्रिप्ट",
            description = "कॉल समाप्त होते ही बातचीत से महत्वपूर्ण बिंदु, तारीखें व एक्शन टास्क लिस्ट बनाना।",
            icon = Icons.Default.Phone,
            iconColor = SnehaPurple,
            statusText = "सारांश इंजन ऑन",
            targetScreen = SnehaScreen.CALL_SUMMARY
        ),
        FeatureDirectoryItem(
            title = "🔐 लॉक स्क्रीन सुरक्षा (पिन, पैटर्न, पासवर्ड)",
            description = "4 से 12+ अंकों का पिन, 3x3 पैटर्न कनेक्टर, टेक्स्ट पासवर्ड व फिंगरप्रिंट सपोर्ट।",
            icon = Icons.Default.Lock,
            iconColor = SnehaCyan,
            statusText = "लॉक गार्ड सक्रिय",
            targetScreen = SnehaScreen.SECURITY_UNLOCK
        ),
        FeatureDirectoryItem(
            title = "🤖 AI क्लाउड कनेक्टर (ChatGPT, Gemini, Claude)",
            description = "OpenAI GPT-4o, Google Gemini और Claude के साथ रियल-टाइम AI ज्ञान।",
            icon = Icons.Default.Cloud,
            iconColor = SnehaPink,
            statusText = "मल्टी-मॉडल कनेक्टेड",
            targetScreen = SnehaScreen.AI_CONNECTOR
        ),
        FeatureDirectoryItem(
            title = "🆘 इमरजेंसी एसओएस व परिजन सुरक्षा",
            description = "112 व परिजनों को आपातकालीन लोकेशन SMS, लाउड सायरन व स्ट्रोब फ्लैशलाइट।",
            icon = Icons.Default.Warning,
            iconColor = SnehaPink,
            statusText = "एसओएस मुस्तैद",
            targetScreen = SnehaScreen.EMERGENCY
        ),
        FeatureDirectoryItem(
            title = "📱 स्क्रीन शेयर व रिमोट असिस्टेंट",
            description = "लाइव स्क्रीन ब्रॉडकास्टिंग, वेब व्यूइंग व दूरस्थ तकनीकी सहायता।",
            icon = Icons.Default.ScreenShare,
            iconColor = SnehaCyan,
            statusText = "रिमोट रेडी",
            targetScreen = SnehaScreen.SCREEN_SHARE
        ),
        FeatureDirectoryItem(
            title = "⚙️ फोन हार्डवेयर व सिस्टम कंट्रोल",
            description = "टॉर्च, सायरन, बैटरी स्थिति, वॉल्यूम, वाई-फाई, ब्लूटूथ व ऐप ओपन कमांड्स।",
            icon = Icons.Default.Settings,
            iconColor = SnehaPurple,
            statusText = "सिस्टम कंट्रोलर ऑन",
            targetScreen = SnehaScreen.PHONE_CONTROL
        ),
        FeatureDirectoryItem(
            title = "🗣️ बैकग्राउंड वॉयस लिसनर व ऑटो-रीड",
            description = "स्क्रीन ऑफ होने पर भी 'स्नेहा' वेक वर्ड सुनना और महत्वपूर्ण संदेश बोलकर सुनाना।",
            icon = Icons.Default.RecordVoiceOver,
            iconColor = SnehaEmerald,
            statusText = "24/7 स्टैंडबाय",
            targetScreen = SnehaScreen.SAFE_MESSAGES
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Extra Toggles Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                border = BorderStroke(1.dp, SnehaCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "सिस्टम बैकग्राउंड स्विच (Quick Toggles) ⚙️",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SnehaTextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Background Service
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = SnehaEmerald)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("बैकग्राउंड वॉयस सर्विस", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SnehaTextPrimary)
                                Text("फोन लॉक होने पर भी स्नेहा एक्टिव रहेगी", fontSize = 11.sp, color = SnehaTextSecondary)
                            }
                        }
                        Switch(
                            checked = isServiceRunning,
                            onCheckedChange = onToggleService,
                            colors = SwitchDefaults.colors(checkedThumbColor = SnehaEmerald)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Auto Read
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = SnehaCyan)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("संदेश स्वतः बोलकर सुनाएं (Auto-Read)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SnehaTextPrimary)
                                Text("SMS व WhatsApp संदेश आते ही बोलकर पढ़ें (OTP सुरक्षित)", fontSize = 11.sp, color = SnehaTextSecondary)
                            }
                        }
                        Switch(
                            checked = isAutoReadEnabled,
                            onCheckedChange = onToggleAutoRead,
                            colors = SwitchDefaults.colors(checkedThumbColor = SnehaCyan)
                        )
                    }
                }
            }
        }

        // Features Catalog Header
        item {
            Text(
                text = "स्नेहा के सभी 12 फीचर्स की संपूर्ण डायरेक्टरी 🚀",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = SnehaTextPrimary
            )
        }

        // Feature Directory List
        items(features) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(item.targetScreen) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                border = BorderStroke(1.dp, SnehaCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(item.iconColor.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(item.icon, contentDescription = null, tint = item.iconColor, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = item.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SnehaTextPrimary
                                )
                                Text(
                                    text = item.statusText,
                                    fontSize = 10.sp,
                                    color = item.iconColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Button(
                            onClick = { onNavigate(item.targetScreen) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = item.iconColor.copy(alpha = 0.25f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("खोलें ➔", color = item.iconColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.description,
                        fontSize = 11.sp,
                        color = SnehaTextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
