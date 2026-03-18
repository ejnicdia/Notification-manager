package com.emilio.notificationmanager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.emilio.notificationmanager.data.AppPreferences
import com.emilio.notificationmanager.data.NotificationGroup
import com.emilio.notificationmanager.data.ThemePreference
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificationLogicTest {

    private lateinit var context: Context
    private lateinit var appPreferences: AppPreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        appPreferences = AppPreferences(context)
        // Clear preferences before each test
        context.getSharedPreferences("block_prefs", Context.MODE_PRIVATE).edit().clear().apply()
    }

    // --- Block tests ---
    @Test
    fun testIsAppBlockedLocally_whenBlocked_returnsTrue() {
        val packageName = "com.test.app"
        appPreferences.blockAppUntil(packageName, System.currentTimeMillis() + 5000)
        assertTrue(appPreferences.isAppBlockedLocally(packageName))
        assertTrue(appPreferences.isAppBlocked(packageName))
    }
    
    @Test
    fun testIsAppBlockedLocally_whenNotBlocked_returnsFalse() {
        val packageName = "com.test.app"
        assertFalse(appPreferences.isAppBlockedLocally(packageName))
    }

    @Test
    fun testIsAppBlockedByGroup_whenGroupBlocked_returnsTrue() {
        val group = NotificationGroup(
            name = "Work",
            packageNames = setOf("com.slack", "com.teams")
        )
        appPreferences.saveGroup(group)
        
        // Initially not blocked
        assertFalse(appPreferences.isAppBlockedByGroup("com.slack"))
        
        // Block group
        val blockedGroup = group.copy(blockedUntil = System.currentTimeMillis() + 5000)
        appPreferences.saveGroup(blockedGroup)
        
        assertTrue(appPreferences.isAppBlockedByGroup("com.slack"))
        assertTrue(appPreferences.isAppBlocked("com.slack"))
        
        // App not in group shouldn't be blocked
        assertFalse(appPreferences.isAppBlockedByGroup("com.whatsapp"))
    }
    
    // --- Silence tests ---
    @Test
    fun testIsAppSilencedLocally_whenSilenced_returnsTrue() {
        val packageName = "com.test.silenced"
        appPreferences.silenceAppUntil(packageName, System.currentTimeMillis() + 5000)
        assertTrue(appPreferences.isAppSilencedLocally(packageName))
        assertTrue(appPreferences.isAppSilenced(packageName))
    }
    
    @Test
    fun testIsAppSilencedLocally_whenNotSilenced_returnsFalse() {
        val packageName = "com.test.silenced"
        assertFalse(appPreferences.isAppSilencedLocally(packageName))
    }
    
    @Test
    fun testSilenceAndBlockAreSeparate() {
        val packageName = "com.test.mutual"
        appPreferences.silenceAppUntil(packageName, System.currentTimeMillis() + 5000)
        assertTrue(appPreferences.isAppSilenced(packageName))
        assertFalse(appPreferences.isAppBlocked(packageName))
        
        // Blocking should override silence
        appPreferences.blockAppUntil(packageName, System.currentTimeMillis() + 5000)
        appPreferences.silenceAppUntil(packageName, 0L) // clear silence
        assertTrue(appPreferences.isAppBlocked(packageName))
        assertFalse(appPreferences.isAppSilenced(packageName))
    }
    
    @Test
    fun testIsAppSilencedByGroup_whenGroupSilenced_returnsTrue() {
        val group = NotificationGroup(
            name = "Social",
            packageNames = setOf("com.whatsapp", "com.instagram")
        )
        val silencedGroup = group.copy(silencedUntil = System.currentTimeMillis() + 5000)
        appPreferences.saveGroup(silencedGroup)
        
        assertTrue(appPreferences.isAppSilencedByGroup("com.whatsapp"))
        assertTrue(appPreferences.isAppSilenced("com.whatsapp"))
        assertFalse(appPreferences.isAppSilencedByGroup("com.telegram"))
    }
    
    @Test
    fun testUnblockAndUnsilence_clearsAll() {
        val packageName = "com.test.reset"
        appPreferences.blockAppUntil(packageName, System.currentTimeMillis() + 5000)
        appPreferences.silenceAppUntil(packageName, System.currentTimeMillis() + 5000)
        
        appPreferences.unblockAndUnsilenceApp(packageName)
        
        assertFalse(appPreferences.isAppBlockedLocally(packageName))
        assertFalse(appPreferences.isAppSilencedLocally(packageName))
    }
    
    // --- Theme tests ---
    @Test
    fun testThemePreference_savesAndRetrievesCorrectly() {
        assertEquals(ThemePreference.SYSTEM, appPreferences.getThemePreference())
        
        appPreferences.setThemePreference(ThemePreference.DARK)
        assertEquals(ThemePreference.DARK, appPreferences.getThemePreference())
        
        appPreferences.setThemePreference(ThemePreference.LIGHT)
        assertEquals(ThemePreference.LIGHT, appPreferences.getThemePreference())
    }
}
