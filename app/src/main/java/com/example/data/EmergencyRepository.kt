package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.audio.EmergencySoundPlayer
import com.example.model.ChecklistItem
import com.example.model.EmergencyIncident
import com.example.model.EmergencyNote
import com.example.model.EmergencyType
import com.example.model.OfficerBadge
import com.example.model.OfficerProfile
import com.example.model.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class ChecklistCelebrationEvent(
    val incident: EmergencyIncident,
    val officerName: String,
    val completedCountForOfficer: Int,
    val timeMinutes: Long,
    val isWithinSla: Boolean,
    val newlyUnlockedBadges: List<OfficerBadge>
)

class EmergencyRepository(
    private val context: Context,
    val soundPlayer: EmergencySoundPlayer
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("emergency_control_prefs_v1", Context.MODE_PRIVATE)

    private val _incidents = MutableStateFlow<List<EmergencyIncident>>(emptyList())
    val incidents: StateFlow<List<EmergencyIncident>> = _incidents.asStateFlow()

    private val _officerProfiles = MutableStateFlow<Map<String, OfficerProfile>>(emptyMap())
    val officerProfiles: StateFlow<Map<String, OfficerProfile>> = _officerProfiles.asStateFlow()

    private val _currentOfficer = MutableStateFlow("SI Rajesh Kumar")
    val currentOfficer: StateFlow<String> = _currentOfficer.asStateFlow()

    private val _celebrationEvent = MutableStateFlow<ChecklistCelebrationEvent?>(null)
    val celebrationEvent: StateFlow<ChecklistCelebrationEvent?> = _celebrationEvent.asStateFlow()

    private val _contactCo = MutableStateFlow("9454401258")
    val contactCo: StateFlow<String> = _contactCo.asStateFlow()

    private val _contactAsp = MutableStateFlow(EmergencyTemplates.DEFAULT_ASP_NUMBER)
    val contactAsp: StateFlow<String> = _contactAsp.asStateFlow()

    private val _contactSp = MutableStateFlow(EmergencyTemplates.DEFAULT_SP_NUMBER)
    val contactSp: StateFlow<String> = _contactSp.asStateFlow()

    init {
        loadFromPrefs()
        soundPlayer.isSoundEnabled = prefs.getBoolean("key_sound_enabled", true)
    }

    fun setSoundEnabled(enabled: Boolean) {
        soundPlayer.isSoundEnabled = enabled
        prefs.edit().putBoolean("key_sound_enabled", enabled).apply()
    }

    fun setCurrentOfficer(name: String) {
        val cleanName = name.trim().ifEmpty { "Duty Officer" }
        _currentOfficer.value = cleanName
        ensureOfficerProfile(cleanName)
        prefs.edit().putString("key_current_officer", cleanName).apply()
    }

    fun setEmergencyContacts(co: String, asp: String, sp: String) {
        _contactCo.value = co
        _contactAsp.value = asp
        _contactSp.value = sp
        prefs.edit()
            .putString("key_co", co)
            .putString("key_asp", asp)
            .putString("key_sp", sp)
            .apply()
    }

    fun clearCelebrationEvent() {
        _celebrationEvent.value = null
    }

    fun createEmergency(
        type: EmergencyType,
        location: String,
        priority: Priority,
        officer: String
    ): EmergencyIncident {
        setCurrentOfficer(officer)
        val now = System.currentTimeMillis()
        val nextSeq = (_incidents.value.size + 1)
        val number = "EM-" + String.format("%03d", nextSeq)

        val items = EmergencyTemplates.getTemplateItems(type)
        val incident = EmergencyIncident(
            id = now,
            number = number,
            type = type,
            location = location,
            priority = priority,
            createdAt = now,
            closedAt = null,
            createdBy = officer,
            items = items,
            notes = emptyList()
        )

        val updatedList = listOf(incident) + _incidents.value
        _incidents.value = updatedList

        // Update officer creation count
        val prof = getOrCreateOfficer(officer)
        val updatedProf = prof.copy(totalCreatedCount = prof.totalCreatedCount + 1)
        updateOfficerProfile(updatedProf)

        saveIncidentsToPrefs(updatedList)

        // Play auditory alert for initiation
        soundPlayer.playInitiationBeep()

        return incident
    }

    fun toggleChecklistItem(incidentId: Long, itemId: String, officerName: String) {
        val currentList = _incidents.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == incidentId }
        if (index == -1) return

        val incident = currentList[index]
        if (incident.isClosed) return

        val wasAlreadyComplete = incident.isChecklistComplete

        val updatedItems = incident.items.map { item ->
            if (item.id == itemId) {
                if (item.isDone) {
                    item.copy(completedAt = null, completedBy = null)
                } else {
                    item.copy(completedAt = System.currentTimeMillis(), completedBy = officerName)
                }
            } else item
        }

        val updatedIncident = incident.copy(items = updatedItems)
        currentList[index] = updatedIncident
        _incidents.value = currentList
        saveIncidentsToPrefs(currentList)

        // Check if this action just COMPLETED the entire checklist!
        if (!wasAlreadyComplete && updatedIncident.isChecklistComplete) {
            handleChecklistCompletion(updatedIncident, officerName)
        }
    }

    fun setItemCustomContact(incidentId: Long, itemId: String, contactNumber: String) {
        val currentList = _incidents.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == incidentId }
        if (index == -1) return

        val incident = currentList[index]
        val updatedItems = incident.items.map { item ->
            if (item.id == itemId) {
                item.copy(customContactNumber = contactNumber)
            } else item
        }
        currentList[index] = incident.copy(items = updatedItems)
        _incidents.value = currentList
        saveIncidentsToPrefs(currentList)
    }

    fun addNote(incidentId: Long, noteText: String, officerName: String) {
        val text = noteText.trim()
        if (text.isEmpty()) return

        val currentList = _incidents.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == incidentId }
        if (index == -1) return

        val incident = currentList[index]
        val newNote = EmergencyNote(
            id = "note_${System.currentTimeMillis()}",
            text = text,
            timestamp = System.currentTimeMillis(),
            author = officerName
        )

        val updatedNotes = incident.notes + newNote
        currentList[index] = incident.copy(notes = updatedNotes)
        _incidents.value = currentList
        saveIncidentsToPrefs(currentList)
    }

    fun closeEmergency(incidentId: Long) {
        val currentList = _incidents.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == incidentId }
        if (index == -1) return

        val incident = currentList[index]
        if (!incident.isChecklistComplete) return // gate closing behind checklist completion

        currentList[index] = incident.copy(closedAt = System.currentTimeMillis())
        _incidents.value = currentList
        saveIncidentsToPrefs(currentList)
    }

    fun reopenEmergency(incidentId: Long) {
        val currentList = _incidents.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == incidentId }
        if (index == -1) return

        currentList[index] = currentList[index].copy(closedAt = null)
        _incidents.value = currentList
        saveIncidentsToPrefs(currentList)
    }

    fun deleteEmergency(incidentId: Long) {
        val updated = _incidents.value.filterNot { it.id == incidentId }
        _incidents.value = updated
        saveIncidentsToPrefs(updated)
    }

    private fun handleChecklistCompletion(incident: EmergencyIncident, officerName: String) {
        val now = System.currentTimeMillis()
        val elapsedMinutes = incident.elapsedMinutes(now)
        val isWithinSla = elapsedMinutes <= incident.priority.slaMinutes

        val prof = getOrCreateOfficer(officerName)
        val newCompletedCount = prof.completedChecklistsCount + 1
        val newOnTimeCount = if (isWithinSla) prof.onTimeCompletedCount + 1 else prof.onTimeCompletedCount
        val xpGain = 100 + if (isWithinSla) 50 else 0
        val newXp = prof.xp + xpGain

        val currentBadges = prof.badges.toMutableSet()
        val newlyUnlocked = mutableListOf<OfficerBadge>()

        // Check Badge criteria
        if (newCompletedCount >= 1 && !currentBadges.contains(OfficerBadge.FIRST_RESPONDER.id)) {
            currentBadges.add(OfficerBadge.FIRST_RESPONDER.id)
            newlyUnlocked.add(OfficerBadge.FIRST_RESPONDER)
        }
        if (isWithinSla && !currentBadges.contains(OfficerBadge.RAPID_DISPATCH.id)) {
            currentBadges.add(OfficerBadge.RAPID_DISPATCH.id)
            newlyUnlocked.add(OfficerBadge.RAPID_DISPATCH)
        }
        if (newCompletedCount >= 5 && !currentBadges.contains(OfficerBadge.SHIELD_OF_HATHRAS.id)) {
            currentBadges.add(OfficerBadge.SHIELD_OF_HATHRAS.id)
            newlyUnlocked.add(OfficerBadge.SHIELD_OF_HATHRAS)
        }
        if (newCompletedCount >= 10 && !currentBadges.contains(OfficerBadge.MASTER_CONTROLLER.id)) {
            currentBadges.add(OfficerBadge.MASTER_CONTROLLER.id)
            newlyUnlocked.add(OfficerBadge.MASTER_CONTROLLER)
        }
        if (incident.type == EmergencyType.FIRE && !currentBadges.contains(OfficerBadge.FIRE_WARDEN.id)) {
            currentBadges.add(OfficerBadge.FIRE_WARDEN.id)
            newlyUnlocked.add(OfficerBadge.FIRE_WARDEN)
        }
        if ((incident.type == EmergencyType.ACCIDENT || incident.type == EmergencyType.MEDICAL) &&
            !currentBadges.contains(OfficerBadge.LIFE_SAVER.id)
        ) {
            currentBadges.add(OfficerBadge.LIFE_SAVER.id)
            newlyUnlocked.add(OfficerBadge.LIFE_SAVER)
        }
        if (newCompletedCount >= 3 && newOnTimeCount == newCompletedCount &&
            !currentBadges.contains(OfficerBadge.PERFECT_RECORD.id)
        ) {
            currentBadges.add(OfficerBadge.PERFECT_RECORD.id)
            newlyUnlocked.add(OfficerBadge.PERFECT_RECORD)
        }

        val updatedProf = prof.copy(
            completedChecklistsCount = newCompletedCount,
            onTimeCompletedCount = newOnTimeCount,
            xp = newXp,
            badges = currentBadges
        )
        updateOfficerProfile(updatedProf)

        // Play celebratory audio jingle and haptics
        soundPlayer.playCelebrationJingle()

        // Post celebration event for UI to display confetti and celebratory dialog
        _celebrationEvent.value = ChecklistCelebrationEvent(
            incident = incident,
            officerName = officerName,
            completedCountForOfficer = newCompletedCount,
            timeMinutes = elapsedMinutes,
            isWithinSla = isWithinSla,
            newlyUnlockedBadges = newlyUnlocked
        )
    }

    private fun getOrCreateOfficer(name: String): OfficerProfile {
        return _officerProfiles.value[name] ?: OfficerProfile(name = name)
    }

    private fun ensureOfficerProfile(name: String) {
        if (!_officerProfiles.value.containsKey(name)) {
            val map = _officerProfiles.value.toMutableMap()
            map[name] = OfficerProfile(name = name)
            _officerProfiles.value = map
            saveOfficersToPrefs(map)
        }
    }

    private fun updateOfficerProfile(profile: OfficerProfile) {
        val map = _officerProfiles.value.toMutableMap()
        map[profile.name] = profile
        _officerProfiles.value = map
        saveOfficersToPrefs(map)
    }

    // --- Persistence via JSON in SharedPreferences ---

    private fun loadFromPrefs() {
        val officerName = prefs.getString("key_current_officer", "SI Rajesh Kumar") ?: "SI Rajesh Kumar"
        _currentOfficer.value = officerName

        _contactCo.value = prefs.getString("key_co", "9454401258") ?: "9454401258"
        _contactAsp.value = prefs.getString("key_asp", EmergencyTemplates.DEFAULT_ASP_NUMBER) ?: EmergencyTemplates.DEFAULT_ASP_NUMBER
        _contactSp.value = prefs.getString("key_sp", EmergencyTemplates.DEFAULT_SP_NUMBER) ?: EmergencyTemplates.DEFAULT_SP_NUMBER

        // Load officers
        val officersJsonStr = prefs.getString("key_officers_json", null)
        val loadedOfficers = mutableMapOf<String, OfficerProfile>()
        if (!officersJsonStr.isNullOrEmpty()) {
            try {
                val array = JSONArray(officersJsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val name = obj.getString("name")
                    val completed = obj.optInt("completed", 0)
                    val onTime = obj.optInt("onTime", 0)
                    val created = obj.optInt("created", 0)
                    val xp = obj.optInt("xp", 0)
                    val badgesArr = obj.optJSONArray("badges")
                    val badgesSet = mutableSetOf<String>()
                    if (badgesArr != null) {
                        for (j in 0 until badgesArr.length()) {
                            badgesSet.add(badgesArr.getString(j))
                        }
                    }
                    loadedOfficers[name] = OfficerProfile(
                        name = name,
                        completedChecklistsCount = completed,
                        onTimeCompletedCount = onTime,
                        totalCreatedCount = created,
                        xp = xp,
                        badges = badgesSet
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (!loadedOfficers.containsKey(officerName)) {
            loadedOfficers[officerName] = OfficerProfile(
                name = officerName,
                completedChecklistsCount = 2,
                onTimeCompletedCount = 2,
                totalCreatedCount = 3,
                xp = 300,
                badges = setOf(OfficerBadge.FIRST_RESPONDER.id, OfficerBadge.RAPID_DISPATCH.id)
            )
        }
        _officerProfiles.value = loadedOfficers

        // Load incidents
        val incidentsJsonStr = prefs.getString("key_incidents_json", null)
        if (!incidentsJsonStr.isNullOrEmpty()) {
            try {
                val array = JSONArray(incidentsJsonStr)
                val incidentsList = mutableListOf<EmergencyIncident>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    incidentsList.add(parseIncident(obj))
                }
                _incidents.value = incidentsList
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Seed with realistic demo incidents if empty
        seedSampleData(officerName)
    }

    private fun seedSampleData(officerName: String) {
        val now = System.currentTimeMillis()
        val fireItems = EmergencyTemplates.getTemplateItems(EmergencyType.FIRE).mapIndexed { i, item ->
            if (i < 3) item.copy(completedAt = now - 180000L, completedBy = officerName) else item
        }
        val sampleFire = EmergencyIncident(
            id = now - 600000L,
            number = "EM-001",
            type = EmergencyType.FIRE,
            location = "Talab Chauraha, Hathras",
            priority = Priority.HIGH,
            createdAt = now - 600000L,
            closedAt = null,
            createdBy = officerName,
            items = fireItems,
            notes = listOf(
                EmergencyNote(
                    id = "n1",
                    text = "दमकल वाहन रवाना, 2 गाड़ियाँ मौके की ओर।",
                    timestamp = now - 500000L,
                    author = officerName
                )
            )
        )

        val accItems = EmergencyTemplates.getTemplateItems(EmergencyType.ACCIDENT).map { item ->
            item.copy(completedAt = now - 3600000L, completedBy = officerName)
        }
        val sampleAcc = EmergencyIncident(
            id = now - 7200000L,
            number = "EM-002",
            type = EmergencyType.ACCIDENT,
            location = "Agra Road, Hathras",
            priority = Priority.MEDIUM,
            createdAt = now - 7200000L,
            closedAt = now - 3600000L,
            createdBy = officerName,
            items = accItems,
            notes = listOf(
                EmergencyNote(
                    id = "n2",
                    text = "घायलों को जिला अस्पताल भेजा गया, स्थिति सामान्य।",
                    timestamp = now - 3700000L,
                    author = officerName
                )
            )
        )

        val initial = listOf(sampleFire, sampleAcc)
        _incidents.value = initial
        saveIncidentsToPrefs(initial)
    }

    private fun saveIncidentsToPrefs(list: List<EmergencyIncident>) {
        val array = JSONArray()
        list.forEach { incident ->
            val obj = JSONObject().apply {
                put("id", incident.id)
                put("number", incident.number)
                put("type", incident.type.key)
                put("location", incident.location)
                put("priority", incident.priority.key)
                put("createdAt", incident.createdAt)
                if (incident.closedAt != null) put("closedAt", incident.closedAt)
                put("createdBy", incident.createdBy)

                val itemsArr = JSONArray()
                incident.items.forEach { itm ->
                    val itmObj = JSONObject().apply {
                        put("id", itm.id)
                        put("title", itm.title)
                        put("contactKey", itm.contactKey)
                        if (itm.customContactNumber != null) put("customContact", itm.customContactNumber)
                        if (itm.completedAt != null) put("completedAt", itm.completedAt)
                        if (itm.completedBy != null) put("completedBy", itm.completedBy)
                    }
                    itemsArr.put(itmObj)
                }
                put("items", itemsArr)

                val notesArr = JSONArray()
                incident.notes.forEach { n ->
                    val nObj = JSONObject().apply {
                        put("id", n.id)
                        put("text", n.text)
                        put("timestamp", n.timestamp)
                        put("author", n.author)
                    }
                    notesArr.put(nObj)
                }
                put("notes", notesArr)
            }
            array.put(obj)
        }
        prefs.edit().putString("key_incidents_json", array.toString()).apply()
    }

    private fun saveOfficersToPrefs(map: Map<String, OfficerProfile>) {
        val array = JSONArray()
        map.values.forEach { prof ->
            val obj = JSONObject().apply {
                put("name", prof.name)
                put("completed", prof.completedChecklistsCount)
                put("onTime", prof.onTimeCompletedCount)
                put("created", prof.totalCreatedCount)
                put("xp", prof.xp)
                val badgesArr = JSONArray()
                prof.badges.forEach { badgesArr.put(it) }
                put("badges", badgesArr)
            }
            array.put(obj)
        }
        prefs.edit().putString("key_officers_json", array.toString()).apply()
    }

    private fun parseIncident(obj: JSONObject): EmergencyIncident {
        val itemsList = mutableListOf<ChecklistItem>()
        val itemsArr = obj.getJSONArray("items")
        for (i in 0 until itemsArr.length()) {
            val itmObj = itemsArr.getJSONObject(i)
            itemsList.add(
                ChecklistItem(
                    id = itmObj.getString("id"),
                    title = itmObj.getString("title"),
                    contactKey = itmObj.optString("contactKey", ""),
                    customContactNumber = if (itmObj.has("customContact")) itmObj.getString("customContact") else null,
                    completedAt = if (itmObj.has("completedAt")) itmObj.getLong("completedAt") else null,
                    completedBy = if (itmObj.has("completedBy")) itmObj.getString("completedBy") else null
                )
            )
        }

        val notesList = mutableListOf<EmergencyNote>()
        val notesArr = obj.optJSONArray("notes")
        if (notesArr != null) {
            for (i in 0 until notesArr.length()) {
                val nObj = notesArr.getJSONObject(i)
                notesList.add(
                    EmergencyNote(
                        id = nObj.getString("id"),
                        text = nObj.getString("text"),
                        timestamp = nObj.getLong("timestamp"),
                        author = nObj.getString("author")
                    )
                )
            }
        }

        return EmergencyIncident(
            id = obj.getLong("id"),
            number = obj.getString("number"),
            type = EmergencyType.fromKey(obj.getString("type")),
            location = obj.getString("location"),
            priority = Priority.fromKey(obj.getString("priority")),
            createdAt = obj.getLong("createdAt"),
            closedAt = if (obj.has("closedAt")) obj.getLong("closedAt") else null,
            createdBy = obj.getString("createdBy"),
            items = itemsList,
            notes = notesList
        )
    }

    fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("officer", _currentOfficer.value)
        val incArray = JSONArray()
        _incidents.value.forEach { inc ->
            // Re-use serialization logic
            val obj = JSONObject().apply {
                put("id", inc.id)
                put("number", inc.number)
                put("type", inc.type.key)
                put("location", inc.location)
                put("priority", inc.priority.key)
                put("createdAt", inc.createdAt)
                if (inc.closedAt != null) put("closedAt", inc.closedAt)
                put("createdBy", inc.createdBy)
                val itms = JSONArray()
                inc.items.forEach { itm ->
                    itms.put(JSONObject().apply {
                        put("id", itm.id)
                        put("title", itm.title)
                        put("contactKey", itm.contactKey)
                        if (itm.customContactNumber != null) put("customContact", itm.customContactNumber)
                        if (itm.completedAt != null) put("completedAt", itm.completedAt)
                        if (itm.completedBy != null) put("completedBy", itm.completedBy)
                    })
                }
                put("items", itms)
                val nts = JSONArray()
                inc.notes.forEach { n ->
                    nts.put(JSONObject().apply {
                        put("id", n.id)
                        put("text", n.text)
                        put("timestamp", n.timestamp)
                        put("author", n.author)
                    })
                }
                put("notes", nts)
            }
            incArray.put(obj)
        }
        root.put("incidents", incArray)
        return root.toString(2)
    }

    fun restoreBackupJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val incArr = root.getJSONArray("incidents")
            val restored = mutableListOf<EmergencyIncident>()
            for (i in 0 until incArr.length()) {
                restored.add(parseIncident(incArr.getJSONObject(i)))
            }
            if (root.has("officer")) {
                setCurrentOfficer(root.getString("officer"))
            }
            _incidents.value = restored
            saveIncidentsToPrefs(restored)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun exportCsv(): String {
        val sb = StringBuilder()
        sb.append("IncidentNo,Type,Location,Priority,Status,Created,Closed,DoneItems,TotalItems,Notes\n")
        _incidents.value.forEach { e ->
            val status = if (e.isClosed) "Closed" else if (e.completedItemsCount > 0) "In Progress" else "Open"
            val notesStr = e.notes.joinToString(" | ") { it.text.replace(",", ";") }
            sb.append("\"${e.number}\",\"${e.type.displayName}\",\"${e.location.replace("\"", "\"\"")}\",\"${e.priority.label}\",\"$status\",\"${e.createdAt}\",\"${e.closedAt ?: ""}\",${e.completedItemsCount},${e.totalItemsCount},\"$notesStr\"\n")
        }
        return sb.toString()
    }
}
