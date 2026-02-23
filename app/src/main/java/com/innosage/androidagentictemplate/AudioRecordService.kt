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
import com.innosage.androidagentictemplate.data.SemanticProcessor
import com.innosage.androidagentictemplate.data.UtteranceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

private const val CHANNEL_ID = "AudioRecordServiceChannel"
private const val NOTIFICATION_ID = 1
private const val LOG_TAG = "AudioRecordService"
private const val CHUNK_DURATION_MS = 10 * 60 * 1000L // 10 minutes
private const val SAMPLE_RATE = 16000
private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
private const val VAD_THRESHOLD = 800.0 // Adjusted for typical room noise
private const val HANGOVER_MS = 1000L // Keep recording for 1s after voice stops
private const val UTTERANCE_SILENCE_THRESHOLD_MS = 1500L // Wait 1.5s for utterance end

private const val ACTION_PAUSE = "PAUSE_RECORDING"
private const val ACTION_RESUME = "RESUME_RECORDING"
private const val ACTION_STOP = "STOP_SERVICE"

class AudioRecordService : Service() {

    companion object {
        private val _isVoicedState = kotlinx.coroutines.flow.MutableStateFlow(false)
        val isVoicedState: kotlinx.coroutines.flow.StateFlow<Boolean> = _isVoicedState
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = AtomicBoolean(false)
    private var isPaused = AtomicBoolean(false)
    private var recordingThread: Thread? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var storageEngine: StorageEngine
    private lateinit var transcriptionEngine: TranscriptionEngine
    private lateinit var database: AppDatabase
    private lateinit var semanticProcessor: SemanticProcessor
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val vadProcessor = VADProcessor(VAD_THRESHOLD, HANGOVER_MS, UTTERANCE_SILENCE_THRESHOLD_MS)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        storageEngine = StorageEngine(getExternalFilesDir(null)!!)
        database = AppDatabase.getDatabase(this)
        semanticProcessor = SemanticProcessor()
        
        transcriptionEngine = TranscriptionEngine(this)
        transcriptionEngine.initialize("small")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(LOG_TAG, "onStartCommand: action=$action")
        
        when (action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                isPaused.set(true)
                updateNotification()
                return START_STICKY
            }
            ACTION_RESUME -> {
                isPaused.set(false)
                updateNotification()
                return START_STICKY
            }
        }

        // Default start or other actions
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

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun startRecording() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(LOG_TAG, "Missing RECORD_AUDIO permission")
            stopSelf()
            return
        }
        
        isRecording.set(true)
        isPaused.set(false)
        recordingThread = Thread {
            recordLoop()
        }.apply { start() }
        Log.d(LOG_TAG, "Started VAD-enabled recording thread")
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

            var currentFile: File = storageEngine.getNextChunkFile()
            var outputStream = FileOutputStream(currentFile)
            var lastChunkStartTime = System.currentTimeMillis()
            val utteranceBuffer = ByteArrayOutputStream()
            
            var utteranceStartTime = 0L
            var energySum = 0.0
            var energyCount = 0

            Log.d(LOG_TAG, "Recording to: ${currentFile.absolutePath}")

            while (isRecording.get()) {
                if (isPaused.get()) {
                    if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        audioRecord?.stop()
                        Log.d(LOG_TAG, "AudioRecord stopped (Paused)")
                    }
                    vadProcessor.reset()
                    utteranceBuffer.reset()
                    energySum = 0.0
                    energyCount = 0
                    utteranceStartTime = 0L
                    Thread.sleep(200)
                    continue
                } else {
                    if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_STOPPED) {
                        audioRecord?.startRecording()
                        Log.d(LOG_TAG, "AudioRecord started (Resumed)")
                    }
                }

                val readResult = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: -1
                if (readResult > 0) {
                    val (voiced, rms) = vadProcessor.isVoiced(audioBuffer, readResult)
                    val now = System.currentTimeMillis()

                    if (voiced) {
                        if (energyCount == 0) {
                            utteranceStartTime = now
                        }
                        energySum += rms
                        energyCount++
                    }
                    _isVoicedState.value = voiced

                    // Prepare byte array for the current frame
                    val byteBuffer = java.nio.ByteBuffer.allocate(readResult * 2)
                    byteBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN)
                    for (i in 0 until readResult) {
                        byteBuffer.putShort(audioBuffer[i])
                    }
                    val currentPcmBytes = byteBuffer.array()

                    // Write to disk if voiced or within hangover period (Long-term storage)
                    if (vadProcessor.shouldRecord(voiced, now)) {
                        outputStream.write(currentPcmBytes)
                        // Also append to current utterance buffer
                        utteranceBuffer.write(currentPcmBytes)
                    }

                    // Check for utterance completion (Real-time reactive engine)
                    if (vadProcessor.isUtteranceComplete(now)) {
                        val utteranceData = utteranceBuffer.toByteArray()
                        if (utteranceData.isNotEmpty()) {
                            val startTime = utteranceStartTime
                            val duration = now - startTime
                            val avgEnergy = if (energyCount > 0) energySum / energyCount else 0.0
                            
                            Log.d(LOG_TAG, "Utterance complete (${utteranceData.size} bytes), sending to engine")
                            transcriptionEngine.transcribeUtterance(utteranceData) { text ->
                                Log.i(LOG_TAG, "UTTERANCE TRANSCRIPT: $text")
                                val analysis = semanticProcessor.process(text, avgEnergy)
                                serviceScope.launch {
                                    val entity = UtteranceEntity(
                                        timestamp = startTime,
                                        duration = duration,
                                        transcript = text,
                                        energyScore = avgEnergy,
                                        intentLabel = analysis.intentLabel,
                                        urgencyScore = analysis.urgencyScore,
                                        contextTags = analysis.contextTags
                                    )
                                    database.utteranceDao().insert(entity)
                                }
                            }
                        }
                        utteranceBuffer.reset()
                        vadProcessor.resetUtterance()
                        energySum = 0.0
                        energyCount = 0
                        utteranceStartTime = 0L
                    }

                    // Check for chunk rotation
                    if (now - lastChunkStartTime > CHUNK_DURATION_MS) {
                        outputStream.close()
                        
                        // Hand off completed chunk for transcription (Background archive)
                        val completedChunk = currentFile
                        transcriptionEngine.transcribeMediaFile(completedChunk) { text ->
                            Log.i(LOG_TAG, "CHUNK TRANSCRIPT: $text")
                        }

                        // Clean old files before starting new chunk
                        val threshold = 24 * 60 * 60 * 1000L
                        storageEngine.cleanup(threshold)
                        serviceScope.launch {
                            database.utteranceDao().deleteOlderThan(System.currentTimeMillis() - threshold)
                        }
                        
                        currentFile = storageEngine.getNextChunkFile()
                        outputStream = FileOutputStream(currentFile)
                        lastChunkStartTime = now
                        Log.d(LOG_TAG, "Rotated to new chunk: ${currentFile.absolutePath}")
                    }
                }
            }

            outputStream.close()
            utteranceBuffer.close()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error in recordLoop: ${e.message}")
        } finally {
            try {
                audioRecord?.stop()
            } catch (e: Exception) {}
            audioRecord?.release()
            audioRecord = null
        }
    }

    private fun stopRecording() {
        isRecording.set(false)
        recordingThread?.join(1000)
        recordingThread = null
        Log.d(LOG_TAG, "Stopped recording thread")
    }

    override fun onDestroy() {
        stopRecording()
        serviceScope.cancel()
        transcriptionEngine.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, AudioRecordService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val pauseResumeAction = if (isPaused.get()) {
            val resumeIntent = Intent(this, AudioRecordService::class.java).apply {
                action = ACTION_RESUME
            }
            val resumePendingIntent = PendingIntent.getService(this, 1, resumeIntent, PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_play,
                "Resume",
                resumePendingIntent
            ).build()
        } else {
            val pauseIntent = Intent(this, AudioRecordService::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePendingIntent = PendingIntent.getService(this, 2, pauseIntent, PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_pause,
                "Pause",
                pausePendingIntent
            ).build()
        }

        val statusText = if (isPaused.get()) "Recording Paused" else "VAD Recording active..."

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("InnoSage Recorder")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .addAction(pauseResumeAction)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Service", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Audio Record Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
