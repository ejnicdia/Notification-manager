package com.emilio.notificationmanager.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

/* Data structure that defines a group of apps to silence/block together */
data class NotificationGroup(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var packageNames: Set<String>,
    var blockedUntil: Long = 0L,  // Expiration timestamp in milliseconds
    var silencedUntil: Long = 0L
)

enum class TriggerType { TEMPORAL, POR_NOTIFICACION }

enum class TriggerAction { SILENCE, BLOCK }

/* Definition of conditions that automatically activate a silence or block rule */
data class Trigger(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var packageNames: Set<String> = emptySet(),
    var groupIds: Set<String> = emptySet(),
    var triggerType: TriggerType = TriggerType.TEMPORAL,
    var action: TriggerAction = TriggerAction.SILENCE,
    var durationMs: Long = 0L,
    var targetHour: Int? = null,
    var targetMinute: Int? = null,
    var keyword: String? = null,
    var lastFiredTimestamp: Long? = null // Prevents firing the same temporal trigger twice a day
)

enum class ThemePreference {
    SYSTEM, LIGHT, DARK
}

/* Main class that handles writing and reading logic to the device SharedPreferences */
class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("block_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // 1. Theme Preference Logic 
    fun setThemePreference(theme: ThemePreference) {
        prefs.edit().putString("theme_pref", theme.name).apply()
    }
    
    /* Function that returns the current theme preference, defaulting to SYSTEM */
    fun getThemePreference(): ThemePreference {
        val themeStr = prefs.getString("theme_pref", ThemePreference.SYSTEM.name)
        return try {
            ThemePreference.valueOf(themeStr ?: ThemePreference.SYSTEM.name)
        } catch (e: Exception) {
            ThemePreference.SYSTEM
        }
    }

    // 2. BLOCKED LOGIC (No notification at all) 
    fun blockAppUntil(packageName: String, timestampMs: Long) {
        prefs.edit().putLong(packageName + "_blocked", timestampMs).apply()
    }

    fun getBlockedUntil(packageName: String): Long {
        return prefs.getLong(packageName + "_blocked", 0L)
    }

    /* Checks if the given app has an active block timer */
    fun isAppBlockedLocally(packageName: String): Boolean {
        val blockedUntil = getBlockedUntil(packageName)
        return System.currentTimeMillis() < blockedUntil
    }
    
    // 3. SILENCED LOGIC (Notification appears, but no sound/vibration if handled)
    fun silenceAppUntil(packageName: String, timestampMs: Long) {
        prefs.edit().putLong(packageName + "_silenced", timestampMs).apply()
    }

    fun getSilencedUntil(packageName: String): Long {
        return prefs.getLong(packageName + "_silenced", 0L)
    }

    /* Checks if the given app has an active silent timer */
    fun isAppSilencedLocally(packageName: String): Boolean {
        val silencedUntil = getSilencedUntil(packageName)
        return System.currentTimeMillis() < silencedUntil
    }

    // --- UNBLOCK/UNSILENCE ---
    fun unblockAndUnsilenceApp(packageName: String) {
        prefs.edit()
            .remove(packageName + "_blocked")
            .remove(packageName + "_silenced")
            .apply()
    }
    
    // --- GROUPS DATA ---
    fun getGroups(): List<NotificationGroup> {
        val json = prefs.getString("notification_groups", "[]")
        val type = object : TypeToken<List<NotificationGroup>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun saveGroups(groups: List<NotificationGroup>) {
        val json = gson.toJson(groups)
        prefs.edit().putString("notification_groups", json).apply()
    }
    
    fun saveGroup(group: NotificationGroup) {
        val groups = getGroups().toMutableList()
        val index = groups.indexOfFirst { it.id == group.id }
        if (index != -1) {
            groups[index] = group
        } else {
            groups.add(group)
        }
        saveGroups(groups)
    }
    
    fun deleteGroup(groupId: String) {
        val groups = getGroups().filter { it.id != groupId }
        saveGroups(groups)
    }

    // --- TRIGGERS DATA ---
    fun getTriggers(): List<Trigger> {
        val json = prefs.getString("notification_triggers", "[]")
        val type = object : TypeToken<List<Trigger>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun saveTriggers(triggers: List<Trigger>) {
        val json = gson.toJson(triggers)
        prefs.edit().putString("notification_triggers", json).apply()
    }
    
    fun saveTrigger(trigger: Trigger) {
        val triggers = getTriggers().toMutableList()
        val index = triggers.indexOfFirst { it.id == trigger.id }
        if (index != -1) {
            triggers[index] = trigger
        } else {
            triggers.add(trigger)
        }
        saveTriggers(triggers)
    }
    
    fun deleteTrigger(triggerId: String) {
        val triggers = getTriggers().filter { it.id != triggerId }
        saveTriggers(triggers)
    }
    
    // --- AGGREGATED STATE CHECKS ---
    fun isAppBlockedByGroup(packageName: String): Boolean {
        val groups = getGroups()
        val currentTime = System.currentTimeMillis()
        for (group in groups) {
            if (group.packageNames.contains(packageName) && currentTime < group.blockedUntil) {
                return true
            }
        }
        return false
    }
    
    fun isAppSilencedByGroup(packageName: String): Boolean {
        val groups = getGroups()
        val currentTime = System.currentTimeMillis()
        for (group in groups) {
            if (group.packageNames.contains(packageName) && currentTime < group.silencedUntil) {
                return true
            }
        }
        return false
    }
    
    fun isAppBlocked(packageName: String): Boolean {
        return isAppBlockedLocally(packageName) || isAppBlockedByGroup(packageName)
    }
    
    fun isAppSilenced(packageName: String): Boolean {
        return isAppSilencedLocally(packageName) || isAppSilencedByGroup(packageName)
    }
}
