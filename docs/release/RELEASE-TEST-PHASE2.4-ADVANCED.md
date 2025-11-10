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
| 음수 간격 | reshow_interval_hours = -1 | 앱에서 최소값(1)으로 클램프, 로그 경고 | 낮음 |
| 0 간격 | reshow_interval_hours = 0 | 매번 재표시 (즉시 재표시) | 중간 |
| 과대 간격 | reshow_interval_hours = 999 | 정상 동작 (999시간 = 약 41일) | 낮음 |
| 0 횟수 | max_later_count = 0 | 즉시 강제 모드 | 중간 |
| 음수 횟수 | max_later_count = -1 | 최소값(1)으로 클램프 | 낮음 |
| 타임존 변경 | 디바이스 타임존 변경 | UTC 기준 추적이면 정상 동작 | 낮음 |
| 시계 뒤로 조작 | 과거 시간으로 설정 | 재표시 안 됨 (경과 시간 음수) | 중간 |

**⚠️ 참고**: 에지 케이스 테스트는 선택 사항입니다. 운영 환경에서는 정상 범위의 값만 사용합니다.

---

### E1. 음수 간격 테스트

**목적**: 음수 값 입력 시 앱이 크래시하지 않고 최소값으로 처리하는지 확인

**SQL 스크립트**:
```sql
UPDATE update_policy
SET reshow_interval_hours = -1
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**기대 동작**:
- 앱에서 음수를 감지하고 최소값(1시간)으로 클램프
- Logcat에 경고 로그 출력:
  ```
  UpdateLater: ⚠️ Invalid reshow_interval_hours: -1, using minimum value 1
  ```

**복구**:
```sql
UPDATE update_policy
SET reshow_interval_hours = 1
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

---

### E2. 0 간격 테스트

**목적**: 0으로 설정 시 매번 재표시되는지 확인

**SQL 스크립트**:
```sql
UPDATE update_policy
SET reshow_interval_seconds = 0
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**기대 동작**:
- "나중에" 클릭 후 앱 재시작 시 즉시 팝업 재표시
- 시간 경과 체크 없이 항상 재표시됨

**테스트**:
1. "나중에" 클릭
2. 즉시 앱 재시작 (1초도 대기 안 함)
3. 팝업이 즉시 재표시되는지 확인

**복구**:
```sql
UPDATE update_policy
SET reshow_interval_seconds = 60
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

---

### E3. 과대 간격 테스트

**목적**: 매우 큰 값(999시간 = 약 41일) 입력 시 정상 동작하는지 확인

**SQL 스크립트**:
```sql
UPDATE update_policy
SET reshow_interval_hours = 999,
    reshow_interval_seconds = NULL
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**기대 동작**:
- "나중에" 클릭 후 999시간 동안 팝업 미표시
- 오버플로우나 크래시 없이 정상 동작

**테스트**:
1. "나중에" 클릭
2. 앱 재시작 (여러 번)
3. 팝업이 계속 스킵되는지 확인
4. Logcat에 `⏸️ Update dialog skipped` 로그 확인

**복구**:
```sql
UPDATE update_policy
SET reshow_interval_hours = 1,
    reshow_interval_seconds = 60
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

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

### E5. 음수 횟수 테스트

**목적**: max_later_count에 음수 입력 시 최소값으로 처리하는지 확인

**SQL 스크립트**:
```sql
UPDATE update_policy
SET max_later_count = -1
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**기대 동작**:
- 앱에서 음수를 감지하고 최소값(1)으로 클램프
- Logcat에 경고 로그:
  ```
  UpdateLater: ⚠️ Invalid max_later_count: -1, using minimum value 1
  ```

**복구**:
```sql
UPDATE update_policy
SET max_later_count = 3
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

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

### 6-2. DB 정책 초기화

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

### 6-3. 완전 초기화 (DB + SharedPreferences)

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

### 6-4. 문제 해결 SQL

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


