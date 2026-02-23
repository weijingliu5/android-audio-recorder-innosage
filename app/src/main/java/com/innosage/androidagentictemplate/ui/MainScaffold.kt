package com.innosage.androidagentictemplate.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
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
    val screens = listOf(Screen.Dashboard, Screen.History)
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
                                    Screen.History -> Icons.Default.Description
                                    Screen.Dashboard -> Icons.Default.Mic
                                    else -> Icons.Default.Mic
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
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    isRecording = isRecording,
                    isVoiced = isVoiced,
                    utterances = utterances,
                    onToggleRecording = onToggleRecording,
                    onFileTranscribe = { /* Implement file picker and transcription */ }
                )
            }
            composable(Screen.History.route) {
                PlaceholderScreen("Transcription History")
            }
        }
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    Surface(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Column(verticalArrangement = Arrangement.Center, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text(text = name, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Previous transcriptions will appear here.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
