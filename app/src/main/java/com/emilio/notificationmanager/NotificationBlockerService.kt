package com.emilio.notificationmanager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import com.emilio.notificationmanager.data.AppPreferences
import kotlinx.coroutines.*
import java.util.Calendar

class NotificationBlockerService : NotificationListenerService() {

    private lateinit var appPreferences: AppPreferences
    private lateinit var notificationManager: NotificationManager
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val SILENT_CHANNEL_ID = "silent_channel"
        const val SILENT_CHANNEL_NAME = "Notificaciones Silenciadas"
        const val PERSISTENT_CHANNEL_ID = "persistent_channel"
        const val PERSISTENT_CHANNEL_NAME = "Estado de Bloqueo General"
        const val PERSISTENT_NOTIFICATION_ID = 9999
    }

    override fun onCreate() {
        super.onCreate()
        appPreferences = AppPreferences(applicationContext)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createSilentChannel()
        createPersistentChannel()
        startBackgroundLoop()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    /* Create the persistent notification channel for Android 8.0 and above */
    private fun createPersistentChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PERSISTENT_CHANNEL_ID,
                PERSISTENT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows if there are active blocks or silences"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /* Endless loop that checks timers and temporal triggers every 30 seconds */
    private fun startBackgroundLoop() {
        serviceScope.launch {
            while (isActive) {
                checkAndUpdatePersistentNotification()
                checkAndFireTemporalTriggers()
                delay(30000L) // Wait 30 seconds before next check
            }
        }
    }

    /* Shows or hides the persistent notification based on active timers */
    private suspend fun checkAndUpdatePersistentNotification() {
        val currentTime = System.currentTimeMillis()
        var activeCount = 0

        // 1. Check if any groups have active timers
        val groups = appPreferences.getGroups()
        activeCount += groups.count { it.blockedUntil > currentTime || it.silencedUntil > currentTime }

        // 2. Check individual apps timers
        val prefs = applicationContext.getSharedPreferences("block_prefs", Context.MODE_PRIVATE)
        val allKeys = prefs.all.keys
        val activeIndividualKeys = allKeys.filter { 
            (it.endsWith("_blocked_until") || it.endsWith("_silenced_until")) && 
            prefs.getLong(it, 0L) > currentTime 
        }
        activeCount += activeIndividualKeys.size

        // 3. Show or hide notification depending on the counter
        if (activeCount > 0) {
            showPersistentNotification(activeCount)
        } else {
            notificationManager.cancel(PERSISTENT_NOTIFICATION_ID)
        }
    }

    /* Build and show the persistent notification with the Action Button */
    private fun showPersistentNotification(activeCount: Int) {
        val intent = Intent(this, DisableTimersReceiver::class.java).apply {
            action = "com.emilio.notificationmanager.DISABLE_ALL_TIMERS"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, PERSISTENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.persistent_title))
            .setContentText(getString(R.string.persistent_text, activeCount))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(0, getString(R.string.btn_deactivate_all), pendingIntent)

        notificationManager.notify(PERSISTENT_NOTIFICATION_ID, builder.build())
    }

    /* Function that reads the daily Temporal Triggers and activates the rule if the hour and minute matches */
    private suspend fun checkAndFireTemporalTriggers() {
        val currentTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        
        // 1. Get the list of Temporal triggers with an exact configured time
        val triggersStrp = appPreferences.getTriggers()
        val temporalTriggers = triggersStrp.filter { it.triggerType == com.emilio.notificationmanager.data.TriggerType.TEMPORAL && it.targetHour != null && it.targetMinute != null }
        
        var triggersUpdated = false
        temporalTriggers.forEach { trigger ->
            val startOfToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val lastFired = trigger.lastFiredTimestamp ?: 0L

            // 2. Only execute if time matches and we haven't fired today
            if (currentHour == trigger.targetHour && currentMinute == trigger.targetMinute && lastFired < startOfToday) {
                
                val expirationTime = currentTime + trigger.durationMs
                
                // 3. Apply action to all assigned Apps
                trigger.packageNames.forEach { pkg ->
                    if (trigger.action == com.emilio.notificationmanager.data.TriggerAction.BLOCK) {
                        appPreferences.blockAppUntil(pkg, expirationTime)
                        appPreferences.silenceAppUntil(pkg, 0L)
                    } else {
                        appPreferences.silenceAppUntil(pkg, expirationTime)
                        appPreferences.blockAppUntil(pkg, 0L)
                    }
                }
                
                // 4. Apply action to all assigned Groups
                val currentGroups = appPreferences.getGroups()
                trigger.groupIds.forEach { groupId ->
                    currentGroups.find { it.id == groupId }?.let { group ->
                        val updated = if (trigger.action == com.emilio.notificationmanager.data.TriggerAction.BLOCK) {
                            group.copy(blockedUntil = expirationTime, silencedUntil = 0L)
                        } else {
                            group.copy(silencedUntil = expirationTime, blockedUntil = 0L)
                        }
                        appPreferences.saveGroup(updated)
                    }
                }

                // 5. Update last fired timestamp
                trigger.lastFiredTimestamp = currentTime
                appPreferences.saveTrigger(trigger)
                triggersUpdated = true
            }
        }
        
    }

    /* Create the silent notification channel where we repost muted notifications */
    private fun createSilentChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SILENT_CHANNEL_ID,
                SILENT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW  // No sound, no vibration
            ).apply {
                setSound(null, null)
                enableVibration(false)
                description = "Channel for notifications silenced by Notification Manager"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /* Main function that intercepts incoming notifications from the system */
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        // Don't intercept our own notifications (e.g. reposted silent ones)
        if (packageName == applicationContext.packageName) return

        // 1. Prepare values to search for keyword triggers
        val notificationText = sbn.notification.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val notificationTitle = sbn.notification.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val fullContent = "$notificationTitle $notificationText"

        // 2. Process active Notification triggers
        val currentTime = System.currentTimeMillis()
        val triggers = appPreferences.getTriggers()
        triggers.filter { it.triggerType == com.emilio.notificationmanager.data.TriggerType.POR_NOTIFICACION }
            .forEach { trigger ->
                val appInTrigger = trigger.packageNames.contains(packageName)
                val inGroup = trigger.groupIds.any { groupId ->
                    appPreferences.getGroups().find { it.id == groupId }?.packageNames?.contains(packageName) == true
                }
                // 3. Match keyword if one is defined in the trigger
                val keywordMatched = if (trigger.keyword.isNullOrBlank()) {
                    true
                } else {
                    val kw = trigger.keyword!!
                    val textToSearch = if (trigger.caseSensitive) fullContent else fullContent.lowercase()
                    val searchKw = if (trigger.caseSensitive) kw else kw.lowercase()

                    if (trigger.exactWord) {
                        val regex = Regex("\\b${Regex.escape(searchKw)}\\b")
                        regex.containsMatchIn(textToSearch)
                    } else {
                        textToSearch.contains(searchKw)
                    }
                }
                
                if ((appInTrigger || inGroup) && keywordMatched) {
                    // 4. Activate this trigger and calculate expiration time
                    val expirationTime = currentTime + trigger.durationMs
                    when (trigger.action) {
                        com.emilio.notificationmanager.data.TriggerAction.BLOCK -> {
                            appPreferences.blockAppUntil(packageName, expirationTime)
                            // Clear silenced if switching to block
                            appPreferences.silenceAppUntil(packageName, 0L)
                        }
                        com.emilio.notificationmanager.data.TriggerAction.SILENCE -> {
                            appPreferences.silenceAppUntil(packageName, expirationTime)
                            appPreferences.blockAppUntil(packageName, 0L)
                        }
                    }
                }
            }

        // 5. Check if the app is currently blocked or silenced
        when {
            appPreferences.isAppBlocked(packageName) -> {
                Log.d("NotificationBlocker", "Blocking notification from: $packageName")
                cancelNotification(sbn.key)
            }

            appPreferences.isAppSilenced(packageName) -> {
                Log.d("NotificationBlocker", "Silencing notification from: $packageName")
                silenceNotification(sbn)
            }
        }
    }

    /* Function that dismisses the noisy original notification and posts a silent copy */
    private fun silenceNotification(sbn: StatusBarNotification) {
        val original = sbn.notification
        val pkg = sbn.packageName

        // 1. Load the original app's icon as the largeIcon so the user still
        // recognizes which app sent the notification.
        val appLargeIcon: Bitmap? = try {
            val drawable = applicationContext.packageManager.getApplicationIcon(pkg)
            drawable.toBitmap(128, 128)
        } catch (e: Exception) {
            null
        }

        // 2. Build a silent copy of the notification
        // CRITICAL: setSmallIcon must use an icon from OUR package, not the source app's
        // package. Using an Icon from a foreign package will cause Android to silently
        // drop the notification. We use our own launcher icon here.
        val silentNotification = Notification.Builder(applicationContext, SILENT_CHANNEL_ID)
            .apply {
                // 3. Set our own icon (always resolvable in our context)
                setSmallIcon(com.emilio.notificationmanager.R.drawable.ic_notification)

                // 4. Show the original app's icon as the large icon
                if (appLargeIcon != null) setLargeIcon(appLargeIcon)

                // 5. Copy textual content and behaviour from the original
                setContentTitle(original.extras?.getCharSequence(Notification.EXTRA_TITLE))
                setContentText(original.extras?.getCharSequence(Notification.EXTRA_TEXT))
                setSubText(original.extras?.getCharSequence(Notification.EXTRA_SUB_TEXT))

                setContentIntent(original.contentIntent)
                setAutoCancel(original.flags and Notification.FLAG_AUTO_CANCEL != 0)
                setOngoing(original.flags and Notification.FLAG_ONGOING_EVENT != 0)
                setColor(original.color)

                // 6. Explicitly disable sound and vibration (channel also ensures this)
                setVibrate(longArrayOf())
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    @Suppress("DEPRECATION")
                    setSound(null)
                }
            }.build()

        // 7. Cancel the noisy original
        cancelNotification(sbn.key)

        // 8. Repost a silent version with a stable ID based on the original
        val notificationId = (pkg.hashCode() xor sbn.id)
        notificationManager.notify(notificationId, silentNotification)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // No-op
    }
}
