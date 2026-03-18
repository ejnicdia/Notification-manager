package com.emilio.notificationmanager.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.emilio.notificationmanager.AppInfo
import com.emilio.notificationmanager.R
import com.emilio.notificationmanager.ui.components.TimerDialog
import com.emilio.notificationmanager.data.AppPreferences
import com.emilio.notificationmanager.ui.components.AppListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(context: Context, appPreferences: AppPreferences) {
    var searchQuery by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }

    // Load apps on an IO thread so we never block the UI
    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val launcherPackages = resolveInfos.map { it.activityInfo.packageName }.toSet()
            packages.filter { it.packageName in launcherPackages }.map {
                AppInfo(
                    packageName = it.packageName,
                    name = pm.getApplicationLabel(it).toString(),
                    icon = pm.getApplicationIcon(it)
                )
            }.sortedBy { it.name.lowercase() }
        }
        apps = loaded
    }

    val filteredApps = if (searchQuery.isBlank()) {
        apps
    } else {
        apps.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val blockedLabel = stringResource(R.string.status_blocked)
    val silencedLabel = stringResource(R.string.status_silenced)

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text(stringResource(R.string.search_app)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filteredApps, key = { it.packageName }) { app ->
                val isBlockedLocally = appPreferences.isAppBlockedLocally(app.packageName)
                val isSilencedLocally = appPreferences.isAppSilencedLocally(app.packageName)
                val expirationTime = when {
                    isBlockedLocally -> appPreferences.getBlockedUntil(app.packageName)
                    isSilencedLocally -> appPreferences.getSilencedUntil(app.packageName)
                    else -> 0L
                }
                val actionText = when {
                    isBlockedLocally -> blockedLabel
                    isSilencedLocally -> silencedLabel
                    else -> ""
                }
                
                AppListItem(
                    app = app,
                    isBlocked = isBlockedLocally || isSilencedLocally,
                    actionText = actionText,
                    expirationTime = expirationTime,
                    onClick = {
                        selectedApp = app
                        showDialog = true
                    }
                )
            }
        }
    }

    if (showDialog) {
        selectedApp?.let { app ->
            TimerDialog(
                title = stringResource(R.string.configure_for, selectedApp?.name ?: ""),
                onDismiss = { showDialog = false },
                onSilenceSet = { durationMs ->
                    selectedApp?.let { app ->
                        val timestamp = System.currentTimeMillis() + durationMs
                        appPreferences.silenceAppUntil(app.packageName, timestamp)
                        appPreferences.blockAppUntil(app.packageName, 0L) // clear block if silencing
                        showDialog = false
                        refreshTrigger++
                    }
                },
                onBlockSet = { durationMs ->
                    selectedApp?.let { app ->
                        val timestamp = System.currentTimeMillis() + durationMs
                        appPreferences.blockAppUntil(app.packageName, timestamp)
                        appPreferences.silenceAppUntil(app.packageName, 0L) // clear silence if blocking
                        showDialog = false
                        refreshTrigger++
                    }
                },
                onReset = {
                    selectedApp?.let { app ->
                        appPreferences.unblockAndUnsilenceApp(app.packageName)
                        showDialog = false
                        refreshTrigger++
                    }
                },
                showReset = selectedApp?.let { 
                    appPreferences.isAppBlockedLocally(it.packageName) || appPreferences.isAppSilencedLocally(it.packageName)
                } ?: false
            )
        }
    }
}
