package com.emilio.notificationmanager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home     : Screen("home",     "Home",         Icons.Default.Home)
    object Silenced : Screen("silenced", "Silenciados",  Icons.Default.NotificationsOff)
    object Blocked  : Screen("blocked",  "Bloqueados",   Icons.Default.Block)
    object Groups   : Screen("groups",   "Grupos",       Icons.Default.List)
    object Settings : Screen("settings", "Ajustes",      Icons.Default.Settings)
    object Triggers : Screen("triggers", "Triggers",     Icons.Default.Bolt)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Groups,
    Screen.Silenced,
    Screen.Blocked
)
