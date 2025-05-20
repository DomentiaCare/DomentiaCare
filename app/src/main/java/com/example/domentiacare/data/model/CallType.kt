package com.example.domentiacare.data.model

enum class CallType(val type: Int) {
    INCOMING_TYPE(1),
    OUTGOING_TYPE(2),
    MISSED_TYPE(3),
    VOICE_MAIL_TYPE(4),
    ALL(5);

    val displayName: String
        get() = when(this) {
            INCOMING_TYPE -> "수신  "
            OUTGOING_TYPE -> "발신  "
            MISSED_TYPE -> "부재중"
            VOICE_MAIL_TYPE -> "메시지"
            ALL -> "전화기록"
        }

    companion object {
        fun fromInt(type: Int): CallType {
            return values().firstOrNull { it.type == type } ?: ALL
        }
    }
}