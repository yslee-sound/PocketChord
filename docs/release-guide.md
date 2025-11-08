# 🚀 PocketChord Release 가이드

**업데이트**: 2025-11-08  
**목적**: Release 빌드 및 배포 가이드

---

## 📋 목차

1. [Release 서명 설정](#release-서명-설정)
2. [빌드 방법](#빌드-방법)
3. [검증](#검증)
4. [Play Store 업로드](#play-store-업로드)
5. [문제 해결](#문제-해결)

---

## Release 서명 설정

### 1. Keystore 준비

#### 이미 있는 경우
```
위치: G:\Workspace\PocketChord\app\release\
파일: pocketchord-release-key.jks
```

#### 새로 생성하는 경우
```bash
keytool -genkey -v -keystore pocketchord-release-key.jks \
  -alias pocketchord \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

**중요**: Keystore 비밀번호를 안전하게 보관하세요!

### 2. keystore.properties 설정

#### 파일 위치
```
G:\Workspace\PocketChord\keystore.properties
```

#### 내용
```properties
storeFile=app/release/pocketchord-release-key.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=pocketchord
keyPassword=YOUR_KEY_PASSWORD
```

**주의**: 이 파일은 `.gitignore`에 포함되어야 합니다!

### 3. build.gradle.kts 설정

#### app/build.gradle.kts
```kotlin
android {
    // Keystore 설정
    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### Fallback (keystore.properties 없을 때)

자동으로 debug 서명으로 전환됩니다:
```kotlin
signingConfig = signingConfigs.getByName(
    if (keystorePropertiesFile.exists()) "release" else "debug"
)
```

---

## 빌드 방법

### 1. Gradle을 이용한 빌드

#### Command Line
```bash
cd G:\Workspace\PocketChord
gradlew assembleRelease
```

#### 성공 메시지
```
BUILD SUCCESSFUL in 2m 30s
```

#### 생성된 APK 위치
```
G:\Workspace\PocketChord\app\release\app-release.apk
```

### 2. Android Studio에서 빌드

1. **Build** 메뉴 선택
2. **Generate Signed Bundle / APK** 클릭
3. **APK** 선택 → **Next**
4. Keystore 정보 입력
5. **Build Variants**: `release` 선택
6. **Finish**

### 3. 버전 관리

#### app/build.gradle.kts
```kotlin
android {
    defaultConfig {
        versionCode = 10  // Play Store에서 이전보다 커야 함
        versionName = "1.2.0"  // 사용자에게 표시되는 버전
    }
}
```

**중요**:
- `versionCode`는 반드시 이전 릴리즈보다 커야 함
- `versionName`은 Semantic Versioning 권장 (예: 1.2.0)

---

## 검증

### 1. APK 서명 확인

```bash
jarsigner -verify -verbose G:\Workspace\PocketChord\app\release\app-release.apk
```

**예상 출력**:
```
jar verified.
```

### 2. APK 내용 확인

```bash
aapt dump badging G:\Workspace\PocketChord\app\release\app-release.apk
```

확인 사항:
- `package: name='com.sweetapps.pocketchord'`
- `versionCode='10'`
- `versionName='1.2.0'`

### 3. 테스트 설치

```bash
adb install G:\Workspace\PocketChord\app\release\app-release.apk
```

**확인 사항**:
- ✅ 정상 설치
- ✅ 앱 실행
- ✅ 주요 기능 동작
- ✅ 광고 표시 (실제 광고 ID 사용)

### 4. ProGuard/R8 확인

Release 빌드는 코드 난독화/최적화가 적용됩니다:
- `isMinifyEnabled = true`
- `isShrinkResources = true`

**확인**:
- APK 크기 감소 (Debug 대비 30-50%)
- 모든 기능 정상 작동

---

## Play Store 업로드

### 1. Play Console 접속

https://play.google.com/console

### 2. 앱 선택

PocketChord 선택

### 3. Release 생성

#### 경로
```
Production → Create new release
```

#### 업로드
1. **Choose file** 클릭
2. `app-release.apk` 선택
3. Release notes 작성

#### Release Notes 예시
```
버전 1.2.0

새로운 기능:
- 광고 시스템 개선

개선 사항:
- 성능 최적화
- 버그 수정
```

### 4. 검토 및 배포

1. **Review release** 클릭
2. 모든 항목 확인
3. **Start rollout to Production** 클릭

### 5. 배포 완료

- 검토 시간: 보통 1-3일
- 단계별 출시 가능 (예: 10% → 50% → 100%)

---

## 문제 해결

### 빌드 실패: Keystore not found

**증상**:
```
Keystore file not found
```

**해결**:
1. `keystore.properties` 파일 확인
2. `storeFile` 경로 확인
3. Keystore 파일 존재 확인

### 빌드 실패: Wrong password

**증상**:
```
Keystore was tampered with, or password was incorrect
```

**해결**:
1. `keystore.properties`의 비밀번호 확인
2. 대소문자 구분 확인
3. 공백 없는지 확인

### APK 설치 실패

**증상**:
```
INSTALL_FAILED_UPDATE_INCOMPATIBLE
```

**해결**:
```bash
# 기존 앱 제거 후 재설치
adb uninstall com.sweetapps.pocketchord
adb install app-release.apk
```

### Play Store 업로드 거부

**증상**:
```
Version code has already been used
```

**해결**:
```kotlin
// versionCode 증가
versionCode = 11  // 이전 10 → 11로 증가
```

### ProGuard 문제로 크래시

**증상**:
- Release 빌드만 크래시
- ClassNotFoundException 등

**해결**:
```proguard
# proguard-rules.pro에 추가
-keep class com.sweetapps.pocketchord.** { *; }
-keep class kotlinx.serialization.** { *; }
```

---

## 체크리스트

### 빌드 전
- [ ] `versionCode` 증가
- [ ] `versionName` 업데이트
- [ ] `keystore.properties` 확인
- [ ] Keystore 파일 존재 확인
- [ ] `.gitignore`에 keystore 포함 확인

### 빌드 후
- [ ] APK 서명 검증
- [ ] 테스트 설치
- [ ] 주요 기능 테스트
- [ ] 광고 표시 확인 (실제 광고)
- [ ] 크기 확인 (적절한지)

### 업로드 전
- [ ] Release notes 작성
- [ ] 스크린샷 업데이트 (필요시)
- [ ] 앱 설명 업데이트 (필요시)

### 업로드 후
- [ ] Play Console에서 확인
- [ ] 검토 완료 대기
- [ ] 배포 확인
- [ ] 사용자 피드백 모니터링

---

## 참고 정보

### Keystore 백업

**중요**: Keystore를 잃어버리면 앱 업데이트 불가!

백업 위치:
1. 안전한 클라우드 (암호화된)
2. 외장 하드
3. 다른 팀원과 공유 (안전하게)

### 버전 네이밍

Semantic Versioning:
```
MAJOR.MINOR.PATCH
  1  .  2  .  0

MAJOR: 큰 변경 (하위 호환 안 됨)
MINOR: 기능 추가 (하위 호환 됨)
PATCH: 버그 수정
```

예시:
- 1.0.0 → 1.0.1 (버그 수정)
- 1.0.1 → 1.1.0 (기능 추가)
- 1.1.0 → 2.0.0 (큰 변경)

---

## 참고 문서

- `DEPLOYMENT-CHECKLIST.md` - 배포 체크리스트
- `app/build.gradle.kts` - 빌드 설정
- `app/proguard-rules.pro` - ProGuard 규칙

---

**작성일**: 2025-11-08  
**업데이트**: 최신

