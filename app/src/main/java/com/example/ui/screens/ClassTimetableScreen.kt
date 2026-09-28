package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.VoicePreferences
import com.example.engine.CallAssistantManager
import com.example.engine.ClassLecture
import com.example.engine.ClassTimetableManager
import com.example.ui.theme.SnehaBgDark
import com.example.ui.theme.SnehaCyan
import com.example.ui.theme.SnehaDarkCard
import com.example.ui.theme.SnehaDarkSurface
import com.example.ui.theme.SnehaDarkSurfaceVariant
import com.example.ui.theme.SnehaPink
import com.example.ui.theme.SnehaTextPrimary
import com.example.ui.theme.SnehaTextSecondary
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun ClassTimetableScreen(
    modifier: Modifier = Modifier,
    onSpeakAnnouncement: (String) -> Unit
) {
    val context = LocalContext.current
    var isModeEnabled by remember { mutableStateOf(VoicePreferences.isClassTimetableModeEnabled(context)) }
    val lectures by ClassTimetableManager.lectures.collectAsState()
    val isClassActive = ClassTimetableManager.isCurrentClassActive(context)
    val activeClass = ClassTimetableManager.getActiveClass(context)
    val nextClass = ClassTimetableManager.getNextLecture()

    val days = listOf(
        Pair(DayOfWeek.MONDAY, "सोमवार"),
        Pair(DayOfWeek.TUESDAY, "मंगलवार"),
        Pair(DayOfWeek.WEDNESDAY, "बुधवार"),
        Pair(DayOfWeek.THURSDAY, "गुरुवार"),
        Pair(DayOfWeek.FRIDAY, "शुक्रवार"),
        Pair(DayOfWeek.SATURDAY, "शनिवार")
    )

    val currentToday = LocalDate.now().dayOfWeek
    var selectedDay by remember {
        mutableStateOf(if (days.any { it.first == currentToday }) currentToday else DayOfWeek.MONDAY)
    }

    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SnehaBgDark)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master Toggle Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SnehaDarkCard),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(SnehaCyan.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "Class Mode",
                                tint = SnehaCyan,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "स्मार्ट क्लास टाइमटेबल मोड",
                                color = SnehaTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (isModeEnabled) "सक्रिय: क्लास में खुद फोन साइलेंट व ऑटो-रिप्लाई होगा" else "मोड बंद है",
                                color = if (isModeEnabled) Color(0xFF00E676) else Color.Red,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = isModeEnabled,
                            onCheckedChange = { checked ->
                                isModeEnabled = checked
                                VoicePreferences.saveClassTimetableModeEnabled(context, checked)
                                ClassTimetableManager.applyClassSilenceIfActive(context)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SnehaCyan,
                                checkedTrackColor = SnehaCyan.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.testTag("class_mode_switch")
                        )
                    }
                }
            }

            // Real-Time Active Lecture Status Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isClassActive && activeClass != null) Color(0xFF143026) else Color(0xFF1B2333)
                    ),
                    shape = RoundedCornerShape(16.dp),
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
                                    imageVector = if (isClassActive) Icons.Default.DoNotDisturbOn else Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (isClassActive) Color(0xFF00E676) else SnehaCyan
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isClassActive) "🎓 क्लास अभी चल रही है (DND सक्रिय)" else "⏳ अगली क्लास की तैयारी",
                                    color = if (isClassActive) Color(0xFF00E676) else SnehaCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isClassActive && activeClass != null) {
                            Text(
                                text = "${activeClass.subject} (${activeClass.startTime} - ${activeClass.endTime})",
                                color = SnehaTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "प्रोफेसर: ${activeClass.professor} • स्थान: ${activeClass.room}",
                                color = SnehaTextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "💡 कॉलर को जाने वाला ऑटो-संदेश: \"नमस्ते, मास्टर अभी ${activeClass.subject} की क्लास में हैं, ${activeClass.endTime} पर कॉल करें।\"",
                                color = Color(0xFF00E676),
                                fontSize = 12.sp
                            )
                        } else if (nextClass != null) {
                            Text(
                                text = "आज अगली क्लास: ${nextClass.subject} (${nextClass.startTime} बजे)",
                                color = SnehaTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "प्रोफेसर: ${nextClass.professor} • ${nextClass.room}",
                                color = SnehaTextSecondary,
                                fontSize = 12.sp
                            )
                        } else {
                            Text(
                                text = "आज कोई और क्लास नहीं है। आप फ्री हैं!",
                                color = SnehaTextPrimary,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    CallAssistantManager.setDefaultMessage(
                                        "नमस्ते, मास्टर अभी कॉलेज की क्लास में हैं। क्लास खत्म होने के बाद आपसे संपर्क करेंगे।"
                                    )
                                    onSpeakAnnouncement("क्लास मोड टेस्ट: फोन साइलेंट हो चुका है और ऑटो-अटेंडेंट एक्टिव है!")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SnehaDarkSurfaceVariant),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = SnehaCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("क्लास साइलेंस टेस्ट करें", color = SnehaCyan, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Day Selector Tabs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    days.forEach { (day, label) ->
                        FilterChip(
                            selected = selectedDay == day,
                            onClick = { selectedDay = day },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SnehaCyan.copy(alpha = 0.25f),
                                selectedLabelColor = SnehaCyan,
                                labelColor = SnehaTextSecondary
                            )
                        )
                    }
                }
            }

            // Lectures List for Selected Day
            val dayLectures = lectures.filter { it.dayOfWeek == selectedDay }

            if (dayLectures.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "इस दिन कोई क्लास शेड्यूल नहीं है।",
                                color = SnehaTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                items(dayLectures) { lecture ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .background(SnehaDarkSurfaceVariant, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = lecture.startTime,
                                    color = SnehaCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "से",
                                    color = SnehaTextSecondary,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = lecture.endTime,
                                    color = SnehaTextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = lecture.subject,
                                    color = SnehaTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "प्रोफेसर: ${lecture.professor}",
                                    color = SnehaTextSecondary,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "📍 ${lecture.room}",
                                    color = SnehaCyan,
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(
                                onClick = { ClassTimetableManager.removeLecture(context, lecture.id) }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SnehaTextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button to Add Class
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = SnehaCyan,
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 20.dp)
                .testTag("add_lecture_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Lecture")
        }

        // Add Lecture Dialog
        if (showAddDialog) {
            var subjectName by remember { mutableStateOf("") }
            var professorName by remember { mutableStateOf("") }
            var roomNumber by remember { mutableStateOf("") }
            var startTimeStr by remember { mutableStateOf("10:00") }
            var endTimeStr by remember { mutableStateOf("11:00") }

            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("नया पीरियड / क्लास जोड़ें", color = SnehaTextPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = subjectName,
                            onValueChange = { subjectName = it },
                            label = { Text("विषय (Subject)", color = SnehaTextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SnehaTextPrimary,
                                unfocusedTextColor = SnehaTextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = professorName,
                            onValueChange = { professorName = it },
                            label = { Text("प्रोफेसर का नाम", color = SnehaTextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SnehaTextPrimary,
                                unfocusedTextColor = SnehaTextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = roomNumber,
                            onValueChange = { roomNumber = it },
                            label = { Text("कमरा / लैब नंबर", color = SnehaTextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SnehaTextPrimary,
                                unfocusedTextColor = SnehaTextPrimary
                            )
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = startTimeStr,
                                onValueChange = { startTimeStr = it },
                                label = { Text("शुरू (HH:mm)", color = SnehaTextSecondary) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = SnehaTextPrimary,
                                    unfocusedTextColor = SnehaTextPrimary
                                )
                            )
                            OutlinedTextField(
                                value = endTimeStr,
                                onValueChange = { endTimeStr = it },
                                label = { Text("समाप्त (HH:mm)", color = SnehaTextSecondary) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = SnehaTextPrimary,
                                    unfocusedTextColor = SnehaTextPrimary
                                )
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (subjectName.isNotBlank()) {
                                ClassTimetableManager.addLecture(
                                    context = context,
                                    lecture = ClassLecture(
                                        id = System.currentTimeMillis().toString(),
                                        dayOfWeek = selectedDay,
                                        subject = subjectName,
                                        professor = if (professorName.isNotBlank()) professorName else "संबंधित शिक्षक",
                                        room = if (roomNumber.isNotBlank()) roomNumber else "कक्षा",
                                        startTime = startTimeStr,
                                        endTime = endTimeStr
                                    )
                                )
                                showAddDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SnehaCyan)
                    ) {
                        Text("जोड़ें", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("रद्द करें", color = SnehaTextSecondary)
                    }
                },
                containerColor = SnehaDarkCard
            )
        }
    }
}
