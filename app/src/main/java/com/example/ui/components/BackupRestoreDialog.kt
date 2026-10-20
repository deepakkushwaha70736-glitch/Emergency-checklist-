package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.EmergencyRepository
import com.example.ui.theme.EmergencyBlue

@Composable
fun BackupRestoreDialog(
    repository: EmergencyRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var backupText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("💾 Backup & Data Management") },
        text = {
            Column {
                Text(
                    text = "Backup copy karke WhatsApp/Notes mein surakshit rakhein. Restore karne ke liye text paste karein.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = backupText,
                    onValueChange = { backupText = it },
                    label = { Text("Backup JSON Data") },
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .testTag("backup_text_field")
                )
                if (statusMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall.copy(color = EmergencyBlue)
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = {
                        val json = repository.exportBackupJson()
                        backupText = json
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Emergency Backup", json))
                        statusMessage = "✔ JSON Backup copied to clipboard"
                    }
                ) {
                    Text("Copy JSON")
                }
                OutlinedButton(
                    onClick = {
                        val csv = repository.exportCsv()
                        backupText = csv
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Emergency CSV", csv))
                        statusMessage = "✔ CSV copied to clipboard (Excel/Sheets ready)"
                    }
                ) {
                    Text("Copy CSV")
                }
                Button(
                    onClick = {
                        if (backupText.isNotBlank()) {
                            val success = repository.restoreBackupJson(backupText.trim())
                            if (success) {
                                statusMessage = "✔ Backup restored successfully!"
                                Toast.makeText(context, "Data restored!", Toast.LENGTH_SHORT).show()
                            } else {
                                statusMessage = "✘ Invalid JSON format"
                            }
                        }
                    }
                ) {
                    Text("Restore")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
