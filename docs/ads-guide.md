# 📱 PocketChord 광고 가이드

**업데이트**: 2025-11-08  
**대상**: AdMob 광고 (App Open, Interstitial, Banner)

---

## 📋 목차

1. [광고 개요](#광고-개요)
2. [광고 종류](#광고-종류)
3. [Supabase 제어](#supabase-제어)
4. [테스트 모드](#테스트-모드)
5. [빈도 제한](#빈도-제한)
6. [문제 해결](#문제-해결)

---

## 광고 개요

PocketChord는 3가지 AdMob 광고를 사용합니다:
- **App Open Ad**: 앱 시작 시 (콜드/웜 스타트)
- **Interstitial Ad**: 화면 전환 시 (전면 광고)
- **Banner Ad**: 하단 배너

모든 광고는 **Supabase ad_policy 테이블**로 실시간 제어됩니다.

---

## 광고 종류

### 1. App Open Ad (앱 오픈 광고)

#### 특징
- 앱 시작 시 또는 백그라운드에서 복귀 시 표시
- 첫 실행 시에는 표시 안 함
- 4시간마다 1회 제한

#### 구현 위치
- `AppOpenAdManager.kt`
- `PocketChordApplication.kt`

#### 제어
```kotlin
// Supabase에서 제어
val adPolicy = adPolicyRepository.getPolicy()
val enabled = adPolicy?.adAppOpenEnabled ?: true

if (enabled) {
    // 광고 표시
}
```

#### 테스트
1. 앱 시작 → 광고 표시 안 됨 (첫 실행)
2. 백그라운드 → 포그라운드 복귀 → 광고 표시 ✅

---

### 2. Interstitial Ad (전면 광고)

#### 특징
- 화면 전환 시 표시
- 조건:
  - 60초 간격
  - 3회 화면 전환 필요
  - 빈도 제한: 시간당 2회, 하루 15회

#### 구현 위치
- `InterstitialAdManager.kt`
- 각 화면의 `NavController`

#### 제어
```kotlin
// Supabase에서 제어
val adPolicy = adPolicyRepository.getPolicy()
val enabled = adPolicy?.adInterstitialEnabled ?: true
val maxPerHour = adPolicy?.adInterstitialMaxPerHour ?: 2
val maxPerDay = adPolicy?.adInterstitialMaxPerDay ?: 15
```

#### 빈도 제한
- **시간당**: 2회 (기본값)
- **하루**: 15회 (기본값)
- Supabase에서 실시간 조정 가능

#### 테스트
1. 홈 → 코드 → 홈 (3회 반복)
2. 60초 경과
3. 전면 광고 표시 ✅

---

### 3. Banner Ad (배너 광고)

#### 특징
- 하단 고정 표시
- 자동 새로고침 (30-120초)
- AdView 컴포저블 사용

#### 구현 위치
- `MainActivity.kt` - `AdBannerView` 컴포저블

#### 제어
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

#### 테스트
1. 앱 실행
2. 하단 배너 광고 표시 ✅

---

## Supabase 제어

### ad_policy 테이블

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

### 실시간 제어 방법

#### 모든 광고 끄기
```sql
UPDATE ad_policy 
SET is_active = false 
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 특정 광고만 끄기
```sql
-- 배너만
UPDATE ad_policy 
SET ad_banner_enabled = false;

-- 전면 광고만
UPDATE ad_policy 
SET ad_interstitial_enabled = false;

-- 앱 오픈 광고만
UPDATE ad_policy 
SET ad_app_open_enabled = false;
```

#### 빈도 제한 조정
```sql
-- 더 보수적으로
UPDATE ad_policy 
SET 
  ad_interstitial_max_per_hour = 1,
  ad_interstitial_max_per_day = 10;

-- 더 적극적으로
UPDATE ad_policy 
SET 
  ad_interstitial_max_per_hour = 3,
  ad_interstitial_max_per_day = 20;
```

### 반영 시간
- **캐싱**: 5분
- **즉시 반영**: 앱 재시작
- **자동 반영**: 5분 이내

---

## 테스트 모드

### Debug 빌드
```kotlin
// BuildConfig에서 테스트 광고 ID 사용
val adUnitId = if (BuildConfig.DEBUG) {
    "ca-app-pub-3940256099942544/3419835294" // 테스트 ID
} else {
    BuildConfig.INTERSTITIAL_AD_UNIT_ID // 실제 ID
}
```

### 테스트 광고 ID
```
App Open: ca-app-pub-3940256099942544/3419835294
Interstitial: ca-app-pub-3940256099942544/1033173712
Banner: ca-app-pub-3940256099942544/6300978111
```

### 실제 광고 ID
```
BuildConfig:
- APP_OPEN_AD_UNIT_ID
- INTERSTITIAL_AD_UNIT_ID
- BANNER_AD_UNIT_ID
```

---

## 빈도 제한

### InterstitialAdManager

#### 시간당 제한
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

#### 일일 제한
```kotlin
val dailyCount = sharedPreferences.getInt("ad_count_daily", 0)
val maxPerDay = adPolicy?.adInterstitialMaxPerDay ?: 15

if (dailyCount >= maxPerDay) {
    Log.d(TAG, "⚠️ 일일 빈도 제한 초과: $dailyCount/$maxPerDay")
    return false
}
```

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
- **시간당**: 1시간 경과 시
- **일일**: 24시간 경과 시

---

## 문제 해결

### 광고가 안 나와요

#### 1. Supabase 확인
```sql
SELECT * FROM ad_policy 
WHERE app_id = 'com.sweetapps.pocketchord';
```

확인 사항:
- `is_active = true`인가?
- 해당 광고 플래그가 `true`인가?

#### 2. 로그 확인
```bash
adb logcat | findstr "AdPolicyRepo"
adb logcat | findstr "InterstitialAdManager"
adb logcat | findstr "AppOpenAdManager"
```

#### 3. 빈도 제한 확인
```
D/InterstitialAdManager: ⚠️ 시간당 빈도 제한 초과: 2/2
```
→ 1시간 기다리거나 앱 데이터 삭제

#### 4. 네트워크 확인
- 인터넷 연결 확인
- AdMob 계정 상태 확인

### 광고가 너무 자주 나와요

#### 빈도 제한 강화
```sql
UPDATE ad_policy 
SET 
  ad_interstitial_max_per_hour = 1,
  ad_interstitial_max_per_day = 5;
```

### 테스트 광고만 나와요

#### BuildConfig 확인
```kotlin
// Release 빌드인지 확인
if (BuildConfig.DEBUG) {
    // 테스트 광고
} else {
    // 실제 광고 ← 여기가 실행되어야 함
}
```

---

## 참고 문서

- `ad-policy-separation-implementation-complete.md` - 광고 정책 분리 가이드
- `ad-policy-table-creation.sql` - 테이블 생성 SQL
- `SUPABASE-TABLE-CREATION-SUCCESS.md` - Supabase 가이드
- `DEPLOYMENT-CHECKLIST.md` - 배포 체크리스트

---

**작성일**: 2025-11-08  
**버전**: 2.0 (ad_policy 분리 후)

