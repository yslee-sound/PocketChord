# app_policy의 is_active 문제 분석 및 개선안

**날짜**: 2025-01-08  
**주제**: 팝업과 광고의 독립적 제어 방안

---

## 🚨 문제 분석

### 현재 구조의 모순

**app_policy 테이블:**
```sql
app_policy
├── is_active (BOOLEAN)           ← 팝업 활성화 여부
├── active_popup_type (TEXT)      ← 팝업 타입
├── ad_app_open_enabled (BOOLEAN) ← 광고 활성화 여부
├── ad_interstitial_enabled (BOOLEAN)
└── ad_banner_enabled (BOOLEAN)
```

**문제 상황:**
```
is_active = false → RLS로 조회 불가 → 광고 정책도 조회 안 됨!
```

---

### 실제 운영 시나리오

#### 시나리오 1: 팝업 OFF + 광고 ON (가장 흔한 경우)

**원하는 상태:**
```
팝업: 없음 (사용자 방해 안 함)
광고: 표시 (수익 발생)
```

**현재 구조로 시도:**
```sql
-- 팝업 끄기
UPDATE app_policy 
SET is_active = false;

-- 결과: RLS로 인해 레코드 자체가 조회 안 됨!
-- → 광고 정책도 못 가져옴
-- → 기본값(true)으로 동작 (운 좋게 광고는 나옴)
-- → 하지만 의도대로 제어하는 게 아님!
```

**문제점:**
- ❌ `is_active = false` 시 RLS로 조회 차단
- ❌ 광고 정책을 의도적으로 제어 불가
- ❌ 기본값에 의존 (위험함)

---

#### 시나리오 2: 팝업 ON + 광고 OFF

**원하는 상태:**
```
팝업: 긴급 공지 표시
광고: 숨김 (사용자 경험 우선)
```

**현재 구조:**
```sql
UPDATE app_policy 
SET 
  is_active = true,              -- 팝업 활성화
  ad_banner_enabled = false;     -- 광고 비활성화

-- 결과: 작동함 ✅
-- 하지만 is_active의 의미가 애매함
```

---

#### 시나리오 3: 명절 이벤트 (팝업 + 광고 모두 OFF)

**원하는 상태:**
```
팝업: 없음
광고: 없음 (사용자 경험 최우선)
```

**현재 구조:**
```sql
-- 방법 1: is_active = false
UPDATE app_policy SET is_active = false;
→ 조회 안 됨 → 기본값으로 광고 나옴 ❌

-- 방법 2: is_active = true + 광고 OFF
UPDATE app_policy 
SET 
  is_active = true,
  ad_app_open_enabled = false,
  ad_interstitial_enabled = false,
  ad_banner_enabled = false;
→ 작동함 ✅
→ 하지만 is_active가 true인데 팝업은 없음 (혼란)
```

---

## 🎯 근본 원인

### is_active의 이중 역할

**원래 의도:**
```
is_active = 팝업 활성화 여부
```

**실제 역할:**
```
is_active = 레코드 조회 가능 여부 (RLS)
           = 팝업 + 광고 정책 모두에 영향
```

**문제:**
- 팝업과 광고는 **독립적으로 제어**되어야 함
- 하지만 `is_active` 하나로 묶여 있음
- **의미적 모순** 발생

---

## 💡 해결 방안 (3가지)

### 방안 1: 테이블 분리 ⭐⭐⭐⭐⭐ (최고 추천)

**구조:**
```
app_policy (팝업 전용)
├── id
├── app_id
├── is_active             ← 팝업만 제어
├── active_popup_type
├── content
├── download_url
├── min_supported_version
└── latest_version_code

ad_policy (광고 전용) ← 새 테이블
├── id
├── app_id
├── is_active             ← 광고 전체 ON/OFF
├── ad_app_open_enabled
├── ad_interstitial_enabled
├── ad_banner_enabled
├── ad_interstitial_max_per_hour
└── ad_interstitial_max_per_day
```

**장점:**
- ✅ 팝업과 광고 **완전히 독립**
- ✅ 각각의 `is_active` 의미 명확
- ✅ RLS 정책도 독립적으로 설정 가능
- ✅ 확장성 좋음 (광고 관련 필드 추가 용이)
- ✅ 책임 분리 (Single Responsibility)

**단점:**
- ⚠️ 테이블 1개 추가
- ⚠️ 코드 수정 필요 (경미)
- ⚠️ 마이그레이션 필요

**운영:**
```sql
-- 팝업 OFF + 광고 ON (가장 흔한 경우)
UPDATE app_policy SET is_active = false;
UPDATE ad_policy SET is_active = true;

-- 팝업 ON + 광고 OFF
UPDATE app_policy SET is_active = true;
UPDATE ad_policy SET is_active = false;

-- 둘 다 OFF
UPDATE app_policy SET is_active = false;
UPDATE ad_policy SET is_active = false;
```

**RLS 정책:**
```sql
-- app_policy: 팝업 활성화된 것만
CREATE POLICY "app_policy_select" ON app_policy
  FOR SELECT USING (is_active = true);

-- ad_policy: 광고 정책 활성화된 것만
CREATE POLICY "ad_policy_select" ON ad_policy
  FOR SELECT USING (is_active = true);
```

---

### 방안 2: is_active 의미 변경 ⭐⭐⭐⭐ (차선)

**구조 변경:**
```sql
app_policy
├── is_active             ← "레코드 활성화 여부"로 의미 변경
├── popup_enabled         ← 새 컬럼: 팝업 ON/OFF
├── active_popup_type
├── ad_app_open_enabled   ← 광고는 기존대로
├── ad_interstitial_enabled
└── ad_banner_enabled
```

**is_active의 새로운 의미:**
```
is_active = true  → 이 레코드를 사용함 (앱 정책 활성)
is_active = false → 이 레코드를 사용 안 함 (앱 정책 비활성)
```

**popup_enabled의 의미:**
```
popup_enabled = true  → 팝업 표시
popup_enabled = false → 팝업 숨김 (광고는 여전히 제어 가능)
```

**장점:**
- ✅ 테이블 분리 안 해도 됨
- ✅ 팝업과 광고 독립 제어 가능
- ✅ 코드 수정 최소화

**단점:**
- ⚠️ `is_active`의 의미가 애매해짐
- ⚠️ 컬럼 1개 추가 필요
- ⚠️ 기존 로직 일부 수정 필요

**운영:**
```sql
-- 팝업 OFF + 광고 ON
UPDATE app_policy 
SET 
  is_active = true,          -- 레코드 활성화
  popup_enabled = false,     -- 팝업 숨김
  ad_banner_enabled = true;  -- 광고 표시

-- 팝업 ON + 광고 OFF
UPDATE app_policy 
SET 
  is_active = true,
  popup_enabled = true,
  ad_banner_enabled = false;
```

---

### 방안 3: 현재 구조 유지 + 운영 규칙 ⭐⭐⭐ (타협안)

**규칙:**
```
is_active = true로 고정 유지
active_popup_type = 'none'으로 팝업 제어
```

**운영:**
```sql
-- 팝업 OFF + 광고 ON
UPDATE app_policy 
SET 
  is_active = true,              -- 항상 true
  active_popup_type = 'none',    -- 팝업 없음
  ad_banner_enabled = true;      -- 광고 ON

-- 팝업 ON + 광고 OFF
UPDATE app_policy 
SET 
  is_active = true,
  active_popup_type = 'notice',  -- 팝업 있음
  ad_banner_enabled = false;     -- 광고 OFF
```

**장점:**
- ✅ 코드 수정 **전혀 없음**
- ✅ 즉시 적용 가능
- ✅ 테이블 구조 변경 없음

**단점:**
- ❌ `is_active`의 의미 상실 (항상 true)
- ❌ 팝업 완전히 끄기 어려움
- ❌ 근본적 해결 아님 (임시방편)

---

## 📊 방안 비교표

| 항목 | 방안 1: 테이블 분리 | 방안 2: is_active 변경 | 방안 3: 현재 유지 |
|-----|-------------------|---------------------|-----------------|
| **명확성** | ⭐⭐⭐⭐⭐ 완벽 | ⭐⭐⭐⭐ 좋음 | ⭐⭐ 애매함 |
| **독립성** | ⭐⭐⭐⭐⭐ 완전 독립 | ⭐⭐⭐⭐ 독립 | ⭐⭐ 약간 의존 |
| **확장성** | ⭐⭐⭐⭐⭐ 매우 좋음 | ⭐⭐⭐ 보통 | ⭐⭐ 제한적 |
| **코드 수정** | ⚠️ 중간 | ⚠️ 소량 | ✅ 없음 |
| **테이블 변경** | ⚠️ 테이블 추가 | ⚠️ 컬럼 추가 | ✅ 없음 |
| **운영 편의성** | ⭐⭐⭐⭐⭐ 매우 쉬움 | ⭐⭐⭐⭐ 쉬움 | ⭐⭐⭐ 보통 |
| **장기 유지보수** | ⭐⭐⭐⭐⭐ 최고 | ⭐⭐⭐⭐ 좋음 | ⭐⭐ 어려움 |
| **적용 난이도** | ⚠️ 중간 | ⚠️ 쉬움 | ✅ 즉시 |

---

## 🎯 최종 추천: 방안 1 (테이블 분리)

### 이유

1. **명확한 책임 분리**
   - 팝업은 `app_policy`
   - 광고는 `ad_policy`
   - 각자의 역할이 명확

2. **독립적 제어**
   - 팝업 ON/OFF ≠ 광고 ON/OFF
   - 서로 영향 없음

3. **확장성**
   - 광고 관련 필드 추가 용이
   - 팝업 관련 필드 추가 용이
   - 나중에 `notification_policy` 등 추가 가능

4. **운영 편의성**
   - SQL 쿼리가 직관적
   - 혼란 없음
   - 실수 방지

5. **장기적 이점**
   - 코드 가독성 향상
   - 유지보수 쉬움
   - 팀원 이해 쉬움

---

## 🚀 방안 1 구현 계획 (코드 수정 최소화)

### 1단계: ad_policy 테이블 생성

```sql
-- 새 테이블 생성
CREATE TABLE ad_policy (
  id BIGSERIAL PRIMARY KEY,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  app_id TEXT UNIQUE NOT NULL,
  is_active BOOLEAN DEFAULT true NOT NULL,
  
  -- 광고 ON/OFF
  ad_app_open_enabled BOOLEAN DEFAULT true NOT NULL,
  ad_interstitial_enabled BOOLEAN DEFAULT true NOT NULL,
  ad_banner_enabled BOOLEAN DEFAULT true NOT NULL,
  
  -- 빈도 제한
  ad_interstitial_max_per_hour INT DEFAULT 3 NOT NULL,
  ad_interstitial_max_per_day INT DEFAULT 20 NOT NULL
);

-- RLS 활성화
ALTER TABLE ad_policy ENABLE ROW LEVEL SECURITY;

-- RLS 정책
CREATE POLICY "ad_policy_select" ON ad_policy
  FOR SELECT USING (is_active = true);

-- 초기 데이터
INSERT INTO ad_policy (
  app_id, 
  is_active,
  ad_app_open_enabled,
  ad_interstitial_enabled,
  ad_banner_enabled
) VALUES (
  'com.sweetapps.pocketchord',
  true,
  true,
  true,
  true
);
```

---

### 2단계: app_policy 정리

```sql
-- 광고 관련 컬럼 제거 (선택사항)
-- 기존 코드와의 호환성을 위해 남겨둘 수도 있음
ALTER TABLE app_policy
DROP COLUMN IF EXISTS ad_app_open_enabled,
DROP COLUMN IF EXISTS ad_interstitial_enabled,
DROP COLUMN IF EXISTS ad_banner_enabled,
DROP COLUMN IF EXISTS ad_interstitial_max_per_hour,
DROP COLUMN IF EXISTS ad_interstitial_max_per_day;
```

---

### 3단계: 코드 수정 (최소화)

**기존:**
```kotlin
// AppPolicy 모델 (팝업 + 광고)
val policy = policyRepository.getPolicy()
val adBannerEnabled = policy?.adBannerEnabled ?: true
```

**변경:**
```kotlin
// AdPolicy 모델 (광고 전용) - 새로 생성
data class AdPolicy(
    val appId: String,
    val isActive: Boolean,
    val adAppOpenEnabled: Boolean,
    val adInterstitialEnabled: Boolean,
    val adBannerEnabled: Boolean,
    val adInterstitialMaxPerHour: Int,
    val adInterstitialMaxPerDay: Int
)

// AdPolicyRepository - 새로 생성
class AdPolicyRepository(
    private val client: SupabaseClient,
    private val appId: String
) {
    suspend fun getPolicy(): Result<AdPolicy?> = runCatching {
        client.from("ad_policy")
            .select()
            .decodeList<AdPolicy>()
            .firstOrNull { it.appId == appId && it.isActive }
    }
}

// 사용
val adPolicy = adPolicyRepository.getPolicy()
val adBannerEnabled = adPolicy?.adBannerEnabled ?: true
```

---

## 📋 단계별 마이그레이션 가이드

### Phase 1: 준비 (1일)
- [ ] `ad_policy` 테이블 생성
- [ ] 초기 데이터 입력
- [ ] RLS 정책 설정
- [ ] 테스트 환경에서 검증

### Phase 2: 코드 수정 (1-2일)
- [ ] `AdPolicy` 모델 생성
- [ ] `AdPolicyRepository` 생성
- [ ] 광고 매니저들 수정 (Repository 변경)
- [ ] 테스트

### Phase 3: 배포 (1일)
- [ ] 운영 DB에 `ad_policy` 테이블 생성
- [ ] 앱 업데이트 배포
- [ ] 모니터링

### Phase 4: 정리 (선택사항)
- [ ] `app_policy`에서 광고 컬럼 제거
- [ ] 문서 업데이트

---

## 💼 운영 시나리오 (방안 1 적용 후)

### 시나리오 1: 팝업 OFF + 광고 ON

```sql
-- 팝업 끄기
UPDATE app_policy 
SET is_active = false 
WHERE app_id = 'com.sweetapps.pocketchord';

-- 광고 켜기
UPDATE ad_policy 
SET is_active = true 
WHERE app_id = 'com.sweetapps.pocketchord';
```

**결과:**
- ✅ 팝업 안 나옴
- ✅ 광고 나옴
- ✅ 의도대로 동작!

---

### 시나리오 2: 명절 이벤트 (모두 OFF)

```sql
-- 팝업 끄기
UPDATE app_policy 
SET is_active = false;

-- 광고 끄기
UPDATE ad_policy 
SET is_active = false;
```

**결과:**
- ✅ 팝업 안 나옴
- ✅ 광고 안 나옴
- ✅ 깔끔!

---

### 시나리오 3: 긴급 공지 + 광고 숨김

```sql
-- 팝업 켜기
UPDATE app_policy 
SET 
  is_active = true,
  active_popup_type = 'emergency',
  content = '긴급 공지';

-- 광고 끄기 (사용자 경험 우선)
UPDATE ad_policy 
SET is_active = false;
```

**결과:**
- ✅ 긴급 팝업 표시
- ✅ 광고 안 나옴
- ✅ 사용자 집중 가능!

---

## 🎯 결론

### 최종 추천: **테이블 분리 (방안 1)**

**이유:**
1. **근본적 해결** - 팝업과 광고의 독립성 완전 보장
2. **명확성** - 각자의 역할이 명확
3. **확장성** - 나중에 추가 기능 구현 용이
4. **운영 편의성** - SQL 쿼리 직관적
5. **장기 유지보수** - 코드 이해 쉬움

**단기 대안: 방안 3 (현재 유지)**
- 당장 코드 수정 어려우면
- `is_active = true` 고정
- `active_popup_type = 'none'`으로 제어
- 나중에 방안 1로 마이그레이션

---

**작성일**: 2025-01-08  
**추천**: 방안 1 (테이블 분리) ⭐⭐⭐⭐⭐  
**차선**: 방안 2 (컬럼 추가) ⭐⭐⭐⭐  
**임시**: 방안 3 (현재 유지) ⭐⭐⭐

