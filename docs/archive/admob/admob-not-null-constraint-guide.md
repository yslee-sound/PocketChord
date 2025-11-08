# NOT NULL 제약 조건 추가 - 설명 문서

**날짜**: 2025-01-08  
**상태**: ✅ 권장 사항

---

## 🎯 문제

### 현재 상황
```sql
-- 현재 스키마
ad_app_open_enabled BOOLEAN DEFAULT true
ad_interstitial_enabled BOOLEAN DEFAULT true
ad_banner_enabled BOOLEAN DEFAULT true
```

**문제점:**
- `null` 값 허용됨
- 광고는 **표시(true) 또는 숨김(false)** 둘 중 하나여야 함
- `null`은 의미가 애매함 (표시? 숨김? 오류?)

---

## ✅ 해결

### 개선된 스키마
```sql
-- NOT NULL 제약 조건 추가
ad_app_open_enabled BOOLEAN DEFAULT true NOT NULL
ad_interstitial_enabled BOOLEAN DEFAULT true NOT NULL
ad_banner_enabled BOOLEAN DEFAULT true NOT NULL
```

**장점:**
1. **명확성**: true 또는 false만 가능
2. **안전성**: 실수로 null 입력 방지
3. **일관성**: 모든 레코드가 유효한 값 보유
4. **코드 단순화**: Kotlin에서 `?: true` 불필요 (하지만 안전을 위해 유지 권장)

---

## 🤔 null의 의미는?

### 3가지 상태 비교

| 값 | 의미 | 광고 표시 |
|----|------|----------|
| `true` | 활성화 | ✅ 표시 |
| `false` | 비활성화 | ❌ 숨김 |
| `null` | ⚠️ 미설정/오류 | ❓ 애매함 |

### 현재 코드 동작 (Kotlin)
```kotlin
val adBannerEnabled = policy?.adBannerEnabled ?: true
```

**null 처리:**
- `null` → 기본값 `true` (광고 표시)
- 안전하지만 DB 레벨에서 막는 게 더 좋음

---

## 📋 적용 방법

### 방법 1: 새로 설치하는 경우

**파일**: `docs/supabase-ad-control-schema.sql` (이미 수정됨)

```sql
-- 1. 컬럼 추가
ALTER TABLE app_policy
ADD COLUMN IF NOT EXISTS ad_banner_enabled BOOLEAN DEFAULT true;

-- 2. NULL 값 제거
UPDATE app_policy
SET ad_banner_enabled = COALESCE(ad_banner_enabled, true)
WHERE ad_banner_enabled IS NULL;

-- 3. NOT NULL 제약 조건 추가
ALTER TABLE app_policy
ALTER COLUMN ad_banner_enabled SET NOT NULL;
```

### 방법 2: 이미 적용한 경우

**파일**: `docs/supabase-ad-control-add-not-null.sql` (새로 생성됨)

**단계:**
1. 현재 NULL 값 확인
2. NULL 값을 기본값으로 업데이트
3. NOT NULL 제약 조건 추가

**실행:**
```sql
-- Supabase SQL Editor에서 실행
-- docs/supabase-ad-control-add-not-null.sql 파일 내용 복사
```

---

## 🧪 테스트

### 1. NOT NULL 제약 조건 확인

```sql
-- 테이블 구조 확인
SELECT 
  column_name, 
  data_type, 
  is_nullable  -- 'NO'여야 함
FROM information_schema.columns
WHERE table_name = 'app_policy'
AND column_name LIKE 'ad_%';
```

**기대 결과:**
```
column_name               | data_type | is_nullable
--------------------------+-----------+-------------
ad_app_open_enabled       | boolean   | NO
ad_interstitial_enabled   | boolean   | NO
ad_banner_enabled         | boolean   | NO
```

### 2. NULL 삽입 시도 (실패해야 정상)

```sql
-- 이제 오류가 발생해야 함
UPDATE app_policy 
SET ad_banner_enabled = NULL 
WHERE app_id = 'com.sweetapps.pocketchord';
```

**기대 오류:**
```
ERROR: null value in column "ad_banner_enabled" violates not-null constraint
```

✅ 이 오류가 나오면 성공!

---

## 🔄 마이그레이션 가이드

### 현재 상태 확인

```sql
-- 1. NULL 값이 있는지 확인
SELECT COUNT(*) 
FROM app_policy
WHERE ad_banner_enabled IS NULL;

-- 2. 제약 조건 확인
SELECT is_nullable
FROM information_schema.columns
WHERE table_name = 'app_policy'
AND column_name = 'ad_banner_enabled';
```

### 상황별 대응

**상황 A: NULL 값 없음 + NOT NULL 없음**
```sql
-- 바로 NOT NULL 추가 가능
ALTER TABLE app_policy
ALTER COLUMN ad_banner_enabled SET NOT NULL;
```

**상황 B: NULL 값 있음**
```sql
-- 1. 먼저 NULL 제거
UPDATE app_policy
SET ad_banner_enabled = true
WHERE ad_banner_enabled IS NULL;

-- 2. 그 다음 NOT NULL 추가
ALTER TABLE app_policy
ALTER COLUMN ad_banner_enabled SET NOT NULL;
```

**상황 C: 이미 NOT NULL 적용됨**
```sql
-- 아무 작업 불필요 ✅
SELECT 'Already done!' as status;
```

---

## 💻 Kotlin 코드 영향

### 현재 코드 (변경 불필요)

```kotlin
@SerialName("ad_banner_enabled")
val adBannerEnabled: Boolean = true,
```

**동작:**
- DB에서 `null` 오면 → 기본값 `true` 사용
- DB에서 `true` 오면 → `true`
- DB에서 `false` 오면 → `false`

### NOT NULL 적용 후

**변화:**
- DB에서 `null`이 절대 오지 않음 (DB 레벨에서 차단)
- 코드는 동일하게 동작
- 더 안전해짐 ✅

---

## ⚠️ 주의사항

### 1. 적용 순서 중요!

```sql
-- ❌ 잘못된 순서 (실패함)
ALTER COLUMN SET NOT NULL  -- NULL 값 있으면 에러!
UPDATE SET = true           -- 너무 늦음

-- ✅ 올바른 순서
UPDATE SET = true           -- 먼저 NULL 제거
ALTER COLUMN SET NOT NULL   -- 그 다음 제약 조건
```

### 2. 기존 레코드 확인

```sql
-- 적용 전 반드시 확인
SELECT * FROM app_policy
WHERE ad_banner_enabled IS NULL;
```

### 3. 롤백 방법

```sql
-- NOT NULL 제약 조건 제거 (필요시)
ALTER TABLE app_policy
ALTER COLUMN ad_banner_enabled DROP NOT NULL;
```

---

## 📊 영향 분석

### DB 레벨
- **제약 조건 추가**: NOT NULL
- **성능 영향**: 없음
- **저장 공간**: 변화 없음

### 애플리케이션 레벨
- **코드 변경**: 불필요
- **동작 변화**: 없음 (더 안전해짐)
- **기존 기능**: 모두 정상 동작

### 운영 레벨
- **장점**: 실수로 NULL 입력 방지
- **단점**: 없음
- **리스크**: 매우 낮음

---

## ✅ 체크리스트

NOT NULL 적용 전:
- [ ] 현재 NULL 값 확인
- [ ] NULL 값 제거 (있다면)
- [ ] NOT NULL 제약 조건 추가
- [ ] 테이블 구조 확인 (is_nullable = NO)
- [ ] NULL 삽입 테스트 (실패해야 함)

---

## 🎉 결론

**NOT NULL 제약 조건 추가 권장!**

**이유:**
1. 광고는 ON/OFF 둘 중 하나만 필요
2. NULL은 불필요하고 혼란만 줌
3. DB 레벨에서 안전성 보장
4. 코드 변경 불필요

**적용 방법:**
- 새로 설치: `supabase-ad-control-schema.sql` 사용
- 이미 설치: `supabase-ad-control-add-not-null.sql` 사용

---

**작성일**: 2025-01-08  
**상태**: ✅ 권장 사항

