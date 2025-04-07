package com.example.domentiacare

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Kakao SDK 초기화 (네이티브 앱 키 사용)
        KakaoSdk.init(this,  BuildConfig.KAKAO_NATIVE_APP_KEY)
    }
}