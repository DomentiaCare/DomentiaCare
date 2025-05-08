package com.example.domentiacare.data.remote.dto

import java.time.LocalDate

data class Schedule(
    val date: LocalDate,
    val time: String,
    val content: String
)