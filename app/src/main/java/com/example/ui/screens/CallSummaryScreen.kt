package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.example.engine.CallSummaryItem
import com.example.engine.CallSummaryManager
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
fun CallSummaryScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val summaries by CallSummaryManager.summaries.collectAsState()

    var selectedFilter by remember { mutableStateOf("सभी") }
    val filterTabs = listOf("सभी", "कॉलेज / पढ़ाई", "दोस्त / परिवार", "ऑफिस / काम")

    var showCreateDialog by remember { mutableStateOf(false) }
    var newCallerName by remember { mutableStateOf("") }
    var newCallerNumber by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("कॉलेज / पढ़ाई") }
    var newTranscript by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    val filteredList = if (selectedFilter == "सभी") {
        summaries
    } else {
        summaries.filter { it.category == selectedFilter }
    }

    Box(modifier = modifier.fillMaxSize().background(SnehaBgDark)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Banner
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
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Summarize, contentDescription = null, tint = SnehaCyan, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("AI कॉल सारांश व कार्य सूची", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SnehaTextPrimary)
                                Text("कॉल खत्म होते ही मुख्य बिंदु व To-Do नोट्स अपने आप तैयार", fontSize = 11.sp, color = SnehaCyan)
                            }
                        }
                    }
                }
            }

            // Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterTabs.forEach { tab ->
                        FilterChip(
                            selected = selectedFilter == tab,
                            onClick = { selectedFilter = tab },
                            label = { Text(tab, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SnehaCyan.copy(alpha = 0.2f),
                                selectedLabelColor = SnehaCyan,
                                containerColor = SnehaDarkSurfaceVariant,
                                labelColor = SnehaTextSecondary
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (selectedFilter == tab) SnehaCyan else Color.Transparent
                            )
                        )
                    }
                }
            }

            // Summary Items
            if (filteredList.isEmpty()) {
                item {
                    Surface(
                        color = SnehaDarkSurface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "इस श्रेणी में कोई कॉल सारांश उपलब्ध नहीं है। नीचे दिए '+' बटन से नया सारांश जोड़ें।",
                            fontSize = 12.sp,
                            color = SnehaTextSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { item ->
                    CallSummaryCard(
                        item = item,
                        onToggleAction = { CallSummaryManager.toggleActionItem(item.id) },
                        onDelete = { CallSummaryManager.deleteSummary(item.id) },
                        onShare = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clip = ClipData.newPlainText("Call Summary", "${item.callerName} कॉल सारांश:\n" + item.summaryPoints.joinToString("\n• ") + "\n\nकार्य बिंदु:\n" + item.actionItems.joinToString("\n- "))
                            clipboard?.setPrimaryClip(clip)
                            Toast.makeText(context, "सारांश कॉपी किया गया!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        // Floating Action Button to Add / Simulate New Summary
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = SnehaCyan,
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 20.dp)
                .testTag("fab_add_call_summary")
        ) {
            Icon(Icons.Default.Add, contentDescription = "नया सारांश बनाएं")
        }

        // Create Summary Dialog
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { if (!isGenerating) showCreateDialog = false },
                containerColor = SnehaDarkSurface,
                title = {
                    Text("📝 नया AI कॉल सारांश बनाएं", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SnehaCyan)
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newCallerName,
                            onValueChange = { newCallerName = it },
                            label = { Text("कॉलर का नाम (उदा. एचओडी सर)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SnehaCyan,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                                focusedTextColor = SnehaTextPrimary,
                                unfocusedTextColor = SnehaTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = newTranscript,
                            onValueChange = { newTranscript = it },
                            label = { Text("कॉल में हुई बातचीत या नोट्स") },
                            placeholder = { Text("उदा: कल सुबह 10 बजे कॉलेज पहुंचना है, असाइनमेंट साइन करवाने हैं") },
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SnehaCyan,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                                focusedTextColor = SnehaTextPrimary,
                                unfocusedTextColor = SnehaTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (isGenerating) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                CircularProgressIndicator(color = SnehaCyan, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("AI सारांश व To-Do तैयार हो रहा है...", fontSize = 12.sp, color = SnehaCyan)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newTranscript.isNotBlank() && !isGenerating) {
                                isGenerating = true
                                scope.launch {
                                    CallSummaryManager.createSummaryFromTranscript(
                                        context = context,
                                        callerName = newCallerName.ifBlank { "कॉलर" },
                                        callerNumber = newCallerNumber.ifBlank { "+91 98765-XXXXX" },
                                        category = newCategory,
                                        transcript = newTranscript
                                    )
                                    isGenerating = false
                                    showCreateDialog = false
                                    newTranscript = ""
                                    newCallerName = ""
                                }
                            }
                        },
                        enabled = newTranscript.isNotBlank() && !isGenerating,
                        colors = ButtonDefaults.buttonColors(containerColor = SnehaCyan),
                        modifier = Modifier.testTag("btn_confirm_create_summary")
                    ) {
                        Text("सारांश निकालें", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("रद्द करें", color = SnehaTextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun CallSummaryCard(
    item: CallSummaryItem,
    onToggleAction: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SnehaDarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = SnehaCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(item.callerName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SnehaTextPrimary)
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = SnehaDarkSurfaceVariant
                ) {
                    Text(
                        text = item.category,
                        fontSize = 10.sp,
                        color = SnehaCyan,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(item.timestamp, fontSize = 11.sp, color = SnehaTextSecondary)
                Text(item.urgencyLevel, fontSize = 11.sp, color = SnehaPink)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bullet Points
            Text("📌 मुख्य बातें:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SnehaTextPrimary)
            item.summaryPoints.forEach { point ->
                Row(modifier = Modifier.padding(top = 3.dp, start = 4.dp)) {
                    Text("• ", color = SnehaCyan, fontSize = 12.sp)
                    Text(point, fontSize = 11.sp, color = SnehaTextSecondary, lineHeight = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Items
            if (item.actionItems.isNotEmpty()) {
                Text("✅ कार्य सूची (Action Items):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFFB300))
                item.actionItems.forEach { action ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        IconButton(
                            onClick = onToggleAction,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isActionCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (item.isActionCompleted) Color(0xFF00E676) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = action,
                            fontSize = 11.sp,
                            color = if (item.isActionCompleted) SnehaTextSecondary else SnehaTextPrimary,
                            fontWeight = if (item.isActionCompleted) FontWeight.Normal else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "कॉपी करें", tint = SnehaCyan, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "हटाएं", tint = SnehaTextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
