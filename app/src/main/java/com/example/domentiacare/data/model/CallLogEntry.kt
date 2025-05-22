package com.example.domentiacare.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CallLogEntry(
    val id: String,
    val name: String?,
    val number: String,
    val type: Int,        // INCOMING_TYPE, OUTGOING_TYPE 등
    val date: Long,       // epoch millis
    val duration: Long,    // 초 단위
    val recordingPath: String? = null,
) {
    val callType: CallType
        get() = CallType.fromInt(type)

    val callTypeText: String
        get() = callType.displayName

    val formattedDate: String
        get() = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", date).toString()

    val formattedDuration: String
        get() = "${duration / 60}분 ${duration % 60}초"
}

fun CallLogEntry.dateFormattedForShortFileName(): String {
    val sdf = SimpleDateFormat("yyMMdd", Locale.getDefault())
    return sdf.format(Date(this.date))
}

fun String.onlyDigits(): String = this.filter { it.isDigit() }