pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        // 프로젝트 전용 리포지토리
        maven { url = uri("https://devrepo.kakao.com/nexus/content/groups/public/") }
        maven { url = uri("https://naver.jfrog.io/artifactory/maven/") }
    }
    plugins {
        id("com.android.application")             version "8.7.3"  apply false
        id("org.jetbrains.kotlin.android")        version "2.0.0"  apply false
        id("org.jetbrains.kotlin.kapt")           version "2.0.0"  apply false
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"  apply false
        id("com.google.dagger.hilt.android")      version "2.48"   apply false
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // SDK 전용 리포지토리
        maven { url = uri("https://devrepo.kakao.com/nexus/content/groups/public/") }
        maven { url = uri("https://naver.jfrog.io/artifactory/maven/") }
    }
}

rootProject.name = "DomentiaCare"
include(":app")
include(":domentiacarewatch")