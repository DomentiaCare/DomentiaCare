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
val googleMapKey = localProperties.getProperty("GOOGLE_MAP_KEY") ?: ""
val naverMapClientId = localProperties.getProperty("NAVER_MAP_CLIENT_ID") ?: ""

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    // ✅ 여기에 추가!!
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
    id("com.google.gms.google-services")
    id("com.chaquo.python")
}

// --------------------- QNN SDK 설정 추가 ---------------------
val qnnSDKLocalPath = "C:\\\\Qualcomm\\\\AIStack\\\\QAIRT\\\\2.32.6.250402"
val models = listOf("llama3_2_3b")
val relAssetsPath = "src/main/assets/models/"
val qnnBuildDir = project(":ChatApp").layout.buildDirectory
val libsDir = qnnBuildDir.dir("libs")
// ------------------------------------------------------------

android {
    namespace = "com.example.domentiacare"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.domentiacare"
        minSdk = 31
        // python ndk 명시 (이종범)
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
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



    packagingOptions {
        jniLibs.useLegacyPackaging = true
    }

    aaptOptions {
        noCompress += listOf("bin", "json")
    }

    // Llama 부분 주석처리
//    tasks.named("preBuild").configure {
//        doFirst {
//            if (!file(qnnSDKLocalPath).exists()) {
//                throw GradleException("QNN SDK does not exist at $qnnSDKLocalPath. Please set `qnnSDKLocalPath` correctly.")
//            }
//
//            if (!file("$qnnSDKLocalPath/lib/aarch64-android/libGenie.so").exists()) {
//                throw GradleException("libGenie does not exist. Please set `qnnSDKLocalPath` correctly.")
//            }
//
//            models.forEach { model ->
//                if (!file("$relAssetsPath$model/genie-config.json").exists()) {
//                    throw GradleException("Missing genie-config.json for $model.")
//                }
//                if (!file("$relAssetsPath$model/tokenizer.json").exists()) {
//                    throw GradleException("Missing tokenizer.json for $model.")
//                }
//            }
//
//            val libsABIDir = File(qnnBuildDir.get().asFile, "libs/arm64-v8a")
//
//            copy {
//                from(qnnSDKLocalPath)
//                include("**/lib/aarch64-android/libQnnHtp.so")
//                include("**/lib/aarch64-android/libQnnHtpPrepare.so")
//                include("**/lib/aarch64-android/libQnnSystem.so")
//                include("**/lib/aarch64-android/libQnnSaver.so")
//                include("**/lib/hexagon-v**/unsigned/libQnnHtpV**Skel.so")
//                include("**/lib/aarch64-android/libQnnHtpV**Stub.so")
//                into(libsABIDir)
//                eachFile {
//                    path = name
//                }
//                includeEmptyDirs = false
//            }
//        }
//    }

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
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.firebase.messaging.ktx)
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
    implementation("com.kizitonwose.calendar:compose:2.0.3")
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
}
