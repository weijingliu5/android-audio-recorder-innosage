package com.innosage.androidagentictemplate.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "utterances")
data class UtteranceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val duration: Long,
    val transcript: String,
    val energyScore: Double,
    val metadata: String? = null,
    val intentLabel: String? = null,
    val urgencyScore: Float = 0.0f,
    val contextTags: String? = null
)
