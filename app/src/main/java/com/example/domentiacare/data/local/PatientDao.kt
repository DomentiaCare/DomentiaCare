package com.example.domentiacare.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.domentiacare.data.model.Patient

@Dao
interface PatientDao {
    @Query("SELECT * FROM patient")
    suspend fun getAll(): List<Patient>

    @Insert
    suspend fun insert(patient: Patient): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(patients: List<Patient>)
    @Update
    suspend fun update(patient: Patient)
    @Delete
    suspend fun delete(patient: Patient)
}