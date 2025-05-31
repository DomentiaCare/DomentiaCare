package com.example.domentiacare.data.util

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

class WavWriter(private val file: File, private val sampleRate: Int, private val channelCount: Int) {
    private val outputStream = BufferedOutputStream(FileOutputStream(file))
    private var totalAudioLen = 0

    fun writeHeader() {
        val byteRate = sampleRate * channelCount * 2
        val header = ByteArray(44)
        val totalDataLen = totalAudioLen + 36

        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()

        writeInt(header, 4, totalDataLen)
        header[8] = 'W'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[16] = 16
        header[20] = 1
        header[22] = channelCount.toByte()
        writeInt(header, 24, sampleRate)
        writeInt(header, 28, byteRate)
        header[32] = (channelCount * 2).toByte()
        header[34] = 16
        header[36] = 'd'.code.toByte()
        header[40] = 'a'.code.toByte()

        outputStream.write(header, 0, 44)
    }

    fun writeData(data: ByteArray) {
        totalAudioLen += data.size
        outputStream.write(data)
    }

    fun finish() {
        outputStream.flush()
        outputStream.close()
        RandomAccessFile(file, "rw").use {
            it.seek(4)
            it.writeIntLE(36 + totalAudioLen)
            it.seek(40)
            it.writeIntLE(totalAudioLen)
        }
    }

    private fun writeInt(array: ByteArray, offset: Int, value: Int) {
        array[offset] = (value and 0xff).toByte()
        array[offset + 1] = ((value shr 8) and 0xff).toByte()
        array[offset + 2] = ((value shr 16) and 0xff).toByte()
        array[offset + 3] = ((value shr 24) and 0xff).toByte()
    }

    private fun RandomAccessFile.writeIntLE(value: Int) {
        write(byteArrayOf(
            (value and 0xff).toByte(),
            ((value shr 8) and 0xff).toByte(),
            ((value shr 16) and 0xff).toByte(),
            ((value shr 24) and 0xff).toByte()
        ))
    }
}
