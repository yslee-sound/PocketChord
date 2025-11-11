# Release 테스트 빠른 시작 가이드

## ✅ 완료된 작업
1. **`releaseTest` 빌드 타입 추가**: Release 설정을 디버그 키스토어로 테스트 가능
2. **빌드 및 설치 성공**: APK 생성 및 에뮬레이터 설치 완료

## ⚠️ 중요: Build Variant 전환 필수!

현재 **debug** 빌드가 선택되어 있습니다. Release 테스트를 하려면:

### Build Variant 변경 방법
1. Android Studio 하단의 **Build Variants** 창 열기
2. **Active Build Variant** 드롭다운 클릭
3. **`releaseTest`** 선택
4. Gradle Sync 완료 대기
5. ▶ 실행 버튼으로 앱 실행

**또는** 명령어:
```bash
.\gradlew.bat installReleaseTest
adb shell am start -n com.sweetapps.pocketchord.releasetest/com.sweetapps.pocketchord.MainActivity
```

## 🎯 다음 단계

### 1. Supabase에 Release Test용 데이터 설정

**중요**: Release Test는 **자체 app_id**를 사용하여 실제 사용자에게 영향을 주지 않습니다!

```sql
-- Release Test 전용 row 추가 (실제 사용자에게 영향 없음!)
INSERT INTO app_policy (
    app_id,
    is_active,
    active_popup_type,
    content,
    download_url,
    min_supported_version,
    latest_version_code
) VALUES (
    'com.sweetapps.pocketchord.releasetest',  -- ✅ Release Test 전용!
    true,
    'force_update',
    '[테스트] 필수 업데이트가 있습니다.\n앱을 최신 버전으로 업데이트해주세요.',
    'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord',
    3,  -- 현재 버전(2)보다 큼 → 강제 업데이트 발생
    3
)
ON CONFLICT (app_id) 
DO UPDATE SET
    is_active = EXCLUDED.is_active,
    active_popup_type = EXCLUDED.active_popup_type,
    content = EXCLUDED.content,
    download_url = EXCLUDED.download_url,
    min_supported_version = EXCLUDED.min_supported_version,
    latest_version_code = EXCLUDED.latest_version_code;
```

**Release용 정책은 별도로 관리:**
```sql
-- Release용 (실제 사용자용) - 필요할 때만 활성화
INSERT INTO app_policy (
    app_id,
    is_active,
    active_popup_type,
    content,
    download_url,
    min_supported_version,
    latest_version_code
) VALUES (
    'com.sweetapps.pocketchord',  -- ✅ Release 전용!
    false,  -- ⚠️ 테스트 중에는 비활성화!
    'force_update',
    '필수 업데이트가 있습니다.\n앱을 최신 버전으로 업데이트해주세요.',
    'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord',
    3,
    3
)
ON CONFLICT (app_id) 
DO UPDATE SET
    is_active = EXCLUDED.is_active,
    active_popup_type = EXCLUDED.active_popup_type,
    content = EXCLUDED.content,
    download_url = EXCLUDED.download_url,
    min_supported_version = EXCLUDED.min_supported_version,
    latest_version_code = EXCLUDED.latest_version_code;
```

### 2. Android Studio에서 Build Variant 변경

#### 방법 A: Build Variants 창 사용 (권장)
1. **View > Tool Windows > Build Variants** 메뉴 열기
2. **Active Build Variant** 를 `releaseTest`로 변경
3. Gradle Sync 완료 대기 (자동 실행)
4. ▶ 실행 버튼으로 앱 실행

#### 방법 B: Gradle 명령어 사용
```bash
# 1. 빌드
.\gradlew.bat assembleReleaseTest

# 2. 설치
.\gradlew.bat installReleaseTest

# 3. 실행
adb shell am start -n com.sweetapps.pocketchord.releasetest/com.sweetapps.pocketchord.MainActivity
```

### 3. 로그로 SUPABASE_APP_ID 확인

앱 실행 후 로그 확인:
```bash
adb logcat -d | Select-String "SUPABASE_APP_ID|Target app_id"
```

**예상 로그**:
```
D/HomeScreen: Startup: SUPABASE_APP_ID=com.sweetapps.pocketchord.releasetest, VERSION_CODE=2
D/AppPolicyRepo: Target app_id: com.sweetapps.pocketchord.releasetest
```

✅ `com.sweetapps.pocketchord.releasetest`가 출력되면 Release Test 설정 적용 성공!

### 4. 테스트 시나리오 실행

#### 시나리오 1: 강제 업데이트 팝업 확인
1. Supabase에서 `is_active=true`, `active_popup_type='force_update'` 설정
2. releaseTest 앱 실행
3. **강제 업데이트 팝업**이 표시되는지 확인

#### 시나리오 2: 팝업 비활성화
1. Supabase에서 `is_active=false`로 변경
2. releaseTest 앱 재실행
3. **팝업이 사라지는지** 확인
4. 로그에서 "Clearing cached force update" 메시지 확인

```bash
adb logcat -d | Select-String "Clearing cached"
```

**예상 로그**:
```
W/HomeScreen: ⚠️ Clearing cached force update (no active policy)
```

## 📊 현재 상태

| 항목 | 상태 |
|------|------|
| releaseTest 빌드 타입 추가 | ✅ 완료 |
| 빌드 성공 | ✅ 완료 |
| 에뮬레이터 설치 | ✅ 완료 |
| Supabase Release 데이터 | ✅ **이미 존재** (UPDATE 필요) |
| 실제 테스트 | 🔄 대기 중 |

## 🔍 디버그/릴리즈 비교

### 핵심 개념: Application ID vs SUPABASE_APP_ID

#### Application ID (패키지명)
- **Play Store와 디바이스에서 앱을 구분하는 고유 ID**
- `.debug`, `.releasetest` suffix로 **완전히 다른 앱**으로 인식
- 동시 설치 가능 (충돌 없음)

#### SUPABASE_APP_ID
- **Supabase 테이블에서 정책을 조회할 때 사용하는 필터링 키**
- 어떤 정책 데이터를 가져올지 결정

### 빌드 타입 비교표

| 빌드 타입 | Application ID | SUPABASE_APP_ID | 실제 사용자 영향 | 용도 |
|-----------|----------------|-----------------|-----------------|------|
| **debug** | `com.sweetapps.pocketchord.debug` | `com.sweetapps.pocketchord.debug` | ❌ 없음 | 개발 및 디버그 |
| **releaseTest** | `com.sweetapps.pocketchord.releasetest` | `com.sweetapps.pocketchord.releasetest` | ❌ 없음 | Release 설정 테스트 |
| **release** | `com.sweetapps.pocketchord` | `com.sweetapps.pocketchord` | ✅ **있음** | 실제 배포 |

### 🎯 Release Test의 목적

**"Release 환경 설정으로 안전하게 테스트하기"**

#### 핵심 개념

Release Test는 **자체 SUPABASE_APP_ID**를 사용합니다:
- ✅ Release와 **동일한 빌드 설정** (난독화 제외)
- ✅ 하지만 **다른 Supabase 정책** 사용
- ✅ 실제 사용자에게 **완전히 영향 없음**

#### 왜 필요한가?

**Release 환경을 안전하게 테스트**하기 위함:

```
┌──────────────────────────────────┐
│ Supabase 테이블                   │
├──────────────────────────────────┤
│ 1. app_id: ...releasetest        │ ← Release Test용 (테스트 전용)
│    is_active: true                │
│                                   │
│ 2. app_id: ...pocketchord         │ ← Release용 (실제 사용자)
│    is_active: false ← 비활성화!  │
└──────────────────────────────────┘
```

#### 실전 활용

```
1. Supabase에 Release Test용 정책 추가
   app_id = 'com.sweetapps.pocketchord.releasetest'
   is_active = true
   
2. Release용 정책은 비활성화
   app_id = 'com.sweetapps.pocketchord'
   is_active = false  ← 실제 사용자 영향 없음!

3. Release Test 앱으로 테스트 (개발자만 가능)
   ✅ 팝업 확인
   ✅ Release 설정 확인
   ✅ 디버깅 가능
   
4. 테스트 완료 후 → Release 정책 활성화
   → 이때 일반 사용자에게 팝업 표시
```

### 🛡️ 안전 장치

Release Test가 실제 사용자에게 영향 없는 이유:
1. **Application ID가 다름** → 완전히 다른 앱
2. **Play Store에 없음** → 개발자만 설치 가능
3. **디버그 키스토어** → Play Store 업로드 불가

---

## 🎨 Build Variants란?

### Android Studio의 핵심 기능

**"하나의 코드로 여러 버전의 앱을 만드는 시스템"**

### 왜 필요한가?

#### 1. 개발 단계별 다른 설정
- Debug: 빠른 빌드, 로그 출력, 디버깅
- Release Test: Release 설정 + 디버깅
- Release: 최적화, 난독화, 실제 배포

#### 2. 동시 설치 가능
```bash
# 3개 앱을 동시에 설치하여 비교 테스트
com.sweetapps.pocketchord.debug
com.sweetapps.pocketchord.releasetest
com.sweetapps.pocketchord
```

#### 3. 빠른 전환
- 코드 변경 없이 **클릭 한 번**으로 빌드 타입 전환
- 각 환경 설정 자동 적용

현재 3개의 빌드 타입이 있습니다:

| 빌드 타입 | Application ID | SUPABASE_APP_ID | 용도 |
|-----------|----------------|-----------------|------|
| **debug** | `com.sweetapps.pocketchord.debug` | `com.sweetapps.pocketchord.debug` | 개발 및 디버그 |
| **releaseTest** | `com.sweetapps.pocketchord.releasetest` | `com.sweetapps.pocketchord.releasetest` | Release 설정 테스트 |
| **release** | `com.sweetapps.pocketchord` | `com.sweetapps.pocketchord` | 실제 배포 |

### 동시 설치 가능
Application ID가 다르므로 debug와 releaseTest를 동시에 설치하여 비교 테스트 가능합니다:

```bash
# Debug 앱 실행
adb shell am start -n com.sweetapps.pocketchord.debug/com.sweetapps.pocketchord.MainActivity

# ReleaseTest 앱 실행
adb shell am start -n com.sweetapps.pocketchord.releasetest/com.sweetapps.pocketchord.MainActivity
```

## ❓ 문제 해결

### "No policy found" 로그가 계속 나옴
- **원인**: Supabase에 `com.sweetapps.pocketchord` 데이터가 없음
- **해결**: 위의 SQL 쿼리 실행

### Build Variant 변경이 안 됨
- **원인**: Gradle Sync 필요
- **해결**: **File > Sync Project with Gradle Files**

### 앱이 실행되지 않음
- **원인**: 패키지명 오류 또는 설치 실패
- **해결**:
  ```bash
  # 기존 앱 제거
  adb uninstall com.sweetapps.pocketchord.releasetest
  
  # 재설치
  .\gradlew.bat installReleaseTest
  ```

---

**작성일**: 2025-11-08  
**상태**: 🔄 **코드 수정 완료! Supabase 테이블 설정 후 테스트 필요**

## 📊 테스트 결과 로그

### ✅ Policy 조회 성공
```
D/AppPolicyRepo: ✅ Policy found:
D/AppPolicyRepo:   - id: 2
D/AppPolicyRepo:   - app_id: com.sweetapps.pocketchord.releasetest
D/AppPolicyRepo:   - is_active: true
D<AppPolicyRepo:   - active_popup_type: force_update
D/AppPolicyRepo:   - content: [테스트] 필수 업데이트가 있습니다...
```

### ✅ 강제 업데이트 팝업 표시
```
D/HomeScreen: Decision: FORCE UPDATE popup (minSupported=3)
D/HomeScreen: showUpdateDialog: true  ✅
```

### ✅ SUPABASE_APP_ID 확인
```
D/HomeScreen: Startup: SUPABASE_APP_ID=com.sweetapps.pocketchord.releasetest, VERSION_CODE=2
D/AppPolicyRepo: Target app_id: com.sweetapps.pocketchord.releasetest
```

---

## ❓ FAQ (자주 묻는 질문)

### Q1. Release Test를 어떻게 실행하나요?

**A:** Build Variants 창에서 `releaseTest`를 선택하고 실행하거나, 명령어로 실행합니다:
```bash
.\gradlew.bat installReleaseTest
adb shell am start -n com.sweetapps.pocketchord.releasetest/com.sweetapps.pocketchord.MainActivity
```

### Q2. Supabase에 어떤 app_id로 데이터를 만들어야 하나요?

**A:** Release Test용과 Release용을 **별도로** 만들어야 합니다!

```sql
-- Release Test용 (테스트 전용)
app_id = 'com.sweetapps.pocketchord.releasetest'
is_active = true  ← 테스트 중에 활성화

-- Release용 (실제 사용자)
app_id = 'com.sweetapps.pocketchord'
is_active = false  ← 테스트 중에는 비활성화!
```

**핵심**: 
- ✅ 각 빌드 타입이 **자기만의 app_id** 사용
- ✅ 실제 사용자에게 **완전히 영향 없음**
- ✅ 안전하게 테스트 가능

### Q3. 왜 Release Test용 app_id를 따로 만들어야 하나요?

**A:** 실제 사용자에게 영향을 주지 않으면서 테스트하기 위해서입니다!

```
Supabase 테이블:
┌────────────────────────────────────┐
│ app_id: ...releasetest             │ ← Release Test용
│ is_active: true                    │   테스트 중!
└────────────────────────────────────┘

┌────────────────────────────────────┐
│ app_id: ...pocketchord             │ ← Release용
│ is_active: false                   │   사용자 영향 없음!
└────────────────────────────────────┘
```

**장점**:
- ✅ 테스트 중에도 실제 Release 앱 정상 작동
- ✅ 실수로 사용자에게 테스트 팝업 표시 방지
- ✅ 테스트용/실제용 정책 명확히 분리

### Q4. Build Variants는 왜 있나요?

**A:** 하나의 코드로 여러 버전(Debug, Release Test, Release)을 만들기 위한 Android Studio의 핵심 기능입니다. 클릭 한 번으로 개발/테스트/배포 환경을 전환할 수 있습니다.

### Q5. Release Test와 Release의 차이점은?

| 항목 | Release Test | Release |
|------|--------------|---------|
| **Application ID** | `.releasetest` suffix | suffix 없음 |
| **SUPABASE_APP_ID** | `.releasetest` suffix | suffix 없음 |
| **디버깅** | ✅ 가능 | ❌ 불가 |
| **난독화** | ❌ 비활성화 | ✅ 활성화 |
| **서명** | Debug keystore | Release keystore 필요 |
| **Play Store 업로드** | ❌ 불가 | ✅ 가능 |
| **실제 사용자 영향** | ❌ 없음 | ✅ 있음 |

### Q6. Debug와 Release Test를 동시에 설치할 수 있나요?

**A:** 네! Application ID가 다르므로 **3개 모두 동시 설치 가능**합니다:
```bash
# 설치된 앱 확인
adb shell pm list packages | grep pocketchord

# 결과:
package:com.sweetapps.pocketchord.debug
package:com.sweetapps.pocketchord.releasetest
package:com.sweetapps.pocketchord
```

### Q7. Build Variant를 변경했는데 코드가 적용 안 되는 것 같아요

**A:** Gradle Sync가 완료될 때까지 기다려야 합니다:
1. Build Variant 변경
2. **Gradle Sync 자동 실행 (하단 진행바 확인)**
3. Sync 완료 후 앱 실행

또는 수동으로: **File > Sync Project with Gradle Files**
