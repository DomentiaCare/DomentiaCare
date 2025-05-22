package com.example.domentiacare.data.util

import android.content.Context
import java.io.File

fun getCallRecordingFiles(context: Context): List<File> {
    val results = mutableListOf<File>()

    // 1. 앱 내 전용 저장소 확인
    val internalRecordingsDir = File(context.getExternalFilesDir(null), "Recordings")
    if (internalRecordingsDir.exists()) {
        val internalFiles = internalRecordingsDir.listFiles { file ->
            file.extension in listOf("mp3", "m4a", "3gp", "amr", "wav")
        } ?: emptyArray()
        results.addAll(internalFiles)
    }

    // 2. 외부 저장소의 일반적인 경로들 확인
    val externalPaths = listOf(
        "/storage/emulated/0/Call",
        "/storage/emulated/0/Sounds/Call",
        "/storage/emulated/0/Record",
        "/storage/emulated/0/Recording",
        "/sdcard/Recordings/Call/"
    )

    for (path in externalPaths) {
        val dir = File(path)
        if (dir.exists()) {
            val audioFiles = dir.listFiles { file ->
                file.extension in listOf("mp3", "m4a", "3gp", "amr", "wav")
            } ?: continue
            results.addAll(audioFiles)
        }
    }

    // 3. 최근 녹음 파일 순으로 정렬
    return results.sortedByDescending { it.lastModified() }
}
