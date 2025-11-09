# 릴리즈 테스트 SQL 스크립트 - Phase 2.5 시나리오 테스트 (섹션 4~6)

- **버전**: v3.1.2  
- **최종 업데이트**: 2025-11-10 03:15:00 KST  
- **이전 문서**: [Phase 2.5 설정 가이드](RELEASE-TEST-PHASE2.5-SETUP.md)

---

## 📋 시작하기 전에

**DB 설정을 아직 완료하지 않았다면:**
➡️ **[Phase 2.5 설정 가이드](RELEASE-TEST-PHASE2.5-SETUP.md)로 이동하여 섹션 3을 먼저 완료하세요**

**이미 DB 설정을 완료했다면:**
➡️ 아래 **S1. DB 변경 및 초기 설정**부터 시작하세요

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

**⚠️ 참고**: 이 시나리오는 디버그 환경에서만 테스트합니다. 릴리즈 환경(24시간 간격)

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
3. Logcat 모니터링 (Filter: `tag:UpdateLater`)

**기대 UI**:
- ✅ 선택적 업데이트 팝업이 화면에 표시되어야 함
- ✅ "나중에" 버튼과 "업데이트" 버튼이 모두 보여야 함

**핵심 확인 포인트**:
- 첫 실행이므로 팝업이 표시되어야 함 (아직 "나중에"를 누른 적 없음)
- ✅ "나중에" 버튼 있음
- ✅ "지금 업데이트" 버튼 있음

---

**3단계: "나중에" 버튼 클릭**

**실행**:
1. 팝업에서 "나중에" 버튼 클릭
2. 팝업 닫힘 확인

**기대 로그** (UpdateLater 태그):
```
UpdateLater: ✋ Update dialog dismissed for code=10
UpdateLater: ⏱️ Tracking: laterCount=0→1, timestamp=1762705544280  ← ✅ 첫 추적 시작!
```

**확인 포인트**:
- ✅ `✋ Update dialog dismissed for code=10` - 팝업이 정상적으로 닫힘
- ✅ `⏱️ Tracking: laterCount=0→1` - **첫 "나중에" 클릭, 카운트 0에서 1로 증가!**
- ✅ `timestamp=...` - 현재 시간 저장됨
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

**기대 로그** (UpdateLater 태그):
```
UpdateLater: ⏸️ Update dialog skipped (dismissed version: 10, target: 10)  ← ✅ 팝업 스킵!
```

**확인 포인트**:
- ✅ `⏸️ Update dialog skipped` - 시간 미경과로 팝업 스킵됨
- ✅ 팝업이 표시되지 않고 메인 화면으로 진입

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
3. Logcat 모니터링 (Filter: `tag:UpdateLater`)

**기대 로그** (UpdateLater 태그):
```
UpdateLater: ⏱️ Update interval elapsed (>= 60s), reshow allowed  ← ✅ 시간 경과 확인!
UpdateLater: 📊 Current later count: 1 / 3  ← ✅ 현재 횟수 확인
```

**필수 확인 포인트**:
1. ✅ `⏱️ Update interval elapsed (>= 60s), reshow allowed` (시간 경과 감지!)
2. ✅ `📊 Current later count: 1 / 3` (현재 카운트 확인 - 아직 증가 안 함!)

**UI 확인**:
- ✅ 업데이트 팝업이 다시 나타남
- ✅ "나중에" 버튼 있음 (아직 최대 횟수 도달 전)
- ✅ "업데이트" 버튼 있음

---

**3단계: "나중에" 버튼 클릭**

**실행**:
1. (2단계에서 팝업이 표시되었다면) "나중에" 버튼 클릭
2. 팝업 닫힘 확인

**기대 로그** (UpdateLater 태그):
```
UpdateLater: ✋ Update dialog dismissed for code=10
UpdateLater: ⏱️ Tracking: laterCount=1→2, timestamp=1731150000000  ← ✅ 카운트 증가 추적!
```

**확인 포인트**:
- ✅ `✋ Update dialog dismissed for code=10` - 팝업 정상 닫힘
- ✅ `⏱️ Tracking: laterCount=1→2` - **카운트가 1에서 2로 증가!**
- ✅ `timestamp=...` - 현재 시간 저장됨

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

**기대 로그** (4번째 표시 시 - UpdateLater 태그):
```
UpdateLater: ⏱️ Update interval elapsed (>= 60s), reshow allowed
UpdateLater: 📊 Current later count: 3 / 3  ← ✅ 최대 횟수 도달!
UpdateLater: 🚨 Later count (3) >= max (3), forcing update mode  ← ✅ 강제 전환!
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

**기대 로그** (UpdateLater 태그):
```
UpdateLater: 🧹 Clearing old update tracking data (version updated)  ← ✅ 자동 초기화!
```

**필수 확인 포인트**:
1. ✅ `🧹 Clearing old update tracking data (version updated)` - **자동 초기화 실행!**
2. ✅ 업데이트 팝업이 표시되지 않음

**UI 확인**:
- ✅ 업데이트 팝업 미표시
- ✅ 앱이 정상적으로 메인 화면으로 진입

---

**3단계: 재시작 후 새 업데이트 팝업 확인**

**실행**:
1. Supabase에서 `target_version_code`를 더 높게 설정 (예: 20):
2. 앱 강제 종료 후 재시작

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

## 6. 초기화/복구 SQL

### SharedPreferences 초기화 명령
```cmd
adb -s emulator-5554 shell run-as com.sweetapps.pocketchord.debug rm shared_prefs/update_preferences.xml
```

### 테스트 전 초기 상태로 복구

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

## ✅ 테스트 완료!

모든 시나리오를 완료했습니다. 문제가 있거나 처음부터 다시 시작하려면:

➡️ **[Phase 2.5 설정 가이드로 돌아가기](RELEASE-TEST-PHASE2.5-SETUP.md)**

