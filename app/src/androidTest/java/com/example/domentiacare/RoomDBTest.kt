package com.example.domentiacare

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.domentiacare.data.local.schedule.Schedule
import com.example.domentiacare.data.local.schedule.ScheduleDao
import com.example.domentiacare.data.local.schedule.ScheduleDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class ScheduleDaoTest {

    private lateinit var database: ScheduleDatabase
    private lateinit var dao: ScheduleDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ScheduleDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.scheduleDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insert_and_select_schedule() = runBlocking {
        // Given
        val schedule = Schedule(
            title = "약 복용",
            description = "오전 9시 알약 복용",
            startDate = "2025-05-28T09:00:00",
            endDate = "2025-05-28T09:10:00"
        )

        // When
        dao.insertSchedule(schedule)
        val result = dao.getAllSchedules()

        // Then
        assertEquals(1, result.size)
        assertEquals("약 복용", result[0].title)
        assertFalse(result[0].isSynced)
    }
}
