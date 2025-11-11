# Update Policy 완전 가이드

**버전**: v2.0.0  
**최종 업데이트**: 2025-11-10 KST  
**목적**: update_policy의 모든 기능과 운영 전략  
**상태**: ✅ 통합 완료

---

## 📝 변경 이력

### v2.0.0 (2025-11-10)
- ✅ 3개 문서 통합 (USAGE-GUIDE, FORCE-CONVERSION, TIME-BASED-STRATEGY)
- ✅ Phase 2.5 (시간 기반 재표시) 구현 완료 반영
- ✅ 실전 운영 가이드 강화

### v1.0.1 (2025-11-09)
- ✅ 문서 업데이트 (emergency_policy button_text 관련 정보 동기화)

---

## 📚 목차

1. [핵심 개념](#1-핵심-개념)
2. [실제 사용 시나리오](#2-실제-사용-시나리오)
3. [시간 기반 재표시 (Phase 2.5)](#3-시간-기반-재표시-phase-25)
4. [강제 전환 메커니즘](#4-강제-전환-메커니즘)
5. [운영 가이드](#5-운영-가이드)
6. [문제 해결](#6-문제-해결)

---

## 1. 핵심 개념

### 1.1 target_version_code란?

**Play Store에 올릴 다음 버전의 versionCode입니다.**

```
현재 앱: versionCode = 10
다음 업데이트: versionCode = 11

→ target_version_code = 11 로 설정
→ 버전 10 사용자에게 "업데이트하세요" 팝업 표시
```

### 1.2 업데이트 타입

| 타입 | 설정 | 사용자 경험 | 사용 시기 |
|------|------|-----------|----------|
| **선택적 업데이트** | `is_force_update = false` | "나중에" 버튼 있음 | 일반 업데이트 |
| **강제 업데이트** | `is_force_update = true` | "나중에" 버튼 없음, 뒤로가기 차단 | 중요 버그 수정, 보안 이슈 |

---

## 2. 실제 사용 시나리오

### 2.1 시나리오: 새 버전 출시 (정상적인 흐름)

#### Step 1: 새 버전 빌드
```
현재 Play Store: versionCode = 10
새 APK 빌드: versionCode = 11
```

#### Step 2: Supabase 설정 (Play Store 출시 전)

**선택적 업데이트 (권장)**:
```sql
UPDATE update_policy
SET target_version_code = 11,
    is_force_update = false,
    is_active = true
WHERE app_id = 'com.sweetapps.pocketchord';
```

**강제 업데이트 (중요 업데이트)**:
```sql
UPDATE update_policy
SET target_version_code = 11,
    is_force_update = true,
    is_active = true
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 3: Play Store 출시
- 새 APK (versionCode = 11) 업로드
- 심사 통과 후 배포

#### Step 4: 사용자 경험
```
버전 10 사용자:
├─ 앱 실행
├─ 업데이트 팝업 표시
├─ 선택적: "업데이트" 또는 "나중에" 선택 가능
└─ 강제: "업데이트"만 가능

버전 11로 업데이트한 사용자:
└─ 팝업 표시 안 됨 (자동 초기화)
```

---

### 2.2 시나리오: 긴급 상황 (버그 발견)

#### 상황
Play Store에 버전 11이 배포되었는데, 심각한 버그 발견!

#### 대응
```sql
-- 즉시 강제 업데이트로 전환
UPDATE update_policy
SET is_force_update = true
WHERE app_id = 'com.sweetapps.pocketchord';
```

**효과**:
- ✅ 즉시 적용 (앱 재시작 없이)
- ✅ 기존 "선택적"으로 표시된 사용자도 다음 실행 시 "강제"로 변경
- ✅ "나중에" 버튼이 사라지고 업데이트만 가능

---

### 2.3 시나리오: 업데이트 중지

#### 상황
새 버전에 문제가 있어서 업데이트를 일시적으로 중지하고 싶음

#### 대응
```sql
-- 업데이트 팝업 비활성화
UPDATE update_policy
SET is_active = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

**효과**:
- ✅ 모든 사용자에게 업데이트 팝업 표시 안 됨
- ✅ 문제 해결 후 `is_active = true`로 재활성화

---

## 3. 시간 기반 재표시 (Phase 2.5)

### 3.1 개요

**"나중에" 클릭 후 일정 시간이 지나면 다시 팝업을 표시**

```
기존 방식 (❌):
"나중에" 클릭 → 다음 버전까지 영구히 숨김
→ 사용자가 업데이트를 잊어버림

Phase 2.5 방식 (✅):
"나중에" 클릭 → 24시간 후 다시 표시
→ 3회까지 허용 → 강제 전환
```

### 3.2 주요 필드

| 필드 | 설명 | 기본값 | 우선순위 |
|------|------|--------|---------|
| `reshow_interval_seconds` | 재표시 간격 (초) | NULL | 1순위 (테스트용) |
| `reshow_interval_minutes` | 재표시 간격 (분) | NULL | 2순위 (테스트용) |
| `reshow_interval_hours` | 재표시 간격 (시간) | 24 | 3순위 (운영용) |
| `max_later_count` | 최대 "나중에" 횟수 | 3 | - |

**⚠️ 운영 환경 필수 설정**:
- `reshow_interval_seconds` = NULL
- `reshow_interval_minutes` = NULL
- `reshow_interval_hours` = 24 (권장)

### 3.3 동작 흐름

```
1회차: "나중에" 클릭
├─ laterCount = 1
├─ 24시간 대기
└─ 24시간 후 재표시

2회차: "나중에" 클릭
├─ laterCount = 2
├─ 24시간 대기
└─ 24시간 후 재표시

3회차: "나중에" 클릭
├─ laterCount = 3
├─ 24시간 대기
└─ 24시간 후 재표시

4회차: laterCount >= max_later_count (3)
└─ 🚨 강제 전환!
    ├─ "나중에" 버튼 숨김
    ├─ "업데이트" 버튼만 표시
    └─ 뒤로가기 차단
```

### 3.4 운영 설정 예시

#### 기본 설정 (24시간, 3회)
```sql
UPDATE update_policy
SET reshow_interval_hours = 24,
    reshow_interval_minutes = NULL,
    reshow_interval_seconds = NULL,
    max_later_count = 3
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 적극적 업데이트 유도 (12시간, 2회)
```sql
UPDATE update_policy
SET reshow_interval_hours = 12,
    max_later_count = 2
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 완화 설정 (48시간, 5회)
```sql
UPDATE update_policy
SET reshow_interval_hours = 48,
    max_later_count = 5
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

## 4. 강제 전환 메커니즘

### 4.1 핵심 개념

**Supabase의 `is_force_update`를 변경하지 않고, 클라이언트에서 조건에 따라 강제처럼 동작**

```
Supabase (서버):
├─ is_force_update: false  (변경 안 함)
└─ max_later_count: 3

앱 (클라이언트):
├─ laterCount < 3 → 선택적 업데이트
│   ├─ "업데이트" 버튼
│   └─ "나중에" 버튼 ✅
│
└─ laterCount >= 3 → 강제처럼 동작
    ├─ "업데이트" 버튼
    └─ "나중에" 버튼 ❌ (숨김)
```

### 4.2 구현 방법

#### 방법 1: "나중에" 버튼 숨기기 (✅ 추천)
```kotlin
val showLaterButton = laterCount < maxLaterCount && !isForceUpdate
```

#### 방법 2: Dialog 취소 불가 설정
```kotlin
if (laterCount >= maxLaterCount || isForceUpdate) {
    dialog.setCancelable(false)
    dialog.setOnKeyListener { _, keyCode, _ ->
        keyCode == KeyEvent.KEYCODE_BACK // 뒤로가기 차단
    }
}
```

### 4.3 사용자 경험

| 횟수 | laterCount | 동작 | 사용자 경험 |
|------|-----------|------|-----------|
| 1회차 | 0 → 1 | 선택적 | ✅ "나중에" 가능 |
| 2회차 | 1 → 2 | 선택적 | ✅ "나중에" 가능 |
| 3회차 | 2 → 3 | 선택적 | ✅ "나중에" 가능 (마지막) |
| 4회차 | 3 | 강제 전환 | ❌ "업데이트"만 가능 |

---

## 5. 운영 가이드

### 5.1 일반적인 설정 조합

#### 1️⃣ 일반 업데이트 (권장)
```sql
UPDATE update_policy
SET target_version_code = 11,
    is_force_update = false,
    reshow_interval_hours = 24,
    max_later_count = 3,
    is_active = true
WHERE app_id = 'com.sweetapps.pocketchord';
```

**특징**:
- 24시간마다 재표시
- 3회까지 "나중에" 가능
- 4회차에 강제 전환

#### 2️⃣ 중요 업데이트 (적극 유도)
```sql
UPDATE update_policy
SET target_version_code = 11,
    is_force_update = false,
    reshow_interval_hours = 12,
    max_later_count = 2,
    is_active = true
WHERE app_id = 'com.sweetapps.pocketchord';
```

**특징**:
- 12시간마다 재표시
- 2회까지 "나중에" 가능
- 3회차에 강제 전환

#### 3️⃣ 긴급 업데이트 (즉시 강제)
```sql
UPDATE update_policy
SET target_version_code = 11,
    is_force_update = true,
    is_active = true
WHERE app_id = 'com.sweetapps.pocketchord';
```

**특징**:
- 즉시 강제 업데이트
- "나중에" 버튼 없음
- 뒤로가기 차단

#### 4️⃣ 완화 업데이트 (사용자 친화적)
```sql
UPDATE update_policy
SET target_version_code = 11,
    is_force_update = false,
    reshow_interval_hours = 48,
    max_later_count = 5,
    is_active = true
WHERE app_id = 'com.sweetapps.pocketchord';
```

**특징**:
- 48시간마다 재표시
- 5회까지 "나중에" 가능
- 6회차에 강제 전환

---

### 5.2 상황별 대응 매트릭스

| 상황 | is_force_update | reshow_hours | max_later_count | 설명 |
|------|----------------|--------------|-----------------|------|
| 일반 기능 추가 | false | 48 | 5 | 사용자 친화적 |
| 버그 수정 | false | 24 | 3 | 표준 설정 |
| 중요 버그 수정 | false | 12 | 2 | 적극 유도 |
| 보안 이슈 | true | - | - | 즉시 강제 |
| 긴급 상황 | true | - | - | 즉시 강제 |

---

### 5.3 테스트 환경 설정

**디버그 빌드 (빠른 테스트)**:
```sql
UPDATE update_policy
SET target_version_code = 10,
    is_force_update = false,
    reshow_interval_seconds = 60,  -- 1분
    max_later_count = 3,
    is_active = true
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**⚠️ 릴리즈 전 필수 확인**:
```sql
-- 운영 설정으로 복구 확인
SELECT app_id, 
       reshow_interval_hours,
       reshow_interval_minutes,
       reshow_interval_seconds,
       max_later_count
FROM update_policy
WHERE app_id = 'com.sweetapps.pocketchord';
```

**기대 결과**:
- `reshow_interval_seconds` = NULL ✅
- `reshow_interval_minutes` = NULL ✅
- `reshow_interval_hours` = 24 ✅
- `max_later_count` = 3 ✅

---

## 6. 문제 해결

### 6.1 팝업이 표시되지 않을 때

**체크리스트**:
```sql
SELECT app_id, 
       is_active,
       target_version_code,
       is_force_update
FROM update_policy
WHERE app_id = 'com.sweetapps.pocketchord';
```

**확인 사항**:
1. ✅ `is_active = true`인가?
2. ✅ `target_version_code`가 현재 앱 버전보다 높은가?
3. ✅ 앱을 완전히 재시작했는가?

**해결**:
```sql
-- 1. is_active 활성화
UPDATE update_policy
SET is_active = true
WHERE app_id = 'com.sweetapps.pocketchord';

-- 2. target_version_code 확인 및 조정
UPDATE update_policy
SET target_version_code = 100  -- 충분히 높은 값
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

### 6.2 "나중에" 클릭 후 영구히 숨김

**원인**: Phase 2.5 구현 전 버전 또는 시간이 아직 경과하지 않음

**확인**:
```sql
SELECT app_id,
       reshow_interval_hours,
       max_later_count
FROM update_policy
WHERE app_id = 'com.sweetapps.pocketchord';
```

**해결**:
```sql
-- Phase 2.5 필드 설정
UPDATE update_policy
SET reshow_interval_hours = 24,
    reshow_interval_minutes = NULL,
    reshow_interval_seconds = NULL,
    max_later_count = 3
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

### 6.3 강제 전환이 작동하지 않음

**원인**: laterCount가 max_later_count에 도달하지 않음

**디버그**:
- Logcat 필터: `tag:UpdateLater`
- 확인 로그: `📊 Current later count: X / Y`

**해결**:
```sql
-- max_later_count 낮추기 (테스트용)
UPDATE update_policy
SET max_later_count = 1
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

---

**문서 버전**: v2.0.0  
**마지막 수정**: 2025-11-10 KST
