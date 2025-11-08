# 🔧 디버그 모드 버튼 구현

## 📋 구현 내용

**목적**: Release 빌드에서 "디버그 모드" 버튼 숨기기

**방법**: `BuildConfig.DEBUG`로 조건부 표시

---

## ✅ 구현 방법

### 적용된 조건

```kotlin
if (BuildConfig.DEBUG) {  // ✅ DEBUG 빌드에서만 표시
    SettingsItem(
        icon = Icons.Default.BugReport,
        title = "디버그 모드",
        subtitle = "광고/아이콘/업데이트 도구",
        showArrow = true,
        onClick = { navController.navigate("debug_settings") }
    )
}
```

---

## 📊 동작

| 빌드 타입 | BuildConfig.DEBUG | 디버그 모드 버튼 |
|-----------|-------------------|------------------|
| **debug** | `true` | ✅ **표시** |
| **release** | `false` | ❌ **숨김** |

---

## 📝 완료된 작업

- [x] `if (BuildConfig.DEBUG)` 조건 추가
- [x] Debug 빌드에서 표시
- [x] Release 빌드에서 숨김
- [x] releaseTest 빌드 타입 제거 (불필요)

**수정 파일**:
- `app/src/main/java/com/sweetapps/pocketchord/ui/screens/SettingsScreen.kt`
- `app/build.gradle.kts` (releaseTest 제거)

---

**작성일**: 2025-11-08  
**상태**: ✅ 구현 완료 (Debug + Release 2개 빌드만 유지)

