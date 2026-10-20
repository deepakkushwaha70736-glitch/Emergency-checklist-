package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EmergencyIncident
import com.example.ui.theme.EmergencyGreen
import com.example.ui.theme.EmergencyGreenBg
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedBg

@Composable
fun EmergencyBanner(
    activeIncident: EmergencyIncident?,
    totalActiveCount: Int,
    onClick: () -> Unit
) {
    if (activeIncident != null && !activeIncident.isClosed) {
        val isComplete = activeIncident.isChecklistComplete
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (!isComplete) 0.65f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )

        if (!isComplete) {
            // Incomplete Checklist: Red Alert Banner
            val pendingItems = activeIncident.items.filterNot { it.isDone }
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = EmergencyRedBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(alpha)
                    .border(2.dp, EmergencyRed, RoundedCornerShape(14.dp))
                    .clickable { onClick() }
                    .testTag("emergency_alert_banner")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = EmergencyRed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🔴 EMERGENCY ALERT — CHECKLIST INCOMPLETE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmergencyRed
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Emergency: ${activeIncident.type.displayName} (${activeIncident.number})",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF7F1D1D)
                    )
                    Text(
                        text = "📍 Location: ${activeIncident.location}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF991B1B))
                    )
                    Text(
                        text = "⏱ Elapsed: ${activeIncident.elapsedMinutes()} min " +
                                if (activeIncident.isLate()) "— ⚠ SLA EXCEEDED (${activeIncident.priority.slaMinutes}m)" else "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmergencyRed
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pending Actions (${pendingItems.size}):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF7F1D1D))
                    )
                    pendingItems.take(3).forEach { item ->
                        Text(
                            text = "• ${item.title}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF7F1D1D))
                        )
                    }
                    if (pendingItems.size > 3) {
                        Text(
                            text = "... and ${pendingItems.size - 3} more",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF7F1D1D), fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        } else {
            // Complete Checklist: Green Success Banner
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = EmergencyGreenBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, EmergencyGreen, RoundedCornerShape(14.dp))
                    .clickable { onClick() }
                    .testTag("checklist_complete_banner")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmergencyGreen
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "🟢 CHECKLIST COMPLETE — All Actions Done",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmergencyGreen
                            )
                        )
                        Text(
                            text = "${activeIncident.number} · ${activeIncident.location} — Ready to close.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF14532D))
                        )
                    }
                }
            }
        }
    } else {
        // Idle banner
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .clickable { onClick() }
                .testTag("idle_banner")
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🚨",
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (totalActiveCount > 0)
                        "Active Emergencies: $totalActiveCount — Tap to view checklist details."
                    else
                        "No active emergencies. System operational and monitoring.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}
