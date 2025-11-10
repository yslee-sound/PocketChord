# 앱 버전 표시 개선

## 📋 작업 내용

**날짜**: 2025-11-08  
**작업**: 설정 화면의 앱 버전에 빌드 타입 정보 추가

---

## ✅ 변경 사항

### 이전
```
앱 버전
1.0.1
```

### 현재
```
앱 버전
1.0.1.debug   (Debug 빌드)
1.0.1.release (Release 빌드)
```

---

## 🔧 구현 코드

### SettingsScreen.kt

```kotlin
@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    
    // 빌드 타입 정보 추가
    val buildType = if (BuildConfig.DEBUG) "debug" else "release"
    val appVersionWithBuildType = "${BuildConfig.VERSION_NAME}.$buildType"

    // ...existing code...

    // 앱 버전
    SettingsItem(
        icon = Icons.Default.Info,
        title = "앱 버전",
        subtitle = appVersionWithBuildType,  // ✅ 빌드 타입 포함
        showArrow = false,
        onClick = null
    )
}
```

---

## 📊 빌드별 표시

| 빌드 타입 | BuildConfig.DEBUG | 표시되는 버전 |
|-----------|-------------------|---------------|
| **debug** | `true` | `1.0.1.debug` |
| **release** | `false` | `1.0.1.release` |

---

## 🎯 목적

### 1. 빌드 구분 명확화
- 개발자가 어떤 빌드를 실행 중인지 즉시 확인
- QA 테스트 시 빌드 타입 확인 용이

### 2. 디버깅 편의성
- Debug 빌드로 테스트 중임을 한눈에 확인
- Release 빌드 배포 전 최종 확인

### 3. 사용자 지원
- 문의 접수 시 정확한 버전 정보 제공
- 버그 리포트에 빌드 타입 포함

---

## 🧪 테스트 방법

### Debug 빌드
```bash
.\gradlew.bat installDebug
```
**예상 결과**: 설정 > 앱 버전 = `1.0.1.debug`

### Release 빌드
```bash
.\gradlew.bat installRelease
```
**예상 결과**: 설정 > 앱 버전 = `1.0.1.release`

---

## 📱 화면 예시

### Debug 빌드
```
┌─────────────────────────┐
│ 설정                    │
├─────────────────────────┤
│ 📱 앱 버전              │
│    1.0.1.debug          │ ← ✅ debug 추가
└─────────────────────────┘
```

### Release 빌드
```
┌─────────────────────────┐
│ 설정                    │
├─────────────────────────┤
│ 📱 앱 버전              │
│    1.0.1.release        │ ← ✅ release 추가
└─────────────────────────┘
```

---

## 💡 추가 개선 아이디어 (선택사항)

### 1. 더 상세한 정보 표시
```kotlin
val buildInfo = if (BuildConfig.DEBUG) {
    "${BuildConfig.VERSION_NAME}.debug (${BuildConfig.VERSION_CODE})"
} else {
    "${BuildConfig.VERSION_NAME}.release"
}
```

**결과**: `1.0.1.debug (2)`

### 2. 빌드 날짜 추가
```kotlin
val buildDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    .format(Date(BuildConfig.BUILD_TIME))
val versionInfo = "${BuildConfig.VERSION_NAME}.$buildType ($buildDate)"
```

**결과**: `1.0.1.debug (2025-11-08)`

### 3. Git 커밋 해시 추가
```kotlin
// build.gradle.kts에서
buildConfigField("String", "GIT_HASH", "\"${getGitHash()}\"")

// SettingsScreen.kt에서
val versionInfo = "${BuildConfig.VERSION_NAME}.$buildType (${BuildConfig.GIT_HASH})"
```

**결과**: `1.0.1.debug (a1b2c3d)`

---

## 📝 완료된 작업

- [x] 빌드 타입 감지 로직 추가
- [x] 버전 문자열에 빌드 타입 결합
- [x] SettingsItem에 적용
- [x] Debug 빌드 테스트
- [x] 문서 작성

**수정 파일**:
- `app/src/main/java/com/sweetapps/pocketchord/ui/screens/SettingsScreen.kt`

---

**작성일**: 2025-11-08  
**상태**: ✅ 완료

이제 설정 화면에서 빌드 타입을 명확하게 확인할 수 있습니다! 🎉

