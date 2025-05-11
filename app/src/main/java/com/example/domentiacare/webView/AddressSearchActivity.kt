package com.example.domentiacare.webView

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView

class AddressSearchActivity : Activity() {  //다음 포스트 코드 주소 검색 API를 사용하기 위한 액티비티 웹뷰
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowFileAccessFromFileURLs = true
        webView.settings.allowUniversalAccessFromFileURLs = true

        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(AddressBridge(this), "Android")
        webView.loadUrl("file:///android_asset/postcode.html")
        setContentView(webView)
    }

    class AddressBridge(private val context: Context) {
        @JavascriptInterface
        fun onAddressSelected(zip: String, roadAddr: String) {
            Log.d("address", "우편번호: $zip, 도로명주소: $roadAddr")  // 로그 추가
            val intent = Intent().apply {
                putExtra("zonecode", zip)
                putExtra("roadAddress", roadAddr)
            }
            (context as Activity).setResult(Activity.RESULT_OK, intent)
            context.finish()
        }
    }
}
