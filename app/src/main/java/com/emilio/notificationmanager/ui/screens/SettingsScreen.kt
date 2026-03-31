package com.emilio.notificationmanager.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.emilio.notificationmanager.R
import com.emilio.notificationmanager.data.AppPreferences
import com.emilio.notificationmanager.data.LanguagePreference
import com.emilio.notificationmanager.data.ThemePreference

/* Settings screen with theme, language, and app info sections */
@Composable
fun SettingsScreen(
    context: Context,
    appPreferences: AppPreferences,
    onThemeChange: (ThemePreference) -> Unit,
    onLanguageChange: () -> Unit
) {
    var selectedTheme by remember { mutableStateOf(appPreferences.getThemePreference()) }
    var selectedLanguage by remember { mutableStateOf(appPreferences.getLanguagePreference()) }

    val packageInfo = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (e: Exception) {
        null
    }
    val versionName = packageInfo?.versionName ?: stringResource(R.string.unknown_version)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // 1. Theme section
        Text(stringResource(R.string.theme_title), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        val themeOptions = listOf(
            ThemePreference.SYSTEM to stringResource(R.string.theme_system),
            ThemePreference.LIGHT to stringResource(R.string.theme_light),
            ThemePreference.DARK to stringResource(R.string.theme_dark)
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

        // 2. Language section
        Text(stringResource(R.string.language_title), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        val languageOptions = listOf(
            LanguagePreference.ES to stringResource(R.string.lang_es),
            LanguagePreference.EN to stringResource(R.string.lang_en),
            LanguagePreference.FR to stringResource(R.string.lang_fr),
            LanguagePreference.PT to stringResource(R.string.lang_pt)
        ).sortedBy { it.second }

        var expanded by remember { mutableStateOf(false) }
        val currentLabel = languageOptions.find { it.first == selectedLanguage }?.second ?: stringResource(R.string.lang_system)

        @OptIn(ExperimentalMaterial3Api::class)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            OutlinedTextField(
                value = currentLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.language_title)) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                languageOptions.forEach { (lang, label) ->
                    DropdownMenuItem(
                        text = { Text(text = label) },
                        onClick = {
                            selectedLanguage = lang
                            appPreferences.setLanguagePreference(lang)
                            expanded = false
                            onLanguageChange()
                        }
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))

        // 3. App info section
        Text(stringResource(R.string.app_info_title), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.version_label, versionName), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
