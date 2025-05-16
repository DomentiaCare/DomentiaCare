package com.example.domentiacare.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.domentiacare.R
import com.example.domentiacare.data.remote.RetrofitClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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

        val title = remoteMessage.notification?.title
        val body = remoteMessage.notification?.body

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
}
