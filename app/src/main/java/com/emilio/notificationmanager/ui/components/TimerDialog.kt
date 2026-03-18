package com.emilio.notificationmanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun TimerDialog(
    title: String,
    onDismiss: () -> Unit,
    onSilenceSet: (Long) -> Unit,
    onBlockSet: (Long) -> Unit,
    onReset: () -> Unit,
    showReset: Boolean = true
) {
    var hoursText by remember { mutableStateOf("0") }
    var minutesText by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text("¿Para cuánto tiempo es esta regla?")
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { 
                            if (it.isEmpty() || (it.all { char -> char.isDigit() } && it.toIntOrNull() != null && it.toInt() <= 23)) {
                                hoursText = it 
                            }
                        },
                        label = { Text("Horas") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { 
                            if (it.isEmpty() || (it.all { char -> char.isDigit() } && it.toIntOrNull() != null && it.toInt() <= 59)) {
                                minutesText = it 
                            }
                        },
                        label = { Text("Min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                val h = hoursText.toLongOrNull() ?: 0L
                val m = minutesText.toLongOrNull() ?: 0L
                val totalMs = (h * 60 + m) * 60 * 1000L
                val canSubmit = totalMs > 0L

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onSilenceSet(totalMs) }, 
                        modifier = Modifier.weight(1f),
                        enabled = canSubmit
                    ) {
                        Text("Silenciar")
                    }
                    Button(
                        onClick = { onBlockSet(totalMs) }, 
                        modifier = Modifier.weight(1f), 
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Bloquear")
                    }
                }

                if (showReset) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                        Text("Desactivar reglas")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
