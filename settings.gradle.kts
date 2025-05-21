pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // ✅ Kakao SDK용 저장소 추가
        maven { url = uri("https://devrepo.kakao.com/nexus/content/groups/public/") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // ✅ 여기도 Kakao SDK용 저장소 추가
        maven { url = uri("https://devrepo.kakao.com/nexus/content/groups/public/") }
        // ✅ 네이버 지도 SDK용 저장소 추가
        maven { url = uri("https://naver.jfrog.io/artifactory/maven/") }
    }
}

rootProject.name = "DomentiaCare"

include(":app")
include(":domentiacarewatch")
include(":ChatApp")
