package com.ian.myocontrol.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavRoute(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : BottomNavRoute(
        route = "home",
        label = "Home",
        icon  = Icons.Filled.Home
    )
    data object Train : BottomNavRoute(
        route = "train",
        label = "Train",
        icon  = Icons.Filled.School
    )
    data object Monitor : BottomNavRoute(
        route = "monitor",
        label = "Monitor",
        icon  = Icons.Filled.Monitor
    )
    data object Analytics : BottomNavRoute(
        route = "analytics",
        label = "Analytics",
        icon  = Icons.Filled.Analytics
    )

    companion object {
        val tabs = listOf(Home, Train, Monitor, Analytics)
    }
}
