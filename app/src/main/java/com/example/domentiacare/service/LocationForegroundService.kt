package com.example.domentiacare.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.domentiacare.R
import com.example.domentiacare.data.local.TokenManager
import com.example.domentiacare.data.remote.RetrofitClient
import com.example.domentiacare.data.remote.dto.LocationRequestBody
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@AndroidEntryPoint
class LocationForegroundService : Service() {

    private var lastSavedTime = 0L

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)


    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startLocationTracking()
        startWebSocketLocationSending() // ✅ 웹소켓 위치 전송 시작
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("위치 추적 중")
            .setContentText("서버로 주기적으로 위치를 전송 중")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(1, notification)
        return START_STICKY
    }

    private fun startLocationTracking() {
        val request = LocationRequest.create().apply {
            interval = 15_000  // 15초 주기
            fastestInterval = 10_000
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    Log.d("LocationService", "위도: ${it.latitude}, 경도: ${it.longitude}")
                    sendLocationToServer(it.latitude, it.longitude)

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastSavedTime > 5 * 60 * 1000) {  // 5분 경과
                        saveLocationToDatabase(it.latitude, it.longitude)
                        lastSavedTime = currentTime
                    }
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        }
    }

    private fun sendLocationToServer(lat: Double, lng: Double) {
        val token = TokenManager.getToken()
        if (token.isNullOrBlank()) {
            Log.d("LocationService", "❌ JWT 없음 → 위치 전송 생략")
            return
        }
        val request = LocationRequestBody(lat, lng)
        RetrofitClient.authApi.sendLocation(request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                Log.d("LocationService", "위치 전송 성공")
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("LocationService", "위치 전송 실패", t)
            }
        })
    }

    private fun saveLocationToDatabase(lat: Double, lng: Double) {
        val token = TokenManager.getToken()
        if (token.isNullOrBlank()) {
            Log.d("LocationDBService", "❌ JWT 없음 → 위치 전송 생략")
            return
        }
        val request = LocationRequestBody(lat, lng)
        RetrofitClient.authApi.saveDBLocation(request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                Log.d("LocationDBService", "위치 전송 성공")
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("LocationDBService", "위치 전송 실패", t)
            }
        })
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location_channel", "위치 서비스",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startWebSocketLocationSending() {
        serviceScope.launch {
            while (true) {
                if (ActivityCompat.checkSelfPermission(
                        this@LocationForegroundService,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            sendLocationViaWebSocket(it.latitude, it.longitude)
                            Log.d("WebSocket", "위치 전송: lat=${it.latitude}, lng=${it.longitude}")
                        }
                    }
                }
                kotlinx.coroutines.delay(5000L) // 5초마다 실행
            }
        }
    }

    private fun sendLocationViaWebSocket(lat: Double, lng: Double) {
        val token = TokenManager.getToken()
        if (token.isNullOrBlank()) {
            Log.d("WebSocket", "❌ JWT 없음 → WebSocket 위치 전송 생략")
            return
        }

        val request = LocationRequestBody(lat, lng)
        RetrofitClient.authApi.websocketLocation(request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                Log.d("WebSocket", "위치 전송 성공")
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("WebSocket", "위치 전송 실패", t)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // ✅ Coroutine 종료
    }
}
