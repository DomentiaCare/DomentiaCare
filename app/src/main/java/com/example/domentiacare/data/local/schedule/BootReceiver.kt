package com.example.domentiacare.data.local.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val repository = ScheduleRepository(context)
                val schedules = repository.getAllSchedules() // suspend 함수로 모든 schedule 가져오기

                val now = System.currentTimeMillis()
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

                schedules.forEach { schedule ->
                    try {
                        val triggerTime = LocalDateTime.parse(schedule.startDate, formatter)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                        if (triggerTime > now) {
                            scheduleAlarm(context, schedule)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
