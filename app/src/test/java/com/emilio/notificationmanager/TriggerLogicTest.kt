package com.emilio.notificationmanager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.emilio.notificationmanager.data.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TriggerLogicTest {

    private lateinit var context: Context
    private lateinit var appPreferences: AppPreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        appPreferences = AppPreferences(context)
        // Clear preferences
        context.getSharedPreferences("block_prefs", Context.MODE_PRIVATE).edit().clear().apply()
    }

    @Test
    fun testSaveAndRetrieveTriggers() {
        val trigger1 = Trigger(
            name = "Test Temporal",
            packageNames = setOf("com.test.app1"),
            triggerType = TriggerType.TEMPORAL,
            action = TriggerAction.BLOCK,
            durationMs = 60000L,
            targetHour = 14,
            targetMinute = 30
        )
        val trigger2 = Trigger(
            name = "Test Notificacion",
            groupIds = setOf("group123"),
            triggerType = TriggerType.POR_NOTIFICACION,
            action = TriggerAction.SILENCE,
            durationMs = 120000L,
            keyword = "urgente"
        )

        appPreferences.saveTrigger(trigger1)
        appPreferences.saveTrigger(trigger2)

        val retrieved = appPreferences.getTriggers()
        assertEquals(2, retrieved.size)
        
        val r1 = retrieved.find { it.id == trigger1.id }
        assertNotNull(r1)
        assertEquals("Test Temporal", r1!!.name)
        assertEquals(TriggerType.TEMPORAL, r1.triggerType)
        assertTrue(r1.packageNames.contains("com.test.app1"))

        val r2 = retrieved.find { it.id == trigger2.id }
        assertNotNull(r2)
        assertEquals("Test Notificacion", r2!!.name)
        assertEquals(TriggerAction.SILENCE, r2.action)
        assertTrue(r2.groupIds.contains("group123"))
    }

    @Test
    fun testDeleteTrigger() {
        val trigger = Trigger(name = "Test", durationMs = 1000L)
        appPreferences.saveTrigger(trigger)
        assertEquals(1, appPreferences.getTriggers().size)

        appPreferences.deleteTrigger(trigger.id)
        assertTrue(appPreferences.getTriggers().isEmpty())
    }
}
