package com.example.domentiacare

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import com.example.domentiacare.data.local.TokenManager
import com.example.domentiacare.service.llama.LlamaServiceManager
import com.kakao.sdk.common.KakaoSdk
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.*
import java.security.MessageDigest

@HiltAndroidApp
class MyApplication : Application() {

    companion object {
        private const val TAG = "MyApplication"

        // 🆕 전역 LlamaServiceManager 인스턴스
        lateinit var llamaServiceManager: LlamaServiceManager
            private set

        // 🆕 연결 상태 콜백들
        private val connectionCallbacks = mutableSetOf<(Boolean) -> Unit>()

        fun addConnectionCallback(callback: (Boolean) -> Unit) {
            connectionCallbacks.add(callback)
            // 현재 상태 즉시 전달
            try {
                callback(llamaServiceManager.isConnected() && llamaServiceManager.isServiceReady())
            } catch (e: UninitializedPropertyAccessException) {
                // 아직 초기화되지 않았으면 false 전달
                callback(false)
            }
        }

        fun removeConnectionCallback(callback: (Boolean) -> Unit) {
            connectionCallbacks.remove(callback)
        }

        private fun notifyConnectionChange(isConnected: Boolean) {
            connectionCallbacks.forEach { it(isConnected) }
        }
    }

    // 🆕 LlamaServiceManager를 위한 코루틴 스코프
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 MyApplication starting...")

        // 🔧 기존 초기화 코드
        // Kakao SDK 초기화 (네이티브 앱 키 사용)
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        getKeyHash(this)
        TokenManager.init(this) // ← MyApplication.kt 안에서

        // 🆕 LlamaServiceManager 초기화
        llamaServiceManager = LlamaServiceManager()

        // 🆕 백그라운드에서 ChatApp 연결 시도
        applicationScope.launch {
            connectToLlamaService()
        }

        // 🆕 주기적 연결 상태 모니터링
        applicationScope.launch {
            monitorConnection()
        }
    }

    // 🆕 ChatApp 연결 로직
    private suspend fun connectToLlamaService() {
        try {
            Log.d(TAG, "🔄 Attempting to connect to ChatApp service...")

            // 첫 번째 연결 시도
            var connected = llamaServiceManager.connectToService(this@MyApplication)

            if (!connected) {
                // 재시도 로직 (최대 3번)
                for (attempt in 1..3) {
                    Log.d(TAG, "🔄 Connection retry $attempt/3...")
                    delay(3000) // 3초 대기

                    connected = llamaServiceManager.connectToService(this@MyApplication)
                    if (connected) {
                        Log.d(TAG, "✅ Connected on retry $attempt")
                        break
                    }
                }
            }

            if (connected) {
                Log.i(TAG, "✅ Successfully connected to ChatApp service")

                // 서비스 준비 상태 대기
                waitForServiceReady()
            } else {
                Log.w(TAG, "❌ Failed to connect to ChatApp service after retries")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error connecting to ChatApp service: ${e.message}", e)
        }
    }

    // 🆕 서비스 준비 상태 대기
    private suspend fun waitForServiceReady() {
        try {
            // 최대 30초 동안 서비스 준비 대기
            var attempts = 0
            val maxAttempts = 30

            while (attempts < maxAttempts) {
                if (llamaServiceManager.isServiceReady()) {
                    Log.i(TAG, "🎉 ChatApp service is ready!")
                    notifyConnectionChange(true)
                    return
                }

                if (llamaServiceManager.isServiceInitializing()) {
                    Log.d(TAG, "⏳ Service initializing... ($attempts/$maxAttempts)")
                } else {
                    Log.w(TAG, "⚠️ Service not initializing... ($attempts/$maxAttempts)")
                }

                delay(1000) // 1초 대기
                attempts++
            }

            Log.w(TAG, "⏰ Service ready timeout after ${maxAttempts} seconds")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error waiting for service ready: ${e.message}", e)
        }
    }

    // 🆕 연결 상태 모니터링
    private suspend fun monitorConnection() {
        while (true) {
            try {
                delay(10000) // 10초마다 체크

                val wasConnected = llamaServiceManager.isConnected() && llamaServiceManager.isServiceReady()

                if (!llamaServiceManager.isConnected()) {
                    Log.w(TAG, "🔌 Connection lost, attempting reconnection...")
                    notifyConnectionChange(false)

                    val reconnected = llamaServiceManager.connectToService(this@MyApplication)
                    if (reconnected) {
                        Log.i(TAG, "🔄 Reconnected successfully")
                        waitForServiceReady()
                    }
                } else if (!wasConnected && llamaServiceManager.isServiceReady()) {
                    // 서비스가 새로 준비됨
                    Log.i(TAG, "🎉 Service became ready")
                    notifyConnectionChange(true)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in connection monitoring: ${e.message}", e)
            }
        }
    }

    // 🔧 기존 키 해시 확인하는 함수. LogCat에 KeyHash에 출력됨. 이걸 카카오 디벨로퍼 콘솔에 등록해야 함.
    private fun getKeyHash(context: Context) {
        try {
            val info = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
            val signatures = info.signingInfo?.apkContentsSigners
            if (signatures != null) {
                for (signature in signatures) {
                    val md = MessageDigest.getInstance("SHA")
                    md.update(signature.toByteArray())
                    val keyHash = Base64.encodeToString(md.digest(), Base64.NO_WRAP)
                    Log.d("KeyHash", "Current Key Hash: $keyHash")
                }
            }
        } catch (e: Exception) {
            Log.e("KeyHash", "Unable to get key hash", e)
        }
    }

    // 🆕 앱 종료시 리소스 정리
    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "🛑 Application terminating...")

        // 리소스 정리
        applicationScope.cancel()
        try {
            llamaServiceManager.disconnect(this)
        } catch (e: UninitializedPropertyAccessException) {
            // 초기화되지 않았으면 무시
            Log.d(TAG, "LlamaServiceManager not initialized, skipping disconnect")
        }
    }
}