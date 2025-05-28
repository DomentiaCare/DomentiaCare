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
- **유상미**: 지도 교수
- **김소룡**: 앱 개발, AI 모델 연동, 백엔드 서버 개발
- **박진호**: 앱 개발, AI 모델 연동, 백엔드 서버 개발  
- **이종범**: 앱 개발, Wear OS 개발, AI 모델 개발, AI 모델 연동
- **임성훈**: 웹 개발

## 📌 서비스 소개
![KakaoTalk_20250520_012403204](https://github.com/user-attachments/assets/a2d98ea8-00c7-49c0-b687-66d7571deb92)


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
![슬라이드7](https://github.com/user-attachments/assets/d77121be-b4e1-4cea-862f-e12984c69ebc)

**Google Maps API 기반 위치 추적**
- 🗺️ **실시간 위치 확인**: 보호자가 언제든 환자 위치 조회
- 🏡 **안전구역 설정**: 환자의 활동 안전반경 설정
- 🚶 **이동경로 추적**: 일일 활동 패턴 분석 및 히스토리 제공
- 📲 **즉시 알림**: 설정 범위 이탈 시 보호자에게 실시간 알림

## 🧑‍💻 시스템 아키텍쳐
![image](https://github.com/user-attachments/assets/5b3eee10-1425-48f6-bc57-ba951c15d042)


### 📱 Client (Android + React Web)
- **Android**: Kotlin 기반 네이티브 앱
- **React**: 보호자용 웹 대시보드
- **Samsung Galaxy S25 + Galaxy Watch**: 최적화된 하드웨어 연동

### 🔧 On-Device AI
- **Meta LLaMA 3.2**: 온디바이스 자연어 처리
- **OpenAI Whisper**: 실시간 음성 인식
- **Android TTS**: 한국어 음성 합성

### 🌐 Backend Infrastructure
- **Spring Boot**: RESTful API 서버
- **Apache Tomcat**: 웹 애플리케이션 서버
- **Amazon EC2**: 클라우드 호스팅
- **MariaDB**: 사용자 데이터 및 일정 관리

### 🔗 External APIs
- **Google Maps API**: 위치 서비스 및 지도
- **Kakao Login API**: 간편 소셜 로그인

## 💡 기대 효과

### 1️⃣ 신속 대응
실시간 위치 공유로 환자의 안전범위 이탈 시 신속한 대응 가능

### 2️⃣ 일정 관리  
약 복용, 일정 누락 없이 관리할 수 있어 보호자의 부담 감소

### 3️⃣ 고령화 맞춤
고령화 사회 대응을 위한 스마트 헬스케어 솔루션 제공

## 🛠️ 기술 스택

### 📱 Frontend & Mobile
- **Android**: Kotlin, Java, Jetpack Compose, Hilt
- **React**: TypeScript, Tailwind CSS (보호자 대시보드)

### 🔧 Backend & Infrastructure  
- **Framework**: Spring Boot, Apache Tomcat
- **Database**: MariaDB (Repository 패턴)
- **Cloud**: Amazon EC2
- **Architecture**: Controller-Service-Repository 구조

### 🤖 AI & ML (On-Device)
- **LLM**: Meta LLaMA 3.2-3B (온디바이스 언어모델)
- **STT**: OpenAI Whisper, Android STT (음성→텍스트 변환)
- **TTS**: Android TTS (텍스트→음성 합성)

### 🔗 External APIs
- **Maps**: Google Maps API (위치 서비스)
- **Auth**: Kakao Login API (소셜 로그인)

### 🖥️ Development Environment
- **개발 환경**: Windows, Ubuntu, Google Colab, Jupyter Notebook
- **개발 도구**: Android Studio, IntelliJ, VSCode,HeidSQL, Jupyter Notebook, Google Colab
- **개발 언어**: JavaScript, Java, Kotlin, Python, TypeScript
- **주요 기술**: React, Spring Boot, Jetpack Compose, TensorFlow Lite, Whisper-small.en, LLaMA 3.2-3B, Android TTS, Google ML Kit

### 📱 Hardware
- **Device**: Samsung Galaxy S25
- **Wearable**: Samsung Galaxy Watch4/5 
- **Chipset**: Snapdragon 8 Elite for Galaxy (온디바이스 AI 최적화)
- **OS Support**: Android 12+, Wear OS 3.0+
- **AI Accerleration**: NPU활용 LLaMA 3.2-3B 가속화

## 📱 주요 화면

### 메인 대시보드
- 환자 상태 실시간 모니터링
- 당일 일정 및 복약 알림
- AI 어시스턴트를 통한 빠른 접근

### AI 어시스턴트
- 음성 명령 인식 및 처리
- 실시간 대화형 인터페이스
- 일정 조회 및 

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

## 📞 문의사항
프로젝트에 대한 문의사항이나 제안사항이 있으시면 언제든 연락주세요!

- 📧 Email: bm8383@naver.com
- 📱 Demo: [데모 사이트](https://dementia-care-demo.vercel.app)

---

<div align="center">
  <strong>🧠 DementiaCare와 함께하는 스마트한 치매 케어 🧠</strong>
  <br/>
  <em>AI 기술로 더 안전하고 편리한 일상을 만들어갑니다</em>
</div>
