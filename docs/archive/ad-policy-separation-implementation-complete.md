# 방안 1 구현 완료: app_policy와 ad_policy 테이블 분리

**날짜**: 2025-01-08  
**상태**: ✅ 구현 완료  
**참조**: app-policy-ad-policy-separation-analysis.md

---

## 📋 작업 요약

### 구현된 내용

✅ **Phase 1: SQL 스키마 및 새 모델 생성**
- `ad_policy` 테이블 생성 SQL 작성 완료
- `AdPolicy` 데이터 모델 생성 완료
- `AdPolicyRepository` 생성 완료

✅ **Phase 2: 기존 코드 수정**
- `InterstitialAdManager` → `AdPolicyRepository` 사용으로 변경
- `AppOpenAdManager` → `AdPolicyRepository` 사용으로 변경  
- `MainActivity` → `AdPolicyRepository` 사용으로 변경

✅ **결과**
- 팝업과 광고 정책이 **완전히 분리**됨
- 각각 독립적으로 제어 가능

---

## 📁 생성된 파일

### 1. SQL 스키마
```
docs/ad-policy-table-creation.sql
```
- `ad_policy` 테이블 생성 스크립트
- RLS 정책 설정
- 초기 데이터 삽입
- 운영 시나리오 예제 포함

### 2. Kotlin 모델
```
app/src/main/java/com/sweetapps/pocketchord/data/supabase/model/AdPolicy.kt
```
- 광고 정책 데이터 클래스
- Kotlinx Serialization 지원
- 모든 광고 제어 필드 포함

### 3. Repository
```
app/src/main/java/com/sweetapps/pocketchord/data/supabase/repository/AdPolicyRepository.kt
```
- 광고 정책 조회 로직
- 5분 캐싱 지원
- RLS 정책 준수

---

## 🔧 수정된 파일

### 1. InterstitialAdManager.kt
**변경 사항:**
- `AppPolicyRepository` → `AdPolicyRepository`로 변경
- 전면 광고 활성화 여부 조회 변경
- 빈도 제한 조회 변경

**핵심 코드:**
```kotlin
private val adPolicyRepository: AdPolicyRepository by lazy {
    val app = context.applicationContext as PocketChordApplication
    AdPolicyRepository(app.supabase)
}

private suspend fun isInterstitialEnabledFromPolicy(): Boolean {
    return adPolicyRepository.getPolicy()
        .getOrNull()
        ?.adInterstitialEnabled
        ?: true
}
```

### 2. AppOpenAdManager.kt
**변경 사항:**
- `AppPolicyRepository` → `AdPolicyRepository`로 변경
- 앱 오픈 광고 활성화 여부 조회 변경

**핵심 코드:**
```kotlin
private val adPolicyRepository: AdPolicyRepository by lazy {
    AdPolicyRepository((application as PocketChordApplication).supabase)
}

private suspend fun isAppOpenEnabledFromPolicy(): Boolean {
    return adPolicyRepository.getPolicy()
        .getOrNull()
        ?.adAppOpenEnabled
        ?: true
}
```

### 3. MainActivity.kt
**변경 사항:**
- 배너 광고 정책 조회를 `AdPolicyRepository`로 변경

**핵심 코드:**
```kotlin
LaunchedEffect(Unit) {
    val adPolicyRepo = AdPolicyRepository(app.supabase)
    
    while (true) {
        val adPolicy = adPolicyRepo.getPolicy().getOrNull()
        val newBannerEnabled = adPolicy?.adBannerEnabled ?: true
        
        if (isBannerEnabled != newBannerEnabled) {
            Log.d("MainActivity", "🔄 배너 광고 정책 변경")
            isBannerEnabled = newBannerEnabled
        }
        
        delay(5 * 60 * 1000L) // 5분마다 체크
    }
}
```

---

## 🚀 배포 절차

### Step 1: Supabase에서 ad_policy 테이블 생성

1. Supabase Dashboard → SQL Editor 접속
2. `docs/ad-policy-table-creation.sql` 내용 복사
3. SQL 실행

**확인:**
```sql
SELECT * FROM ad_policy 
WHERE app_id = 'com.sweetapps.pocketchord';
```

**예상 결과:**
```
app_id                         | is_active | ad_app_open_enabled | ad_interstitial_enabled | ad_banner_enabled
com.sweetapps.pocketchord      | true      | true                | true                    | true
```

### Step 2: 앱 빌드 및 테스트

```bash
# 빌드
gradlew assembleDebug

# 설치 및 테스트
adb install app/build/outputs/apk/debug/app-debug.apk
```

**테스트 시나리오:**
1. ✅ 앱 시작 → 앱 오픈 광고 표시 확인
2. ✅ 화면 전환 → 전면 광고 표시 확인
3. ✅ 홈 화면 → 배너 광고 표시 확인

### Step 3: Supabase에서 광고 ON/OFF 테스트

**시나리오 1: 배너 광고 끄기**
```sql
UPDATE ad_policy 
SET ad_banner_enabled = false 
WHERE app_id = 'com.sweetapps.pocketchord';
```

**확인:**
- 5분 이내에 배너 광고가 사라짐
- 로그: `🔄 배너 광고 정책 변경: 활성화 → 비활성화`

**시나리오 2: 모든 광고 끄기**
```sql
UPDATE ad_policy 
SET 
  ad_app_open_enabled = false,
  ad_interstitial_enabled = false,
  ad_banner_enabled = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

**확인:**
- 앱 오픈 광고 안 나옴
- 전면 광고 안 나옴
- 배너 광고 안 나옴

**시나리오 3: 팝업 OFF + 광고 ON**
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

**확인:**
- ✅ 팝업 안 나옴
- ✅ 광고는 나옴
- ✅ **독립적으로 제어됨!**

### Step 4: 운영 환경 배포

```bash
# Release 빌드
gradlew assembleRelease

# Play Store 업로드
```

---

## 📊 운영 가이드

### 자주 사용하는 SQL 쿼리

#### 1. 현재 상태 확인
```sql
-- 팝업 정책
SELECT app_id, is_active, active_popup_type, content 
FROM app_policy 
WHERE app_id = 'com.sweetapps.pocketchord';

-- 광고 정책
SELECT 
  app_id, 
  is_active,
  ad_app_open_enabled,
  ad_interstitial_enabled,
  ad_banner_enabled,
  ad_interstitial_max_per_hour,
  ad_interstitial_max_per_day
FROM ad_policy 
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 2. 팝업만 끄기 (광고는 유지)
```sql
UPDATE app_policy 
SET is_active = false 
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 3. 광고만 끄기 (팝업은 유지)
```sql
UPDATE ad_policy 
SET is_active = false 
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 4. 특정 광고만 제어
```sql
-- 배너만 끄기
UPDATE ad_policy 
SET ad_banner_enabled = false 
WHERE app_id = 'com.sweetapps.pocketchord';

-- 전면 광고만 끄기
UPDATE ad_policy 
SET ad_interstitial_enabled = false 
WHERE app_id = 'com.sweetapps.pocketchord';

-- 앱 오픈 광고만 끄기
UPDATE ad_policy 
SET ad_app_open_enabled = false 
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 5. 빈도 제한 조정
```sql
-- 더 보수적으로 (광고 덜 표시)
UPDATE ad_policy 
SET 
  ad_interstitial_max_per_hour = 2,
  ad_interstitial_max_per_day = 15
WHERE app_id = 'com.sweetapps.pocketchord';

-- 더 적극적으로 (광고 더 표시)
UPDATE ad_policy 
SET 
  ad_interstitial_max_per_hour = 5,
  ad_interstitial_max_per_day = 30
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 6. 명절/이벤트 대응 (모두 끄기)
```sql
-- 명절 시작
UPDATE app_policy SET is_active = false;
UPDATE ad_policy SET is_active = false;

-- 명절 종료
UPDATE app_policy SET is_active = true;
UPDATE ad_policy SET is_active = true;
```

---

## 🎯 장점 확인

### 1. 독립성
- ✅ 팝업 OFF → 광고 ON 가능
- ✅ 팝업 ON → 광고 OFF 가능
- ✅ 각자의 `is_active` 의미 명확

### 2. 확장성
- ✅ 광고 관련 필드 추가 용이
- ✅ 나중에 `notification_policy` 등 추가 가능
- ✅ 각 테이블의 책임이 명확

### 3. 운영 편의성
- ✅ SQL 쿼리가 직관적
- ✅ 실수 방지
- ✅ 팀원 이해 쉬움

### 4. 유지보수
- ✅ 코드 가독성 향상
- ✅ 각 매니저가 자신의 정책만 참조
- ✅ 의존성 최소화

---

## 📝 다음 단계 (선택사항)

### Option 1: app_policy에서 광고 컬럼 제거

**목적:** 완전한 분리, 테이블 정리

**파일:** `app-policy-remove-ad-columns.sql`

**SQL:**
```sql
ALTER TABLE app_policy
DROP COLUMN IF EXISTS ad_app_open_enabled,
DROP COLUMN IF EXISTS ad_interstitial_enabled,
DROP COLUMN IF EXISTS ad_banner_enabled,
DROP COLUMN IF EXISTS ad_interstitial_max_per_hour,
DROP COLUMN IF EXISTS ad_interstitial_max_per_day;
```

**장점:**
- 테이블 책임이 더 명확해짐
- 혼란 방지
- 데이터베이스 정리

**단점:**
- 되돌릴 수 없음 (백업 필수!)
- 구버전 앱과 호환성 문제

**권장 시기:**
- **즉시**: 개발/테스트 단계 (사용자 없음)
- **1-2주 후**: 운영 중 (새 버전 배포 후)
- **나중에 또는 안 함**: 급하지 않음 (기능적으로 문제 없음)

**상세 가이드:** `APP-POLICY-CLEANUP-GUIDE.md` 참조

### Option 2: AppPolicy 모델에서 광고 필드 제거 (Option 1 이후)

**파일:** `AppPolicy.kt`

**변경:**
```kotlin
@Serializable
data class AppPolicy(
    // ...existing code...
    
    // ===== 광고 제어 필드 제거 =====
    // @SerialName("ad_app_open_enabled")
    // val adAppOpenEnabled: Boolean = true,
    // ... (나머지 광고 필드도 제거)
)
```

**권장:** Option 1 이후에 진행

---

## 🔍 모니터링

### 로그 확인

#### 광고 정책 조회
```
D/AdPolicyRepo: ===== Ad Policy Fetch Started =====
D/AdPolicyRepo: 🔄 Supabase에서 광고 정책 새로 가져오기
D/AdPolicyRepo: Target app_id: com.sweetapps.pocketchord
D/AdPolicyRepo: Total rows fetched: 1
D/AdPolicyRepo: ✅ 광고 정책 발견!
D/AdPolicyRepo:   - App Open Ad: true
D/AdPolicyRepo:   - Interstitial Ad: true
D/AdPolicyRepo:   - Banner Ad: true
D/AdPolicyRepo:   - Max Per Hour: 3
D/AdPolicyRepo:   - Max Per Day: 20
D/AdPolicyRepo: ===== Ad Policy Fetch Completed =====
```

#### 배너 광고 정책 변경
```
D/MainActivity: 🔄 배너 광고 정책 변경: 활성화 → 비활성화
```

#### 전면 광고
```
D/InterstitialAdManager: ✅ 빈도 제한 통과: 시간당 0/3, 일일 0/20
D/InterstitialAdManager: 📊 광고 카운트 증가: 시간당 1, 일일 1
```

---

## ✅ 체크리스트

### 배포 전
- [x] `ad_policy` 테이블 생성 SQL 작성
- [x] `AdPolicy` 모델 생성
- [x] `AdPolicyRepository` 생성
- [x] `InterstitialAdManager` 수정
- [x] `AppOpenAdManager` 수정
- [x] `MainActivity` 수정
- [x] 컴파일 에러 없음 확인

### 배포 시
- [ ] Supabase에서 `ad_policy` 테이블 생성
- [ ] 초기 데이터 확인
- [ ] Debug 빌드 테스트
- [ ] 광고 ON/OFF 테스트
- [ ] Release 빌드
- [ ] Play Store 업로드

### 배포 후
- [ ] 앱 업데이트 확인
- [ ] 광고 표시 확인
- [ ] Supabase에서 정책 변경 테스트
- [ ] 실시간 제어 확인
- [ ] 로그 모니터링

---

## 🎉 결론

방안 1(테이블 분리)이 성공적으로 구현되었습니다!

**핵심 성과:**
- ✅ 팝업과 광고가 **완전히 독립적**으로 제어됨
- ✅ `is_active`의 의미적 모순 해결
- ✅ 운영 편의성 향상
- ✅ 확장성 확보

**다음 시나리오 가능:**
```
✅ 팝업 OFF + 광고 ON  (가장 흔한 경우)
✅ 팝업 ON + 광고 OFF
✅ 둘 다 OFF (명절 이벤트)
✅ 특정 광고만 ON/OFF
```

---

**작성일**: 2025-01-08  
**상태**: ✅ 구현 완료  
**배포 대기 중**: Supabase 테이블 생성 필요

