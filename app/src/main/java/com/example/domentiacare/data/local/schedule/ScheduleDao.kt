package com.example.domentiacare.data.local.schedule

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM schedule WHERE isSynced = 0")
    suspend fun getUnsyncedSchedules(): List<Schedule>

    @Update
    suspend fun updateSchedules(schedules: List<Schedule>)

    @Insert
    suspend fun insertSchedule(schedule: Schedule)

    @Insert
    suspend fun insertSchedules(schedules: List<Schedule>)

    @Query("SELECT * FROM schedule")
    suspend fun getAllSchedules(): List<Schedule>

    //db에서 넣으면 자동으로 가져오게함
//    @Query("SELECT * FROM schedule")
//    fun getAllSchedulesFlow(): Flow<List<Schedule>>

    @Query("DELETE FROM schedule")
    suspend fun deleteAllSchedules()

}
