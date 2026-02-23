package com.innosage.androidagentictemplate.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UtteranceDao {
    @Insert
    suspend fun insert(utterance: UtteranceEntity)

    @Query("SELECT * FROM utterances ORDER BY timestamp ASC")
    fun getRecentUtterancesFlow(): Flow<List<UtteranceEntity>>

    @Query("SELECT * FROM utterances ORDER BY timestamp DESC LIMIT :limit")
    suspend fun queryRecent(limit: Int): List<UtteranceEntity>

    @Query("DELETE FROM utterances WHERE timestamp < :threshold")
    suspend fun deleteOlderThan(threshold: Long)
}
