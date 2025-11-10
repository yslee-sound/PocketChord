# 릴리즈 테스트 SQL 스크립트 - Phase 5 (광고 정책)

- **버전**: v2.0.0  
- **최종 업데이트**: 2025-11-10 KST  
- **대상 app_id**: `com.sweetapps.pocketchord` (릴리즈) / `com.sweetapps.pocketchord.debug` (디버그)  
- **테스트 상태**: ✅ 준비 완료
- **변경 이력**: v2.0.0 - ads-guide.md 통합 (광고 구현 가이드 추가)

---

## 0. 목적

**Phase 5**: ad_policy 테이블 기반 **광고 제어 시스템** 검증

| 구분 | 항목 | 설명 |
|------|------|------|
| **테스트 대상** | ad_policy 테이블 | 광고 활성화/비활성화 제어 |
| **광고 타입** | App Open | 앱 시작 시 전면 광고 |
| | Interstitial | 코드 조회 시 전면 광고 |
| | Banner | 화면 하단 배너 광고 |
| **제어 필드** | `is_active` | 전체 광고 ON/OFF |
| | `ad_app_open_enabled` | App Open 광고 ON/OFF |
| | `ad_interstitial_enabled` | Interstitial 광고 ON/OFF |
| | `ad_banner_enabled` | Banner 광고 ON/OFF |
| **빈도 제한** | `ad_interstitial_max_per_hour` | 시간당 최대 Interstitial 광고 수 |
| | `ad_interstitial_max_per_day` | 일일 최대 Interstitial 광고 수 |

---

## 🚀 빠른 테스트 시작

**처음 테스트하는 경우:**
➡️ **[섹션 1. 테스트 시나리오 요약](#1-테스트-시나리오-요약)** 부터 시작하세요

**긴급 광고 끄기:**
➡️ **[섹션 5. 긴급 조치](#5-긴급-조치)** 로 바로 이동

---

## 1. 테스트 시나리오 요약

| 시나리오 | 목적 | 기대 결과 |
|----------|------|-----------|
| S1 초기 상태 확인 | ad_policy 테이블 조회 | 현재 광고 설정 확인 |
| S2 전체 광고 비활성화 | `is_active = false` | 모든 광고 표시 안 됨 |
| S3 전체 광고 활성화 | `is_active = true` | 모든 광고 정상 표시 |
| S4 App Open 광고 제어 | `ad_app_open_enabled` | 앱 시작 광고 ON/OFF |
| S5 Interstitial 광고 제어 | `ad_interstitial_enabled` | 코드 조회 광고 ON/OFF |
| S6 Banner 광고 제어 | `ad_banner_enabled` | 배너 광고 ON/OFF |
| S7 빈도 제한 테스트 | `max_per_hour/day` | 광고 빈도 제한 동작 확인 |
| S8 복구 및 최종 확인 | 운영 설정으로 복구 | 릴리즈 전 최종 상태 확인 |

---

## 2. Logcat 필터 & 예상 로그

### 📋 Logcat 필터 설정

**Filter 설정**: `tag:AdMob` 또는 `tag:AdPolicy`

광고 로드, 표시, 실패 관련 로그를 표시합니다.

---

### 📊 Phase 5 주요 로그 패턴

| 로그 패턴 | 의미 | 테스트 시나리오 |
|----------|------|----------------|
| `AdPolicy: is_active=true/false` | 전체 광고 활성화 여부 | S2, S3 |
| `AdPolicy: App Open enabled=true/false` | App Open 광고 활성화 여부 | S4 |
| `AdPolicy: Interstitial enabled=true/false` | Interstitial 광고 활성화 여부 | S5 |
| `AdPolicy: Banner enabled=true/false` | Banner 광고 활성화 여부 | S6 |
| `AdMob: App Open Ad loaded` | App Open 광고 로드 성공 | S4 |
| `AdMob: Interstitial Ad loaded` | Interstitial 광고 로드 성공 | S5 |
| `AdMob: Banner Ad loaded` | Banner 광고 로드 성공 | S6 |
| `AdMob: Ad disabled by policy` | 정책에 의해 광고 비활성화 | S2, S4, S5, S6 |

---

## 3. DB 스키마 확인

### ad_policy 테이블 구조 확인

```sql
-- 테이블 구조 확인
SELECT column_name, data_type, column_default, is_nullable
FROM information_schema.columns
WHERE table_name = 'ad_policy'
ORDER BY ordinal_position;
```

**기대 결과**:

| column_name | data_type | column_default | is_nullable |
|------------|-----------|----------------|-------------|
| id | bigint | | NO |
| app_id | text | | NO |
| is_active | boolean | true | YES |
| ad_app_open_enabled | boolean | true | YES |
| ad_interstitial_enabled | boolean | true | YES |
| ad_banner_enabled | boolean | true | YES |
| ad_interstitial_max_per_hour | integer | 2 | YES |
| ad_interstitial_max_per_day | integer | 15 | YES |
| created_at | timestamp with time zone | now() | YES |
| updated_at | timestamp with time zone | now() | YES |

---

## 4. 시나리오별 테스트

### 4.S1. 초기 상태 확인

**전제조건**: 없음

**목적**: 현재 광고 설정 상태 확인

#### 📌 4.S1.1단계: 현재 설정 조회

**SQL 스크립트 - 공통**:
```sql
-- 릴리즈 & 디버그 광고 설정 확인
SELECT app_id, is_active, 
       ad_app_open_enabled, 
       ad_interstitial_enabled, 
       ad_banner_enabled,
       ad_interstitial_max_per_hour,
       ad_interstitial_max_per_day
FROM ad_policy
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug')
ORDER BY app_id;
```

**기대 결과** (운영 기본값):

| app_id | is_active | ad_app_open_enabled | ad_interstitial_enabled | ad_banner_enabled | max_per_hour | max_per_day |
|--------|-----------|---------------------|------------------------|-------------------|--------------|-------------|
| com.sweetapps.pocketchord | true | true | true | true | 2 | 15 |
| com.sweetapps.pocketchord.debug | true | true | true | true | 2 | 15 |

**확인 포인트**:
- ✅ 두 버전 모두 광고가 활성화되어 있음
- ✅ 모든 광고 타입이 활성화되어 있음
- ✅ 빈도 제한이 설정되어 있음

---

### 4.S2. 전체 광고 비활성화

| 전제조건 | 대상 | 목적 | 참고 |
|---------|------|------|------|
| S1 완료 | 디버그 앱 | `is_active = false` 설정 시 모든 광고가 표시되지 않는지 확인 | 전체 광고 비활성화는 긴급 상황에서 사용 |

---

#### 📌 4.S2.1단계: 전체 광고 비활성화 설정

**SQL 스크립트 - 디버그**:
```sql
-- 디버그 버전만 전체 광고 비활성화
UPDATE ad_policy
SET is_active = false
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 확인
SELECT app_id, is_active, 
       ad_app_open_enabled, 
       ad_interstitial_enabled, 
       ad_banner_enabled
FROM ad_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**기대 결과**:

| app_id | is_active | ad_app_open_enabled | ad_interstitial_enabled | ad_banner_enabled |
|--------|-----------|---------------------|------------------------|-------------------|
| com.sweetapps.pocketchord.debug | **false** | true | true | true |

---

#### 📌 4.S2.2단계: 앱 실행 및 광고 확인

| 실행 | 기대 로그 (AdPolicy) | UI 확인 |
|------|---------------------|---------|
| 1. 디버그 앱 강제 종료 | `AdPolicy: is_active=false` | ✅ App Open 광고 표시 안 됨 |
| 2. 앱 Cold Start로 재실행 | `AdMob: All ads disabled by policy` | ✅ 코드 조회 시 Interstitial 광고 표시 안 됨 |
| 3. 코드 여러 개 조회 | | ✅ Banner 광고 표시 안 됨 |

**확인 포인트**:
- ✅ `is_active = false`이면 개별 설정과 무관하게 모든 광고가 표시되지 않음
- ✅ 광고 로드 자체가 시도되지 않음 (빠른 응답)

---

### 4.S3. 전체 광고 활성화

| 전제조건 | 대상 | 목적 | 참고 |
|---------|------|------|------|
| S2 완료 | 디버그 앱 | `is_active = true` 설정 시 모든 광고가 정상 표시되는지 확인 | 기본 운영 상태 |

---

#### 📌 4.S3.1단계: 전체 광고 활성화 설정

**SQL 스크립트 - 디버그**:
```sql
-- 디버그 버전 전체 광고 활성화
UPDATE ad_policy
SET is_active = true
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 확인
SELECT app_id, is_active
FROM ad_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

---

#### 📌 4.S3.2단계: 앱 실행 및 광고 확인

| 실행 | 기대 로그 (AdPolicy) | UI 확인 |
|------|---------------------|---------|
| 1. 디버그 앱 강제 종료 | `AdPolicy: is_active=true` | ✅ App Open 광고 표시됨 |
| 2. 앱 재실행 | `AdMob: App Open Ad loaded` | ✅ 광고 로드 후 표시 |
| 3. 코드 조회 시도 | `AdMob: Interstitial Ad loaded` | ✅ 전면 광고 표시됨 |

---

### 4.S4. App Open 광고 제어

| 전제조건 | 대상 | 목적 | 참고 |
|---------|------|------|------|
| S3 완료 | 디버그 앱 | `ad_app_open_enabled` 제어로 앱 시작 광고만 ON/OFF | 앱 시작 시 사용자 경험 개선 |

---

#### 📌 4.S4.1단계: App Open 광고 비활성화

**SQL 스크립트 - 디버그**:
```sql
-- App Open 광고만 비활성화
UPDATE ad_policy
SET ad_app_open_enabled = false
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 확인
SELECT app_id, is_active, ad_app_open_enabled, ad_interstitial_enabled, ad_banner_enabled
FROM ad_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**기대 결과**:

| app_id | is_active | ad_app_open_enabled | ad_interstitial_enabled | ad_banner_enabled |
|--------|-----------|---------------------|------------------------|-------------------|
| com.sweetapps.pocketchord.debug | true | **false** | true | true |

---

#### 📌 4.S4.2단계: 앱 실행 및 확인

| 실행 | 기대 로그 | UI 확인 |
|------|----------|---------|
| 1. 앱 강제 종료 | `AdPolicy: App Open enabled=false` | ✅ App Open 광고 표시 안 됨 |
| 2. 앱 재실행 | `AdMob: App Open Ad disabled by policy` | ✅ 바로 메인 화면 진입 |
| 3. 코드 조회 | `AdMob: Interstitial Ad loaded` | ✅ Interstitial 광고는 정상 표시 |

---

#### 📌 4.S4.3단계: App Open 광고 재활성화

**SQL 스크립트 - 디버그**:
```sql
-- App Open 광고 재활성화
UPDATE ad_policy
SET ad_app_open_enabled = true
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

---

### 4.S5. Interstitial 광고 제어

| 전제조건 | 대상 | 목적 | 참고 |
|---------|------|------|------|
| S4 완료 | 디버그 앱 | `ad_interstitial_enabled` 제어로 전면 광고만 ON/OFF | 사용자 경험 개선 시 유용 |

---

#### 📌 4.S5.1단계: Interstitial 광고 비활성화

**SQL 스크립트 - 디버그**:
```sql
-- Interstitial 광고만 비활성화
UPDATE ad_policy
SET ad_interstitial_enabled = false
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 확인
SELECT app_id, ad_app_open_enabled, ad_interstitial_enabled, ad_banner_enabled
FROM ad_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

---

#### 📌 4.S5.2단계: 앱 실행 및 확인

| 실행 | 기대 로그 | UI 확인 |
|------|----------|---------|
| 1. 앱 재실행 | `AdPolicy: Interstitial enabled=false` | ✅ App Open 광고는 정상 표시 |
| 2. 코드 여러 개 조회 | `AdMob: Interstitial Ad disabled by policy` | ✅ 코드 조회 시 전면 광고 표시 안 됨 |
| 3. 화면 확인 | | ✅ Banner 광고는 정상 표시 |

---

#### 📌 4.S5.3단계: Interstitial 광고 재활성화

**SQL 스크립트 - 디버그**:
```sql
-- Interstitial 광고 재활성화
UPDATE ad_policy
SET ad_interstitial_enabled = true
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

---

### 4.S6. Banner 광고 제어

| 전제조건 | 대상 | 목적 | 참고 |
|---------|------|------|------|
| S5 완료 | 디버그 앱 | `ad_banner_enabled` 제어로 배너 광고만 ON/OFF | 화면 공간 확보 시 유용 |

---

#### 📌 4.S6.1단계: Banner 광고 비활성화

**SQL 스크립트 - 디버그**:
```sql
-- Banner 광고만 비활성화
UPDATE ad_policy
SET ad_banner_enabled = false
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 확인
SELECT app_id, ad_app_open_enabled, ad_interstitial_enabled, ad_banner_enabled
FROM ad_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

---

#### 📌 4.S6.2단계: 앱 실행 및 확인

| 실행 | 기대 로그 | UI 확인 |
|------|----------|---------|
| 1. 앱 재실행 | `AdPolicy: Banner enabled=false` | ✅ App Open 광고는 정상 표시 |
| 2. 메인 화면 확인 | `AdMob: Banner Ad disabled by policy` | ✅ 화면 하단 배너 광고 표시 안 됨 |
| 3. 코드 조회 | `AdMob: Interstitial Ad loaded` | ✅ Interstitial 광고는 정상 표시 |

---

#### 📌 4.S6.3단계: Banner 광고 재활성화

**SQL 스크립트 - 디버그**:
```sql
-- Banner 광고 재활성화
UPDATE ad_policy
SET ad_banner_enabled = true
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

---

### 4.S7. 빈도 제한 테스트

| 전제조건 | 대상 | 목적 | 참고 |
|---------|------|------|------|
| S6 완료 | 디버그 앱 | `max_per_hour/day` 설정으로 광고 빈도 제한 동작 확인 | 사용자 경험 개선 |

---

#### 📌 4.S7.1단계: 빈도 제한 설정 (테스트용)

**SQL 스크립트 - 디버그**:
```sql
-- 테스트를 위해 빈도 제한을 낮게 설정
UPDATE ad_policy
SET ad_interstitial_max_per_hour = 1,  -- 시간당 1회로 제한
    ad_interstitial_max_per_day = 3     -- 일일 3회로 제한
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 확인
SELECT app_id, ad_interstitial_max_per_hour, ad_interstitial_max_per_day
FROM ad_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

---

#### 📌 4.S7.2단계: 빈도 제한 동작 확인

| 실행 | 기대 로그 | UI 확인 |
|------|----------|---------|
| 1. 앱 재실행 | `AdPolicy: Interstitial max_per_hour=1` | - |
| 2. 코드 1번 조회 | `AdMob: Interstitial Ad shown (1/1 per hour)` | ✅ 첫 번째 광고 표시됨 |
| 3. 코드 2번 조회 | `AdMob: Interstitial Ad skipped (hourly limit reached)` | ✅ 광고 표시 안 됨 (제한 도달) |
| 4. 1시간 후 코드 조회 | `AdMob: Interstitial Ad shown (1/1 per hour)` | ✅ 다시 광고 표시됨 |

**⚠️ 참고**: 
- 실제로 1시간을 기다릴 수 없으므로, 로그로 제한 동작만 확인
- 일일 제한은 3회까지 가능, 4번째부터 제한됨

---

#### 📌 4.S7.3단계: 빈도 제한 복구 (운영 설정)

**SQL 스크립트 - 디버그**:
```sql
-- 운영 설정으로 복구
UPDATE ad_policy
SET ad_interstitial_max_per_hour = 2,   -- 기본값
    ad_interstitial_max_per_day = 15    -- 기본값
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

---

### 4.S8. 복구 및 최종 확인

| 전제조건 | 대상 | 목적 | 참고 |
|---------|------|------|------|
| S1~S7 완료 | 릴리즈 + 디버그 | 운영 설정으로 복구 및 최종 상태 확인 | 릴리즈 전 필수 |

---

#### 📌 4.S8.1단계: 운영 설정으로 복구

**SQL 스크립트 - 공통**:
```sql
-- 두 버전 모두 운영 설정으로 복구
UPDATE ad_policy
SET is_active = true,
    ad_app_open_enabled = true,
    ad_interstitial_enabled = true,
    ad_banner_enabled = true,
    ad_interstitial_max_per_hour = 2,
    ad_interstitial_max_per_day = 15
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');

-- 최종 확인
SELECT app_id, is_active, 
       ad_app_open_enabled, 
       ad_interstitial_enabled, 
       ad_banner_enabled,
       ad_interstitial_max_per_hour,
       ad_interstitial_max_per_day
FROM ad_policy
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug')
ORDER BY app_id;
```

**기대 결과** (최종 운영 상태):

| app_id | is_active | ad_app_open_enabled | ad_interstitial_enabled | ad_banner_enabled | max_per_hour | max_per_day |
|--------|-----------|---------------------|------------------------|-------------------|--------------|-------------|
| com.sweetapps.pocketchord | true | true | true | true | 2 | 15 |
| com.sweetapps.pocketchord.debug | true | true | true | true | 2 | 15 |

---

#### 📌 4.S8.2단계: 릴리즈 버전 최종 확인

| 실행 | 확인 포인트 | S8 완료 조건 |
|------|-----------|------------|
| 1. 릴리즈 앱 실행 | ✅ App Open 광고 정상 표시 | ✅ 모든 광고 타입 정상 동작 |
| 2. 코드 조회 | ✅ Interstitial 광고 정상 표시 | ✅ 빈도 제한 설정 확인 (2/시간, 15/일) |
| 3. 메인 화면 확인 | ✅ Banner 광고 정상 표시 | ✅ 릴리즈 버전 운영 설정 확인 |

---

## 5. 긴급 조치

### 🚨 긴급: 모든 광고 즉시 끄기

```sql
-- 릴리즈 버전 광고 즉시 비활성화
UPDATE ad_policy
SET is_active = false
WHERE app_id = 'com.sweetapps.pocketchord';

-- 확인
SELECT app_id, is_active FROM ad_policy WHERE app_id = 'com.sweetapps.pocketchord';
```

**효과**: 
- ✅ 즉시 모든 광고가 비활성화됨
- ✅ 앱 재시작 없이 적용됨 (다음 광고 로드 시점부터)

---

### 🔧 특정 광고만 끄기

```sql
-- Interstitial 광고만 즉시 끄기 (예: 사용자 불만 시)
UPDATE ad_policy
SET ad_interstitial_enabled = false
WHERE app_id = 'com.sweetapps.pocketchord';

-- App Open 광고만 끄기 (예: 앱 시작 경험 개선)
UPDATE ad_policy
SET ad_app_open_enabled = false
WHERE app_id = 'com.sweetapps.pocketchord';

-- Banner 광고만 끄기 (예: UI 개선)
UPDATE ad_policy
SET ad_banner_enabled = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

## 7. 광고 구현 가이드

### 7.1 광고 종류 및 특징

#### App Open Ad (앱 오픈 광고)

**특징**:
- 앱 시작 시 또는 백그라운드에서 복귀 시 표시
- 첫 실행 시에는 표시 안 함
- 4시간마다 1회 제한

**구현 위치**:
- `AppOpenAdManager.kt`
- `PocketChordApplication.kt`

**제어 코드**:
```kotlin
// Supabase에서 제어
val adPolicy = adPolicyRepository.getPolicy()
val enabled = adPolicy?.adAppOpenEnabled ?: true

if (enabled) {
    // 광고 표시
}
```

**테스트 방법**:
1. 앱 시작 → 광고 표시 안 됨 (첫 실행)
2. 백그라운드 → 포그라운드 복귀 → 광고 표시 ✅

---

#### Interstitial Ad (전면 광고)

**특징**:
- 화면 전환 시 표시
- 조건:
  - 60초 간격
  - 3회 화면 전환 필요
  - 빈도 제한: 시간당 2회, 하루 15회

**구현 위치**:
- `InterstitialAdManager.kt`
- 각 화면의 `NavController`

**제어 코드**:
```kotlin
// Supabase에서 제어
val adPolicy = adPolicyRepository.getPolicy()
val enabled = adPolicy?.adInterstitialEnabled ?: true
val maxPerHour = adPolicy?.adInterstitialMaxPerHour ?: 2
val maxPerDay = adPolicy?.adInterstitialMaxPerDay ?: 15
```

**빈도 제한 구현**:

시간당 제한:
```kotlin
private suspend fun checkFrequencyLimit(): Boolean {
    val hourlyCount = sharedPreferences.getInt("ad_count_hourly", 0)
    val maxPerHour = adPolicy?.adInterstitialMaxPerHour ?: 2
    
    if (hourlyCount >= maxPerHour) {
        Log.d(TAG, "⚠️ 시간당 빈도 제한 초과: $hourlyCount/$maxPerHour")
        return false
    }
    return true
}
```

일일 제한:
```kotlin
val dailyCount = sharedPreferences.getInt("ad_count_daily", 0)
val maxPerDay = adPolicy?.adInterstitialMaxPerDay ?: 15

if (dailyCount >= maxPerDay) {
    Log.d(TAG, "⚠️ 일일 빈도 제한 초과: $dailyCount/$maxPerDay")
    return false
}
```

**테스트 방법**:
1. 홈 → 코드 → 홈 (3회 반복)
2. 60초 경과
3. 전면 광고 표시 ✅

---

#### Banner Ad (배너 광고)

**특징**:
- 하단 고정 표시
- 자동 새로고침 (30-120초)
- AdView 컴포저블 사용

**구현 위치**:
- `MainActivity.kt` - `AdBannerView` 컴포저블

**제어 코드**:
```kotlin
// Supabase에서 제어
LaunchedEffect(Unit) {
    val adPolicyRepo = AdPolicyRepository(app.supabase)
    
    while (true) {
        val adPolicy = adPolicyRepo.getPolicy().getOrNull()
        val bannerEnabled = adPolicy?.adBannerEnabled ?: true
        
        if (isBannerEnabled != bannerEnabled) {
            isBannerEnabled = bannerEnabled
        }
        
        delay(5 * 60 * 1000L) // 5분마다 체크
    }
}
```

**테스트 방법**:
1. 앱 실행
2. 하단 배너 광고 표시 ✅

---

### 7.2 테스트 모드

#### Debug 빌드
```kotlin
// BuildConfig에서 테스트 광고 ID 사용
val adUnitId = if (BuildConfig.DEBUG) {
    "ca-app-pub-3940256099942544/3419835294" // 테스트 ID
} else {
    BuildConfig.INTERSTITIAL_AD_UNIT_ID // 실제 ID
}
```

#### 테스트 광고 ID
```
App Open: ca-app-pub-3940256099942544/3419835294
Interstitial: ca-app-pub-3940256099942544/1033173712
Banner: ca-app-pub-3940256099942544/6300978111
```

#### 실제 광고 ID
```
BuildConfig:
- APP_OPEN_AD_UNIT_ID
- INTERSTITIAL_AD_UNIT_ID
- BANNER_AD_UNIT_ID
```

---

### 7.3 빈도 제한 상세

#### 카운트 증가
```kotlin
private fun incrementFrequencyCount() {
    val hourlyCount = sharedPreferences.getInt("ad_count_hourly", 0)
    val dailyCount = sharedPreferences.getInt("ad_count_daily", 0)
    
    sharedPreferences.edit {
        putInt("ad_count_hourly", hourlyCount + 1)
        putInt("ad_count_daily", dailyCount + 1)
    }
}
```

#### 자동 리셋
- **시간당**: 1시간 경과 시 자동 리셋
- **일일**: 24시간 경과 시 자동 리셋

---

### 7.4 Supabase 제어 상세

#### ad_policy 테이블 구조
```sql
CREATE TABLE ad_policy (
  id BIGSERIAL PRIMARY KEY,
  created_at TIMESTAMP,
  app_id TEXT UNIQUE NOT NULL,
  is_active BOOLEAN DEFAULT true,
  
  -- 광고 ON/OFF
  ad_app_open_enabled BOOLEAN DEFAULT true,
  ad_interstitial_enabled BOOLEAN DEFAULT true,
  ad_banner_enabled BOOLEAN DEFAULT true,
  
  -- 빈도 제한
  ad_interstitial_max_per_hour INT DEFAULT 2,
  ad_interstitial_max_per_day INT DEFAULT 15
);
```

#### 실시간 제어 방법

**모든 광고 끄기**:
```sql
UPDATE ad_policy 
SET is_active = false 
WHERE app_id = 'com.sweetapps.pocketchord';
```

**특정 광고만 끄기**:
```sql
-- 배너만
UPDATE ad_policy 
SET ad_banner_enabled = false
WHERE app_id = 'com.sweetapps.pocketchord';

-- 전면 광고만
UPDATE ad_policy 
SET ad_interstitial_enabled = false
WHERE app_id = 'com.sweetapps.pocketchord';

-- 앱 오픈 광고만
UPDATE ad_policy 
SET ad_app_open_enabled = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

**빈도 제한 조정**:
```sql
-- 더 보수적으로 (광고 적게)
UPDATE ad_policy 
SET 
  ad_interstitial_max_per_hour = 1,
  ad_interstitial_max_per_day = 10
WHERE app_id = 'com.sweetapps.pocketchord';

-- 더 적극적으로 (광고 많이)
UPDATE ad_policy 
SET 
  ad_interstitial_max_per_hour = 3,
  ad_interstitial_max_per_day = 20
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 반영 시간
- **캐싱**: 5분
- **즉시 반영**: 앱 재시작
- **자동 반영**: 5분 이내

---

### 7.5 문제 해결

#### 광고가 안 나올 때

**1. Supabase 확인**:
```sql
SELECT * FROM ad_policy 
WHERE app_id = 'com.sweetapps.pocketchord';
```

확인 사항:
- ✅ `is_active = true`인가?
- ✅ 해당 광고 플래그가 `true`인가?

**2. 로그 확인**:
```bash
adb logcat | findstr "AdPolicyRepo"
adb logcat | findstr "InterstitialAdManager"
adb logcat | findstr "AppOpenAdManager"
```

**3. 빈도 제한 확인**:
```
D/InterstitialAdManager: ⚠️ 시간당 빈도 제한 초과: 2/2
```
→ 1시간 기다리거나 앱 데이터 삭제

**4. 캐시 초기화**:
```bash
adb shell pm clear com.sweetapps.pocketchord.debug
```

**5. 네트워크 확인**:
- AdMob 서버 연결 확인
- 테스트 광고 ID 사용 여부 확인

---

## 8. 운영 가이드 (기존 섹션 6)

### 📊 일반적인 광고 설정 조합

#### 1️⃣ 기본 운영 (권장)
```sql
UPDATE ad_policy
SET is_active = true,
    ad_app_open_enabled = true,
    ad_interstitial_enabled = true,
    ad_banner_enabled = true,
    ad_interstitial_max_per_hour = 2,
    ad_interstitial_max_per_day = 15
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 2️⃣ 사용자 경험 우선 (App Open 제거)
```sql
UPDATE ad_policy
SET is_active = true,
    ad_app_open_enabled = false,  -- 앱 시작 시 광고 없음
    ad_interstitial_enabled = true,
    ad_banner_enabled = true,
    ad_interstitial_max_per_hour = 2,
    ad_interstitial_max_per_day = 15
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 3️⃣ 수익 최적화 (빈도 제한 완화)
```sql
UPDATE ad_policy
SET is_active = true,
    ad_app_open_enabled = true,
    ad_interstitial_enabled = true,
    ad_banner_enabled = true,
    ad_interstitial_max_per_hour = 3,  -- 증가
    ad_interstitial_max_per_day = 20   -- 증가
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 4️⃣ 최소 광고 (Banner만)
```sql
UPDATE ad_policy
SET is_active = true,
    ad_app_open_enabled = false,
    ad_interstitial_enabled = false,
    ad_banner_enabled = true  -- Banner만 표시
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

## ✅ Phase 5 완료 체크리스트

- [ ] S1: 초기 상태 확인 완료
- [ ] S2: 전체 광고 비활성화 테스트 완료
- [ ] S3: 전체 광고 활성화 테스트 완료
- [ ] S4: App Open 광고 제어 테스트 완료
- [ ] S5: Interstitial 광고 제어 테스트 완료
- [ ] S6: Banner 광고 제어 테스트 완료
- [ ] S7: 빈도 제한 테스트 완료
- [ ] S8: 운영 설정으로 복구 완료
- [ ] 릴리즈 버전 최종 확인 완료
- [ ] 디버그 버전 최종 확인 완료

---

## 📚 관련 문서

- **[POPUP-SYSTEM-GUIDE.md](POPUP-SYSTEM-GUIDE.md)** - 전체 팝업 시스템 가이드
- **[RELEASE-TEST-CHECKLIST.md](RELEASE-TEST-CHECKLIST.md)** - 릴리즈 테스트 체크리스트

---

**문서 버전**: v2.0.0 (광고 구현 가이드 통합)  
**마지막 수정**: 2025-11-10 KST

