package com.example.domentiacare.data.util

import android.content.Context
import com.example.domentiacare.data.model.CallLogEntry
import android.provider.CallLog

fun queryCallLogs(context: Context): List<CallLogEntry> {
    val uri = CallLog.Calls.CONTENT_URI
    val projection = arrayOf(
        CallLog.Calls._ID,
        CallLog.Calls.CACHED_NAME,
        CallLog.Calls.NUMBER,
        CallLog.Calls.TYPE,
        CallLog.Calls.DATE,
        CallLog.Calls.DURATION
    )

    // DATE 내림차순 정렬
    val cursor = context.contentResolver.query(
        uri, projection, null, null,
        "${CallLog.Calls.DATE} DESC"
    ) ?: return emptyList()

    var count = 0 //
    val maxCount = 50 // 최근 50개만 가져오기
    val list = mutableListOf<CallLogEntry>()
    cursor.use {
        val idxId       = it.getColumnIndexOrThrow(CallLog.Calls._ID)
        val idxName     = it.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
        val idxNumber   = it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
        val idxType     = it.getColumnIndexOrThrow(CallLog.Calls.TYPE)
        val idxDate     = it.getColumnIndexOrThrow(CallLog.Calls.DATE)
        val idxDuration = it.getColumnIndexOrThrow(CallLog.Calls.DURATION)

        // Cursor를 한 줄씩 순회
        while (it.moveToNext() && count < maxCount) {
            list += CallLogEntry(
                id       = it.getString(idxId),
                name     = it.getString(idxName),
                number   = it.getString(idxNumber),
                type     = it.getInt(idxType),
                date     = it.getLong(idxDate),
                duration = it.getLong(idxDuration)
            )
            count++
        }
    }
    return list
}