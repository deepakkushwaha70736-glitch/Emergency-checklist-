package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EmergencyTemplates
import com.example.model.ChecklistItem
import com.example.model.EmergencyIncident
import com.example.ui.theme.EmergencyBlue
import com.example.ui.theme.EmergencyGreen
import com.example.ui.theme.EmergencyGreenBg
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChecklistItemCard(
    item: ChecklistItem,
    incident: EmergencyIncident,
    coNumber: String,
    aspNumber: String,
    spNumber: String,
    onToggle: () -> Unit,
    onCustomContactChange: (String) -> Unit
) {
    val context = LocalContext.current
    val resolvedNumber = resolvePhoneNumber(
        item = item,
        location = incident.location,
        globalCo = coNumber,
        globalAsp = aspNumber,
        globalSp = spNumber
    )

    val dateFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isDone) EmergencyGreenBg else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isDone) 0.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (item.isDone) EmergencyGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .testTag("checklist_item_${item.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = (if (item.isDone) "☑ " else "☐ ") + item.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.isDone) Color(0xFF14532D) else MaterialTheme.colorScheme.onSurface
                        )
                    )

                    if (item.completedAt != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "✔ Done at ${dateFormat.format(Date(item.completedAt))} · ${item.completedBy ?: "Officer"}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row: Contact Tag / Dropdown / Dial Button / Toggle Done Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Call Action & Contact Tag
                if (resolvedNumber.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "📞 $resolvedNumber",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$resolvedNumber"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("call_btn_${item.id}")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call", fontSize = 13.sp)
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Action Step",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Done / Pending Toggle Button
                if (item.isDone) {
                    Button(
                        onClick = onToggle,
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("toggle_done_${item.id}")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Done", fontSize = 13.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = onToggle,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("toggle_pending_${item.id}")
                    ) {
                        Text("Mark Done", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

private fun resolvePhoneNumber(
    item: ChecklistItem,
    location: String,
    globalCo: String,
    globalAsp: String,
    globalSp: String
): String {
    if (!item.customContactNumber.isNullOrEmpty()) return item.customContactNumber
    return when (item.contactKey) {
        "101" -> "101"
        "108" -> "108"
        "thana" -> EmergencyTemplates.resolveThanaNumber(location)
        "co" -> {
            val thanaNum = EmergencyTemplates.resolveThanaNumber(location)
            EmergencyTemplates.resolveCONumber(thanaNum).ifEmpty { globalCo }
        }
        "asp" -> globalAsp
        "sp" -> globalSp
        else -> ""
    }
}
