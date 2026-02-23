package com.innosage.androidagentictemplate

import android.annotation.SuppressLint
import android.content.pm.ServiceInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.innosage.androidagentictemplate.data.AppDatabase
import com.innosage.androidagentictemplate.data.UtteranceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

private const val CHANNEL_ID = "WhisperLiveServiceChannel"
private const val NOTIFICATION_ID = 1
private const val LOG_TAG = "WhisperLiveService"
private const val SAMPLE_RATE = 16000
private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
private const val VAD_THRESHOLD = 800.0 
private const val HANGOVER_MS = 1000L 
private const val UTTERANCE_SILENCE_THRESHOLD_MS = 1500L

private const val ACTION_STOP = "STOP_SERVICE"

/**
 * Simplified Foreground Service for Live Whisper Transcription validation.
 * Focused on capturing audio and passing it to TranscriptionEngine.
 */
class AudioRecordService : Service() {

    companion object {
        private val _isVoicedState = kotlinx.coroutines.flow.MutableStateFlow(false)
        val isVoicedState: kotlinx.coroutines.flow.StateFlow<Boolean> = _isVoicedState

        private val _currentUtteranceText = kotlinx.coroutines.flow.MutableStateFlow("")
        val currentUtteranceText: kotlinx.coroutines.flow.StateFlow<String> = _currentUtteranceText
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = AtomicBoolean(false)
    private var recordingThread: Thread? = null
    
    private lateinit var transcriptionEngine: TranscriptionEngine
    private lateinit var database: AppDatabase
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val vadProcessor = VADProcessor(VAD_THRESHOLD, HANGOVER_MS, UTTERANCE_SILENCE_THRESHOLD_MS)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        database = AppDatabase.getDatabase(this)
        
        transcriptionEngine = TranscriptionEngine(this)
        // Default to base model for validation speed
        transcriptionEngine.initialize("base")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }

        if (!isRecording.get()) {
            startRecording()
        }

        return START_STICKY
    }

    private fun startRecording() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(LOG_TAG, "Missing RECORD_AUDIO permission")
            stopSelf()
            return
        }
        
        isRecording.set(true)
        recordingThread = Thread {
            recordLoop()
        }.apply { start() }
        Log.d(LOG_TAG, "Started Live Whisper recording")
    }

    @SuppressLint("MissingPermission")
    private fun recordLoop() {
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = Math.max(minBufferSize, 2048)
        val audioBuffer = ShortArray(bufferSize)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(LOG_TAG, "AudioRecord initialization failed")
                return
            }

            audioRecord?.startRecording()
            vadProcessor.reset()

            val utteranceBuffer = java.io.ByteArrayOutputStream()
            var utteranceStartTime = 0L

            while (isRecording.get()) {
                val readResult = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: -1
                if (readResult > 0) {
                    val (voiced, rms) = vadProcessor.isVoiced(audioBuffer, readResult)
                    val now = System.currentTimeMillis()

                    if (voiced && utteranceStartTime == 0L) {
                        utteranceStartTime = now
                    }
                    _isVoicedState.value = voiced

                    // PCM to Bytes
                    val byteBuffer = java.nio.ByteBuffer.allocate(readResult * 2)
                    byteBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN)
                    for (i in 0 until readResult) {
                        byteBuffer.putShort(audioBuffer[i])
                    }
                    val currentPcmBytes = byteBuffer.array()

                    if (vadProcessor.shouldRecord(voiced, now)) {
                        utteranceBuffer.write(currentPcmBytes)
                        
                        // Progressive Transcription
                        val utteranceDataSoFar = utteranceBuffer.toByteArray()
                        if (utteranceDataSoFar.size >= 16000 * 2) { // Every 1s
                            if (System.currentTimeMillis() % 1000 < 100) { // Throttle slightly
                                transcriptionEngine.transcribeUtterance(utteranceDataSoFar) { text ->
                                    _currentUtteranceText.value = text
                                }
                            }
                        }
                    }

                    if (vadProcessor.isUtteranceComplete(now)) {
                        val utteranceData = utteranceBuffer.toByteArray()
                        if (utteranceData.isNotEmpty()) {
                            val startTime = utteranceStartTime
                            val duration = now - startTime
                            
                            Log.d(LOG_TAG, "Utterance detected, transcribing...")
                            transcriptionEngine.transcribeUtterance(utteranceData) { text ->
                                Log.i(LOG_TAG, "LIVE: $text")
                                _currentUtteranceText.value = "" // Reset for next utterance
                                serviceScope.launch {
                                    database.utteranceDao().insert(UtteranceEntity(
                                        timestamp = startTime,
                                        duration = duration,
                                        transcript = text,
                                        energyScore = rms // Simplified
                                    ))
                                }
                            }
                        }
                        utteranceBuffer.reset()
                        vadProcessor.resetUtterance()
                        utteranceStartTime = 0L
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error in recordLoop: ${e.message}")
        } finally {
            audioRecord?.release()
        }
    }

    override fun onDestroy() {
        isRecording.set(false)
        recordingThread?.join(500)
        serviceScope.cancel()
        transcriptionEngine.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, AudioRecordService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Whisper Live")
            .setContentText("Transcribing audio in real-time...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Whisper Live Channel", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
