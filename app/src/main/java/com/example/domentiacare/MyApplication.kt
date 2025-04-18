package com.example.domentiacare

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import com.example.domentiacare.data.local.TokenManager
import com.kakao.sdk.common.KakaoSdk
import dagger.hilt.android.HiltAndroidApp
import java.security.MessageDigest

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Kakao SDK 초기화 (네이티브 앱 키 사용)
        KakaoSdk.init(this,  BuildConfig.KAKAO_NATIVE_APP_KEY)
        getKeyHash(this)
        TokenManager.init(this) // ← MyApplication.kt 안에서
    }

    // 키 해시 확인하는 함수. LogCat에 KeyHash에 출력됨. 이걸 카카오 디벨로퍼 콘솔에 등록해야 함.
    fun getKeyHash(context: Context) {
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
}