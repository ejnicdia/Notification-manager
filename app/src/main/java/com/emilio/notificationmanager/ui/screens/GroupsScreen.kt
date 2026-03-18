package com.emilio.notificationmanager.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.emilio.notificationmanager.AppInfo
import com.emilio.notificationmanager.R
import com.emilio.notificationmanager.data.AppPreferences
import com.emilio.notificationmanager.data.NotificationGroup
import com.emilio.notificationmanager.ui.components.AppListItem
import com.emilio.notificationmanager.ui.components.TimerDialog
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GroupsScreen(context: Context, appPreferences: AppPreferences) {
    var groups by remember { mutableStateOf(appPreferences.getGroups()) }
    var selectedGroupForTimer by remember { mutableStateOf<NotificationGroup?>(null) }
    var groupToEdit by remember { mutableStateOf<NotificationGroup?>(null) }
    var groupToDelete by remember { mutableStateOf<NotificationGroup?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    // Which group card is expanded to show its apps
    var expandedGroupId by remember { mutableStateOf<String?>(null) }
    // Holds (group, packageName) when user taps Remove — triggers a confirmation dialog
    var appToRemoveFromGroup by remember { mutableStateOf<Pair<NotificationGroup, String>?>(null) }

    val refreshGroups = {
        groups = appPreferences.getGroups()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_group))
            }
        }
    ) { padding ->
        if (groups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_groups), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(groups, key = { it.id }) { group ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable {
                                // Toggle the expanded state when tapping the card body
                                expandedGroupId = if (expandedGroupId == group.id) null else group.id
                            }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            // ---- Header row: name + edit/delete buttons ----
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(group.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                Row {
                                    IconButton(onClick = { groupToEdit = group }) {
                                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.btn_edit))
                                    }
                                    IconButton(onClick = { groupToDelete = group }) {
                                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.btn_delete), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            // ---- Status text ----
                            val isBlocked = System.currentTimeMillis() < group.blockedUntil
                            val isSilenced = System.currentTimeMillis() < group.silencedUntil

                            if (isBlocked) {
                                val remainingMins = ((group.blockedUntil - System.currentTimeMillis()) / 60000).coerceAtLeast(1)
                                Text(stringResource(R.string.blocked_min_remaining, remainingMins), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            } else if (isSilenced) {
                                val remainingMins = ((group.silencedUntil - System.currentTimeMillis()) / 60000).coerceAtLeast(1)
                                Text(stringResource(R.string.silenced_min_remaining, remainingMins), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            } else {
                                Text(stringResource(R.string.apps_tap_to_see, group.packageNames.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            // ---- Expanded app list ----
                            if (expandedGroupId == group.id && group.packageNames.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(4.dp))
                                val pm = context.packageManager
                                group.packageNames.forEach { pkg ->
                                    val appInfo = try {
                                        val info = pm.getApplicationInfo(pkg, android.content.pm.PackageManager.GET_META_DATA)
                                        AppInfo(
                                            packageName = pkg,
                                            name = pm.getApplicationLabel(info).toString(),
                                            icon = pm.getApplicationIcon(info)
                                        )
                                    } catch (e: Exception) { null }

                                    if (appInfo != null) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AppListItem(
                                                app = appInfo,
                                                showDivider = false,
                                                onClick = {},
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedButton(
                                                onClick = {
                                                    // Ask for confirmation before removing
                                                    appToRemoveFromGroup = Pair(group, pkg)
                                                },
                                                modifier = Modifier.padding(start = 4.dp)
                                            ) {
                                                Text(stringResource(R.string.btn_remove))
                                            }
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { selectedGroupForTimer = group },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.configure_actions))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var groupName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(stringResource(R.string.new_group)) },
            text = {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text(stringResource(R.string.group_name_label)) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (groupName.isNotBlank()) {
                        val newGroup = NotificationGroup(name = groupName, packageNames = emptySet())
                        appPreferences.saveGroup(newGroup)
                        refreshGroups()
                        showCreateDialog = false
                        groupToEdit = newGroup
                    }
                }) {
                    Text(stringResource(R.string.btn_create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }

    groupToEdit?.let { group ->
        EditGroupDialog(
            context = context,
            group = group,
            onDismiss = { groupToEdit = null },
            onSave = { updatedGroup ->
                appPreferences.saveGroup(updatedGroup)
                refreshGroups()
                groupToEdit = null
            }
        )
    }

    groupToDelete?.let { group ->
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text(stringResource(R.string.delete_group_title)) },
            text = { Text(stringResource(R.string.delete_group_msg, group.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        appPreferences.deleteGroup(group.id)
                        refreshGroups()
                        groupToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.btn_confirm_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    // Confirmation dialog for removing an app from a group
    appToRemoveFromGroup?.let { (group, pkg) ->
        val appName = try {
            context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(pkg, 0)
            ).toString()
        } catch (e: Exception) { pkg }
        AlertDialog(
            onDismissRequest = { appToRemoveFromGroup = null },
            title = { Text(stringResource(R.string.remove_app_title)) },
            text = { Text(stringResource(R.string.remove_app_msg, appName, group.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = group.copy(packageNames = group.packageNames - pkg)
                        appPreferences.saveGroup(updated)
                        refreshGroups()
                        appToRemoveFromGroup = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.btn_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { appToRemoveFromGroup = null }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }

    selectedGroupForTimer?.let { group ->
        TimerDialog(
            title = stringResource(R.string.configure_group, group.name),
            onDismiss = { selectedGroupForTimer = null },
            onSilenceSet = { durationMs ->
                val updatedGroup = group.copy(
                    silencedUntil = System.currentTimeMillis() + durationMs,
                    blockedUntil = 0L
                )
                appPreferences.saveGroup(updatedGroup)
                refreshGroups()
                selectedGroupForTimer = null
            },
            onBlockSet = { durationMs ->
                val updatedGroup = group.copy(
                    blockedUntil = System.currentTimeMillis() + durationMs,
                    silencedUntil = 0L
                )
                appPreferences.saveGroup(updatedGroup)
                refreshGroups()
                selectedGroupForTimer = null
            },
            onReset = {
                val updatedGroup = group.copy(blockedUntil = 0L, silencedUntil = 0L)
                appPreferences.saveGroup(updatedGroup)
                refreshGroups()
                selectedGroupForTimer = null
            },
            showReset = System.currentTimeMillis() < group.blockedUntil || System.currentTimeMillis() < group.silencedUntil
        )
    }
}

@Composable
fun EditGroupDialog(context: Context, group: NotificationGroup, onDismiss: () -> Unit, onSave: (NotificationGroup) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var editedName by remember { mutableStateOf(group.name) }
    var selectedPackages by remember { mutableStateOf(group.packageNames.toSet()) }

    // Load Apps asynchronously on IO to avoid blocking UI thread
    LaunchedEffect(Unit) {
        val loadedApps = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
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
        apps = loadedApps
    }

    val filteredApps = if (searchQuery.isBlank()) { apps } else { apps.filter { it.name.contains(searchQuery, ignoreCase = true) } }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Editable group name at the top
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text(stringResource(R.string.group_name_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    singleLine = true
                )

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    placeholder = { Text(stringResource(R.string.search_app)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    singleLine = true
                )

                LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isSelected = selectedPackages.contains(app.packageName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Produce a new Set object so Compose re-composes
                                    selectedPackages = if (isSelected) {
                                        selectedPackages - app.packageName
                                    } else {
                                        selectedPackages + app.packageName
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null
                            )
                            // Show the app icon + name using AppListItem (no divider here)
                            AppListItem(
                                app = app,
                                showDivider = false,
                                onClick = {
                                    selectedPackages = if (isSelected) {
                                        selectedPackages - app.packageName
                                    } else {
                                        selectedPackages + app.packageName
                                    }
                                }
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSave(group.copy(name = editedName.ifBlank { group.name }, packageNames = selectedPackages)) }) { Text(stringResource(R.string.btn_save)) }
                }
            }
        }
    }
}
