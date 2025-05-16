package com.example.domentiacare

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.domentiacare.service.LocationForegroundService
import com.example.domentiacare.ui.AppNavHost
import com.example.domentiacare.ui.theme.DomentiaCareTheme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val IS_DEV_MODE = true
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private lateinit var finePermissionLauncher: ActivityResultLauncher<String>
    private lateinit var backgroundPermissionLauncher: ActivityResultLauncher<String>
    // 🔹 POST_NOTIFICATIONS 권한 요청 런처
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("Permission", "✅ 알림 권한 허용됨")
        } else {
            Toast.makeText(this, "❌ 알림 권한이 거부되었습니다", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Android 13 이상일 경우 알림 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askNotificationPermission()
        }

        finePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Log.d("Permission", "정밀 위치 권한 허용됨")
                requestBackgroundLocationPermission()
            } else {
                Toast.makeText(this, "정확한 위치 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }

        backgroundPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Log.d("Permission", "백그라운드 위치 권한 허용됨")
                startLocationService()
            } else {
                Toast.makeText(this, "백그라운드 위치 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }

        if (hasFineLocationPermission()) {
            if (hasBackgroundLocationPermission()) {
                startLocationService()
            } else {
                requestBackgroundLocationPermission()
            }
        } else {
            requestFineLocationPermission()
        }

        enableEdgeToEdge()
        setContent {
            DomentiaCareTheme {
                if (IS_DEV_MODE) {
                    AppNavHost()
                }
            }
        }
    }

    private fun hasFineLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasBackgroundLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestFineLocationPermission() {
        finePermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            startLocationService() // Q 이하에서는 백그라운드 권한 필요 없음
        }
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationForegroundService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun askNotificationPermission() {
        // 이미 권한이 허용되었는지 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("Permission", "📢 알림 권한 이미 허용됨")
        }
        // 사용자에게 권한 요청 사유 설명이 필요한 경우
        else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            // 설명 다이얼로그 띄운 후 → OK 누르면 requestPermissionLauncher.launch(...) 호출
            AlertDialog.Builder(this)
                .setTitle("알림 권한 필요")
                .setMessage("위치 이탈 알림을 수신하려면 알림 권한이 필요합니다.")
                .setPositiveButton("허용") { _, _ ->
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                .setNegativeButton("거부", null)
                .show()
        }
        // 설명 없이 바로 요청
        else {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
