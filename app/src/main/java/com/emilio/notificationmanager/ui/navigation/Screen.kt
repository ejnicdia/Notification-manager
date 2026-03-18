package com.emilio.notificationmanager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.ui.graphics.vector.ImageVector
import com.emilio.notificationmanager.R

/* Screen definitions using string resource IDs so they adapt to the current locale */
sealed class Screen(val route: String, val titleResId: Int, val icon: ImageVector) {
    object Home     : Screen("home",     R.string.nav_home,     Icons.Default.Home)
    object Silenced : Screen("silenced", R.string.nav_silenced, Icons.Default.NotificationsOff)
    object Blocked  : Screen("blocked",  R.string.nav_blocked,  Icons.Default.Block)
    object Groups   : Screen("groups",   R.string.nav_groups,   Icons.Default.List)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
    object Triggers : Screen("triggers", R.string.nav_triggers, Icons.Default.Bolt)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Groups,
    Screen.Silenced,
    Screen.Blocked
)
