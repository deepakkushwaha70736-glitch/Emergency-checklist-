package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun OfficerContactSettingsDialog(
    initialCo: String,
    initialAsp: String,
    initialSp: String,
    onSave: (co: String, asp: String, sp: String) -> Unit,
    onDismiss: () -> Unit
) {
    var coNumber by remember { mutableStateOf(initialCo) }
    var aspNumber by remember { mutableStateOf(initialAsp) }
    var spNumber by remember { mutableStateOf(initialSp) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⚙️ अधिकारी के नंबर (CO / ASP / SP)") },
        text = {
            Column {
                OutlinedTextField(
                    value = coNumber,
                    onValueChange = { coNumber = it },
                    label = { Text("CO Number (Circle Officer)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("setting_co_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = aspNumber,
                    onValueChange = { aspNumber = it },
                    label = { Text("ASP Number (Addl. Superintendent)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("setting_asp_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = spNumber,
                    onValueChange = { spNumber = it },
                    label = { Text("SP Number (Superintendent of Police)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("setting_sp_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(coNumber.trim(), aspNumber.trim(), spNumber.trim())
                    onDismiss()
                }
            ) {
                Text("Save Numbers")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
