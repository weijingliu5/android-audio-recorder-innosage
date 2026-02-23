package com.innosage.androidagentictemplate.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Simplified entity for Whisper.cpp validation transcripts.
 */
@Entity(tableName = "utterances")
data class UtteranceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val duration: Long,
    val transcript: String,
    val energyScore: Double = 0.0
)
