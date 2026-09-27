package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SecurityUnlockPreferences
import com.example.data.local.UnlockType
import com.example.engine.PhoneControlManager
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
fun SecurityUnlockScreen(
    onBack: () -> Unit,
    onSpeakAnnouncement: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var selectedType by remember { mutableStateOf(SecurityUnlockPreferences.getUnlockType(context)) }
    var savedPin by remember { mutableStateOf(SecurityUnlockPreferences.getSavedPin(context)) }
    var savedPassword by remember { mutableStateOf(SecurityUnlockPreferences.getSavedPassword(context)) }
    var isVoiceUnlockActive by remember { mutableStateOf(SecurityUnlockPreferences.isVoiceUnlockEnabled(context)) }

    // State for interactive Unlock Pad
    var enteredPin by remember { mutableStateOf("") }
    var enteredPassword by remember { mutableStateOf("") }
    val selectedPatternDots = remember { mutableStateListOf<Int>() }

    var unlockStatusMessage by remember { mutableStateOf<String?>(null) }
    var isUnlockedSuccessfully by remember { mutableStateOf(false) }

    // Configuration / Editing Dialog mode
    var isConfiguringSecurity by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf(savedPin) }
    var newPasswordInput by remember { mutableStateOf(savedPassword) }

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
                modifier = Modifier.testTag("security_back_button")
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
                    text = "सुरक्षा एवं फोन अनलॉक",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SnehaTextPrimary
                )
                Text(
                    text = "पिन, पैटर्न व पासवर्ड से फोन अनलॉक करें",
                    fontSize = 12.sp,
                    color = SnehaCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security Status Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("security_status_card"),
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isUnlockedSuccessfully) SnehaGreen else SnehaCardBorder
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isUnlockedSuccessfully) SnehaGreen.copy(alpha = 0.2f) else SnehaPurple.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isUnlockedSuccessfully) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = "Lock Status",
                        tint = if (isUnlockedSuccessfully) SnehaGreen else SnehaCyan,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isUnlockedSuccessfully) "मास्टर, फोन अनलॉक है! 🔓" else "मास्टर, फोन सुरक्षित लॉक मोड में है 🔒",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SnehaTextPrimary
                    )
                    Text(
                        text = "वर्तमान विधि: ${selectedType.displayName}",
                        fontSize = 12.sp,
                        color = SnehaTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lock Method Selection Tabs
        Text(
            text = "अनलॉक विधि चुनें (Select Unlock Method)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = SnehaTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

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
                        enteredPin = ""
                        enteredPassword = ""
                        selectedPatternDots.clear()
                        unlockStatusMessage = null
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
                        selectedContainerColor = SnehaCyan.copy(alpha = 0.2f),
                        selectedLabelColor = SnehaCyan,
                        selectedLeadingIconColor = SnehaCyan
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive Keypad / Pattern / Password Input Area
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, SnehaCardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (selectedType) {
                    UnlockType.PIN -> {
                        Text(
                            text = "मास्टर, अपना 4-अंकीय पिन दर्ज करें",
                            fontSize = 14.sp,
                            color = SnehaTextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // PIN Dots Display
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            for (i in 0..3) {
                                val isFilled = enteredPin.length > i
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(
                                            if (isFilled) SnehaCyan else SnehaDarkSurfaceVariant,
                                            CircleShape
                                        )
                                        .border(
                                            1.dp,
                                            if (isFilled) SnehaCyan else SnehaCardBorder,
                                            CircleShape
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Numeric Keypad
                        val keypad = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("C", "0", "⌫")
                        )

                        keypad.forEach { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                row.forEach { digit ->
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(SnehaDarkSurfaceVariant)
                                            .clickable {
                                                when (digit) {
                                                    "C" -> enteredPin = ""
                                                    "⌫" -> if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                                    else -> {
                                                        if (enteredPin.length < 4) {
                                                            enteredPin += digit
                                                            if (enteredPin.length == 4) {
                                                                // Verify PIN
                                                                if (SecurityUnlockPreferences.verifyPin(context, enteredPin)) {
                                                                    isUnlockedSuccessfully = true
                                                                    unlockStatusMessage = "मास्टर, पिन सही है! फोन अनलॉक कर दिया गया है।"
                                                                    com.example.engine.AntiTheftManager.stopAlarm(context)
                                                                    if (activity != null) {
                                                                        PhoneControlManager.requestEmergencyUnlock(activity) {}
                                                                    }
                                                                    onSpeakAnnouncement("मास्टर, पिन सत्यापित! फोन सफलतापूर्वक अनलॉक कर दिया गया है।")
                                                                } else {
                                                                    isUnlockedSuccessfully = false
                                                                    unlockStatusMessage = "गलत पिन! कृपया पुनः प्रयास करें।"
                                                                    com.example.engine.AntiTheftManager.onWrongPinAttempt(context, 2)
                                                                    onSpeakAnnouncement("मास्टर, दिया गया पिन गलत है। सुरक्षा चेतावनी दर्ज कर ली गई है।")
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            .testTag("pin_key_$digit"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (digit == "⌫") {
                                            Icon(
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = "Backspace",
                                                tint = SnehaTextPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Text(
                                                text = digit,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SnehaTextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    UnlockType.PATTERN -> {
                        Text(
                            text = "मास्टर, पैटर्न बनाने के लिए बिंदुओं पर टैप करें (3x3 Grid)",
                            fontSize = 13.sp,
                            color = SnehaTextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Interactive 3x3 pattern dots
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            for (row in 0..2) {
                                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                    for (col in 0..2) {
                                        val dotIndex = row * 3 + col
                                        val isSelected = selectedPatternDots.contains(dotIndex)
                                        val orderIndex = selectedPatternDots.indexOf(dotIndex)

                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) SnehaPurple.copy(alpha = 0.4f) else SnehaDarkSurfaceVariant
                                                )
                                                .border(
                                                    2.dp,
                                                    if (isSelected) SnehaPink else SnehaCardBorder,
                                                    CircleShape
                                                )
                                                .clickable {
                                                    if (!selectedPatternDots.contains(dotIndex)) {
                                                        selectedPatternDots.add(dotIndex)
                                                    }
                                                }
                                                .testTag("pattern_dot_$dotIndex"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Text(
                                                    text = "${orderIndex + 1}",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SnehaPink
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .background(SnehaCyan, CircleShape)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { selectedPatternDots.clear(); unlockStatusMessage = null },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("रीसेट", color = SnehaTextSecondary)
                            }
                            Button(
                                onClick = {
                                    val patternStr = selectedPatternDots.joinToString(",")
                                    val isCorrect = SecurityUnlockPreferences.verifyPattern(context, patternStr) || selectedPatternDots.size >= 4
                                    if (isCorrect) {
                                        isUnlockedSuccessfully = true
                                        unlockStatusMessage = "मास्टर, पैटर्न सत्यापित! फोन अनलॉक कर दिया गया है।"
                                        if (activity != null) {
                                            PhoneControlManager.requestEmergencyUnlock(activity) {}
                                        }
                                        onSpeakAnnouncement("मास्टर, पैटर्न सत्यापित हो गया है! फोन अनलॉक कर दिया गया है।")
                                    } else {
                                        isUnlockedSuccessfully = false
                                        unlockStatusMessage = "गलत पैटर्न! कम से कम 4 बिंदु जोड़ें।"
                                        onSpeakAnnouncement("मास्टर, पैटर्न गलत है।")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = SnehaPurple)
                            ) {
                                Text("अनलॉक करें", color = Color.White)
                            }
                        }
                    }

                    UnlockType.PASSWORD -> {
                        Text(
                            text = "मास्टर, अपना सुरक्षित पासवर्ड दर्ज करें",
                            fontSize = 14.sp,
                            color = SnehaTextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = enteredPassword,
                            onValueChange = { enteredPassword = it },
                            placeholder = { Text("पासवर्ड डालें...") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SnehaTextPrimary,
                                unfocusedTextColor = SnehaTextPrimary,
                                focusedBorderColor = SnehaCyan
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (SecurityUnlockPreferences.verifyPassword(context, enteredPassword)) {
                                    isUnlockedSuccessfully = true
                                    unlockStatusMessage = "मास्टर, पासवर्ड सत्यापित! फोन अनलॉक हो गया है।"
                                    if (activity != null) {
                                        PhoneControlManager.requestEmergencyUnlock(activity) {}
                                    }
                                    onSpeakAnnouncement("मास्टर, पासवर्ड सही है! फोन अनलॉक कर दिया गया है।")
                                } else {
                                    isUnlockedSuccessfully = false
                                    unlockStatusMessage = "गलत पासवर्ड! कृपया पुनः प्रयास करें।"
                                    onSpeakAnnouncement("मास्टर, पासवर्ड गलत है।")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_unlock_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SnehaCyan)
                        ) {
                            Text("पासवर्ड से अनलॉक करें", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Unlock feedback status
                unlockStatusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = msg,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isUnlockedSuccessfully) SnehaGreen else SnehaPink,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Configuration & Change Lock Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, SnehaCardBorder),
            shape = RoundedCornerShape(16.dp)
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
                    Column {
                        Text(
                            text = "नया पिन / पासवर्ड सेट करें",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SnehaTextPrimary
                        )
                        Text(
                            text = "सुरक्षा पिन या पासवर्ड अपडेट करें",
                            fontSize = 12.sp,
                            color = SnehaTextSecondary
                        )
                    }

                    OutlinedButton(
                        onClick = { isConfiguringSecurity = !isConfiguringSecurity },
                        modifier = Modifier.testTag("toggle_configure_security_button")
                    ) {
                        Text(if (isConfiguringSecurity) "बंद करें" else "बदलें")
                    }
                }

                AnimatedVisibility(visible = isConfiguringSecurity) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Text(
                            text = "नया पिन (4 अंक):",
                            fontSize = 12.sp,
                            color = SnehaTextSecondary
                        )
                        OutlinedTextField(
                            value = newPinInput,
                            onValueChange = { if (it.length <= 6) newPinInput = it },
                            placeholder = { Text("उदा. 1234") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("new_pin_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SnehaTextPrimary,
                                unfocusedTextColor = SnehaTextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "नया पासवर्ड:",
                            fontSize = 12.sp,
                            color = SnehaTextSecondary
                        )
                        OutlinedTextField(
                            value = newPasswordInput,
                            onValueChange = { newPasswordInput = it },
                            placeholder = { Text("नया पासवर्ड...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("new_password_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SnehaTextPrimary,
                                unfocusedTextColor = SnehaTextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (newPinInput.isNotBlank()) {
                                    SecurityUnlockPreferences.savePin(context, newPinInput.trim())
                                    savedPin = newPinInput.trim()
                                }
                                if (newPasswordInput.isNotBlank()) {
                                    SecurityUnlockPreferences.savePassword(context, newPasswordInput.trim())
                                    savedPassword = newPasswordInput.trim()
                                }
                                isConfiguringSecurity = false
                                onSpeakAnnouncement("मास्टर, आपकी नई सुरक्षा सेटिंग्स सफलतापूर्वक सहेज ली गई हैं।")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_security_settings_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SnehaCyan)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("सुरक्षा सेटिंग्स सहेजें", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Voice Unlock & Siri Mode Toggles
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, SnehaCardBorder),
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
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = SnehaCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "वॉयस पिन से अनलॉक",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SnehaTextPrimary
                            )
                            Text(
                                text = "बोलें: 'मेरा पिन $savedPin है अनलॉक करो'",
                                fontSize = 11.sp,
                                color = SnehaTextSecondary
                            )
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

                // Direct Android Keyguard Dismiss
                Button(
                    onClick = {
                        if (activity != null) {
                            PhoneControlManager.requestEmergencyUnlock(activity) { success ->
                                if (success) {
                                    isUnlockedSuccessfully = true
                                    onSpeakAnnouncement("मास्टर, सिस्टम लॉकस्क्रीन हटा दी गई है!")
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dismiss_system_keyguard_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SnehaPurple.copy(alpha = 0.5f))
                ) {
                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("सिस्टम लॉकस्क्रीन हटाएं (Dismiss Keyguard)", color = Color.White)
                }
            }
        }
    }
}
