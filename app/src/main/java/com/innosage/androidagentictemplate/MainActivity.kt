package com.innosage.androidagentictemplate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import com.innosage.androidagentictemplate.ui.MainScaffold
import com.innosage.androidagentictemplate.ui.theme.InnoSageTheme

private const val REQUEST_RECORD_AUDIO_PERMISSION = 200

class MainActivity : ComponentActivity() {

    private var isRecordingState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkPermissions()

        setContent {
            InnoSageTheme {
                MainScaffold(
                    isRecording = isRecordingState.value,
                    onToggleRecording = { toggleService() }
                )
            }
        }
    }

    private fun toggleService() {
        val serviceIntent = Intent(this, AudioRecordService::class.java)
        if (!isRecordingState.value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            isRecordingState.value = true
        } else {
            stopService(serviceIntent)
            isRecordingState.value = false
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        val missingPermissions = permissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), REQUEST_RECORD_AUDIO_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // Permission denied, we could show a dialog or finish
            }
        }
    }
}
