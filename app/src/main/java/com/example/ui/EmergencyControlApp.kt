package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EmergencyRepository
import com.example.model.EmergencyIncident
import com.example.model.EmergencyType
import com.example.model.Priority
import com.example.ui.components.BackupRestoreDialog
import com.example.ui.components.CelebrationDialog
import com.example.ui.components.EmergencyBanner
import com.example.ui.components.EmergencyDetailView
import com.example.ui.components.NewEmergencyDialog
import com.example.ui.components.OfficerContactSettingsDialog
import com.example.ui.components.OfficerStatsSheet
import com.example.ui.theme.EmergencyAmber
import com.example.ui.theme.EmergencyBlue
import com.example.ui.theme.EmergencyGreen
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.NavyDark
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyControlApp(repository: EmergencyRepository) {
    val incidents by repository.incidents.collectAsState()
    val officerProfiles by repository.officerProfiles.collectAsState()
    val currentOfficer by repository.currentOfficer.collectAsState()
    val celebrationEvent by repository.celebrationEvent.collectAsState()
    val coNumber by repository.contactCo.collectAsState()
    val aspNumber by repository.contactAsp.collectAsState()
    val spNumber by repository.contactSp.collectAsState()

    var isSoundEnabled by remember { mutableStateOf(repository.soundPlayer.isSoundEnabled) }

    var selectedIncidentId by remember { mutableStateOf<Long?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<EmergencyType?>(null) }
    var statusFilter by remember { mutableStateOf("active") } // "active", "all", "Open", "In Progress", "Closed"

    var showNewEmergencyDialog by remember { mutableStateOf(false) }
    var showOfficerStatsSheet by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Handle back button when an incident is selected to deselect it
    BackHandler(enabled = selectedIncidentId != null) {
        selectedIncidentId = null
    }

    val currentSelectedIncident = incidents.find { it.id == selectedIncidentId }
        ?: incidents.firstOrNull { !it.isClosed }

    // Filter and sort incidents
    val filteredIncidents = incidents.filter { e ->
        val status = if (e.isClosed) "Closed" else if (e.completedItemsCount > 0) "In Progress" else "Open"
        val matchesStatus = when (statusFilter) {
            "active" -> !e.isClosed
            "all" -> true
            else -> status.equals(statusFilter, ignoreCase = true)
        }
        val matchesType = selectedTypeFilter == null || e.type == selectedTypeFilter
        val matchesQuery = searchQuery.isBlank() ||
                (e.number + e.location + e.type.displayName).contains(searchQuery, ignoreCase = true)

        matchesStatus && matchesType && matchesQuery
    }.sortedWith(
        compareBy<EmergencyIncident> { it.isClosed }
            .thenBy { it.priority.ordinal }
            .thenByDescending { it.createdAt }
    )

    // Calculate Control Room statistics
    val activeCount = incidents.count { !it.isClosed }
    val closedCount = incidents.count { it.isClosed }
    val lateCount = incidents.count { it.isLate() }
    val todayStart = remember {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.timeInMillis
    }
    val todayCount = incidents.count { it.createdAt >= todayStart }
    val currentOfficerProfile = officerProfiles[currentOfficer]

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🚨 Emergency Control",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                        Text(
                            text = "Hathras Control Room · Duty: $currentOfficer",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF93C5FD))
                        )
                    }
                },
                actions = {
                    // Audio alert sound toggle (Mute / Unmute)
                    IconButton(
                        onClick = {
                            val newSound = !isSoundEnabled
                            isSoundEnabled = newSound
                            repository.setSoundEnabled(newSound)
                        },
                        modifier = Modifier.testTag("sound_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = if (isSoundEnabled) "Sound On" else "Sound Muted",
                            tint = if (isSoundEnabled) Color(0xFFFDE047) else Color(0xFF94A3B8)
                        )
                    }

                    // Officer Stats & Badges Leaderboard button
                    IconButton(
                        onClick = { showOfficerStatsSheet = true },
                        modifier = Modifier.testTag("officer_stats_btn")
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Officer Stats & Badges",
                                tint = Color(0xFFFBBF24)
                            )
                            if ((currentOfficerProfile?.completedChecklistsCount ?: 0) > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = EmergencyGreen,
                                    modifier = Modifier.size(14.dp)
                                ) {
                                    Text(
                                        text = "${currentOfficerProfile?.completedChecklistsCount}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Contact Settings (CO, ASP, SP numbers)
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Officer Contacts",
                            tint = Color.White
                        )
                    }

                    // Backup / Restore
                    IconButton(
                        onClick = { showBackupDialog = true },
                        modifier = Modifier.testTag("backup_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "Backup and Restore",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyDark,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewEmergencyDialog = true },
                containerColor = EmergencyRed,
                contentColor = Color.White,
                modifier = Modifier.testTag("create_emergency_fab")
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Emergency", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // Emergency Alert / Status Banner
            item {
                EmergencyBanner(
                    activeIncident = currentSelectedIncident,
                    totalActiveCount = activeCount,
                    onClick = {
                        if (currentSelectedIncident != null) {
                            selectedIncidentId = currentSelectedIncident.id
                        }
                    }
                )
            }

            // Dashboard Statistics Cards
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📊 Control Room Dashboard",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Officer: $currentOfficer (${currentOfficerProfile?.completedChecklistsCount ?: 0} done)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = EmergencyBlue,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            DashMetric(label = "आज", value = "$todayCount", color = MaterialTheme.colorScheme.onSurface)
                            DashMetric(label = "सक्रिय", value = "$activeCount", color = EmergencyBlue)
                            DashMetric(label = "देरी", value = "$lateCount", color = EmergencyRed)
                            DashMetric(label = "बंद", value = "$closedCount", color = EmergencyGreen)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Type filter tags
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedTypeFilter == null,
                                    onClick = { selectedTypeFilter = null },
                                    label = { Text("सभी") }
                                )
                            }
                            items(EmergencyType.entries) { type ->
                                val count = incidents.count { it.type == type }
                                FilterChip(
                                    selected = selectedTypeFilter == type,
                                    onClick = {
                                        selectedTypeFilter = if (selectedTypeFilter == type) null else type
                                    },
                                    label = { Text("${type.icon} ${type.name.take(4)}: $count") }
                                )
                            }
                        }
                    }
                }
            }

            // Search & Status Filter Row
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("🔍 Search (नंबर, location, प्रकार)") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_field")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("active" to "सक्रिय", "all" to "सभी", "Open" to "Open", "In Progress" to "In Progress", "Closed" to "Closed").forEach { (key, label) ->
                            FilterChip(
                                selected = statusFilter == key,
                                onClick = { statusFilter = key },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // Selected Emergency Detail (Expanded when chosen)
            if (currentSelectedIncident != null) {
                item {
                    Text(
                        text = "🎯 Active Response Workspace",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmergencyBlue
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    EmergencyDetailView(
                        incident = currentSelectedIncident,
                        currentOfficer = currentOfficer,
                        coNumber = coNumber,
                        aspNumber = aspNumber,
                        spNumber = spNumber,
                        onToggleChecklistItem = { itemId ->
                            repository.toggleChecklistItem(currentSelectedIncident.id, itemId, currentOfficer)
                        },
                        onCustomContactChange = { itemId, contact ->
                            repository.setItemCustomContact(currentSelectedIncident.id, itemId, contact)
                        },
                        onAddNote = { noteText ->
                            repository.addNote(currentSelectedIncident.id, noteText, currentOfficer)
                        },
                        onCloseEmergency = {
                            repository.closeEmergency(currentSelectedIncident.id)
                        },
                        onReopenEmergency = {
                            repository.reopenEmergency(currentSelectedIncident.id)
                        },
                        onDeleteEmergency = {
                            repository.deleteEmergency(currentSelectedIncident.id)
                            selectedIncidentId = null
                        }
                    )
                }
            }

            // Emergency List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Emergency List (${filteredIncidents.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (selectedIncidentId != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedIncidentId = null }
                        ) {
                            Text(
                                text = "Clear Selection ✕",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Emergency List Items
            if (filteredIncidents.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("कोई Emergency नहीं मिली।", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showNewEmergencyDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                            ) {
                                Text("＋ नई Emergency बनाएं")
                            }
                        }
                    }
                }
            } else {
                items(filteredIncidents, key = { it.id }) { incident ->
                    val isSelected = incident.id == currentSelectedIncident?.id
                    EmergencyListItemCard(
                        incident = incident,
                        isSelected = isSelected,
                        onClick = {
                            selectedIncidentId = incident.id
                            scope.launch {
                                listState.animateScrollToItem(3)
                            }
                        }
                    )
                }
            }

            // App Footer
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 70.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            EmergencyBlue.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🇮🇳 Developed by AO DEEPAK KUSHWAHA",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmergencyBlue
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Emergency Control Room & Checklist System · Hathras",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }

    // Celebratory Reward Dialog (with Confetti animation and audio jingle!)
    celebrationEvent?.let { event ->
        CelebrationDialog(
            event = event,
            onDismiss = { repository.clearCelebrationEvent() },
            onCloseEmergency = {
                repository.closeEmergency(event.incident.id)
            },
            onReplayJingle = {
                repository.soundPlayer.playCelebrationJingle()
            }
        )
    }

    // New Emergency Dialog
    if (showNewEmergencyDialog) {
        NewEmergencyDialog(
            initialOfficerName = currentOfficer,
            onDismiss = { showNewEmergencyDialog = false },
            onCreate = { type, location, priority, officer ->
                val newIncident = repository.createEmergency(type, location, priority, officer)
                selectedIncidentId = newIncident.id
                scope.launch {
                    listState.animateScrollToItem(3)
                }
            }
        )
    }

    // Officer Stats Sheet
    if (showOfficerStatsSheet) {
        OfficerStatsSheet(
            sheetState = sheetState,
            currentOfficerName = currentOfficer,
            officersMap = officerProfiles,
            onSelectOfficer = { newName ->
                repository.setCurrentOfficer(newName)
            },
            onDismiss = { showOfficerStatsSheet = false }
        )
    }

    // Backup & Restore Dialog
    if (showBackupDialog) {
        BackupRestoreDialog(
            repository = repository,
            onDismiss = { showBackupDialog = false }
        )
    }

    // Officer Contact Settings Dialog
    if (showSettingsDialog) {
        OfficerContactSettingsDialog(
            initialCo = coNumber,
            initialAsp = aspNumber,
            initialSp = spNumber,
            onSave = { co, asp, sp ->
                repository.setEmergencyContacts(co, asp, sp)
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun EmergencyListItemCard(
    incident: EmergencyIncident,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when (incident.priority) {
        Priority.HIGH -> EmergencyRed
        Priority.MEDIUM -> EmergencyAmber
        Priority.LOW -> EmergencyGreen
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) EmergencyBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .testTag("emergency_card_${incident.number}")
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left Priority colored bar
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(96.dp)
                    .background(borderColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "<b>${incident.number}</b> ${incident.type.icon} ${incident.type.displayName}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${incident.completedItemsCount}/${incident.totalItemsCount}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (incident.isChecklistComplete) EmergencyGreen else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "📍 ${incident.location}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${incident.priority.emoji} ${incident.priority.name}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    val status = if (incident.isClosed) "Closed" else if (incident.completedItemsCount > 0) "In Progress" else "Open"
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (incident.isClosed) Color(0xFFE2E8F0) else Color(0xFFDBEAFE)
                    ) {
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (incident.isClosed) Color(0xFF475569) else EmergencyBlue
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "⏱ ${incident.elapsedMinutes()}m",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (incident.isLate()) EmergencyRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (incident.isLate()) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                    if (incident.isLate()) {
                        Text(
                            text = "⚠ SLA",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = EmergencyRed,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                val progress = if (incident.totalItemsCount > 0) {
                    incident.completedItemsCount.toFloat() / incident.totalItemsCount.toFloat()
                } else 0f

                LinearProgressIndicator(
                    progress = { progress },
                    color = if (incident.isChecklistComplete) EmergencyGreen else EmergencyBlue,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                )
            }
        }
    }
}

@Composable
private fun DashMetric(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}
