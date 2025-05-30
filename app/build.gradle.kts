import java.util.Properties



val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
val kakaoKey = localProperties.getProperty("KAKAO_NATIVE_APP_KEY") ?: ""
val googleMapKey = localProperties.getProperty("GOOGLE_MAP_KEY") ?: ""
val naverMapClientId = localProperties.getProperty("NAVER_MAP_CLIENT_ID") ?: ""

val localIpAddress = localProperties.getProperty("SERVER_IP") ?: "127.0.0.1"
//val localIpAddress = InetAddress.getLocalHost().hostAddress // 로컬 IP 주소 자동 설정

val baseUrl = "http://$localIpAddress:8080"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    // Hilt DI
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt") // Hilt만을 위해 유지
    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.domentiacare"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.domentiacare"
        minSdk = 31
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
        buildConfigField("String", "IP_ADDRESS", "\"${localIpAddress}\"")

        manifestPlaceholders["GOOGLE_MAP_KEY"] = googleMapKey
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
        aidl = true
    }

    // KAPT 설정 (Hilt만을 위해)
    kapt {
        correctErrorTypes = true
        useBuildCache = true

        // Room 특화 설정 추가
        arguments {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
            arg("room.expandProjection", "true")
        }
    }

}

// android 블록 아래에 추가
configurations.all {
    resolutionStrategy {
        // Kotlin stdlib 버전 강제 통일
        force("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.0.0")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.0")

        // 문제가 되는 캘린더 라이브러리 버전 다운그레이드
        force("com.kizitonwose.calendar:compose:2.5.0")
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    implementation("com.google.accompanist:accompanist-navigation-animation:0.32.0")

    // WorkManager for background sync
    implementation(libs.androidx.work.runtime.ktx)
    implementation("androidx.work:work-runtime-ktx:2.8.1")

    // Firebase
    implementation(libs.firebase.messaging.ktx)
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-analytics")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.1")
    implementation("com.google.code.gson:gson:2.10.1")

    // Coroutines
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Kakao SDK
    implementation("com.kakao.sdk:v2-user:2.21.1")

    // Google Services
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.libraries.places:places:3.3.0")
    implementation("com.google.maps.android:maps-compose:2.11.4")

    // UI Components
//    implementation("com.kizitonwose.calendar:compose:2.0.3")
    implementation("com.google.accompanist:accompanist-pager:0.30.1")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")

    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.13.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.2")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.13.0")

    // Room 완전 제거 - SharedPreferences 사용
    // implementation("androidx.room:room-runtime:2.5.0") // 제거
    // implementation("androidx.room:room-ktx:2.5.0") // 제거
    // kapt("androidx.room:room-compiler:2.5.0") // 제거
    // implementation(libs.androidx.room.common.jvm) // 제거

    // Test dependencies
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
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.libraries.places:places:3.3.0")

    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    implementation("com.google.accompanist:accompanist-navigation-animation:0.32.0")
//    implementation("com.kizitonwose.calendar:compose:2.0.3")
    implementation("com.google.accompanist:accompanist-pager:0.30.1")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.maps.android:maps-compose:2.11.4")

    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")

    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-analytics")

    implementation("org.tensorflow:tensorflow-lite:2.13.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.2")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.13.0")

    // ✅ WebSocket
    implementation ("org.java-websocket:Java-WebSocket:1.5.2")
    implementation ("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")
    implementation ("io.reactivex.rxjava2:rxjava:2.2.21")


    // Watch service
    implementation ("com.google.android.gms:play-services-wearable:18.1.0")

    // RoomDB
    // 현재 Room 2.6.1을 2.7.1로 변경
    implementation("androidx.room:room-runtime:2.7.1")  // 2.6.1 → 2.7.1
    kapt("androidx.room:room-compiler:2.7.1")           // 2.6.1 → 2.7.1
    implementation("androidx.room:room-ktx:2.7.1")      // 2.6.1 → 2.7.1

// 테스트 의존성도 업그레이드
    testImplementation("androidx.room:room-testing:2.7.1") // 2.6.1 → 2.7.1

    //NetworkCallback
    implementation ("androidx.core:core-ktx:1.12.0")

    // JUnit + 코루틴
    testImplementation ("junit:junit:4.13.2")
    testImplementation ("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    //달력
    // 캘린더 라이브러리 버전 변경
    implementation("com.kizitonwose.calendar:compose:2.5.0") // 2.7.0 → 2.5.0



}
