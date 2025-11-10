# 릴리즈 테스트 SQL 스크립트 - Phase 2.3 고급 테스트 (섹션 5~6)

- **버전**: v3.1.2  
- **최종 업데이트**: 2025-11-10 03:15:00 KST  
- **이전 문서**: [Phase 2.4 시나리오 테스트](RELEASE-TEST-PHASE2.4-SCENARIOS.md)

---

## 📋 시작하기 전에

**기본 시나리오 테스트(S1~S6)를 아직 완료하지 않았다면:**
➡️ **[Phase 2.4 시나리오 테스트](RELEASE-TEST-PHASE2.4-SCENARIOS.md)로 이동하세요**

**기본 시나리오를 모두 완료했다면:**
➡️ 아래 에지 케이스 및 초기화 방법을 확인하세요

---

## 5. 에지 케이스 테스트

### 📊 에지 케이스 요약

| 케이스 | 설정 | 기대 동작 | 테스트 우선순위 |
|--------|------|-----------|---------------|
| 음수 간격 | reshow_interval_hours = -1 | **Supabase에서 에러 반환** (CHECK 제약 조건) | 중간 (DB 설정 검증) |
| 0 간격 | reshow_interval_seconds = 0 | **즉시 재표시** (0초 간격, elapsed >= 0 항상 true) | 중간 |
| 과대 간격 | reshow_interval_hours = 999 | 정상 동작 (999시간 = 약 41일) | 낮음 |
| 0 횟수 | max_later_count = 0 | 즉시 강제 모드 | 중간 |
| 음수 횟수 | max_later_count = -1 | **Supabase에서 에러 반환** (CHECK 제약 조건) | 중간 (DB 설정 검증) |
| 타임존 변경 | 디바이스 타임존 변경 | UTC 기준 추적이면 정상 동작 | 낮음 |
| 시계 뒤로 조작 | 과거 시간으로 설정 | 재표시 안 됨 (경과 시간 음수) | 중간 |

**⚠️ 참고**: 
- 음수 간격/횟수는 **Supabase CHECK 제약 조건**으로 DB 레벨에서 차단됩니다
- 에지 케이스 테스트는 선택 사항입니다. 운영 환경에서는 정상 범위의 값만 사용합니다

---

### E1. 음수 간격 테스트 (DB 제약 조건)

**목적**: Supabase CHECK 제약 조건이 음수 값을 차단하는지 확인

**전제조건**: Supabase에 CHECK 제약 조건이 적용되어 있어야 함 (섹션 6-2 참조)

**SQL 스크립트** (실패해야 정상):
```sql
UPDATE update_policy
SET reshow_interval_hours = -1
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**기대 동작**:
- ❌ **SQL 실행 실패** (이것이 정상!)
- Supabase에서 에러 반환:
  ```
  new row for relation "update_policy" violates check constraint "check_reshow_interval_positive"
  ```
- 앱에서는 음수 값을 받을 수 없음 (DB가 차단)

**검증**:
- ✅ SQL 실행 시 에러 발생
- ✅ 에러 메시지에 "check constraint" 포함
- ✅ DB 값이 변경되지 않음

**참고**: 
- 이전에는 앱 코드에서 클램프 처리했지만, DB 레벨 차단이 더 안전합니다
- CHECK 제약 조건 적용 방법은 **섹션 6-2**를 참조하세요

---

### E2. 0초 간격 테스트

**목적**: 0으로 설정 시 즉시 재표시되는지 확인

**⚠️ 실제 동작**: 
- Supabase CHECK 제약 조건이 음수를 차단하므로, 코드에서 `coerceAtLeast` 불필요
- `reshow_interval_seconds = 0` → **실제로 0초 간격**으로 동작
- "나중에" 클릭 후 **즉시 재표시** 가능

**⚠️ 우선순위**: 재표시 간격은 **하나만 선택**됩니다 (조합 안 됨)
- 1순위: `reshow_interval_seconds` (NULL이 아니면 이것만 사용)
- 2순위: `reshow_interval_minutes` (seconds가 NULL이고 minutes가 있으면 사용)
- 3순위: `reshow_interval_hours` (둘 다 NULL이면 사용)

**예시**:
- `hours = 0, seconds = 40` → **40초만 적용** (hours는 무시됨)
- `hours = 24, minutes = 5, seconds = NULL` → **5분만 적용** (hours는 무시됨)

**SQL 스크립트**:
```sql
UPDATE update_policy
SET reshow_interval_seconds = 0,  -- 실제로 0초 (즉시 재표시)
    reshow_interval_minutes = NULL,
    reshow_interval_hours = NULL
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**기대 동작**:
- "나중에" 클릭 후 앱 재시작 시 **즉시 팝업 재표시**
- 시간 경과 체크: `elapsed >= 0` (항상 true)

**테스트**:
1. 앱 시작 → 팝업 표시
2. "나중에" 클릭 → 팝업 닫힘
3. **즉시 앱 재시작** (대기 없음)
4. 팝업 **즉시 재표시됨** ✅

**복구**:
```sql
UPDATE update_policy
SET reshow_interval_seconds = 60
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**💡 참고**: 
- 음수는 Supabase CHECK 제약 조건에서 차단되므로 0은 안전하게 사용 가능
- 하지만 0초 간격은 실용적이지 않으므로 운영 환경에서는 사용하지 마세요

---

### E3. 과대 간격 테스트

**목적**: 매우 큰 값(999시간 = 약 41일) 입력 시 정상 동작하는지 확인

**⚠️ 테스트 전략**: 
- 999시간을 실제로 기다릴 수 없으므로, **오버플로우나 크래시 없이 설정만 확인**
- 팝업이 999시간 동안 스킵되는지 로그로 검증

**SQL 스크립트**:
```sql
UPDATE update_policy
SET reshow_interval_hours = 999,
    reshow_interval_minutes = NULL,  -- 우선순위 충돌 방지
    reshow_interval_seconds = NULL   -- seconds가 있으면 hours가 무시됨
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**💡 참고**: 
- `seconds`나 `minutes`가 NULL이 아니면 `hours`는 무시됩니다
- 반드시 다른 단위를 NULL로 설정해야 `hours`가 적용됩니다

**기대 동작**:
- "나중에" 클릭 후 999시간 동안 팝업 미표시
- 오버플로우나 크래시 없이 정상 동작
- `reshowIntervalMs` 계산: `999 * 60 * 60 * 1000 = 3,596,400,000 ms` (약 41일)

**테스트 절차**:

**1단계: 첫 팝업 표시 및 "나중에" 클릭**
- 앱 시작 → 팝업 표시
- "나중에" 클릭 → `dismissedTime` 저장됨

**2단계: 즉시 재시작 (999시간 경과 전)**
- 앱 재시작 (여러 번)
- Logcat 확인:
  ```
  UpdateLater: ⏸️ Update dialog skipped (dismissed version: 10, target: 10)
  ```
- ✅ 팝업이 스킵됨 (정상)

**3단계: 코드 동작 검증 (중요)**
- Logcat에서 `reshowIntervalMs` 값 확인:
  ```kotlin
  val reshowIntervalMs = 999 * 60 * 60 * 1000L  // 3596400000
  val elapsed = now - dismissedTime  // 예: 5000 (5초)
  // elapsed < reshowIntervalMs → 스킵
  ```
- ✅ 오버플로우 없이 계산됨
- ✅ Long 타입 범위 내 (최대 약 292억 년)

**4단계: DB 값 확인**
```sql
SELECT app_id, reshow_interval_hours, reshow_interval_minutes, reshow_interval_seconds
FROM update_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**기대 결과**:
| app_id | reshow_interval_hours | reshow_interval_minutes | reshow_interval_seconds |
|--------|----------------------|------------------------|------------------------|
| com.sweetapps.pocketchord.debug | 999 | NULL | NULL |

**검증 완료 조건**:
- ✅ SQL 실행 시 에러 없음
- ✅ 앱이 크래시하지 않음
- ✅ 팝업이 정상적으로 스킵됨
- ✅ Logcat에 "⏸️ Update dialog skipped" 로그 출력

**복구**:
```sql
UPDATE update_policy
SET reshow_interval_hours = NULL,
    reshow_interval_seconds = 60
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**💡 실용적 참고**: 
- 이 테스트는 **코드가 큰 값을 안전하게 처리하는지 확인**하는 것이 목적
- 실제로 41일을 기다릴 필요는 없음
- 운영 환경에서는 24시간 또는 72시간 정도의 합리적인 값 사용 권장

---

### E4. 0 횟수 테스트 (즉시 강제 모드)

**목적**: max_later_count를 0으로 설정 시 즉시 강제 모드로 전환되는지 확인

**SQL 스크립트**:
```sql
UPDATE update_policy
SET max_later_count = 0
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- SharedPreferences 초기화 (새로 시작)
```
```cmd
adb -s emulator-5554 shell run-as com.sweetapps.pocketchord.debug rm shared_prefs/update_preferences.xml
```

**기대 동작**:
- 첫 팝업 표시 시부터 "나중에" 버튼 없음
- 즉시 강제 모드
- Logcat: `🚨 Later count (0) >= max (0), forcing update mode`

**테스트**:
1. 앱 시작
2. 업데이트 팝업 표시 시 "나중에" 버튼이 없는지 확인
3. 뒤로가기 차단 확인

**복구**:
```sql
UPDATE update_policy
SET max_later_count = 3
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

---

### E5. 음수 횟수 테스트 (DB 제약 조건)

**목적**: Supabase CHECK 제약 조건이 음수 값을 차단하는지 확인

**전제조건**: Supabase에 CHECK 제약 조건이 적용되어 있어야 함 (섹션 6-2 참조)

**SQL 스크립트** (실패해야 정상):
```sql
UPDATE update_policy
SET max_later_count = -1
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**기대 동작**:
- ❌ **SQL 실행 실패** (이것이 정상!)
- Supabase에서 에러 반환:
  ```
  new row for relation "update_policy" violates check constraint "check_reshow_interval_positive"
  ```
- 앱에서는 음수 값을 받을 수 없음 (DB가 차단)

**검증**:
- ✅ SQL 실행 시 에러 발생
- ✅ 에러 메시지에 "check constraint" 포함
- ✅ DB 값이 변경되지 않음

**참고**: 
- 이전에는 앱 코드에서 클램프 처리했지만, DB 레벨 차단이 더 안전합니다
- CHECK 제약 조건 적용 방법은 **섹션 6-2**를 참조하세요

---

### E6. 타임존 변경 테스트

**목적**: 디바이스 타임존 변경 시에도 정상 작동하는지 확인

**전제조건**: 앱이 UTC 기준으로 시간을 추적하는 경우

**테스트**:
1. "나중에" 클릭 (한국 시간: UTC+9)
2. 디바이스 타임존을 뉴욕(UTC-5)으로 변경
3. 앱 재시작
4. 팝업이 정상적으로 스킵되는지 확인

**기대 동작**:
- UTC 기준으로 시간 추적이 되므로 타임존 변경과 무관하게 정상 동작
- 실제 경과 시간(60초)만 체크

**복구**:
- 디바이스 타임존을 원래대로 복구

---

### E7. 시계 뒤로 조작 테스트

**목적**: 시스템 시간을 과거로 조작해도 재표시되지 않는지 확인

**테스트**:
1. "나중에" 클릭 (현재 시간: 14:00)
2. 디바이스 시스템 시간을 과거(13:00)로 변경
3. 앱 재시작

**기대 동작**:
- 경과 시간이 음수(-1시간)이므로 팝업 미표시
- Logcat: `⏸️ Update dialog skipped` (시간 미경과)

**복구**:
- 디바이스 시스템 시간을 현재 시간으로 복구

---

## 6. 초기화/복구 SQL

### 6-1. SharedPreferences 관리

#### 📌 현재 값 확인 (조회)
**용도**: 현재 저장된 추적 데이터 확인

```cmd
adb -s emulator-5554 shell run-as com.sweetapps.pocketchord.debug cat shared_prefs/update_preferences.xml
```

**확인 항목**:
- `update_dismissed_time`: 마지막 "나중에" 클릭 시간 (timestamp)
- `update_later_count`: 누적 "나중에" 클릭 횟수
- `dismissedVersionCode`: 거부한 버전 코드

**예시 출력**:
```xml
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <long name="update_dismissed_time" value="1762705544280" />
    <int name="update_later_count" value="2" />
    <int name="dismissedVersionCode" value="10" />
</map>
```

---

#### 📌 전체 초기화 (삭제)
**용도**: 추적 데이터를 완전히 삭제하여 처음 상태로 리셋

```cmd
adb -s emulator-5554 shell run-as com.sweetapps.pocketchord.debug rm shared_prefs/update_preferences.xml
```

**효과**:
- ✅ `update_dismissed_time` 삭제 → 시간 추적 리셋
- ✅ `update_later_count` 삭제 → 카운트 0으로 리셋
- ✅ `dismissedVersionCode` 삭제 → 거부 이력 삭제
- ✅ 앱 재시작 시 업데이트 팝업이 다시 표시됨 (처음 상태)

**사용 시기**:
- 테스트를 처음부터 다시 시작하고 싶을 때
- S2부터 다시 테스트하고 싶을 때
- laterCount가 3에 도달했는데 다시 테스트하고 싶을 때

---

### 6-2. DB 제약 조건 적용

#### 📌 CHECK 제약 조건 생성 (음수 차단)

**목적**: Supabase DB 레벨에서 음수 값을 원천 차단

**적용 범위**: 
- ✅ `update_policy` **테이블 전체**에 적용
- ✅ 디버그 버전 (`com.sweetapps.pocketchord.debug`) 포함
- ✅ 릴리즈 버전 (`com.sweetapps.pocketchord`) 포함
- ✅ **향후 추가될 모든 앱 ID**에도 자동 적용
- ⚠️ **한 번만 실행**하면 됨 (앱 ID별로 따로 실행하지 않음)

**전제조건**: 기존 음수 데이터가 있다면 먼저 정리 필요

**1단계: 기존 음수 데이터 정리** (있는 경우만):
```sql
-- ⚠️ 주의: 테이블의 모든 row를 검사하고 정리합니다 (디버그, 릴리즈, 기타 앱 ID 모두 포함)

-- 음수 간격 데이터 정리
UPDATE update_policy
SET reshow_interval_hours = 1
WHERE reshow_interval_hours < 0;

UPDATE update_policy
SET reshow_interval_minutes = 1
WHERE reshow_interval_minutes < 0;

UPDATE update_policy
SET reshow_interval_seconds = 60
WHERE reshow_interval_seconds < 0;

-- 음수 횟수 데이터 정리
UPDATE update_policy
SET max_later_count = 1
WHERE max_later_count < 0;

-- 확인 (모든 앱 ID에 대해)
SELECT app_id, reshow_interval_hours, reshow_interval_minutes, reshow_interval_seconds, max_later_count
FROM update_policy
WHERE reshow_interval_hours < 0 
   OR reshow_interval_minutes < 0 
   OR reshow_interval_seconds < 0 
   OR max_later_count < 0;
-- 결과: 0 rows (음수 없음)
```

**2단계: CHECK 제약 조건 생성**:
```sql
-- ✅ update_policy 테이블 자체에 제약 조건 추가 (테이블의 모든 row에 적용됨)
-- ✅ 디버그, 릴리즈, 향후 추가될 모든 앱 ID에 자동 적용
-- ✅ 한 번만 실행하면 됨

ALTER TABLE update_policy
ADD CONSTRAINT check_reshow_interval_positive
CHECK (
    (reshow_interval_hours IS NULL OR reshow_interval_hours >= 0) AND
    (reshow_interval_minutes IS NULL OR reshow_interval_minutes >= 0) AND
    (reshow_interval_seconds IS NULL OR reshow_interval_seconds >= 0) AND
    (max_later_count >= 0)
);
```

**💡 설명**:
- `ALTER TABLE update_policy`: **테이블 레벨 제약 조건**
- 모든 INSERT, UPDATE 작업에 대해 검증
- `app_id`와 무관하게 모든 row에 적용
- 향후 새로운 앱 추가 시에도 자동으로 적용됨

**3단계: 제약 조건 확인**:
```sql
-- 제약 조건 목록 조회
SELECT conname, contype, pg_get_constraintdef(oid)
FROM pg_constraint
WHERE conrelid = 'update_policy'::regclass
  AND conname = 'check_reshow_interval_positive';
```

**기대 결과**:
| conname | contype | pg_get_constraintdef |
|---------|---------|---------------------|
| check_reshow_interval_positive | c | CHECK ((reshow_interval_hours IS NULL OR reshow_interval_hours >= 0) AND ...) |

**4단계: 제약 조건 테스트**:
```sql
-- 디버그 버전에서 음수 입력 시도 (실패해야 정상)
UPDATE update_policy
SET reshow_interval_hours = -1
WHERE app_id = 'com.sweetapps.pocketchord.debug';
-- 예상 에러: new row violates check constraint "check_reshow_interval_positive"

-- 릴리즈 버전에서도 동일하게 차단됨 (테이블 전체 제약)
UPDATE update_policy
SET max_later_count = -5
WHERE app_id = 'com.sweetapps.pocketchord';
-- 예상 에러: new row violates check constraint "check_reshow_interval_positive"
```

**✅ 두 SQL 모두 실패해야 정상입니다!**

**효과**:
- ✅ SQL 직접 실행으로 음수 입력 불가
- ✅ Supabase Dashboard에서 음수 입력 불가
- ✅ API 호출로 음수 입력 불가
- ✅ 앱 코드에서 음수 클램프 로직 불필요 (DB가 이미 차단)

**제약 조건 삭제** (필요한 경우):
```sql
ALTER TABLE update_policy
DROP CONSTRAINT check_reshow_interval_positive;
```

---

### 6-3. DB 정책 초기화

#### 📌 디버그 버전 초기화
```sql
-- 디버그: 테스트 기본값으로 복구
UPDATE update_policy
SET target_version_code = 10,
    is_force_update = false,
    reshow_interval_hours = 1,
    reshow_interval_minutes = NULL,
    reshow_interval_seconds = 60,
    max_later_count = 3,
    is_active = true
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 확인
SELECT app_id, target_version_code, is_force_update,
       reshow_interval_hours, reshow_interval_minutes, reshow_interval_seconds,
       max_later_count, is_active
FROM update_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**기대 결과**:

| app_id | target_version_code | is_force_update | reshow_interval_hours | reshow_interval_minutes | reshow_interval_seconds | max_later_count | is_active |
|--------|---------------------|-----------------|----------------------|------------------------|------------------------|-----------------|-----------|
| com.sweetapps.pocketchord.debug | 10 | false | 1 | NULL | 60 | 3 | true |

---

#### 📌 릴리즈 버전 초기화
```sql
-- 릴리즈: 운영 기본값으로 복구
UPDATE update_policy
SET target_version_code = 10,
    is_force_update = false,
    reshow_interval_hours = 24,
    reshow_interval_minutes = NULL,
    reshow_interval_seconds = NULL,
    max_later_count = 3,
    is_active = true
WHERE app_id = 'com.sweetapps.pocketchord';

-- 확인
SELECT app_id, target_version_code, is_force_update,
       reshow_interval_hours, reshow_interval_minutes, reshow_interval_seconds,
       max_later_count, is_active
FROM update_policy
WHERE app_id = 'com.sweetapps.pocketchord';
```

**기대 결과**:

| app_id | target_version_code | is_force_update | reshow_interval_hours | reshow_interval_minutes | reshow_interval_seconds | max_later_count | is_active |
|--------|---------------------|-----------------|----------------------|------------------------|------------------------|-----------------|-----------|
| com.sweetapps.pocketchord | 10 | false | 24 | NULL | NULL | 3 | true |

---

#### 📌 두 버전 동시 초기화
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

### 6-4. 완전 초기화 (DB + SharedPreferences)

**용도**: 테스트를 완전히 처음부터 다시 시작

#### 📌 실행 순서:
1. **DB 초기화** (위의 "두 버전 동시 초기화" SQL 실행)
2. **SharedPreferences 초기화**:
   ```cmd
   adb -s emulator-5554 shell run-as com.sweetapps.pocketchord.debug rm shared_prefs/update_preferences.xml
   ```
3. **앱 재시작**
4. **확인**: 업데이트 팝업이 처음 상태로 표시되는지 확인

---

### 6-5. 문제 해결 SQL

#### 📌 정책이 활성화되지 않을 때
```sql
UPDATE update_policy
SET is_active = true
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

#### 📌 팝업이 표시되지 않을 때
```sql
-- target_version_code가 현재 앱 버전보다 높은지 확인
UPDATE update_policy
SET target_version_code = 100  -- 매우 높은 값으로 설정
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

#### 📌 강제 모드에서 빠져나올 수 없을 때
```sql
UPDATE update_policy
SET is_force_update = false,
    max_later_count = 3
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

그리고 SharedPreferences 초기화:
```cmd
adb -s emulator-5554 shell run-as com.sweetapps.pocketchord.debug rm shared_prefs/update_preferences.xml
```

---

## ✅ 모든 테스트 완료!


