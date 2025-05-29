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
import com.example.domentiacare.data.local.CurrentUser
import com.example.domentiacare.service.watch.WatchMessageHelper
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "일정 알림"
        val description = intent.getStringExtra("description") ?: "일정이 시작됩니다."
        val startDate = intent.getStringExtra("startDate") ?: ""

        Log.d("ScheduleAlarm", "🔔 알림 울림: $title")

        // 📱 핸드폰 알림
        showPhoneNotification(context, title, description)

        // ⌚ 워치 알림 추가
        sendWatchNotification(context, title, description, startDate)
    }

    /**
     * 📱 핸드폰 알림 표시
     */
    private fun showPhoneNotification(context: Context, title: String, description: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "schedule_channel"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, "일정 알림", NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(title)
                .setContentText(description)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            Log.d("ScheduleAlarm", "📱 핸드폰 알림 전송 완료")
        } catch (e: Exception) {
            Log.e("ScheduleAlarm", "❌ 핸드폰 알림 오류: ${e.message}", e)
        }
    }

    /**
     * ⌚ 워치 알림 전송
     */
    private fun sendWatchNotification(context: Context, title: String, description: String, startDate: String) {
        try {
            // 사용자 이름 가져오기
            val name = CurrentUser.user?.nickname ?: "환자"

            // description에서 활동 내용 추출 (장소 정보 제거)
            val activity = extractActivityFromDescription(description)

            // 간단한 워치 메시지 생성: "김철수님 병원 진료 시간입니다."
            val watchMessage = "${name}님 ${activity} 시간입니다."

            // 워치에 간단한 일정 알림 전송
            WatchMessageHelper.sendMessageToWatch(
                context = context,
                path = "/schedule_simple_notify",
                message = watchMessage
            )

            Log.d("ScheduleAlarm", "⌚ 워치 간단 알림 전송 완료: $watchMessage")
        } catch (e: Exception) {
            Log.e("ScheduleAlarm", "❌ 워치 알림 오류: ${e.message}", e)
        }
    }

    /**
     * description에서 활동 내용만 추출
     * "장소 :" 부분이 있으면 그 앞부분만, 없으면 전체를 활동으로 사용
     */
    private fun extractActivityFromDescription(description: String): String {
        return try {
            Log.d("ScheduleAlarm", "🔧 활동 추출 시작 - description: $description")

            // "장소 :"가 포함되어 있는지 확인
            if (description.contains("장소 :")) {
                // "장소 :" 앞부분만 추출 (활동 내용)
                val parts = description.split("장소 :")
                val activity = parts[0].trim()
                Log.d("ScheduleAlarm", "✅ 활동 추출 성공: $activity")
                return activity
            }

            // "장소 :"가 없으면 description 전체를 활동으로 사용
            Log.d("ScheduleAlarm", "✅ 장소 구분자 없음, description 전체 사용: $description")
            description

        } catch (e: Exception) {
            Log.e("ScheduleAlarm", "❌ 활동 추출 오류: ${e.message}")
            description // 오류 발생시 description 전체 반환
        }
    }
}

fun scheduleAlarm(context: Context, schedule: Schedule) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
        putExtra("title", schedule.title)
        putExtra("description", schedule.description)
        putExtra("startDate", schedule.startDate)
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