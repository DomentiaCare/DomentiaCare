package com.example.domentiacare.service.llama

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LlamaServiceManager {

    companion object {
        private const val TAG = "LlamaServiceManager"
        private const val CHATAPP_PACKAGE = "com.quicinc.chatapp"
        private const val SERVICE_CLASS = "com.quicinc.chatapp.LlamaAnalysisService"
        private const val CONNECTION_TIMEOUT = 15000L // 15초
    }

    private var serviceConnection: ServiceConnection? = null
    private var llamaService: Any? = null
    private var isConnected = false
    private var isConnecting = false

    /**
     * ChatApp의 LlamaAnalysisService에 연결
     */
    suspend fun connectToService(context: Context): Boolean {
        if (isConnected) return true
        if (isConnecting) return false

        isConnecting = true
        Log.d(TAG, "Attempting to connect to ChatApp LlamaAnalysisService...")

        return try {
            suspendCoroutine { continuation ->
                val intent = Intent().apply {
                    setClassName(CHATAPP_PACKAGE, SERVICE_CLASS)
                }

                serviceConnection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        try {
                            Log.d(TAG, "Service connected, getting service instance...")

                            // 리플렉션을 통해 LocalBinder에서 서비스 인스턴스 획득
                            val getServiceMethod = service?.javaClass?.getMethod("getService")
                            llamaService = getServiceMethod?.invoke(service)

                            isConnected = true
                            isConnecting = false

                            Log.i(TAG, "Successfully connected to LlamaAnalysisService")
                            continuation.resume(true)

                        } catch (e: Exception) {
                            Log.e(TAG, "Error connecting to service: ${e.message}", e)
                            isConnecting = false
                            continuation.resume(false)
                        }
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        llamaService = null
                        isConnected = false
                        isConnecting = false
                        Log.d(TAG, "Disconnected from LlamaAnalysisService")
                    }
                }

                val bound = context.bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)
                if (!bound) {
                    Log.e(TAG, "Failed to bind to LlamaAnalysisService - service not found")
                    isConnecting = false
                    continuation.resume(false)
                    return@suspendCoroutine
                }

                // 타임아웃 처리
                CoroutineScope(Dispatchers.Main).launch {
                    delay(CONNECTION_TIMEOUT)
                    if (isConnecting) {
                        Log.e(TAG, "Connection timeout after ${CONNECTION_TIMEOUT}ms")
                        isConnecting = false
                        continuation.resume(false)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during service connection: ${e.message}", e)
            isConnecting = false
            false
        }
    }

    /**
     * 간단한 텍스트 쿼리 전송 (일정 분석이 아닌 일반 질문용)
     */
    suspend fun sendQuery(query: String): String {
        if (!isConnected || llamaService == null) {
            return "Error: Service not connected"
        }

        Log.d(TAG, "Sending query: $query")

        return suspendCoroutine { continuation ->
            try {
                // 리플렉션을 통해 analyzeSchedule 메서드 호출 (일반 쿼리도 이 메서드 사용)
                val analyzeMethod = llamaService!!.javaClass.getMethod(
                    "analyzeSchedule",
                    String::class.java,
                    Class.forName("com.quicinc.chatapp.LlamaAnalysisService\$ScheduleAnalysisCallback")
                )

                // 콜백 프록시 생성
                val callback = createCallbackProxy { result ->
                    val response = when (result) {
                        is ScheduleAnalysisResult.Success -> result.jsonResult
                        is ScheduleAnalysisResult.NoSchedule -> "No specific information found"
                        is ScheduleAnalysisResult.Error -> "Error: ${result.message}"
                    }
                    continuation.resume(response)
                }

                analyzeMethod.invoke(llamaService, query, callback)

            } catch (e: Exception) {
                Log.e(TAG, "Error sending query: ${e.message}", e)
                continuation.resume("Error: Failed to send query - ${e.message}")
            }
        }
    }

    /**
     * 서비스 연결 해제
     */
    fun disconnect(context: Context) {
        try {
            serviceConnection?.let {
                context.unbindService(it)
                serviceConnection = null
            }
            llamaService = null
            isConnected = false
            isConnecting = false
            Log.d(TAG, "Disconnected from LlamaAnalysisService")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from service: ${e.message}")
        }
    }

    /**
     * 서비스 준비 상태 확인
     */
    fun isServiceReady(): Boolean {
        if (!isConnected || llamaService == null) return false

        return try {
            val isReadyMethod = llamaService!!.javaClass.getMethod("isServiceReady")
            isReadyMethod.invoke(llamaService) as Boolean
        } catch (e: Exception) {
            Log.e(TAG, "Error checking service ready state: ${e.message}")
            false
        }
    }

    /**
     * 연결 상태 확인
     */
    fun isConnected(): Boolean = isConnected

    /**
     * 연결 진행 중 여부 확인
     */
    fun isConnecting(): Boolean = isConnecting

    /**
     * 콜백 프록시 생성 (리플렉션용)
     */
    private fun createCallbackProxy(onResult: (ScheduleAnalysisResult) -> Unit): Any {
        return java.lang.reflect.Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(Class.forName("com.quicinc.chatapp.LlamaAnalysisService\$ScheduleAnalysisCallback"))
        ) { _, method, args ->
            when (method.name) {
                "onScheduleFound" -> {
                    val jsonResult = args[0] as String
                    onResult(ScheduleAnalysisResult.Success(jsonResult))
                }
                "onNoScheduleFound" -> {
                    onResult(ScheduleAnalysisResult.NoSchedule)
                }
                "onError" -> {
                    val errorMessage = args[0] as String
                    onResult(ScheduleAnalysisResult.Error(errorMessage))
                }
            }
            null
        }
    }
}