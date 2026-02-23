package com.innosage.androidagentictemplate

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val TAG = "AudioConverter"
private const val TARGET_SAMPLE_RATE = 16000

class AudioConverter {
    /**
     * Extracts and resamples audio from a media file (M4A, MP4, etc.) to 16kHz mono PCM.
     * @return FloatArray of PCM samples, or null if failed.
     */
    fun convertTo16kHzMono(mediaFile: File): FloatArray? {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(mediaFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set data source: ${e.message}")
            return null
        }

        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                trackIndex = i
                format = f
                break
            }
        }

        if (trackIndex < 0 || format == null) {
            Log.e(TAG, "No audio track found in ${mediaFile.name}")
            extractor.release()
            return null
        }

        extractor.selectTrack(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val pcmOut = ByteArrayOutputStream()
        val info = MediaCodec.BufferInfo()
        var sawInputEOS = false
        var sawOutputEOS = false
        
        val inputBuffers = codec.inputBuffers
        val outputBuffers = codec.outputBuffers

        var inputSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var inputChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        Log.i(TAG, "Input: $inputSampleRate Hz, $inputChannels channels")

        while (!sawOutputEOS) {
            if (!sawInputEOS) {
                val inputBufferIndex = codec.dequeueInputBuffer(10000)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = inputBuffers[inputBufferIndex]
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        sawInputEOS = true
                    } else {
                        codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            val outputBufferIndex = codec.dequeueOutputBuffer(info, 10000)
            if (outputBufferIndex >= 0) {
                val outputBuffer = outputBuffers[outputBufferIndex]
                val chunk = ByteArray(info.size)
                outputBuffer.get(chunk)
                outputBuffer.clear()
                pcmOut.write(chunk)
                codec.releaseOutputBuffer(outputBufferIndex, false)

                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    sawOutputEOS = true
                }
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = codec.outputFormat
                inputSampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                inputChannels = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                Log.i(TAG, "Output format changed: $inputSampleRate Hz, $inputChannels channels")
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        val rawPcm = pcmOut.toByteArray()
        return processPcm(rawPcm, inputSampleRate, inputChannels)
    }

    private fun processPcm(pcmData: ByteArray, sampleRate: Int, channels: Int): FloatArray {
        // Assume PCM 16-bit
        val shortBuffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val numSamples = shortBuffer.remaining()
        
        // Downmix to mono if needed
        val monoPcm = if (channels > 1) {
            val result = ShortArray(numSamples / channels)
            for (i in result.indices) {
                var sum = 0
                for (c in 0 until channels) {
                    sum += shortBuffer.get()
                }
                result[i] = (sum / channels).toShort()
            }
            result
        } else {
            val result = ShortArray(numSamples)
            shortBuffer.get(result)
            result
        }

        // Resample to 16kHz
        val resampled = if (sampleRate != TARGET_SAMPLE_RATE) {
            resample(monoPcm, sampleRate, TARGET_SAMPLE_RATE)
        } else {
            monoPcm
        }

        // Convert to FloatArray
        val floatData = FloatArray(resampled.size)
        for (i in floatData.indices) {
            floatData[i] = resampled[i].toFloat() / 32768.0f
        }
        return floatData
    }

    private fun resample(input: ShortArray, inputRate: Int, targetRate: Int): ShortArray {
        if (inputRate == targetRate) return input
        val ratio = inputRate.toDouble() / targetRate
        val outputSize = (input.size / ratio).toInt()
        val output = ShortArray(outputSize)
        for (i in 0 until outputSize) {
            val pos = i * ratio
            val index = pos.toInt()
            val frac = pos - index
            if (index + 1 < input.size) {
                val s1 = input[index].toInt()
                val s2 = input[index + 1].toInt()
                output[i] = (s1 + frac * (s2 - s1)).toInt().toShort()
            } else {
                output[i] = input[index]
            }
        }
        return output
    }
}
