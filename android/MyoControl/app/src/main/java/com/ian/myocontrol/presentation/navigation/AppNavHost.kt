package com.ian.myocontrol.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ian.myocontrol.core.theme.McColors
import com.ian.myocontrol.presentation.analytics.AnalyticsScreen
import com.ian.myocontrol.presentation.home.HomeScreen
import com.ian.myocontrol.presentation.monitor.MonitorScreen
import com.ian.myocontrol.presentation.train.TrainScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        // Use MaterialTheme so cream/absolute-black switches with dark mode
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation  = androidx.compose.ui.unit.Dp(0f)
            ) {
                BottomNavRoute.tabs.forEach { tab ->
                    val selected = currentDestination?.hierarchy
                        ?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected  = selected,
                        onClick   = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        icon  = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = McColors.Coral,
                            selectedTextColor   = McColors.Coral,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor      = McColors.CoralContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = BottomNavRoute.Home.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavRoute.Home.route)      { HomeScreen() }
            composable(BottomNavRoute.Train.route)     { TrainScreen() }
            composable(BottomNavRoute.Monitor.route)   { MonitorScreen() }
            composable(BottomNavRoute.Analytics.route) { AnalyticsScreen() }
        }
    }
}
