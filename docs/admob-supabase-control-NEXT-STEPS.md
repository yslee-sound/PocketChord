# AdMob Supabase 제어 - 다음 단계 가이드

## 🎯 지금 해야 할 일

### 1️⃣ Supabase 스키마 적용 (필수!)

**Supabase 대시보드 접속**:
1. https://supabase.com 로그인
2. PocketChord 프로젝트 선택
3. SQL Editor 클릭

**SQL 스크립트 실행**:
```sql
-- 파일: docs/supabase-ad-control-schema.sql 내용 복사 후 실행

-- 1. 컬럼 추가
ALTER TABLE app_policy
ADD COLUMN IF NOT EXISTS ad_app_open_enabled BOOLEAN DEFAULT true,
ADD COLUMN IF NOT EXISTS ad_interstitial_enabled BOOLEAN DEFAULT true,
ADD COLUMN IF NOT EXISTS ad_banner_enabled BOOLEAN DEFAULT true,
ADD COLUMN IF NOT EXISTS ad_interstitial_max_per_hour INT DEFAULT 3,
ADD COLUMN IF NOT EXISTS ad_interstitial_max_per_day INT DEFAULT 20;

-- 2. 기존 데이터 업데이트
UPDATE app_policy
SET 
  ad_app_open_enabled = COALESCE(ad_app_open_enabled, true),
  ad_interstitial_enabled = COALESCE(ad_interstitial_enabled, true),
  ad_banner_enabled = COALESCE(ad_banner_enabled, true),
  ad_interstitial_max_per_hour = COALESCE(ad_interstitial_max_per_hour, 3),
  ad_interstitial_max_per_day = COALESCE(ad_interstitial_max_per_day, 20)
WHERE ad_app_open_enabled IS NULL;

-- 3. 확인
SELECT 
  app_id,
  ad_app_open_enabled,
  ad_interstitial_enabled,
  ad_banner_enabled,
  ad_interstitial_max_per_hour,
  ad_interstitial_max_per_day
FROM app_policy;
```

✅ **실행 후 확인사항**:
- 컬럼이 추가되었는지
- 기존 레코드에 기본값이 설정되었는지
- `com.sweetapps.pocketchord` 레코드가 있는지

---

### 2️⃣ 앱 빌드 및 실행

**방법 1: Android Studio**
```
1. Android Studio에서 프로젝트 열기
2. Build > Make Project
3. Run > Run 'app'
```

**방법 2: 명령줄**
```cmd
cd G:\Workspace\PocketChord
gradlew assembleDebug
gradlew installDebug
```

---

### 3️⃣ 로그 확인

**Android Studio Logcat에서 확인**:
```
필터: "AppPolicyRepo|MainActivity|InterstitialAdManager|AppOpenAdManager"
```

**기대되는 로그**:
```
AppPolicyRepo: ===== Policy Fetch Started =====
AppPolicyRepo: 🔄 Supabase에서 정책 새로 가져오기
AppPolicyRepo: ✅ Policy found
AppPolicyRepo: 🔍 광고 정책:
AppPolicyRepo:   - App Open: true
AppPolicyRepo:   - Interstitial: true
AppPolicyRepo:   - Banner: true

MainActivity: 🎯 배너 광고 정책: 활성화
```

❌ **오류 로그가 보이면**:
```
AppPolicyRepo: ❌ No policy found!
→ Supabase 스키마가 적용되지 않았거나 레코드가 없음
```

---

### 4️⃣ 광고 동작 테스트

#### 테스트 1: 배너 광고
1. 앱 실행
2. 상단에 배너 광고 표시 확인 ✅
3. Supabase에서 OFF 설정:
   ```sql
   UPDATE app_policy 
   SET ad_banner_enabled = false 
   WHERE app_id = 'com.sweetapps.pocketchord';
   ```
4. **앱을 끄지 말고 5분 대기** (또는 캐시 만료 후)
5. Logcat에서 변경 로그 확인:
   ```
   MainActivity: 🔄 배너 광고 정책 변경: 활성화 → 비활성화
   ```
6. 배너 광고 자동으로 숨김 확인 ✅
7. Supabase에서 다시 ON 설정
8. 5분 대기 후 배너 광고 자동으로 표시 확인 ✅

**중요**: 앱을 재시작하지 않아도 5분 이내에 자동으로 반영됩니다!

#### 테스트 2: 전면 광고
1. 홈 → 코드 → 홈 (3회 반복, 60초 간격)
2. 전면 광고 표시 확인 ✅
3. Logcat에서 빈도 카운트 확인:
   ```
   InterstitialAdManager: 📊 광고 카운트 증가: 시간당 1, 일일 1
   ```
4. 시간당 2회 제한 테스트 (보수적 기본값)
5. Supabase에서 OFF 설정:
   ```sql
   UPDATE app_policy 
   SET ad_interstitial_enabled = false 
   WHERE app_id = 'com.sweetapps.pocketchord';
   ```
6. 앱 재시작 (또는 5분 대기)
7. 전면 광고 표시 안 됨 확인 ✅

#### 테스트 3: 앱 오픈 광고
1. 앱 시작 (첫 실행)
2. 광고 표시 안 됨 확인 ✅ (콜드 스타트)
3. 백그라운드로 이동 (홈 버튼)
4. 다시 앱으로 복귀
5. 앱 오픈 광고 표시 확인 ✅ (웜 스타트)
6. Supabase에서 OFF 설정:
   ```sql
   UPDATE app_policy 
   SET ad_app_open_enabled = false 
   WHERE app_id = 'com.sweetapps.pocketchord';
   ```
7. 백그라운드 → 복귀
8. 광고 표시 안 됨 확인 ✅

---

### 5️⃣ 캐싱 동작 확인

1. 앱 실행 → Logcat 확인:
   ```
   AppPolicyRepo: 🔄 Supabase에서 정책 새로 가져오기
   ```

2. 5분 이내 앱 재시작 → Logcat 확인:
   ```
   AppPolicyRepo: 📦 캐시된 정책 사용 (유효 시간: XXX초 남음)
   ```

3. 5분 경과 후 앱 재시작 → Logcat 확인:
   ```
   AppPolicyRepo: 🔄 Supabase에서 정책 새로 가져오기
   ```

---

## 🐛 문제 해결

### 문제 1: "No policy found" 로그

**원인**: Supabase에 레코드가 없거나 `is_active = false`

**해결**:
```sql
-- 레코드 확인
SELECT * FROM app_policy WHERE app_id = 'com.sweetapps.pocketchord';

-- 없으면 생성 (예시)
INSERT INTO app_policy (
  app_id, 
  is_active, 
  active_popup_type,
  ad_app_open_enabled,
  ad_interstitial_enabled,
  ad_banner_enabled
) VALUES (
  'com.sweetapps.pocketchord',
  true,
  'none',
  true,
  true,
  true
);
```

---

### 문제 2: 빌드 오류

**증상**: Kotlin 컴파일 에러

**해결**:
```cmd
# Clean 후 재빌드
gradlew clean
gradlew assembleDebug
```

**확인할 파일**:
- `AppPolicy.kt` - 광고 필드 추가되었는지
- `AppPolicyRepository.kt` - 캐싱 로직 있는지
- `MainActivity.kt` - Supabase 정책 조회하는지
- `InterstitialAdManager.kt` - 빈도 제한 있는지
- `AppOpenAdManager.kt` - 테스트 모드 제거되었는지

---

### 문제 3: 광고가 표시되지 않음

**원인 1**: Supabase에서 비활성화됨
```sql
-- 확인
SELECT ad_app_open_enabled, ad_interstitial_enabled, ad_banner_enabled
FROM app_policy;

-- 모두 활성화
UPDATE app_policy SET
  ad_app_open_enabled = true,
  ad_interstitial_enabled = true,
  ad_banner_enabled = true
WHERE app_id = 'com.sweetapps.pocketchord';
```

**원인 2**: 캐시가 오래된 정책 사용 중
- 앱 재시작 또는 5분 대기

**원인 3**: 빈도 제한 초과 (전면 광고만)
```
Logcat: ⚠️ 시간당 빈도 제한 초과
```
- 1시간 대기 또는 앱 재설치

---

### 문제 4: 정책 변경이 반영 안 됨

**원인**: 캐시 때문에 최대 5분 지연

**해결**:
1. 5분 대기
2. 또는 앱 재시작 (캐시 리셋)

---

## 📊 운영 시나리오

### 시나리오 1: 긴급 광고 중단

**상황**: 광고에 문제 발생, 즉시 중단 필요

**조치**:
```sql
UPDATE app_policy SET
  ad_app_open_enabled = false,
  ad_interstitial_enabled = false,
  ad_banner_enabled = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

**반영 시간**: 최대 5분

---

### 시나리오 2: 명절 이벤트

**상황**: 추석/크리스마스에 광고 없는 좋은 경험 제공

**조치**:
```sql
-- 이벤트 시작
UPDATE app_policy SET
  ad_interstitial_enabled = false,
  ad_banner_enabled = false
WHERE app_id = 'com.sweetapps.pocketchord';

-- 이벤트 종료
UPDATE app_policy SET
  ad_interstitial_enabled = true,
  ad_banner_enabled = true
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

### 시나리오 3: 사용자 불만 대응

**상황**: 리뷰에 "광고 너무 많아요" 증가

**조치**:
```sql
-- 빈도 줄이기
UPDATE app_policy SET
  ad_interstitial_max_per_hour = 2,
  ad_interstitial_max_per_day = 15
WHERE app_id = 'com.sweetapps.pocketchord';
```

**모니터링**: 평점 변화 관찰 후 조정

---

## ✅ 최종 체크리스트

구현 완료 확인:

- [ ] Supabase SQL 스크립트 실행 완료
- [ ] 테이블에 새 컬럼 5개 추가 확인
- [ ] 앱 빌드 성공
- [ ] 배너 광고 표시 확인
- [ ] 전면 광고 표시 확인 (60초 + 3회 전환 후)
- [ ] 앱 오픈 광고 표시 확인 (백그라운드 복귀 시)
- [ ] Logcat에서 정책 조회 로그 확인
- [ ] Supabase에서 광고 OFF → 반영 확인
- [ ] 캐싱 동작 확인 (5분 이내 캐시 사용)
- [ ] 빈도 제한 동작 확인 (시간당 2회, 일일 15회 - 보수적 기본값)

---

## 🎉 완료!

모든 체크리스트를 통과하면 구현이 완료된 것입니다!

**문의사항이 있으면 다음 문서 참고**:
- 구현 계획: `docs/admob-supabase-control-plan.md`
- 구현 완료: `docs/admob-supabase-control-IMPLEMENTATION-COMPLETE.md`
- SQL 스크립트: `docs/supabase-ad-control-schema.sql`

