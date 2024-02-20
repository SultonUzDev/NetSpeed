package com.uzdev.netspeed.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavRoute(val route: String, val icon: ImageVector, val label: String) {
    object Home : NavRoute(route = "home", Icons.Rounded.Home, label = "Test")
    object Result : NavRoute(route = "result", Icons.Rounded.Menu, "Result")
    object Settings : NavRoute(route = "settings", Icons.Rounded.Settings, "Settings")
}
