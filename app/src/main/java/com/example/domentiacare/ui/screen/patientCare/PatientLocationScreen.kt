package com.example.domentiacare.ui.screen.patientCare

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

@Composable
fun PatientLocationScreen(
    navController: NavController,
    patientName: String,
) {
    val patientLatLng = LatLng(37.58284829999999, 127.0105811)

    // ✅ 카메라 포지션 상태 초기화
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(patientLatLng, 16f)
    }

    // ✅ 마커 상태 초기화
    val markerState = rememberMarkerState(position = patientLatLng)


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
                center = patientLatLng,
                radius = 500.0,
                strokeColor = Color.Blue,
                fillColor = Color(0x220000FF),
                strokeWidth = 2f
            )
        }
    }
}
