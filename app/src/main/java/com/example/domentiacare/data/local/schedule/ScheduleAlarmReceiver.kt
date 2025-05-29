package com.example.domentiacare.data.local.schedule

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.domentiacare.R
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "일정 알림"
        val description = intent.getStringExtra("description") ?: "일정이 시작됩니다."

        Log.d("ScheduleAlarm", "🔔 알림 울림: $title")


        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "schedule_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "일정 알림", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_background) // 아이콘 리소스 필요
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        //이거 호출하는순간 핸드폰에 호출
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)

        //워치 알람도 구현해줬으면 좋겠어.
    }
}

fun scheduleAlarm(context: Context, schedule: Schedule) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
        putExtra("title", schedule.title)
        putExtra("description", schedule.description)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        schedule.id.toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    val triggerTime = LocalDateTime.parse(schedule.startDate, formatter)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    Log.d("ScheduleAlarm", "⏰ 알람 등록됨: ${schedule.title} @ $triggerTime (ms)")

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!alarmManager.canScheduleExactAlarms()) {
            Log.e("ScheduleAlarm", "❌ 정확한 알람 권한 없음 - 알람 등록 생략")
            return
        }
    }

    alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        triggerTime,
        pendingIntent
    )
}

fun cancelAlarm(context: Context, schedule: Schedule) {
    val intent = Intent(context, ScheduleAlarmReceiver::class.java)

    val requestCode = (schedule.title + schedule.startDate).hashCode()
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.cancel(pendingIntent)

    Log.d("ScheduleAlarm", "❌ 알람 취소됨: ${schedule.title}")
}


