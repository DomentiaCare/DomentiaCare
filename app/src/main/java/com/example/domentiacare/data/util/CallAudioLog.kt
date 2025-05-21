package com.example.domentiacare.data.util

import java.io.File

fun getCallRecordingFiles(): List<File> {
    val paths = listOf(
        "/storage/emulated/0/Call",
        "/storage/emulated/0/Sounds/Call",
        "/storage/emulated/0/Record",
        "/storage/emulated/0/Recording",
        "/sdcard/Recordings/Call/"
    )

    for (path in paths) {
        val dir = File(path)
        if (dir.exists()) {
            val audioFiles = dir.listFiles { file ->
                file.extension in listOf("mp3", "m4a", "3gp", "amr", "wav")
            }
            if (!audioFiles.isNullOrEmpty()) {
                return audioFiles.sortedByDescending { it.lastModified() }
            }
        }
    }

    return emptyList()
}