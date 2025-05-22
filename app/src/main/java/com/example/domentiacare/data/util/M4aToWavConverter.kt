package com.example.domentiacare.data.util

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File

fun convertM4aToWav(inputFile: File, outputFile: File) {
    val extractor = MediaExtractor()
    extractor.setDataSource(inputFile.absolutePath)

    var format: MediaFormat? = null
    var trackIndex = -1

    for (i in 0 until extractor.trackCount) {
        val trackFormat = extractor.getTrackFormat(i)
        val mime = trackFormat.getString(MediaFormat.KEY_MIME)
        if (mime?.startsWith("audio/") == true) {
            format = trackFormat
            trackIndex = i
            break
        }
    }

    if (format == null || trackIndex == -1) throw RuntimeException("No audio track found")

    extractor.selectTrack(trackIndex)

    val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
    codec.configure(format, null, null, 0)
    codec.start()

    val bufferInfo = MediaCodec.BufferInfo()
    val wavWriter = WavWriter(outputFile, format.getInteger(MediaFormat.KEY_SAMPLE_RATE), format.getInteger(MediaFormat.KEY_CHANNEL_COUNT))
    wavWriter.writeHeader()

    var sawInputEOS = false
    var sawOutputEOS = false

    while (!sawOutputEOS) {
        if (!sawInputEOS) {
            val inputBufferIndex = codec.dequeueInputBuffer(10000)
            if (inputBufferIndex >= 0) {
                val inputBuffer = codec.getInputBuffer(inputBufferIndex)!!
                val sampleSize = extractor.readSampleData(inputBuffer, 0)

                if (sampleSize < 0) {
                    codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    sawInputEOS = true
                } else {
                    val presentationTimeUs = extractor.sampleTime
                    codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTimeUs, 0)
                    extractor.advance()
                }
            }
        }

        val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
        if (outputBufferIndex >= 0) {
            val outputBuffer = codec.getOutputBuffer(outputBufferIndex)!!
            val chunk = ByteArray(bufferInfo.size)
            outputBuffer.get(chunk)
            outputBuffer.clear()

            wavWriter.writeData(chunk)
            codec.releaseOutputBuffer(outputBufferIndex, false)

            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                sawOutputEOS = true
            }
        }
    }

    codec.stop()
    codec.release()
    extractor.release()

    wavWriter.finish()
}
