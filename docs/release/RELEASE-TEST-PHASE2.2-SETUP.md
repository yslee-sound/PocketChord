# 릴리즈 테스트 SQL 스크립트 - Phase 2.2 설정 가이드 (섹션 0~3)

- **버전**: v3.1.2  
- **최종 업데이트**: 2025-11-10 03:15:00 KST  
- **다음 문서**: [Phase 2.4 시나리오 테스트](RELEASE-TEST-PHASE2.4-SCENARIOS.md)

---
## 0. 목적

**Phase 2.2**: 선택적 업데이트에서 "나중에" 클릭 후 **시간 기반 재표시** 구현

| 구분 | 항목 | 설명 | 운영 환경 (릴리즈시) | 우선순위 |
|------|------|------|-------------|-----|
| **DB 필드** | `reshow_interval_hours` | 재표시 간격 - 시간 단위 | NULL (필수) | 2 |
| | `reshow_interval_minutes` | 재표시 간격 - 분 단위 (테스트용) | NULL (필수) | 1 |
| | `reshow_interval_seconds` | 재표시 간격 - 초 단위 (초고속 테스트용, **최우선**) | 24 (기본값, 반드시 hours 단위만 사용) | 0 |
| | `max_later_count` | 최대 "나중에" 횟수 |
| **추적 데이터** | SharedPreferences | `update_dismissed_time`, `update_later_count` | 예시 | 24시간 간격으로 재표시 |
| **테스트 환경<br>(디버그)** | 설정 가능 | 빠른 테스트를 위해 초/분 단위 사용 가능 | 예시 | 60초 간격으로 재표시 |

---
## 🚀 빠른 테스트 시작

**처음 테스트하는 경우:**
➡️ **[섹션 3. DB 스키마 변경 SQL](#3-db-스키마-변경-sql)로 이동하여 초기값 설정부터 시작하세요**

**이미 섹션 3의 DB 스키마 변경 및 초기값 설정을 완료했다면:**
➡️ **[Phase 2.4 시나리오 테스트](RELEASE-TEST-PHASE2.4-SCENARIOS.md)로 이동**

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

### 📊 Phase 2.2 주요 로그 패턴 `tag:UpdateLater`

| 로그 패턴 | 의미 | 테스트 시나리오 |
|----------|------|----------------|
| `UpdateLater: 📊 Current later count: X / Y` | 현재 카운트 확인 (매 시작 시) | 모든 시나리오 |
| `UpdateLater: ⏱️ Update interval elapsed (>= Xs), reshow allowed` | 지정 시간 경과, 재표시 허용 | S3 (디버그: 60s) |
| `UpdateLater: ⏱️ Tracking: laterCount=X→Y, timestamp=...` | "나중에" 클릭 시 카운트 증가 및 시간 기록 | S2, S3 |
| `UpdateLater: 🚨 Later count (3) >= max (3), forcing update mode` | 최대 횟수 도달, 강제 전환 | S4 |
| `UpdateLater: ✋ Update dialog dismissed for code=X` | "나중에" 클릭 완료 | S2, S3 |
| `UpdateLater: ⏸️ Update dialog skipped (dismissed version: X, target: X)` | 시간 미경과로 스킵 | S2 재시작 |
| `UpdateLater: 🧹 Clearing old update tracking data (version updated)` | 업데이트 완료, 추적 초기화 | S5 |

---
## 3. DB 스키마 변경 SQL

### Phase 2.2 필드 추가

**⚠️ 설계 원칙**: 
- `reshow_interval_hours`는 **NOT NULL + DEFAULT 24**로 설정
- 이유: 모두 NULL이 되는 것을 방지하고, 항상 안전한 기본값 보장
- 30초 테스트는 `seconds = 30`만 설정 (우선순위로 해결)

**1단계: 기존 스키마 확인 및 수정** (필요한 경우):
```sql
-- 현재 상태 확인
SELECT column_name, is_nullable, column_default, data_type
FROM information_schema.columns
WHERE table_name = 'update_policy' 
  AND column_name LIKE 'reshow_interval%';
```

**만약 `reshow_interval_hours`가 이미 NULL 허용으로 되어 있다면**:
```sql
-- NOT NULL 제약 추가 + DEFAULT 설정
ALTER TABLE public.update_policy
ALTER COLUMN reshow_interval_hours SET DEFAULT 24,
ALTER COLUMN reshow_interval_hours SET NOT NULL;

-- 기존 NULL 값을 24로 업데이트
UPDATE update_policy
SET reshow_interval_hours = 24
WHERE reshow_interval_hours IS NULL;
```

**2단계: 필드 추가** (아직 추가하지 않은 경우):
```sql
-- update_policy 테이블에 시간 기반 재표시 필드 추가
ALTER TABLE public.update_policy
ADD COLUMN IF NOT EXISTS reshow_interval_hours INT DEFAULT 24 NOT NULL,  -- NOT NULL 유지
ADD COLUMN IF NOT EXISTS reshow_interval_minutes INT DEFAULT NULL,
ADD COLUMN IF NOT EXISTS reshow_interval_seconds INT DEFAULT NULL,
ADD COLUMN IF NOT EXISTS max_later_count INT DEFAULT 3 NOT NULL;

-- 기본값 설정 확인
SELECT app_id, target_version_code, is_force_update, 
       reshow_interval_hours, reshow_interval_minutes, reshow_interval_seconds, max_later_count
FROM update_policy
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```

**확인 결과**:
| column_name | is_nullable | column_default | data_type |
|-------------|-------------|----------------|-----------|
| reshow_interval_hours | NO | 24 | integer |
| reshow_interval_minutes | YES | NULL | integer |
| reshow_interval_seconds | YES | NULL | integer |

**필드 우선순위 및 운영 환경 설정** (가장 작은 단위가 최우선):

**⚠️ 중요**: 세 필드는 **조합되지 않고 우선순위에 따라 하나만 선택**됩니다.
- 예: `hours = 24, seconds = 60` → **60초만 적용** (hours는 무시됨)
- 예: `hours = 1, minutes = 5, seconds = NULL` → **5분만 적용** (hours는 무시됨)
- 예: **30초로 설정** → `hours = 24, seconds = 30` ✅ (hours는 자동 유지, seconds만 설정)

| 우선순위 | 필드 | 단위 | 사용 조건 | 용도 | 운영 환경 설정 | NULL 허용 |
|---------|------|------|----------|------|---------------|----------|
| **1순위** | `reshow_interval_seconds` | 초 | NULL이 아니면 최우선 사용 | 초고속 테스트용 | ⚠️ NULL (필수) | YES |
| **2순위** | `reshow_interval_minutes` | 분 | seconds가 NULL이고 minutes가 NULL이 아니면 사용 | 빠른 테스트용 | ⚠️ NULL (필수) | YES |
| **3순위** | `reshow_interval_hours` | 시간 | 위 두 개가 모두 NULL이면 사용 | 운영 환경 | ✅ 24 (기본값) | **NO** (NOT NULL) |

**💡 30초 간격 설정 예시**:
```sql
UPDATE update_policy
SET reshow_interval_seconds = 30       -- seconds에 30 설정
WHERE app_id = 'com.sweetapps.pocketchord.debug';
-- reshow_interval_hours는 자동으로 24 유지 (DEFAULT)
```

**✅ 올바른 방법**:
```sql
-- 간단하게 seconds만 설정
UPDATE update_policy
SET reshow_interval_seconds = 30
WHERE app_id = 'com.sweetapps.pocketchord.debug';
-- hours = 24 (기본값), seconds = 30 → seconds 우선 적용 (30초)
```

**❌ 잘못된 방법 (이제는 불가능)**:
```sql
-- hours를 NULL로 만들 수 없음 (NOT NULL 제약)
UPDATE update_policy
SET reshow_interval_hours = NULL,
    reshow_interval_seconds = 30
WHERE app_id = 'com.sweetapps.pocketchord.debug';
-- 에러: null value in column "reshow_interval_hours" violates not-null constraint
```

**💡 복구 방법 (초 단위 테스트 해제)**:
```sql
UPDATE update_policy
SET reshow_interval_seconds = NULL  -- seconds만 NULL로
WHERE app_id = 'com.sweetapps.pocketchord.debug';
-- 결과: hours = 24가 적용됨 (24시간 간격)
```

**💡 안전성 보장**:
- `reshow_interval_hours`가 NOT NULL이므로 모든 interval이 NULL이 되는 상황 불가능
- 코드에서 `else` 블록 없이도 안전하게 동작
- 항상 최소 24시간 간격이 보장됨

### 초기값 설정 (릴리즈)
```sql
UPDATE update_policy
SET reshow_interval_hours = 24,
    max_later_count = 3,
    is_force_update = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

### 초기값 설정 (디버그 - 테스트 단축)

**⚠️ 디버그 환경**: 60초 간격 테스트를 위해 `seconds = 60`만 설정합니다.
- `hours`는 기본값 24로 자동 유지됨 (NOT NULL + DEFAULT)
- 우선순위 때문에 seconds가 적용됨

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
            reshow_interval_seconds = 60,      -- 60초 간격
            max_later_count = 3,
            release_notes = '• [DEBUG] 테스트 업데이트',
            download_url = 'https://play.google.com/'
        WHERE app_id = 'com.sweetapps.pocketchord.debug';
        
        RAISE NOTICE '✅ DEBUG 행 업데이트 완료';
    ELSE
        -- 행이 없으면 INSERT
        INSERT INTO update_policy (
            app_id, is_active, target_version_code, is_force_update,
            reshow_interval_seconds,  -- hours는 DEFAULT 24 자동 적용
            max_later_count, release_notes, download_url
        ) VALUES (
            'com.sweetapps.pocketchord.debug', true, 10, false,
            60, 3,  -- seconds만 60, hours는 자동 24
            '• [DEBUG] 테스트 업데이트', 'https://play.google.com/'
        );
        
        RAISE NOTICE '✅ DEBUG 행 생성 완료';
    END IF;
END
$$;

-- 설정 즉시 확인
SELECT app_id, target_version_code, is_force_update, is_active,
       reshow_interval_hours, reshow_interval_minutes, reshow_interval_seconds, max_later_count
FROM update_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**기대 결과**:

| app_id | target_version_code | is_force_update | is_active | reshow_interval_hours | reshow_interval_minutes | reshow_interval_seconds | max_later_count |
|--------|---------------------|-----------------|-----------|----------------------|------------------------|------------------------|-----------------|
| com.sweetapps.pocketchord.debug | 10 | false | true | **24** | NULL | **60** | 3 |

**💡 설명**:
- `reshow_interval_hours = 24` ✅ (DEFAULT 자동 적용)
- `reshow_interval_seconds = 60` ✅ (60초 간격)
- 우선순위: seconds(1순위) > minutes(2순위) > hours(3순위)
- 결과: **60초 간격**으로 동작 (hours는 무시됨)
- **안전성**: hours가 항상 24이므로 모든 interval이 NULL인 상황 불가능
| com.sweetapps.pocketchord.debug | 10 | false | true | 1 | NULL | 60 | 3 |

---

## ➡️ 다음 단계
