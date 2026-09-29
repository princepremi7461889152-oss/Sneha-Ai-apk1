package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.example.engine.AntiTheftManager
import com.example.engine.PhoneControlManager
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InstalledApp
import com.example.ui.theme.SnehaAmber
import com.example.ui.theme.SnehaCyan
import com.example.ui.theme.SnehaDarkSurface
import com.example.ui.theme.SnehaDarkSurfaceVariant
import com.example.ui.theme.SnehaEmerald
import com.example.ui.theme.SnehaPink
import com.example.ui.theme.SnehaPurple
import com.example.ui.theme.SnehaTextPrimary
import com.example.ui.theme.SnehaTextSecondary

@Composable
fun PhoneControlScreen(
    installedApps: List<InstalledApp>,
    isTorchActive: Boolean,
    batteryInfo: String,
    onToggleTorch: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onVolumeMute: () -> Unit,
    onLaunchApp: (String) -> Unit,
    onSearchInsideApp: (appName: String, query: String) -> Unit,
    onSearchYouTube: (String) -> Unit,
    onSearchWeb: (String) -> Unit,
    onOpenDialer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWifi: () -> Unit,
    onOpenCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isBluetoothOn by PhoneControlManager.isBluetoothActive.collectAsState()
    val isHotspotOn by PhoneControlManager.isHotspotActive.collectAsState()
    val isWifiOn by PhoneControlManager.isWifiActive.collectAsState()
    val isAlarmActive by AntiTheftManager.isAlarmActive.collectAsState()

    var appSearchQuery by remember { mutableStateOf("") }
    var inAppSearchText by remember { mutableStateOf("") }
    var selectedTargetApp by remember { mutableStateOf("YouTube") }

    val filteredApps = remember(installedApps, appSearchQuery) {
        if (appSearchQuery.isBlank()) installedApps
        else installedApps.filter { it.name.contains(appSearchQuery, ignoreCase = true) }
    }

    val appSearchTargets = listOf(
        "YouTube", "WhatsApp", "Maps", "Spotify", "Play Store", "Google"
    )

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
                text = "फोन नियंत्रण & हार्डवेयर कंट्रोल 📱",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SnehaTextPrimary
            )
            Text(
                text = "ब्लूटूथ, हॉटस्पॉट, वाईफाई, टॉर्च, सायरन व ऐप्स को आवाज या स्क्रीन से नियंत्रित करें",
                fontSize = 13.sp,
                color = SnehaTextSecondary
            )
        }

        // Quick System Controls Grid
        item {
            Text(
                text = "हार्डवेयर & कनेक्टिविटी नियंत्रण",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = SnehaCyan
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Row 1: Bluetooth, Hotspot, Wi-Fi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ControlTile(
                        title = if (isBluetoothOn) "ब्लूटूथ: ON" else "ब्लूटूथ: OFF",
                        icon = Icons.Default.Bluetooth,
                        active = isBluetoothOn,
                        activeColor = Color(0xFF2979FF),
                        onClick = { PhoneControlManager.toggleBluetooth(context, !isBluetoothOn) },
                        modifier = Modifier.weight(1f).testTag("control_tile_bluetooth")
                    )

                    ControlTile(
                        title = if (isHotspotOn) "हॉटस्पॉट: ON" else "हॉटस्पॉट: OFF",
                        icon = Icons.Default.WifiTethering,
                        active = isHotspotOn,
                        activeColor = Color(0xFFFF9100),
                        onClick = { PhoneControlManager.toggleHotspot(context, !isHotspotOn) },
                        modifier = Modifier.weight(1f).testTag("control_tile_hotspot")
                    )

                    ControlTile(
                        title = if (isWifiOn) "वाईफाई: ON" else "वाईफाई: OFF",
                        icon = Icons.Default.Wifi,
                        active = isWifiOn,
                        activeColor = SnehaEmerald,
                        onClick = { PhoneControlManager.toggleWifi(context, !isWifiOn) },
                        modifier = Modifier.weight(1f).testTag("control_tile_wifi")
                    )
                }

                // Row 2: Torch, Emergency Siren, Camera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ControlTile(
                        title = if (isTorchActive) "टॉर्च: ON" else "टॉर्च: OFF",
                        icon = if (isTorchActive) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                        active = isTorchActive,
                        activeColor = SnehaAmber,
                        onClick = onToggleTorch,
                        modifier = Modifier.weight(1f).testTag("control_tile_torch")
                    )

                    ControlTile(
                        title = if (isAlarmActive) "सायरन: बज रहा" else "सायरन टेस्ट",
                        icon = Icons.Default.NotificationsActive,
                        active = isAlarmActive,
                        activeColor = SnehaPink,
                        onClick = {
                            if (isAlarmActive) {
                                AntiTheftManager.stopAlarm(context)
                            } else {
                                AntiTheftManager.triggerTheftAlarm(context, "मास्टर! इमरजेंसी सायरन टेस्ट चालू है!")
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("control_tile_siren")
                    )

                    ControlTile(
                        title = "कैमरा",
                        icon = Icons.Default.CameraAlt,
                        active = false,
                        activeColor = SnehaPurple,
                        onClick = onOpenCamera,
                        modifier = Modifier.weight(1f).testTag("control_tile_camera")
                    )
                }

                // Row 3: Volume Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ControlTile(
                        title = "वॉल्यूम बढ़ाएं",
                        icon = Icons.Default.VolumeUp,
                        active = false,
                        activeColor = SnehaCyan,
                        onClick = onVolumeUp,
                        modifier = Modifier.weight(1f).testTag("control_tile_volume_up")
                    )

                    ControlTile(
                        title = "वॉल्यूम घटाएं",
                        icon = Icons.Default.VolumeDown,
                        active = false,
                        activeColor = SnehaCyan,
                        onClick = onVolumeDown,
                        modifier = Modifier.weight(1f).testTag("control_tile_volume_down")
                    )

                    ControlTile(
                        title = "म्यूट करें",
                        icon = Icons.Default.VolumeMute,
                        active = false,
                        activeColor = SnehaPink,
                        onClick = onVolumeMute,
                        modifier = Modifier.weight(1f).testTag("control_tile_mute")
                    )
                }
            }
        }

        // Voice Hardware Control Helper Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SnehaCyan.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🗣️ बोलकर नियंत्रित करें (Voice Commands):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnehaCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• \"ब्लूटूथ ऑन करो\" / \"ब्लूटूथ बंद करो\"\n• \"हॉटस्पॉट ऑन करो\" / \"हॉटस्पॉट बंद करो\"\n• \"वाईफाई ऑन करो\" / \"वाईफाई बंद करो\"\n• \"टॉर्च जलाओ\" / \"टॉर्च बंद करो\"\n• \"सायरन बजाओ\" / \"सायरन बंद करो\"\n• \"कोई और फोन ले तो सायरन बजाओ\"",
                        fontSize = 12.sp,
                        color = SnehaTextPrimary,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Battery Status Banner
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
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(SnehaEmerald.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = "Battery",
                            tint = SnehaEmerald
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "बैटरी स्थिति",
                            fontSize = 12.sp,
                            color = SnehaTextSecondary
                        )
                        Text(
                            text = batteryInfo,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = SnehaTextPrimary
                        )
                    }
                    Button(
                        onClick = onOpenDialer,
                        colors = ButtonDefaults.buttonColors(containerColor = SnehaDarkSurfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "डायलर",
                            tint = SnehaCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("डायलर", fontSize = 12.sp, color = SnehaCyan)
                    }
                }
            }
        }

        // Dedicated IN-APP SEARCH Section
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
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = SnehaCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ऐप्स के अंदर सर्च करें (In-App Search) 🔍",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SnehaCyan
                        )
                    }
                    Text(
                        text = "टारगेट ऐप चुनें और सर्च क्वेरी डालें, स्नेहा सीधे ऐप खोलकर सर्च करेगी:",
                        fontSize = 12.sp,
                        color = SnehaTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // App target selection chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(appSearchTargets) { target ->
                            val isSelected = selectedTargetApp == target
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTargetApp = target },
                                label = { Text(target, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SnehaCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = SnehaCyan
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = inAppSearchText,
                        onValueChange = { inAppSearchText = it },
                        placeholder = { Text("$selectedTargetApp में क्या सर्च करना है?", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("in_app_search_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SnehaDarkSurfaceVariant,
                            unfocusedContainerColor = SnehaDarkSurfaceVariant,
                            focusedBorderColor = SnehaCyan,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = SnehaTextPrimary,
                            unfocusedTextColor = SnehaTextPrimary
                        ),
                        singleLine = true,
                        trailingIcon = {
                            if (inAppSearchText.isNotBlank()) {
                                Button(
                                    onClick = {
                                        onSearchInsideApp(selectedTargetApp, inAppSearchText)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SnehaCyan),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.padding(end = 4.dp).testTag("btn_trigger_in_app_search")
                                ) {
                                    Text("सर्च करें", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Search Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (inAppSearchText.isNotBlank()) onSearchInsideApp("WhatsApp", inAppSearchText)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("btn_quick_whatsapp_search")
                        ) {
                            Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                if (inAppSearchText.isNotBlank()) onSearchInsideApp("Maps", inAppSearchText)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("btn_quick_maps_search")
                        ) {
                            Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Maps", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                if (inAppSearchText.isNotBlank()) onSearchInsideApp("Spotify", inAppSearchText)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("btn_quick_spotify_search")
                        ) {
                            Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Spotify", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Installed Apps Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "डिवाइस के सभी ऐप्स (${filteredApps.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SnehaCyan
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = SnehaTextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = appSearchQuery,
                onValueChange = { appSearchQuery = it },
                placeholder = { Text("ऐप का नाम खोजें (जैसे WhatsApp, Camera)...", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_filter_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SnehaDarkSurfaceVariant,
                    unfocusedContainerColor = SnehaDarkSurfaceVariant,
                    focusedBorderColor = SnehaCyan,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = SnehaTextPrimary,
                    unfocusedTextColor = SnehaTextPrimary
                ),
                singleLine = true
            )
        }

        items(filteredApps) { app ->
            AppItemRow(
                app = app,
                onLaunch = { onLaunchApp(app.packageName) },
                onSearchInThisApp = {
                    selectedTargetApp = app.name
                }
            )
        }
    }
}

@Composable
private fun ControlTile(
    title: String,
    icon: ImageVector,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (active) activeColor.copy(alpha = 0.25f) else SnehaDarkSurface,
        border = BorderStroke(1.dp, if (active) activeColor else Color(0xFF2C3252)),
        modifier = modifier.height(72.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (active) activeColor else SnehaCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = SnehaTextPrimary
            )
        }
    }
}

@Composable
private fun AppItemRow(
    app: InstalledApp,
    onLaunch: () -> Unit,
    onSearchInThisApp: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLaunch),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
        border = BorderStroke(1.dp, Color(0xFF242A44))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(listOf(SnehaDarkSurfaceVariant, Color(0xFF2D3748)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.name.take(1).uppercase(),
                    color = SnehaCyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SnehaTextPrimary
                )
                Text(
                    text = app.packageName,
                    fontSize = 11.sp,
                    color = SnehaTextSecondary,
                    maxLines = 1
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onSearchInThisApp,
                    colors = ButtonDefaults.buttonColors(containerColor = SnehaDarkSurfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "सर्च", tint = SnehaCyan, modifier = Modifier.size(14.dp))
                }

                Button(
                    onClick = onLaunch,
                    colors = ButtonDefaults.buttonColors(containerColor = SnehaCyan.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("launch_app_${app.packageName}")
                ) {
                    Text(
                        text = "खोलो",
                        fontSize = 12.sp,
                        color = SnehaCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
