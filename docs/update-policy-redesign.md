# 업데이트 정책 재검토 및 update_policy 테이블 설계

**날짜**: 2025-11-09  
**목적**: 업데이트 정책 필드 구조 재검토 및 테이블 분리  
**현재 문제**: min_supported_version과 latest_version_code의 혼란스러운 구조

---

## 📋 목차

1. [현재 구조 분석](#현재-구조-분석)
2. [문제점 분석](#문제점-분석)
3. [다른 앱의 구조 비교](#다른-앱의-구조-비교)
4. [최적 구조 제안](#최적-구조-제안)
5. [update_policy 테이블 설계](#update_policy-테이블-설계)
6. [마이그레이션 가이드](#마이그레이션-가이드)

---

## 🔍 현재 구조 분석

### app_policy 테이블의 업데이트 관련 필드

```kotlin
@Serializable
data class AppPolicy(
    // ... 다른 필드들 ...
    
    @SerialName("active_popup_type")
    val activePopupType: String = "none",  // 'force_update', 'optional_update', 'notice', 'emergency', 'none'
    
    @SerialName("min_supported_version")
    val minSupportedVersion: Int? = null,    // ← 강제 업데이트용
    
    @SerialName("latest_version_code")
    val latestVersionCode: Int? = null,      // ← 선택적 업데이트용
    
    @SerialName("content")
    val content: String? = null,             // 업데이트 메시지
    
    @SerialName("download_url")
    val downloadUrl: String? = null          // 스토어 링크
) {
    // 강제 업데이트 확인
    fun requiresForceUpdate(currentVersionCode: Int): Boolean {
        if (activePopupType != "force_update") return false
        val min = minSupportedVersion ?: return false
        return currentVersionCode < min
    }
    
    // 선택적 업데이트 확인
    fun recommendsUpdate(currentVersionCode: Int): Boolean {
        if (activePopupType != "optional_update") return false
        val latest = latestVersionCode ?: return false
        return currentVersionCode < latest
    }
}
```

### 사용 예시 (현재)

```sql
-- 강제 업데이트 (버전 10 미만 차단)
UPDATE app_policy 
SET active_popup_type = 'force_update',
    min_supported_version = 10,
    latest_version_code = NULL,  -- 사용 안 함
    content = '앱을 업데이트해주세요'
WHERE app_id = 'com.sweetapps.pocketchord';

-- 선택적 업데이트 (버전 15 권장)
UPDATE app_policy 
SET active_popup_type = 'optional_update',
    min_supported_version = NULL,  -- 사용 안 함
    latest_version_code = 15,
    content = '새로운 기능이 추가되었습니다'
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

## 🔴 문제점 분석

### 문제 1: 필드명이 직관적이지 않음

```
❌ min_supported_version
   → "최소 지원 버전"? "최소 버전"?
   → 이게 강제 업데이트 기준인지 명확하지 않음

❌ latest_version_code
   → "최신 버전 코드"? 
   → 이게 선택적 업데이트용인지 명확하지 않음
```

**혼란스러운 시나리오**:
```sql
-- 이게 맞나? 틀렸나?
UPDATE app_policy 
SET min_supported_version = 15,  -- 최신 버전?
    latest_version_code = 10     -- 최소 버전?
WHERE ...;

-- 정답: 완전히 반대!
-- min_supported_version = 10 (강제 업데이트 기준)
-- latest_version_code = 15 (권장 버전)
```

---

### 문제 2: 2개 필드의 역할이 중복됨

```
강제 업데이트:
  currentVersion < minSupportedVersion

선택적 업데이트:
  currentVersion < latestVersionCode

→ 둘 다 "현재 버전 < 목표 버전" 비교!
→ 로직이 동일한데 왜 필드가 2개?
```

**논리적 관계**:
```
현재 앱 버전: 8

Case 1: 강제 업데이트만
  min_supported_version = 10
  latest_version_code = NULL
  → 8 < 10 → 강제 업데이트 팝업

Case 2: 선택적 업데이트만
  min_supported_version = NULL
  latest_version_code = 12
  → 8 < 12 → 선택적 업데이트 팝업

Case 3: 둘 다 설정?
  min_supported_version = 10
  latest_version_code = 12
  → 8 < 10 → 강제 업데이트 (우선순위)
  → active_popup_type이 결정 ('force_update' vs 'optional_update')
```

**문제**: 
- `active_popup_type`으로 이미 구분하는데 왜 필드가 2개?
- NULL 값 관리가 복잡함
- 어느 필드에 값을 넣어야 하는지 헷갈림

---

### 문제 3: 테이블 책임 과다

```
app_policy 테이블이 담당하는 것:
✅ 강제 업데이트
✅ 선택적 업데이트
✅ 일반 공지
✅ 긴급 공지
✅ 광고 제어 정책

→ 너무 많은 책임! (Single Responsibility Principle 위반)
```

---

## 📊 다른 앱의 구조 비교

### 패턴 1: 단일 필드 방식 ⭐ (가장 일반적)

**많은 앱이 사용하는 방식**:

```kotlin
data class UpdatePolicy(
    val targetVersionCode: Int,    // 목표 버전 (하나만!)
    val isForceUpdate: Boolean,    // 강제 여부
    val message: String,
    val downloadUrl: String?
)

// 사용법
fun needsUpdate(currentVersion: Int): Boolean {
    return currentVersion < targetVersionCode
}
```

**Supabase 테이블**:
```sql
CREATE TABLE update_policy (
    id BIGINT PRIMARY KEY,
    app_id TEXT NOT NULL,
    target_version_code INT NOT NULL,  -- 단일 필드!
    is_force_update BOOLEAN NOT NULL,  -- 강제 여부
    message TEXT,
    download_url TEXT,
    is_active BOOLEAN DEFAULT TRUE
);
```

**장점**:
- ✅ **직관적**: "목표 버전"이라는 단 하나의 개념
- ✅ **간단**: NULL 처리 불필요
- ✅ **명확**: 어느 필드에 값을 넣어야 하는지 고민 불필요

**사용 예시**:
```sql
-- 강제 업데이트 (버전 10 미만 차단)
UPDATE update_policy 
SET target_version_code = 10,
    is_force_update = true,
    message = '필수 업데이트입니다'
WHERE app_id = 'com.sweetapps.pocketchord';

-- 선택적 업데이트 (버전 12 권장)
UPDATE update_policy 
SET target_version_code = 12,
    is_force_update = false,
    message = '새로운 기능이 추가되었습니다'
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

### 패턴 2: 분리된 테이블 방식

```sql
-- 강제 업데이트 전용
CREATE TABLE force_update_policy (
    app_id TEXT PRIMARY KEY,
    min_version_code INT NOT NULL,
    message TEXT
);

-- 선택적 업데이트 전용
CREATE TABLE optional_update_policy (
    app_id TEXT PRIMARY KEY,
    recommended_version_code INT NOT NULL,
    message TEXT
);
```

**장점**:
- ✅ 명확한 분리

**단점**:
- ❌ 테이블이 너무 많아짐
- ❌ 유사한 구조의 중복

---

### 패턴 3: 이중 필드 방식 (현재 우리 방식)

```sql
CREATE TABLE app_policy (
    min_supported_version INT,
    latest_version_code INT,
    active_popup_type TEXT
);
```

**단점**:
- ❌ **혼란스러움**: 어느 필드를 사용해야 하는지 불명확
- ❌ **중복 로직**: 둘 다 비교 로직이 동일
- ❌ **NULL 처리**: 사용하지 않는 필드는 NULL로 관리
- ❌ **실수 가능성**: 두 필드에 모두 값을 넣으면?

---

## ✅ 최적 구조 제안

### 권장: 패턴 1 (단일 필드 + Boolean) ⭐⭐⭐⭐⭐

**이유**:
1. ✅ **직관적**: `target_version_code` 하나만 관리
2. ✅ **간단**: NULL 처리 불필요
3. ✅ **명확**: 강제/선택은 `is_force_update`로 구분
4. ✅ **일반적**: 대부분의 앱이 이 방식 사용
5. ✅ **유지보수**: 코드가 단순해짐

### 비교표

| 항목 | 현재 (이중 필드) | 제안 (단일 필드) |
|------|-----------------|-----------------|
| **필드 수** | 2개 (min, latest) | 1개 (target) |
| **직관성** | ❌ 낮음 (헷갈림) | ✅ 높음 (명확) |
| **NULL 처리** | ❌ 필요 | ✅ 불필요 |
| **실수 가능성** | ❌ 높음 | ✅ 낮음 |
| **코드 복잡도** | ❌ 높음 | ✅ 낮음 |
| **일반성** | ❌ 드묾 | ✅ 일반적 |

---

## 🎯 update_policy 테이블 설계

### 최종 제안 구조

```sql
-- 업데이트 정책 전용 테이블
CREATE TABLE public.update_policy (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- 기본 정보
    app_id TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- ===== 핵심 필드 (단순화) =====
    target_version_code INT NOT NULL,        -- 목표 버전 (단일 필드!)
    is_force_update BOOLEAN NOT NULL,        -- 강제 업데이트 여부
    
    -- 부가 정보
    message TEXT,                            -- 업데이트 메시지
    release_notes TEXT,                      -- 릴리즈 노트 (선택적)
    download_url TEXT,                       -- 스토어 링크
    
    CONSTRAINT update_policy_pkey PRIMARY KEY (id),
    -- 앱당 1개의 활성 정책만 허용
    CONSTRAINT update_policy_unique_active 
        UNIQUE (app_id, is_active) 
        WHERE is_active = true
);

-- 인덱스
CREATE INDEX idx_update_policy_app_id ON public.update_policy(app_id);
CREATE INDEX idx_update_policy_active ON public.update_policy(is_active);

-- RLS 정책
ALTER TABLE public.update_policy ENABLE ROW LEVEL SECURITY;

CREATE POLICY "allow_read_update_policy"
ON public.update_policy
FOR SELECT
USING (true);

-- 코멘트
COMMENT ON TABLE public.update_policy IS '앱 업데이트 정책 (강제/선택적 업데이트)';
COMMENT ON COLUMN public.update_policy.target_version_code IS '목표 버전 코드 (현재 버전 < 목표 버전이면 업데이트 필요)';
COMMENT ON COLUMN public.update_policy.is_force_update IS '강제 업데이트 여부 (true: 강제, false: 선택적)';
```

---

## 📝 사용 예시 (신규 구조)

### Supabase 관리

```sql
-- ===== 1. 강제 업데이트 (버전 10 미만 차단) =====
UPDATE update_policy 
SET target_version_code = 10,              -- 목표 버전: 10
    is_force_update = true,                -- 강제!
    message = '보안 업데이트가 필요합니다.\n지금 업데이트해주세요.',
    release_notes = '• 보안 취약점 수정\n• 버그 수정',
    download_url = NULL                    -- NULL이면 기본 스토어
WHERE app_id = 'com.sweetapps.pocketchord';

-- 결과:
-- - 버전 8 사용자: 8 < 10 → 강제 업데이트 팝업 (닫기 불가)
-- - 버전 9 사용자: 9 < 10 → 강제 업데이트 팝업
-- - 버전 10 사용자: 10 >= 10 → 팝업 없음
-- - 버전 11 사용자: 11 >= 10 → 팝업 없음


-- ===== 2. 선택적 업데이트 (버전 12 권장) =====
UPDATE update_policy 
SET target_version_code = 12,              -- 목표 버전: 12
    is_force_update = false,               -- 선택적!
    message = '새로운 기능이 추가되었습니다',
    release_notes = '• 다크 모드 지원\n• 성능 개선\n• 새로운 코드 추가',
    download_url = NULL
WHERE app_id = 'com.sweetapps.pocketchord';

-- 결과:
-- - 버전 10 사용자: 10 < 12 → 선택적 업데이트 팝업 (닫기 가능)
-- - 버전 11 사용자: 11 < 12 → 선택적 업데이트 팝업
-- - 버전 12 사용자: 12 >= 12 → 팝업 없음


-- ===== 3. 업데이트 없음 (정상 운영) =====
UPDATE update_policy 
SET is_active = false  -- 비활성화
WHERE app_id = 'com.sweetapps.pocketchord';

-- 또는 target_version_code를 낮게 설정
UPDATE update_policy 
SET target_version_code = 1,  -- 모든 버전이 1 이상이므로 팝업 없음
    is_active = true
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

## 💻 Kotlin 모델 (신규)

### UpdatePolicy.kt

```kotlin
package com.sweetapps.pocketchord.data.supabase.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 업데이트 정책 모델
 *
 * 단순화된 구조:
 * - target_version_code: 목표 버전 (하나의 필드로 통일)
 * - is_force_update: 강제/선택적 구분
 */
@Serializable
data class UpdatePolicy(
    @SerialName("id")
    val id: Long? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("app_id")
    val appId: String,

    @SerialName("is_active")
    val isActive: Boolean = false,

    // ===== 핵심 필드 (단순화) =====
    @SerialName("target_version_code")
    val targetVersionCode: Int,           // 목표 버전 (단일 필드!)

    @SerialName("is_force_update")
    val isForceUpdate: Boolean,           // 강제 여부

    // ===== 부가 정보 =====
    @SerialName("message")
    val message: String? = null,

    @SerialName("release_notes")
    val releaseNotes: String? = null,

    @SerialName("download_url")
    val downloadUrl: String? = null
) {
    /**
     * 업데이트가 필요한지 확인
     * @param currentVersionCode 현재 앱 버전 코드
     * @return true: 업데이트 필요, false: 업데이트 불필요
     */
    fun needsUpdate(currentVersionCode: Int): Boolean {
        return currentVersionCode < targetVersionCode
    }

    /**
     * 강제 업데이트가 필요한지 확인
     */
    fun requiresForceUpdate(currentVersionCode: Int): Boolean {
        return isForceUpdate && needsUpdate(currentVersionCode)
    }

    /**
     * 선택적 업데이트 권장 여부
     */
    fun recommendsOptionalUpdate(currentVersionCode: Int): Boolean {
        return !isForceUpdate && needsUpdate(currentVersionCode)
    }
}
```

**사용 예시**:
```kotlin
val policy = updatePolicyRepository.getPolicy()

when {
    policy.requiresForceUpdate(currentVersion) -> {
        // 강제 업데이트 팝업 (닫기 불가)
        showForceUpdateDialog()
    }
    policy.recommendsOptionalUpdate(currentVersion) -> {
        // 선택적 업데이트 팝업 (닫기 가능)
        showOptionalUpdateDialog()
    }
    else -> {
        // 업데이트 불필요
    }
}
```

---

## 🔄 마이그레이션 가이드

### 1단계: update_policy 테이블 생성

```sql
-- 새 테이블 생성
CREATE TABLE public.update_policy (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    app_id TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    target_version_code INT NOT NULL,
    is_force_update BOOLEAN NOT NULL,
    message TEXT,
    release_notes TEXT,
    download_url TEXT,
    CONSTRAINT update_policy_pkey PRIMARY KEY (id),
    CONSTRAINT update_policy_unique_active 
        UNIQUE (app_id, is_active) 
        WHERE is_active = true
);

CREATE INDEX idx_update_policy_app_id ON public.update_policy(app_id);
CREATE INDEX idx_update_policy_active ON public.update_policy(is_active);

ALTER TABLE public.update_policy ENABLE ROW LEVEL SECURITY;

CREATE POLICY "allow_read_update_policy"
ON public.update_policy
FOR SELECT
USING (true);
```

---

### 2단계: 데이터 마이그레이션 (선택적)

```sql
-- app_policy에서 force_update 데이터 마이그레이션
INSERT INTO update_policy (
    app_id, 
    is_active, 
    target_version_code, 
    is_force_update, 
    message, 
    download_url
)
SELECT 
    app_id,
    is_active,
    min_supported_version,  -- → target_version_code
    true,                   -- is_force_update
    content,                -- → message
    download_url
FROM app_policy
WHERE active_popup_type = 'force_update'
  AND min_supported_version IS NOT NULL;

-- optional_update 데이터 마이그레이션
INSERT INTO update_policy (
    app_id, 
    is_active, 
    target_version_code, 
    is_force_update, 
    message, 
    download_url
)
SELECT 
    app_id,
    is_active,
    latest_version_code,    -- → target_version_code
    false,                  -- is_force_update
    content,                -- → message
    download_url
FROM app_policy
WHERE active_popup_type = 'optional_update'
  AND latest_version_code IS NOT NULL;
```

---

### 3단계: app_policy 정리 (선택적)

```sql
-- 업데이트 관련 컬럼 제거 (선택적, 추후)
ALTER TABLE app_policy 
DROP COLUMN IF EXISTS min_supported_version,
DROP COLUMN IF EXISTS latest_version_code,
DROP COLUMN IF EXISTS active_popup_type;  -- emergency와 notice는 별도 테이블로

-- 또는 컬럼은 유지하고 광고 정책 전용으로 사용
-- (마이그레이션 기간 동안 병행 운영)
```

---

## 📊 최종 비교: 현재 vs 제안

### 필드 구조 비교

```
현재 (app_policy):
┌─────────────────────────────────────────────────┐
│ active_popup_type: 'force_update' | 'optional_update' │
│ min_supported_version: 10 | NULL                │  ← 헷갈림!
│ latest_version_code: NULL | 15                  │  ← 헷갈림!
└─────────────────────────────────────────────────┘

제안 (update_policy):
┌─────────────────────────────────────────────────┐
│ target_version_code: 12                         │  ← 명확!
│ is_force_update: true | false                   │  ← 명확!
└─────────────────────────────────────────────────┘
```

### 사용 편의성 비교

```sql
-- ❌ 현재: 어느 필드에 값을 넣어야 하는지 혼란
UPDATE app_policy 
SET active_popup_type = 'force_update',
    min_supported_version = 10,  -- 이거? 
    latest_version_code = NULL   -- 아니면 이거? → NULL로 설정!
WHERE ...;

-- ✅ 제안: 직관적이고 명확
UPDATE update_policy 
SET target_version_code = 10,    -- 목표 버전 (명확!)
    is_force_update = true       -- 강제 업데이트 (명확!)
WHERE ...;
```

### 코드 비교

```kotlin
// ❌ 현재: 필드가 2개, NULL 처리 필요
fun requiresForceUpdate(currentVersion: Int): Boolean {
    if (activePopupType != "force_update") return false
    val min = minSupportedVersion ?: return false  // NULL 체크
    return currentVersion < min
}

fun recommendsUpdate(currentVersion: Int): Boolean {
    if (activePopupType != "optional_update") return false
    val latest = latestVersionCode ?: return false  // NULL 체크
    return currentVersion < latest
}

// ✅ 제안: 필드 1개, NULL 불필요
fun needsUpdate(currentVersion: Int): Boolean {
    return currentVersion < targetVersionCode  // 간단!
}

fun requiresForceUpdate(currentVersion: Int): Boolean {
    return isForceUpdate && needsUpdate(currentVersion)
}
```

---

## ✅ 최종 권장사항

### 1. update_policy 테이블 생성 ⭐⭐⭐⭐⭐

**장점**:
- ✅ **직관적**: target_version_code 하나로 통일
- ✅ **간단**: NULL 처리 불필요
- ✅ **명확**: is_force_update로 강제/선택 구분
- ✅ **일반적**: 대부분의 앱이 이 방식 사용
- ✅ **책임 분리**: 업데이트 정책만 담당

### 2. 테이블 분리 전략 (최종)

```
┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐
│ update_policy    │   │  notice_policy   │   │ emergency_policy │
├──────────────────┤   ├──────────────────┤   ├──────────────────┤
│ 업데이트 정책     │   │ 일반 공지        │   │ 긴급 상황        │
│                  │   │                  │   │                  │
│ • 강제 업데이트   │   │ • 이벤트         │   │ • 앱 차단        │
│ • 선택적 업데이트 │   │ • 신규 기능      │   │ • 서비스 종료    │
│                  │   │                  │   │                  │
│ target_version   │   │ (추적 TBD)       │   │ (추적 없음)      │
│ is_force_update  │   │                  │   │ is_dismissible   │
└──────────────────┘   └──────────────────┘   └──────────────────┘

app_policy는?
→ 광고 정책 전용으로 사용 (ad_policy로 이름 변경 고려)
→ 또는 완전히 제거하고 3개 테이블만 사용
```

### 3. 구현 순서

1. ⭐ **update_policy 테이블 생성** (최우선)
   - 단순하고 명확한 구조
   - 기존 코드 변경 최소화
   
2. ⭐ **emergency_policy 테이블 생성**
   - Google Play 정책 준수 (is_dismissible)
   
3. ⭐ **notice_policy 테이블 생성**
   - 추적 방식은 추후 결정

4. (선택) **app_policy 정리**
   - 광고 정책만 남기거나
   - ad_policy로 이름 변경

---

## 📚 참고: 실제 앱들의 업데이트 정책

### 예시 1: Firebase Remote Config 패턴

```json
{
  "force_update_version": 10,
  "latest_version": 12,
  "update_message": "..."
}
```

**특징**: 
- 2개 필드 사용하지만 **명확한 이름**
- `force_update_version`, `latest_version` → 역할이 명확

### 예시 2: 단일 필드 + Boolean 패턴 ⭐ (권장)

```json
{
  "required_version": 12,
  "is_force_update": true,
  "message": "..."
}
```

**특징**: 
- **가장 단순하고 명확**
- 우리의 제안과 동일

### 예시 3: 다중 버전 패턴 (복잡)

```json
{
  "minimum_version": 8,
  "recommended_version": 10,
  "latest_version": 12
}
```

**특징**: 
- 3개 필드 (과도하게 복잡)
- 대부분의 앱은 이렇게 안 함

---

**결론**: 단일 필드 + Boolean 방식이 **업계 표준**이며 **가장 직관적**입니다! ⭐

---

**작성일**: 2025-11-09  
**대상 독자**: PocketChord 개발팀  
**난이도**: 중급  
**키워드**: update_policy, target_version_code, is_force_update, 테이블 분리, 단순화

