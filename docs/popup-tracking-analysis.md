# 4가지 팝업 타입별 추적 분석 및 테이블 분리 전략

**날짜**: 2025-11-09  
**목적**: notice, emergency, force_update, optional_update의 재표시 로직 분석 및 테이블 분리  
**설계 원칙**: 책임 분리 (emergency_policy + notice_policy + app_policy)

---

## 📋 목차

1. [설계 원칙](#설계-원칙)
2. [1️⃣ notice 팝업](#1️⃣-notice-팝업)
3. [2️⃣ emergency 팝업](#2️⃣-emergency-팝업)
4. [3️⃣ force_update 팝업](#3️⃣-force_update-팝업)
5. [4️⃣ optional_update 팝업](#4️⃣-optional_update-팝업)
6. [통합 검토](#통합-검토)
7. [문제점 및 해결책](#문제점-및-해결책)

---

## 🎯 설계 원칙

### 핵심 제약사항

```
목표: 3개 테이블로 책임 분리

┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐
│   app_policy     │   │  notice_policy   │   │ emergency_policy │
├──────────────────┤   ├──────────────────┤   ├──────────────────┤
│ 업데이트 정책     │   │ 일반 공지        │   │ 긴급 상황        │
│                  │   │                  │   │                  │
│ • force_update   │   │ • 이벤트         │   │ • 앱 차단        │
│ • optional_...   │   │ • 신규 기능      │   │ • 서비스 종료    │
│                  │   │ • 안내           │   │                  │
│ (1개 앱=1개 행)  │   │ (1개 앱=1개 행)  │   │ (필요 시 추가)   │
└──────────────────┘   └──────────────────┘   └──────────────────┘

우선순위: 1. emergency > 2. update > 3. notice
```

**이유**:
- ✅ 100개 앱을 관리해야 함
- ✅ 각 테이블이 **단일 책임** (Single Responsibility Principle)
- ✅ emergency와 notice는 **정상 운영과 성격이 다름**
- ✅ 확장성: 새 팝업 타입 추가 시 새 테이블로
- ✅ 일관성: emergency와 notice 모두 별도 테이블

---

## 1️⃣ notice 팝업

### 현재 코드 분석

```kotlin
// HomeScreen.kt (라인 ~248)
"notice" -> {
    Log.d("HomeScreen", "Decision: NOTICE popup")

    // 이미 본 공지사항인지 확인
    val prefs = context.getSharedPreferences("announcement_prefs", Context.MODE_PRIVATE)
    val viewedIds = prefs.getStringSet("viewed_announcements", setOf()) ?: setOf()
    val policyIdStr = p.id?.toString() ?: "null"

    if (viewedIds.contains(policyIdStr)) {
        Log.d("HomeScreen", "Notice already viewed (policy id=$policyIdStr), skipping")
    } else {
        Log.d("HomeScreen", "Showing new notice (policy id=$policyIdStr)")
        announcement = Announcement(
            id = p.id,  // ← policy ID를 announcement ID로 사용
            // ...
        )
        showAnnouncementDialog = true
    }
}
```

### 추적 메커니즘

**추적 대상**: `policy.id`  
**저장 위치**: SharedPreferences `"announcement_prefs"`  
**저장 키**: `"viewed_announcements"` (Set<String>)  
**저장 시점**: X 버튼 클릭 시

```kotlin
// HomeScreen.kt (라인 ~355)
AnnouncementDialog(
    announcement = announcement!!,
    onDismiss = {
        announcement?.id?.let { id ->
            val prefs = context.getSharedPreferences("announcement_prefs", Context.MODE_PRIVATE)
            val viewedIds = prefs.getStringSet("viewed_announcements", setOf())
                ?.toMutableSet() ?: mutableSetOf()
            
            viewedIds.add(id.toString())  // ← policy.id 저장
            
            prefs.edit {
                putStringSet("viewed_announcements", viewedIds)
            }
        }
        showAnnouncementDialog = false
    }
)
```

### 재표시 조건

| 사용자 행동 | SharedPreferences 상태 | 다음 실행 시 |
|------------|----------------------|-------------|
| 앱 실행 → X 누르지 않고 종료 | `[]` (저장 안 됨) | ✅ 다시 표시 |
| 앱 실행 → X 클릭 → 종료 | `["123"]` (저장됨) | ❌ 표시 안 됨 |
| 다음 실행 | `["123"]` | ❌ 표시 안 됨 (영구적) |

### 🔴 **문제점**

#### 문제 1: 추적 방식 오류

**같은 행을 UPDATE해도 재표시되지 않음!**

```sql
-- 1월 공지
UPDATE app_policy 
SET content = '1월 이벤트 안내', active_popup_type = 'notice'
WHERE app_id = 'com.sweetapps.pocketchord';
-- policy.id = 1 (불변)

-- 사용자가 X 클릭 → SharedPreferences: ["1"]

-- 2월 공지 (같은 행 UPDATE)
UPDATE app_policy 
SET content = '2월 신규 기능 안내'
WHERE app_id = 'com.sweetapps.pocketchord';
-- policy.id = 1 (여전히 같음)

-- 결과: viewedIds.contains("1") = true → ❌ 표시 안 됨!
```

**근본 원인**: `id`는 불변이므로 내용이 바뀌어도 구분할 수 없음

---

#### 문제 2: 별도 테이블 필요성 ⭐

**현재 구조의 문제**:
```
app_policy 테이블 (1개 앱 = 1개 행)
┌────┬───────────────────────────┬──────────────────┬─────────┐
│ id │ app_id                    │ active_popup_type│ content │
├────┼───────────────────────────┼──────────────────┼─────────┤
│ 1  │ com.sweetapps.pocketchord │ notice           │ 1월 공지│
└────┴───────────────────────────┴──────────────────┴─────────┘
```

**문제**: 
- ❌ `app_policy`는 **앱의 현재 상태**를 나타내는 테이블
- ❌ `notice`는 **시간이 지나면 변경되는 콘텐츠**
- ❌ emergency처럼 **정상 운영과 성격이 다른 기능**
- ❌ 일관성: emergency를 별도 테이블로 분리하면 notice도 분리해야 함

**해결책**: `notice_policy` 별도 테이블 생성 필요 ⭐

```
설계 제안:
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   app_policy    │    │ notice_policy   │    │ emergency_policy│
├─────────────────┤    ├─────────────────┤    ├─────────────────┤
│ 정상 운영 정책   │    │ 일반 공지 전용   │    │ 긴급 상황 전용   │
│ - force_update  │    │ - 이벤트 안내    │    │ - 앱 차단        │
│ - optional_...  │    │ - 신규 기능      │    │ - 서비스 종료    │
└─────────────────┘    │ - 업데이트 안내  │    └─────────────────┘
                       └─────────────────┘
```

**장점**:
- ✅ `app_policy`: 업데이트 정책만 관리 (force/optional)
- ✅ `notice_policy`: 일반 공지만 관리 (이벤트, 안내 등)
- ✅ `emergency_policy`: 긴급 상황만 관리 (앱 차단 등)
- ✅ 각 테이블이 **단일 책임** (Single Responsibility Principle)
- ✅ 100개 앱 = 100개 app_policy + α개 notice + α개 emergency

**테이블 구조 제안** (추적 방식은 추후 결정):
```sql
CREATE TABLE public.notice_policy (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    app_id TEXT NOT NULL,                    -- 앱 ID
    title TEXT,                              -- 공지 제목
    content TEXT NOT NULL,                   -- 공지 내용
    is_active BOOLEAN NOT NULL DEFAULT TRUE, -- 활성화 여부
    -- 추적 방식은 추후 결정 (content 해시 / version / updated_at 등)
    CONSTRAINT notice_policy_pkey PRIMARY KEY (id)
);
```

**우선순위 로직** (수정됨):
```kotlin
// 1순위: emergency_policy 확인
val emergency = emergencyPolicyRepository.getActiveEmergency()
if (emergency != null) {
    // emergency 팝업 표시
    return
}

// 2순위: app_policy에서 업데이트 확인
val policy = appPolicyRepository.getPolicy()
when (policy.activePopupType) {
    "force_update" -> // ...
    "optional_update" -> // ...
    // "notice"는 제거됨
}

// 3순위: notice_policy 확인
val notice = noticePolicyRepository.getActiveNotice()
if (notice != null && !isViewed(notice)) {
    // notice 팝업 표시
}
```

### ✅ **해결책**

#### 해결책 1: notice_policy 별도 테이블 + 버전 방식 ⭐⭐⭐⭐⭐ (최종 권장)

**장점**: 
- ✅ emergency/update와 일관성 유지 (모두 별도 테이블)
- ✅ **명시적 제어**: 언제 새 공지로 할지 결정 가능
- ✅ **오타 수정 가능**: 버전 안 올리면 재표시 안 됨
- ✅ **업계 표준**: Slack, Discord 등 주요 앱 사용
- ✅ 확장성: 공지사항 기능 추가 시 용이

**테이블 구조**:
```sql
CREATE TABLE public.notice_policy (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    app_id TEXT NOT NULL,
    title TEXT,
    content TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- ===== 핵심: 버전 필드 ⭐ =====
    notice_version INTEGER NOT NULL DEFAULT 1,  -- 공지 버전
    
    CONSTRAINT notice_policy_pkey PRIMARY KEY (id)
);
```

**추적 로직**:
```kotlin
// 식별자 생성
val identifier = "notice_v${notice.noticeVersion}"
// 예: "notice_v1", "notice_v2", ...

// 조회 확인
if (viewedVersions.contains(identifier)) {
    // 이미 본 버전
}
```

**사용 예시**:
```sql
-- 오타 수정 (버전 유지)
UPDATE notice_policy 
SET content = '수정된 내용'
WHERE app_id = 'com.sweetapps.pocketchord';
-- notice_version = 1 (그대로) → 재표시 안 됨 ✅

-- 새 공지 (버전 증가)
UPDATE notice_policy 
SET content = '2월 이벤트',
    notice_version = 2  -- 버전 증가
WHERE app_id = 'com.sweetapps.pocketchord';
-- → 모든 사용자에게 재표시! ✅
```

**상세 내용**: `notice-policy-redesign.md` 참조 ⭐

**우선순위**:
```
1. emergency_policy (긴급)
2. update_policy (업데이트)
3. notice_policy (일반 공지)
```

---

#### 대안: 해시 방식 (테이블 수정 불가 시)

**장점**: 
- ✅ 테이블 수정 불필요
- ✅ 코드만 수정하면 됨
- ✅ 완전 자동

**단점**:
- ❌ **제어 불가**: 오타 수정도 새 공지로 인식
- ❌ 사용자 경험 저하 가능

**언제 선택?**:
- 테이블 수정이 정말 어려운 경우
- 즉시 구현이 필요한 경우
- 오타 수정이 거의 없는 경우

```kotlin
// 해시 방식
val identifier = "notice_${policy.content.hashCode()}"
```

**상세 비교**: `notice-policy-redesign.md`의 "3가지 방식 비교" 참조

---

## 2️⃣ emergency 팝업

### 현재 코드 분석

```kotlin
// HomeScreen.kt (라인 ~206)
"emergency" -> {
    Log.d("HomeScreen", "Decision: EMERGENCY popup will show")
    appPolicy = p  // 정책 객체 저장
    showEmergencyDialog = true
    // 정책이 유효하므로 이전 강제 캐시 정리
    if (storedForceVersion != -1) updatePrefs.edit {
        remove("force_required_version")
        remove("force_update_info")
    }
}
```

```kotlin
// HomeScreen.kt (라인 ~298)
if (showEmergencyDialog && appPolicy != null) {
    EmergencyRedirectDialog(
        title = "🚨 긴급공지",
        description = appPolicy!!.content ?: "",
        newAppPackage = "com.sweetapps.pocketchord",
        redirectUrl = appPolicy!!.downloadUrl,
        buttonText = "새 앱 설치하기",
        isDismissible = false,  // ← X 버튼 없음!
        onDismiss = { /* X 버튼 없음 */ },
        badgeText = "긴급"
    )
}
```

### 추적 메커니즘

**추적 여부**: ❌ **없음**  
**저장 위치**: 없음  
**재표시 조건**: 항상 표시 (X 버튼 없음)

### 재표시 조건

| 사용자 행동 | 다음 실행 시 |
|------------|-------------|
| 앱 실행 → emergency 팝업 표시 | ✅ 매번 표시 |
| "새 앱 설치하기" 클릭 → 외부 링크 이동 | ✅ 매번 표시 |
| 앱 강제 종료 | ✅ 매번 표시 |

### ✅ **특징**

- ✅ **X 버튼 옵션 제공** (`isDismissible` 파라미터)
- ✅ **항상 재표시 가능** (추적 없음 또는 선택적 추적)
- ✅ **긴급 상황 대응**

### 🔴 **문제점 발견!**

#### 문제 1: 별도 테이블 필요성

**현재 구조의 문제**:
```
app_policy 테이블 (1개 앱 = 1개 행)
┌────┬───────────────────────────┬──────────────────┐
│ id │ app_id                    │ active_popup_type│
├────┼───────────────────────────┼──────────────────┤
│ 1  │ com.sweetapps.pocketchord │ emergency        │
└────┴───────────────────────────┴──────────────────┘
```

**문제**: 
- ❌ 기존 앱이 emergency 상태가 되면 **새 앱을 emergency_policy에서 관리할 수 없음**
- ❌ `app_policy` 테이블은 **1개 앱 = 1개 행** 원칙
- ❌ 기존 앱 차단 시나리오에서는 **2개 앱(기존 + 새)을 모두 관리**해야 함

**해결책**: `emergency_policy` 별도 테이블 생성 필요 ⭐

```
설계 제안:
┌─────────────────┐         ┌─────────────────┐
│   app_policy    │         │ emergency_policy│
├─────────────────┤         ├─────────────────┤
│ 정상 운영 정책   │         │ 긴급 상황 전용   │
│ - notice        │         │ - 앱 차단        │
│ - force_update  │         │ - 서비스 종료    │
│ - optional_...  │         │ - 마이그레이션   │
└─────────────────┘         └─────────────────┘
```

**장점**:
- ✅ `app_policy`: 정상 운영 (1개 앱 = 1개 행 유지)
- ✅ `emergency_policy`: 긴급 상황만 관리 (필요 시에만 행 추가)
- ✅ emergency 발생 시 `app_policy`를 건드리지 않음
- ✅ 100개 앱이 모두 정상이면 `emergency_policy`는 0개 행

**테이블 구조 제안**:
```sql
CREATE TABLE public.emergency_policy (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    app_id TEXT NOT NULL,                    -- 차단된 앱 ID
    content TEXT NOT NULL,                   -- 긴급 메시지
    redirect_url TEXT,                       -- 새 앱 다운로드 URL
    new_app_id TEXT,                         -- 새 앱 ID (옵션)
    is_active BOOLEAN NOT NULL DEFAULT TRUE, -- 긴급 상황 활성화 여부
    is_dismissible BOOLEAN NOT NULL DEFAULT FALSE, -- X 버튼 허용 여부 ⭐
    CONSTRAINT emergency_policy_pkey PRIMARY KEY (id)
);
```

**우선순위 로직**:
```kotlin
// 1순위: emergency_policy 확인
val emergency = emergencyPolicyRepository.getActiveEmergency()
if (emergency != null) {
    // emergency 팝업 표시
    return
}

// 2순위: app_policy 확인
val policy = appPolicyRepository.getPolicy()
when (policy.activePopupType) {
    "force_update" -> // ...
    "optional_update" -> // ...
    "notice" -> // ...
}
```

---

#### 문제 2: Google Play 정책 위반 가능성 ⚠️

**현재 구현**:
```kotlin
EmergencyRedirectDialog(
    isDismissible = false,  // ← X 버튼 없음 (하드코딩)
    onDismiss = { /* 닫기 불가 */ }
)
```

**Google Play 정책**:
> "사용자가 원하지 않는 경우 대화상자나 전체 화면 메시지를 닫을 수 있어야 합니다."
> - [Disruptive Ads Policy](https://support.google.com/googleplay/android-developer/answer/9914283)

**문제**:
- ❌ **절대 꺼지지 않는 팝업 = 정책 위반 가능**
- ❌ 사용자가 탈출할 수 없음 (강제 종료만 가능)
- ❌ Google의 "Disruptive Ads" 정책에 저촉될 수 있음

**해결책**: `isDismissible` 옵션을 DB에서 제어 ⭐

```sql
-- emergency_policy 테이블에 필드 추가
ALTER TABLE emergency_policy 
ADD COLUMN is_dismissible BOOLEAN NOT NULL DEFAULT FALSE;

-- 사용 예시 1: 정말 심각한 상황 (앱 차단)
INSERT INTO emergency_policy (app_id, content, redirect_url, is_dismissible)
VALUES (
    'com.sweetapps.pocketchord',
    '⚠️ 이 앱은 더 이상 지원되지 않습니다.',
    'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord.v2',
    false  -- X 버튼 없음 (최종 수단)
);

-- 사용 예시 2: 서비스 종료 예정 (선택 가능)
INSERT INTO emergency_policy (app_id, content, redirect_url, is_dismissible)
VALUES (
    'com.sweetapps.oldapp',
    '📢 이 앱은 12월 31일 종료됩니다.\n새 앱으로 이동해주세요.',
    'https://play.google.com/store/apps/details?id=com.sweetapps.newapp',
    true   -- X 버튼 있음 (Google Play 정책 준수)
);
```

**코드 반영**:
```kotlin
// 1. emergency_policy에서 조회
val emergency = emergencyPolicyRepository.getActiveEmergency()

// 2. DB 값으로 X 버튼 제어
EmergencyRedirectDialog(
    title = "긴급공지",
    description = emergency.content,
    redirectUrl = emergency.redirectUrl,
    isDismissible = emergency.isDismissible,  // ← DB에서 제어!
    onDismiss = if (emergency.isDismissible) {
        {
            // X 클릭 시 추적 (선택적)
            // 추적하지 않으면 항상 표시
            // 추적하면 1회만 표시
        }
    } else null
)
```

**권장 사항**:
- ✅ **기본값 `true`** (Google Play 정책 준수)
- ⚠️ **`false`는 최후의 수단** (앱 완전 차단 등)
- ✅ **대부분의 경우 `true`로 설정하고 추적 없이 계속 표시**

---

### 🟡 **수정된 특징**

- ✅ **X 버튼 제어 가능** (`is_dismissible` 필드로 DB에서 관리)
- ✅ **항상 재표시 또는 선택적 추적** (isDismissible에 따라 다름)
- ✅ **Google Play 정책 준수** (기본적으로 X 버튼 제공)
- ✅ **별도 테이블 관리** (`emergency_policy` 권장)

### 🟢 **문제 없음**

긴급 공지는 **항상 표시되어야 하는 것이 정상**입니다.

### 긴급공지의 목적

**시나리오**: 앱이 플레이스토어에서 중지됨 (정책 위반 등)

```
기존 앱 (com.sweetapps.pocketchord)
  ↓
플레이스토어에서 차단됨! 
  ↓
emergency_policy 생성 (별도 테이블)
  ↓
사용자: "⚠️ 이 앱은 중단되었습니다. 새 앱을 설치하세요"
  ↓
새 앱 설치 (com.sweetapps.pocketchord.v2)
  ↓
사용자는 새 앱 사용 시작
```

### 실제 사용 예시 (수정됨)

```sql
-- ⚠️ 앱이 스토어에서 차단됨!
-- emergency_policy 테이블에 긴급 공지 추가 (별도 테이블)
INSERT INTO emergency_policy (
    app_id, 
    content, 
    redirect_url, 
    new_app_id,
    is_active,
    is_dismissible  -- ⭐ X 버튼 허용 여부
) VALUES (
    'com.sweetapps.pocketchord',  -- 차단된 기존 앱
    '⚠️ 이 앱은 더 이상 지원되지 않습니다.\n새 버전을 설치해주세요.',
    'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord.v2',
    'com.sweetapps.pocketchord.v2',
    true,
    false  -- 정말 심각한 경우만 false (Google Play 정책 주의!)
);

-- app_policy는 그대로 유지 (정상 운영 상태)
-- emergency_policy가 우선순위가 높으므로 긴급 팝업이 표시됨

-- 새 앱 정책 생성 (정상 운영)
INSERT INTO app_policy (app_id, is_active, active_popup_type, content)
VALUES (
    'com.sweetapps.pocketchord.v2',  -- 새 앱
    true,
    'none',  -- 정상 운영
    ''
);
```

### 중요! 긴급공지 관리 방법 (수정됨)

**기존 앱 (com.sweetapps.pocketchord)**:
- ✅ `emergency_policy` 테이블에서 관리 (별도 테이블)
- ✅ `app_policy`는 건드리지 않음 (정상 상태 유지)
- ✅ 사용자가 앱을 실행할 때마다 긴급 팝업 우선 표시
- ⚠️ **X 버튼 여부는 `is_dismissible` 필드로 제어**
- ✅ 긴급 상황 종료 시 `emergency_policy.is_active = false`

**새 앱 (com.sweetapps.pocketchord.v2)**:
- ✅ 정상 운영 (`app_policy`에서 관리)
- ✅ 독립적인 app_policy 행
- ✅ 기존 사용자가 이주하면 이 앱 사용

**Google Play 정책 준수**:
- ✅ **기본적으로 `is_dismissible = true` 권장**
- ⚠️ `is_dismissible = false`는 **최후의 수단** (앱 완전 차단 등)
- ✅ X 버튼이 있어도 **추적하지 않으면 매번 표시 가능**
- ✅ 사용자가 X 클릭 → 앱 사용 가능 → 다음 실행 시 다시 표시

**설계 의도**: 
- 긴급공지는 **별도 테이블(`emergency_policy`)에서 관리**
- `app_policy`는 정상 운영만 담당 (1개 앱 = 1개 행 유지)
- X 버튼 제공으로 Google Play 정책 준수
- 추적 없이 매번 표시하여 사용자에게 계속 알림

---

## 3️⃣ force_update 팝업

### 현재 코드 분석

```kotlin
// HomeScreen.kt (라인 ~212)
"force_update" -> {
    if (p.requiresForceUpdate(currentVersion)) {
        Log.d("HomeScreen", "Decision: FORCE UPDATE popup (minSupported=${p.minSupportedVersion})")
        updateInfo = UpdateInfo(
            id = null,
            versionCode = p.minSupportedVersion ?: (currentVersion + 1),
            versionName = "",
            appId = BuildConfig.SUPABASE_APP_ID,
            isForce = true,
            releaseNotes = p.content ?: "",
            releasedAt = null,
            downloadUrl = p.downloadUrl
        )
        showUpdateDialog = true
        updatePrefs.edit {
            putInt("force_required_version", updateInfo!!.versionCode)
            putString("force_update_info", gson.toJson(updateInfo!!))
        }
    } else {
        // 강제 업데이트 조건 해제 → 캐시 제거
        if (storedForceVersion != -1) updatePrefs.edit {
            remove("force_required_version")
            remove("force_update_info")
        }
    }
}
```

### 추적 메커니즘

**추적 대상**: `minSupportedVersion` (최소 지원 버전 코드)  
**저장 위치**: SharedPreferences `"update_prefs"`  
**저장 키**: `"force_required_version"` (Int)  
**저장 시점**: 강제 업데이트 조건 감지 시 (자동)

```kotlin
// HomeScreen.kt (라인 ~111)
// 강제 업데이트 로컬 복원 (오프라인 대비)
val storedForceVersion = updatePrefs.getInt("force_required_version", -1)
var restoredForcedUpdate: UpdateInfo? = null
if (storedForceVersion != -1 && storedForceVersion > BuildConfig.VERSION_CODE) {
    val json = updatePrefs.getString("force_update_info", null)
    restoredForcedUpdate = runCatching { 
        json?.let { gson.fromJson(it, UpdateInfo::class.java) } 
    }.getOrNull()
    // 로컬에서 복원
}
```

### 재표시 조건

| 앱 버전 | minSupportedVersion | 조건 | 다음 실행 시 |
|---------|---------------------|------|-------------|
| 10 | 12 | `10 < 12` | ✅ 매번 표시 |
| 11 | 12 | `11 < 12` | ✅ 매번 표시 |
| 12 | 12 | `12 >= 12` | ❌ 표시 안 됨 |
| 13 | 12 | `13 >= 12` | ❌ 표시 안 됨 |

**로직**:
```kotlin
// AppPolicy.kt
fun requiresForceUpdate(currentVersion: Int): Boolean {
    return minSupportedVersion?.let { it > currentVersion } ?: false
}
```

### ✅ **특징**

- ✅ **버전 기반 추적** (내용 무관)
- ✅ **항상 재표시** (업데이트 전까지)
- ✅ **X 버튼 없음** (업데이트 강제)
- ✅ **오프라인 대응** (로컬 캐시 사용)

### 🟢 **문제 없음**

강제 업데이트는 **앱 버전**으로 판단하므로 정상입니다.

```sql
-- 강제 업데이트 설정 (버전 12 미만 차단)
UPDATE app_policy 
SET active_popup_type = 'force_update',
    min_supported_version = 12,
    content = '새 버전으로 업데이트해주세요'
WHERE app_id = 'com.sweetapps.pocketchord';

-- 앱 버전 10 사용자 → 매번 표시 (정상)
-- 앱 버전 12 사용자 → 표시 안 됨 (정상)

-- 최소 버전 상향 (버전 15 미만 차단)
UPDATE app_policy 
SET min_supported_version = 15
WHERE app_id = 'com.sweetapps.pocketchord';

-- 앱 버전 12 사용자 → 이제 매번 표시됨 (정상)
```

**설계 의도**: 
- ✅ 앱 버전이 기준이므로 **id 불필요**
- ✅ 같은 행을 UPDATE해도 `minSupportedVersion`만 변경하면 됨
- ✅ 사용자가 업데이트하면 자동으로 표시 안 됨

### ⚠️ **개선 예정: update_policy 테이블로 전환**

**현재 문제**:
- ❌ `minSupportedVersion`과 `latestVersionCode` 필드명이 혼란스러움
- ❌ 어느 필드에 값을 넣어야 하는지 불명확

**개선안** (상세 내용: `update-policy-redesign.md` 참조):
```sql
-- 단순하고 명확한 구조로 전환
CREATE TABLE update_policy (
    target_version_code INT NOT NULL,  -- 목표 버전 (단일 필드!)
    is_force_update BOOLEAN NOT NULL   -- 강제 여부
);

-- 사용 예시
UPDATE update_policy 
SET target_version_code = 12,  -- 명확!
    is_force_update = true     -- 명확!
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

## 4️⃣ optional_update 팝업

### 현재 코드 분석

```kotlin
// HomeScreen.kt (라인 ~229)
"optional_update" -> {
    if (p.recommendsUpdate(currentVersion) &&
        dismissedVersionCode.value != (p.latestVersionCode ?: -1)) {
        Log.d("HomeScreen", "Decision: OPTIONAL UPDATE popup (latest=${p.latestVersionCode})")
        updateInfo = UpdateInfo(
            id = null,
            versionCode = p.latestVersionCode!!,
            versionName = "",
            appId = BuildConfig.SUPABASE_APP_ID,
            isForce = false,
            releaseNotes = p.content ?: "",
            releasedAt = null,
            downloadUrl = p.downloadUrl
        )
        showUpdateDialog = true
    }
}
```

### 추적 메커니즘

**추적 대상**: `latestVersionCode` (최신 버전 코드)  
**저장 위치**: SharedPreferences `"update_prefs"`  
**저장 키**: `"dismissed_version_code"` (Int)  
**저장 시점**: "나중에" 버튼 클릭 시

```kotlin
// HomeScreen.kt (라인 ~320)
OptionalUpdateDialog(
    isForce = updateInfo!!.isForce,
    title = "앱 업데이트",
    updateButtonText = "지금 업데이트",
    features = if (features.isNotEmpty()) features else null,
    onUpdateClick = {
        tryOpenStore(updateInfo!!)
    },
    onLaterClick = if (updateInfo!!.isForce) null else {
        {
            // 선택적 업데이트를 사용자가 닫았으므로 동일 versionCode 재표시 방지 저장
            updatePrefs.edit {
                putInt("dismissed_version_code", updateInfo!!.versionCode)
            }
            dismissedVersionCode.value = updateInfo!!.versionCode
            showUpdateDialog = false
            Log.d("HomeScreen", "Update dialog dismissed for code=${updateInfo!!.versionCode}")
        }
    }
)
```

### 재표시 조건

| 앱 버전 | latestVersionCode | dismissed | 조건 | 다음 실행 시 |
|---------|-------------------|-----------|------|-------------|
| 10 | 12 | -1 | `10 < 12` && `-1 != 12` | ✅ 표시 |
| 10 | 12 | 12 | `10 < 12` && `12 == 12` | ❌ 표시 안 됨 |
| 11 | 12 | 12 | `11 < 12` && `12 == 12` | ❌ 표시 안 됨 |
| 11 | 13 | 12 | `11 < 13` && `12 != 13` | ✅ 표시 (새 버전) |

**로직**:
```kotlin
// AppPolicy.kt
fun recommendsUpdate(currentVersion: Int): Boolean {
    return latestVersionCode?.let { it > currentVersion } ?: false
}

// HomeScreen.kt
if (p.recommendsUpdate(currentVersion) &&
    dismissedVersionCode.value != (p.latestVersionCode ?: -1)) {
    // 표시
}
```

### ✅ **특징**

- ✅ **버전 기반 추적** (내용 무관)
- ✅ **"나중에" 선택 시 추적**
- ✅ **새 버전 나오면 다시 표시**
- ✅ **X 버튼 있음** (선택적)

### 🟢 **문제 없음**

선택적 업데이트도 **버전 코드**로 판단하므로 정상입니다.

```sql
-- 선택적 업데이트 권장 (버전 12)
UPDATE app_policy 
SET active_popup_type = 'optional_update',
    latest_version_code = 12,
    content = '새로운 기능이 추가되었습니다'
WHERE app_id = 'com.sweetapps.pocketchord';

-- 앱 버전 10 사용자 → 표시됨
-- "나중에" 클릭 → SharedPreferences: dismissed = 12
-- 다음 실행 → 표시 안 됨 (dismissed = 12)

-- 새 버전 출시 (버전 13)
UPDATE app_policy 
SET latest_version_code = 13,
    content = '버그 수정 및 성능 개선'
WHERE app_id = 'com.sweetapps.pocketchord';

-- 앱 버전 10 사용자 → 다시 표시됨! (dismissed=12, latest=13)
```

**설계 의도**: 
- ✅ 버전 코드가 기준이므로 **id 불필요**
- ✅ 같은 행을 UPDATE해도 `latestVersionCode`만 변경하면 됨
- ✅ 사용자가 "나중에"를 눌러도 새 버전이 나오면 다시 표시

### ⚠️ **개선 예정: update_policy 테이블로 전환**

**현재 문제**:
- ❌ `minSupportedVersion`과 `latestVersionCode` 필드명이 혼란스러움
- ❌ 어느 필드에 값을 넣어야 하는지 불명확
- ❌ NULL 처리가 복잡

**개선안** (상세 내용: `update-policy-redesign.md` 참조):
```sql
-- 단순하고 명확한 구조로 전환
CREATE TABLE update_policy (
    target_version_code INT NOT NULL,  -- 목표 버전 (단일 필드!)
    is_force_update BOOLEAN NOT NULL   -- 강제 여부
);

-- 사용 예시
UPDATE update_policy 
SET target_version_code = 12,  -- 명확!
    is_force_update = false    -- 선택적!
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

## 🔍 통합 검토

### 4가지 팝업 비교표

| 팝업 타입 | 추적 대상 | 저장 키 | X 버튼 | 재표시 조건 | 관리 테이블 | 문제 여부 |
|-----------|----------|---------|--------|------------|------------|----------|
| **emergency** | 없음 (추천) | 없음 | ⚠️ **DB 제어** | 항상 (추적 없음) | `emergency_policy` ⭐ | 🟡 **테이블 분리 필요** |
| **force_update** | `targetVersionCode` | `force_required_version` | ❌ 없음 | 버전 낮으면 항상 | `update_policy` ⭐ | 🟡 **테이블 분리 + 단순화** |
| **optional_update** | `targetVersionCode` | `dismissed_version_code` | ✅ "나중에" | 새 버전 나오면 | `update_policy` ⭐ | 🟡 **테이블 분리 + 단순화** |
| **notice** | `noticeVersion` ⭐ | `viewed_notices` | ✅ X | 버전 증가 시 | `notice_policy` ⭐ | 🟡 **테이블 분리 + 버전 필드** |

### 문제 요약

```
🟡 emergency        → 별도 테이블 필요 + X 버튼 옵션 필요
                      └─> emergency_policy 테이블 생성 권장
                      └─> is_dismissible 필드로 Google Play 정책 준수

🟡 force_update     → 별도 테이블 필요 + 필드 단순화 필요
🟡 optional_update  └─> update_policy 테이블 생성 권장
                      └─> target_version_code (단일 필드)
                      └─> is_force_update (Boolean)
                      └─> 자세한 내용: update-policy-redesign.md

🟡 notice           → 별도 테이블 필요 + 버전 필드 추가 ⭐
                      └─> notice_policy 테이블 생성 권장
                      └─> notice_version (명시적 버전 관리)
                      └─> 업계 표준 방식 (Slack, Discord 등)
                      └─> 자세한 내용: notice-policy-redesign.md
```

### 테이블 책임 분리 (수정됨)

```
┌─────────────────────────────────────────────────────────────┐
│                     Supabase 테이블 구조                     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐   │
│  │update_policy │   │notice_policy │   │emergency_... │   │
│  ├──────────────┤   ├──────────────┤   ├──────────────┤   │
│  │업데이트 정책  │   │일반 공지     │   │긴급상황      │   │
│  │              │   │              │   │              │   │
│  │• 강제 업데이트│   │• 이벤트 안내 │   │• 앱차단      │   │
│  │• 선택적 업데이트│ │• 신규 기능   │   │• 종료        │   │
│  │              │   │• 업데이트안내│   │              │   │
│  │target_version│   │(추적방식TBD) │   │(추적없음)    │   │
│  │is_force_...  │   │              │   │is_dismissible│   │
│  │(1개앱=1개행) │   │(필요시행추가)│   │(필요시)      │   │
│  └──────────────┘   └──────────────┘   └──────────────┘   │
│                                                              │
│  우선순위: emergency > update > notice                       │
│                                                              │
│  ⚠️ 중요: update_policy는 단순화된 구조 사용!               │
│    - target_version_code (단일 필드)                        │
│    - is_force_update (Boolean)                              │
│    자세한 내용: update-policy-redesign.md 참조              │
└─────────────────────────────────────────────────────────────┘
```

**장점**:
- ✅ **단일 책임 원칙** (각 테이블이 하나의 책임만)
- ✅ **일관성**: emergency와 notice 모두 별도 테이블
- ✅ **확장성**: 각 기능을 독립적으로 확장 가능
- ✅ **관리 용이**: 100개 앱 = 100개 update + α개 notice + α개 emergency
- ✅ **직관성**: update_policy는 단일 필드로 단순화 (더 이상 min/latest 혼란 없음)

---

## 🔴 문제점 및 해결책

### 문제: notice와 emergency는 별도 테이블이 필요함

**현재 방식**:
```
app_policy 테이블 (모든 팝업을 한 곳에 관리)
┌────┬───────────────────────┬──────────────────┬─────────┐
│ id │ app_id                │ active_popup_type│ content │
├────┼───────────────────────┼──────────────────┼─────────┤
│ 1  │ com.sweetapps.app1    │ emergency        │ ...     │
│ 2  │ com.sweetapps.app2    │ force_update     │ ...     │
│ 3  │ com.sweetapps.app3    │ notice           │ ...     │
└────┴───────────────────────┴──────────────────┴─────────┘
```

**문제**:
```
❌ notice: 시간에 따라 변경되는 콘텐츠 (1월→2월→3월)
   └─> app_policy의 1개 행으로는 추적 불가능
   └─> id가 변하지 않아 재표시 안 됨

❌ emergency: 앱 차단 시 새 앱으로 이동 유도
   └─> 기존 앱 emergency + 새 앱 정상 = 2개 앱 관리 복잡
   └─> app_policy가 정상 운영과 긴급 상황을 동시에 담당

✅ force/optional_update: 버전 기반이라 현재 구조 적합
   └─> 앱 버전으로 판단 (minSupportedVersion, latestVersionCode)
   └─> 1개 앱 = 1개 행으로 충분
```

---

### 해결책: 3개 테이블로 책임 분리 ⭐

```
┌──────────────────┐
│   app_policy     │  업데이트 정책 (1개 앱 = 1개 행)
├──────────────────┤
│ • force_update   │  버전 기반 → 추적 필요 없음
│ • optional_...   │  버전 기반 → dismissed_version_code
└──────────────────┘

┌──────────────────┐
│  notice_policy   │  일반 공지 (필요 시 행 추가)
├──────────────────┤
│ • 이벤트         │  추적 방식은 추후 결정
│ • 신규 기능      │  (해시/버전/타임스탬프 중 선택)
│ • 안내           │
└──────────────────┘

┌──────────────────┐
│ emergency_policy │  긴급 상황 (필요 시 행 추가)
├──────────────────┤
│ • 앱 차단        │  추적 없음 (매번 표시)
│ • 서비스 종료    │  is_dismissible로 X 버튼 제어
└──────────────────┘

우선순위: emergency > update > notice
```

---

### 해결책 1: emergency_policy 테이블 생성 (즉시 필요) ⭐⭐⭐

**이유**:
- 긴급 상황은 정상 운영과 **성격이 완전히 다름**
- Google Play 정책 준수를 위해 `is_dismissible` 필드 필요
- 기존 앱 차단 시 새 앱과 별도 관리 필요

**테이블 구조**:
```sql
CREATE TABLE public.emergency_policy (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    app_id TEXT NOT NULL,
    content TEXT NOT NULL,
    redirect_url TEXT,
    new_app_id TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_dismissible BOOLEAN NOT NULL DEFAULT TRUE,  -- ⭐ Google Play 정책 준수
    CONSTRAINT emergency_policy_pkey PRIMARY KEY (id)
);
```

---

### 해결책 2: notice_policy 테이블 생성 (권장) ⭐⭐⭐

**이유**:
- emergency와 **일관성** 유지 (둘 다 별도 테이블)
- app_policy는 **업데이트 정책만** 담당 (단일 책임)
- 추적 방식을 나중에 **유연하게 선택** 가능

**테이블 구조** (추적 필드는 추후 결정):
```sql
CREATE TABLE public.notice_policy (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    app_id TEXT NOT NULL,
    title TEXT,
    content TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- ⚠️ 추적 방식은 추후 결정 (아래 중 하나 선택):
    -- 옵션 A: notice_version INTEGER (버전 기반)
    -- 옵션 B: content_hash TEXT (해시 기반)
    -- 옵션 C: updated_at + 트리거 (시간 기반)
    
    CONSTRAINT notice_policy_pkey PRIMARY KEY (id)
);
```

**추적 방식 결정 보류 이유**:
- 행이 많아지는 문제를 추후 검토 필요
- 여러 옵션 중 최적의 방법 선택 필요
- 테이블 구조는 먼저 확정하고 추적 로직은 나중에

---

## 🎯 최종 권장사항

### 각 팝업별 관리 방식

| 팝업 타입 | 추적 방식 | 식별자 예시 | 관리 테이블 | 우선순위 |
|-----------|----------|------------|------------|----------|
| **emergency** | 추적 없음 (추천) | - | `emergency_policy` ⭐ | 1순위 |
| **force_update** | 버전 코드 | `12` | `update_policy` ⭐ | 2순위 |
| **optional_update** | 버전 코드 | `12` | `update_policy` ⭐ | 2순위 |
| **notice** | **버전 필드** ⭐ | `"notice_v2"` | `notice_policy` ⭐ | 3순위 |

### 테이블 분리 전략

```
현재 (app_policy 1개)          →     목표 (3개 테이블 분리)
┌─────────────────────┐              ┌─────────────────────┐
│   app_policy        │              │  update_policy      │
├─────────────────────┤              ├─────────────────────┤
│ • emergency  ❌     │              │ • 강제 업데이트   ✅ │
│ • force_update  ❌  │              │ • 선택적 업데이트✅ │
│ • optional_update❌ │              │                     │
│ • notice  ❌        │              │ target_version ⭐   │
│ • 광고 정책         │              │ is_force_update ⭐  │
│                     │              └─────────────────────┘
│ min_supported ❌    │              ┌─────────────────────┐
│ latest_version ❌   │              │  notice_policy      │
└─────────────────────┘              ├─────────────────────┤
                                     │ • 일반 공지       ✅ │
  책임 과다                          │ • 추적 방식 TBD    │
  필드명 혼란                        └─────────────────────┘
  NULL 처리 복잡                     ┌─────────────────────┐
                                     │ emergency_policy    │
                                     ├─────────────────────┤
                                     │ • 긴급 상황       ✅ │
                                     │ • is_dismissible   │
                                     └─────────────────────┘

                                     각 테이블이 명확한 책임
                                     → 관리 용이
                                     → 확장성 우수
                                     → 필드명 직관적
```

### 구현 우선순위

#### 1단계: update_policy 테이블 생성 (최우선) ⭐⭐⭐⭐⭐

**이유**: 
- **필드 구조 단순화** (min/latest → target)
- 혼란스러운 필드명 개선
- emergency/notice와 일관성 유지

**핵심 개선사항**:
```
Before (app_policy):
  ❌ min_supported_version: 10 | NULL   (헷갈림!)
  ❌ latest_version_code: NULL | 15     (헷갈림!)
  ❌ active_popup_type: 'force_update' | 'optional_update'

After (update_policy):
  ✅ target_version_code: 12            (명확!)
  ✅ is_force_update: true | false      (명확!)
```

**상세 내용**: `update-policy-redesign.md` 필독! ⭐

**작업**:
- [ ] Supabase에 update_policy 테이블 생성
- [ ] RLS 정책 설정
- [ ] UpdatePolicy 모델 클래스 생성
- [ ] UpdatePolicyRepository 생성
- [ ] HomeScreen 로직 수정 (2순위)

---

#### 2단계: emergency_policy 테이블 생성 (권장) ⭐⭐⭐

**이유**: 
- Google Play 정책 준수 필요 (`is_dismissible`)
- 긴급 상황은 정상 운영과 성격이 다름
- 즉시 구현 가능

**작업**:
- [ ] Supabase에 emergency_policy 테이블 생성
- [ ] RLS 정책 설정
- [ ] EmergencyPolicy 모델 클래스 생성
- [ ] EmergencyPolicyRepository 생성
- [ ] HomeScreen 우선순위 로직 수정 (1순위)

---

#### 3단계: notice_policy 테이블 생성 (권장) ⭐⭐⭐

**이유**:
- emergency/update와 일관성 유지
- **버전 방식**: 명시적 제어 가능 (오타 수정 vs 새 공지)
- **업계 표준**: Slack, Discord 등 주요 앱 사용

**핵심 개선사항**:
```
Before (해시 방식):
  ❌ content 변경 → 해시 변경 → 항상 재표시 (오타도!)

After (버전 방식):
  ✅ 오타 수정: content만 변경 → 재표시 안 됨
  ✅ 새 공지: content + notice_version 증가 → 재표시됨!
```

**상세 내용**: `notice-policy-redesign.md` 필독! ⭐

**작업**:
- [ ] Supabase에 notice_policy 테이블 생성
  - [ ] notice_version INTEGER 필드 포함
- [ ] RLS 정책 설정
- [ ] NoticePolicy 모델 클래스 생성
- [ ] NoticePolicyRepository 생성
- [ ] HomeScreen 우선순위 로직 수정 (3순위)
- [ ] 버전 기반 추적 로직 구현

---

#### 4단계: 운영 가이드 작성 (구현 후)

**관리자를 위한 가이드**:

| 작업 | notice_version | 결과 |
|------|---------------|------|
| **새 공지** | 증가 (`+1`) | ✅ 모든 사용자에게 표시 |
| **오타 수정** | 유지 (그대로) | ✅ 이미 본 사용자에게 안 뜸 |
| **내용 보완** | 상황에 따라 | 판단 필요 |

**예시**:
```sql
-- 새 이벤트 (버전 증가)
UPDATE notice_policy 
SET content = '3월 봄맞이 이벤트',
    notice_version = notice_version + 1
WHERE app_id = 'com.sweetapps.pocketchord';

-- 오타만 수정 (버전 유지)
UPDATE notice_policy 
SET content = '3월 봄맞이 이벤트'  -- 오타 수정
WHERE app_id = 'com.sweetapps.pocketchord';
-- notice_version은 건드리지 않음!
```

---

### update_policy 테이블 생성 스크립트 ⭐

```sql
-- 업데이트 정책 전용 테이블 (단순화된 구조)
CREATE TABLE public.update_policy (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- 기본 정보
    app_id TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- ===== 핵심 필드 (단순화!) =====
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

**사용 예시**:
```sql
-- 강제 업데이트 (직관적!)
UPDATE update_policy 
SET target_version_code = 12,  -- 목표 버전: 12
    is_force_update = true     -- 강제!
WHERE app_id = 'com.sweetapps.pocketchord';

-- 선택적 업데이트 (직관적!)
UPDATE update_policy 
SET target_version_code = 15,  -- 목표 버전: 15
    is_force_update = false    -- 선택적!
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

## 📊 동작 예시 (3개 테이블 구조)

### Supabase 작업

```sql
-- ===== 1. 정상 운영 (app_policy만 사용) =====

-- 앱 최초 설정 (업데이트 정책만)
INSERT INTO app_policy (
    app_id, 
    is_active, 
    active_popup_type, 
    min_supported_version,
    latest_version_code
) VALUES (
    'com.sweetapps.pocketchord', 
    true, 
    'none',  -- 업데이트 없음
    10,      -- 최소 지원 버전
    15       -- 최신 버전
);
-- app_policy id = 1

-- 선택적 업데이트 권장
UPDATE app_policy 
SET active_popup_type = 'optional_update',
    latest_version_code = 16
WHERE app_id = 'com.sweetapps.pocketchord';


-- ===== 2. 일반 공지 추가 (notice_policy 사용) =====

-- 1월 공지 생성
INSERT INTO notice_policy (
    app_id,
    title,
    content,
    is_active
) VALUES (
    'com.sweetapps.pocketchord',
    '신년 이벤트',
    '🎉 새해 맞이 50% 할인',
    true
);
-- notice_policy id = 1
-- 추적 방식은 추후 결정 (해시/버전/타임스탬프)

-- 2월 공지로 변경 (같은 행 UPDATE)
UPDATE notice_policy 
SET title = '밸런타인 이벤트',
    content = '💝 2월 특별 프로모션'
WHERE app_id = 'com.sweetapps.pocketchord';
-- notice_policy id = 1 (불변)
-- 추적 방식에 따라 재표시 여부 결정


-- ===== 3. 긴급 상황 발생! (emergency_policy 사용) =====

-- ⚠️ 앱이 스토어에서 차단됨!
INSERT INTO emergency_policy (
    app_id, 
    content, 
    redirect_url, 
    new_app_id,
    is_active,
    is_dismissible
) VALUES (
    'com.sweetapps.pocketchord',  -- 차단된 기존 앱
    '⚠️ 이 앱은 더 이상 지원되지 않습니다.\n새 버전을 설치해주세요.',
    'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord.v2',
    'com.sweetapps.pocketchord.v2',
    true,
    true   -- X 버튼 허용 (Google Play 정책 준수)
);
-- emergency_policy id = 1

-- ⚠️ app_policy와 notice_policy는 그대로 유지!
-- emergency가 우선순위 1순위라서 긴급 팝업이 먼저 표시됨


-- ===== 4. 새 앱 정상 운영 =====

-- 새 앱 업데이트 정책
INSERT INTO app_policy (
    app_id, 
    is_active, 
    active_popup_type,
    min_supported_version,
    latest_version_code
) VALUES (
    'com.sweetapps.pocketchord.v2',
    true,
    'none',
    1,
    1
);
-- app_policy id = 2

-- 새 앱 공지
INSERT INTO notice_policy (
    app_id,
    title,
    content,
    is_active
) VALUES (
    'com.sweetapps.pocketchord.v2',
    '환영합니다!',
    '🎊 새로운 PocketChord에 오신 것을 환영합니다',
    true
);
-- notice_policy id = 2
```

### 테이블 상태 (3개 테이블 분리)

```
app_policy 테이블 (업데이트 정책만)
┌────┬──────────────────────────────┬───────────┬──────────────────┬────────┬─────────┐
│ id │ app_id                       │ is_active │ active_popup_type│ min_ver│ latest  │
├────┼──────────────────────────────┼───────────┼──────────────────┼────────┼─────────┤
│ 1  │ com.sweetapps.pocketchord    │ true      │ optional_update  │ 10     │ 16      │
├────┼──────────────────────────────┼───────────┼──────────────────┼────────┼─────────┤
│ 2  │ com.sweetapps.pocketchord.v2 │ true      │ none             │ 1      │ 1       │
└────┴──────────────────────────────┴───────────┴──────────────────┴────────┴─────────┘

notice_policy 테이블 (일반 공지만)
┌────┬──────────────────────────────┬───────────┬────────────┬──────────────────────┐
│ id │ app_id                       │ is_active │ title      │ content              │
├────┼──────────────────────────────┼───────────┼────────────┼──────────────────────┤
│ 1  │ com.sweetapps.pocketchord    │ true      │ 밸런타인   │ 2월 특별 프로모션    │
├────┼──────────────────────────────┼───────────┼────────────┼──────────────────────┤
│ 2  │ com.sweetapps.pocketchord.v2 │ true      │ 환영합니다 │ 새로운 PocketChord   │
└────┴──────────────────────────────┴───────────┴────────────┴──────────────────────┘

emergency_policy 테이블 (긴급 상황만)
┌────┬──────────────────────────────┬───────────┬────────────────┬──────────────────────┐
│ id │ app_id                       │ is_active │ is_dismissible │ content              │
├────┼──────────────────────────────┼───────────┼────────────────┼──────────────────────┤
│ 1  │ com.sweetapps.pocketchord    │ true      │ true           │ 새 앱 설치하세요     │
└────┴──────────────────────────────┴───────────┴────────────────┴──────────────────────┘
```

### 우선순위 로직

```kotlin
// HomeScreen.kt (수정 예정)

// 1순위: emergency_policy 확인
val emergency = emergencyPolicyRepository.getActiveEmergency(appId)
if (emergency != null) {
    showEmergencyDialog = true
    return  // 긴급 상황이면 다른 팝업 표시 안 함
}

// 2순위: app_policy에서 업데이트 확인
val policy = appPolicyRepository.getPolicy(appId)
when (policy.activePopupType) {
    "force_update" -> {
        if (policy.requiresForceUpdate(currentVersion)) {
            showUpdateDialog = true
            return
        }
    }
    "optional_update" -> {
        if (policy.recommendsUpdate(currentVersion) && 
            !isDismissed(policy.latestVersionCode)) {
            showUpdateDialog = true
            return
        }
    }
}

// 3순위: notice_policy 확인
val notice = noticePolicyRepository.getActiveNotice(appId)
if (notice != null && !isViewed(notice)) {  // 추적 방식은 추후 결정
    showAnnouncementDialog = true
}
```

### 사용자별 팝업 표시

```
기존 앱 사용자 (com.sweetapps.pocketchord)
┌─────────────────────────────────────────────┐
│ 1순위: emergency_policy 확인                │
│ → emergency 팝업 표시 (X 버튼 있음)         │
│ → 사용자가 X 클릭 가능                      │
│ → 다음 실행 시 다시 표시 (추적 없음)        │
│                                             │
│ 2순위: app_policy (optional_update)         │
│ → emergency가 우선이라 표시 안 됨           │
│                                             │
│ 3순위: notice_policy                        │
│ → emergency가 우선이라 표시 안 됨           │
└─────────────────────────────────────────────┘

새 앱 사용자 (com.sweetapps.pocketchord.v2)
┌─────────────────────────────────────────────┐
│ 1순위: emergency_policy 확인                │
│ → 없음 (정상)                               │
│                                             │
│ 2순위: app_policy (none)                    │
│ → 업데이트 없음 (정상)                      │
│                                             │
│ 3순위: notice_policy                        │
│ → notice 팝업 표시 (환영 메시지)            │
│ → 사용자가 X 클릭                           │
│ → 추적 방식에 따라 재표시 여부 결정         │
└─────────────────────────────────────────────┘
```

**결과**: 
- ✅ 3개 테이블로 **명확한 책임 분리**
- ✅ 각 테이블이 **독립적으로 관리**
- ✅ 100개 앱 = 100개 app_policy + α개 notice + α개 emergency
- ✅ 확장성 우수 (새 기능 추가 시 새 테이블로)

---

## ✅ 체크리스트

### 현재 상태

- 🟡 emergency: **별도 테이블 분리 필요** (emergency_policy)
- ✅ force_update: 정상 (app_policy에서 관리 적합)
- ✅ optional_update: 정상 (app_policy에서 관리 적합)
- 🟡 notice: **별도 테이블 분리 필요** (notice_policy)

### 1단계: emergency_policy 테이블 생성 (최우선)

- [ ] **Supabase 작업**
  - [ ] emergency_policy 테이블 생성 (SQL 실행)
  - [ ] RLS 정책 설정 (읽기 허용)
  - [ ] 테스트 데이터 삽입 확인
  
- [ ] **Kotlin 작업**
  - [ ] EmergencyPolicy 모델 클래스 생성
  - [ ] EmergencyPolicyRepository 생성
  - [ ] HomeScreen 우선순위 로직 수정 (1순위)
  - [ ] `is_dismissible` 필드 활용
  
- [ ] **테스트**
  - [ ] emergency 팝업 표시 확인
  - [ ] X 버튼 동작 확인 (isDismissible에 따라)
  - [ ] 우선순위 확인 (emergency > update > notice)

---

### 2단계: notice_policy 테이블 생성 (권장)

- [ ] **Supabase 작업**
  - [ ] notice_policy 테이블 생성 (추적 필드 제외)
  - [ ] RLS 정책 설정 (읽기 허용)
  - [ ] 테스트 데이터 삽입 확인
  
- [ ] **Kotlin 작업**
  - [ ] NoticePolicy 모델 클래스 생성
  - [ ] NoticePolicyRepository 생성
  - [ ] HomeScreen 우선순위 로직 수정 (3순위)
  - [ ] 임시 추적 로직 (추후 변경 예정)
  
- [ ] **테스트**
  - [ ] notice 팝업 표시 확인
  - [ ] X 버튼 동작 확인
  - [ ] 우선순위 확인 (emergency > update > notice)

---

### 3단계: notice 추적 방식 결정 (추후)

- [ ] **검토 사항**
  - [ ] 각 방법의 장단점 재검토
    - 해시 방식 (content.hashCode())
    - 버전 방식 (notice_version 필드)
    - 타임스탬프 방식 (updated_at + 트리거)
  - [ ] 실제 사용 시나리오 분석
  - [ ] 다른 100개 앱에도 적용 가능한지 확인
  - [ ] 행 증가 문제 없는지 확인 (모든 방식이 1개 앱 = 1개 행)
  
- [ ] **구현**
  - [ ] 선택한 방식에 따라 테이블 수정 (필요 시)
  - [ ] 추적 로직 구현
  - [ ] 기존 SharedPreferences 마이그레이션 (필요 시)
  
- [ ] **테스트**
  - [ ] 같은 행 UPDATE 후 재표시 확인
  - [ ] X 클릭 후 재실행 시 표시 안 됨 확인
  - [ ] 새 공지 띄우기 테스트

---

### Google Play 정책 준수 가이드

**emergency_policy 사용 시 주의사항**:

1. ✅ **기본값 `is_dismissible = true` 사용** (권장)
   - X 버튼 제공으로 Google Play 정책 준수
   - 추적 없이 매번 표시하여 효과 유지
   
2. ⚠️ **`is_dismissible = false`는 최후의 수단**
   - 앱이 완전히 차단되어 사용 불가능한 경우만
   - Google Play 정책 위반 가능성 고려
   - 사용 전 법률 검토 권장

3. ✅ **사용자 경험 개선**
   - X 버튼이 있어도 매번 표시 가능 (추적 안 함)
   - 사용자가 긴급 상황을 인지하면서도 앱 사용 가능
   - 부드러운 마이그레이션 유도

---

### 테이블 분리 완료 후 기대 효과

```
Before (1개 테이블):
┌─────────────────────────────────────────────────────┐
│           app_policy (모든 책임)                     │
├─────────────────────────────────────────────────────┤
│ • emergency  → 추적 복잡                            │
│ • force_update                                      │
│ • optional_update                                   │
│ • notice  → 추적 복잡                               │
│                                                      │
│ 문제: 책임 과다, 추적 로직 복잡, 확장성 낮음         │
└─────────────────────────────────────────────────────┘

After (3개 테이블):
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│app_policy    │  │notice_policy │  │emergency_... │
├──────────────┤  ├──────────────┤  ├──────────────┤
│• force_update│  │• 일반 공지   │  │• 긴급 상황   │
│• optional_...│  │• 이벤트      │  │• 앱 차단     │
│              │  │              │  │              │
│ 버전 기반    │  │ 추후 결정    │  │ 추적 없음    │
│ 추적 간단    │  │              │  │ X 버튼 제어  │
└──────────────┘  └──────────────┘  └──────────────┘

효과:
✅ 단일 책임 원칙 (각 테이블이 하나의 책임)
✅ 명확한 우선순위 (emergency > update > notice)
✅ 독립적 확장 (새 팝업 타입 추가 시 새 테이블)
✅ 관리 용이 (100개 앱 = 100개 app + α개 notice + α개 emergency)
```

---

**작성일**: 2025-11-09  
**대상 독자**: PocketChord 개발팀  
**난이도**: 고급  
**키워드**: 팝업 추적, content 해시, 앱당 1개 행, notice/emergency/force_update/optional_update

