package com.example.domentiacare.data.local.schedule

import com.example.domentiacare.data.local.CurrentUser

data class ScheduleDto(

    val title: String,
    val description: String,

    val startDate: String, // ISO_LOCAL_DATE_TIME 포맷 (ex: "2025-05-27T14:30:00")
    val endDate: String,

    val isAi: Boolean = false,
    val isCompleted: Boolean = false,
//    val notificationSent: Boolean = false,

//    val isSynced: Boolean = false, // 클라이언트 전송 여부

    val recordName: String? = null,
    val patientId: Long? = CurrentUser.user?.id, // 환자 ID (null일 경우 보호자 일정으로 간주)
)