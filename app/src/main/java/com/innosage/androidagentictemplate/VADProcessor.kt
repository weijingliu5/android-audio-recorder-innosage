package com.innosage.androidagentictemplate

import kotlin.math.sqrt

/**
 * Voice Activity Detection Processor.
 * Handles RMS-based speech detection and hangover logic.
 */
class VADProcessor(
    private val threshold: Double = 800.0,
    private val hangoverMs: Long = 1000L,
    private val silenceThresholdMs: Long = 1500L
) {
    private var lastVoiceTime = 0L
    private var utteranceStarted = false

    /**
     * Determines if the given PCM 16-bit buffer contains speech based on RMS threshold.
     * Returns Pair(voiced, rms)
     */
    fun isVoiced(buffer: ShortArray, size: Int): Pair<Boolean, Double> {
        if (size <= 0) return Pair(false, 0.0)
        var sum = 0.0
        for (i in 0 until size) {
            sum += buffer[i].toDouble() * buffer[i]
        }
        val rms = sqrt(sum / size)
        val voiced = rms > threshold
        if (voiced) {
            utteranceStarted = true
        }
        return Pair(voiced, rms)
    }

    /**
     * Determines if the given ByteArray (PCM 16-bit Little Endian) contains speech.
     * Returns Pair(voiced, rms)
     */
    fun isVoiced(byteBuffer: ByteArray): Pair<Boolean, Double> {
        val shortBuffer = ShortArray(byteBuffer.size / 2)
        for (i in shortBuffer.indices) {
            val low = byteBuffer[i * 2].toInt() and 0xFF
            val high = byteBuffer[i * 2 + 1].toInt()
            shortBuffer[i] = ((high shl 8) or low).toShort()
        }
        return isVoiced(shortBuffer, shortBuffer.size)
    }

    /**
     * Determines if the current frame should be recorded based on current voice status
     * and the hangover period.
     */
    fun shouldRecord(voiced: Boolean, nowMs: Long): Boolean {
        if (voiced) {
            lastVoiceTime = nowMs
            return true
        }
        return (nowMs - lastVoiceTime) < hangoverMs
    }

    /**
     * Checks if a discrete utterance has likely ended (silence > threshold).
     */
    fun isUtteranceComplete(nowMs: Long): Boolean {
        if (!utteranceStarted) return false
        return (nowMs - lastVoiceTime) >= silenceThresholdMs
    }

    /**
     * Resets the internal state (e.g. last voice time).
     */
    fun reset() {
        lastVoiceTime = 0L
        utteranceStarted = false
    }

    /**
     * Resets the utterance tracking after one is processed.
     */
    fun resetUtterance() {
        utteranceStarted = false
    }
}
