# Debug 테스트 데이터 추가 완료

**작성일**: 2025-11-09  
**목적**: 각 테이블에 `***.debug` app_id 테스트 데이터 추가  
**상태**: ✅ 완료

---

## ✅ 추가된 테스트 데이터

### 1. update_policy ✅

```sql
INSERT INTO public.update_policy (
    app_id,
    is_active,
    target_version_code,
    is_force_update,
    message,
    release_notes
) VALUES (
    'com.sweetapps.pocketchord.debug',
    false,  -- 비활성화 (테스트 시 수동으로 활성화)
    999,    -- 높은 버전 (테스트용)
    false,
    '[DEBUG] 테스트용 업데이트 메시지',
    '• [DEBUG] 테스트 기능 1\n• [DEBUG] 테스트 기능 2'
);
```

**특징**:
- ✅ `is_active = false` (기본 비활성화)
- ✅ `target_version_code = 999` (높은 버전으로 테스트 용이)
- ✅ `[DEBUG]` 접두사로 식별 가능

---

### 2. emergency_policy ✅

```sql
INSERT INTO public.emergency_policy (
    app_id,
    is_active,
    content,
    redirect_url,
    new_app_id,
    is_dismissible
) VALUES (
    'com.sweetapps.pocketchord.debug',
    false,  -- 비활성화 (테스트 시 수동으로 활성화)
    '🚨 [DEBUG] 긴급 테스트 메시지입니다.\n이것은 디버그용 팝업입니다.',
    'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord.debug',
    'com.sweetapps.pocketchord.debug.v2',
    true    -- X 버튼 허용
);
```

**특징**:
- ✅ `is_active = false` (기본 비활성화)
- ✅ `is_dismissible = true` (X 버튼 있음)
- ✅ `new_app_id` 포함 (리다이렉트 테스트 가능)
- ✅ `[DEBUG]` 접두사로 식별 가능

---

### 3. notice_policy ✅

```sql
INSERT INTO public.notice_policy (
    app_id,
    is_active,
    title,
    content,
    notice_version,
    image_url,
    action_url
) VALUES (
    'com.sweetapps.pocketchord.debug',
    false,  -- 비활성화 (테스트 시 수동으로 활성화)
    '[DEBUG] 디버그 테스트 공지 📋',
    '[DEBUG] 이것은 테스트용 공지사항입니다.\n버전 관리 테스트를 위한 샘플 데이터입니다.',
    1,  -- 버전 1
    'https://via.placeholder.com/300x200?text=DEBUG',
    'https://example.com/debug'
);
```

**특징**:
- ✅ `is_active = false` (기본 비활성화)
- ✅ `notice_version = 1` (버전 관리 테스트 가능)
- ✅ `image_url`, `action_url` 포함 (전체 기능 테스트)
- ✅ `[DEBUG]` 접두사로 식별 가능

---

## 🧪 테스트 방법

### 각 테이블 데이터 확인

```sql
-- update_policy 확인
SELECT * FROM update_policy WHERE app_id LIKE '%.debug';

-- emergency_policy 확인
SELECT * FROM emergency_policy WHERE app_id LIKE '%.debug';

-- notice_policy 확인
SELECT * FROM notice_policy WHERE app_id LIKE '%.debug';
```

**예상 결과**: 각 테이블에서 1개씩, 총 3개의 debug 데이터 확인

---

### Debug 데이터 활성화 (테스트용)

#### update_policy 활성화
```sql
UPDATE update_policy 
SET is_active = true,
    target_version_code = 999,  -- 현재 버전보다 높게
    is_force_update = false     -- 선택적 업데이트 테스트
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**예상 결과**: Debug 빌드 실행 시 선택적 업데이트 팝업 표시

---

#### emergency_policy 활성화
```sql
UPDATE emergency_policy 
SET is_active = true,
    is_dismissible = true  -- X 버튼 있음
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**예상 결과**: Debug 빌드 실행 시 긴급 팝업 표시 (최우선)

---

#### notice_policy 활성화
```sql
UPDATE notice_policy 
SET is_active = true,
    notice_version = 10  -- 높은 버전으로 설정
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**예상 결과**: Debug 빌드 실행 시 공지 팝업 표시

---

### Debug 데이터 비활성화 (원복)

```sql
-- 모든 debug 데이터 비활성화
UPDATE update_policy SET is_active = false WHERE app_id LIKE '%.debug';
UPDATE emergency_policy SET is_active = false WHERE app_id LIKE '%.debug';
UPDATE notice_policy SET is_active = false WHERE app_id LIKE '%.debug';
```

---

## 📊 전체 데이터 구조

### 각 테이블별 데이터 (SQL 실행 후)

```
update_policy (2개 행)
┌────┬──────────────────────────────────┬───────────┬─────────────────────┐
│ id │ app_id                           │ is_active │ target_version_code │
├────┼──────────────────────────────────┼───────────┼─────────────────────┤
│ 1  │ com.sweetapps.pocketchord        │ true      │ 1                   │
│ 2  │ com.sweetapps.pocketchord.debug  │ false     │ 999                 │
└────┴──────────────────────────────────┴───────────┴─────────────────────┘

emergency_policy (2개 행)
┌────┬──────────────────────────────────┬───────────┬────────────────┐
│ id │ app_id                           │ is_active │ is_dismissible │
├────┼──────────────────────────────────┼───────────┼────────────────┤
│ 1  │ com.sweetapps.pocketchord        │ false     │ true           │
│ 2  │ com.sweetapps.pocketchord.debug  │ false     │ true           │
└────┴──────────────────────────────────┴───────────┴────────────────┘

notice_policy (2개 행)
┌────┬──────────────────────────────────┬───────────┬────────────────┐
│ id │ app_id                           │ is_active │ notice_version │
├────┼──────────────────────────────────┼───────────┼────────────────┤
│ 1  │ com.sweetapps.pocketchord        │ true      │ 1              │
│ 2  │ com.sweetapps.pocketchord.debug  │ false     │ 1              │
└────┴──────────────────────────────────┴───────────┴────────────────┘
```

---

## 🎯 Debug 데이터 사용 시나리오

### 시나리오 1: 개발 중 빠른 테스트

```kotlin
// BuildConfig.SUPABASE_APP_ID를 debug로 변경
// build.gradle.kts 또는 local.properties에서 설정
```

**장점**:
- ✅ 프로덕션 데이터 영향 없음
- ✅ 언제든 활성화/비활성화 가능
- ✅ 높은 버전으로 설정되어 즉시 테스트 가능

---

### 시나리오 2: 버전 관리 테스트 (notice)

```sql
-- 버전 1 테스트
UPDATE notice_policy 
SET is_active = true, notice_version = 1
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 앱 실행 → 공지 확인 → X 클릭

-- 버전 2로 증가
UPDATE notice_policy 
SET notice_version = 2, content = '[DEBUG] 버전 2 테스트'
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 앱 재실행 → 공지 다시 표시됨 확인 ✅
```

---

### 시나리오 3: X 버튼 테스트 (emergency)

```sql
-- X 버튼 있는 경우
UPDATE emergency_policy 
SET is_active = true, is_dismissible = true
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 앱 실행 → X 버튼 확인 → 클릭 가능 ✅

-- X 버튼 없는 경우
UPDATE emergency_policy 
SET is_dismissible = false
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 앱 재실행 → X 버튼 없음 확인 ✅
```

---

## 🎉 완료!

### 추가된 내용
- ✅ 3개 SQL 파일에 각각 debug 데이터 추가
- ✅ 모두 `is_active = false` (기본 비활성화)
- ✅ `[DEBUG]` 접두사로 식별 가능
- ✅ 프로덕션 데이터와 분리

### 다음 단계
1. **SQL 실행**: 3개 SQL 파일 순서대로 실행
2. **데이터 확인**: `SELECT * FROM ... WHERE app_id LIKE '%.debug'`
3. **테스트**: Debug 데이터 활성화 후 앱 실행

**이제 안전하게 테스트할 수 있습니다!** 🚀

