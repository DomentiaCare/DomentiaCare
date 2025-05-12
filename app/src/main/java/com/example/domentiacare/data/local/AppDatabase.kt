package com.example.domentiacare.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.domentiacare.data.model.Patient

@Database(entities = [Patient::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dementia_care"
                ).build().also { INSTANCE = it }
            }
    }
}