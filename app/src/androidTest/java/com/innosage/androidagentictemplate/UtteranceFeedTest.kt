package com.innosage.androidagentictemplate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.innosage.androidagentictemplate.data.AppDatabase
import com.innosage.androidagentictemplate.data.UtteranceEntity
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UtteranceFeedTest {
    @Test
    fun insertDummyUtterances() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = AppDatabase.getDatabase(context)
        val utteranceDao = database.utteranceDao()
        
        val dummyData = listOf(
            UtteranceEntity(
                timestamp = System.currentTimeMillis() - 10000,
                duration = 5000,
                transcript = "This is a dummy decision",
                energyScore = 0.9,
                intentLabel = "decision"
            ),
            UtteranceEntity(
                timestamp = System.currentTimeMillis() - 5000,
                duration = 3000,
                transcript = "This is a dummy blocker",
                energyScore = 0.7,
                intentLabel = "blocker"
            ),
            UtteranceEntity(
                timestamp = System.currentTimeMillis(),
                duration = 2000,
                transcript = "This is a dummy idea",
                energyScore = 0.85,
                intentLabel = "idea"
            )
        )
        
        dummyData.forEach { utteranceDao.insert(it) }
    }
}
