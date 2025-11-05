# 릴리즈 빌드 가이드

PocketChord 앱의 릴리즈 빌드를 생성하는 방법을 안내합니다.

---

## ✅ 완료된 설정

`build.gradle.kts`에 다음 설정이 추가되었습니다:

### 1. BuildConfig 필드
```kotlin
defaultConfig {
    buildConfigField("String", "VERSION_NAME", "\"${versionName}\"")
    buildConfigField("int", "VERSION_CODE", "${versionCode}")
}
```

### 2. Debug 빌드 설정
```kotlin
debug {
    buildConfigField("String", "SUPABASE_APP_ID", 
        "\"com.sweetapps.pocketchord.debug\"")
    applicationIdSuffix = ".debug"
    versionNameSuffix = "-DEBUG"
}
```

### 3. Release 빌드 설정
```kotlin
release {
    buildConfigField("String", "SUPABASE_APP_ID", 
        "\"com.sweetapps.pocketchord\"")
    isMinifyEnabled = true
    isShrinkResources = true
    proguardFiles(...)
}
```

---

## 🚀 릴리즈 빌드 방법

### 방법 1: Android Studio (권장)

1. **Build Variant 변경**
   - `View` → `Tool Windows` → `Build Variants`
   - `Active Build Variant`를 `release`로 변경

2. **서명되지 않은 APK 생성**
   - `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
   - 결과: `app/build/outputs/apk/release/app-release-unsigned.apk`

3. **서명된 APK/Bundle 생성** (Google Play용)
   - `Build` → `Generate Signed Bundle / APK`
   - `Android App Bundle` 선택 (권장) 또는 `APK`
   - 키스토어 생성 또는 선택 (아래 참조)

### 방법 2: Gradle 명령어

```bash
# APK 빌드 (서명 안 됨)
.\gradlew assembleRelease

# AAB 빌드 (서명 안 됨)
.\gradlew bundleRelease

# 출력 위치
# APK: app/build/outputs/apk/release/app-release-unsigned.apk
# AAB: app/build/outputs/bundle/release/app-release.aab
```

---

## 🔑 키스토어 생성 (첫 릴리즈 시)

### 키스토어가 없는 경우

1. **Android Studio에서 생성**
   - `Build` → `Generate Signed Bundle / APK`
   - `Create new...` 클릭
   - 정보 입력:
     - **Key store path**: `G:\Workspace\PocketChord\keystore\pocketchord-release.jks`
     - **Password**: 안전한 비밀번호 (기록 필수!)
     - **Alias**: `pocketchord`
     - **Validity**: `25년`
     - **Certificate 정보** 입력

2. **명령어로 생성**
   ```bash
   # 키스토어 폴더 생성
   mkdir keystore
   
   # 키스토어 생성
   keytool -genkey -v -keystore keystore/pocketchord-release.jks -alias pocketchord -keyalg RSA -keysize 2048 -validity 9125
   ```

### ⚠️ 중요: 키스토어 보안

- 🔒 **절대 Git에 커밋하지 마세요!**
- 💾 **안전한 곳에 백업하세요** (USB, 클라우드 등)
- 🔑 **비밀번호를 기록하세요** (잃어버리면 복구 불가!)

---

## 🔧 서명 설정 (선택사항)

### build.gradle.kts에 서명 설정 추가

현재는 주석 처리되어 있습니다. 키스토어 생성 후 활성화하세요:

```kotlin
android {
    // 1. signingConfigs 블록 주석 해제
    signingConfigs {
        create("release") {
            storeFile = file("keystore/pocketchord-release.jks")
            storePassword = "YOUR_KEYSTORE_PASSWORD"  // ← 실제 비밀번호로 변경
            keyAlias = "pocketchord"
            keyPassword = "YOUR_KEY_PASSWORD"  // ← 실제 비밀번호로 변경
        }
    }
    
    buildTypes {
        release {
            // 2. signingConfig 주석 해제
            signingConfig = signingConfigs.getByName("release")
            // ...existing code...
        }
    }
}
```

### 보안 강화: 환경변수 사용 (권장)

비밀번호를 코드에 직접 입력하지 않고 환경변수 사용:

1. **local.properties에 추가** (Git 제외됨)
   ```properties
   RELEASE_STORE_FILE=keystore/pocketchord-release.jks
   RELEASE_STORE_PASSWORD=your_password_here
   RELEASE_KEY_ALIAS=pocketchord
   RELEASE_KEY_PASSWORD=your_password_here
   ```

2. **build.gradle.kts 수정**
   ```kotlin
   val keystorePropertiesFile = rootProject.file("local.properties")
   val keystoreProperties = Properties()
   if (keystorePropertiesFile.exists()) {
       keystoreProperties.load(FileInputStream(keystorePropertiesFile))
   }
   
   android {
       signingConfigs {
           create("release") {
               storeFile = file(keystoreProperties["RELEASE_STORE_FILE"] as String)
               storePassword = keystoreProperties["RELEASE_STORE_PASSWORD"] as String
               keyAlias = keystoreProperties["RELEASE_KEY_ALIAS"] as String
               keyPassword = keystoreProperties["RELEASE_KEY_PASSWORD"] as String
           }
       }
   }
   ```

---

## 📦 빌드 결과물

### APK vs AAB

| 형식 | 용도 | 크기 | 권장도 |
|------|------|------|--------|
| **APK** | 직접 배포, 테스트 | 큼 | ⭐⭐⭐ |
| **AAB** | Google Play 업로드 | 작음 (최적화) | ⭐⭐⭐⭐⭐ |

### 출력 위치

**APK**:
```
app/build/outputs/apk/release/
├── app-release-unsigned.apk  (서명 안 됨)
└── app-release.apk            (서명됨, signingConfig 설정 시)
```

**AAB**:
```
app/build/outputs/bundle/release/
└── app-release.aab
```

---

## ✅ 릴리즈 체크리스트

### 빌드 전
- [ ] `versionCode` 증가 (이전 버전보다 높게)
- [ ] `versionName` 업데이트 (예: "1.0.0" → "1.0.1")
- [ ] Supabase app_id가 `"com.sweetapps.pocketchord"`인지 확인
- [ ] 테스트 코드 제거 (MainActivity의 `testSupabaseConnection()` 등)
- [ ] ProGuard 규칙 확인

### 빌드
- [ ] Release 빌드 선택
- [ ] 서명 설정 완료
- [ ] APK/AAB 생성 성공

### 빌드 후
- [ ] APK/AAB 크기 확인 (비정상적으로 크지 않은지)
- [ ] 실제 기기에 설치하여 테스트
- [ ] 모든 기능 정상 작동 확인
- [ ] Supabase 공지사항 표시 확인 (실제 공지만)

---

## 🧪 릴리즈 빌드 테스트

### 설치 방법

```bash
# APK 직접 설치
adb install app/build/outputs/apk/release/app-release.apk

# 기존 앱 제거 후 설치 (데이터도 삭제됨)
adb install -r app/build/outputs/apk/release/app-release.apk
```

### 확인 사항

1. **앱 설치 및 실행**
   - 크래시 없이 정상 실행되는지

2. **공지사항 확인**
   - Supabase 실제 공지사항만 표시되는지
   - 디버그 공지사항은 표시 안 되는지

3. **모든 기능 테스트**
   - 코드 그리드 표시
   - 코드 상세 화면
   - 메트로놈/튜너
   - 설정 변경
   - 광고 표시 (실제 광고 ID 사용 시)

4. **BuildConfig 확인**
   ```kotlin
   // 임시 로그 추가
   Log.d("BuildConfig", "DEBUG = ${BuildConfig.DEBUG}")
   Log.d("BuildConfig", "BUILD_TYPE = ${BuildConfig.BUILD_TYPE}")
   Log.d("BuildConfig", "SUPABASE_APP_ID = ${BuildConfig.SUPABASE_APP_ID}")
   ```
   
   **예상 출력** (Release):
   ```
   D/BuildConfig: DEBUG = false
   D/BuildConfig: BUILD_TYPE = release
   D/BuildConfig: SUPABASE_APP_ID = com.sweetapps.pocketchord
   ```

---

## 📤 Google Play 업로드

### 준비물
- ✅ 서명된 AAB 파일
- ✅ Google Play Console 계정
- ✅ 앱 스크린샷 (최소 2개, 권장 8개)
- ✅ 앱 설명
- ✅ 개인정보 보호정책 URL (필수)

### 업로드 절차

1. **Google Play Console 접속**
   - https://play.google.com/console

2. **새 앱 만들기** (첫 출시)
   - 앱 이름: PocketChord
   - 기본 언어: 한국어
   - 앱 유형: 앱
   - 무료/유료: 무료

3. **AAB 업로드**
   - `프로덕션` → `새 버전 만들기`
   - AAB 파일 업로드
   - 버전 정보 입력

4. **스토어 등록정보**
   - 앱 설명
   - 스크린샷
   - 아이콘 (512x512)
   - 기능 그래픽 (1024x500)

5. **검토 제출**

---

## 🔧 ProGuard 규칙

현재 `proguard-rules.pro`에 다음이 포함되어야 합니다:

```proguard
# Supabase
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Serializable classes
-keep,includedescriptorclasses class com.sweetapps.pocketchord.data.supabase.model.** { *; }

# Google Ads
-keep class com.google.android.gms.ads.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
```

---

## 🎯 버전 관리

### versionCode vs versionName

```kotlin
defaultConfig {
    versionCode = 1      // 숫자, 항상 증가
    versionName = "1.0.0"  // 문자열, 사용자에게 표시
}
```

### 버전 넘버링 규칙 (Semantic Versioning)

```
MAJOR.MINOR.PATCH

예시:
1.0.0  - 첫 출시
1.0.1  - 버그 수정
1.1.0  - 새 기능 추가 (하위 호환)
2.0.0  - 대규모 변경 (호환성 깨짐)
```

### 버전 업데이트 예시

**버그 수정 버전**:
```kotlin
versionCode = 2
versionName = "1.0.1"
```

**기능 추가 버전**:
```kotlin
versionCode = 3
versionName = "1.1.0"
```

**메이저 업데이트**:
```kotlin
versionCode = 10
versionName = "2.0.0"
```

---

## ⚠️ 주의사항

### 1. 키스토어 절대 분실 금지!
- 키스토어를 잃어버리면 **앱 업데이트 불가능**
- 새 키스토어로는 기존 앱 업데이트 불가
- 반드시 **안전한 곳에 백업**

### 2. 비밀번호 기록
- 키스토어 비밀번호
- 키 비밀번호
- 별도 문서에 안전하게 보관

### 3. Git 제외
```.gitignore
# 키스토어 제외
keystore/
*.jks
*.keystore

# 비밀번호 제외
local.properties
```

### 4. 테스트 코드 제거
릴리즈 전 반드시 테스트 코드 제거:
- MainActivity의 `testSupabaseConnection()`
- 디버그 로그
- 테스트용 공지사항 데이터

---

## 🐛 문제 해결

### 문제 1: 빌드 실패

**오류**: `Execution failed for task ':app:minifyReleaseWithR8'`

**해결**: ProGuard 규칙 추가 또는 난독화 비활성화
```kotlin
release {
    isMinifyEnabled = false  // 임시로 비활성화
}
```

### 문제 2: 서명 실패

**오류**: `Cannot read key store file`

**해결**: 키스토어 경로 확인
```kotlin
storeFile = file("keystore/pocketchord-release.jks")
// 절대 경로 확인
println(storeFile.absolutePath)
```

### 문제 3: BuildConfig 미생성

**오류**: `Unresolved reference: BuildConfig`

**해결**:
```kotlin
android {
    buildFeatures {
        buildConfig = true  // 활성화
    }
}
```

그리고 Gradle Sync

---

## ✅ 완료!

**다음 단계**:
1. 키스토어 생성 (첫 릴리즈 시)
2. `.\gradlew assembleRelease` 실행
3. APK 테스트
4. Google Play 업로드 (AAB)

**참고 문서**:
- Google Play Console: https://play.google.com/console
- Android 서명 가이드: https://developer.android.com/studio/publish/app-signing

릴리즈 빌드 준비 완료! 🚀

