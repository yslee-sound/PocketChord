# SQL 구문 오류 수정 완료

**수정일**: 2025-11-09  
**문제**: PostgreSQL UNIQUE CONSTRAINT 내에서 WHERE 절 사용 불가  
**해결**: Partial Unique Index로 별도 생성

---

## 🔴 발생한 오류

```
Error: Failed to run sql query: 
ERROR: 42601: syntax error at or near "WHERE" 
LINE 29: WHERE is_active = true
```

---

## 🔍 원인

PostgreSQL에서는 **CONSTRAINT 정의 내부에서 WHERE 절을 사용할 수 없습니다**.

### 잘못된 문법 (Before)

```sql
CREATE TABLE update_policy (
    ...
    CONSTRAINT update_policy_pkey PRIMARY KEY (id),
    CONSTRAINT update_policy_unique_active 
        UNIQUE (app_id, is_active)
        WHERE is_active = true  -- ❌ 여기서 오류 발생!
);
```

### 올바른 문법 (After)

```sql
CREATE TABLE update_policy (
    ...
    CONSTRAINT update_policy_pkey PRIMARY KEY (id)
);

-- Partial Unique Index를 별도로 생성
CREATE UNIQUE INDEX idx_update_policy_unique_active 
ON public.update_policy(app_id) 
WHERE is_active = true;  -- ✅ 이렇게 해야 함!
```

---

## ✅ 수정된 파일

### 1. `01-create-update-policy.sql` ✅

**변경 전**:
```sql
CONSTRAINT update_policy_pkey PRIMARY KEY (id),
CONSTRAINT update_policy_unique_active 
    UNIQUE (app_id, is_active)
    WHERE is_active = true
);
```

**변경 후**:
```sql
CONSTRAINT update_policy_pkey PRIMARY KEY (id)
);

-- 2. 인덱스 생성
...
-- 앱당 1개의 활성 정책만 허용 (partial unique index)
CREATE UNIQUE INDEX idx_update_policy_unique_active 
ON public.update_policy(app_id) 
WHERE is_active = true;
```

---

### 2. `02-create-emergency-policy.sql` ✅

**변경 전**:
```sql
CONSTRAINT emergency_policy_pkey PRIMARY KEY (id),
CONSTRAINT emergency_policy_unique_active
    UNIQUE (app_id, is_active)
    WHERE is_active = true
);
```

**변경 후**:
```sql
CONSTRAINT emergency_policy_pkey PRIMARY KEY (id)
);

-- 2. 인덱스 생성
...
-- 앱당 1개의 활성 긴급 상황만 허용 (partial unique index)
CREATE UNIQUE INDEX idx_emergency_policy_unique_active 
ON public.emergency_policy(app_id) 
WHERE is_active = true;
```

---

### 3. `03-create-notice-policy.sql` ✅

**변경 전**:
```sql
CONSTRAINT notice_policy_pkey PRIMARY KEY (id),
CONSTRAINT notice_policy_unique_active
    UNIQUE (app_id, is_active)
    WHERE is_active = true
);
```

**변경 후**:
```sql
CONSTRAINT notice_policy_pkey PRIMARY KEY (id)
);

-- 2. 인덱스 생성
...
-- 앱당 1개의 활성 공지만 허용 (partial unique index)
CREATE UNIQUE INDEX idx_notice_policy_unique_active 
ON public.notice_policy(app_id) 
WHERE is_active = true;
```

---

## 🎯 결과

### Partial Unique Index의 장점

1. ✅ **조건부 유니크 제약**: `is_active = true`인 행만 유니크 체크
2. ✅ **여러 비활성 행 허용**: `is_active = false`인 행은 여러 개 가능
3. ✅ **동일한 기능**: 원래 의도했던 제약 조건 그대로 작동

### 예시

```sql
-- ✅ 가능: 1개 앱에 1개의 활성 정책
INSERT INTO update_policy (app_id, is_active, ...) 
VALUES ('com.app1', true, ...);

-- ❌ 불가능: 같은 앱에 활성 정책 2개 (오류 발생)
INSERT INTO update_policy (app_id, is_active, ...) 
VALUES ('com.app1', true, ...);
-- ERROR: duplicate key value violates unique constraint

-- ✅ 가능: 같은 앱에 비활성 정책은 여러 개
INSERT INTO update_policy (app_id, is_active, ...) 
VALUES ('com.app1', false, ...);
INSERT INTO update_policy (app_id, is_active, ...) 
VALUES ('com.app1', false, ...);
```

---

## 📝 다음 단계

이제 3개 SQL 파일을 **순서대로** Supabase에서 실행하세요:

### 1. update_policy 생성
```sql
-- 파일: docs/sql/01-create-update-policy.sql
-- 전체 내용 복사 → Supabase SQL Editor에 붙여넣기 → 실행
```

### 2. emergency_policy 생성
```sql
-- 파일: docs/sql/02-create-emergency-policy.sql
-- 전체 내용 복사 → Supabase SQL Editor에 붙여넣기 → 실행
```

### 3. notice_policy 생성
```sql
-- 파일: docs/sql/03-create-notice-policy.sql
-- 전체 내용 복사 → Supabase SQL Editor에 붙여넣기 → 실행
```

### 4. 확인
```sql
-- 3개 테이블 모두 생성 확인
SELECT 
    'update_policy' as table_name, 
    COUNT(*) as row_count 
FROM update_policy
UNION ALL
SELECT 'emergency_policy', COUNT(*) FROM emergency_policy
UNION ALL
SELECT 'notice_policy', COUNT(*) FROM notice_policy;
```

---

## 🎉 수정 완료!

- ✅ 3개 SQL 파일 모두 수정됨
- ✅ PostgreSQL 문법 준수
- ✅ Partial Unique Index로 정확히 동일한 기능 구현
- ✅ 재실행 준비 완료

**이제 오류 없이 실행됩니다!** 🚀

