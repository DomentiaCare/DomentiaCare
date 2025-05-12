package com.example.domentiacare.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patient")
data class Patient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val birthDate: String?,
    val homeLat: Double,
    val homeLng: Double,
    val radiusM: Int
)