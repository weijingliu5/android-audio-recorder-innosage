package com.innosage.androidagentictemplate.whisper

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "ModelDownloader"

class ModelDownloader {
    // Standard models from ggerganov's HuggingFace repo
    private val modelUrls = mapOf(
        "tiny" to "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin",
        "base" to "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin",
        "small" to "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin",
        "medium" to "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium.bin"
    )

    suspend fun downloadModel(modelName: String, destination: File, onProgress: (Int) -> Unit): Boolean = withContext(Dispatchers.IO) {
        val urlString = modelUrls[modelName] ?: return@withContext false
        val url = URL(urlString)
        
        try {
            Log.i(TAG, "Downloading model $modelName from $urlString")
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Server returned HTTP ${connection.responseCode}")
                return@withContext false
            }

            val fileLength = connection.contentLength
            val input = connection.inputStream
            val output = FileOutputStream(destination)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            while (input.read(data).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    onProgress((total * 100 / fileLength).toInt())
                }
                output.write(data, 0, count)
            }

            output.flush()
            output.close()
            input.close()
            Log.i(TAG, "Model $modelName downloaded to ${destination.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download model", e)
            if (destination.exists()) destination.delete()
            false
        }
    }
}
