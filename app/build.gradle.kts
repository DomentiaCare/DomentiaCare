import java.net.InetAddress
import java.util.Properties

val localIpAddress = InetAddress.getLocalHost().hostAddress
val baseUrl = "http://$localIpAddress:8080"

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
val kakaoKey = localProperties.getProperty("KAKAO_NATIVE_APP_KEY") ?: ""
val naverMapClientId = localProperties.getProperty("NAVER_MAP_CLIENT_ID") ?: ""

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.domentiacare"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.domentiacare"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["kakao_scheme"] = "kakao$kakaoKey"
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoKey\"")

        // 네이버 지도 API 키 설정
        manifestPlaceholders["naverMapClientId"] = naverMapClientId
        buildConfigField("String", "NAVER_MAP_CLIENT_ID", "\"$naverMapClientId\"")
        buildConfigField("String", "BASE_URL", "\"${baseUrl}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("com.kakao.sdk:v2-user:2.21.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.1")

    //navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    //viewmodel
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    //화면 전환 애니메이션
    implementation("com.google.accompanist:accompanist-navigation-animation:0.32.0")

    //캘린더 라이브러리
    implementation("com.kizitonwose.calendar:compose:2.0.3")


    // 네이버 지도 SDK
    //implementation ("com.naver.maps:map-sdk:3.16.2")

    // 위치 서비스를 위한 Play Services 의존성
    implementation ("com.google.android.gms:play-services-location:21.0.1")

    // Retrofit 및 네트워크 통신 라이브러리 (API 호출용)
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.11.0")

}
