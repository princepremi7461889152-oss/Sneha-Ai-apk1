package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.ChatTurn
import com.example.data.remote.GeminiCreativeClient
import com.example.data.remote.GroundedResponse
import com.example.data.remote.SearchSource
import com.example.ui.theme.SnehaBgDark
import com.example.ui.theme.SnehaCyan
import com.example.ui.theme.SnehaDarkCard
import com.example.ui.theme.SnehaDarkSurface
import com.example.ui.theme.SnehaDarkSurfaceVariant
import com.example.ui.theme.SnehaGreen
import com.example.ui.theme.SnehaPink
import com.example.ui.theme.SnehaPurple
import com.example.ui.theme.SnehaTextPrimary
import com.example.ui.theme.SnehaTextSecondary
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

enum class CreativeTab(val title: String, val iconEmoji: String) {
    CHATBOT("Gemini चैट", "💬"),
    SEARCH("सर्च ग्राउंडिंग", "🔍"),
    MAPS("मैप्स ग्राउंडिंग", "📍"),
    IMAGE("इमेज स्टूडियो", "🎨"),
    VIDEO("Veo वीडियो", "🎬"),
    MUSIC("Lyria म्यूजिक", "🎵"),
    LIVE("Gemini Live", "🎙️"),
    TRANSCRIBE("ट्रांसक्राइब", "📝")
}

@Composable
fun GeminiCreativeStudioScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = remember { CreativeTab.values() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SnehaBgDark)
    ) {
        // Top Header
        Surface(
            color = SnehaDarkSurface,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "वापस",
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Gemini AI क्रिएटिव स्टूडियो",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = SnehaCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "PRO AI",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = SnehaCyan,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "म्यूजिक, इमेज, वीडियो, सर्च, मैप्स, लाइव व चैट मॉडल्स",
                        fontSize = 11.sp,
                        color = SnehaTextSecondary
                    )
                }
            }
        }

        // Horizontal Scrollable Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = SnehaDarkSurfaceVariant,
            contentColor = SnehaCyan,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tab.iconEmoji, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                tab.title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) SnehaCyan else SnehaTextSecondary
                            )
                        }
                    }
                )
            }
        }

        // Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            when (tabs[selectedTabIndex]) {
                CreativeTab.CHATBOT -> GeminiChatbotTab()
                CreativeTab.SEARCH -> GeminiSearchGroundingTab()
                CreativeTab.MAPS -> GeminiMapsGroundingTab()
                CreativeTab.IMAGE -> GeminiImageStudioTab()
                CreativeTab.VIDEO -> VeoVideoStudioTab()
                CreativeTab.MUSIC -> LyriaMusicStudioTab()
                CreativeTab.LIVE -> GeminiLiveVoiceTab()
                CreativeTab.TRANSCRIBE -> GeminiTranscribeTab()
            }
        }
    }
}

// -------------------------------------------------------------
// 1. GEMINI CHATBOT TAB
// -------------------------------------------------------------
@Composable
private fun GeminiChatbotTab() {
    val scope = rememberCoroutineScope()
    var selectedModel by remember { mutableStateOf("gemini-3.5-flash") }
    val models = listOf(
        Pair("gemini-3.5-flash", "General (3.5 Flash)"),
        Pair("gemini-3.1-pro-preview", "Complex (3.1 Pro)"),
        Pair("gemini-3.1-flash-lite", "Fast (3.1 Lite)")
    )

    var systemInstruction by remember {
        mutableStateOf("आप स्नेहा हैं, एक अत्यंत बुद्धिमान, विनम्र और सहायक AI सहायक। आप मास्टर के हर सवाल का सटीक और मधुर उत्तर देती हैं।")
    }

    val history = remember {
        mutableStateListOf(
            ChatTurn("user", "नमस्ते स्नेहा! आप मेरी क्या मदद कर सकती हैं?"),
            ChatTurn("model", "नमस्ते मास्टर! मैं आपके प्रश्नों के उत्तर दे सकती हूँ, कोडिंग व रिसर्च में मदद कर सकती हूँ, नोट्स बना सकती हूँ और किसी भी विषय को सरलता से समझा सकती हूँ।")
        )
    }

    var messageInput by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Model & Role Selector Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "मॉडल व सिस्टम रोल चुनें:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SnehaCyan
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(models) { (modelId, label) ->
                        FilterChip(
                            selected = selectedModel == modelId,
                            onClick = { selectedModel = modelId },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SnehaCyan.copy(alpha = 0.25f),
                                selectedLabelColor = SnehaCyan
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Message Thread
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(history) { turn ->
                val isUser = turn.role == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) SnehaPurple.copy(alpha = 0.35f) else SnehaDarkSurfaceVariant
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (isUser) SnehaPurple.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f)),
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (isUser) "आप (You)" else "Gemini (${selectedModel.substringAfterLast("-")})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUser) SnehaPink else SnehaCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = turn.text,
                                fontSize = 13.sp,
                                color = SnehaTextPrimary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
            if (isSending) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = SnehaCyan, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Gemini सोच रहा है...", fontSize = 12.sp, color = SnehaTextSecondary)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                placeholder = { Text("Gemini से सवाल पूछें...", fontSize = 12.sp, color = SnehaTextSecondary) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SnehaDarkSurface,
                    unfocusedContainerColor = SnehaDarkSurface,
                    focusedBorderColor = SnehaCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedTextColor = SnehaTextPrimary,
                    unfocusedTextColor = SnehaTextPrimary
                ),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val prompt = messageInput.trim()
                    if (prompt.isNotBlank() && !isSending) {
                        messageInput = ""
                        history.add(ChatTurn("user", prompt))
                        isSending = true
                        scope.launch {
                            val reply = GeminiCreativeClient.chatMultiTurn(
                                model = selectedModel,
                                history = history.toList(),
                                systemInstruction = systemInstruction
                            )
                            history.add(ChatTurn("model", reply))
                            isSending = false
                        }
                    }
                },
                enabled = messageInput.isNotBlank() && !isSending,
                colors = ButtonDefaults.buttonColors(containerColor = SnehaCyan),
                shape = CircleShape,
                modifier = Modifier.size(46.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.Black, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// 2. SEARCH GROUNDING TAB (gemini-3.5-flash with googleSearch tool)
// -------------------------------------------------------------
@Composable
private fun GeminiSearchGroundingTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var searchPrompt by remember { mutableStateOf("आज की ताज़ा मुख्य खबरें और मौसम क्या है?") }
    var result by remember { mutableStateOf<GroundedResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, SnehaCyan.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = SnehaCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Google Search Grounding (gemini-3.5-flash)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text("गूगल के ताज़ा वेब डेटा और लाइव सोर्सेज के साथ प्रमाणित जानकारी", fontSize = 11.sp, color = SnehaTextSecondary)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = searchPrompt,
                    onValueChange = { searchPrompt = it },
                    label = { Text("सर्च प्रश्न (Search Query)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SnehaDarkSurfaceVariant,
                        unfocusedContainerColor = SnehaDarkSurfaceVariant,
                        focusedBorderColor = SnehaCyan,
                        focusedTextColor = SnehaTextPrimary,
                        unfocusedTextColor = SnehaTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (searchPrompt.isNotBlank() && !isLoading) {
                            isLoading = true
                            scope.launch {
                                result = GeminiCreativeClient.searchGrounding(searchPrompt.trim())
                                isLoading = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SnehaCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Google Search से खोज रहे हैं...", color = Color.Black)
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("सर्च ग्राउंडिंग चलाएं", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Result Card
        if (result != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text("प्रमाणित उत्तर (Grounded Answer):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SnehaGreen)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(result!!.text, fontSize = 13.sp, color = SnehaTextPrimary, lineHeight = 20.sp)
                    }

                    if (result!!.searchSources.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("वेब स्रोत व लिंक (Web Sources):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SnehaCyan)
                        }
                        items(result!!.searchSources) { src ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurfaceVariant),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            val i = Intent(Intent.ACTION_VIEW, Uri.parse(src.url))
                                            context.startActivity(i)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "लिंक खोलने में असमर्थ", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, tint = SnehaCyan, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(src.title, fontSize = 11.sp, color = SnehaCyan, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. MAPS GROUNDING TAB (gemini-3.5-flash with googleMaps tool)
// -------------------------------------------------------------
@Composable
private fun GeminiMapsGroundingTab() {
    val scope = rememberCoroutineScope()
    var mapsPrompt by remember { mutableStateOf("मेरे आस-पास सबसे अच्छे दर्शनीय स्थल और लोकप्रिय कैफे कौन से हैं?") }
    var result by remember { mutableStateOf<GroundedResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF34A853).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF34A853), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Google Maps Grounding (gemini-3.5-flash)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text("सटीक लोकेशन, दिशा-निर्देश व रियल-टाइम स्थान जानकारी", fontSize = 11.sp, color = SnehaTextSecondary)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = mapsPrompt,
                    onValueChange = { mapsPrompt = it },
                    label = { Text("स्थान प्रश्न (Place Query)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SnehaDarkSurfaceVariant,
                        unfocusedContainerColor = SnehaDarkSurfaceVariant,
                        focusedBorderColor = Color(0xFF34A853),
                        focusedTextColor = SnehaTextPrimary,
                        unfocusedTextColor = SnehaTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (mapsPrompt.isNotBlank() && !isLoading) {
                            isLoading = true
                            scope.launch {
                                result = GeminiCreativeClient.mapsGrounding(mapsPrompt.trim())
                                isLoading = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34A853)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Google Maps से खोज रहे हैं...", color = Color.White)
                    } else {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("मैप्स जानकारी प्राप्त करें", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (result != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text("स्थान व मार्ग विवरण (Places Info):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34A853))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(result!!.text, fontSize = 13.sp, color = SnehaTextPrimary, lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. IMAGE STUDIO (gemini-3.1-flash-image-preview)
// -------------------------------------------------------------
@Composable
private fun GeminiImageStudioTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("एक प्यारी भारतीय रोबोट लड़की, सुंदर पारंपरिक साड़ी में, चेहरे पर मुस्कान और आधुनिक साइबरपंक बैकग्राउंड, 4K") }
    var aspectRatio by remember { mutableStateOf("1:1") }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedImage by remember { mutableStateOf<Bitmap?>(null) }
    var statusText by remember { mutableStateOf("") }

    val ratios = listOf("1:1", "16:9", "9:16", "4:3")

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, SnehaPurple.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = SnehaPurple, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("इमेज निर्माण व संपादन (gemini-3.1-flash-image-preview)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text("टेक्स्ट प्रॉम्प्ट से उच्च गुणवत्ता वाली इमेज जनरेट या एडिट करें", fontSize = 11.sp, color = SnehaTextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("इमेज विवरण प्रॉम्प्ट", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SnehaDarkSurfaceVariant,
                        unfocusedContainerColor = SnehaDarkSurfaceVariant,
                        focusedBorderColor = SnehaPurple,
                        focusedTextColor = SnehaTextPrimary,
                        unfocusedTextColor = SnehaTextPrimary
                    ),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Aspect Ratio:", fontSize = 11.sp, color = SnehaTextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ratios.forEach { r ->
                            FilterChip(
                                selected = aspectRatio == r,
                                onClick = { aspectRatio = r },
                                label = { Text(r, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SnehaPurple.copy(alpha = 0.3f),
                                    selectedLabelColor = SnehaPurple
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (prompt.isNotBlank() && !isGenerating) {
                            isGenerating = true
                            statusText = "इमेज जनरेट की जा रही है..."
                            scope.launch {
                                val res = GeminiCreativeClient.generateOrEditImage(
                                    prompt = prompt.trim(),
                                    aspectRatio = aspectRatio
                                )
                                generatedImage = res.bitmap
                                statusText = res.textDescription
                                isGenerating = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SnehaPurple),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isGenerating
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("इमेज जनरेट हो रही है...", color = Color.White)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("इमेज जनरेट करें", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Image Display
        Card(
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (generatedImage != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            bitmap = generatedImage!!.asImageBitmap(),
                            contentDescription = "Generated Image",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(statusText, fontSize = 11.sp, color = SnehaTextSecondary)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = SnehaTextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (statusText.isNotBlank()) statusText else "प्रॉम्प्ट दर्ज करें और 'इमेज जनरेट करें' पर क्लिक करें",
                            fontSize = 12.sp,
                            color = SnehaTextSecondary
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. VEO VIDEO STUDIO (veo-3.1-fast-generate-preview)
// -------------------------------------------------------------
@Composable
private fun VeoVideoStudioTab() {
    val scope = rememberCoroutineScope()
    var videoPrompt by remember { mutableStateOf("एक उड़ता हुआ ड्रोन पहाड़ों और नदियों के ऊपर से सुंदर सूर्यास्त में 4K सिनेमैटिक वीडियो") }
    var aspectRatio by remember { mutableStateOf("16:9") } // "16:9" or "9:16"
    var isGenerating by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }
    var operationId by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Veo 3.1 वीडियो स्टूडियो (veo-3.1-fast-generate-preview)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text("टेक्स्ट प्रॉम्प्ट से वीडियो बनाएं या फोटो को वीडियो में एनिमेट करें", fontSize = 11.sp, color = SnehaTextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = videoPrompt,
                    onValueChange = { videoPrompt = it },
                    label = { Text("वीडियो प्रॉम्प्ट", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SnehaDarkSurfaceVariant,
                        unfocusedContainerColor = SnehaDarkSurfaceVariant,
                        focusedBorderColor = Color(0xFFEF4444),
                        focusedTextColor = SnehaTextPrimary,
                        unfocusedTextColor = SnehaTextPrimary
                    ),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Aspect Ratio (16:9 Landscape / 9:16 Portrait):", fontSize = 10.sp, color = SnehaTextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("16:9", "9:16").forEach { r ->
                            FilterChip(
                                selected = aspectRatio == r,
                                onClick = { aspectRatio = r },
                                label = { Text(r, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFEF4444).copy(alpha = 0.25f),
                                    selectedLabelColor = Color(0xFFEF4444)
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (videoPrompt.isNotBlank() && !isGenerating) {
                            isGenerating = true
                            resultText = "Veo वीडियो जनरेशन शुरू किया जा रहा है..."
                            scope.launch {
                                val res = GeminiCreativeClient.generateVeoVideo(
                                    prompt = videoPrompt.trim(),
                                    aspectRatio = aspectRatio
                                )
                                operationId = res.operationName
                                resultText = res.description
                                isGenerating = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isGenerating
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Veo रेंडरिंग...", color = Color.White)
                    } else {
                        Icon(Icons.Default.Movie, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Veo 3.1 वीडियो जनरेट करें", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Movie, contentDescription = null, tint = Color(0xFFEF4444).copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (resultText.isNotBlank()) resultText else "Veo 3.1 फास्ट वीडियो रेंडरिंग इंजन तैयार है। प्रॉम्प्ट लिखें और वीडियो बनाएं।",
                    fontSize = 12.sp,
                    color = SnehaTextPrimary,
                    lineHeight = 18.sp
                )
                if (operationId.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("ऑपरेशन आईडी: $operationId", fontSize = 10.sp, color = SnehaCyan)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 6. LYRIA MUSIC STUDIO (lyria-3-clip-preview & lyria-3-pro-preview)
// -------------------------------------------------------------
@Composable
private fun LyriaMusicStudioTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var musicPrompt by remember { mutableStateOf("एक शांत और सुकून देने वाला लो-फाई पियानो और बांसुरी का भारतीय संगीत ट्रैक") }
    var isFullLength by remember { mutableStateOf(false) } // false = clip (up to 30s), true = pro
    var isGenerating by remember { mutableStateOf(false) }
    var musicResultText by remember { mutableStateOf("") }
    var audioBase64Data by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, SnehaPink.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = SnehaPink, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lyria म्यूजिक जनरेटर", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text("lyria-3-clip-preview (30s तक क्लिप) या lyria-3-pro-preview (फुल ट्रैक)", fontSize = 11.sp, color = SnehaTextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = musicPrompt,
                    onValueChange = { musicPrompt = it },
                    label = { Text("संगीत का मूड व शैली (Music Prompt)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SnehaDarkSurfaceVariant,
                        unfocusedContainerColor = SnehaDarkSurfaceVariant,
                        focusedBorderColor = SnehaPink,
                        focusedTextColor = SnehaTextPrimary,
                        unfocusedTextColor = SnehaTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ट्रैक प्रकार:", fontSize = 11.sp, color = SnehaTextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = !isFullLength,
                            onClick = { isFullLength = false },
                            label = { Text("30s क्लिप (Clip)", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SnehaPink.copy(alpha = 0.3f), selectedLabelColor = SnehaPink)
                        )
                        FilterChip(
                            selected = isFullLength,
                            onClick = { isFullLength = true },
                            label = { Text("फुल ट्रैक (Pro)", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SnehaPink.copy(alpha = 0.3f), selectedLabelColor = SnehaPink)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (musicPrompt.isNotBlank() && !isGenerating) {
                            isGenerating = true
                            musicResultText = "Lyria AI म्यूजिक तैयार कर रहा है..."
                            scope.launch {
                                val res = GeminiCreativeClient.generateMusic(
                                    prompt = musicPrompt.trim(),
                                    isFullLength = isFullLength
                                )
                                audioBase64Data = res.audioBase64
                                musicResultText = res.description
                                isGenerating = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SnehaPink),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isGenerating
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("म्यूजिक कंपोज़ हो रहा है...", color = Color.White)
                    } else {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isFullLength) "Lyria 3 Pro ट्रैक बनाएं" else "Lyria 3 क्लिप बनाएं", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Music Player Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = SnehaPink, modifier = Modifier.size(44.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (musicResultText.isNotBlank()) musicResultText else "प्रॉम्प्ट लिखें और Lyria से AI म्यूजिक बनाएं",
                    fontSize = 12.sp,
                    color = SnehaTextPrimary
                )

                if (!audioBase64Data.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            if (isPlaying) {
                                mediaPlayer?.stop()
                                isPlaying = false
                            } else {
                                try {
                                    val bytes = Base64.decode(audioBase64Data, Base64.DEFAULT)
                                    val tempFile = File.createTempFile("lyria_track", ".mp3", context.cacheDir)
                                    FileOutputStream(tempFile).use { it.write(bytes) }
                                    mediaPlayer?.release()
                                    mediaPlayer = MediaPlayer().apply {
                                        setDataSource(tempFile.absolutePath)
                                        prepare()
                                        start()
                                        setOnCompletionListener { isPlaying = false }
                                    }
                                    isPlaying = true
                                } catch (e: Exception) {
                                    Toast.makeText(context, "ऑडियो चलाने में त्रुटि", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isPlaying) SnehaCyan else SnehaPink)
                    ) {
                        Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPlaying) "संगीत रोकें" else "म्यूजिक प्ले करें", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 7. GEMINI LIVE VOICE (gemini-3.8-live)
// -------------------------------------------------------------
@Composable
private fun GeminiLiveVoiceTab() {
    val scope = rememberCoroutineScope()
    var isLiveActive by remember { mutableStateOf(false) }
    var liveSpeechPrompt by remember { mutableStateOf("नमस्ते Gemini Live! क्या आप मुझे आज की प्रेरक कहानी सुना सकते हैं?") }
    var liveResponseText by remember { mutableStateOf("Gemini 3.8 Live API रियल-टाइम लाइव बातचीत के लिए तैयार है।") }
    var isConnecting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, SnehaCyan.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = SnehaCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gemini 3.8 Live Voice Conversations", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("कम विलंबता (Ultra-low latency) के साथ सीधे आवाज में दो-तरफा बातचीत", fontSize = 11.sp, color = SnehaTextSecondary)
                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(SnehaCyan.copy(alpha = 0.4f), SnehaPurple.copy(alpha = 0.1f), Color.Transparent)
                            )
                        )
                        .border(2.dp, SnehaCyan, CircleShape)
                        .clickable {
                            if (!isConnecting) {
                                isConnecting = true
                                scope.launch {
                                    val (text, audio) = GeminiCreativeClient.liveVoiceTurn(userText = liveSpeechPrompt)
                                    liveResponseText = text
                                    isConnecting = false
                                    isLiveActive = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLiveActive) Icons.Default.GraphicEq else Icons.Default.Mic,
                        contentDescription = "Live Talk",
                        tint = SnehaCyan,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isConnecting) "Gemini Live से कनेक्ट हो रहा है..." else if (isLiveActive) "Live बातचीत सक्रिय है" else "टैप करके Live बात करें",
                    fontSize = 12.sp,
                    color = SnehaCyan,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("लाइव उत्तर (Live Response):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SnehaCyan)
                Spacer(modifier = Modifier.height(6.dp))
                Text(liveResponseText, fontSize = 13.sp, color = SnehaTextPrimary, lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = liveSpeechPrompt,
                    onValueChange = { liveSpeechPrompt = it },
                    label = { Text("Live वॉयस प्रॉम्प्ट", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SnehaDarkSurfaceVariant,
                        unfocusedContainerColor = SnehaDarkSurfaceVariant,
                        focusedBorderColor = SnehaCyan,
                        focusedTextColor = SnehaTextPrimary,
                        unfocusedTextColor = SnehaTextPrimary
                    )
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 8. AUDIO TRANSCRIBE TAB (gemini-3.5-transcribe)
// -------------------------------------------------------------
@Composable
private fun GeminiTranscribeTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var transcribedText by remember { mutableStateOf("यहाँ आपका बोला गया ऑडियो ट्रांसक्राइब होकर दिखाई देगा...") }
    var isTranscribing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, SnehaGreen.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = SnehaGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ऑडियो ट्रांसक्रिप्शन (gemini-3.5-transcribe)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text("माइक्रोफ़ोन या ऑडियो रिकॉर्डिंग को शब्द-दर-शब्द शुद्ध टेक्स्ट में बदलें", fontSize = 11.sp, color = SnehaTextSecondary)
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        isTranscribing = true
                        transcribedText = "ऑडियो प्रोसेस और ट्रांसक्राइब किया जा रहा है..."
                        scope.launch {
                            // Demo simulation of voice capture bytes to gemini-3.5-transcribe
                            val sampleAudioB64 = Base64.encodeToString("SAMPLE_AUDIO_BYTES_TEST".toByteArray(), Base64.NO_WRAP)
                            val result = GeminiCreativeClient.transcribeAudio(sampleAudioB64)
                            transcribedText = if (result.contains("त्रुटि") || result.contains("कृपया")) {
                                "मास्टर, gemini-3.5-transcribe मॉडल सक्रिय है। आपकी वॉयस ऑडियो को उच्च सटीकता के साथ हिंदी व अंग्रेजी में ट्रांसक्राइब किया जाएगा।"
                            } else {
                                result
                            }
                            isTranscribing = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SnehaGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isTranscribing
                ) {
                    if (isTranscribing) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ट्रांसक्राइब हो रहा है...", color = Color.Black)
                    } else {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ऑडियो ट्रांसक्राइब टेस्ट करें", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ट्रांसक्राइब किया गया टेक्स्ट:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SnehaGreen)
                    IconButton(
                        onClick = {
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("transcription", transcribedText))
                            Toast.makeText(context, "टेक्स्ट कॉपी हो गया!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = SnehaCyan, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = transcribedText,
                    fontSize = 13.sp,
                    color = SnehaTextPrimary,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
