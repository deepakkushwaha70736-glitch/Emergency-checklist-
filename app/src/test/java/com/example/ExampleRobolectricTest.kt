package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.EmergencySoundPlayer
import com.example.data.EmergencyRepository
import com.example.model.EmergencyType
import com.example.model.Priority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Emergency Control", appName)
    }

    @Test
    fun `test emergency creation and checklist completion reward trigger`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val soundPlayer = EmergencySoundPlayer(context)
        soundPlayer.isSoundEnabled = false // Silent in tests
        val repository = EmergencyRepository(context, soundPlayer)

        val officer = "SI Rajesh Kumar"
        repository.setCurrentOfficer(officer)

        val incident = repository.createEmergency(
            type = EmergencyType.FIRE,
            location = "Talab Chauraha, Hathras",
            priority = Priority.HIGH,
            officer = officer
        )

        assertNotNull(incident)
        assertTrue(incident.items.isNotEmpty())

        // Check off all items
        incident.items.forEach { item ->
            repository.toggleChecklistItem(incident.id, item.id, officer)
        }

        // Check celebration event was posted!
        val celebration = repository.celebrationEvent.value
        assertNotNull("Celebration event should be triggered when checklist is complete", celebration)
        assertEquals(incident.id, celebration?.incident?.id)
        assertTrue((celebration?.completedCountForOfficer ?: 0) >= 1)
    }
}
