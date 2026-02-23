package com.innosage.androidagentictemplate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.innosage.androidagentictemplate.whisper.WhisperContext
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class WhisperTranscriptionTest {

    @Test
    fun testTranscriptionWorks() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val transcriptionEngine = TranscriptionEngine(context)
        
        val filesDir = context.getExternalFilesDir(null)
        val modelFile = File(filesDir, "ggml-small.bin")
        val pushedModel = File(filesDir, "whisper-small.q8_0.bin")
        
        if (pushedModel.exists() && !modelFile.exists()) {
            pushedModel.copyTo(modelFile)
        }
        
        val audioFile = File(filesDir, "jfk.pcm")
        
        assertTrue("Model file should exist at ${modelFile.absolutePath}", modelFile.exists())
        assertTrue("Audio file should exist at ${audioFile.absolutePath}", audioFile.exists())
        
        val latch = CountDownLatch(1)
        var resultText = ""
        
        transcriptionEngine.initialize("small")
        
        // Wait for initialization
        Thread.sleep(10000)
        
        val pcmData = audioFile.readBytes()
        
        transcriptionEngine.transcribeUtterance(pcmData) { text ->
            resultText = text
            latch.countDown()
        }
        
        val completed = latch.await(60, TimeUnit.SECONDS)
        assertTrue("Transcription should complete within timeout", completed)
        assertTrue("Transcription result should not be empty. Got: '$resultText'", resultText.isNotEmpty())
        
        // The JFK speech contains "fellow"
        assertTrue("Transcription should contain expected text. Got: '$resultText'", 
            resultText.contains("fellow", ignoreCase = true))
            
        transcriptionEngine.release()
    }
}
