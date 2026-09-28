package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CallTranslatorManager
import com.example.ui.theme.SnehaBgDark
import com.example.ui.theme.SnehaCyan
import com.example.ui.theme.SnehaDarkCard
import com.example.ui.theme.SnehaDarkSurface
import com.example.ui.theme.SnehaDarkSurfaceVariant
import com.example.ui.theme.SnehaPink
import com.example.ui.theme.SnehaTextPrimary
import com.example.ui.theme.SnehaTextSecondary
import kotlinx.coroutines.launch

@Composable
fun LiveCallTranslatorScreen(
    modifier: Modifier = Modifier,
    onSpeak: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val translations by CallTranslatorManager.translationHistory.collectAsState()
    val sourceLang by CallTranslatorManager.activeSourceLang.collectAsState()
    val targetLang by CallTranslatorManager.activeTargetLang.collectAsState()

    var manualInput by remember { mutableStateOf("") }
    var isTranslating by remember { mutableStateOf(false) }

    var showSourceDropdown by remember { mutableStateOf(false) }
    var showTargetDropdown by remember { mutableStateOf(false) }

    val languages = CallTranslatorManager.supportedLanguages

    val sourceLangObj = languages.firstOrNull { it.code == sourceLang } ?: languages[0]
    val targetLangObj = languages.firstOrNull { it.code == targetLang } ?: languages[1]

    // Speech recognition launcher for Master
    val masterSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = spokenList?.firstOrNull()?.trim() ?: ""
            if (spoken.isNotBlank()) {
                scope.launch {
                    isTranslating = true
                    val trans = CallTranslatorManager.translateText(
                        text = spoken,
                        fromLang = sourceLang,
                        toLang = targetLang,
                        isUser = true,
                        context = context
                    )
                    isTranslating = false
                    onSpeak(trans)
                }
            }
        }
    }

    // Speech recognition launcher for Caller
    val callerSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = spokenList?.firstOrNull()?.trim() ?: ""
            if (spoken.isNotBlank()) {
                scope.launch {
                    isTranslating = true
                    val trans = CallTranslatorManager.translateText(
                        text = spoken,
                        fromLang = targetLang,
                        toLang = sourceLang,
                        isUser = false,
                        context = context
                    )
                    isTranslating = false
                    onSpeak(trans)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SnehaBgDark)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header Card with Language Selectors and Swap
        Card(
            colors = CardDefaults.cardColors(containerColor = SnehaDarkCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Translate",
                            tint = SnehaCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "कॉल के दौरान लाइव ट्रांसलेटर",
                            color = SnehaTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    if (translations.isNotEmpty()) {
                        TextButton(
                            onClick = { CallTranslatorManager.clearHistory(context) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("साफ़ करें", color = SnehaCyan, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Language Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Source Lang Box
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { showSourceDropdown = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${sourceLangObj.flagEmoji} ${sourceLangObj.nameHindi}",
                                color = SnehaTextPrimary,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                        DropdownMenu(
                            expanded = showSourceDropdown,
                            onDismissRequest = { showSourceDropdown = false }
                        ) {
                            languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text("${lang.flagEmoji} ${lang.nameHindi} (${lang.nameEnglish})") },
                                    onClick = {
                                        CallTranslatorManager.setLanguages(lang.code, targetLang)
                                        showSourceDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Swap Button
                    IconButton(
                        onClick = { CallTranslatorManager.swapLanguages() },
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .testTag("swap_languages_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Swap Languages",
                            tint = SnehaCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Target Lang Box
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { showTargetDropdown = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${targetLangObj.flagEmoji} ${targetLangObj.nameHindi}",
                                color = SnehaTextPrimary,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                        DropdownMenu(
                            expanded = showTargetDropdown,
                            onDismissRequest = { showTargetDropdown = false }
                        ) {
                            languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text("${lang.flagEmoji} ${lang.nameHindi} (${lang.nameEnglish})") },
                                    onClick = {
                                        CallTranslatorManager.setLanguages(sourceLang, lang.code)
                                        showTargetDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dual Speaking Action Bar (Master speaks vs Caller speaks)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Master Button (Hindi -> English)
            Button(
                onClick = {
                    if (manualInput.isNotBlank()) {
                        scope.launch {
                            isTranslating = true
                            val translated = CallTranslatorManager.translateText(
                                text = manualInput,
                                fromLang = sourceLang,
                                toLang = targetLang,
                                isUser = true,
                                context = context
                            )
                            isTranslating = false
                            manualInput = ""
                            onSpeak(translated)
                        }
                    } else {
                        // Start speech recognition for Master
                        try {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (sourceLang == "hi") "hi-IN" else sourceLang)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "मास्टर, बोलिए (${sourceLangObj.nameHindi})...")
                            }
                            masterSpeechLauncher.launch(intent)
                        } catch (e: Exception) {
                            // Fallback to phrase translation
                            scope.launch {
                                isTranslating = true
                                val translated = CallTranslatorManager.translateText(
                                    text = "मैं अभी कॉलेज की लाइब्रेरी में हूँ।",
                                    fromLang = sourceLang,
                                    toLang = targetLang,
                                    isUser = true,
                                    context = context
                                )
                                isTranslating = false
                                onSpeak(translated)
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SnehaCyan),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("master_speak_translate_button")
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("मास्टर बोलें", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("(${sourceLangObj.nameHindi} ➔ ${targetLangObj.nameEnglish})", color = Color.Black.copy(alpha = 0.7f), fontSize = 9.sp)
                }
            }

            // Caller Button (English -> Hindi)
            Button(
                onClick = {
                    if (manualInput.isNotBlank()) {
                        scope.launch {
                            isTranslating = true
                            val translated = CallTranslatorManager.translateText(
                                text = manualInput,
                                fromLang = targetLang,
                                toLang = sourceLang,
                                isUser = false,
                                context = context
                            )
                            isTranslating = false
                            manualInput = ""
                            onSpeak(translated)
                        }
                    } else {
                        // Start speech recognition for Caller
                        try {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (targetLang == "en") "en-US" else targetLang)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Caller speaking (${targetLangObj.nameEnglish})...")
                            }
                            callerSpeechLauncher.launch(intent)
                        } catch (e: Exception) {
                            // Fallback to phrase translation
                            scope.launch {
                                isTranslating = true
                                val translated = CallTranslatorManager.translateText(
                                    text = "Hello, where are you right now?",
                                    fromLang = targetLang,
                                    toLang = sourceLang,
                                    isUser = false,
                                    context = context
                                )
                                isTranslating = false
                                onSpeak(translated)
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SnehaPink),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("caller_speak_translate_button")
            ) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("कॉलर बोलें", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("(${targetLangObj.nameEnglish} ➔ ${sourceLangObj.nameHindi})", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Direct Text Input for Translation
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = manualInput,
                onValueChange = { manualInput = it },
                placeholder = { Text("अनुवाद के लिए वाक्य लिखें...", color = SnehaTextSecondary, fontSize = 12.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("translator_text_input"),
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
                    if (manualInput.isNotBlank()) {
                        scope.launch {
                            val trans = CallTranslatorManager.translateText(
                                text = manualInput,
                                fromLang = sourceLang,
                                toLang = targetLang,
                                isUser = true,
                                context = context
                            )
                            manualInput = ""
                            onSpeak(trans)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SnehaDarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Translate & Speak", tint = SnehaCyan)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live Conversation Transcript
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(translations) { item ->
                val isMaster = item.isUserSpeaking
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMaster) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMaster) Color(0xFF16323B) else Color(0xFF381E2E)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isMaster) "👤 मास्टर (${item.sourceLang.uppercase()})" else "📞 कॉलर (${item.sourceLang.uppercase()})",
                                    color = if (isMaster) SnehaCyan else SnehaPink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                IconButton(
                                    onClick = { onSpeak(item.translatedText) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Speak Aloud",
                                        tint = SnehaTextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "बोला: \"${item.originalText}\"",
                                color = SnehaTextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "अनुवाद: \"${item.translatedText}\"",
                                color = SnehaTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
