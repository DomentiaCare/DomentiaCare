package com.example.domentiacare.data.remote.dto

data class Patient(
    val patientId: Long,
    val patientName: String,
    val gender: String,
    val age: String,
    val address: String,
    val addressDetail1: String,
    val addressDetail2: String,
    val phone: String,
    val isescape: Boolean
)