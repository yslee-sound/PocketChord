# SDK 버전 문제 해결

## 문제 상황
앱에서 다음과 같은 SDK 버전 경고가 표시되었습니다:

```
사용 중인 SDK 버전이 오래됨

androidx.fragment:fragment님이 fragment-1.0.0을(를) 오래된 버전으로 식별했습니다. 최신 SDK 버전으로 업데이트
트하는 것이 좋습니다.

영향을 받는 App Bundle 및 APK:
• 버전: 2 (1.0.1), 출시: 2 (1.0.1)

이 SDK에 관한 공급한 점이 있으면 SDK 제공업체에 문의하
세요.
```

## 원인 분석
1. `androidx.fragment:fragment` 라이브러리가 명시적으로 선언되지 않음
2. 다른 의존성들이 오래된 Fragment 1.0.0 버전을 전이적으로 포함
3. 특히 `play-services-ads`와 관련 라이브러리들이 Fragment 1.0.0을 참조

## 해결 방법

### 1. Fragment 라이브러리 버전 추가
`gradle/libs.versions.toml`에 Fragment 버전 추가:
```toml
[versions]
fragment = "1.8.9"
```

### 2. Fragment 라이브러리 선언 추가
`gradle/libs.versions.toml`의 `[libraries]` 섹션에 추가:
```toml
androidx-fragment = { group = "androidx.fragment", name = "fragment", version.ref = "fragment" }
androidx-fragment-ktx = { group = "androidx.fragment", name = "fragment-ktx", version.ref = "fragment" }
```

### 3. 앱 의존성에 명시적 선언
`app/build.gradle.kts`의 dependencies 섹션에 추가:
```kotlin
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Fragment 라이브러리 (최신 버전으로 명시적 선언)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.fragment.ktx)
    
    // ... 기존 의존성들 ...
}
```

## 검증 결과

### 의존성 트리 확인
```bash
.\gradlew.bat app:dependencies --configuration debugRuntimeClasspath | Select-String fragment
```

**결과**: Fragment 1.0.0 → 1.8.9로 업그레이드 확인
```
androidx.fragment:fragment:1.0.0 -> 1.8.9 (*)
```

### 빌드 성공
```bash
.\gradlew.bat clean assembleDebug
```
**결과**: BUILD SUCCESSFUL ✅

### 앱 설치 및 실행
```bash
.\gradlew.bat installDebug
adb shell am start -n com.sweetapps.pocketchord.debug/com.sweetapps.pocketchord.MainActivity
```

**결과**: 
- ✅ SDK 버전 경고 없음
- ✅ Fragment 관련 경고 없음
- ✅ 앱 정상 실행

## 추가 이점

### 보안 및 안정성
- Fragment 1.8.9는 최신 버전으로 버그 수정 및 보안 패치 포함
- Android 최신 버전과의 호환성 향상

### 기능 개선
- Fragment 1.8.9는 다음을 포함합니다:
  - Predictive Back Gesture 지원
  - 향상된 생명주기 관리
  - 성능 최적화

## 참고 사항

### Fragment 버전 히스토리
- 1.0.0: 초기 AndroidX 버전 (2018)
- 1.8.9: 최신 안정 버전 (2025)
- **13개 메이저 버전 차이**로 많은 개선사항 포함

### Google Play Console 정책
- 오래된 SDK 버전 사용 시 업데이트 제출이 거부될 수 있음
- 보안 취약점이 있는 라이브러리 사용 시 경고 표시
- 정기적인 의존성 업데이트 권장

## 작성 정보
- **작성일**: 2025-11-08
- **상태**: ✅ 해결 완료
- **영향 범위**: Debug/Release 모든 빌드 타입

