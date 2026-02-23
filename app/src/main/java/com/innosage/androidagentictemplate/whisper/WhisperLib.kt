package com.innosage.androidagentictemplate.whisper

import android.content.res.AssetManager
import java.io.InputStream

class WhisperLib {
    companion object {
        /**
         * Initializes the Whisper context with the provided model path.
         * @param modelPath Absolute path to the .bin model file.
         * @return A pointer (long) to the whisper_context, or 0 if failed.
         */
        @JvmStatic
        external fun initContext(modelPath: String): Long

        @JvmStatic
        external fun initContextFromInputStream(inputStream: InputStream): Long

        @JvmStatic
        external fun initContextFromAsset(assetManager: AssetManager, assetPath: String): Long

        /**
         * Frees the Whisper context.
         * @param contextPtr The pointer returned by initContext.
         */
        @JvmStatic
        external fun freeContext(contextPtr: Long)

        /**
         * Transcribes audio data.
         * @param contextPtr The pointer to the whisper_context.
         * @param numThreads Number of threads to use.
         * @param audioData Float array of PCM audio (16kHz, mono).
         * @return 0 on success, non-zero on error.
         */
        @JvmStatic
        external fun fullTranscribe(contextPtr: Long, numThreads: Int, audioData: FloatArray): Int

        /**
         * Returns the number of text segments from the last transcription.
         */
        @JvmStatic
        external fun getTextSegmentCount(contextPtr: Long): Int

        /**
         * Returns a specific text segment.
         */
        @JvmStatic
        external fun getTextSegment(contextPtr: Long, index: Int): String

        @JvmStatic
        external fun getTextSegmentT0(contextPtr: Long, index: Int): Long

        @JvmStatic
        external fun getTextSegmentT1(contextPtr: Long, index: Int): Long

        /**
         * Returns system information (AVX, NEON, etc).
         */
        @JvmStatic
        external fun getSystemInfo(): String

        init {
            System.loadLibrary("whisper")
        }
    }
}
