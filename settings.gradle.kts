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

        // ✅ Kakao SDK 저장소
        maven { url = uri("https://devrepo.kakao.com/nexus/content/groups/public/") }

        // ✅ 로컬 .aar 파일 경로
        flatDir {
            dirs("libs") // app/libs 폴더를 가리킴
        }

        // ✅ JitPack 저장소 (필요한 경우)
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "DomentiaCare"
include(":app")
include(":domentiacarewatch")
