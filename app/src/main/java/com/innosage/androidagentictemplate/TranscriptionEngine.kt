package com.innosage.androidagentictemplate

import android.content.Context
import android.util.Log
import com.innosage.androidagentictemplate.whisper.WhisperContext
import com.innosage.androidagentictemplate.whisper.ModelDownloader
import kotlinx.coroutines.*
import java.io.File

/**
 * Handles background transcription of PCM chunks using Whisper.cpp.
 * Integrated with ModelDownloader and AudioConverter.
 */
class TranscriptionEngine(private val context: Context) {
    private var whisperContext: WhisperContext? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var transcriptionJob: Job? = null
    private val converter = AudioConverter()
    private val TAG = "TranscriptionEngine"

    fun initialize(modelName: String, onStatus: (String) -> Unit = {}) {
        scope.launch {
            try {
                val modelFile = File(context.getExternalFilesDir(null), "ggml-$modelName.bin")
                if (!modelFile.exists()) {
                    // Try fallback location first to avoid download
                    val fallback = File("/sdcard/Download/ggml-$modelName.bin")
                    if (fallback.exists()) {
                        Log.i(TAG, "Found model at fallback: ${fallback.absolutePath}")
                        fallback.copyTo(modelFile)
                    }
                }

                if (!modelFile.exists()) {
                    onStatus("Downloading $modelName model...")
                    val downloader = ModelDownloader()
                    val success = downloader.downloadModel(modelName, modelFile) { progress ->
                        onStatus("Downloading $modelName: $progress%")
                    }
                    if (!success) {
                        onStatus("Failed to download model")
                        return@launch
                    }
                }

                onStatus("Loading model...")
                whisperContext = WhisperContext.createContextFromFile(modelFile.absolutePath)
                onStatus("Ready")
                Log.i(TAG, "Whisper initialized with model: ${modelFile.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error in TranscriptionEngine initialization", e)
                onStatus("Error: ${e.message}")
            }
        }
    }

    fun transcribeMediaFile(mediaFile: File, onResult: (String) -> Unit) {
        scope.launch {
            val currentContext = whisperContext
            if (currentContext == null) {
                Log.w(TAG, "Engine not initialized. Skipping transcription.")
                return@launch
            }

            try {
                Log.i(TAG, "Converting ${mediaFile.name}...")
                val floatData = converter.convertTo16kHzMono(mediaFile)
                if (floatData != null) {
                    Log.i(TAG, "Transcribing ${mediaFile.name}...")
                    val transcript = currentContext.transcribeData(floatData)
                    onResult(transcript)
                } else {
                    Log.e(TAG, "Failed to convert media file")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error transcribing media file: ${e.message}")
            }
        }
    }

    fun transcribeUtterance(pcmData: ByteArray, onResult: (String) -> Unit) {
        transcriptionJob?.cancel() // Cancel previous if still running
        transcriptionJob = scope.launch {
            val currentContext = whisperContext
            if (currentContext == null) return@launch

            try {
                // Convert 16-bit PCM (Little Endian) to FloatArray (-1.0 to 1.0)
                val floatData = FloatArray(pcmData.size / 2)
                for (i in floatData.indices) {
                    val low = pcmData[i * 2].toInt() and 0xFF
                    val high = pcmData[i * 2 + 1].toInt()
                    val sample = ((high shl 8) or low).toShort()
                    floatData[i] = sample.toFloat() / 32768.0f
                }

                val transcript = currentContext.transcribeData(floatData, printTimestamp = false)
                if (transcript.isNotEmpty()) {
                    onResult(transcript)
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Log.e(TAG, "Error transcribing utterance: ${e.message}")
                }
            }
        }
    }

    fun release() {
        scope.launch {
            whisperContext?.release()
            whisperContext = null
            scope.cancel()
        }
    }
}
