package com.example.domentiacare.data.local.schedule

import android.content.Context
import androidx.room.Room

object ScheduleDatabaseProvider {
    @Volatile
    private var INSTANCE: ScheduleDatabase? = null

    fun getDatabase(context: Context): ScheduleDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                ScheduleDatabase::class.java,
                "schedule-db"
            ).build()
            INSTANCE = instance
            instance
        }
    }
}
