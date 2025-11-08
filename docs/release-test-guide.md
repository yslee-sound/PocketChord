# Release 버전 테스트 가이드

## 개요
Release 빌드와 동일한 Supabase 설정(`com.sweetapps.pocketchord`)을 사용하면서도 디버그 키스토어로 서명하여 테스트할 수 있는 `releaseTest` 빌드 타입이 추가되었습니다.

## 빌드 타입 비교

| 항목 | debug | releaseTest | release |
|------|-------|-------------|---------|
| **SUPABASE_APP_ID** | `com.sweetapps.pocketchord.debug` | `com.sweetapps.pocketchord` | `com.sweetapps.pocketchord` |
| **Application ID** | `com.sweetapps.pocketchord.debug` | `com.sweetapps.pocketchord.releasetest` | `com.sweetapps.pocketchord` |
| **서명** | Debug keystore | Debug keystore | Release keystore (필요) |
| **난독화** | ❌ | ❌ | ✅ |
| **디버깅** | ✅ | ✅ | ❌ |
| **용도** | 개발 및 디버그 테스트 | Release 설정 테스트 | 실제 배포용 |

## releaseTest 빌드 타입의 특징

### ✅ 장점
1. **Release 설정 테스트**: Release와 동일한 `SUPABASE_APP_ID` 사용
2. **별도 키스토어 불필요**: 디버그 키스토어로 서명
3. **디버깅 가능**: 로그 확인 및 디버거 사용 가능
4. **난독화 없음**: 코드 추적이 쉬움
5. **기존 앱과 공존**: Application ID가 달라서 동시 설치 가능

### ⚠️ 주의사항
1. **실제 배포 금지**: 디버그 키로 서명되어 Play Store 업로드 불가
2. **성능 차이**: 난독화가 없어서 실제 Release보다 APK 크기가 큼
3. **Application ID 차이**: `.releasetest` 접미사로 인해 패키지명이 다름

## Supabase 테이블 설정

### 1. app_policy 테이블에 Release용 데이터 추가

```sql
INSERT INTO app_policy (
    app_id,
    is_active,
    active_popup_type,
    content,
    download_url,
    min_supported_version,
    latest_version_code
) VALUES (
    'com.sweetapps.pocketchord',  -- Release용 app_id
    true,                          -- 활성화 여부
    'force_update',                -- 또는 'optional_update', 'emergency', 'notice', 'none'
    '새 버전이 출시되었습니다!',   -- 팝업 메시지
    'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord',  -- 다운로드 URL
    1,                             -- 최소 지원 버전 (이 버전 미만은 강제 업데이트)
    3                              -- 최신 버전 코드
);
```

### 2. 테스트를 위한 설정 예시

#### 강제 업데이트 테스트
```sql
UPDATE app_policy 
SET is_active = true,
    active_popup_type = 'force_update',
    min_supported_version = 3,  -- 현재 버전(2)보다 큼 → 강제 업데이트
    content = '필수 업데이트가 있습니다.\n앱을 최신 버전으로 업데이트해주세요.'
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 선택적 업데이트 테스트
```sql
UPDATE app_policy 
SET is_active = true,
    active_popup_type = 'optional_update',
    latest_version_code = 3,  -- 현재 버전(2)보다 큼
    content = '새로운 버전이 출시되었습니다.\n업데이트하시겠습니까?'
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 팝업 비활성화
```sql
UPDATE app_policy 
SET is_active = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

## 테스트 방법

### 1. Android Studio에서 Build Variant 변경
1. **View > Tool Windows > Build Variants** 메뉴 열기
2. **Active Build Variant**를 `releaseTest`로 변경
3. Gradle Sync 완료 대기

### 2. 빌드 및 설치

#### 방법 A: Android Studio에서 실행
```
Run > Run 'app' (Shift+F10)
```

#### 방법 B: Gradle 명령어 사용
```bash
# 빌드
.\gradlew.bat assembleReleaseTest

# 설치
.\gradlew.bat installReleaseTest

# 빌드 + 설치 + 실행
adb shell am force-stop com.sweetapps.pocketchord.releasetest
.\gradlew.bat installReleaseTest
adb shell am start -n com.sweetapps.pocketchord.releasetest/com.sweetapps.pocketchord.MainActivity
```

### 3. 로그 확인

#### releaseTest 앱의 로그만 필터링
```bash
adb logcat | Select-String "com.sweetapps.pocketchord.releasetest"
```

#### 정책 조회 로그 확인
```bash
adb logcat -d | Select-String -Pattern "AppPolicyRepo|HomeScreen" | Select-Object -First 50
```

### 4. Supabase APP_ID 확인
앱 실행 시 로그에서 다음을 확인:
```
D/HomeScreen: Startup: SUPABASE_APP_ID=com.sweetapps.pocketchord, VERSION_CODE=2
D/AppPolicyRepo: Target app_id: com.sweetapps.pocketchord
```

✅ **`com.sweetapps.pocketchord`가 출력되면 성공**

## 테스트 시나리오

### 시나리오 1: 강제 업데이트 활성화 → 비활성화
1. Supabase에서 `is_active=true`, `active_popup_type='force_update'` 설정
2. releaseTest 앱 실행 → 강제 업데이트 팝업 확인
3. 앱 종료 (업데이트 안 함)
4. 앱 재실행 → 로컬 캐시로 팝업 다시 표시 확인
5. Supabase에서 `is_active=false`로 변경
6. 앱 재실행 → 팝업 사라짐 + 로컬 캐시 삭제 확인

**예상 로그**:
```
W/HomeScreen: ⚠️ Clearing cached force update (no active policy)
D/HomeScreen: showUpdateDialog: false
```

### 시나리오 2: Debug vs ReleaseTest 동시 테스트
1. Debug 앱 실행 (app_id: `com.sweetapps.pocketchord.debug`)
   - Debug용 Supabase 정책 적용
2. ReleaseTest 앱 설치 및 실행 (app_id: `com.sweetapps.pocketchord`)
   - Release용 Supabase 정책 적용
3. 두 앱이 각각 다른 정책을 사용하는지 확인

### 시나리오 3: 로컬 캐시 동작 확인
1. 강제 업데이트 활성화 → 앱 실행 → 팝업 확인
2. 비행기 모드 활성화 (네트워크 차단)
3. 앱 재실행 → 로컬 캐시로 팝업 표시 확인
4. 비행기 모드 해제 + Supabase에서 `is_active=false` 설정
5. 앱 재실행 → 캐시 삭제 + 팝업 사라짐 확인

## 문제 해결

### "SUPABASE_APP_ID=com.sweetapps.pocketchord.debug"로 표시됨
- **원인**: Build Variant가 제대로 변경되지 않음
- **해결**: 
  1. Android Studio 재시작
  2. **File > Invalidate Caches / Restart**
  3. Build Variant 다시 확인

### "No policy found"가 계속 표시됨
- **원인**: Supabase 테이블에 Release용 데이터가 없음
- **해결**: 위의 SQL 쿼리로 데이터 추가

### 앱 설치 실패 (INSTALL_FAILED_CONFLICTING_PROVIDER)
- **원인**: Application ID가 충돌
- **해결**: 기존 앱 제거 후 재설치
  ```bash
  adb uninstall com.sweetapps.pocketchord.releasetest
  .\gradlew.bat installReleaseTest
  ```

## 실제 Release 빌드

실제 배포용 Release 빌드를 하려면:

### 1. 키스토어 환경변수 설정
```bash
# PowerShell
$env:KEYSTORE_PATH = "G:\path\to\your\keystore.jks"
$env:KEYSTORE_STORE_PW = "your_store_password"
$env:KEY_ALIAS = "your_key_alias"
$env:KEY_PASSWORD = "your_key_password"
```

### 2. Release 빌드 실행
```bash
.\gradlew.bat assembleRelease
```

### 3. APK 확인
```
app\build\outputs\apk\release\app-release.apk
```

## 참고 문서
- [release-build-guide.md](./release-build-guide.md) - Release 빌드 상세 가이드
- [supabase-inactive-policy-fix.md](./supabase-inactive-policy-fix.md) - 정책 비활성화 문제 해결
- [a_RELEASE_SIGNING.md](./a_RELEASE_SIGNING.md) - 릴리즈 서명 설정

## 작성 정보
- **작성일**: 2025-11-08
- **빌드 타입**: `releaseTest` 추가
- **용도**: Release 설정 테스트 (디버그 키스토어 사용)

