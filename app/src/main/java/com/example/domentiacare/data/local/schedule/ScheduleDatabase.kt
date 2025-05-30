package com.example.domentiacare.data.local.schedule

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Schedule::class], version = 1)
abstract class ScheduleDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
}