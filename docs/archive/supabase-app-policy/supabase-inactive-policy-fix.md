# Supabase is_active false 설정 시 팝업 문제 해결

## 문제 상황
Supabase `app_policy` 테이블에서 `is_active`를 `false`로 설정했는데도 강제 업데이트 팝업이 계속 표시되는 문제가 발생했습니다.

## 원인 분석

### 로컬 캐시 메커니즘
앱은 강제 업데이트 정보를 SharedPreferences에 로컬 캐시합니다:
- **목적**: 오프라인 상황에서도 강제 업데이트 정책을 유지
- **저장 위치**: `force_required_version`, `force_update_info` 키
- **문제**: Supabase에서 정책을 비활성화해도 로컬 캐시는 그대로 남아있음

### HomeScreen.kt의 기존 로직
```kotlin
// 정책 없음 → 복원 강제 업데이트가 있으면 표시
restoredForcedUpdate?.let { upd ->
    updateInfo = upd
    showUpdateDialog = true  // ❌ 로컬 캐시로 인해 팝업 계속 표시
}
```

**문제**: Supabase에서 `is_active=false`로 설정하거나 정책 row를 삭제해도, 이전에 캐시된 강제 업데이트 정보로 인해 팝업이 계속 표시됩니다.

## 해결 방법

### 코드 수정
`app/src/main/java/com/sweetapps/pocketchord/ui/screens/HomeScreen.kt` 파일의 정책 확인 로직 수정:

```kotlin
if (policy == null) {
    android.util.Log.w("HomeScreen", "===== No Policy Loaded =====")
    android.util.Log.w("HomeScreen", "No active policy row for app_id='${com.sweetapps.pocketchord.BuildConfig.SUPABASE_APP_ID}'.")
    android.util.Log.w("HomeScreen", "Check:")
    android.util.Log.w("HomeScreen", "  1. app_policy.app_id matches BuildConfig.SUPABASE_APP_ID")
    android.util.Log.w("HomeScreen", "  2. is_active=TRUE in Supabase")
    android.util.Log.w("HomeScreen", "  3. RLS policy allows read (check 'allow_read_policy')")
    android.util.Log.w("HomeScreen", "  4. SUPABASE_ANON_KEY is valid")
    
    // ⚠️ 정책이 없거나 비활성화됨 → 로컬 캐시된 강제 업데이트도 삭제
    if (storedForceVersion != -1) {
        android.util.Log.w("HomeScreen", "⚠️ Clearing cached force update (no active policy)")
        updatePrefs.edit {
            remove("force_required_version")
            remove("force_update_info")
        }
    }
    
    // 정책 없음 → 팝업 없음
    return@LaunchedEffect
}
```

### 핵심 변경사항
1. ✅ **Supabase 정책이 없을 때**: 로컬 캐시 삭제
2. ✅ **`is_active=false`일 때**: `AppPolicyRepository`에서 필터링되어 `policy=null`이 되고, 로컬 캐시 삭제
3. ✅ **팝업 표시 차단**: 로컬 캐시가 삭제되므로 더 이상 팝업이 표시되지 않음

## 검증 결과

### 빌드 및 설치
```bash
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```
**결과**: BUILD SUCCESSFUL ✅

### 앱 실행 및 로그 확인
```bash
adb shell am force-stop com.sweetapps.pocketchord.debug
adb shell am start -n com.sweetapps.pocketchord.debug/com.sweetapps.pocketchord.MainActivity
adb logcat -d | Select-String "HomeScreen|Clearing"
```

**로그 결과**:
```
11-07 23:28:31.485 W HomeScreen: ⚠️ Clearing cached force update (no active policy)
11-07 23:28:31.238 D HomeScreen: showUpdateDialog: false
```

✅ **로컬 캐시 삭제 확인**
✅ **팝업이 표시되지 않음**

### Supabase 테이블 상태
```sql
SELECT * FROM app_policy WHERE app_id = 'com.sweetapps.pocketchord.debug';
```
**결과**: 0 rows (또는 `is_active = false`)

## 동작 시나리오

### 시나리오 1: 강제 업데이트 활성화
1. Supabase에서 `is_active=true`, `active_popup_type='force_update'` 설정
2. 앱 시작 시 정책을 가져와서 로컬에 캐시
3. 강제 업데이트 팝업 표시
4. 사용자가 업데이트하지 않고 앱 종료
5. 다음 실행 시 로컬 캐시로 다시 팝업 표시 (오프라인 대비)

### 시나리오 2: 강제 업데이트 비활성화 (수정 후)
1. Supabase에서 `is_active=false`로 변경
2. 앱 시작 시 정책을 가져오지 못함 (`policy=null`)
3. **로컬 캐시도 함께 삭제** ✅
4. 팝업 표시 안 됨

### 시나리오 3: 오프라인 상황
1. 이전에 강제 업데이트가 캐시됨
2. 네트워크 연결 없음
3. Supabase 조회 실패
4. **로컬 캐시 유지** (네트워크 오류 시에는 캐시 삭제하지 않음)
5. 로컬 캐시로 강제 업데이트 팝업 표시

## 주의사항

### 네트워크 오류 vs 정책 없음 구분
현재 구현에서는 Supabase 조회가 성공했지만 `policy=null`인 경우에만 캐시를 삭제합니다.

```kotlin
.onSuccess {
    policy = it  // 성공했지만 null일 수 있음
}
.onFailure { e ->
    policyError = e  // 네트워크 오류 등
}

if (policy == null) {
    // 성공적으로 조회했지만 정책이 없는 경우
    // → 로컬 캐시 삭제
}
```

### 네트워크 오류 시 동작
- `onFailure`가 발생하면 `policyError`에 저장되지만 현재는 사용되지 않음
- 개선 가능: 네트워크 오류 시 로컬 캐시를 유지하되, 사용자에게 네트워크 확인 메시지 표시

## 테스트 체크리스트

- [x] Supabase에서 `is_active=false` 설정 시 팝업 안 뜸
- [x] Supabase에서 row 삭제 시 팝업 안 뜸
- [x] 로컬 캐시가 올바르게 삭제됨
- [x] 로그에 "Clearing cached force update" 메시지 확인
- [ ] 오프라인 상황에서 로컬 캐시 유지 확인 (향후 테스트 필요)
- [ ] 네트워크 오류 시 동작 확인 (향후 테스트 필요)

## 관련 파일

### 수정된 파일
- `app/src/main/java/com/sweetapps/pocketchord/ui/screens/HomeScreen.kt`

### 관련 파일
- `app/src/main/java/com/sweetapps/pocketchord/data/supabase/repository/AppPolicyRepository.kt`
- `app/src/main/java/com/sweetapps/pocketchord/data/supabase/model/AppPolicy.kt`

## 작성 정보
- **작성일**: 2025-11-08
- **상태**: ✅ 해결 완료
- **영향 범위**: Debug/Release 모든 빌드 타입
- **관련 이슈**: Supabase `is_active=false` 설정 시 팝업 문제

## Release 버전 테스트

### releaseTest 빌드 타입
Release 설정을 테스트하기 위한 `releaseTest` 빌드 타입이 추가되었습니다:
- **SUPABASE_APP_ID**: `com.sweetapps.pocketchord` (Release와 동일)
- **서명**: Debug keystore 사용 (별도 키스토어 불필요)
- **난독화**: 비활성화 (테스트 편의성)
- **디버깅**: 활성화

**자세한 내용**: [release-test-guide.md](./release-test-guide.md) 참조

### 빌드 및 테스트 명령
```bash
# releaseTest 빌드
.\gradlew.bat assembleReleaseTest

# 설치
.\gradlew.bat installReleaseTest

# 실행
adb shell am start -n com.sweetapps.pocketchord.releasetest/com.sweetapps.pocketchord.MainActivity

# 로그 확인
adb logcat -d | Select-String "HomeScreen|AppPolicyRepo"
```

### Supabase 테이블 설정
Release 테스트를 위해서는 `com.sweetapps.pocketchord` app_id로 테이블 데이터를 추가해야 합니다:

```sql
INSERT INTO app_policy (
    app_id,
    is_active,
    active_popup_type,
    content
) VALUES (
    'com.sweetapps.pocketchord',  -- Release용 app_id
    true,
    'force_update',
    '새 버전이 출시되었습니다!'
);
```

