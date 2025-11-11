# SQL 오류 수정: 테이블 이미 존재

**수정일**: 2025-11-09  
**오류**: `ERROR: 42P07: relation "update_policy" already exists`  
**해결**: `DROP TABLE IF EXISTS` 추가

---

## 🔴 발생한 오류

```
Error: Failed to run sql query: 
ERROR: 42P07: relation "update_policy" already exists
```

**원인**: 테이블을 이미 한 번 생성했거나 이전에 생성된 테이블이 존재함

---

## ✅ 해결 방법

모든 SQL 파일에 **`DROP TABLE IF EXISTS`** 구문을 추가하여 기존 테이블을 먼저 삭제합니다.

### 수정 전 (오류 발생)
```sql
-- 1. update_policy 테이블 생성
CREATE TABLE public.update_policy (
    ...
);
```

### 수정 후 (정상 동작)
```sql
-- 0. 기존 테이블 삭제 (있으면)
DROP TABLE IF EXISTS public.update_policy CASCADE;

-- 1. update_policy 테이블 생성
CREATE TABLE public.update_policy (
    ...
);
```

---

## 📝 수정된 파일 (3개)

### 1. `01-create-update-policy.sql` ✅
```sql
-- 0. 기존 테이블 삭제 (있으면)
DROP TABLE IF EXISTS public.update_policy CASCADE;
```

### 2. `02-create-emergency-policy.sql` ✅
```sql
-- 0. 기존 테이블 삭제 (있으면)
DROP TABLE IF EXISTS public.emergency_policy CASCADE;
```

### 3. `03-create-notice-policy.sql` ✅
```sql
-- 0. 기존 테이블 삭제 (있으면)
DROP TABLE IF EXISTS public.notice_policy CASCADE;
```

---

## 🎯 CASCADE의 의미

`CASCADE` 옵션은 해당 테이블을 참조하는 다른 객체(인덱스, 제약조건 등)도 함께 삭제합니다.

```sql
DROP TABLE IF EXISTS public.update_policy CASCADE;
```

- ✅ 테이블이 없으면 에러 없이 넘어감
- ✅ 테이블이 있으면 삭제
- ✅ 관련된 인덱스, 제약조건도 모두 삭제
- ✅ 다른 테이블의 외래 키도 함께 삭제 (만약 있다면)

---

## 🚀 이제 다시 실행하세요

### 순서대로 실행

#### 1. update_policy 생성
```sql
-- 파일: docs/sql/01-create-update-policy.sql
-- Supabase SQL Editor에서 실행
```

**예상 결과**:
```
DROP TABLE
CREATE TABLE
CREATE INDEX
CREATE INDEX
CREATE UNIQUE INDEX
ALTER TABLE
CREATE POLICY
COMMENT
COMMENT
COMMENT
INSERT 0 1
INSERT 0 1
status: update_policy 테이블 생성 완료!
```

#### 2. emergency_policy 생성
```sql
-- 파일: docs/sql/02-create-emergency-policy.sql
-- Supabase SQL Editor에서 실행
```

**예상 결과**:
```
DROP TABLE
CREATE TABLE
CREATE INDEX
CREATE INDEX
CREATE UNIQUE INDEX
ALTER TABLE
CREATE POLICY
COMMENT
COMMENT
INSERT 0 1
INSERT 0 1
status: emergency_policy 테이블 생성 완료!
```

#### 3. notice_policy 생성
```sql
-- 파일: docs/sql/03-create-notice-policy.sql
-- Supabase SQL Editor에서 실행
```

**예상 결과**:
```
DROP TABLE
CREATE TABLE
CREATE INDEX
CREATE INDEX
CREATE UNIQUE INDEX
ALTER TABLE
CREATE POLICY
COMMENT
COMMENT
INSERT 0 1
INSERT 0 1
status: notice_policy 테이블 생성 완료!
```

---

## ✅ 확인

### 3개 테이블 생성 확인
```sql
SELECT 
    'update_policy' as table_name, 
    COUNT(*) as row_count 
FROM update_policy
UNION ALL
SELECT 'emergency_policy', COUNT(*) FROM emergency_policy
UNION ALL
SELECT 'notice_policy', COUNT(*) FROM notice_policy;
```

**예상 결과**:
```
table_name          | row_count
--------------------+-----------
update_policy       | 2
emergency_policy    | 2
notice_policy       | 2
```

### 각 테이블 데이터 확인
```sql
-- update_policy
SELECT id, app_id, is_active, target_version_code, is_force_update 
FROM update_policy 
ORDER BY id;

-- emergency_policy
SELECT id, app_id, is_active, is_dismissible, LEFT(content, 30) as content_preview 
FROM emergency_policy 
ORDER BY id;

-- notice_policy
SELECT id, app_id, is_active, notice_version, title 
FROM notice_policy 
ORDER BY id;
```

---

## ⚠️ 주의사항

### DROP TABLE의 의미
- **기존 데이터가 모두 삭제됩니다!**
- 테스트 환경에서는 문제없음
- 프로덕션 환경에서는 주의 필요

### 안전한 사용
```sql
-- 개발/테스트 환경: DROP 사용 OK ✅
DROP TABLE IF EXISTS public.update_policy CASCADE;

-- 프로덕션 환경: DROP 대신 ALTER TABLE 사용 권장
-- ALTER TABLE public.update_policy ADD COLUMN ...;
```

---

## 🎉 수정 완료!

- ✅ 3개 SQL 파일에 `DROP TABLE IF EXISTS` 추가
- ✅ 이미 테이블이 있어도 에러 없이 재생성
- ✅ 여러 번 실행해도 문제없음 (멱등성)

**이제 오류 없이 실행됩니다!** 🚀

---
