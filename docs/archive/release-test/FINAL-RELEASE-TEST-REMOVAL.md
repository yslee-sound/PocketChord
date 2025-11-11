# ✅ releaseTest 제거 완료

## 🎉 작업 완료

**날짜**: 2025-11-08  
**작업**: releaseTest 빌드 타입 제거

---

## 📋 최종 상태

### 빌드 구조 (간소화)

**이전** (복잡):
```
📦 PocketChord
├─ debug         (개발용)
├─ releaseTest   (❌ 제거됨)
└─ release       (배포용)
```

**현재** (단순):
```
📦 PocketChord
├─ debug         (개발용)
└─ release       (배포용)
```

---

## ✅ 수정된 파일

### 1. `app/build.gradle.kts`
```kotlin
buildTypes {
    debug {
        buildConfigField("String", "SUPABASE_APP_ID", 
            "\"com.sweetapps.pocketchord.debug\"")
        applicationIdSuffix = ".debug"
        versionNameSuffix = "-DEBUG"
    }

    release {
        buildConfigField("String", "SUPABASE_APP_ID", 
            "\"com.sweetapps.pocketchord\"")
        signingConfig = signingConfigs.getByName("release")
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(...)
    }
}
```

**변경**: `create("releaseTest") { ... }` 블록 제거

---

### 2. `SettingsScreen.kt`
```kotlin
// 디버그 모드 버튼 (변경 없음)
if (BuildConfig.DEBUG) {
    SettingsItem(
        icon = Icons.Default.BugReport,
        title = "디버그 모드",
        ...
    )
}
```

**동작**:
- Debug: ✅ 버튼 표시
- Release: ❌ 버튼 숨김

---

## 📊 빌드 비교

| 항목 | Debug | Release |
|------|-------|---------|
| **Package ID** | `.debug` | 없음 |
| **SUPABASE_APP_ID** | `.debug` | 실제 |
| **디버깅** | ✅ 가능 | ❌ 불가 |
| **난독화** | ❌ 없음 | ✅ 적용 |
| **최적화** | ❌ 없음 | ✅ 적용 |
| **서명** | Debug keystore | Release keystore |
| **디버그 모드 버튼** | ✅ 표시 | ❌ 숨김 |

---

## 🎯 개발 워크플로우

### 1. 개발 중
```bash
# Build Variant: debug 선택
# 또는
.\gradlew.bat installDebug
```

**특징**:
- ✅ 빠른 빌드
- ✅ 디버깅 가능
- ✅ 디버그 모드 접근 가능
- ✅ Supabase `.debug` 사용 (실제 사용자 영향 없음)

### 2. 배포 준비
```bash
# Build Variant: release 선택
# 또는
.\gradlew.bat assembleRelease
```

**특징**:
- ✅ 난독화 적용
- ✅ 코드 최적화
- ✅ 디버그 모드 버튼 숨김
- ✅ Release keystore로 서명
- ✅ Play Store 업로드 가능

---

## 📝 제거 이유 (요약)

1. **복잡성**: `initWith(debug)` + 수작업 오버라이드
2. **비효율**: Release와 너무 다름 (난독화 없음, debug처럼 동작)
3. **비표준**: 대부분의 앱은 debug + release만 사용
4. **유지보수**: 관리 포인트 증가

---

## 🚀 앞으로

### 현재 방식 (권장)
```
debug  → 개발 및 테스트
release → 실제 배포
```

### 필요시 고려 (나중에)
```
Product Flavors:
- dev (개발 서버)
- staging (테스트 서버)
- production (실제 서버)

자동 조합:
- devDebug, devRelease
- stagingDebug, stagingRelease
- productionDebug, productionRelease
```

---

**작성일**: 2025-11-08  
**상태**: ✅ **완료 - Debug + Release 2개 빌드만 유지**

이제 프로젝트가 더 단순하고 표준적인 구조가 되었습니다! 🎉
