package com.emilio.notificationmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.emilio.notificationmanager.data.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DisableTimersReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == "com.emilio.notificationmanager.DISABLE_ALL_TIMERS") {
            val appPreferences = AppPreferences(context)
            
            CoroutineScope(Dispatchers.IO).launch {
                // Clear all individual timers
                val prefs = context.getSharedPreferences("block_prefs", Context.MODE_PRIVATE)
                val editor = prefs.edit()
                
                prefs.all.keys.forEach { key ->
                    if (key.endsWith("_silenced_until") || key.endsWith("_blocked_until")) {
                        editor.remove(key)
                    }
                }
                editor.apply()
                
                // Clear all group timers
                val groups = appPreferences.getGroups().map { it.copy(silencedUntil = 0L) }
                groups.forEach { appPreferences.saveGroup(it) }

                // The background service will automatically detect the state change
                // in the next loop and remove the notification.
            }
        }
    }
}
