package com.innosage.androidagentictemplate.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.innosage.androidagentictemplate.ui.screens.DashboardScreen

import androidx.compose.ui.unit.dp

@Composable
fun MainScaffold(
    isRecording: Boolean,
    onToggleRecording: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val navController = rememberNavController()
    val screens = listOf(Screen.History, Screen.Dashboard, Screen.Settings)
    val utterances by viewModel.utterances.collectAsState()
    val isVoiced by viewModel.isVoiced.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = when (screen) {
                                    Screen.History -> Icons.Default.History
                                    Screen.Dashboard -> Icons.Default.Dashboard
                                    Screen.Settings -> Icons.Default.Settings
                                },
                                contentDescription = screen.label
                            )
                        },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.History.route) {
                PlaceholderScreen(Screen.History.label)
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    isRecording = isRecording,
                    isVoiced = isVoiced,
                    utterances = utterances,
                    onToggleRecording = onToggleRecording,
                    onManualTag = { /* Handle manual tag */ }
                )
            }
            composable(Screen.Settings.route) {
                PlaceholderScreen(Screen.Settings.label)
            }
        }
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    Surface(modifier = Modifier.padding(16.dp)) {
        Text(text = "$name Screen Placeholder", style = MaterialTheme.typography.headlineSmall)
    }
}
