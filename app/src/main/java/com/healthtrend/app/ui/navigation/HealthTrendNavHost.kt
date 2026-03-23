package com.healthtrend.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.healthtrend.app.ui.analytics.AnalyticsScreen
import com.healthtrend.app.ui.daycard.DayCardScreen
import com.healthtrend.app.ui.settings.SettingsScreen

/**
 * Bottom navigation destinations.
 * Order of enum entries defines display order in the navigation bar.
 */
enum class BottomNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    TODAY(
        route = "daycard",
        label = "Today",
        icon = Icons.Outlined.Today,
        selectedIcon = Icons.Filled.Today
    ),
    ANALYTICS(
        route = "analytics",
        label = "Analytics",
        icon = Icons.Outlined.BarChart,
        selectedIcon = Icons.Filled.BarChart
    ),
    SETTINGS(
        route = "settings",
        label = "Settings",
        icon = Icons.Outlined.Settings,
        selectedIcon = Icons.Filled.Settings
    )
}

/**
 * Root composable hosting NavHost with bottom navigation.
 * Three flat routes: daycard, analytics, settings.
 *
 * Supports "Today" tab re-tap: when the Today tab is already selected and tapped again,
 * a trigger is emitted for future day navigation behavior.
 */
@Composable
fun HealthTrendNavHost(
    modifier: Modifier = Modifier,
    openDayCardTrigger: Int = 0,
    navController: NavHostController = rememberNavController()
) {
    // Incrementing trigger: when Today tab is re-tapped while already selected,
    // this counter increments and DayCardScreen animates pager to today.
    var scrollToTodayTrigger by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            HealthTrendBottomBar(
                navController = navController,
                onTodayReselected = { scrollToTodayTrigger++ }
            )
        }
    ) { innerPadding ->
        LaunchedEffect(openDayCardTrigger) {
            if (openDayCardTrigger > 0) {
                navController.navigate(BottomNavDestination.TODAY.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
                scrollToTodayTrigger++
            }
        }

        NavHost(
            navController = navController,
            startDestination = BottomNavDestination.TODAY.route,
            modifier = modifier.padding(innerPadding)
        ) {
            composable(BottomNavDestination.TODAY.route) {
                DayCardScreen(scrollToTodayTrigger = scrollToTodayTrigger)
            }
            composable(BottomNavDestination.ANALYTICS.route) {
                AnalyticsScreen()
            }
            composable(BottomNavDestination.SETTINGS.route) {
                SettingsScreen()
            }
        }
    }
}

/**
 * Bottom navigation bar with Today, Analytics, Settings tabs.
 *
 * @param onTodayReselected Callback invoked when the "Today" tab is tapped while already selected.
 *   Reserved for future day navigation behavior (AC #4).
 */
@Composable
private fun HealthTrendBottomBar(
    navController: NavHostController,
    onTodayReselected: () -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        BottomNavDestination.entries.forEach { destination ->
            val selected = currentRoute == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (destination == BottomNavDestination.TODAY && selected) {
                        // Already on Today tab — trigger pager scroll to today (AC #4)
                        onTodayReselected()
                    } else {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.icon,
                        contentDescription = destination.label
                    )
                },
                label = { Text(destination.label) }
            )
        }
    }
}
