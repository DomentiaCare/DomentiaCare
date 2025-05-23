package com.example.domentiacare.ui.screen.patientCare

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.example.domentiacare.data.remote.RetrofitClient
import com.example.domentiacare.data.remote.connectWebSocket
import com.example.domentiacare.data.remote.dto.LocationRequestBody
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun PatientLocationScreen(
    navController: NavController,
    patientName: String,
    patientId : Long,
) {
    val patientLatLng = remember { mutableStateOf(LatLng(37.5828483, 127.0105811)) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(patientLatLng.value, 16f)
    }
    val markerState = rememberMarkerState(position = patientLatLng.value)

    LaunchedEffect(Unit) {
        RetrofitClient.authApi.getCurrentLocation(patientId).enqueue(object :
            Callback<LocationRequestBody> {
            override fun onResponse(
                call: Call<LocationRequestBody>,
                response: Response<LocationRequestBody>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val location = response.body()!!
                    Log.d("초기 위치", "lat=${location.latitude}, lon=${location.longitude}")
                    // 지도 초기화
                    // ✅ 상태변수 업데이트 (지도 초기화)
                    patientLatLng.value = LatLng(location.latitude, location.longitude)
                    markerState.position = patientLatLng.value
                    cameraPositionState.move(CameraUpdateFactory.newLatLng(patientLatLng.value))
                    // ✅ WebSocket 연결 시작
                    connectWebSocket(patientId) { lat, lon ->
                        patientLatLng.value = LatLng(lat, lon)
                        markerState.position = patientLatLng.value
                    }
                } else {
                    Log.e("위치 없음", "Redis에서 위치를 찾을 수 없음")
                    // "위치 없음" 메시지 표시
                }
            }
            override fun onFailure(call: Call<LocationRequestBody>, t: Throwable) {
                Log.e("네트워크 오류", "초기 위치 요청 실패", t)
            }
        })

        }
//    val patientLatLng = LatLng(37.58284829999999, 127.0105811)


    // ✅ 카메라 포지션 상태 초기화
//    val cameraPositionState = rememberCameraPositionState {
//        position = CameraPosition.fromLatLngZoom(patientLatLng.value, 16f)
//    }

    // ✅ 마커 상태 초기화


    Box(
        modifier = Modifier.fillMaxSize()
        //.padding(innerPadding)
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            Marker(
                state = markerState,
                title = patientName,
                snippet = "환자의 현재 위치입니다."
            )
            Circle(
                center = patientLatLng.value,
                radius = 500.0,
                strokeColor = Color.Blue,
                fillColor = Color(0x220000FF),
                strokeWidth = 2f
            )
        }
    }
}
