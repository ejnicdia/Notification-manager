package com.emilio.notificationmanager.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emilio.notificationmanager.data.AppPreferences
import com.emilio.notificationmanager.data.ThemePreference

@Composable
fun SettingsScreen(context: Context, appPreferences: AppPreferences, onThemeChange: (ThemePreference) -> Unit) {
    var selectedTheme by remember { mutableStateOf(appPreferences.getThemePreference()) }
    
    val packageInfo = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (e: Exception) {
        null
    }
    val versionName = packageInfo?.versionName ?: "Desconocida"

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text("Tema de la aplicación", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        val themeOptions = listOf(
            ThemePreference.SYSTEM to "Tema del Sistema",
            ThemePreference.LIGHT to "Tema Claro (Blanco y Naranja)",
            ThemePreference.DARK to "Tema Oscuro (Negro y Azul)"
        )

        themeOptions.forEach { (theme, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedTheme = theme
                        appPreferences.setThemePreference(theme)
                        onThemeChange(theme)
                    }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedTheme == theme,
                    onClick = null // handled by Row
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = label, style = MaterialTheme.typography.bodyLarge)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))

        Text("Información de la aplicación", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Versión: $versionName", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
