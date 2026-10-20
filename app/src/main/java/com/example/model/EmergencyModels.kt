package com.example.model

enum class EmergencyType(val key: String, val displayName: String, val icon: String) {
    FIRE("fire", "आग लगने की घटना (Fire)", "🔥"),
    ACCIDENT("acc", "दुर्घटना (Accident)", "🚗"),
    STAMPEDE("stamp", "भगदड़ (Stampede)", "👥"),
    MEDICAL("med", "मेडिकल Emergency (Medical)", "🏥"),
    LAW_AND_ORDER("law", "कानून-व्यवस्था (Law & Order)", "🛡️"),
    OTHER("oth", "अन्य (Other Incident)", "❗");

    companion object {
        fun fromKey(key: String): EmergencyType =
            entries.find { it.key == key } ?: OTHER
    }
}

enum class Priority(val key: String, val label: String, val slaMinutes: Int, val emoji: String) {
    HIGH("high", "High – अति गंभीर", 15, "🔴"),
    MEDIUM("med", "Medium – मध्यम", 30, "🟠"),
    LOW("low", "Low – सामान्य", 60, "🟢");

    companion object {
        fun fromKey(key: String): Priority =
            entries.find { it.key == key } ?: MEDIUM
    }
}

data class ChecklistItem(
    val id: String,
    val title: String,
    val contactKey: String = "", // "101", "108", "thana", "co", "asp", "sp", or empty
    val customContactNumber: String? = null,
    val completedAt: Long? = null,
    val completedBy: String? = null
) {
    val isDone: Boolean get() = completedAt != null
}

data class EmergencyNote(
    val id: String,
    val text: String,
    val timestamp: Long,
    val author: String
)

data class EmergencyIncident(
    val id: Long,
    val number: String,
    val type: EmergencyType,
    val location: String,
    val priority: Priority,
    val createdAt: Long,
    val closedAt: Long? = null,
    val createdBy: String,
    val items: List<ChecklistItem>,
    val notes: List<EmergencyNote> = emptyList()
) {
    val isClosed: Boolean get() = closedAt != null
    val completedItemsCount: Int get() = items.count { it.isDone }
    val totalItemsCount: Int get() = items.size
    val isChecklistComplete: Boolean get() = items.isNotEmpty() && items.all { it.isDone }

    fun elapsedMinutes(currentTime: Long = System.currentTimeMillis()): Long {
        val endTime = closedAt ?: currentTime
        return ((endTime - createdAt) / 60000L).coerceAtLeast(0L)
    }

    fun isLate(currentTime: Long = System.currentTimeMillis()): Boolean {
        if (isClosed || isChecklistComplete) return false
        return elapsedMinutes(currentTime) > priority.slaMinutes
    }
}

enum class OfficerBadge(
    val id: String,
    val title: String,
    val description: String,
    val icon: String
) {
    FIRST_RESPONDER("first_resp", "First Responder", "Completed 1st emergency checklist", "🎖️"),
    RAPID_DISPATCH("rapid_disp", "Rapid Dispatch", "Completed checklist within SLA target", "⚡"),
    SHIELD_OF_HATHRAS("shield_hathras", "Hathras Shield", "Successfully completed 5 checklists", "🛡️"),
    MASTER_CONTROLLER("master_ctrl", "Master Controller", "Completed 10+ emergency checklists", "👑"),
    FIRE_WARDEN("fire_warden", "Fire Warden", "Resolved a critical Fire emergency", "🔥"),
    LIFE_SAVER("life_saver", "Life Saver", "Completed an Accident or Medical emergency", "🚑"),
    PERFECT_RECORD("perfect_rec", "Elite Operator", "Achieved 100% on-time resolution rate", "🌟")
}

data class OfficerProfile(
    val name: String,
    val completedChecklistsCount: Int = 0,
    val onTimeCompletedCount: Int = 0,
    val totalCreatedCount: Int = 0,
    val xp: Int = 0,
    val badges: Set<String> = emptySet()
) {
    val rankTitle: String
        get() = when {
            completedChecklistsCount >= 15 -> "Chief Emergency Director"
            completedChecklistsCount >= 10 -> "Senior Operations Lead"
            completedChecklistsCount >= 5 -> "Emergency Dispatch Specialist"
            completedChecklistsCount >= 1 -> "Active Control Room Officer"
            else -> "Probationary Dispatcher"
        }

    val onTimePercentage: Int
        get() = if (completedChecklistsCount > 0) {
            ((onTimeCompletedCount.toDouble() / completedChecklistsCount) * 100).toInt()
        } else 100
}
