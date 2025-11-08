# Supabase 테이블의 id 컬럼 이해하기

**날짜**: 2025-11-08  
**대상**: ad_policy, app_policy 테이블  
**목적**: id 컬럼의 역할과 사용법 이해

---

## 📋 id 컬럼이란?

### 정의
`id`는 **Primary Key (기본 키)** 로 사용되는 고유 식별자입니다.

```sql
CREATE TABLE ad_policy (
  id BIGSERIAL PRIMARY KEY,  -- ← 이것!
  created_at TIMESTAMP,
  app_id TEXT UNIQUE,
  ...
);
```

### 특징
- **타입**: `BIGSERIAL` (자동 증가하는 큰 정수)
- **값**: 1, 2, 3, 4... 자동으로 증가
- **고유성**: 절대 중복되지 않음
- **자동 생성**: INSERT 시 자동으로 값 할당

---

## 🔑 id의 3가지 역할

### 1. 고유 식별자
각 레코드(행)를 유일하게 구분합니다.

```
┌────┬─────────────────────────────────┬───────────┐
│ id │ app_id                          │ is_active │
├────┼─────────────────────────────────┼───────────┤
│ 1  │ com.sweetapps.pocketchord       │ true      │ ← id로 구분
│ 2  │ com.sweetapps.pocketchord.debug │ true      │ ← id로 구분
└────┴─────────────────────────────────┴───────────┘
```

### 2. 관계 설정 (Foreign Key)
다른 테이블에서 이 레코드를 참조할 때 사용합니다.

```sql
-- 예: 광고 정책 변경 이력 테이블
CREATE TABLE ad_policy_history (
  id BIGSERIAL PRIMARY KEY,
  ad_policy_id BIGINT REFERENCES ad_policy(id),  -- ← 관계 설정
  changed_at TIMESTAMP,
  changed_by TEXT,
  old_value JSONB,
  new_value JSONB
);

-- 사용 예
INSERT INTO ad_policy_history (ad_policy_id, changed_at, ...)
VALUES (1, NOW(), ...);  -- ← id = 1인 레코드의 변경 이력
```

### 3. 로그 및 감사 추적
어떤 레코드가 변경되었는지 추적합니다.

```sql
-- 감사 로그 테이블
CREATE TABLE audit_log (
  log_id BIGSERIAL PRIMARY KEY,
  table_name TEXT,
  record_id BIGINT,  -- ← 변경된 레코드의 id
  action TEXT,
  changed_at TIMESTAMP
);

-- 사용 예
INSERT INTO audit_log (table_name, record_id, action)
VALUES ('ad_policy', 1, 'updated');  -- ← id = 1이 변경됨
```

---

## 🆚 id vs app_id

### 우리 프로젝트의 경우

| 구분 | id | app_id |
|------|-----|---------|
| **타입** | BIGSERIAL (숫자) | TEXT (문자열) |
| **값** | 1, 2, 3... | `com.sweetapps.pocketchord` |
| **용도** | 데이터베이스 내부 | 비즈니스 로직 |
| **생성** | 자동 | 직접 지정 |
| **가독성** | 낮음 (1, 2, 3) | 높음 (의미 있는 이름) |
| **우리가 사용** | ❌ 거의 안 씀 | ✅ 주로 사용 |

### 코드에서의 사용

#### ❌ id로 찾지 않음
```kotlin
// 이렇게 하지 않음
val policy = client.from("ad_policy")
    .select()
    .eq("id", 1)  // ← id로 찾지 않음
    .decodeSingle<AdPolicy>()
```

#### ✅ app_id로 찾음
```kotlin
// 이렇게 함
val allPolicies = client.from("ad_policy")
    .select()
    .decodeList<AdPolicy>()

val policy = allPolicies.firstOrNull { 
    it.appId == "com.sweetapps.pocketchord"  // ← app_id로 찾음
}
```

#### SQL에서도 app_id 사용
```sql
-- ✅ 우리가 주로 사용하는 방법
SELECT * FROM ad_policy 
WHERE app_id = 'com.sweetapps.pocketchord';

-- ❌ id로는 거의 사용 안 함 (값을 모르니까)
SELECT * FROM ad_policy 
WHERE id = 1;  -- 1이 뭔지 모름
```

---

## 💡 왜 id와 app_id 둘 다 있나?

### id (Primary Key)
**장점**:
- ✅ 숫자라서 빠름 (성능 최적화)
- ✅ 관계 설정에 유리
- ✅ 데이터베이스 표준 관행
- ✅ 절대 중복 안 됨

**단점**:
- ❌ 의미 없음 (1, 2, 3이 뭔지 모름)
- ❌ 사람이 기억하기 어려움

### app_id (Unique Key)
**장점**:
- ✅ 의미 있음 (`com.sweetapps.pocketchord`)
- ✅ 비즈니스 로직에서 사용하기 쉬움
- ✅ 사람이 이해하기 쉬움

**단점**:
- ❌ 문자열이라 숫자보다 느림
- ❌ 길어서 저장 공간 더 필요

### 결론
**둘 다 사용하는 것이 베스트 프랙티스!**
- `id`: 데이터베이스 레벨 (내부 관리)
- `app_id`: 애플리케이션 레벨 (비즈니스 로직)

---

## 📊 실제 사용 예시

### 우리 테이블의 모습
```
ad_policy 테이블:
┌────┬─────────────────────────────────┬───────────┬─────────┬──────────┬─────────┐
│ id │ app_id                          │ is_active │ ad_open │ ad_inter │ ad_banner │
├────┼─────────────────────────────────┼───────────┼─────────┼──────────┼─────────┤
│ 1  │ com.sweetapps.pocketchord       │ true      │ true    │ true     │ true    │
│ 2  │ com.sweetapps.pocketchord.debug │ true      │ true    │ true     │ true    │
└────┴─────────────────────────────────┴───────────┴─────────┴──────────┴─────────┘
```

### 시나리오 1: 정책 조회 (app_id 사용)
```kotlin
// AdPolicyRepository.kt
suspend fun getPolicy(): Result<AdPolicy?> = runCatching {
    val allPolicies = client.from("ad_policy")
        .select()
        .decodeList<AdPolicy>()
    
    // app_id로 필터링 (id는 사용 안 함)
    val policy = allPolicies.firstOrNull { 
        it.appId == appId && it.isActive 
    }
    
    policy
}
```

### 시나리오 2: 정책 업데이트 (app_id 사용)
```sql
-- ✅ app_id로 업데이트 (의미 명확)
UPDATE ad_policy 
SET ad_banner_enabled = false 
WHERE app_id = 'com.sweetapps.pocketchord';

-- ❌ id로 업데이트 (어느 앱인지 모름)
UPDATE ad_policy 
SET ad_banner_enabled = false 
WHERE id = 1;  -- 이게 어느 앱이지?
```

### 시나리오 3: 로그 기록 (id 사용)
```sql
-- 감사 로그에는 id 사용 가능
INSERT INTO audit_log (table_name, record_id, action, timestamp)
VALUES ('ad_policy', 1, 'banner_disabled', NOW());

-- 나중에 조회
SELECT 
  a.action,
  a.timestamp,
  p.app_id  -- ← JOIN으로 app_id 가져오기
FROM audit_log a
JOIN ad_policy p ON a.record_id = p.id
WHERE a.table_name = 'ad_policy';
```

---

## 🎯 핵심 정리

### 1. id는 자동 관리됨
```sql
-- INSERT 시 id는 지정하지 않음
INSERT INTO ad_policy (app_id, is_active, ...)
VALUES ('com.sweetapps.pocketchord', true, ...);
-- ↑ id는 자동으로 1, 2, 3... 할당됨
```

### 2. 우리는 app_id를 사용
```kotlin
// Kotlin 모델
data class AdPolicy(
    val id: Long? = null,        // ← 있긴 하지만 거의 안 씀
    val appId: String,           // ← 이것만 주로 사용!
    val isActive: Boolean,
    ...
)
```

### 3. id는 신경 쓰지 않아도 됨
- ✅ Supabase가 자동으로 관리
- ✅ 우리는 app_id로 모든 작업 수행
- ✅ 코드에서 id 값을 직접 다룰 일 없음

---

## ❓ 자주 묻는 질문 (FAQ)

### Q1: id 값을 직접 지정할 수 있나요?
**A**: 가능하지만 권장하지 않습니다. Supabase가 자동으로 관리하도록 두세요.

```sql
-- ❌ 비권장
INSERT INTO ad_policy (id, app_id, ...) 
VALUES (100, 'com.example.app', ...);

-- ✅ 권장
INSERT INTO ad_policy (app_id, ...) 
VALUES ('com.example.app', ...);  -- id 자동 생성
```

### Q2: id 값이 꼭 1부터 시작하나요?
**A**: 일반적으로 1부터 시작하지만, 레코드를 삭제하면 중간에 빈 번호가 생길 수 있습니다.

```
삭제 전: 1, 2, 3, 4
삭제 후: 1, 3, 4     (2번이 사라짐)
새 추가: 1, 3, 4, 5  (5번으로 추가, 2번은 재사용 안 됨)
```

### Q3: app_id만 있으면 되는데 왜 id가 필요한가요?
**A**: 데이터베이스 성능과 관계 설정을 위해서입니다. 숫자(id)가 문자열(app_id)보다 빠르고, 관계 설정 시 유리합니다.

### Q4: 코드에서 id를 사용해야 할 때는?
**A**: 거의 없지만, 다른 테이블에서 Foreign Key로 참조할 때는 사용할 수 있습니다.

```kotlin
// 거의 이런 경우는 없지만...
data class AdPolicyHistory(
    val id: Long,
    val adPolicyId: Long,  // ← ad_policy의 id 참조
    val changedAt: String,
    ...
)
```

### Q5: Supabase UI에서 id를 보지 않으려면?
**A**: 테이블 뷰에서 id 컬럼을 숨길 수 있지만, 굳이 숨길 필요는 없습니다. 무시하면 됩니다.

---

## 📚 관련 문서

- `ad-policy-table-creation.sql` - 테이블 생성 스크립트
- `AdPolicy.kt` - Kotlin 모델 정의
- `AdPolicyRepository.kt` - 정책 조회 로직
- `ad-policy-separation-implementation-complete.md` - 전체 가이드

---

## ✅ 요약

### 한 문장 요약
**id는 데이터베이스가 자동으로 관리하는 내부 번호이고, 우리는 app_id를 사용합니다.**

### 기억할 것
1. ✅ id는 신경 쓰지 않아도 됨
2. ✅ app_id로 모든 작업 수행
3. ✅ id는 자동으로 증가
4. ✅ 코드에서 id 직접 사용할 일 거의 없음

### 실무 팁
```kotlin
// 이것만 기억하세요!
val policy = repository.getPolicy()  // id 신경 안 씀
```

```sql
-- 이것만 기억하세요!
SELECT * FROM ad_policy 
WHERE app_id = 'com.sweetapps.pocketchord';  -- app_id 사용
```

---

**작성일**: 2025-11-08  
**대상 독자**: PocketChord 개발팀  
**난이도**: 초급  
**키워드**: id, Primary Key, app_id, Supabase, 데이터베이스

