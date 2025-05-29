🧠 온디바이스 AI 기반 치매 환자 관리 어플리케이션 : DementiaCare
> DementiaCare는 치매 환자의 안전과 자율성을 높이고, 보호자의 관리 부담을 줄이기 위해 개발된 스마트 헬스케어 앱입니다.
>
> 스마트폰 내장 AI 모델(TTS, STT, LLaMA 등)을 활용해 약 복용 및 일정 안내, 통화 내용 분석과 자동 일정 등록, 실시간 위치 추적 등
> 다양한 기능을 제공합니다. 또한 워치와 연동하여 약 복용 시간 알림, 안전범위 이탈 경고 등 실시간 알림 기능을 통해 환자와 보호자
> 모두에게 즉각적인 정보를 제공합니다.

![슬라이드3](https://github.com/user-attachments/assets/571941a9-4130-45d9-932c-64411ad25638)

<br/><br/>

## 🍁 Developers
**전담 업무:**
|  |  |  |  |
|:-:|:-:|:-:|:-:|
| <img src="https://github.com/user-attachments/assets/7e8cef4c-b951-4f1b-a8e6-1b3f2b7d0118" width="300"/> | <img src="https://github.com/user-attachments/assets/0c8f226a-d73f-47ba-8d1c-0e773f55e769" width="300"/> | <img src="https://github.com/user-attachments/assets/ffa22d00-9f9f-42b5-9ea2-2f5fe32e4636" width="300"/> | <img src="https://github.com/user-attachments/assets/dfbbea63-19af-4e70-9ede-955fbd4f97f6" width="300"/> |
| 김소룡 | 박진호 | 이종범 | 임성훈 |
| [@SoRyong-Kim](https://github.com/SoRyong-Kim) | [@parkddddd](https://github.com/parkddddd) | [@KorJIGSAW](https://github.com/KorJIGSAW) | [@PocheonLim](https://github.com/PocheonLim) |
| 앱 개발, AI 모델 연동<br> 백엔드 서버 개발 | 앱 개발, AI 모델 연동<br> 백엔드 서버 개발 | 앱 개발, Wear OS 개발<br> AI 모듈 개발, AI 모델 연동 | 웹 개발 |

 **유상미**: 지도 교수

## 📌 서비스 소개
![image](https://github.com/user-attachments/assets/757216a0-1ed3-4989-9d8b-41955e532980)



## 🚀 주요 기능

### 🎙️ 통화녹음 AI분석기능 - 통화 후 일정 자동 정리
![슬라이드6](https://github.com/user-attachments/assets/0afa7ded-dee3-4beb-85b8-cdd5b6f1dc22)


**OpenAI Whisper + LLaMA 3.2-3B 기반 통화 분석**
- 📞 **실시간 통화 녹음**: 사용자 통화 내용 자동 녹음
- 🧠 **AI 기반 내용 분석**: Whisper로 음성→텍스트 변환 후 LLaMA로 일정 추출
- 📅 **자동 일정 등록**: 약속, 병원 예약 등을 캘린더에 자동 추가
- 🔔 **스마트 알림**: 중요한 약속 놓침 방지를 위한 맞춤 알림

### ⌚ 스마트워치 연동 보호기능 - 환자·보호자 동시 알림 제공
![슬라이드5](https://github.com/user-attachments/assets/c626f7be-2001-4208-8473-ce5817db8577)


**Android + TTS 기반 실시간 모니터링**
- 🏠 **안전구역 모니터링**: Geofence 설정으로 이탈 시 즉시 알림
- 💊 **복약 알림**: 정확한 시간에 약물 복용 안내
- 📱 **신속한 알림**: 환자 스마트폰과 보호자 워치 알림
- 🆘 **응급상황 대응**: 위험 상황 감지 시 자동 보호자 연락

### 📍 위치 기반 환자 관리 기능 - 환자 위치 실시간 조회
![슬라이드7](https://github.com/user-attachments/assets/bf4eebdb-499e-4d48-8639-48ff5e60ffc3)


### 위치 관리
- 실시간 위치 추적 지도
- 안전구역 설정 및 관리

### 일정 관리
- 자동 추출된 일정 확인
- 복약 알림 설정

## 🎯 핵심 가치

### 👥 사용자 중심 설계
- **치매 환자**: 직관적인 음성 인터페이스로 쉬운 조작
- **보호자**: 실시간 모니터링으로 안심하고 일상 생활 가능

### 🔒 개인정보 보호 우선
- **온디바이스 AI**: 민감한 의료정보의 외부 유출 방지
- **로컬 처리**: 통화 내용 등 개인정보를 기기 내에서만 처리

### 💡 혁신적인 기술 융합
- **실시간 AI 분석**: 통화 내용을 즉시 분석하여 일정 자동 생성
- **멀티모달 인터페이스**: 음성, 터치, 알림을 조합한 직관적 UX


## 🎬 시연 영상
🔗 [DementiaCare 시연 영상](https://youtu.be/demo-video-link)

## 프로젝트 구조
<details>
  <summary>여기를 클릭하면 내용이 보입니다!</summary>
  
```
DementiaCare
├─ app
│  └─ src
│     ├─ androidTest
│     │  └─ java
│     │     └─ com
│     │        └─ example
│     │           └─ domentiacare
│     │              ├─ ExampleInstrumentedTest.kt
│     │              └─ RoomDBTest.kt
│     ├─ main
│     │  ├─ aidl
│     │  │  └─ com
│     │  │     └─ quicinc
│     │  │        └─ chatapp
│     │  │           ├─ IAnalysisCallback.aidl
│     │  │           └─ ILlamaAnalysisService.aidl
│     │  ├─ AndroidManifest.xml
│     │  ├─ assets
│     │  │  ├─ english_test1.wav
│     │  │  ├─ english_test_3_bili.wav
│     │  │  ├─ filters_vocab_en.bin
│     │  │  ├─ filters_vocab_multilingual.bin
│     │  │  ├─ postcode.html
│     │  │  ├─ test.wav
│     │  │  └─ whisper-tiny.en.tflite
│     │  ├─ ic_launcher-playstore.png
│     │  ├─ java
│     │  │  └─ com
│     │  │     └─ example
│     │  │        └─ domentiacare
│     │  │           ├─ assistant
│     │  │           │  ├─ AiAssistant.kt
│     │  │           │  └─ PatientSelectionDialog.kt
│     │  │           ├─ data
│     │  │           │  ├─ AiScheduleRequest.kt
│     │  │           │  ├─ isOnline.kt
│     │  │           │  ├─ local
│     │  │           │  │  ├─ CurrentUser.kt
│     │  │           │  │  ├─ RecordStorage.kt
│     │  │           │  │  ├─ schedule
│     │  │           │  │  │  ├─ BootReceiver.kt
│     │  │           │  │  │  ├─ Schedule.kt
│     │  │           │  │  │  ├─ ScheduleAlarmReceiver.kt
│     │  │           │  │  │  ├─ ScheduleDao.kt
│     │  │           │  │  │  ├─ ScheduleDatabase.kt
│     │  │           │  │  │  ├─ ScheduleDatabaseProvider.kt
│     │  │           │  │  │  ├─ ScheduleDto.kt
│     │  │           │  │  │  └─ ScheduleRepository.kt
│     │  │           │  │  ├─ SimpleLocalStorage.kt
│     │  │           │  │  └─ TokenManager.kt
│     │  │           │  ├─ model
│     │  │           │  │  ├─ CallLogEntry.kt
│     │  │           │  │  ├─ CallRecordingViewModel.kt
│     │  │           │  │  ├─ CallType.kt
│     │  │           │  │  ├─ Holiday.kt
│     │  │           │  │  ├─ PatientViewModel.kt
│     │  │           │  │  ├─ Record.kt
│     │  │           │  │  └─ RecordViewModel.kt
│     │  │           │  ├─ remote
│     │  │           │  │  ├─ api
│     │  │           │  │  │  └─ AuthApi.kt
│     │  │           │  │  ├─ AuthInterceptor.kt
│     │  │           │  │  ├─ connectWebSocket.kt
│     │  │           │  │  ├─ dto
│     │  │           │  │  │  ├─ KakaoLoginResponse.kt
│     │  │           │  │  │  ├─ KakaoTokenRequest.kt
│     │  │           │  │  │  ├─ LocationRequestBody.kt
│     │  │           │  │  │  ├─ Patient.kt
│     │  │           │  │  │  ├─ Phone.kt
│     │  │           │  │  │  ├─ RegisterUserRequest.kt
│     │  │           │  │  │  ├─ Schedule.kt
│     │  │           │  │  │  └─ User.kt
│     │  │           │  │  └─ RetrofitClient.kt
│     │  │           │  ├─ ScheduleData.kt
│     │  │           │  ├─ ScheduleInfo.kt
│     │  │           │  ├─ ScheduleResponse.kt
│     │  │           │  ├─ sync
│     │  │           │  │  └─ SimpleSyncManager.kt
│     │  │           │  └─ util
│     │  │           │     ├─ CallAudioLog.kt
│     │  │           │     ├─ CallLogQueryUtil.kt
│     │  │           │     ├─ M4aToWavConverter.kt
│     │  │           │     ├─ UserPreferences.kt
│     │  │           │     └─ WavWriter.kt
│     │  │           ├─ MainActivity.kt
│     │  │           ├─ MyApplication.kt
│     │  │           ├─ network
│     │  │           │  ├─ dto
│     │  │           │  │  ├─ ScheduleCreateRequest.kt
│     │  │           │  │  └─ ScheduleResponse.kt
│     │  │           │  ├─ RecordApiService.kt
│     │  │           │  └─ ScheduleApi.kt
│     │  │           ├─ service
│     │  │           │  ├─ androidtts
│     │  │           │  │  └─ TTSServiceManager.kt
│     │  │           │  ├─ CallRecordAnalyzeService.kt
│     │  │           │  ├─ llama
│     │  │           │  │  ├─ LlamaServiceManager.kt
│     │  │           │  │  ├─ ScheduleAnalysisResult.kt
│     │  │           │  │  └─ ScheduleData.kt
│     │  │           │  ├─ LocationForegroundService.kt
│     │  │           │  ├─ MyFirebaseMessagingService.kt
│     │  │           │  ├─ watch
│     │  │           │  │  └─ WatchMessageHelper.kt
│     │  │           │  └─ whisper
│     │  │           │     ├─ WaveUtil.java
│     │  │           │     ├─ Whisper.java
│     │  │           │     ├─ WhisperEngine.java
│     │  │           │     ├─ WhisperEngineJava.java
│     │  │           │     ├─ WhisperScreen.kt
│     │  │           │     ├─ WhisperUtil.java
│     │  │           │     └─ WhisperWrapper.kt
│     │  │           ├─ ui
│     │  │           │  ├─ AppNavHost.kt
│     │  │           │  ├─ calllog
│     │  │           │  │  ├─ CallLogItem.kt
│     │  │           │  │  └─ CallLogScreen.kt
│     │  │           │  ├─ component
│     │  │           │  │  ├─ BottomNavBar.kt
│     │  │           │  │  ├─ CustomCalendar.kt
│     │  │           │  │  ├─ DMT_Button.kt
│     │  │           │  │  ├─ DMT_DrawerMenuItem.kt
│     │  │           │  │  ├─ DMT_MenuItem.kt
│     │  │           │  │  ├─ DMT_WhiteButton.kt
│     │  │           │  │  ├─ SimpleDropdown.kt
│     │  │           │  │  └─ TopBar.kt
│     │  │           │  ├─ screen
│     │  │           │  │  ├─ call
│     │  │           │  │  │  ├─ business
│     │  │           │  │  │  │  └─ handle.kt
│     │  │           │  │  │  ├─ CallDetailScreen.kt
│     │  │           │  │  │  ├─ CallLogScreen.kt
│     │  │           │  │  │  ├─ CallLogViewModel.kt
│     │  │           │  │  │  ├─ components
│     │  │           │  │  │  │  ├─ AudioPlayer.kt
│     │  │           │  │  │  │  ├─ AudioPlayerSection.kt
│     │  │           │  │  │  │  ├─ CallInfoHeader.kt
│     │  │           │  │  │  │  ├─ DateTimeSelectionSection.kt
│     │  │           │  │  │  │  ├─ SaveButton.kt
│     │  │           │  │  │  │  ├─ ScheduleAnalysisSection.kt
│     │  │           │  │  │  │  ├─ SectionCard.kt
│     │  │           │  │  │  │  ├─ StatusMessage.kt
│     │  │           │  │  │  │  └─ TranscriptSection.kt
│     │  │           │  │  │  ├─ models
│     │  │           │  │  │  │  └─ Quin.kt
│     │  │           │  │  │  ├─ theme
│     │  │           │  │  │  │  └─ OrangeLight.kt
│     │  │           │  │  │  └─ utils
│     │  │           │  │  │     ├─ DateTimeParser.kt
│     │  │           │  │  │     └─ util.kt
│     │  │           │  │  ├─ home
│     │  │           │  │  │  └─ Home.kt
│     │  │           │  │  ├─ login
│     │  │           │  │  │  ├─ LoginScreen.kt
│     │  │           │  │  │  └─ RegisterScreen.kt
│     │  │           │  │  ├─ MyPage
│     │  │           │  │  │  └─ MyPageScreen.kt
│     │  │           │  │  ├─ MySettingScreen
│     │  │           │  │  │  └─ MySettingScreen.kt
│     │  │           │  │  ├─ navigate
│     │  │           │  │  │  └─ HomeNavigationScreen.kt
│     │  │           │  │  ├─ patientCare
│     │  │           │  │  │  ├─ GuardianScheduleScreen.kt
│     │  │           │  │  │  ├─ PatientAddScheduleScreen.kt
│     │  │           │  │  │  ├─ PatientDetailScreen.kt
│     │  │           │  │  │  ├─ PatientList.kt
│     │  │           │  │  │  ├─ PatientLocationScreen.kt
│     │  │           │  │  │  ├─ PatientScheduleViewModel.kt
│     │  │           │  │  │  └─ ScheduleScreenWrapper.kt
│     │  │           │  │  └─ schedule
│     │  │           │  │     ├─ AddScheduleScreen.kt
│     │  │           │  │     ├─ GuardianStyleScheduleScreen.kt
│     │  │           │  │     ├─ HorizontalCalendarComponent.kt
│     │  │           │  │     ├─ PagerCalendar.kt
│     │  │           │  │     ├─ ScheduleDetailScreen.kt
│     │  │           │  │     ├─ ScheduleScreen.kt
│     │  │           │  │     ├─ ScheduleViewModel.kt
│     │  │           │  │     ├─ SingleMonthCalendar.kt
│     │  │           │  │     └─ StyledAddScheduleScreen.kt
│     │  │           │  ├─ test
│     │  │           │  │  ├─ TestCalendar.kt
│     │  │           │  │  └─ TestLlamaActivity.kt
│     │  │           │  └─ theme
│     │  │           │     ├─ AppTypography.kt
│     │  │           │     ├─ Color.kt
│     │  │           │     ├─ Theme.kt
│     │  │           │     └─ Type.kt
│     │  │           └─ webView
│     │  │              └─ AddressSearchActivity.kt
│     │  ├─ python
│     │  │  ├─ convert.py
│     │  │  └─ requirements.txt
│     │  └─ res
│     │     ├─ drawable
│     │     │  ├─ home.png
│     │     │  ├─ ic_launcher_background.xml
│     │     │  └─ ic_launcher_foreground.xml
│     │     ├─ font
│     │     │  ├─ pretendard_black.ttf
│     │     │  ├─ pretendard_bold.ttf
│     │     │  ├─ pretendard_light.ttf
│     │     │  ├─ pretendard_medium.ttf
│     │     │  ├─ pretendard_regular.ttf
│     │     │  └─ pretendard_thin.ttf
│     │     ├─ mipmap-anydpi-v26
│     │     │  ├─ ic_launcher.xml
│     │     │  └─ ic_launcher_round.xml
│     │     ├─ mipmap-hdpi
│     │     │  ├─ ic_launcher.webp
│     │     │  ├─ ic_launcher_foreground.webp
│     │     │  └─ ic_launcher_round.webp
│     │     ├─ mipmap-mdpi
│     │     │  ├─ ic_launcher.webp
│     │     │  ├─ ic_launcher_foreground.webp
│     │     │  └─ ic_launcher_round.webp
│     │     ├─ mipmap-xhdpi
│     │     │  ├─ ic_launcher.webp
│     │     │  ├─ ic_launcher_foreground.webp
│     │     │  └─ ic_launcher_round.webp
│     │     ├─ mipmap-xxhdpi
│     │     │  ├─ ic_launcher.webp
│     │     │  ├─ ic_launcher_foreground.webp
│     │     │  └─ ic_launcher_round.webp
│     │     ├─ mipmap-xxxhdpi
│     │     │  ├─ ic_launcher.webp
│     │     │  ├─ ic_launcher_foreground.webp
│     │     │  └─ ic_launcher_round.webp
│     │     ├─ values
│     │     │  ├─ colors.xml
│     │     │  ├─ strings.xml
│     │     │  └─ themes.xml
│     │     └─ xml
│     │        ├─ backup_rules.xml
│     │        ├─ data_extraction_rules.xml
│     │        └─ network_security_config.xml
│     └─ test
│        └─ java
│           └─ com
│              └─ example
│                 └─ domentiacare
│                    └─ ExampleUnitTest.kt
├─ ChatApp
│  ├─ assets
│  │  ├─ ai-hub-qnn-version.png
│  │  ├─ chatapp_demo_1.mov
│  │  └─ chatapp_demo_2.mov
│  ├─ README.md
│  └─ src
│     └─ main
│        ├─ aidl
│        │  └─ com
│        │     └─ quicinc
│        │        └─ chatapp
│        │           ├─ IAnalysisCallback.aidl
│        │           └─ ILlamaAnalysisService.aidl
│        ├─ AndroidManifest.xml
│        ├─ assets
│        │  ├─ htp_config
│        │  │  ├─ qualcomm-snapdragon-8-elite.json
│        │  │  ├─ qualcomm-snapdragon-8-gen2.json
│        │  │  └─ qualcomm-snapdragon-8-gen3.json
│        │  ├─ models
│        │  │  └─ llama3_2_3b
│        │  │     ├─ genie-config.json
|        |  |     ├─ llama_v3_2_3b_chat_quantized_part_1_of_3
|        |  |     ├─ llama_v3_2_3b_chat_quantized_part_2_of_3
|        |  |     ├─ llama_v3_2_3b_chat_quantized_part_3_of_3
│        │  │     └─ tokenizer.json
│        │  └─ README.txt
│        ├─ cpp
│        │  ├─ CMakeLists.txt
│        │  ├─ GenieLib.cpp
│        │  ├─ GenieWrapper.cpp
│        │  ├─ GenieWrapper.hpp
│        │  ├─ PromptHandler.cpp
│        │  └─ PromptHandler.hpp
│        ├─ ic_launcher-playstore.png
│        ├─ java
│        │  └─ com
│        │     └─ quicinc
│        │        └─ chatapp
│        │           ├─ ChatMessage.java
│        │           ├─ Conversation.java
│        │           ├─ GenieWrapper.java
│        │           ├─ LlamaAnalysisService.java
│        │           ├─ MainActivity.java
│        │           ├─ MessageSender.java
│        │           ├─ Message_RecyclerViewAdapter.java
│        │           └─ StringCallback.java
│        └─ res
│           ├─ drawable
│           │  ├─ bot_response.xml
│           │  ├─ ic_launcher_background.xml
│           │  ├─ ic_launcher_foreground.xml
│           │  ├─ text_rounded_corner.xml
│           │  └─ user_input.xml
│           ├─ layout
│           │  ├─ activity_main.xml
│           │  ├─ chat.xml
│           │  └─ chat_row.xml
│           ├─ mipmap-anydpi-v26
│           │  ├─ ic_launcher.xml
│           │  └─ ic_launcher_round.xml
│           ├─ mipmap-hdpi
│           │  ├─ ic_launcher.webp
│           │  ├─ ic_launcher_foreground.webp
│           │  └─ ic_launcher_round.webp
│           ├─ mipmap-mdpi
│           │  ├─ ic_launcher.webp
│           │  ├─ ic_launcher_foreground.webp
│           │  └─ ic_launcher_round.webp
│           ├─ mipmap-xhdpi
│           │  ├─ ic_launcher.webp
│           │  ├─ ic_launcher_foreground.webp
│           │  └─ ic_launcher_round.webp
│           ├─ mipmap-xxhdpi
│           │  ├─ ic_launcher.webp
│           │  ├─ ic_launcher_foreground.webp
│           │  └─ ic_launcher_round.webp
│           ├─ mipmap-xxxhdpi
│           │  ├─ ic_launcher.webp
│           │  ├─ ic_launcher_foreground.webp
│           │  └─ ic_launcher_round.webp
│           ├─ values
│           │  ├─ colors.xml
│           │  ├─ strings.xml
│           │  └─ themes.xml
│           ├─ values-night
│           │  └─ themes.xml
│           └─ xml
│              ├─ backup_rules.xml
│              └─ data_extraction_rules.xml
├─ domentiacarewatch
│  ├─ lint.xml
│  ├─ proguard-rules.pro
│  └─ src
│     └─ main
│        ├─ AndroidManifest.xml
│        ├─ java
│        │  └─ com
│        │     └─ example
│        │        └─ domentiacarewatch
│        │           ├─ complication
│        │           │  └─ MainComplicationService.kt
│        │           ├─ presentation
│        │           │  ├─ MainActivity.kt
│        │           │  └─ theme
│        │           │     └─ Theme.kt
│        │           ├─ service
│        │           └─ tile
│        │              └─ MainTileService.kt
│        └─ res
│           ├─ drawable
│           │  ├─ splash_icon.xml
│           │  └─ tile_preview.png
│           ├─ drawable-round
│           │  └─ tile_preview.png
│           ├─ mipmap-hdpi
│           │  └─ ic_launcher.webp
│           ├─ mipmap-mdpi
│           │  └─ ic_launcher.webp
│           ├─ mipmap-xhdpi
│           │  └─ ic_launcher.webp
│           ├─ mipmap-xxhdpi
│           │  └─ ic_launcher.webp
│           ├─ mipmap-xxxhdpi
│           │  └─ ic_launcher.webp
│           ├─ values
│           │  ├─ strings.xml
│           │  └─ styles.xml
│           └─ values-round
│              └─ strings.xml
├─ gradle
│  ├─ libs.versions.toml
│  └─ wrapper
│     ├─ gradle-wrapper.jar
│     └─ gradle-wrapper.properties
├─ gradle.properties
├─ gradlew
└─ gradlew.bat
```


</details>


---

<div align="center">
  <strong>🧠 DementiaCare와 함께하는 스마트한 치매 케어 🧠</strong>
  <br/>
  <em>AI 기술로 더 안전하고 편리한 일상을 만들어갑니다</em>
</div>
