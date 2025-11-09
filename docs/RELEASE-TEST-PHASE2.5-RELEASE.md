# 릴리즈 테스트 SQL 스크립트 - Phase 2.5 (선택적 업데이트 시간 기반 재표시)

**버전**: v3.1.2  
**최종 업데이트**: 2025-11-10 02:15:00 KST  
**대상 app_id**: `com.sweetapps.pocketchord` (릴리즈) / `com.sweetapps.pocketchord.debug` (디버그)

---
## 0. 목적

**Phase 2.5**: 선택적 업데이트에서 "나중에" 클릭 후 **시간 기반 재표시** 구현

### 핵심 변경
- DB 필드: 
  - `reshow_interval_hours` (재표시 간격 - 시간 단위)
  - `reshow_interval_minutes` (재표시 간격 - 분 단위, 테스트용)
  - `reshow_interval_seconds` (재표시 간격 - 초 단위, 초고속 테스트용, **최우선**)
  - `max_later_count` (최대 "나중에" 횟수)
- 우선순위: **seconds > minutes > hours** (가장 작은 단위가 우선)
- 추적: `update_dismissed_time`, `update_later_count` (SharedPreferences 저장)

### ⚠️ 운영 환경 주의사항
- **운영 환경(릴리즈)**: 반드시 `hours` 단위만 사용 (예: 24시간)
  - `reshow_interval_seconds` = NULL
  - `reshow_interval_minutes` = NULL
  - `reshow_interval_hours` = 24
- **테스트 환경(디버그)**: 빠른 테스트를 위해 초/분 단위 사용 가능 (예: 60초)

---
## 🚀 빠른 테스트 시작

**처음 테스트하는 경우:**
➡️ **[섹션 3. DB 스키마 변경 SQL](#3-db-스키마-변경-sql)로 이동하여 초기값 설정부터 시작하세요**

**이미 섹션 3의 DB 스키마 변경 및 초기값 설정을 완료했다면:**
➡️ **[섹션 4. 시나리오별 테스트](#4-시나리오별-테스트)로 바로 이동**

**테스트 순서:**
1. **S1**: DB 변경 확인만 (이미 완료했다면 스킵 가능)
2. **S2**: 첫 "나중에" 클릭 테스트 ⭐ (여기서부터 시작)
3. **S3**: 시간 경과 후 재표시 테스트
4. **S4**: 3회 "나중에" 후 강제 전환 테스트
5. **S5**: 업데이트 후 초기화 테스트
6. **S6**: 정책 변경 테스트


---
## 1. 테스트 시나리오 요약

| 시나리오 | 목적 | 기대 결과 |
|----------|------|-----------|
| S1 DB 변경 및 초기 설정 | 새 필드 추가 및 기본값 설정 | 정책 조회 시 새 필드 확인 |
| S2 첫 "나중에" 클릭 | 시간 추적 시작 | 팝업 닫힘, 재시작 시 미표시 (디버그: 1분 미경과 / 릴리즈: 24시간 미경과) |
| S3 시간 경과 후 재표시 | 지정 시간 경과 후 재표시 로직 | 팝업 재표시, count 증가 (디버그: 1분 후 / 릴리즈: 24시간 후) |
| S4 3회 "나중에" 후 강제 전환 | 최대 횟수 도달 시 강제 전환 | "나중에" 버튼 숨김, 뒤로가기 차단 |
| S5 업데이트 후 초기화 | 업데이트 완료 시 추적 초기화 | 새 버전에서 카운트 리셋 |
| S6 정책 변경 테스트 | 간격/횟수 조정 동작 확인 | 변경된 값으로 동작 |

---
## 2. Logcat 필터 & 예상 로그

### 🎯 Phase 2.5 전용 필터 (권장)

**Filter 설정**: `tag:UpdateLater`

**설명**: "나중에" 기능의 시간 추적, 카운트, 강제 전환 관련 로그만 표시합니다.

**주요 로그 패턴**:
```
UpdateLater: ⏱️ Update interval elapsed (>= 60s), reshow allowed
UpdateLater: 📊 Current later count: 1 / 3
UpdateLater: 🚨 Later count (3) >= max (3), forcing update mode
UpdateLater: ✋ Update dialog dismissed for code=10
UpdateLater: ⏱️ Tracking: laterCount=0→1, timestamp=1762705544280
UpdateLater: ⏸️ Update dialog skipped (dismissed version: 10, target: 10)
UpdateLater: 🧹 Clearing old update tracking data (version updated)
```

### 전체 업데이트 로직 확인 필터 (상세)

**Filter 설정**: `tag:HomeScreen`

**설명**: Phase 1~4 모든 팝업 우선순위 로직을 포함한 전체 로그를 표시합니다. (정보량이 많아 Phase 2.5만 테스트 시에는 권장하지 않음)

**실제 로그 패턴 예시**:
- 정책 로드: `HomeScreen: ✅ update_policy found: targetVersion=10, isForce=false`
- 업데이트 결정: `HomeScreen: Decision: OPTIONAL UPDATE from update_policy (target=10)`

---
### 📊 Phase 2.5 주요 로그 설명

| 로그 | 의미 | 테스트 시나리오 |
|------|------|----------------|
| `⏱️ Update interval elapsed (>= Xs)` | 지정 시간 경과, 재표시 허용 | S3 (디버그: 60s) |
| `📊 Current later count: X / Y` | 현재 카운트 확인 (시간 경과 시) | S3, S4 |
| `⏱️ Tracking: laterCount=X→Y` | "나중에" 클릭 시 카운트 증가 | S2, S3 |
| `🚨 Later count (3) >= max (3)` | 최대 횟수 도달, 강제 전환 | S4 |
| `✋ Update dialog dismissed` | "나중에" 클릭 완료 | S2, S3 |
| `⏸️ Update dialog skipped` | 시간 미경과로 스킵 | S2 재시작 |
| `🧹 Clearing old update tracking` | 업데이트 완료, 추적 초기화 | S5 |

---
## 3. DB 스키마 변경 SQL

### Phase 2.5 필드 추가

```sql
-- update_policy 테이블에 시간 기반 재표시 필드 추가
ALTER TABLE public.update_policy
ADD COLUMN IF NOT EXISTS reshow_interval_hours INT DEFAULT 24 NOT NULL,
ADD COLUMN IF NOT EXISTS reshow_interval_minutes INT DEFAULT NULL,  -- 테스트용 (분 단위)
ADD COLUMN IF NOT EXISTS reshow_interval_seconds INT DEFAULT NULL,  -- 초고속 테스트용 (초 단위, 최우선)
ADD COLUMN IF NOT EXISTS max_later_count INT DEFAULT 3 NOT NULL;

-- 기본값 설정 확인
SELECT app_id, target_version_code, is_force_update, 
       reshow_interval_hours, reshow_interval_minutes, reshow_interval_seconds, max_later_count
FROM update_policy
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```

**필드 우선순위** (가장 작은 단위가 최우선):
1. **`reshow_interval_seconds`** (초 단위) - NULL이 아니면 최우선 사용 (초고속 테스트용)
2. **`reshow_interval_minutes`** (분 단위) - seconds가 NULL이고 minutes가 NULL이 아니면 사용 (빠른 테스트용)
3. **`reshow_interval_hours`** (시간 단위) - 위 두 개가 모두 NULL이면 사용 (운영 환경)

**⚠️ 운영 환경 필수 조건**:
- `reshow_interval_seconds` = NULL (항상!)
- `reshow_interval_minutes` = NULL (항상!)
- `reshow_interval_hours` = 24 (기본값)

### 초기값 설정 (릴리즈)
```sql
UPDATE update_policy
SET reshow_interval_hours = 24,
    max_later_count = 3,
    is_force_update = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

### 초기값 설정 (디버그 - 테스트 단축)

```sql
-- 디버그 행 존재 여부 자동 확인 후 INSERT 또는 UPDATE (설정 + 즉시 확인)
DO $$
DECLARE
    v_exists BOOLEAN;
BEGIN
    -- 행 존재 여부 확인
    SELECT EXISTS (SELECT 1 FROM update_policy WHERE app_id = 'com.sweetapps.pocketchord.debug') INTO v_exists;
    
    IF v_exists THEN
        -- 행이 있으면 UPDATE
        UPDATE update_policy
        SET is_active = true,
            target_version_code = 10,
            is_force_update = false,
            reshow_interval_hours = 1,
            reshow_interval_minutes = NULL,
            reshow_interval_seconds = 60,
            max_later_count = 3,
            release_notes = '• [DEBUG] 테스트 업데이트',
            download_url = 'https://play.google.com/'
        WHERE app_id = 'com.sweetapps.pocketchord.debug';
        
        RAISE NOTICE '✅ DEBUG 행 업데이트 완료';
    ELSE
        -- 행이 없으면 INSERT
        INSERT INTO update_policy (
            app_id, is_active, target_version_code, is_force_update,
            reshow_interval_hours, reshow_interval_minutes, reshow_interval_seconds,
            max_later_count, release_notes, download_url
        ) VALUES (
            'com.sweetapps.pocketchord.debug', true, 10, false,
            1, NULL, 60, 3,
            '• [DEBUG] 테스트 업데이트', 'https://play.google.com/'
        );
        
        RAISE NOTICE '✅ DEBUG 행 생성 완료';
    END IF;
DO $$;

-- 설정 즉시 확인
SELECT app_id, target_version_code, is_force_update, is_active,
       reshow_interval_hours, reshow_interval_minutes, reshow_interval_seconds, max_later_count
FROM update_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**기대 결과**:
- **메시지**: `✅ DEBUG 행 업데이트 완료` 또는 `✅ DEBUG 행 생성 완료`
- **테이블** (디버그 행만 표시됨):

| app_id | target_version_code | is_force_update | is_active | reshow_interval_hours | reshow_interval_minutes | reshow_interval_seconds | max_later_count |
|--------|---------------------|-----------------|-----------|----------------------|------------------------|------------------------|-----------------|
| com.sweetapps.pocketchord.debug | 10 | false | true | 1 | NULL | 60 | 3 |

**참고**: 이 단계는 디버그 버전만 설정하므로 릴리즈 행(`com.sweetapps.pocketchord`)은 표시되지 않는 것이 정상입니다.

---
## 4. 시나리오별 테스트

### S1. DB 변경 및 초기 설정

**전제조건**: 없음 (섹션 3의 SQL 실행 후 이 단계로 진행)

**목적**: 섹션 3에서 실행한 DB 스키마 변경과 초기값 설정이 정상적으로 적용되었는지 검증

**1단계: 테이블 구조 확인** (새 필드가 추가되었는지 확인)

**SQL 스크립트 - 공통** (릴리즈/디버그 구분 없음):
```sql
-- update_policy 테이블의 모든 컬럼 확인 (테이블 스키마 확인)
SELECT column_name, data_type, column_default, is_nullable
FROM information_schema.columns
WHERE table_name = 'update_policy'
  AND column_name IN ('reshow_interval_hours', 'reshow_interval_minutes', 'reshow_interval_seconds', 'max_later_count')
ORDER BY column_name;
```

**기대 결과**: 4개 행 반환

| column_name | data_type | column_default | is_nullable |
|------------|-----------|----------------|-------------|
| max_later_count | integer | 3 | NO |
| reshow_interval_hours | integer | 24 | NO |
| reshow_interval_minutes | integer | NULL | YES |
| reshow_interval_seconds | integer | NULL | YES |

**참고**: 이 SQL은 테이블 구조 자체를 확인하므로 릴리즈/디버그 구분이 없습니다.

**⚠️ 결과가 0개 행일 경우**:
- 원인: ALTER TABLE이 실행되지 않음
- 조치: 섹션 3의 "Phase 2.5 필드 추가" SQL(ALTER TABLE) 다시 실행
- 재검증: 위 테이블 구조 확인 SQL 다시 실행

---

**2단계: 데이터 값 확인**

**SQL 스크립트 - 공통** (릴리즈 + 디버그 동시 확인):
```sql
-- 디버그 & 릴리즈 설정 확인 (한 번에)
SELECT app_id, is_active, target_version_code, is_force_update, 
       reshow_interval_hours, reshow_interval_minutes, reshow_interval_seconds, max_later_count
FROM update_policy 
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug')
ORDER BY app_id;
```

**기대 결과** (2행 반환):

| app_id | is_active | target_version_code | is_force_update | reshow_interval_hours | reshow_interval_minutes | reshow_interval_seconds | max_later_count |
|--------|-----------|---------------------|-----------------|----------------------|------------------------|------------------------|-----------------|
| com.sweetapps.pocketchord | true | 10 | false | 24 | NULL | NULL | 3 |
| com.sweetapps.pocketchord.debug | true | 10 | false | 1 | NULL | 60 | 3 |

**⚠️ 새 필드가 NULL이거나 조회 안 될 경우**:
- 원인: ALTER TABLE은 되었지만 UPDATE가 안 됨
- 조치: 섹션 3의 "초기값 설정" SQL(UPDATE 문) 다시 실행
  - 릴리즈: `UPDATE update_policy SET reshow_interval_hours = 24...`
  - 디버그: `UPDATE update_policy SET reshow_interval_hours = 1...`
- 재검증: 위 데이터 값 확인 SQL 다시 실행

---

### S2. 첫 "나중에" 클릭

**전제조건**: S1 완료 (DB 필드 추가 및 초기값 설정 완료)

**대상**: 디버그 앱 (reshow_interval_seconds = 60초)

**목적**: "나중에" 버튼 클릭 후 시간 추적이 시작되고, 재시작 시 지정 시간 동안 팝업이 표시되지 않는지 확인

**⚠️ 참고**: 이 시나리오는 디버그 환경에서만 테스트합니다. 릴리즈 환경(24시간 간격)은 실제 운영에서 동작하며, 테스트 시 24시간을 기다릴 수 없기 때문입니다.

---

**1단계: DB 설정 간단 확인** (S1이 정상 완료되었는지만 확인)
```sql
-- 디버그 설정 빠른 확인
SELECT app_id, target_version_code, is_force_update, is_active
FROM update_policy 
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**기대 결과**:

| app_id | target_version_code | is_force_update | is_active |
|--------|---------------------|-----------------|-----------|
| com.sweetapps.pocketchord.debug | 10 | false | true |

**⚠️ 만약 결과가 다르면**: S1 단계로 돌아가서 초기값 설정 SQL을 다시 실행하세요.

---

**2단계: 앱 실행 및 팝업 표시 확인**

**실행**:
1. 디버그 앱 강제 종료 (완전히 종료)
2. 앱 Cold Start로 재실행
3. Logcat 모니터링 (Filter: `tag:HomeScreen`)

**기대 로그** (정상 케이스 - 실제 출력 패턴):
```
HomeScreen: ===== Popup Display Check =====
HomeScreen: showEmergencyDialog: false
HomeScreen: showUpdateDialog: false
HomeScreen: showAnnouncementDialog: false
HomeScreen: Startup: SUPABASE_APP_ID=com.sweetapps.pocketchord.debug, VERSION_CODE=3
HomeScreen: Supabase configured=true
HomeScreen: ===== Phase 1: Checking emergency_policy =====
HomeScreen: ✅ emergency_policy found: isDismissible=null
HomeScreen: ===== Phase 2: Trying update_policy =====
HomeScreen: ✅ update_policy found: targetVersion=10, isForce=false
HomeScreen: Decision: OPTIONAL UPDATE from update_policy (target=10)
HomeScreen: ===== Popup Display Check =====
HomeScreen: showEmergencyDialog: false
HomeScreen: showUpdateDialog: true  ← ✅ 팝업 표시!
HomeScreen: showAnnouncementDialog: false
```

**✅ 사용자의 로그가 위와 동일합니다! 정상 동작입니다!**

**핵심 확인 포인트**:
- ✅ `update_policy found: targetVersion=10, isForce=false` (NULL 아님)
- ✅ `Decision: OPTIONAL UPDATE from update_policy (target=10)` (업데이트 결정)
- ✅ `showUpdateDialog: true` (팝업 표시됨)

**UI 확인 (앱 화면)**:
- ✅ 선택적 업데이트 팝업이 화면에 표시되어야 함
- ✅ "나중에" 버튼 있음
- ✅ "지금 업데이트" 버튼 있음

---

**❌ 만약 다음과 같은 로그가 나온다면 (문제 케이스)**:
```
HomeScreen: ✅ update_policy found: targetVersion=null, isForce=null
```

**문제**: DB에서 `target_version_code` 값이 NULL로 조회됨

**즉시 조치**:
1. Supabase SQL 에디터에서 다음 SQL 실행:
```sql
-- NULL 값 수정
UPDATE update_policy
SET target_version_code = 10,
    is_force_update = false,
    is_active = true,
    reshow_interval_hours = 1,
    max_later_count = 3
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 재확인
SELECT app_id, target_version_code, is_force_update, is_active
FROM update_policy 
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

2. 앱 강제 종료 후 재시작
3. Logcat에서 `targetVersion=10, isForce=false` 확인

**또는**: `docs/sql/fix-s2-null-values.sql` 파일 실행

---

**❌ 만약 팝업이 전혀 안 나온다면**:
- 원인 1: target_version_code <= 앱 versionCode
  - 조치: 2단계의 UPDATE SQL 다시 실행 (target을 더 높게)
- 원인 2: 네트워크 오류 (Supabase 연결 실패)
  - 조치: Logcat에서 "Supabase" 또는 "network" 키워드 검색, 연결 확인
- 원인 3: 앱이 정책을 fetch하지 못함
  - 조치: 앱 재시작 후 다시 테스트

---

**3단계: "나중에" 버튼 클릭**

**실행**:
1. 팝업에서 "나중에" 버튼 클릭
2. 팝업 닫힘 확인

**기대 로그** (실제 출력 패턴):
```
HomeScreen: Update dialog dismissed for code=10
HomeScreen: ⏱️ Tracking: laterCount=0→1, timestamp=1762705544280  ← ✅ 첫 추적 시작!
HomeScreen: ===== Popup Display Check =====
HomeScreen: showEmergencyDialog: false
HomeScreen: showUpdateDialog: false  ← ✅ 팝업이 닫힘!
HomeScreen: showAnnouncementDialog: false
```

**확인 포인트**:
- ✅ `Update dialog dismissed for code=10` - 팝업이 정상적으로 닫힘
- ✅ `⏱️ Tracking: laterCount=0→1` - **첫 "나중에" 클릭, 카운트 0에서 1로 증가!**
- ✅ `timestamp=...` - 현재 시간 저장됨
- ✅ `showUpdateDialog: false` - 팝업 상태가 false로 전환됨
- ✅ 메인 화면으로 복귀

**참고**: 내부적으로 SharedPreferences에 다음 값이 저장됩니다:
- `update_dismissed_time`: 현재 시간 (timestamp)
- `update_later_count`: 1 (처음 저장됨)
- `dismissedVersionCode`: 10

---

**4단계: 재시작 후 미표시 확인 (1분 이내)**

**실행**:
1. 앱 강제 종료
2. 즉시 재시작 (1분 경과 안 함)

**기대 로그** (수정된 코드 - 실제 출력 패턴):
```
HomeScreen: ===== Popup Display Check =====
HomeScreen: showEmergencyDialog: false
HomeScreen: showUpdateDialog: false
HomeScreen: showAnnouncementDialog: false
HomeScreen: Startup: SUPABASE_APP_ID=com.sweetapps.pocketchord.debug, VERSION_CODE=3
HomeScreen: Supabase configured=true
HomeScreen: ===== Phase 1: Checking emergency_policy =====
HomeScreen: ✅ emergency_policy found: isDismissible=null
HomeScreen: ===== Phase 2: Trying update_policy =====
HomeScreen: ✅ update_policy found: targetVersion=10, isForce=false
HomeScreen: ⏸️ Update dialog skipped (dismissed version: 10, target: 10)  ← ✅ 팝업 스킵!
HomeScreen: ===== Phase 3: Checking notice_policy =====
HomeScreen: ✅ notice_policy found: version=null, title=null
```

**필수 확인 포인트** (로그 출력 순서대로):
1. ✅ **로그 시작 부분** (`showUpdateDialog: false` 확인)
   ```
   HomeScreen: showUpdateDialog: false  ← 초기 상태 확인
   ```
2. ✅ **정책 조회** (`update_policy found: targetVersion=10, isForce=false`)
   - NULL 아님, 정상 조회됨
3. ✅ **팝업 스킵 로그** (`⏸️ Update dialog skipped (dismissed version: 10, target: 10)`)
   - 이미 거부한 버전 → 팝업 스킵 (의도된 동작)
4. ✅ **다음 Phase 진행** (`===== Phase 3: Checking notice_policy =====`)
   - Phase 2에서 팝업을 표시하지 않았으므로 Phase 3로 진행

**로그 메시지 설명**:
```
"⏸️ Update dialog skipped (dismissed version: 10, target: 10)"
```

**왜 팝업이 스킵되는가?**
1. **S2-3단계**에서 "나중에" 클릭 시 `dismissedVersionCode = 10` 저장됨
2. **재시작 시** 조건 확인:
   - `currentVersion (3) < targetVersion (10)` → true (업데이트 필요함) ✅
   - `isForceUpdate` → false (선택적 업데이트) ✅
   - **코드 조건**: `dismissedVersionCode (10) != targetVersionCode (10)` → **false** ❌
     - 실제 값 비교: `10 == 10` → true (같음)
     - 코드는 `!=` (같지 않음)을 확인하므로 → false
3. **판단**: `!=` 조건이 false이므로 팝업 표시 조건 불충족 → 팝업 스킵 (시간 미경과)



**기대 결과**: 
- 팝업 미표시, `showUpdateDialog: false` 유지, Phase 3 정상 진행

**참고**: 
- **1분(60초) 경과 후**에는 `dismissedVersionCode`를 무시하고 재표시됨 (S3에서 테스트)
- 현재는 **시간 미경과 + 이미 거부한 버전**이므로 스킵되는 것이 정상


---

**S2 완료 조건**: ✅ 모든 단계(1~4) 통과

---

### S3. 시간 경과 후 재표시

**전제조건**: S2 완료 상태 (1회 "나중에" 클릭 완료)

**대상**: 디버그 앱 (reshow_interval_seconds = 60초)

---

**✅ 코드 수정 완료: 실제 시간 경과 재표시가 정상 작동합니다!**

**변경 사항**:
- ✅ 시간 경과 체크를 버전 비교보다 **먼저** 실행
- ✅ `laterCount` 자동 증가 (1→2→3)
- ✅ 최대 횟수 도달 시 강제 전환
- ✅ SharedPreferences에 `update_preferences` 파일 사용
  - `update_dismissed_time`: 마지막 "나중에" 클릭 시간
  - `update_later_count`: 누적 횟수
  - `dismissedVersionCode`: 거부한 버전

---

**1단계: 1분 경과 대기**

**실행**:
1. S2-4단계 완료 후 (첫 "나중에" 클릭)
2. **실제로 1분(60초) 대기** (디버그 앱 기준 - `reshow_interval_seconds = 60`)
   - 또는 에뮬레이터 시스템 시간을 60초 앞으로 변경:
   ```cmd
   adb -s emulator-5554 shell su root date @$(($(($(date +%s) + 60))))
   ```

**주의**: SharedPreferences 삭제는 **하지 마세요!** (추적 데이터가 초기화됨)

---

**2단계: 앱 재시작**

**실행**:
1. 앱 강제 종료
2. 앱 재실행
3. Logcat 모니터링 (Filter: `tag:HomeScreen`)

**기대 로그** (실제 출력 패턴):
```
HomeScreen: ===== Popup Display Check =====
HomeScreen: showEmergencyDialog: false
HomeScreen: showUpdateDialog: false
HomeScreen: showAnnouncementDialog: false
HomeScreen: Startup: SUPABASE_APP_ID=com.sweetapps.pocketchord.debug, VERSION_CODE=3
HomeScreen: Supabase configured=true
HomeScreen: ===== Phase 1: Checking emergency_policy =====
HomeScreen: ✅ emergency_policy found: isDismissible=null
HomeScreen: ===== Phase 2: Trying update_policy =====
HomeScreen: ✅ update_policy found: targetVersion=10, isForce=false
UpdateLater: ⏱️ Update interval elapsed (>= 60s), reshow allowed  ← ✅ 시간 경과 확인!
UpdateLater: 📊 Current later count: 1 / 3  ← ✅ 현재 횟수 확인
HomeScreen: ===== Popup Display Check =====
HomeScreen: showEmergencyDialog: false
HomeScreen: showUpdateDialog: true  ← ✅ 팝업 재표시!
HomeScreen: showAnnouncementDialog: false
```

**필수 확인 포인트** (로그 순서대로):
1. ✅ `showUpdateDialog: false` (초기 상태)
2. ✅ `update_policy found: targetVersion=10, isForce=false` (정책 조회 성공)
3. ✅ `⏱️ Update interval elapsed (>= 60s), reshow allowed` (시간 경과 감지!)
4. ✅ `📊 Current later count: 1 / 3` (현재 카운트 확인 - 아직 증가 안 함!)
5. ✅ `showUpdateDialog: true` (팝업 재표시)

**UI 확인**:
- ✅ 업데이트 팝업이 다시 나타남
- ✅ "나중에" 버튼 있음 (아직 최대 횟수 도달 전)
- ✅ "업데이트" 버튼 있음

---

**❌ 만약 시간 경과 로그가 안 나온다면**:
```
HomeScreen: ⏸️ Update dialog skipped (dismissed version: 10, target: 10)
```

**원인**: 
1. 60초가 경과하지 않음
2. 시스템 시간 조작 실패
3. SharedPreferences가 삭제됨

**조치**:
1. SharedPreferences 확인:
```cmd
adb -s emulator-5554 shell run-as com.sweetapps.pocketchord.debug cat shared_prefs/update_preferences.xml
```
- `update_dismissed_time`이 있어야 함
- `update_later_count`가 1이어야 함

2. 실제 경과 시간 확인:
```kotlin
// 현재 시간 - dismissedTime >= reshowIntervalMs (60000ms = 60초)
```

3. 더 긴 시간 대기 또는 시스템 시간 조작 재시도

---

**3단계: "나중에" 버튼 클릭**

**실행**:
1. (2단계에서 팝업이 표시되었다면) "나중에" 버튼 클릭
2. 팝업 닫힘 확인

**기대 로그** (실제 출력 패턴):
```
UpdateLater: ✋ Update dialog dismissed for code=10
UpdateLater: ⏱️ Tracking: laterCount=1→2, timestamp=1731150000000  ← ✅ 카운트 증가 추적!
HomeScreen: ===== Popup Display Check =====
HomeScreen: showUpdateDialog: false
HomeScreen: showAnnouncementDialog: false
```

**확인 포인트**:
- ✅ `✋ Update dialog dismissed for code=10` - 팝업 정상 닫힘
- ✅ `⏱️ Tracking: laterCount=1→2` - **카운트가 1에서 2로 증가!**
- ✅ `timestamp=...` - 현재 시간 저장됨
- ✅ `showUpdateDialog: false` - 팝업 상태 false 전환

**내부 동작** (SharedPreferences):
- `update_dismissed_time`: 현재 시간으로 갱신
- `update_later_count`: 1 → 2로 증가
- `dismissedVersionCode`: 10 유지

---

**S3 완료 조건**: 
- ✅ 1분(60초) 경과 후 팝업 재표시 확인
- ✅ `⏱️ Update interval elapsed` 로그 확인
- ✅ `📊 Current later count: X / 3` 로그 확인 (현재 횟수 표시)
- ✅ "나중에" 클릭 후 `laterCount=X→Y` 추적 로그 확인 (이 시점에 카운트 증가!)

**다음 단계**: S3를 총 3회 반복하여 `laterCount`가 3에 도달하면 S4로 이동

---

### S4. 3회 "나중에" 후 강제 전환

**전제조건**: S3를 2회 더 반복 (총 3회 "나중에" 클릭)

**대상**: 디버그 앱 (max_later_count = 3)

**테스트 단계**:
1. S3 과정 반복 → laterCount = 2
2. 다시 1분 경과 후 재시작
3. "나중에" 3번째 클릭 → laterCount = 3
4. 다시 1분 경과 후 재시작

**기대 로그** (4번째 표시 시 - 실제 출력 패턴):
```
HomeScreen: ===== Phase 2: Trying update_policy =====
HomeScreen: ✅ update_policy found: targetVersion=10, isForce=false
UpdateLater: ⏱️ Update interval elapsed (>= 60s), reshow allowed
UpdateLater: 📊 Current later count: 3 / 3  ← ✅ 최대 횟수 도달!
UpdateLater: 🚨 Later count (3) >= max (3), forcing update mode  ← ✅ 강제 전환!
HomeScreen: ===== Popup Display Check =====
HomeScreen: showUpdateDialog: true  ← ✅ 강제 모드로 표시!
```

**UI 확인**:
- ✅ "나중에" 버튼 없음
- ✅ "업데이트" 버튼만 표시
- ✅ 뒤로가기 눌러도 팝업 닫히지 않음
- ✅ X 버튼 없음

---

### S5. 업데이트 후 초기화

**전제조건**: S3 또는 S4 상태 (선택적/강제 업데이트 팝업 표시 중)

**대상**: 디버그/릴리즈 공통 (버전 증가 시 추적 초기화)

---

**✅ 코드 수정 완료: 버전 업데이트 시 추적 데이터가 자동으로 초기화됩니다!**

**자동 초기화 조건**:
- `currentVersionCode >= targetVersionCode` 일 때
- SharedPreferences의 Phase 2.5 추적 데이터 자동 삭제:
  - `update_dismissed_time` 삭제
  - `update_later_count` 삭제
  - `dismissedVersionCode` 삭제

---

**테스트 단계**:

**1단계: 앱 버전 증가**

**실행**:
1. Android Studio에서 `app/build.gradle.kts` 파일 열기
2. `versionCode` 증가:
   ```kotlin
   android {
       defaultConfig {
           versionCode = 11  // 3 → 11로 증가 (target 10보다 높게)
       }
   }
   ```
3. 앱 재빌드 & 디버그 기기에 설치
4. 앱 시작

---

**2단계: 로그 확인**

**기대 로그** (실제 출력 패턴):
```
HomeScreen: ===== Popup Display Check =====
HomeScreen: showEmergencyDialog: false
HomeScreen: showUpdateDialog: false
HomeScreen: showAnnouncementDialog: false
HomeScreen: Startup: SUPABASE_APP_ID=com.sweetapps.pocketchord.debug, VERSION_CODE=11  ← ✅ 새 버전!
HomeScreen: Supabase configured=true
HomeScreen: ===== Phase 1: Checking emergency_policy =====
HomeScreen: ✅ emergency_policy found: isDismissible=null
HomeScreen: ===== Phase 2: Trying update_policy =====
HomeScreen: ✅ update_policy found: targetVersion=10, isForce=false
HomeScreen: update_policy exists but no update needed (current=11 >= target=10)  ← ✅ 업데이트 불필요!
HomeScreen: 🧹 Clearing old update tracking data (version updated)  ← ✅ 자동 초기화!
HomeScreen: ===== Phase 3: Checking notice_policy =====
```

**필수 확인 포인트**:
1. ✅ `VERSION_CODE=11` - 새 버전으로 실행됨
2. ✅ `current=11 >= target=10` - 업데이트 불필요 판정
3. ✅ `🧹 Clearing old update tracking data (version updated)` - **자동 초기화 실행!**
4. ✅ 업데이트 팝업이 표시되지 않음
5. ✅ Phase 3로 정상 진행

**UI 확인**:
- ✅ 업데이트 팝업 미표시
- ✅ 앱이 정상적으로 메인 화면으로 진입

---

**3단계: SharedPreferences 초기화 검증** (선택 사항)

**실행**:
```cmd
adb -s emulator-5554 shell run-as com.sweetapps.pocketchord.debug cat shared_prefs/update_preferences.xml
```

**기대 결과**:
- 파일이 비어있거나 Phase 2.5 관련 키(`update_dismissed_time`, `update_later_count`, `dismissedVersionCode`)가 없음

**또는**:
```
cat: shared_prefs/update_preferences.xml: No such file or directory
```
- ✅ 파일 자체가 삭제됨 (완전 초기화)

---

**4단계: 재시작 후 새 업데이트 팝업 확인**

**실행**:
1. Supabase에서 `target_version_code`를 더 높게 설정 (예: 20):
   ```sql
   UPDATE update_policy
   SET target_version_code = 20,
       is_force_update = false
   WHERE app_id = 'com.sweetapps.pocketchord.debug';
   ```
2. 앱 강제 종료 후 재시작

**기대 로그**:
```
HomeScreen: ===== Phase 2: Trying update_policy =====
HomeScreen: ✅ update_policy found: targetVersion=20, isForce=false
HomeScreen: Decision: OPTIONAL UPDATE from update_policy (target=20)  ← ✅ 새 업데이트 감지!
HomeScreen: showUpdateDialog: true
```

**확인 포인트**:
- ✅ 새 target (20) 업데이트 팝업이 표시됨
- ✅ 이전 추적 데이터(laterCount 등)가 완전히 초기화되어 새로 시작됨

---

**S5 완료 조건**: 
- ✅ 버전 증가 시 팝업 미표시 확인
- ✅ `🧹 Clearing old update tracking data` 로그 확인
- ✅ SharedPreferences 초기화 검증 (선택)
- ✅ 새 업데이트 팝업 정상 표시 확인

---

### S6. 정책 변경 테스트

#### S6-1. 재표시 간격 변경

**SQL 스크립트 - 릴리즈 버전** ⭐:
```sql
-- 릴리즈: 간격을 48시간으로 변경 (기본 24시간 → 48시간)
UPDATE update_policy
SET reshow_interval_hours = 48,
    reshow_interval_minutes = NULL,  -- 운영: 항상 NULL
    reshow_interval_seconds = NULL   -- 운영: 항상 NULL
WHERE app_id = 'com.sweetapps.pocketchord';
```

**SQL 스크립트 - 디버그 버전** 🔧:
```sql
-- 디버그: 간격을 2분(120초)로 변경 (기본 60초 → 120초)
UPDATE update_policy
SET reshow_interval_hours = 1,      -- 미사용 (초 단위 우선)
    reshow_interval_minutes = NULL,  -- 미사용 (초 단위 우선)
    reshow_interval_seconds = 120    -- 120초 (2분)
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**테스트** (디버그 기준): 
- "나중에" 클릭 후 60초 경과 → 미표시 ✅
- 120초 경과 → 재표시 ✅

---

#### S6-2. 최대 횟수 변경

**SQL 스크립트 - 릴리즈 버전** ⭐:
```sql
-- 릴리즈: 최대 1회로 변경 (기본 3회 → 1회)
UPDATE update_policy
SET max_later_count = 1
WHERE app_id = 'com.sweetapps.pocketchord';
```

**SQL 스크립트 - 디버그 버전** 🔧:
```sql
-- 디버그: 최대 1회로 변경 (기본 3회 → 1회)
UPDATE update_policy
SET max_later_count = 1
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**테스트** (디버그 기준):
- 첫 "나중에" 클릭 → laterCount = 1
- 시간 경과 후 재시작 → laterCount(1) >= max(1) → 즉시 강제 모드 ✅

---

#### S6-3. 즉시 강제 전환

**SQL 스크립트 - 릴리즈 버전** ⭐:
```sql
-- 릴리즈: 정책을 강제 업데이트로 변경
UPDATE update_policy
SET is_force_update = true
WHERE app_id = 'com.sweetapps.pocketchord';
```

**SQL 스크립트 - 디버그 버전** 🔧:
```sql
-- 디버그: 정책을 강제 업데이트로 변경
UPDATE update_policy
SET is_force_update = true
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**테스트** (디버그 기준):
- 앱 재시작
- 기존 laterCount 무시하고 즉시 강제 팝업 표시 ✅
- "나중에" 버튼 없음, 뒤로가기 차단 ✅

---

## 5. 에지 케이스 테스트

| 케이스 | 설정 | 기대 동작 |
|--------|------|-----------|
| 음수 간격 | reshow_interval_hours = -1 | 앱에서 최소값(1)으로 클램프, 로그 경고 |
| 0 간격 | reshow_interval_hours = 0 | 매번 재표시 (즉시 재표시) |
| 과대 간격 | reshow_interval_hours = 999 | 정상 동작 (999시간 = 약 41일) |
| 0 횟수 | max_later_count = 0 | 즉시 강제 모드 |
| 음수 횟수 | max_later_count = -1 | 최소값(1)으로 클램프 |
| 타임존 변경 | 디바이스 타임존 변경 | UTC 기준 추적이면 정상 동작 |
| 시계 뒤로 조작 | 과거 시간으로 설정 | 재표시 안 됨 (경과 시간 음수) |

---

## 6. SharedPreferences 초기화 명령

### 전체 초기화
```cmd
adb -s emulator-5554 shell run-as com.sweetapps.pocketchord.debug rm shared_prefs/update_preferences.xml
```

### 특정 값만 확인
```cmd
adb -s emulator-5554 shell run-as com.sweetapps.pocketchord.debug cat shared_prefs/update_preferences.xml
```

---

## 7. 최소 체크리스트

Phase 2.5 완료 조건:
- [ ] S1: DB 필드 추가 및 초기값 설정 확인
- [ ] S2: "나중에" 클릭 후 시간 추적 확인 (재시작 시 미표시)
- [ ] S3: 시간 경과 후 재표시 확인 (디버그: 1분 후 / 릴리즈: 24시간 후 재등장)
- [ ] S4: 3회 "나중에" 후 강제 전환 확인 (버튼 숨김, 뒤로가기 차단)
- [ ] S5: 버전 증가 시 추적 초기화 확인
- [ ] S6: 정책 변경(간격/횟수) 반영 확인
- [ ] 에지 케이스: 음수/0 값 방어 로직 확인

---

## 8. 초기화/복구 SQL

### 테스트 전 초기 상태로 복구

**디버그 버전 초기화** 🔧:
```sql
-- 디버그 초기화 (테스트용 짧은 간격)
UPDATE update_policy
SET target_version_code = 10,
    is_force_update = false,
    reshow_interval_hours = 1,   -- 1시간 (미사용)
    reshow_interval_minutes = NULL,  -- NULL (초 단위 우선)
    reshow_interval_seconds = 60,    -- 60초 (1분 테스트)
    max_later_count = 3,
    is_active = true
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 확인
SELECT app_id, target_version_code, is_force_update, 
       reshow_interval_hours, reshow_interval_minutes, reshow_interval_seconds, max_later_count, is_active
FROM update_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**기대 결과**:

| app_id | target_version_code | is_force_update | reshow_interval_hours | reshow_interval_minutes | reshow_interval_seconds | max_later_count | is_active |
|--------|---------------------|-----------------|----------------------|------------------------|------------------------|-----------------|-----------|
| com.sweetapps.pocketchord.debug | 10 | false | 1 | NULL | 60 | 3 | true |

**릴리즈 버전 초기화** ⭐:
```sql
-- 릴리즈 운영 기본값
UPDATE update_policy
SET target_version_code = 10,
    is_force_update = false,
    reshow_interval_hours = 24,  -- 24시간
    reshow_interval_minutes = NULL,  -- 운영: 항상 NULL
    reshow_interval_seconds = NULL,  -- 운영: 항상 NULL
    max_later_count = 3,
    is_active = true
WHERE app_id = 'com.sweetapps.pocketchord';

-- 확인
SELECT app_id, target_version_code, is_force_update,
       reshow_interval_hours, reshow_interval_minutes, reshow_interval_seconds, max_later_count, is_active
FROM update_policy
WHERE app_id = 'com.sweetapps.pocketchord';
```

**기대 결과**:

| app_id | target_version_code | is_force_update | reshow_interval_hours | reshow_interval_minutes | reshow_interval_seconds | max_later_count | is_active |
|--------|---------------------|-----------------|----------------------|------------------------|------------------------|-----------------|-----------|
| com.sweetapps.pocketchord | 10 | false | 24 | NULL | NULL | 3 | true |

**두 버전 동시 초기화**:
```sql
-- 디버그 초기화
UPDATE update_policy
SET target_version_code = 10, is_force_update = false,
    reshow_interval_hours = 1, reshow_interval_minutes = NULL, reshow_interval_seconds = 60,
    max_later_count = 3, is_active = true
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 릴리즈 초기화
UPDATE update_policy
SET target_version_code = 10, is_force_update = false,
    reshow_interval_hours = 24, reshow_interval_minutes = NULL, reshow_interval_seconds = NULL,
    max_later_count = 3, is_active = true
WHERE app_id = 'com.sweetapps.pocketchord';

-- 두 버전 확인
SELECT app_id, target_version_code, is_force_update,
       reshow_interval_hours, reshow_interval_minutes, reshow_interval_seconds, max_later_count, is_active
FROM update_policy
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug')
ORDER BY app_id;
```

**기대 결과**:

| app_id | target_version_code | is_force_update | reshow_interval_hours | reshow_interval_minutes | reshow_interval_seconds | max_later_count | is_active |
|--------|---------------------|-----------------|----------------------|------------------------|------------------------|-----------------|-----------|
| com.sweetapps.pocketchord | 10 | false | 24 | NULL | NULL | 3 | true |
| com.sweetapps.pocketchord.debug | 10 | false | 1 | NULL | 60 | 3 | true |

---

## 9. 참고 문서

- **설계 문서**: [UPDATE-POLICY-TIME-BASED-STRATEGY.md](UPDATE-POLICY-TIME-BASED-STRATEGY.md)
- **통합 가이드**: [POPUP-SYSTEM-GUIDE.md](POPUP-SYSTEM-GUIDE.md)
- **Phase 2 기본**: [RELEASE-TEST-PHASE2-RELEASE.md](RELEASE-TEST-PHASE2-RELEASE.md)

---

## 10. 변경 이력

- v3.1.2 (2025-11-10 02:15 KST): 문서 논리 오류 수정 (S2 전제조건 등), 시간 표기 일관성 개선
- v3.1.1 (2025-11-10 01:50 KST): 테스트 간격을 60초로 설정 (디버그 버전 빠른 테스트용)
- v3.1.0 (2025-11-10 00:15 KST): 초 단위 테스트 지원 추가 (reshow_interval_seconds)
- v3.0.0 (2025-11-09 20:40 KST): Phase 2.5 원래 목적(시간 기반 재표시)으로 전면 재작성
- v2.0.0 (2025-11-09 16:39 KST): 광고 정책 검증 (폐기)
- v1.0.x: 이전 시간대 제어 실험 (폐기)

---
**문서 버전**: v3.1.2  
**마지막 수정**: 2025-11-10 02:15:00 KST

