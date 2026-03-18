package com.emilio.notificationmanager.ui.components

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.emilio.notificationmanager.AppInfo
import com.emilio.notificationmanager.R
import com.emilio.notificationmanager.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TriggerWizard(
    context: Context,
    appPreferences: AppPreferences,
    initialTrigger: Trigger?,
    onDismiss: () -> Unit,
    onSave: (Trigger) -> Unit
) {
    var step by remember { mutableStateOf(0) }
    
    // Step 0: Name
    var triggerName by remember { mutableStateOf(initialTrigger?.name ?: "") }
    
    // Step 1: Apps & Groups
    var selectedPackages by remember { mutableStateOf(initialTrigger?.packageNames ?: emptySet()) }
    var selectedGroups by remember { mutableStateOf(initialTrigger?.groupIds ?: emptySet()) }
    
    // Step 2: Trigger Type & Extra Config
    var triggerType by remember { mutableStateOf(initialTrigger?.triggerType ?: TriggerType.TEMPORAL) }
    var targetHourText by remember { mutableStateOf(initialTrigger?.targetHour?.toString() ?: "") }
    var targetMinuteText by remember { mutableStateOf(initialTrigger?.targetMinute?.toString() ?: "") }
    var keywordText by remember { mutableStateOf(initialTrigger?.keyword ?: "") }
    
    // Step 3: Action & Time
    var triggerAction by remember { mutableStateOf(initialTrigger?.action ?: TriggerAction.SILENCE) }
    var timeInput by remember { mutableStateOf((initialTrigger?.durationMs?.div(60000))?.toString() ?: "") }

    val canGoNext = when (step) {
        0 -> triggerName.isNotBlank() && triggerName.length <= 50
        1 -> selectedPackages.isNotEmpty() || selectedGroups.isNotEmpty()
        2 -> {
            if (triggerType == TriggerType.TEMPORAL) {
                targetHourText.isNotBlank() && targetMinuteText.isNotBlank()
            } else {
                keywordText.isNotBlank()
            }
        }
        3 -> timeInput.toLongOrNull() != null && timeInput.toLong() > 0
        else -> false
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Text(
                    text = stringResource(R.string.wizard_header, step + 1),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()

                // Content area
                Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                    when (step) {
                        0 -> Step0Name(triggerName, { triggerName = it })
                        1 -> Step1AppsGroups(context, appPreferences, selectedPackages, { selectedPackages = it }, selectedGroups, { selectedGroups = it })
                        2 -> Step2Type(
                            type = triggerType, onTypeChange = { triggerType = it },
                            targetHour = targetHourText, onHourChange = { targetHourText = it },
                            targetMinute = targetMinuteText, onMinuteChange = { targetMinuteText = it },
                            keyword = keywordText, onKeywordChange = { keywordText = it }
                        )
                        3 -> Step3ActionTime(triggerAction, { triggerAction = it }, timeInput, { timeInput = it })
                    }
                }

                HorizontalDivider()
                // Footer Nav
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        if (step > 0) step-- else onDismiss()
                    }) {
                        Text(if (step == 0) stringResource(R.string.btn_cancel) else stringResource(R.string.btn_back))
                    }
                    
                    Button(
                        onClick = {
                            if (step < 3) {
                                step++
                            } else {
                                val durationMs = (timeInput.toLongOrNull() ?: 0L) * 60 * 1000
                                val finalTrigger = Trigger(
                                    id = initialTrigger?.id ?: java.util.UUID.randomUUID().toString(),
                                    name = triggerName,
                                    packageNames = selectedPackages,
                                    groupIds = selectedGroups,
                                    triggerType = triggerType,
                                    action = triggerAction,
                                    durationMs = durationMs,
                                    targetHour = if (triggerType == TriggerType.TEMPORAL) targetHourText.toIntOrNull() else null,
                                    targetMinute = if (triggerType == TriggerType.TEMPORAL) targetMinuteText.toIntOrNull() else null,
                                    keyword = if (triggerType == TriggerType.POR_NOTIFICACION) keywordText else null,
                                    lastFiredTimestamp = initialTrigger?.lastFiredTimestamp
                                )
                                onSave(finalTrigger)
                            }
                        },
                        enabled = canGoNext
                    ) {
                        Text(if (step < 3) stringResource(R.string.btn_next) else stringResource(R.string.btn_confirm))
                    }
                }
            }
        }
    }
}

@Composable
fun Step0Name(name: String, onNameChange: (String) -> Unit) {
    Column {
        Text(stringResource(R.string.step0_prompt))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 50) onNameChange(it) },
            label = { Text(stringResource(R.string.step0_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
fun Step1AppsGroups(
    context: Context,
    appPreferences: AppPreferences,
    selectedPackages: Set<String>,
    onPackagesChange: (Set<String>) -> Unit,
    selectedGroups: Set<String>,
    onGroupsChange: (Set<String>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    val groups = remember { appPreferences.getGroups() }
    var selectedTab by remember { mutableStateOf(0) }

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

    Column(modifier = Modifier.fillMaxSize()) {
        Text(stringResource(R.string.step1_prompt), style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(stringResource(R.string.tab_apps)) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(stringResource(R.string.tab_groups)) })
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        if (selectedTab == 0) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.search_app)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true
            )

            LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isSelected = selectedPackages.contains(app.packageName)
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable {
                                if (isSelected) onPackagesChange(selectedPackages - app.packageName)
                                else onPackagesChange(selectedPackages + app.packageName)
                            }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isSelected, onCheckedChange = null)
                        AppListItem(app = app, showDivider = false, onClick = {}, modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            if (groups.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_groups), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(groups, key = { it.id }) { group ->
                        val isSelected = selectedGroups.contains(group.id)
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    if (isSelected) onGroupsChange(selectedGroups - group.id)
                                    else onGroupsChange(selectedGroups + group.id)
                                }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isSelected, onCheckedChange = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(group.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Step2Type(
    type: TriggerType, 
    onTypeChange: (TriggerType) -> Unit,
    targetHour: String,
    onHourChange: (String) -> Unit,
    targetMinute: String,
    onMinuteChange: (String) -> Unit,
    keyword: String,
    onKeywordChange: (String) -> Unit
) {
    Column {
        Text(stringResource(R.string.step2_prompt), style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onTypeChange(TriggerType.TEMPORAL) }) {
            RadioButton(selected = type == TriggerType.TEMPORAL, onClick = null)
            Text(stringResource(R.string.type_temporal), modifier = Modifier.padding(start = 8.dp))
        }
        
        if (type == TriggerType.TEMPORAL) {
            Row(modifier = Modifier.fillMaxWidth().padding(start = 40.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = targetHour,
                    onValueChange = { 
                        if (it.isEmpty() || (it.all { char -> char.isDigit() } && it.toIntOrNull() != null && it.toInt() <= 23)) {
                            onHourChange(it)
                        }
                    },
                    label = { Text(stringResource(R.string.label_hour_range)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = targetMinute,
                    onValueChange = { 
                        if (it.isEmpty() || (it.all { char -> char.isDigit() } && it.toIntOrNull() != null && it.toInt() <= 59)) {
                            onMinuteChange(it)
                        }
                    },
                    label = { Text(stringResource(R.string.label_minute_range)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onTypeChange(TriggerType.POR_NOTIFICACION) }) {
            RadioButton(selected = type == TriggerType.POR_NOTIFICACION, onClick = null)
            Text(stringResource(R.string.type_notification), modifier = Modifier.padding(start = 8.dp))
        }

        if (type == TriggerType.POR_NOTIFICACION) {
            OutlinedTextField(
                value = keyword,
                onValueChange = onKeywordChange,
                label = { Text(stringResource(R.string.label_keyword)) },
                modifier = Modifier.fillMaxWidth().padding(start = 40.dp, top = 8.dp),
                singleLine = true
            )
        }
    }
}

@Composable
fun Step3ActionTime(action: TriggerAction, onActionChange: (TriggerAction) -> Unit, timeInput: String, onTimeChange: (String) -> Unit) {
    Column {
        Text(stringResource(R.string.step3_prompt), style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onActionChange(TriggerAction.SILENCE) }) {
            RadioButton(selected = action == TriggerAction.SILENCE, onClick = null)
            Text(stringResource(R.string.action_silence), modifier = Modifier.padding(start = 8.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onActionChange(TriggerAction.BLOCK) }) {
            RadioButton(selected = action == TriggerAction.BLOCK, onClick = null)
            Text(stringResource(R.string.action_block), modifier = Modifier.padding(start = 8.dp))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = timeInput,
            onValueChange = { if (it.all { char -> char.isDigit() }) onTimeChange(it) },
            label = { Text(stringResource(R.string.label_duration_min)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}
