package com.innosage.androidagentictemplate.ui

sealed class Screen(val route: String, val label: String) {
    object History : Screen("history", "History")
    object Dashboard : Screen("dashboard", "Dashboard")
    object Settings : Screen("settings", "Settings")
}
