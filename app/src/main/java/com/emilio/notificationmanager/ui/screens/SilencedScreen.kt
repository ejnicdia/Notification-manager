package com.emilio.notificationmanager.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.emilio.notificationmanager.AppInfo
import com.emilio.notificationmanager.R
import com.emilio.notificationmanager.data.AppPreferences
import com.emilio.notificationmanager.ui.components.AppListItem
import kotlinx.coroutines.delay

@Composable
fun SilencedScreen(context: Context, appPreferences: AppPreferences) {
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // Auto refresh every minute to update the remaining time
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000)
            refreshTrigger++
        }
    }

    val pm = context.packageManager
    val currentTime = System.currentTimeMillis()

    // Aggregate individual silenced apps
    val allPackages = remember { 
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        pm.queryIntentActivities(intent, 0).map { it.activityInfo.packageName }
    }
    
    val silencedIndividuals = remember(refreshTrigger) {
        allPackages.filter { appPreferences.getSilencedUntil(it) > currentTime }
    }

    val silencedGroups = remember(refreshTrigger) {
        appPreferences.getGroups().filter { it.silencedUntil > currentTime }
    }
    
    if (silencedIndividuals.isEmpty() && silencedGroups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.no_silenced_apps), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        if (silencedGroups.isNotEmpty()) {
            item {
                Text(stringResource(R.string.silenced_groups_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            }
            items(silencedGroups) { group ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(group.name, style = MaterialTheme.typography.bodyLarge)
                            val remainingMins = ((group.silencedUntil - System.currentTimeMillis()) / 60000).coerceAtLeast(1)
                            Text(stringResource(R.string.remaining_min, group.packageNames.size, remainingMins), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        OutlinedButton(onClick = {
                            val newGroup = group.copy(silencedUntil = 0L)
                            appPreferences.saveGroup(newGroup)
                            refreshTrigger++
                        }) {
                            Text(stringResource(R.string.btn_remove))
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        if (silencedIndividuals.isNotEmpty()) {
            item {
                Text(stringResource(R.string.silenced_individual_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            }
            items(silencedIndividuals) { pkg ->
                val appInfo = try {
                    val info = pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA)
                    AppInfo(
                        packageName = pkg,
                        name = pm.getApplicationLabel(info).toString(),
                        icon = pm.getApplicationIcon(info)
                    )
                } catch (e: Exception) { null }

                if (appInfo != null) {
                    val silencedUntil = appPreferences.getSilencedUntil(pkg)
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        AppListItem(
                            app = appInfo,
                            isBlocked = true,
                            actionText = stringResource(R.string.status_silenced),
                            expirationTime = silencedUntil,
                            rightContent = {
                                OutlinedButton(onClick = {
                                    appPreferences.unblockAndUnsilenceApp(pkg)
                                    refreshTrigger++
                                }) {
                                    Text(stringResource(R.string.btn_remove))
                                }
                            },
                            onClick = {}
                        )
                    }
                }
            }
        }
    }
}
