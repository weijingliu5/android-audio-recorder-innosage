package com.innosage.androidagentictemplate.data

import android.util.Log

class SemanticProcessor {
    data class AnalysisResult(
        val intentLabel: String?,
        val urgencyScore: Float,
        val contextTags: String?
    )

    fun process(transcript: String, energyScore: Double): AnalysisResult {
        var intentLabel: String? = null
        var keywordWeight = 0.0f

        val upperTranscript = transcript.uppercase()
        
        when {
            upperTranscript.contains("[DECISION]") || upperTranscript.contains("DECISION") -> {
                intentLabel = "DECISION"
                keywordWeight = 0.8f
            }
            upperTranscript.contains("[BLOCKER]") || upperTranscript.contains("BLOCKER") -> {
                intentLabel = "BLOCKER"
                keywordWeight = 1.0f
            }
            upperTranscript.contains("[IDEA]") || upperTranscript.contains("IDEA") -> {
                intentLabel = "IDEA"
                keywordWeight = 0.5f
            }
            upperTranscript.contains("[FOLLOWUP]") || upperTranscript.contains("FOLLOWUP") -> {
                intentLabel = "FOLLOWUP"
                keywordWeight = 0.6f
            }
        }

        // Urgency Score: Calculate using a weighted formula (e.g., keyword weight + energy score).
        // normalizedEnergy: 0.0 to 1.0 (assuming max reasonable RMS is around 32767/4 or something, let's use 5000 as max for normalization)
        val normalizedEnergy = (energyScore / 5000.0).coerceAtMost(1.0).toFloat()
        val urgencyScore = (keywordWeight * 0.7f + normalizedEnergy * 0.3f)

        Log.d("SemanticProcessor", "Analyzed: intent=$intentLabel, urgency=$urgencyScore, energy=$energyScore")

        return AnalysisResult(
            intentLabel = intentLabel,
            urgencyScore = urgencyScore,
            contextTags = null
        )
    }
}
