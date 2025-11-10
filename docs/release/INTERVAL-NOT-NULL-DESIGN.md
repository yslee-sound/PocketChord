# reshow_interval_hours NOT NULL + DEFAULT 설계 결정

**작성일**: 2025-11-10  
**버전**: v3.1.2

---

## 📋 문제 상황

**초기 설계**:
```sql
ALTER TABLE update_policy
ADD COLUMN reshow_interval_hours INT DEFAULT 24,  -- NULL 허용
ADD COLUMN reshow_interval_minutes INT DEFAULT NULL,
ADD COLUMN reshow_interval_seconds INT DEFAULT NULL;
```

**문제점**:
1. ❌ **3개의 interval이 모두 NULL이 될 수 있음**
2. ❌ 30초 테스트 시 `hours = NULL, seconds = 30` 설정 필요 (혼란)
3. ❌ 코드에서 `else` 블록 필요 (모두 NULL인 경우 처리)
4. ❌ 안전성 부족

---

## ✅ 해결 방법: NOT NULL + DEFAULT

### 설계 변경

```sql
ALTER TABLE public.update_policy
ALTER COLUMN reshow_interval_hours SET DEFAULT 24,
ALTER COLUMN reshow_interval_hours SET NOT NULL;
```

**핵심 원칙**:
- `reshow_interval_hours`는 **NOT NULL + DEFAULT 24**
- `reshow_interval_minutes`, `reshow_interval_seconds`는 **NULL 허용**

---

## 🎯 장점

### 1. 안전성 보장 ✅
- **모든 interval이 NULL이 되는 상황 불가능**
- `hours`가 항상 최소 24 값을 가지므로 안전
- 코드에서 `else` 블록 불필요

### 2. 간단한 사용법 ✅
**30초 테스트 설정**:
```sql
-- 간단하게 seconds만 설정
UPDATE update_policy
SET reshow_interval_seconds = 30
WHERE app_id = 'com.sweetapps.pocketchord.debug';
-- hours는 자동으로 24 유지 (DEFAULT)
```

**이전 방법 (복잡함)**:
```sql
-- hours를 명시적으로 NULL로 만들어야 함
UPDATE update_policy
SET reshow_interval_hours = NULL,
    reshow_interval_seconds = 30
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

### 3. 우선순위 시스템과 완벽 호환 ✅
**코드 동작**:
```kotlin
val reshowIntervalMs = when {
    policy.reshowIntervalSeconds != null -> policy.reshowIntervalSeconds * 1000L
    policy.reshowIntervalMinutes != null -> policy.reshowIntervalMinutes * 60 * 1000L
    else -> policy.reshowIntervalHours * 60 * 60 * 1000L  // 항상 안전함!
}
```

**예시**:
- `hours = 24, seconds = 30` → **30초 적용** (seconds 우선)
- `hours = 24, seconds = NULL` → **24시간 적용** (hours 사용)

### 4. 명확한 의도 전달 ✅
- `hours = 24`가 항상 보임 → 기본 간격이 명확
- `seconds = 60`이 있으면 테스트 모드임을 알 수 있음

---

## 📊 비교표

| 구분 | 이전 (NULL 허용) | 변경 후 (NOT NULL + DEFAULT) |
|------|-----------------|---------------------------|
| **모두 NULL 가능?** | ✅ 가능 (위험) | ❌ 불가능 (안전) |
| **30초 설정** | `hours = NULL, seconds = 30` | `seconds = 30` (간단) |
| **코드 else 블록** | 필요 | 불필요 |
| **기본값 명확성** | 낮음 | 높음 (항상 hours = 24) |
| **실수 가능성** | 높음 | 낮음 |

---

## 🔧 마이그레이션 가이드

### 1단계: 스키마 변경
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

### 2단계: 기존 설정 정리
```sql
-- 디버그: hours를 24로 명시
UPDATE update_policy
SET reshow_interval_hours = 24
WHERE app_id = 'com.sweetapps.pocketchord.debug'
  AND reshow_interval_hours IS NULL;

-- 릴리즈: hours를 24로 명시
UPDATE update_policy
SET reshow_interval_hours = 24
WHERE app_id = 'com.sweetapps.pocketchord'
  AND reshow_interval_hours IS NULL;
```

### 3단계: 확인
```sql
SELECT column_name, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'update_policy' 
  AND column_name LIKE 'reshow_interval%';
```

**기대 결과**:
| column_name | is_nullable | column_default |
|-------------|-------------|----------------|
| reshow_interval_hours | **NO** | 24 |
| reshow_interval_minutes | YES | NULL |
| reshow_interval_seconds | YES | NULL |

---

## 📝 사용 예시

### 테스트 환경 (디버그)

**60초 간격 테스트**:
```sql
UPDATE update_policy
SET reshow_interval_seconds = 60
WHERE app_id = 'com.sweetapps.pocketchord.debug';
-- hours = 24는 자동 유지, seconds 우선 적용
```

**30초 간격 테스트**:
```sql
UPDATE update_policy
SET reshow_interval_seconds = 30
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**테스트 해제 (24시간으로 복귀)**:
```sql
UPDATE update_policy
SET reshow_interval_seconds = NULL
WHERE app_id = 'com.sweetapps.pocketchord.debug';
-- hours = 24가 적용됨
```

### 운영 환경 (릴리즈)

**24시간 간격** (기본):
```sql
UPDATE update_policy
SET reshow_interval_hours = 24,
    reshow_interval_seconds = NULL
WHERE app_id = 'com.sweetapps.pocketchord';
```

**72시간 간격**:
```sql
UPDATE update_policy
SET reshow_interval_hours = 72,
    reshow_interval_seconds = NULL
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

## ⚠️ 주의사항

### 1. hours를 NULL로 만들 수 없음
```sql
-- ❌ 에러 발생
UPDATE update_policy
SET reshow_interval_hours = NULL
WHERE app_id = 'com.sweetapps.pocketchord.debug';
-- 에러: null value in column "reshow_interval_hours" violates not-null constraint
```

### 2. seconds만 설정하면 됨 (hours는 자동 유지)
```sql
-- ✅ 올바른 방법
UPDATE update_policy
SET reshow_interval_seconds = 30
WHERE app_id = 'com.sweetapps.pocketchord.debug';
-- hours는 자동으로 24 유지
```

### 3. 복구 시 seconds를 NULL로
```sql
-- ✅ 올바른 복구 방법
UPDATE update_policy
SET reshow_interval_seconds = NULL
WHERE app_id = 'com.sweetapps.pocketchord.debug';
-- hours = 24가 적용됨 (24시간 간격)
```

---

## 🎉 결론

**NOT NULL + DEFAULT 설계의 핵심**:
1. ✅ **안전성**: 모든 interval이 NULL이 되는 상황 불가능
2. ✅ **단순성**: 30초 테스트는 `seconds = 30`만 설정
3. ✅ **명확성**: `hours = 24`가 항상 보여서 기본값이 명확
4. ✅ **유연성**: 우선순위 시스템으로 테스트/운영 환경 모두 대응

**권장 사항**:
- 운영 환경: `hours`만 사용 (24시간 또는 72시간)
- 테스트 환경: `hours = 24` 유지 + `seconds` 설정 (30, 60 등)
- 복구: `seconds = NULL`로 설정 (hours 자동 적용)

이 설계로 **안전하고 명확한 interval 관리**가 가능합니다! 🎯

