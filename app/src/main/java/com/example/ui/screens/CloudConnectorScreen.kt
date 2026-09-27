package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.AiCloudConnectorManager
import com.example.engine.AiProvider
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
fun CloudConnectorScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activeProvider by AiCloudConnectorManager.activeProvider.collectAsState()
    val testStatus by AiCloudConnectorManager.connectionStatus.collectAsState()

    var apiKeyInput by remember { mutableStateOf("") }
    var modelInput by remember { mutableStateOf("") }
    var endpointInput by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }

    var testPromptInput by remember { mutableStateOf("नमस्ते, आप कौन हैं और आपकी क्षमताएं क्या हैं?") }
    var testPromptResult by remember { mutableStateOf("") }
    var isRunningTestPrompt by remember { mutableStateOf(false) }

    // Sync input when active provider changes
    LaunchedEffect(activeProvider) {
        apiKeyInput = AiCloudConnectorManager.getApiKey(context, activeProvider)
        modelInput = AiCloudConnectorManager.getCustomModel(context, activeProvider)
        endpointInput = AiCloudConnectorManager.getCustomEndpoint(context)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SnehaBgDark)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SnehaDarkCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SnehaCyan.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SnehaCyan.copy(alpha = 0.2f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = SnehaCyan, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "मल्टी-मॉडल AI क्लाउड कनेक्टर",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SnehaTextPrimary
                            )
                            Text(
                                text = "ChatGPT, Gemini, Claude या अपने सर्वर को सीधे कनेक्ट करें",
                                fontSize = 11.sp,
                                color = SnehaCyan
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "स्नेहा AI आपके पसंदीदा मॉडल के दिमाग का इस्तेमाल करके बातचीत, ऑटो-रिप्लाई और कॉल ट्रांसलेशन का जवाब देगी।",
                        fontSize = 11.sp,
                        color = SnehaTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Active Provider Selector
        item {
            Text("AI इंजन चुनें (Active AI Brain):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SnehaTextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AiProvider.values().forEach { provider ->
                    val isSelected = activeProvider == provider
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) SnehaDarkSurfaceVariant else SnehaDarkSurface
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.2.dp,
                            if (isSelected) SnehaCyan else Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                AiCloudConnectorManager.setActiveProvider(context, provider)
                            }
                            .testTag("provider_card_${provider.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(provider.iconEmoji, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(provider.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SnehaTextPrimary)
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = SnehaCyan.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                "सक्रिय (Active)",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SnehaCyan,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(provider.description, fontSize = 11.sp, color = SnehaTextSecondary)
                            }

                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SnehaCyan, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }

        // Configuration Card for Active Provider
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${activeProvider.displayName} सेटिंग्स",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SnehaCyan
                    )

                    // API Key Field (if not local custom)
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            AiCloudConnectorManager.saveApiKey(context, activeProvider, it)
                        },
                        label = { Text("${activeProvider.displayName} API Key") },
                        placeholder = { Text("उदा. sk-proj-...") },
                        singleLine = true,
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Icon(
                                    imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "की दिखाएं/छिपाएं",
                                    tint = SnehaTextSecondary
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SnehaCyan,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedTextColor = SnehaTextPrimary,
                            unfocusedTextColor = SnehaTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_api_key")
                    )

                    // Model Name Field
                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = {
                            modelInput = it
                            AiCloudConnectorManager.saveCustomModel(context, activeProvider, it)
                        },
                        label = { Text("मॉडल नाम (Model ID)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SnehaCyan,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedTextColor = SnehaTextPrimary,
                            unfocusedTextColor = SnehaTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Custom Endpoint for Local / Ollama
                    if (activeProvider == AiProvider.CUSTOM) {
                        OutlinedTextField(
                            value = endpointInput,
                            onValueChange = {
                                endpointInput = it
                                AiCloudConnectorManager.saveCustomEndpoint(context, it)
                            },
                            label = { Text("कस्टम API URL / Webhook") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SnehaCyan,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                                focusedTextColor = SnehaTextPrimary,
                                unfocusedTextColor = SnehaTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Test Connection Button
                    Button(
                        onClick = {
                            isTestingConnection = true
                            scope.launch {
                                AiCloudConnectorManager.testConnection(context, activeProvider)
                                isTestingConnection = false
                            }
                        },
                        enabled = !isTestingConnection,
                        colors = ButtonDefaults.buttonColors(containerColor = SnehaCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_test_connection")
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("कनेक्शन जांचा जा रहा है...", color = Color.Black, fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("कनेक्शन टेस्ट करें (Test Connection)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Test Result Display
                    testStatus?.let { status ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (status.success) Color(0xFF00E676).copy(alpha = 0.15f) else SnehaPink.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, if (status.success) Color(0xFF00E676) else SnehaPink),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = status.message,
                                fontSize = 11.sp,
                                color = if (status.success) Color(0xFF00E676) else SnehaPink,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Test Playground
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🚀 लाइव AI टेस्ट प्लेग्राउंड (${activeProvider.displayName})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SnehaTextPrimary
                    )

                    OutlinedTextField(
                        value = testPromptInput,
                        onValueChange = { testPromptInput = it },
                        label = { Text("टेस्ट प्रॉम्प्ट पूछें") },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SnehaCyan,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedTextColor = SnehaTextPrimary,
                            unfocusedTextColor = SnehaTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (testPromptInput.isNotBlank() && !isRunningTestPrompt) {
                                isRunningTestPrompt = true
                                testPromptResult = ""
                                scope.launch {
                                    val res = AiCloudConnectorManager.generateResponse(context, testPromptInput)
                                    testPromptResult = if (res.isNotBlank()) res else "उत्तर प्राप्त नहीं हुआ। कृपया API Key और नेटवर्क जांचें।"
                                    isRunningTestPrompt = false
                                }
                            }
                        },
                        enabled = !isRunningTestPrompt && testPromptInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = SnehaDarkSurfaceVariant),
                        border = BorderStroke(1.dp, SnehaCyan.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth().testTag("btn_run_test_prompt")
                    ) {
                        if (isRunningTestPrompt) {
                            CircularProgressIndicator(color = SnehaCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI सोच रहा है...", color = SnehaCyan, fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null, tint = SnehaCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("भेजें और AI उत्तर देखें", color = SnehaCyan, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }

                    if (testPromptResult.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SnehaDarkSurfaceVariant,
                            border = BorderStroke(1.dp, SnehaCyan.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("उत्तर (${activeProvider.displayName}):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SnehaCyan)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(testPromptResult, fontSize = 12.sp, color = SnehaTextPrimary, lineHeight = 17.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
