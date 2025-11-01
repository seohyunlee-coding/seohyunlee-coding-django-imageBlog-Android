# 🖼️ Image Blog - Android 클라이언트

이 저장소는 Django 기반 백엔드(예: Python Image Blog)와 통신하는 Android 클라이언트 앱입니다. 학습용/샘플용으로 게시글 목록 조회, 검색, 이미지 업로드(게시물 작성), 수정, 삭제 기능을 제공합니다.

## 📂 프로젝트 구성(예시)
```
ImageBlog-Android/
├── README.md
├── app/
│   ├── build.gradle
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/imageblog/
│   │   │   │   ├── MainActivate.java
│   │   │   │   ├── NewPostActivity.java
│   │   │   │   ├── PostDetailActivity.java
│   │   │   │   ├── NetworkClient.java
│   │   │   │   ├── AuthInterceptor.java
│   │   │   │   └── AuthHelper.java
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   ├── drawable/
│   │   │   │   └── values/
│   │   │   └── AndroidManifest.xml
│   └── proguard-rules.pro
└── gradlew, gradlew.bat, settings.gradle.kts
```

## ⚙️ 개발 환경 및 필수 도구
- Android Studio 최신 안정화 버전
- Android SDK (프로젝트의 compileSdk / targetSdk에 맞게 설치)
- Java 11 이상
- (백엔드) Django REST API가 로컬 또는 외부 서버에서 동작해야 앱이 정상 동작

## 🔧 프로젝트 빌드·실행 (Windows 명령 예시)
### 1) Android Studio에서 열기:
- Android Studio → Open → 이 폴더(ImageBlog-Android) 선택

### 2) 명령줄 빌드(로컬에서 빠르게 빌드만 할 때):
```cmd
cd "C:\Users\dev\Desktop\new android"
gradlew.bat assembleDebug
```
- 혹은 상단의 플레이 버튼을 눌러 실행

### 3) 디바이스에서 실행
- 에뮬레이터 또는 물리 디바이스 연결 후 Android Studio에서 Run

## 🚀 주요 기능
- 게시글 목록 조회
- 게시글 상세 보기(이미지 포함),
- 게시글 수정
- 게시글 삭제
- 새 게시글 작성
* 모든 기능은 토큰 기반 인증을 바탕으로 함
* Token 인증에 오류가 생길 경우 토큰 발급 api에 username: admin, password: myblog1008을 넣어 토큰을 재발급 받아볼 것

## 🔗 네트워크 / API (앱에서 사용하는 엔드포인트 예)
- 게시글 목록 조회: GET /api/posts
- 게시글 생성: POST /api_root/Post/
- 게시글 수정: PATCH /api_root/Post/{id}/
- 게시글 삭제: DELETE /api_root/Post/{id}/
- 게시글 검색: GET /api/posts/search/?q={query}
- 토큰 발급:  POST /api-token-auth/

## 🛠️ 앱 내부 구조(주요 클래스)
- `NetworkClient` — OkHttp 클라이언트 싱글턴, `AuthInterceptor` 추가
- `AuthInterceptor` — 모든 요청에 Authorization 헤더를 자동으로 추가, 401 처리 로직 포함
- `AuthHelper` — 토큰 획득/캐시/무효화 관리
- `MainActivate` — 메인 액티비티(목록/검색/동기화)
- `NewPostActivity` — 이미지 선택 및 업로드(POST, PATCH)
- `PostDetailActivity` — 상세/삭제(DELETE)

## 📚 사용 기술 / 라이브러리
- Android (Java)
- OkHttp
- Glide
- AndroidX, Material Components

## 📦 배포 / 서버 참고
- 백엔드는 PythonAnywherehttps://cwijiq.pythonanywhere.com/)에 배포 완료

## 👩‍💻 작성자/연락처
- 이름: 이서현
- 이메일: cwijiq3085@gmail.com
- GitHub: https://github.com/seohyunlee-coding
