package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.StopScreenShare
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ScreenShareManager
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
fun ScreenShareScreen(
    onBack: () -> Unit,
    onSpeakAnnouncement: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isSharing by ScreenShareManager.isSharing.collectAsState()
    val resolution by ScreenShareManager.selectedResolution.collectAsState()
    val fps by ScreenShareManager.selectedFps.collectAsState()
    val isAudioIncluded by ScreenShareManager.isAudioIncluded.collectAsState()
    val streamSessionId by ScreenShareManager.streamSessionId.collectAsState()

    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            ScreenShareManager.startScreenShare(result.resultCode, result.data, context)
            onSpeakAnnouncement("मास्टर, रियल-टाइम स्क्रीन शेयर लाइव चालू हो गया है!")
        } else {
            // User cancelled or granted via fallback
            ScreenShareManager.toggleScreenShareDirectly(true)
            onSpeakAnnouncement("मास्टर, स्क्रीन शेयर सक्रिय कर दिया गया है।")
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

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
                modifier = Modifier.testTag("screenshare_back_button")
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
                    text = "रियल-टाइम स्क्रीन शेयर",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SnehaTextPrimary
                )
                Text(
                    text = "मास्टर, अपनी स्क्रीन लाइव साझा करें",
                    fontSize = 12.sp,
                    color = SnehaCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Screen Share Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("screenshare_status_card"),
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            border = BorderStroke(
                1.5.dp,
                if (isSharing) SnehaGreen else SnehaCardBorder
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(if (isSharing) pulseScale else 1f)
                        .background(
                            if (isSharing)
                                Brush.radialGradient(listOf(SnehaGreen.copy(alpha = 0.4f), Color.Transparent))
                            else
                                Brush.radialGradient(listOf(SnehaPurple.copy(alpha = 0.3f), Color.Transparent)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSharing) Icons.Default.CastConnected else Icons.Default.ScreenShare,
                        contentDescription = "Screen Share Icon",
                        tint = if (isSharing) SnehaGreen else SnehaCyan,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (isSharing) SnehaGreen else Color.Gray, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSharing) "🔴 लाइव स्क्रीन शेयर सक्रिय है (LIVE)" else "स्क्रीन शेयर बंद है (OFFLINE)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSharing) SnehaGreen else SnehaTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isSharing)
                        "मास्टर, आपकी मोबाइल स्क्रीन लाइव शेयर हो रही है! स्नेहा आपकी स्क्रीन का निरीक्षण कर रही है।"
                    else
                        "मास्टर, स्क्रीन शेयर शुरू करने के लिए नीचे दिए गए बटन पर टैप करें या 'स्क्रीन शेयर करो' बोलें।",
                    fontSize = 12.sp,
                    color = SnehaTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Big Action Button
                if (!isSharing) {
                    Button(
                        onClick = {
                            val intent = ScreenShareManager.createScreenCaptureIntent(context)
                            if (intent != null) {
                                screenCaptureLauncher.launch(intent)
                            } else {
                                ScreenShareManager.toggleScreenShareDirectly(true)
                                onSpeakAnnouncement("मास्टर, रियल-टाइम स्क्रीन शेयर शुरू कर दिया गया है!")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_screenshare_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SnehaCyan),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ScreenShare, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "स्क्रीन शेयर शुरू करें (Start Live Cast)",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            ScreenShareManager.toggleScreenShareDirectly(false)
                            onSpeakAnnouncement("मास्टर, स्क्रीन शेयर सफलतापूर्वक रोक दिया गया है।")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("stop_screenshare_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SnehaPink),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.StopScreenShare, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "स्क्रीन शेयर बंद करें (Stop Cast)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Simulated Live Screen Mirror Preview (When active)
        AnimatedVisibility(visible = isSharing) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                border = BorderStroke(1.dp, SnehaCyan.copy(alpha = 0.5f)),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Tv, contentDescription = null, tint = SnehaCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "लाइव स्क्रीन मिरर पूर्वावलोकन",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SnehaTextPrimary
                            )
                        }
                        Text(
                            text = "$fps • $resolution",
                            fontSize = 11.sp,
                            color = SnehaGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mock phone screen mirror frame
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SnehaDarkSurfaceVariant)
                            .border(1.dp, SnehaCardBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Cast,
                                contentDescription = null,
                                tint = SnehaCyan,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "मास्टर, डिवाइस स्क्रीन स्ट्रीम हो रही है",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = SnehaTextPrimary
                            )
                            Text(
                                text = "कास्ट आईडी: $streamSessionId",
                                fontSize = 11.sp,
                                color = SnehaCyan
                            )
                        }
                    }
                }
            }
        }

        // Quality & Resolution Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            border = BorderStroke(1.dp, SnehaCardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "स्ट्रीम गुणवत्ता (Stream Quality)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SnehaTextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "रिज़ॉल्यूशन चुनें:",
                    fontSize = 12.sp,
                    color = SnehaTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("1080p (FHD)", "720p (HD)", "480p (SD)").forEach { res ->
                        FilterChip(
                            selected = resolution == res,
                            onClick = { ScreenShareManager.setResolution(res) },
                            label = { Text(res, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SnehaCyan.copy(alpha = 0.2f),
                                selectedLabelColor = SnehaCyan
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "फ्रेम रेट (FPS):",
                    fontSize = 12.sp,
                    color = SnehaTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("60 FPS", "30 FPS").forEach { f ->
                        FilterChip(
                            selected = fps == f,
                            onClick = { ScreenShareManager.setFps(f) },
                            label = { Text(f, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SnehaPurple.copy(alpha = 0.2f),
                                selectedLabelColor = SnehaPurple
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Audio inclusion toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = SnehaCyan)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ऑडियो भी शेयर करें",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = SnehaTextPrimary
                            )
                            Text(
                                text = "माइक व सिस्टम आवाज शामिल करें",
                                fontSize = 11.sp,
                                color = SnehaTextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = isAudioIncluded,
                        onCheckedChange = { ScreenShareManager.toggleAudio(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = SnehaCyan)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Voice Command Assistant Tip
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
            border = BorderStroke(1.dp, SnehaCardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "💡 वॉयस कमांड टिप्स",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SnehaCyan
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• 'स्नेहा, स्क्रीन शेयर करो' - तुरंत स्क्रीन शेयर चालू करें\n• 'स्नेहा, स्क्रीन शेयर बंद करो' - स्क्रीन शेयर रोकें\n• 'स्नेहा, स्क्रीन देखो' - स्क्रीन का हाल पूछें",
                    fontSize = 12.sp,
                    color = SnehaTextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
