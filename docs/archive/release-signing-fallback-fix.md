# Release 빌드 서명 설정 문제 해결

## 📋 문제 상황

**에러 메시지**: 
```
SigningConfig "release" is missing required property "storeFile".
```

**발생 시점**: Build Variants를 `release`로 변경 후 앱 실행 시

---

## 🔍 원인 분석

### build.gradle.kts의 서명 설정

```kotlin
signingConfigs {
    create("release") {
        // 환경변수에서 서명 정보 읽기
        val keystorePath = System.getenv("KEYSTORE_PATH")
        val keystoreStorePw = System.getenv("KEYSTORE_STORE_PW")
        val keyAlias = System.getenv("KEY_ALIAS")
        val keyPw = System.getenv("KEY_PASSWORD")

        if (keystorePath != null && ...) {
            storeFile = file(keystorePath)
            // ...
        } else {
            // ⚠️ 환경변수가 없으면 storeFile 설정 안 됨!
            println("⚠️ WARNING: Release signing config missing!")
        }
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        // ❌ storeFile이 없는 config를 사용하려고 해서 에러!
    }
}
```

**문제점**:
- Release 서명 설정이 환경변수에 의존
- 환경변수가 없으면 `storeFile`이 설정되지 않음
- 하지만 release 빌드는 무조건 release 서명을 사용하려고 함
- → 빌드 실패!

---

## ✅ 해결 방법

### 수정된 코드

```kotlin
buildTypes {
    release {
        buildConfigField("String", "SUPABASE_APP_ID", 
            "\"com.sweetapps.pocketchord\"")

        // 서명 설정: 환경변수 있으면 release, 없으면 debug (개발용)
        val hasReleaseKey = System.getenv("KEYSTORE_PATH") != null
        signingConfig = if (hasReleaseKey) {
            signingConfigs.getByName("release")
        } else {
            println("⚠️ Using debug keystore for release build (development only!)")
            signingConfigs.getByName("debug")
        }

        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(...)
    }
}
```

**변경 사항**:
- ✅ 환경변수 확인 로직 추가
- ✅ 환경변수 있음 → release keystore 사용
- ✅ 환경변수 없음 → **debug keystore 사용 (fallback)**

---

## 📊 동작 방식

### 개발 환경 (환경변수 없음)

```
Release 빌드 실행
    ↓
환경변수 확인: KEYSTORE_PATH = null
    ↓
Fallback: debug keystore 사용 ✅
    ↓
빌드 성공!
    ↓
⚠️ 경고 메시지: "Using debug keystore for release build"
```

**특징**:
- ✅ 개발 중 테스트 가능
- ✅ 난독화 적용됨
- ✅ Release 설정으로 빌드됨
- ⚠️ **Play Store 업로드 불가** (debug keystore이므로)

### 실제 배포 (환경변수 설정)

```
환경변수 설정:
KEYSTORE_PATH=path/to/release.keystore
KEYSTORE_STORE_PW=...
KEY_ALIAS=...
KEY_PASSWORD=...
    ↓
Release 빌드 실행
    ↓
환경변수 확인: KEYSTORE_PATH = 있음!
    ↓
Release keystore 사용 ✅
    ↓
빌드 성공!
    ↓
✅ Play Store 업로드 가능
```

---

## 🎯 각 케이스별 설명

### Case 1: Debug 빌드
```
Build Variant: debug
서명: debug keystore (자동)
용도: 개발 및 테스트
```

### Case 2: Release 빌드 (환경변수 없음)
```
Build Variant: release
서명: debug keystore (fallback)
용도: 로컬에서 Release 설정 테스트
⚠️ Play Store 업로드 불가
```

### Case 3: Release 빌드 (환경변수 있음)
```
Build Variant: release
서명: release keystore
용도: 실제 배포용 APK/AAB 생성
✅ Play Store 업로드 가능
```

---

## 🧪 테스트 결과

### Release 빌드 (debug keystore 사용)

```bash
.\gradlew.bat assembleRelease
```

**출력**:
```
⚠️ Using debug keystore for release build (development only!)
BUILD SUCCESSFUL
```

**결과**:
- ✅ 빌드 성공
- ✅ 난독화 적용됨
- ✅ 앱 버전: `1.0.1.release` 표시
- ⚠️ Debug 키스토어로 서명됨

---

## 📝 주의사항

### ⚠️ Debug Keystore로 서명된 Release 빌드

**할 수 있는 것**:
- ✅ 로컬에서 Release 동작 테스트
- ✅ 난독화된 코드 확인
- ✅ Release 환경 검증

**할 수 없는 것**:
- ❌ Play Store 업로드
- ❌ 실제 사용자에게 배포
- ❌ Google Play Console에서 업데이트

### ✅ 실제 배포 시

**환경변수 설정 방법**:

#### Windows (PowerShell)
```powershell
$env:KEYSTORE_PATH = "C:\path\to\release.keystore"
$env:KEYSTORE_STORE_PW = "your_store_password"
$env:KEY_ALIAS = "your_key_alias"
$env:KEY_PASSWORD = "your_key_password"

.\gradlew.bat assembleRelease
```

#### Linux/Mac
```bash
export KEYSTORE_PATH="/path/to/release.keystore"
export KEYSTORE_STORE_PW="your_store_password"
export KEY_ALIAS="your_key_alias"
export KEY_PASSWORD="your_key_password"

./gradlew assembleRelease
```

#### CI/CD (GitHub Actions 등)
```yaml
env:
  KEYSTORE_PATH: ${{ secrets.KEYSTORE_PATH }}
  KEYSTORE_STORE_PW: ${{ secrets.KEYSTORE_STORE_PW }}
  KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
  KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
```

---

## 📝 완료된 작업

- [x] 문제 원인 분석
- [x] Fallback 로직 추가
- [x] Release 빌드 테스트 (debug keystore)
- [x] Release 빌드 설치 확인
- [x] 문서 작성

**수정 파일**:
- `app/build.gradle.kts`

---

**작성일**: 2025-11-08  
**상태**: ✅ 해결 완료

이제 환경변수 없이도 Release 빌드를 로컬에서 테스트할 수 있습니다! 🎉  
(실제 Play Store 배포 시에는 환경변수 설정 필요)
