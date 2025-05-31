package com.example.domentiacare.ui.screen.call.models

data class RecordLog(
    val name: String,
    val type: String,
    val time: String
)

data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)