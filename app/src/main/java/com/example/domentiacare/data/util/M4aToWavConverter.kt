package com.example.domentiacare.data.util

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import kotlin.math.roundToInt

fun convertM4aToWavForWhisper(inputFile: File, outputFile: File) {
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

    // 원본 오디오 정보
    val originalSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
    val originalChannelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

    // Whisper 요구사항: 16kHz, 모노채널, 16비트 PCM
    val targetSampleRate = 16000
    val targetChannelCount = 1
    val targetBitDepth = 16

    val wavWriter = WhisperWavWriter(
        outputFile,
        targetSampleRate,
        targetChannelCount,
        targetBitDepth
    )
    wavWriter.writeHeader()

    var sawInputEOS = false
    var sawOutputEOS = false

    // 리샘플링을 위한 변수들
    val resampleRatio = originalSampleRate.toDouble() / targetSampleRate.toDouble()
    var inputSampleIndex = 0.0

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

            // PCM 데이터를 Whisper 형식으로 변환
            val convertedData = convertAudioForWhisper(
                chunk,
                originalSampleRate,
                originalChannelCount,
                targetSampleRate,
                targetChannelCount
            )

            wavWriter.writeData(convertedData)
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

/**
 * PCM 오디오 데이터를 Whisper 요구사항에 맞게 변환
 * 16kHz, 모노채널, 16비트로 변환
 */
private fun convertAudioForWhisper(
    inputData: ByteArray,
    originalSampleRate: Int,
    originalChannelCount: Int,
    targetSampleRate: Int,
    targetChannelCount: Int
): ByteArray {
    // 16비트 PCM 데이터로 변환 (Little Endian)
    val samples = ShortArray(inputData.size / 2)
    for (i in samples.indices) {
        samples[i] = ((inputData[i * 2].toInt() and 0xFF) or
                (inputData[i * 2 + 1].toInt() shl 8)).toShort()
    }

    // 스테레오에서 모노로 변환 (필요한 경우)
    val monoSamples = if (originalChannelCount > 1) {
        convertToMono(samples, originalChannelCount)
    } else {
        samples
    }

    // 샘플링 레이트 변환 (필요한 경우)
    val resampledSamples = if (originalSampleRate != targetSampleRate) {
        resample(monoSamples, originalSampleRate, targetSampleRate)
    } else {
        monoSamples
    }

    // 다시 바이트 배열로 변환
    val outputData = ByteArray(resampledSamples.size * 2)
    for (i in resampledSamples.indices) {
        val sample = resampledSamples[i].toInt()
        outputData[i * 2] = (sample and 0xFF).toByte()
        outputData[i * 2 + 1] = (sample shr 8 and 0xFF).toByte()
    }

    return outputData
}

/**
 * 멀티채널 오디오를 모노채널로 변환
 */
private fun convertToMono(samples: ShortArray, channelCount: Int): ShortArray {
    val monoSamples = ShortArray(samples.size / channelCount)

    for (i in monoSamples.indices) {
        var sum = 0L
        for (ch in 0 until channelCount) {
            sum += samples[i * channelCount + ch]
        }
        monoSamples[i] = (sum / channelCount).toShort()
    }

    return monoSamples
}

/**
 * 단순 리샘플링 (Linear Interpolation)
 */
private fun resample(samples: ShortArray, originalRate: Int, targetRate: Int): ShortArray {
    if (originalRate == targetRate) return samples

    val ratio = originalRate.toDouble() / targetRate.toDouble()
    val outputLength = (samples.size / ratio).roundToInt()
    val resampled = ShortArray(outputLength)

    for (i in resampled.indices) {
        val srcIndex = i * ratio
        val srcIndexInt = srcIndex.toInt()
        val fraction = srcIndex - srcIndexInt

        val sample1 = if (srcIndexInt < samples.size) samples[srcIndexInt] else 0
        val sample2 = if (srcIndexInt + 1 < samples.size) samples[srcIndexInt + 1] else sample1

        resampled[i] = (sample1 + fraction * (sample2 - sample1)).roundToInt().toShort()
    }

    return resampled
}

/**
 * Whisper 전용 WAV 파일 작성기
 * 16kHz, 모노채널, 16비트 PCM 형식으로 고정
 */
class WhisperWavWriter(
    private val file: File,
    private val sampleRate: Int = 16000,
    private val channels: Int = 1,
    private val bitsPerSample: Int = 16
) {
    private val outputStream = file.outputStream()
    private var dataSize = 0

    fun writeHeader() {
        // WAV 헤더 작성 (44바이트)
        val header = ByteArray(44)

        // RIFF 헤더
        "RIFF".toByteArray().copyInto(header, 0)
        // 파일 크기 (나중에 업데이트)
        writeInt32LE(header, 4, 0)
        "WAVE".toByteArray().copyInto(header, 8)

        // fmt 청크
        "fmt ".toByteArray().copyInto(header, 12)
        writeInt32LE(header, 16, 16) // fmt 청크 크기
        writeInt16LE(header, 20, 1)  // PCM 형식
        writeInt16LE(header, 22, channels.toShort())
        writeInt32LE(header, 24, sampleRate)
        writeInt32LE(header, 28, sampleRate * channels * bitsPerSample / 8) // 바이트 레이트
        writeInt16LE(header, 32, (channels * bitsPerSample / 8).toShort()) // 블록 정렬
        writeInt16LE(header, 34, bitsPerSample.toShort())

        // data 청크
        "data".toByteArray().copyInto(header, 36)
        writeInt32LE(header, 40, 0) // 데이터 크기 (나중에 업데이트)

        outputStream.write(header)
    }

    fun writeData(data: ByteArray) {
        outputStream.write(data)
        dataSize += data.size
    }

    fun finish() {
        // 파일 크기 정보 업데이트
        outputStream.close()

        val randomAccessFile = java.io.RandomAccessFile(file, "rw")
        randomAccessFile.seek(4)
        randomAccessFile.writeInt(Integer.reverseBytes(36 + dataSize)) // 전체 파일 크기 - 8
        randomAccessFile.seek(40)
        randomAccessFile.writeInt(Integer.reverseBytes(dataSize)) // 데이터 크기
        randomAccessFile.close()
    }

    private fun writeInt32LE(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value and 0xFF).toByte()
        buffer[offset + 1] = (value shr 8 and 0xFF).toByte()
        buffer[offset + 2] = (value shr 16 and 0xFF).toByte()
        buffer[offset + 3] = (value shr 24 and 0xFF).toByte()
    }

    private fun writeInt16LE(buffer: ByteArray, offset: Int, value: Short) {
        buffer[offset] = (value.toInt() and 0xFF).toByte()
        buffer[offset + 1] = (value.toInt() shr 8 and 0xFF).toByte()
    }
}