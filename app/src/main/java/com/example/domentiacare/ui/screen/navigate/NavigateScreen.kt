package com.example.domentiacare.ui.screen.navigate

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.launch

// 경로 안내 단계 데이터 클래스
data class RouteStep(
    val instruction: String,
    val distance: String,
    val isHighlight: Boolean = false
)

@Composable
fun NavigateScreen() {
    RouteFinderScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteFinderScreen() {
    val context = LocalContext.current

    var startLocation = remember { mutableStateOf("") }
    var destination = remember { mutableStateOf("") }
    var currentLatLng by remember { mutableStateOf<LatLng?>(null) }
    var googleMapInstance by remember { mutableStateOf<GoogleMap?>(null) }
    var isRouteCalculated by remember { mutableStateOf(false) }

    val routeSteps = remember {
        mutableStateListOf(
            RouteStep("출발: 현재 위치", "0m", true),
            RouteStep("직진 후 횡단보도를 건너세요", "100m"),
            RouteStep("오른쪽으로 꺾어서 메인 도로로 진입하세요", "250m"),
            RouteStep("버스 정류장을 지나 직진하세요", "400m"),
            RouteStep("약국 앞에서 왼쪽으로 꺾으세요", "550m"),
            RouteStep("도착: 목적지", "700m", true)
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            getCurrentLocation(context) { location ->
                currentLatLng = LatLng(location.latitude, location.longitude)
                startLocation.value = "내 위치 (${location.latitude}, ${location.longitude})"

                googleMapInstance?.let { map ->
                    val latLng = currentLatLng!!
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
                    map.addMarker(MarkerOptions().position(latLng).title("현재 위치"))
                }
            }
        }
    }

    fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    fun calculateAndShowRoute() {
        if (startLocation.value.isNotEmpty() && destination.value.isNotEmpty() && googleMapInstance != null) {
            val startPoint = currentLatLng ?: LatLng(37.5666102, 126.9783881)
            val endPoint = LatLng(37.5707, 126.9756)

            googleMapInstance?.apply {
                addMarker(MarkerOptions().position(startPoint).title("출발지"))
                addMarker(MarkerOptions().position(endPoint).title("도착지"))

                addPolyline(
                    PolylineOptions()
                        .add(
                            startPoint,
                            LatLng(37.5680, 126.9770),
                            LatLng(37.5690, 126.9760),
                            endPoint
                        )
                        .width(12f)
                        .color(Color(0xFF4285F4).toArgb())
                )

                moveCamera(CameraUpdateFactory.newLatLngBounds(
                    LatLngBounds.builder().include(startPoint).include(endPoint).build(), 100
                ))
            }
            isRouteCalculated = true
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF4285F4)).padding(16.dp)) {
            Text("경로 찾기", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
        }

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = startLocation.value,
                    onValueChange = { startLocation.value = it },
                    label = { Text("출발지", fontSize = 18.sp) },
                    modifier = Modifier.weight(1f).height(65.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color(0xFF4285F4),
                        unfocusedIndicatorColor = Color(0xFF9E9E9E)
                    )
                )
                IconButton(onClick = { requestLocationPermission() }, modifier = Modifier.padding(start = 8.dp).size(48.dp).background(Color(0xFF4285F4), RoundedCornerShape(8.dp))) {
                    Icon(imageVector = Icons.Default.MyLocation, contentDescription = "내 위치 가져오기", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = destination.value,
                onValueChange = { destination.value = it },
                label = { Text("도착지", fontSize = 18.sp) },
                modifier = Modifier.fillMaxWidth().height(65.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = "도착지", tint = Color(0xFFE53935))
                },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color(0xFF4285F4),
                    unfocusedIndicatorColor = Color(0xFF9E9E9E)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { calculateAndShowRoute() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("경로 찾기", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).border(2.dp, Color(0xFF9E9E9E), RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp))
            ) {
                AndroidView(
                    factory = { context ->
                        MapView(context).apply {
                            onCreate(null)
                            onResume()
                            getMapAsync(OnMapReadyCallback { map ->
                                googleMapInstance = map
                                map.uiSettings.isZoomControlsEnabled = true
                                map.uiSettings.isMyLocationButtonEnabled = true
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(37.5666102, 126.9783881), 15f))
                                currentLatLng?.let {
                                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 17f))
                                    map.addMarker(MarkerOptions().position(it).title("현재 위치"))
                                }
                            })
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (isRouteCalculated) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("경로 안내", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4), modifier = Modifier.padding(bottom = 8.dp))
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                        items(routeSteps) { step ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).background(if (step.isHighlight) Color(0xFFE3F2FD) else Color.Transparent, RoundedCornerShape(4.dp)).padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = if (step.isHighlight) Color(0xFFE53935) else Color(0xFF4285F4), modifier = Modifier.size(24.dp))
                                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                    Text(step.instruction, fontSize = 18.sp, fontWeight = if (step.isHighlight) FontWeight.Bold else FontWeight.Normal)
                                }
                                Text(step.distance, fontSize = 16.sp, color = Color(0xFF757575))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getCurrentLocation(context: Context, onLocationReceived: (Location) -> Unit) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (lastKnownLocation != null) onLocationReceived(lastKnownLocation)

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                onLocationReceived(location)
                locationManager.removeUpdates(this)
            }
            override fun onProviderDisabled(provider: String) {}
            override fun onProviderEnabled(provider: String) {}
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10f, locationListener)
    }
}
