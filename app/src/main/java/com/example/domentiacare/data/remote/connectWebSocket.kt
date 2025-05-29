package com.example.domentiacare.data.remote

import android.util.Log
import com.example.domentiacare.BuildConfig
import com.example.domentiacare.data.remote.dto.LocationRequestBody
import com.google.gson.Gson
import io.reactivex.disposables.Disposable
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent

lateinit var stompClient: StompClient
var locationSubscription: Disposable? = null


object WebSocketManager {
    fun connectWebSocket(patientId: Long, onLocationUpdate: (Double, Double) -> Unit) {
        if (!::stompClient.isInitialized) {
            stompClient = Stomp.over(
                Stomp.ConnectionProvider.OKHTTP,
                "ws://${BuildConfig.IP_ADDRESS}:8080/ws/websocket"
            )
        }

        stompClient.lifecycle().subscribe {
            when (it.type) {
                LifecycleEvent.Type.OPENED -> {
                    Log.d("STOMP", "연결됨")

                    // 이전 구독 해제
                    locationSubscription?.dispose()

                    locationSubscription = stompClient.topic("/topic/patient/$patientId")
                        .subscribe { message ->
                            val location =
                                Gson().fromJson(message.payload, LocationRequestBody::class.java)
                            Log.d(
                                "WebSocket 수신",
                                "lat=${location.latitude}, lon=${location.longitude}"
                            )
                            val lat = location.latitude
                            val lon = location.longitude
                            Log.d("WebSocket 수신", "lat=$lat, lon=$lon")
                            // 지도 업데이트
                            onLocationUpdate(lat, lon)
                        }
                }

                LifecycleEvent.Type.ERROR -> Log.e("STOMP", "에러", it.exception)
                LifecycleEvent.Type.CLOSED -> Log.d("STOMP", "연결 종료")
                else -> Unit
            }
        }

        stompClient.connect()
    }

    fun disconnect() {
        Log.d("STOMP", "disconnect() 호출됨")
        locationSubscription?.dispose()
        if (::stompClient.isInitialized) {
            stompClient.disconnect()
        }
        // 🔥 이게 핵심!
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, "ws://${BuildConfig.IP_ADDRESS}:8080/ws/websocket")
    }
}
