package com.example.domentiacare.data.local.schedule

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule")
data class Schedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

//    val userId: Long,
//    val creatorId: Long,

    val title: String,
    val description: String,

    val startDate: String, // ISO_LOCAL_DATE_TIME 포맷 (ex: "2025-05-27T14:30:00")
    val endDate: String,

    val isAi: Boolean = false,
    val isCompleted: Boolean = false,
//    val notificationSent: Boolean = false,

    val isSynced: Boolean = false, // 클라이언트 전송 여부

    val recordName: String? = null
)