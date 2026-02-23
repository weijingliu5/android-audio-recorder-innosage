package com.innosage.androidagentictemplate.whisper

import android.content.res.AssetManager
import android.util.Log
import kotlinx.coroutines.*
import java.io.InputStream
import java.util.concurrent.Executors

private const val LOG_TAG = "WhisperContext"

class WhisperContext private constructor(private var ptr: Long) {
    private val scope: CoroutineScope = CoroutineScope(
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    )

    suspend fun transcribeData(data: FloatArray, printTimestamp: Boolean = true): String = withContext(scope.coroutineContext) {
        if (ptr == 0L) return@withContext ""
        val numThreads = 4 // Default or detect
        Log.d(LOG_TAG, "Transcribing with $numThreads threads")
        val result = WhisperLib.fullTranscribe(ptr, numThreads, data)
        if (result != 0) {
            Log.e(LOG_TAG, "Transcription failed: $result")
            return@withContext ""
        }
        val textCount = WhisperLib.getTextSegmentCount(ptr)
        return@withContext buildString {
            for (i in 0 until textCount) {
                if (printTimestamp) {
                    val textTimestamp = "[${toTimestamp(WhisperLib.getTextSegmentT0(ptr, i))} --> ${toTimestamp(WhisperLib.getTextSegmentT1(ptr, i))}]"
                    val textSegment = WhisperLib.getTextSegment(ptr, i)
                    append("$textTimestamp: $textSegment\n")
                } else {
                    append(WhisperLib.getTextSegment(ptr, i))
                }
            }
        }.trim()
    }

    suspend fun release() = withContext(scope.coroutineContext) {
        if (ptr != 0L) {
            WhisperLib.freeContext(ptr)
            ptr = 0
        }
    }

    companion object {
        fun createContextFromFile(filePath: String): WhisperContext {
            val ptr = WhisperLib.initContext(filePath)
            if (ptr == 0L) {
                throw RuntimeException("Couldn't create context with path $filePath")
            }
            return WhisperContext(ptr)
        }

        fun createContextFromInputStream(stream: InputStream): WhisperContext {
            val ptr = WhisperLib.initContextFromInputStream(stream)
            if (ptr == 0L) {
                throw RuntimeException("Couldn't create context from input stream")
            }
            return WhisperContext(ptr)
        }

        fun createContextFromAsset(assetManager: AssetManager, assetPath: String): WhisperContext {
            val ptr = WhisperLib.initContextFromAsset(assetManager, assetPath)
            if (ptr == 0L) {
                throw RuntimeException("Couldn't create context from asset $assetPath")
            }
            return WhisperContext(ptr)
        }
        
        private fun toTimestamp(t: Long): String {
            var msec = t * 10
            val hr = msec / (1000 * 60 * 60)
            msec -= hr * (1000 * 60 * 60)
            val min = msec / (1000 * 60)
            msec -= min * (1000 * 60)
            val sec = msec / 1000
            msec -= sec * 1000
            return String.format("%02d:%02d:%02d.%03d", hr, min, sec, msec)
        }
    }
}
