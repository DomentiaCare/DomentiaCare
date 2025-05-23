package com.example.domentiacare.service.llama

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.quicinc.chatapp.IAnalysisCallback
import com.quicinc.chatapp.ILlamaAnalysisService
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import java.util.concurrent.atomic.AtomicBoolean

class LlamaServiceManager {

    companion object {
        private const val TAG = "LlamaServiceManager"
        private const val CHATAPP_PACKAGE = "com.quicinc.chatapp"
        private const val SERVICE_CLASS = "com.quicinc.chatapp.LlamaAnalysisService"
        private const val CONNECTION_TIMEOUT = 15000L
    }

    private var serviceConnection: ServiceConnection? = null
    private var llamaService: ILlamaAnalysisService? = null
    private var isConnected = false
    private var isConnecting = false

    /**
     * ChatApp의 LlamaAnalysisService에 연결 (AIDL 방식)
     */
    suspend fun connectToService(context: Context): Boolean {
        if (isConnected) return true
        if (isConnecting) return false

        isConnecting = true
        Log.d(TAG, "Attempting to connect to ChatApp LlamaAnalysisService via AIDL...")

        return try {
            suspendCoroutine { continuation ->
                val intent = Intent().apply {
                    setClassName(CHATAPP_PACKAGE, SERVICE_CLASS)
                }

                serviceConnection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        try {
                            Log.d(TAG, "Service connected via AIDL")

                            // AIDL 스텁으로 변환
                            llamaService = ILlamaAnalysisService.Stub.asInterface(service)

                            if (llamaService != null) {
                                isConnected = true
                                isConnecting = false
                                Log.i(TAG, "Successfully connected to LlamaAnalysisService via AIDL")
                                continuation.resume(true)
                            } else {
                                Log.e(TAG, "Failed to convert IBinder to ILlamaAnalysisService")
                                isConnecting = false
                                continuation.resume(false)
                            }

                        } catch (e: Exception) {
                            Log.e(TAG, "Error in onServiceConnected: ${e.message}", e)
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
                    Log.e(TAG, "Failed to bind to LlamaAnalysisService")
                    isConnecting = false
                    continuation.resume(false)
                    return@suspendCoroutine
                }

                Log.d(TAG, "Service binding initiated successfully")

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
     * 텍스트 쿼리 전송 (AIDL 방식) - 실시간 스트리밍 지원
     */
    /**
     * 텍스트 쿼리 전송 (AIDL 방식) - 실시간 스트리밍 지원
     */
    suspend fun sendQuery(
        query: String,
        onPartialUpdate: ((String) -> Unit)? = null
    ): String {
        if (!isConnected || llamaService == null) {
            return "Error: Service not connected"
        }

        Log.d(TAG, "Sending query via AIDL: $query")

        return suspendCoroutine { continuation ->
            // 🔧 중복 응답 방지를 위한 AtomicBoolean
            val responseCalled = AtomicBoolean(false)
            var lastResponse = ""

            try {
                val callback = object : IAnalysisCallback.Stub() {
                    override fun onPartialResult(partialText: String?) {
                        // 🆕 실시간 부분 결과 처리
                        partialText?.let { partial ->
                            Log.d(TAG, "Received partial result via AIDL, length: ${partial.length}")
                            lastResponse = partial
                            try {
                                onPartialUpdate?.invoke(partial)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in partial update callback: ${e.message}")
                            }
                        }
                    }

                    override fun onResult(result: String?) {
                        result?.let {
                            Log.d(TAG, "Received result via AIDL, length: ${it.length}, treating as partial")
                            lastResponse = it

                            // 🆕 onResult도 부분 결과로 처리 (responseCalled 체크 없이)
                            try {
                                onPartialUpdate?.invoke(it)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in result->partial update callback: ${e.message}")
                            }
                        }

                        // 최종 완료는 타임아웃에서만 처리하지 않고 여기서도 처리
                        // 단, 첫 번째가 아닌 경우에만
                        if (result != null && result.length > 100) {
                            if (responseCalled.compareAndSet(false, true)) {
                                Log.d(TAG, "Final result accepted: $result")
                                continuation.resume(result)
                            } else {
                                Log.d(TAG, "Duplicate final result ignored: $result")
                            }
                        }
                    }

                    override fun onError(error: String?) {
                        if (responseCalled.compareAndSet(false, true)) {
                            Log.e(TAG, "Received error via AIDL: $error")
                            continuation.resume("Error: ${error ?: "Unknown error"}")
                        } else {
                            Log.d(TAG, "Duplicate error ignored: $error")
                        }
                    }

                    override fun onNoResult() {
                        if (responseCalled.compareAndSet(false, true)) {
                            Log.d(TAG, "No result from LLaMA")
                            continuation.resume("No response generated")
                        } else {
                            Log.d(TAG, "Duplicate no result ignored")
                        }
                    }
                }

                llamaService!!.analyzeText(query, callback)
                Log.d(TAG, "Query sent to service successfully")

                // 🔧 타임아웃 처리 (20초)
                CoroutineScope(Dispatchers.Main).launch {
                    delay(20000)
                    if (responseCalled.compareAndSet(false, true)) {
                        Log.w(TAG, "Query timeout after 20 seconds, using last response")
                        continuation.resume(if (lastResponse.isNotEmpty()) lastResponse else "Error: Response timeout")
                    }
                }

            } catch (e: RemoteException) {
                if (responseCalled.compareAndSet(false, true)) {
                    Log.e(TAG, "RemoteException while sending query: ${e.message}", e)
                    continuation.resume("Error: Remote service error - ${e.message}")
                }
            } catch (e: Exception) {
                if (responseCalled.compareAndSet(false, true)) {
                    Log.e(TAG, "Error sending query: ${e.message}", e)
                    continuation.resume("Error: Failed to send query - ${e.message}")
                }
            }
        }
    }

    /**
     * 간단한 쿼리 전송 (기존 호환성 유지)
     */
    suspend fun sendQuery(query: String): String {
        return sendQuery(query, null)
    }

    /**
     * 서비스 준비 상태 확인 (AIDL 방식)
     */
    fun isServiceReady(): Boolean {
        if (!isConnected || llamaService == null) return false

        return try {
            llamaService!!.isServiceReady()
        } catch (e: RemoteException) {
            Log.e(TAG, "Error checking service ready state: ${e.message}")
            false
        }
    }

    /**
     * 서비스 초기화 진행 중 여부 확인 (AIDL 방식)
     */
    fun isServiceInitializing(): Boolean {
        if (!isConnected || llamaService == null) return false

        return try {
            llamaService!!.isServiceInitializing()
        } catch (e: RemoteException) {
            Log.e(TAG, "Error checking service initializing state: ${e.message}")
            false
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
     * 연결 상태 확인
     */
    fun isConnected(): Boolean = isConnected

    /**
     * 연결 진행 중 여부 확인
     */
    fun isConnecting(): Boolean = isConnecting
}