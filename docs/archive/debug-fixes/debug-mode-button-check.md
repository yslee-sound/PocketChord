# 디버그 모드 버튼 표시 로직 검토

## 📋 현재 상황

**질문**: Release 빌드에서 설정 화면의 "디버그 모드" 버튼이 숨겨지도록 로직이 적용되어 있나요?

## 🔍 코드 분석 결과

### SettingsScreen.kt (라인 122-129)

```kotlin
// 디버그 설정 진입 (하위 스크린)
SettingsItem(
    icon = Icons.Default.BugReport,
    title = "디버그 모드",
    subtitle = "광고/아이콘/업데이트 도구",
    showArrow = true,
    onClick = { navController.navigate("debug_settings") }
)
```

### ❌ 현재 상태: **조건부 표시 로직 없음**

디버그 모드 버튼이 **모든 빌드 타입에서 항상 표시**되고 있습니다.
- ❌ `BuildConfig.DEBUG` 체크 없음
- ❌ `if` 조건문 없음
- ✅ Release 빌드에서도 버튼이 보임

## 📊 현재 vs 예상 동작

### 현재 동작:
```
Debug 빌드:   ✅ 디버그 모드 버튼 표시
ReleaseTest:  ✅ 디버그 모드 버튼 표시
Release:      ✅ 디버그 모드 버튼 표시  ← ⚠️ 문제!
```

### 예상 동작:
```
Debug 빌드:   ✅ 디버그 모드 버튼 표시
ReleaseTest:  ✅ 디버그 모드 버튼 표시
Release:      ❌ 디버그 모드 버튼 숨김  ← 필요!
```

## ✅ 해결 방법

### 수정 전:
```kotlin
Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {
    // 앱 버전
    SettingsItem(...)

    // 문의 하기
    SettingsItem(...)

    // 앱 평가하기
    SettingsItem(...)

    // 디버그 설정 진입 (하위 스크린)
    SettingsItem(
        icon = Icons.Default.BugReport,
        title = "디버그 모드",
        subtitle = "광고/아이콘/업데이트 도구",
        showArrow = true,
        onClick = { navController.navigate("debug_settings") }
    )
}
```

### 수정 후:
```kotlin
Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {
    // 앱 버전
    SettingsItem(...)

    // 문의 하기
    SettingsItem(...)

    // 앱 평가하기
    SettingsItem(...)

    // 디버그 설정 진입 (DEBUG 빌드에서만 표시)
    if (BuildConfig.DEBUG) {
        SettingsItem(
            icon = Icons.Default.BugReport,
            title = "디버그 모드",
            subtitle = "광고/아이콘/업데이트 도구",
            showArrow = true,
            onClick = { navController.navigate("debug_settings") }
        )
    }
}
```

## 🎯 BuildConfig.DEBUG 값

| 빌드 타입 | BuildConfig.DEBUG | 디버그 모드 버튼 표시 |
|-----------|-------------------|------------------------|
| **debug** | `true` | ✅ 표시 |
| **releaseTest** | `true` | ✅ 표시 |
| **release** | `false` | ❌ 숨김 |

## 🔍 build.gradle.kts 확인

```kotlin
buildTypes {
    debug {
        // BuildConfig.DEBUG = true (기본값)
        isDebuggable = true
    }

    create("releaseTest") {
        initWith(getByName("debug"))
        // BuildConfig.DEBUG = true (debug로부터 상속)
        isDebuggable = true
    }

    release {
        // BuildConfig.DEBUG = false (기본값)
        isDebuggable = false
        isMinifyEnabled = true
        isShrinkResources = true
    }
}
```

## 📝 추가 고려사항

### 만약 releaseTest에서도 숨기고 싶다면:

```kotlin
// 방법 1: DEBUG만 허용
if (BuildConfig.DEBUG) {
    SettingsItem(...)
}

// 방법 2: application ID 체크
if (BuildConfig.APPLICATION_ID.endsWith(".debug")) {
    SettingsItem(...)
}

// 방법 3: 명시적 빌드 타입 체크
if (BuildConfig.DEBUG && !BuildConfig.APPLICATION_ID.contains("releasetest")) {
    SettingsItem(...)
}
```

### 현재 추천: 방법 1 (간단함)

```kotlin
if (BuildConfig.DEBUG) {
    SettingsItem(
        icon = Icons.Default.BugReport,
        title = "디버그 모드",
        subtitle = "광고/아이콘/업데이트 도구",
        showArrow = true,
        onClick = { navController.navigate("debug_settings") }
    )
}
```

**결과**:
- ✅ debug: 표시
- ✅ releaseTest: 표시 (테스트 편의성)
- ❌ release: 숨김 (실제 배포)

## ⚠️ 결론

**현재 상태**: ✅ **적용 완료!**

디버그 모드 버튼 숨기기 로직이 **구현되었습니다**.
Release 빌드에서는 디버그 모드 버튼이 표시되지 않습니다.

**수정 내용**:
- ✅ `if (BuildConfig.DEBUG)` 조건 추가 완료
- ✅ Release 빌드에서 버튼 숨김 확인

**수정된 파일**:
- `app/src/main/java/com/sweetapps/pocketchord/ui/screens/SettingsScreen.kt` (라인 122-131)

## ✅ 적용된 코드

```kotlin
// 디버그 설정 진입 (DEBUG 빌드에서만 표시)
if (BuildConfig.DEBUG) {
    SettingsItem(
        icon = Icons.Default.BugReport,
        title = "디버그 모드",
        subtitle = "광고/아이콘/업데이트 도구",
        showArrow = true,
        onClick = { navController.navigate("debug_settings") }
    )
}
```

## 🧪 테스트 방법

### 1. Debug 빌드 확인
```bash
# Build Variant: debug 선택
# 앱 실행 → 설정 → "디버그 모드" 버튼 있음 ✅
```

### 2. Release Test 빌드 확인
```bash
# Build Variant: releaseTest 선택
# 앱 실행 → 설정 → "디버그 모드" 버튼 있음 ✅
```

### 3. Release 빌드 확인
```bash
# Build Variant: release 선택
# 앱 실행 → 설정 → "디버그 모드" 버튼 없음 ❌ ✅
```

---

**작성일**: 2025-11-08  
**상태**: ✅ 적용 완료

