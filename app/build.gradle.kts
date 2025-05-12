import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.compose.compiler)
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

        manifestPlaceholders["naverMapClientId"] = naverMapClientId
        buildConfigField("String", "NAVER_MAP_CLIENT_ID", "\"$naverMapClientId\"")
        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")

        manifestPlaceholders["GOOGLE_MAP_KEY"] = project.findProperty("GOOGLE_MAP_KEY") ?: ""
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
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11) // 기존의 jvmTarget과 버전을 맞춰주자
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    kapt {
        correctErrorTypes = true
        arguments {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }
}

composeCompiler {
    enableStrongSkippingMode = true
    includeSourceInformation = true
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
    implementation(libs.androidx.room.common.jvm)
    implementation(libs.androidx.room.runtime)     // androidx-room-runtime
    implementation(libs.androidx.room.ktx)         // androidx-room-ktx
    kapt         (libs.androidx.room.compiler)     // androidx-room-compiler

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
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
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

    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-compiler:2.51.1")

    // Room
    implementation("androidx.room:room-ktx:2.7.1")
    kapt("androidx.room:room-compiler:2.7.1")

}
