package com.emilio.notificationmanager.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emilio.notificationmanager.data.AppPreferences
import com.emilio.notificationmanager.data.Trigger
import com.emilio.notificationmanager.ui.components.TriggerWizard
import kotlinx.coroutines.launch

@Composable
fun TriggersScreen(context: Context, appPreferences: AppPreferences) {
    var triggers by remember { mutableStateOf(appPreferences.getTriggers()) }
    var showWizard by remember { mutableStateOf(false) }
    var triggerToEdit by remember { mutableStateOf<Trigger?>(null) }
    var triggerToDelete by remember { mutableStateOf<Trigger?>(null) }
    var expandedTriggerId by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val refreshTriggers = {
        triggers = appPreferences.getTriggers()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showWizard = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Trigger")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (triggers.isEmpty()) {
                Text(
                    text = "Aún no hay triggers creados.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(triggers, key = { it.id }) { trigger ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { 
                                expandedTriggerId = if (expandedTriggerId == trigger.id) null else trigger.id
                            },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = trigger.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tipo: ${trigger.triggerType.name}\n" +
                                               "Acción: ${trigger.action.name}\n" +
                                               "Tiempo: ${trigger.durationMs / 60000} minutos",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row {
                                    if (trigger.triggerType == com.emilio.notificationmanager.data.TriggerType.TEMPORAL) {
                                        IconButton(onClick = {
                                            val expirationTime = System.currentTimeMillis() + trigger.durationMs
                                            
                                            // 1. Apply to specific packages
                                            trigger.packageNames.forEach { pkg ->
                                                if (trigger.action == com.emilio.notificationmanager.data.TriggerAction.BLOCK) {
                                                    appPreferences.blockAppUntil(pkg, expirationTime)
                                                    appPreferences.silenceAppUntil(pkg, 0L)
                                                } else {
                                                    appPreferences.silenceAppUntil(pkg, expirationTime)
                                                    appPreferences.blockAppUntil(pkg, 0L)
                                                }
                                            }
                                            
                                            // 2. Apply to groups
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
                                            
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Trigger temporal '${trigger.name}' activado")
                                            }
                                        }) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Activar", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    IconButton(onClick = { triggerToEdit = trigger }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                                    }
                                    IconButton(onClick = { triggerToDelete = trigger }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            if (expandedTriggerId == trigger.id) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Afecta a:", style = MaterialTheme.typography.labelLarge)
                                    if (trigger.groupIds.isNotEmpty()) {
                                        Text("Grupos: ${appPreferences.getGroups().filter { it.id in trigger.groupIds }.joinToString { it.name }}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (trigger.packageNames.isNotEmpty()) {
                                        val pm = context.packageManager
                                        val appNames = trigger.packageNames.map { pkg ->
                                            try { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() } catch (e: Exception) { pkg }
                                        }
                                        Text("Aplicaciones: ${appNames.joinToString()}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWizard) {
        TriggerWizard(
            context = context,
            appPreferences = appPreferences,
            initialTrigger = null,
            onDismiss = { showWizard = false },
            onSave = {
                appPreferences.saveTrigger(it)
                refreshTriggers()
                showWizard = false
            }
        )
    }

    triggerToEdit?.let { trigger ->
        TriggerWizard(
            context = context,
            appPreferences = appPreferences,
            initialTrigger = trigger,
            onDismiss = { triggerToEdit = null },
            onSave = { updatedTrigger ->
                appPreferences.saveTrigger(updatedTrigger)
                refreshTriggers()
                triggerToEdit = null
            }
        )
    }

    triggerToDelete?.let { trigger ->
        AlertDialog(
            onDismissRequest = { triggerToDelete = null },
            title = { Text("Eliminar Trigger") },
            text = { Text("¿Estás seguro de que deseas eliminar el trigger '${trigger.name}'? Esto no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        appPreferences.deleteTrigger(trigger.id)
                        refreshTriggers()
                        triggerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { triggerToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
