package com.innosage.androidagentictemplate

/**
 * Super simple Voice Activity Detector using RMS threshold.
 * Used for Whisper.cpp validation focus.
 */
class VADProcessor(
    private val threshold: Double,
    private val hangoverMs: Long,
    private val silenceThresholdMs: Long
) {
    private var lastVoiceTime = 0L
    private var utteranceStartTime = 0L
    private var isCurrentlyVoiced = false

    fun isVoiced(audioData: ShortArray, size: Int): Pair<Boolean, Double> {
        var sum = 0.0
        for (i in 0 until size) {
            sum += audioData[i] * audioData[i]
        }
        val rms = Math.sqrt(sum / size)
        val voiced = rms > threshold
        
        if (voiced) {
            lastVoiceTime = System.currentTimeMillis()
            if (utteranceStartTime == 0L) utteranceStartTime = lastVoiceTime
        }
        
        isCurrentlyVoiced = voiced
        return voiced to rms
    }

    fun shouldRecord(isVoiced: Boolean, now: Long): Boolean {
        return isVoiced || (now - lastVoiceTime < hangoverMs)
    }

    fun isUtteranceComplete(now: Long): Boolean {
        return utteranceStartTime != 0L && !isCurrentlyVoiced && (now - lastVoiceTime > silenceThresholdMs)
    }

    fun reset() {
        lastVoiceTime = 0L
        utteranceStartTime = 0L
        isCurrentlyVoiced = false
    }

    fun resetUtterance() {
        utteranceStartTime = 0L
    }
}
