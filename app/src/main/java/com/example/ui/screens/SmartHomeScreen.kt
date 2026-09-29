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
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DeviceType
import com.example.engine.SmartDevice
import com.example.engine.SmartHomeManager
import com.example.ui.theme.SnehaBgDark
import com.example.ui.theme.SnehaCyan
import com.example.ui.theme.SnehaDarkSurface
import com.example.ui.theme.SnehaDarkSurfaceVariant
import com.example.ui.theme.SnehaEmerald
import com.example.ui.theme.SnehaPink
import com.example.ui.theme.SnehaPurple
import com.example.ui.theme.SnehaTextPrimary
import com.example.ui.theme.SnehaTextSecondary

@Composable
fun SmartHomeScreen(
    onBack: () -> Unit,
    onVoiceCommand: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val devices by SmartHomeManager.devices.collectAsState()
    var selectedRoom by remember { mutableStateOf("सभी उपकरण") }

    LaunchedEffect(Unit) {
        SmartHomeManager.init(context)
    }

    val rooms = listOf("सभी उपकरण", "लिविंग रूम", "बेडरूम", "बालकनी", "किचन व बाथ")
    val filteredDevices = if (selectedRoom == "सभी उपकरण") {
        devices
    } else {
        devices.filter { it.room == selectedRoom }
    }

    val activeCount = devices.count { it.isOn }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SnehaBgDark)
    ) {
        // Top App Bar
        Surface(
            color = SnehaDarkSurface,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("smart_home_back_btn")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "वापस",
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "IoT स्मार्ट होम हब",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = SnehaEmerald.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "$activeCount सक्रिय",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SnehaEmerald,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "आवाज व टच से घर की लाइट्स, पंखे, एसी और टीवी नियंत्रित करें",
                        fontSize = 11.sp,
                        color = SnehaTextSecondary
                    )
                }

                // All Off Button
                IconButton(
                    onClick = {
                        SmartHomeManager.allOff(context)
                        Toast.makeText(context, "घर के सभी उपकरण बंद कर दिए गए", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("btn_all_off")
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "सब बंद करें",
                        tint = SnehaPink
                    )
                }
            }
        }

        // Room Filter Tabs
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(rooms) { room ->
                val isSelected = selectedRoom == room
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedRoom = room },
                    label = { Text(room, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SnehaCyan.copy(alpha = 0.25f),
                        selectedLabelColor = SnehaCyan,
                        containerColor = SnehaDarkSurfaceVariant,
                        labelColor = SnehaTextSecondary
                    )
                )
            }
        }

        // Quick Voice Prompts Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Mic, contentDescription = null, tint = SnehaCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("वॉयस सुझाव: 'लाइट जलाओ' • 'पंखा 4 पर करो' • 'एसी 24 डिग्री' • 'सब बंद करो'", fontSize = 10.sp, color = SnehaCyan)
        }

        // Devices List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredDevices, key = { it.id }) { device ->
                SmartDeviceCard(
                    device = device,
                    onToggle = { SmartHomeManager.toggleDevice(context, device.id) },
                    onValueChange = { newVal -> SmartHomeManager.setDeviceValue(context, device.id, newVal) },
                    onColorChange = { color -> SmartHomeManager.setDeviceColor(context, device.id, color) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SmartDeviceCard(
    device: SmartDevice,
    onToggle: () -> Unit,
    onValueChange: (Int) -> Unit,
    onColorChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("device_card_${device.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
        border = BorderStroke(1.2.dp, if (device.isOn) SnehaCyan.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (device.isOn) SnehaCyan.copy(alpha = 0.2f) else SnehaDarkSurfaceVariant,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(device.type.iconEmoji, fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = device.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnehaTextPrimary
                        )
                        Text(
                            text = "${device.room} • ${if (device.isOn) "सक्रिय (ON)" else "बंद (OFF)"}",
                            fontSize = 11.sp,
                            color = if (device.isOn) SnehaEmerald else SnehaTextSecondary
                        )
                    }
                }

                Switch(
                    checked = device.isOn,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = SnehaCyan,
                        uncheckedThumbColor = SnehaTextSecondary,
                        uncheckedTrackColor = SnehaDarkSurfaceVariant
                    ),
                    modifier = Modifier.testTag("switch_${device.id}")
                )
            }

            AnimatedVisibility(visible = device.isOn) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    when (device.type) {
                        DeviceType.LIGHT -> {
                            // Brightness slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("चमक (Brightness)", fontSize = 11.sp, color = SnehaTextSecondary)
                                Text("${device.value}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SnehaCyan)
                            }
                            Slider(
                                value = device.value.toFloat(),
                                onValueChange = { onValueChange(it.toInt()) },
                                valueRange = 10f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = SnehaCyan,
                                    activeTrackColor = SnehaCyan,
                                    inactiveTrackColor = SnehaDarkSurfaceVariant
                                )
                            )

                            // Light Color chips
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("लाइट का रंग (Color Mode):", fontSize = 10.sp, color = SnehaTextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            val colors = listOf("Warm White", "Cyber Cyan", "Sunset Gold", "Rose Pink")
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                colors.forEach { col ->
                                    val isSelected = device.colorName == col
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onColorChange(col) },
                                        label = { Text(col, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SnehaPurple.copy(alpha = 0.3f),
                                            selectedLabelColor = SnehaPurple
                                        )
                                    )
                                }
                            }
                        }

                        DeviceType.FAN -> {
                            // Fan speed selector 1 to 5
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("पंखा स्पीड (Fan Speed):", fontSize = 11.sp, color = SnehaTextSecondary)
                                Text("स्पीड ${device.value}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SnehaCyan)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                (1..5).forEach { speed ->
                                    val isSel = device.value == speed
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) SnehaCyan else SnehaDarkSurfaceVariant)
                                            .clickable { onValueChange(speed) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$speed",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSel) Color.Black else SnehaTextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        DeviceType.AC -> {
                            // AC Temperature adjust
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("तापमान (Target Temp):", fontSize = 11.sp, color = SnehaTextSecondary)
                                Text("${device.value}°C", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SnehaCyan)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = { if (device.value > 16) onValueChange(device.value - 1) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(SnehaDarkSurfaceVariant, CircleShape)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "कम करें", tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(20.dp))
                                Text(
                                    text = "${device.value}°C",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(20.dp))
                                IconButton(
                                    onClick = { if (device.value < 30) onValueChange(device.value + 1) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(SnehaDarkSurfaceVariant, CircleShape)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "बढ़ाएं", tint = Color.White)
                                }
                            }
                        }

                        DeviceType.TV -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("टीवी वॉल्यूम (Volume):", fontSize = 11.sp, color = SnehaTextSecondary)
                                Text("${device.value}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SnehaCyan)
                            }
                            Slider(
                                value = device.value.toFloat(),
                                onValueChange = { onValueChange(it.toInt()) },
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = SnehaCyan,
                                    activeTrackColor = SnehaCyan
                                )
                            )
                        }

                        DeviceType.PLUG -> {
                            Text(
                                text = "ऑटो शट-ऑफ टाइमर: ${device.value} मिनट में गीजर स्वतः बंद हो जाएगा।",
                                fontSize = 11.sp,
                                color = SnehaTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
