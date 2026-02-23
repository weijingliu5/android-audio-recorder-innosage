package com.innosage.androidagentictemplate.ui

sealed class Screen(val route: String, val label: String) {
    object Dashboard : Screen("dashboard", "Live Transcribe")
    object History : Screen("history", "History")
}
