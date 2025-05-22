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
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.domentiacare.ui.screen.call.CallLogViewModel
import com.example.domentiacare.service.LocationForegroundService
import com.example.domentiacare.ui.AppNavHost
import com.example.domentiacare.ui.theme.DomentiaCareTheme
import dagger.hilt.android.AndroidEntryPoint

//wav파일 변환 테스트 (이종범)
import com.chaquo.python.Python
import java.io.File



@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: CallLogViewModel by viewModels()
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

    // 통화 기록 권한
    private val requestReadCallLog = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.loadCallLogs()
        } else {
            Toast.makeText(this, "통화 기록 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
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
//        setContent  {
//            DomentiaCareTheme {
//                //wav 변환 테스팅 (이종범)
//                // 임시 변환 버튼
//                androidx.compose.material3.Button(
//                    onClick = { convertLatestCallRecordingToWav() }
//                ) {
//                    androidx.compose.material3.Text("통화녹음 WAV 변환")
//                }
//                //wav 변환 테스팅 (이종범) ↑
//
//                if (IS_DEV_MODE) {
//                    AppNavHost()
//                }
//            }
//        }
        setContent {
            DomentiaCareTheme {
                androidx.compose.foundation.layout.Column {
                    androidx.compose.material3.Button(
                        onClick = { convertLatestCallRecordingToWav() },
                        modifier = androidx.compose.ui.Modifier.padding(16.dp)
                    ) {
                        androidx.compose.material3.Text("통화녹음 WAV 변환")
                    }
                    if (IS_DEV_MODE) {
                        AppNavHost()
                    }
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


    // wav파일 변환 테스트 (이종범)
    // 최신 m4a 파일 찾기
    fun findLatestCallRecording(): File? {
        val callDir = File("/sdcard/Recordings/Call/")
        if (callDir.exists() && callDir.isDirectory) {
            val allFiles = callDir.listFiles()
            if (allFiles == null) {
                Log.d("통화녹음", "폴더는 있지만 파일이 없음 또는 접근 불가!")
            } else {
                allFiles.forEach { Log.d("통화녹음", "파일: ${it.name}") }
            }
            val m4aFiles = allFiles?.filter { it.name.endsWith(".m4a") }
            Log.d("통화녹음", "M4A 파일 수: ${m4aFiles?.size}")
            return m4aFiles?.maxByOrNull { it.lastModified() }
        } else {
            Log.d("통화녹음", "폴더가 없거나 디렉터리가 아님!")
        }
        return null
    }


    // 앱 내부로 파일 복사
    fun copyToAppStorage(src: File, destName: String): File {
        val dest = File(filesDir, destName)
        src.inputStream().use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return dest
    }

    // m4a → wav 변환 실행 (버튼 클릭 또는 원하는 위치에서 호출)
    fun convertLatestCallRecordingToWav() {
        val latestM4a = findLatestCallRecording()
        if (latestM4a != null) {
            val copiedM4a = copyToAppStorage(latestM4a, "call_audio.m4a")
            val wavFile = File(filesDir, "call_audio.wav")

            val py = Python.getInstance()
            val module = py.getModule("convert")
            module.callAttr("convert_m4a_to_wav", copiedM4a.absolutePath, wavFile.absolutePath)

            // 검증(optional)
            val isValid = module.callAttr("check_wav_format", wavFile.absolutePath).toBoolean()
            if (isValid) {
                Toast.makeText(this, "변환 성공! $wavFile", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "변환 실패!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "통화녹음 파일을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
