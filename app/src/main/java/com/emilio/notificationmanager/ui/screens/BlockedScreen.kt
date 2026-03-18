package com.emilio.notificationmanager.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emilio.notificationmanager.AppInfo
import com.emilio.notificationmanager.data.AppPreferences
import com.emilio.notificationmanager.ui.components.AppListItem
import kotlinx.coroutines.delay

@Composable
fun BlockedScreen(context: Context, appPreferences: AppPreferences) {
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

    // Aggregate individual blocked apps
    val allPackages = remember { 
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        pm.queryIntentActivities(intent, 0).map { it.activityInfo.packageName }
    }
    
    val blockedIndividuals = remember(refreshTrigger) {
        allPackages.filter { appPreferences.getBlockedUntil(it) > currentTime }
    }

    val blockedGroups = remember(refreshTrigger) {
        appPreferences.getGroups().filter { it.blockedUntil > currentTime }
    }
    
    if (blockedIndividuals.isEmpty() && blockedGroups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Text("No hay aplicaciones ni grupos bloqueados", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        if (blockedGroups.isNotEmpty()) {
            item {
                Text("Grupos Bloqueados", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            }
            items(blockedGroups) { group ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(group.name, style = MaterialTheme.typography.bodyLarge)
                            val remainingMins = ((group.blockedUntil - System.currentTimeMillis()) / 60000).coerceAtLeast(1)
                            Text("${group.packageNames.size} apps • ${remainingMins} min restantes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        OutlinedButton(onClick = {
                            val newGroup = group.copy(blockedUntil = 0L)
                            appPreferences.saveGroup(newGroup)
                            refreshTrigger++
                        }) {
                            Text("Quitar")
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        if (blockedIndividuals.isNotEmpty()) {
            item {
                Text("Aplicaciones Bloqueadas (Individual)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            }
            items(blockedIndividuals) { pkg ->
                val appInfo = try {
                    val info = pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA)
                    AppInfo(
                        packageName = pkg,
                        name = pm.getApplicationLabel(info).toString(),
                        icon = pm.getApplicationIcon(info)
                    )
                } catch (e: Exception) { null }

                if (appInfo != null) {
                    val blockedUntil = appPreferences.getBlockedUntil(pkg)
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        AppListItem(
                            app = appInfo,
                            isBlocked = true,
                            actionText = "Bloqueado",
                            expirationTime = blockedUntil,
                            rightContent = {
                                OutlinedButton(onClick = {
                                    appPreferences.unblockAndUnsilenceApp(pkg)
                                    refreshTrigger++
                                }) {
                                    Text("Quitar")
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
