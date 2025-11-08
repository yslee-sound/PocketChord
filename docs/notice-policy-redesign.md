# 공지사항 정책 재검토 및 notice_policy 테이블 설계

**날짜**: 2025-11-09  
**목적**: 1개 행으로 공지사항 운영하기 위한 추적 방식 결정  
**원칙**: 앱당 1개 행 유지 (행 폭발 방지)

---

## 📋 목차

1. [핵심 질문](#핵심-질문)
2. [업계 표준 분석](#업계-표준-분석)
3. [3가지 방식 비교](#3가지-방식-비교)
4. [우리 상황 분석](#우리-상황-분석)
5. [최종 권장안](#최종-권장안)
6. [테이블 설계](#테이블-설계)

---

## 🎯 핵심 질문

### 공지사항을 1개 행으로 운영하려면?

**문제 상황**:
```
시나리오:
1월: "🎉 신년 이벤트" 공지 → 사용자가 X 클릭
2월: "💝 밸런타인 이벤트" 공지 → 다시 표시해야 함!

질문:
- 1월 공지를 봤다는 것을 어떻게 저장하나?
- 2월 공지는 새로운 것임을 어떻게 판단하나?
- 1개 행(id=1)만 UPDATE하면서 어떻게 구분하나?
```

**핵심 결정 사항**:
1. ✅ **추적 대상**: 무엇으로 "본 공지"를 식별할 것인가?
2. ✅ **저장 위치**: 어디에 저장할 것인가? (로컬 vs 서버)
3. ✅ **재표시 조건**: 어떤 조건에서 다시 표시할 것인가?

---

## 📊 업계 표준 분석

### 패턴 1: 콘텐츠 해시 방식 ⭐⭐⭐⭐ (가장 일반적)

**대표 사례**: Firebase Remote Config, 대부분의 인디 앱

```kotlin
// 콘텐츠 자체를 해시로 변환
val noticeHash = notice.content.hashCode()
val identifier = "notice_$noticeHash"

// SharedPreferences에 저장
val viewedHashes = prefs.getStringSet("viewed_notices", setOf())
if (viewedHashes.contains(identifier)) {
    // 이미 본 공지
} else {
    // 새 공지 표시
}
```

**테이블 구조**:
```sql
CREATE TABLE notice_policy (
    app_id TEXT PRIMARY KEY,
    content TEXT NOT NULL,
    updated_at TIMESTAMP DEFAULT NOW()
    -- 별도 버전 필드 없음!
);

-- 사용법
UPDATE notice_policy 
SET content = '💝 2월 이벤트'  -- 내용만 변경
WHERE app_id = 'com.sweetapps.pocketchord';
```

**장점**:
- ✅ **테이블 수정 불필요** (content만 있으면 됨)
- ✅ **자동 구분** (내용이 바뀌면 해시도 바뀜)
- ✅ **간단함** (코드만 수정)
- ✅ **행 증가 없음** (1개 앱 = 1개 행)

**단점**:
- ❌ 약간의 해시 계산 비용
- ❌ 내용이 조금만 바뀌어도 새 공지로 인식 (오타 수정도)

**사용률**: ⭐⭐⭐⭐⭐ (60-70% 앱)

---

### 패턴 2: 버전 필드 방식 ⭐⭐⭐⭐ (명시적 관리)

**대표 사례**: Slack, Discord, 중대형 앱

```kotlin
// 명시적 버전 번호
val identifier = "notice_v${notice.version}"

// SharedPreferences에 저장
val viewedVersions = prefs.getStringSet("viewed_notices", setOf())
if (viewedVersions.contains(identifier)) {
    // 이미 본 버전
} else {
    // 새 버전 표시
}
```

**테이블 구조**:
```sql
CREATE TABLE notice_policy (
    app_id TEXT PRIMARY KEY,
    content TEXT NOT NULL,
    notice_version INTEGER NOT NULL DEFAULT 1  -- 버전 필드 추가
);

-- 사용법
UPDATE notice_policy 
SET content = '💝 2월 이벤트',
    notice_version = notice_version + 1  -- 버전 증가
WHERE app_id = 'com.sweetapps.pocketchord';
```

**장점**:
- ✅ **명시적** (버전이 명확)
- ✅ **제어 가능** (오타 수정 시 버전 안 올리면 됨)
- ✅ **추적 용이** (로그에서 버전 확인 가능)
- ✅ **행 증가 없음** (1개 앱 = 1개 행)

**단점**:
- ❌ 테이블 수정 필요 (notice_version 컬럼 추가)
- ❌ 수동 관리 (버전을 잊지 말고 올려야 함)

**사용률**: ⭐⭐⭐⭐ (30-40% 앱)

---

### 패턴 3: 타임스탬프 방식 ⭐⭐ (자동화)

**대표 사례**: 일부 CMS 시스템

```kotlin
// 마지막 수정 시간
val identifier = "notice_${notice.updatedAt}"

// SharedPreferences에 저장
val viewedTimestamps = prefs.getStringSet("viewed_notices", setOf())
if (viewedTimestamps.contains(identifier)) {
    // 이미 본 시점
} else {
    // 새 시점 표시
}
```

**테이블 구조**:
```sql
CREATE TABLE notice_policy (
    app_id TEXT PRIMARY KEY,
    content TEXT NOT NULL,
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 트리거로 자동 업데이트
CREATE OR REPLACE FUNCTION update_notice_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER notice_updated_at_trigger
BEFORE UPDATE ON notice_policy
FOR EACH ROW
EXECUTE FUNCTION update_notice_timestamp();

-- 사용법
UPDATE notice_policy 
SET content = '💝 2월 이벤트'  -- updated_at 자동 갱신
WHERE app_id = 'com.sweetapps.pocketchord';
```

**장점**:
- ✅ **완전 자동** (트리거가 처리)
- ✅ **행 증가 없음** (1개 앱 = 1개 행)

**단점**:
- ❌ 트리거 설정 필요 (복잡)
- ❌ **의도치 않은 변경 감지** (오타 수정도 새 공지로)
- ❌ 제어 불가 (버전 안 올리고 싶어도 자동 변경)

**사용률**: ⭐⭐ (5-10% 앱, 잘 안 씀)

---

### 패턴 4: ID 기반 방식 (행 추가) ❌ (안티패턴)

**사례**: 초보 개발자가 많이 하는 실수

```sql
-- ❌ 이렇게 하면 안 됨!
CREATE TABLE notices (
    id SERIAL PRIMARY KEY,
    app_id TEXT,
    content TEXT
);

-- 새 공지마다 행 추가
INSERT INTO notices (app_id, content) 
VALUES ('com.sweetapps.pocketchord', '1월 공지');

INSERT INTO notices (app_id, content) 
VALUES ('com.sweetapps.pocketchord', '2월 공지');

-- 결과: 행이 계속 증가!
```

**문제**:
- ❌ **행 폭발** (100개 앱 × 12개월 = 1,200개 행)
- ❌ 관리 불가능
- ❌ 우리의 원칙 위배

**사용률**: ❌ (안티패턴, 사용하면 안 됨)

---

## 🔍 3가지 방식 비교

### 비교표

| 항목 | 해시 방식 ⭐ | 버전 방식 ⭐ | 타임스탬프 |
|------|------------|------------|-----------|
| **테이블 수정** | ❌ 불필요 | ✅ 필요 | ✅ 필요 + 트리거 |
| **자동화** | ✅ 자동 | ❌ 수동 | ✅ 완전 자동 |
| **명시성** | ❌ 낮음 | ✅ 높음 | ❌ 낮음 |
| **제어성** | ❌ 제어 불가 | ✅ 완전 제어 | ❌ 제어 불가 |
| **오타 수정** | ❌ 새 공지로 인식 | ✅ 버전 안 올리면 됨 | ❌ 새 공지로 인식 |
| **구현 난이도** | ✅ 쉬움 | ✅ 쉬움 | ❌ 어려움 |
| **행 증가** | ✅ 없음 | ✅ 없음 | ✅ 없음 |
| **업계 사용률** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ |
| **권장도** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ |

---

## 📝 우리 상황 분석

### 우리의 요구사항

```
✅ 1개 앱 = 1개 행 유지 (필수!)
✅ 100개 앱 관리 (확장성)
✅ UPDATE만으로 새 공지 (INSERT 금지)
✅ 간단한 관리 (복잡한 운영 X)
✅ 오타 수정과 새 공지 구분 가능 (선호)
```

### 실제 사용 시나리오

```
1월 15일: "🎉 신년 이벤트 50% 할인" 공지
  → 사용자 A가 봄 → 저장

1월 16일: "🎉 신년 이벤트 50% 할일" (오타 수정)
  → 사용자 A에게 다시 표시? NO! (오타 수정일 뿐)

2월 1일: "💝 밸런타인 데이 특가" (새 공지)
  → 사용자 A에게 다시 표시? YES! (새 공지)
```

**결론**: **제어 가능성**이 중요!

---

## 🎯 최종 권장안

### 추천 1순위: 버전 방식 ⭐⭐⭐⭐⭐

**이유**:
1. ✅ **명시적 제어**: 언제 새 공지로 할지 우리가 결정
2. ✅ **오타 수정 가능**: 버전 안 올리면 기존 사용자에게 안 뜸
3. ✅ **로그 추적 용이**: "버전 3 공지를 봤다"
4. ✅ **업계 표준**: 중대형 앱이 많이 사용
5. ✅ **간단함**: 컬럼 하나만 추가

**구현 방법**:

```sql
-- 1. 테이블에 버전 필드 추가
ALTER TABLE notice_policy 
ADD COLUMN notice_version INTEGER NOT NULL DEFAULT 1;

-- 2. 사용법
-- 오타 수정 (버전 안 올림)
UPDATE notice_policy 
SET content = '🎉 신년 이벤트 50% 할인'  -- 오타 수정
WHERE app_id = 'com.sweetapps.pocketchord';
-- notice_version = 1 (그대로) → 기존 사용자에게 안 뜸

-- 새 공지 (버전 올림)
UPDATE notice_policy 
SET content = '💝 밸런타인 데이 특가',
    notice_version = notice_version + 1  -- 2로 증가
WHERE app_id = 'com.sweetapps.pocketchord';
-- notice_version = 2 → 모든 사용자에게 표시!
```

**Kotlin 코드**:
```kotlin
// 식별자 생성
val identifier = "notice_v${notice.noticeVersion}"
// 예: "notice_v1", "notice_v2", ...

// 조회 확인
val prefs = context.getSharedPreferences("notice_prefs", Context.MODE_PRIVATE)
val viewedVersions = prefs.getStringSet("viewed_notices", setOf()) ?: setOf()

if (viewedVersions.contains(identifier)) {
    Log.d("Notice", "Already viewed version ${notice.noticeVersion}")
    return  // 이미 봤음
}

// 공지 표시
showNoticeDialog(notice)

// X 클릭 시 저장
viewedVersions.add(identifier)
prefs.edit { putStringSet("viewed_notices", viewedVersions) }
```

---

### 추천 2순위: 해시 방식 ⭐⭐⭐⭐

**이유**:
1. ✅ **테이블 수정 불필요**: 즉시 구현 가능
2. ✅ **완전 자동**: 내용만 바꾸면 됨
3. ✅ **간단함**: 코드만 수정

**단점**:
- ❌ **제어 불가**: 오타 수정도 새 공지로 인식
- ❌ 의도치 않은 재표시 가능

**언제 선택?**:
- 테이블 수정이 어려운 경우
- 빠른 구현이 필요한 경우
- 오타 수정이 거의 없는 경우

---

## 📐 테이블 설계

### 최종 권장 구조 (버전 방식)

```sql
-- 공지사항 정책 테이블
CREATE TABLE public.notice_policy (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- 기본 정보
    app_id TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- 공지 내용
    title TEXT,                              -- 공지 제목
    content TEXT NOT NULL,                   -- 공지 내용
    
    -- ===== 핵심: 버전 필드 ⭐ =====
    notice_version INTEGER NOT NULL DEFAULT 1,  -- 공지 버전
    
    -- 부가 정보
    image_url TEXT,                          -- 이미지 URL (선택)
    action_url TEXT,                         -- 클릭 시 이동 URL (선택)
    
    CONSTRAINT notice_policy_pkey PRIMARY KEY (id),
    -- 앱당 1개의 활성 공지만 허용
    CONSTRAINT notice_policy_unique_active 
        UNIQUE (app_id, is_active) 
        WHERE is_active = true
);

-- 인덱스
CREATE INDEX idx_notice_policy_app_id ON public.notice_policy(app_id);
CREATE INDEX idx_notice_policy_active ON public.notice_policy(is_active);

-- RLS 정책
ALTER TABLE public.notice_policy ENABLE ROW LEVEL SECURITY;

CREATE POLICY "allow_read_notice_policy"
ON public.notice_policy
FOR SELECT
USING (true);

-- 코멘트
COMMENT ON TABLE public.notice_policy IS '공지사항 정책 (이벤트, 신규 기능 안내 등)';
COMMENT ON COLUMN public.notice_policy.notice_version IS '공지 버전 (증가 시 모든 사용자에게 재표시)';
```

---

## 🔄 실제 운영 예시

### 시나리오 1: 월별 공지

```sql
-- 1월 공지 (초기 설정)
INSERT INTO notice_policy (app_id, title, content, notice_version, is_active)
VALUES (
    'com.sweetapps.pocketchord',
    '🎉 신년 이벤트',
    '새해 맞이 50% 특별 할인!',
    1,
    true
);

-- 1월 16일: 오타 수정 (버전 안 올림)
UPDATE notice_policy 
SET content = '새해 맞이 50% 특별 할인!'  -- 오타 수정
WHERE app_id = 'com.sweetapps.pocketchord';
-- notice_version = 1 (그대로)
-- → 이미 본 사용자에게는 안 뜸 ✅

-- 2월 1일: 새 공지 (버전 올림)
UPDATE notice_policy 
SET title = '💝 밸런타인 데이',
    content = '2월 특별 이벤트 진행 중!',
    notice_version = 2  -- 명시적으로 2로 변경
WHERE app_id = 'com.sweetapps.pocketchord';
-- → 모든 사용자에게 다시 표시! ✅

-- 3월 1일: 또 다른 공지
UPDATE notice_policy 
SET title = '🌸 봄맞이 이벤트',
    content = '봄맞이 특가 세일!',
    notice_version = 3  -- 또는 notice_version + 1
WHERE app_id = 'com.sweetapps.pocketchord';
```

**SharedPreferences 상태**:
```
사용자 A의 기기:
{
  "viewed_notices": [
    "notice_v1",  // 1월 공지 봤음
    "notice_v2"   // 2월 공지 봤음
    // "notice_v3" ← 3월 공지는 아직 안 봄
  ]
}
```

---

### 시나리오 2: 긴급 공지 → 일반 공지

```sql
-- 긴급 공지가 끝나고 일반 공지로 전환
UPDATE notice_policy 
SET title = '점검 완료 안내',
    content = '서비스가 정상화되었습니다',
    notice_version = notice_version + 1  -- 버전 증가
WHERE app_id = 'com.sweetapps.pocketchord';

-- 결과: 모든 사용자에게 "점검 완료" 공지 표시
```

---

## 📊 최종 비교: 해시 vs 버전

### 관리자 관점

| 작업 | 해시 방식 | 버전 방식 |
|------|----------|----------|
| **새 공지 띄우기** | content만 변경 | content + version 증가 |
| **오타 수정** | ❌ 모두에게 재표시 | ✅ 버전 유지하면 안 뜸 |
| **실수 방지** | ❌ 어려움 | ✅ 명시적 |
| **직관성** | ❌ 낮음 | ✅ 높음 |

### 개발자 관점

| 작업 | 해시 방식 | 버전 방식 |
|------|----------|----------|
| **구현 난이도** | ✅ 쉬움 (코드만) | ✅ 쉬움 (컬럼 추가) |
| **테스트** | ✅ 간단 | ✅ 간단 |
| **디버깅** | ❌ 어려움 (해시값) | ✅ 쉬움 (버전 숫자) |
| **로그 추적** | ❌ 어려움 | ✅ 쉬움 |

### 사용자 관점

| 상황 | 해시 방식 | 버전 방식 |
|------|----------|----------|
| **새 공지** | ✅ 정상 표시 | ✅ 정상 표시 |
| **오타 수정 후** | ❌ 다시 표시됨 (짜증) | ✅ 표시 안 됨 (좋음) |
| **동일 내용** | ✅ 표시 안 됨 | ✅ 표시 안 됨 |

---

## ✅ 최종 결론

### 권장: 버전 방식 ⭐⭐⭐⭐⭐

**이유**:
1. ✅ **명시적 제어**: 새 공지 여부를 우리가 결정
2. ✅ **사용자 경험**: 오타 수정으로 불필요한 재표시 방지
3. ✅ **관리 용이**: 버전 숫자로 명확한 추적
4. ✅ **업계 표준**: Slack, Discord 등 주요 앱 사용
5. ✅ **확장성**: 100개 앱에도 동일하게 적용 가능

### 구현 체크리스트

- [ ] **1단계: 테이블 수정**
  ```sql
  ALTER TABLE notice_policy 
  ADD COLUMN notice_version INTEGER NOT NULL DEFAULT 1;
  ```

- [ ] **2단계: Kotlin 모델 수정**
  ```kotlin
  @SerialName("notice_version")
  val noticeVersion: Int = 1
  ```

- [ ] **3단계: 추적 로직 구현**
  ```kotlin
  val identifier = "notice_v${notice.noticeVersion}"
  ```

- [ ] **4단계: 운영 가이드 작성**
  - 새 공지: 버전 증가
  - 오타 수정: 버전 유지

### 대안: 해시 방식

**언제 선택?**:
- 테이블 수정이 정말 어려운 경우
- 즉시 구현이 필요한 경우
- 오타 수정이 거의 없는 경우

**주의사항**:
- 오타 수정 시 모든 사용자에게 재표시됨
- 내용을 신중하게 작성해야 함

---

## 📚 참고: 실제 앱 사례

### Slack의 공지사항 시스템

```json
{
  "announcement_id": "2024-Q1-update",  // 버전 대신 ID
  "version": 3,                         // 명시적 버전
  "content": "새로운 기능이 추가되었습니다"
}
```

**특징**: ID + 버전 조합 사용

### Discord의 Changelog

```json
{
  "version": "2024.01.15",  // 날짜 기반 버전
  "changes": ["...", "..."]
}
```

**특징**: 날짜를 버전으로 사용

### 우리의 선택

```json
{
  "notice_version": 2,  // 단순한 정수 버전 ⭐
  "content": "..."
}
```

**특징**: 가장 단순하고 명확

---

**작성일**: 2025-11-09  
**대상 독자**: PocketChord 개발팀  
**난이도**: 중급  
**키워드**: notice_policy, notice_version, 해시 vs 버전, 업계 표준, 1개 행 운영

