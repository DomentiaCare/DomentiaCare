package com.example.domentiacare.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.domentiacare.R
import com.example.domentiacare.data.local.schedule.ScheduleDatabaseProvider
import com.example.domentiacare.data.remote.RetrofitClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// 워치 알림을 위한 import 추가
import com.example.domentiacare.service.watch.WatchMessageHelper

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "토큰 수신: $token")

        // ✅ 백엔드에 토큰 전송
        //sendTokenToServer(token)
    }

    private fun sendTokenToServer(token: String) {
        // 예시 - 로그인 후 토큰 전송
        val request = mapOf("token" to token)
        RetrofitClient.authApi.sendFcmToken(request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                Log.d("FCM", "✅ 토큰 서버 전송 성공")
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("FCM", "❌ 토큰 전송 실패", t)
            }
        })
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // ✅ 1. 데이터 신호가 있을 경우
        if (remoteMessage.data.isNotEmpty()) {
            val command = remoteMessage.data["command"]
            Log.d("FCM", "📡 신호 수신됨: command = $command")

            // 필요시 분기 처리
            when (command) {
                "SYNC_SCHEDULE" -> {
                    Log.d("FCM", "📥 일정 동기화 신호 수신됨")
                    // TODO: ViewModel 등 호출하려면 Broadcast/Repository 연계 필요
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val response = RetrofitClient.authApi.getPendingSchedules()
                            if (response.isNotEmpty()) {

                                // ✅ RoomDB 인스턴스 및 DAO
                                val db = ScheduleDatabaseProvider.getDatabase(applicationContext)
                                val dao = db.scheduleDao()

                                dao.insertSchedules(response)

                                Log.d("FCM", "📦 RoomDB에 일정 저장 완료")

                            } else {
                                Log.d("FCM", "📭 받아올 일정 없음")
                            }
                        } catch (e: Exception) {
                            Log.e("FCM", "❌ 일정 동기화 실패", e)
                        }
                    }
                }
                else -> {
                    Log.d("FCM", "🔍 알 수 없는 명령: $command")
                }
            }

            return // 🔁 신호일 경우 노티피케이션은 띄우지 않음
        }
 
        val title = remoteMessage.notification?.title ?: "알림"
        val body = remoteMessage.notification?.body ?: ""

        // 기존 앱 노티피케이션
        showAppNotification(title, body)

        // 🆕 워치 알림 추가
        sendDangerAlertToWatch(title, body)
    }

    /**
     * 기존 앱 노티피케이션 로직 (분리)
     */
    private fun showAppNotification(title: String, body: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "alert_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "경고 알림",
                NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 포그라운드일 때도 헤드업
            .build()

        notificationManager.notify(1, notification)
    }

    /**
     * 🆕 치매환자 위험 알림을 워치로 전송
     */
    private fun sendDangerAlertToWatch(title: String, body: String) {
        try {
            // 워치 메시지 형식 (위험 알림에 맞게 구성)
            val watchMessage = """
                $title
                $body
            """.trimIndent()

            Log.d("FCM", "워치 위험 알림 전송: $watchMessage")

            // CallRecordAnalyzeService에서 사용한 것과 동일한 방식
            WatchMessageHelper.sendMessageToWatch(
                context = this,
                path = "/danger_alert", // 기존과 동일한 path 사용 (워치에서 같은 방식으로 처리)
                message = watchMessage
            )

        } catch (e: Exception) {
            Log.e("FCM", "워치 위험 알림 전송 실패: ${e.message}", e)
        }
    }
}