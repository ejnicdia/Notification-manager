package com.emilio.notificationmanager

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.emilio.notificationmanager.data.AppPreferences
import com.emilio.notificationmanager.data.LanguagePreference
import com.emilio.notificationmanager.ui.navigation.Screen
import com.emilio.notificationmanager.ui.navigation.bottomNavItems
import com.emilio.notificationmanager.ui.screens.BlockedScreen
import com.emilio.notificationmanager.ui.screens.GroupsScreen
import com.emilio.notificationmanager.ui.screens.HomeScreen
import com.emilio.notificationmanager.ui.screens.SettingsScreen
import com.emilio.notificationmanager.ui.screens.SilencedScreen
import com.emilio.notificationmanager.ui.theme.NotificationManagerTheme
import kotlinx.coroutines.delay
import java.util.Locale

class MainActivity : ComponentActivity() {

    /* Main initialization function of the application */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize data preferences layer
        val appPreferences = AppPreferences(this)

        // 2. Apply saved language before inflating any UI
        applyLocale(this, appPreferences.getLanguagePreference())

        setContent {
            var currentTheme by remember { mutableStateOf(appPreferences.getThemePreference()) }

            // 3. Request POST_NOTIFICATIONS permission on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* result handled via banner in UI */ }

                LaunchedEffect(Unit) {
                    val granted = ContextCompat.checkSelfPermission(
                        this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            NotificationManagerTheme(themePreference = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp(
                        appPreferences = appPreferences,
                        onThemeChange = { newTheme -> currentTheme = newTheme },
                        onLanguageChange = {
                            // Recreate Activity so every string uses the new locale
                            recreate()
                        }
                    )
                }
            }
        }
    }
}

/* Apply the selected locale to the Activity context before UI inflation */
fun applyLocale(context: Context, langPref: LanguagePreference) {
    val locale = when (langPref) {
        LanguagePreference.ES -> Locale("es")
        LanguagePreference.EN -> Locale("en")
        LanguagePreference.FR -> Locale("fr")
        LanguagePreference.PT -> Locale("pt")
        LanguagePreference.SYSTEM -> return // use whatever the device has
    }
    Locale.setDefault(locale)
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
/* Main composable function that builds the App's UI structure */
fun MainApp(
    appPreferences: AppPreferences,
    onThemeChange: (com.emilio.notificationmanager.data.ThemePreference) -> Unit,
    onLanguageChange: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    val navController = rememberNavController()
    var expandedMenu by remember { mutableStateOf(false) }

    // 1. Derive the current screen title from the back-stack for the TopAppBar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val topBarTitle = when (currentRoute) {
        Screen.Home.route, null -> stringResource(R.string.app_name)
        Screen.Silenced.route  -> stringResource(Screen.Silenced.titleResId)
        Screen.Blocked.route   -> stringResource(Screen.Blocked.titleResId)
        Screen.Groups.route    -> stringResource(Screen.Groups.titleResId)
        Screen.Settings.route  -> stringResource(Screen.Settings.titleResId)
        Screen.Triggers.route  -> stringResource(Screen.Triggers.titleResId)
        else                   -> stringResource(R.string.app_name)
    }

    // 2. Check if POST_NOTIFICATIONS is granted (needed to repost silent notifications)
    val hasPostPermission by remember {
        derivedStateOf {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true // not needed before Android 13
        }
    }

    // 3. Re-check both permissions periodically
    LaunchedEffect(Unit) {
        while (true) {
            hasPermission = isNotificationServiceEnabled(context)
            delay(1000)
        }
    }

    if (!hasPermission) {
        PermissionScreen(
            onGrantClick = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(topBarTitle) },
                    actions = {
                        IconButton(onClick = { expandedMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu_options))
                        }
                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.nav_triggers)) },
                                onClick = {
                                    expandedMenu = false
                                    navController.navigate(Screen.Triggers.route) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.nav_settings)) },
                                onClick = {
                                    expandedMenu = false
                                    navController.navigate(Screen.Settings.route) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }
                )
            },
            bottomBar = { BottomNavigationBar(navController) }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                // 4. Warning banner if POST_NOTIFICATIONS is not granted
                if (!hasPostPermission) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.notification_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                }
                            ) {
                                Text(stringResource(R.string.btn_enable), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                NavigationGraph(navController, context, appPreferences, onThemeChange, onLanguageChange)
            }
        }
    }
}

/* Composable function for the Navigation Bar sitting at the bottom of the screen */
@Composable
fun BottomNavigationBar(navController: NavHostController) {
    NavigationBar {
        // 1. Get the current active route
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        bottomNavItems.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = stringResource(item.titleResId)) },
                label = { Text(stringResource(item.titleResId)) },
                alwaysShowLabel = true,
                // 2. Highlight only if we are exactly on this route
                selected = currentRoute == item.route,
                onClick = {
                    // 3. Navigate to the tab, clearing everything above Home in the back-stack.
                    // Using saveState = false so we never restore a stale back-stack entry
                    // that might have been left by a dropdown navigation.
                    navController.navigate(item.route) {
                        popUpTo(Screen.Home.route) {
                            saveState = false
                            inclusive = (item.route == Screen.Home.route)
                        }
                        launchSingleTop = true
                        restoreState = false
                    }
                }
            )
        }
    }
}

/* Composable function that registers all available screens for routing */
@Composable
fun NavigationGraph(
    navController: NavHostController,
    context: Context,
    appPreferences: AppPreferences,
    onThemeChange: (com.emilio.notificationmanager.data.ThemePreference) -> Unit,
    onLanguageChange: () -> Unit
) {
    NavHost(navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(context, appPreferences)
        }
        composable(Screen.Silenced.route) {
            SilencedScreen(context, appPreferences)
        }
        composable(Screen.Blocked.route) {
            BlockedScreen(context, appPreferences)
        }
        composable(Screen.Groups.route) {
            GroupsScreen(context, appPreferences)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(context, appPreferences, onThemeChange, onLanguageChange)
        }
        composable(Screen.Triggers.route) {
            com.emilio.notificationmanager.ui.screens.TriggersScreen(context, appPreferences)
        }
    }
}

@Composable
fun PermissionScreen(onGrantClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.permission_needed),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onGrantClick) {
            Text(stringResource(R.string.btn_grant_permission))
        }
    }
}

fun isNotificationServiceEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    val cn = ComponentName(context, NotificationBlockerService::class.java)
    return flat?.contains(cn.flattenToString()) == true
}

// Data class AppInfo is required app-wide
data class AppInfo(
    val packageName: String,
    val name: String,
    val icon: android.graphics.drawable.Drawable
)
