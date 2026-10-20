package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EmergencyTemplates
import com.example.model.EmergencyIncident
import com.example.ui.theme.EmergencyAmber
import com.example.ui.theme.EmergencyBlue
import com.example.ui.theme.EmergencyGreen
import com.example.ui.theme.EmergencyGreenBg
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedBg
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EmergencyDetailView(
    incident: EmergencyIncident,
    currentOfficer: String,
    coNumber: String,
    aspNumber: String,
    spNumber: String,
    onToggleChecklistItem: (String) -> Unit,
    onCustomContactChange: (itemId: String, contactNumber: String) -> Unit,
    onAddNote: (String) -> Unit,
    onCloseEmergency: () -> Unit,
    onReopenEmergency: () -> Unit,
    onDeleteEmergency: () -> Unit
) {
    val context = LocalContext.current
    var noteInput by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val isComplete = incident.isChecklistComplete

    val fullReportText = remember(incident) {
        generateEmergencyReport(incident, dateFormat)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .testTag("emergency_detail_view")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${incident.number} · ${incident.type.icon} ${incident.type.displayName}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "📍 ${incident.location}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                FilledTonalButton(
                    onClick = {
                        val query = incident.location.replace("🚔 ", "")
                        val mapUri = Uri.parse("geo:0,0?q=" + Uri.encode("$query, Hathras"))
                        val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                        try {
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode("$query, Hathras"))
                            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                        }
                    },
                    modifier = Modifier.testTag("open_map_btn")
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Map")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Timestamps and Officer
            Text(
                text = "बनाई गई: ${dateFormat.format(Date(incident.createdAt))} · ${incident.createdBy}" +
                        if (incident.closedAt != null) "\nबंद की गई: ${dateFormat.format(Date(incident.closedAt))}" else "",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Badges row: Priority, Status, SLA Timer, Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${incident.priority.emoji} ${incident.priority.label}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when {
                        incident.isClosed -> Color(0xFFE2E8F0)
                        incident.completedItemsCount > 0 -> Color(0xFFDBEAFE)
                        else -> Color(0xFFFEF3C7)
                    }
                ) {
                    Text(
                        text = if (incident.isClosed) "Closed" else if (incident.completedItemsCount > 0) "In Progress" else "Open",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (incident.isClosed) Color(0xFF475569) else if (incident.completedItemsCount > 0) EmergencyBlue else Color(0xFF92400E)
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (incident.isLate()) EmergencyRedBg else EmergencyGreenBg
                ) {
                    Text(
                        text = "⏱ ${incident.elapsedMinutes()}m" + if (incident.isLate()) " ⚠ SLA पार" else "",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (incident.isLate()) EmergencyRed else EmergencyGreen
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "${incident.completedItemsCount}/${incident.totalItemsCount}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isComplete) EmergencyGreen else MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick dialer bar for common emergency numbers
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickDialChip(label = "100 Police", number = "100", context = context)
                    QuickDialChip(label = "101 Fire", number = "101", context = context)
                    QuickDialChip(label = "108 Amb", number = "108", context = context)
                    QuickDialChip(label = "112 Quick", number = "112", context = context)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Checklist items section
            Text(
                text = "📋 Action Checklist (${incident.completedItemsCount}/${incident.totalItemsCount})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                incident.items.forEach { item ->
                    ChecklistItemCard(
                        item = item,
                        incident = incident,
                        coNumber = coNumber,
                        aspNumber = aspNumber,
                        spNumber = spNumber,
                        onToggle = { onToggleChecklistItem(item.id) },
                        onCustomContactChange = { newContact ->
                            onCustomContactChange(item.id, newContact)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Notes Section
            Text(
                text = "📝 Notes & Action Log (${incident.notes.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (incident.notes.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    incident.notes.forEach { note ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = note.text,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${dateFormat.format(Date(note.timestamp))} · ${note.author}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    placeholder = { Text("जैसे: घायल 2, वाहन नंबर, निर्देश") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("note_input")
                )
                Button(
                    onClick = {
                        if (noteInput.isNotBlank()) {
                            onAddNote(noteInput)
                            noteInput = ""
                        }
                    },
                    modifier = Modifier.testTag("add_note_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Share & Report Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showReportDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("view_report_btn")
                ) {
                    Text("📄 Report")
                }

                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, fullReportText)
                            setPackage("com.whatsapp")
                        }
                        try {
                            context.startActivity(shareIntent)
                        } catch (e: Exception) {
                            // Fallback to generic share
                            val chooser = Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, fullReportText)
                                },
                                "Share Emergency Report"
                            )
                            context.startActivity(chooser)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("whatsapp_btn")
                ) {
                    Text("💬 WhatsApp")
                }

                Button(
                    onClick = {
                        val smsIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("sms:")
                            putExtra("sms_body", fullReportText)
                        }
                        try {
                            context.startActivity(smsIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "SMS app not available", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyBlue),
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("sms_btn")
                ) {
                    Text("✉ SMS")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(14.dp))

            // Emergency Closure / Status actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (incident.isClosed) {
                    OutlinedButton(
                        onClick = onReopenEmergency,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reopen_emergency_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reopen Emergency")
                    }
                } else {
                    Button(
                        onClick = onCloseEmergency,
                        enabled = isComplete,
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyGreen),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("close_emergency_btn")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isComplete) "✔ Close Emergency" else "Complete Checklist to Close")
                    }
                }

                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed),
                    modifier = Modifier.testTag("delete_emergency_btn")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }

    // Report View & Copy Dialog
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Emergency Report – ${incident.number}") },
            text = {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = fullReportText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Emergency Report", fullReportText))
                        Toast.makeText(context, "✔ Report copied to clipboard", Toast.LENGTH_SHORT).show()
                        showReportDialog = false
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Emergency ${incident.number}?") },
            text = { Text("Are you sure you want to permanently delete this emergency incident record?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteEmergency()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun QuickDialChip(label: String, number: String, context: Context) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.clickable {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
            context.startActivity(intent)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(12.dp), tint = EmergencyBlue)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        }
    }
}

private fun generateEmergencyReport(incident: EmergencyIncident, dateFormat: SimpleDateFormat): String {
    val sb = StringBuilder()
    sb.append("🚨 EMERGENCY REPORT – ${incident.number}\n")
    sb.append("घटना: ${incident.type.displayName}\n")
    sb.append("Location: ${incident.location}\n")
    sb.append("Priority: ${incident.priority.label}\n")
    val status = if (incident.isClosed) "Closed" else if (incident.completedItemsCount > 0) "In Progress" else "Open"
    sb.append("Status: $status\n")
    sb.append("बनाई गई: ${dateFormat.format(Date(incident.createdAt))} (${incident.createdBy})\n")
    if (incident.closedAt != null) {
        sb.append("बंद की गई: ${dateFormat.format(Date(incident.closedAt))}\n")
    }
    sb.append("कुल समय: ${incident.elapsedMinutes()} मिनट\n\n")

    sb.append("Checklist Actions:\n")
    incident.items.forEachIndexed { i, item ->
        val mark = if (item.isDone) "✔" else "✘"
        val time = if (item.completedAt != null) " – ${dateFormat.format(Date(item.completedAt))} – ${item.completedBy}" else " – pending"
        sb.append("${i + 1}. $mark ${item.title}$time\n")
    }

    if (incident.notes.isNotEmpty()) {
        sb.append("\nNotes & Updates:\n")
        incident.notes.forEach { note ->
            sb.append("• ${dateFormat.format(Date(note.timestamp))} – ${note.text} (${note.author})\n")
        }
    }
    sb.append("\n— Emergency Control Room, Hathras\n🇮🇳 Developed by AO DEEPAK KUSHWAHA")
    return sb.toString()
}
