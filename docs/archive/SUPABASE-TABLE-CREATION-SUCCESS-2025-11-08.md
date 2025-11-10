# ✅ Supabase 테이블 생성 완료!

**날짜**: 2025-11-08 13:11  
**상태**: ✅ 성공  
**업데이트**: 2025-11-08 - Debug 빌드 데이터 추가 필요

---

## 🎉 ad_policy 테이블 생성 확인

### 생성된 데이터 (실제 Supabase 결과)

#### Release 빌드 (com.sweetapps.pocketchord)
| 컬럼 | 값 | 비고 |
|------|-----|------|
| app_id | com.sweetapps.pocketchord | ✅ Release 앱 ID |
| is_active | **true** | ✅ 광고 정책 활성화 |
| ad_app_open_enabled | **true** | ✅ 앱 오픈 광고 ON |
| ad_interstitial_enabled | **true** | ✅ 전면 광고 ON |
| ad_banner_enabled | **true** | ✅ 배너 광고 ON |
| ad_interstitial_max_per_hour | **2** | ✅ 시간당 최대 2회 (보수적) |
| ad_interstitial_max_per_day | **15** | ✅ 하루 최대 15회 (보수적) |
| created_at | 2025-11-08 13:11:47 | ✅ 생성 시간 |

#### ⚠️ Debug 빌드 데이터 추가 필요!
Debug 빌드(`com.sweetapps.pocketchord.debug`)에서도 테스트하려면 추가 SQL 실행 필요

**실행할 파일**: `docs/ad-policy-add-debug-build.sql`

**또는 직접 실행**:
```sql
INSERT INTO ad_policy (
  app_id, is_active,
  ad_app_open_enabled, ad_interstitial_enabled, ad_banner_enabled,
  ad_interstitial_max_per_hour, ad_interstitial_max_per_day
) VALUES (
  'com.sweetapps.pocketchord.debug',
  true, true, true, true, 2, 15
)
ON CONFLICT (app_id) DO UPDATE SET
  is_active = EXCLUDED.is_active,
  ad_app_open_enabled = EXCLUDED.ad_app_open_enabled,
  ad_interstitial_enabled = EXCLUDED.ad_interstitial_enabled,
  ad_banner_enabled = EXCLUDED.ad_banner_enabled,
  ad_interstitial_max_per_hour = EXCLUDED.ad_interstitial_max_per_hour,
  ad_interstitial_max_per_day = EXCLUDED.ad_interstitial_max_per_day;
```

---

## ✅ 확인 사항

### 1. 테이블 구조
- ✅ id (BIGSERIAL)
- ✅ created_at (TIMESTAMP)
- ✅ app_id (TEXT UNIQUE)
- ✅ is_active (BOOLEAN)
- ✅ 모든 광고 제어 컬럼
- ✅ 모든 빈도 제한 컬럼

### 2. RLS 정책
- ✅ RLS 활성화됨
- ✅ `ad_policy_select` 정책 생성됨
- ✅ `is_active = true`인 레코드만 조회 가능

### 3. 인덱스
- ✅ `idx_ad_policy_app_id` (app_id)
- ✅ `idx_ad_policy_is_active` (is_active)

### 4. 초기 데이터
- ✅ PocketChord 앱 데이터 삽입 완료
- ✅ 모든 광고 활성화 상태
- ✅ 기본 빈도 제한 설정 (3/시간, 20/일)

---

## 🚀 다음 단계

### ✅ 1. Supabase 테이블 생성 (완료!)

### 2. 앱 빌드 (진행 중...)
```bash
cd G:\Workspace\PocketChord
gradlew.bat assembleDebug
```

### 3. APK 설치
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 4. 앱 테스트
- [ ] 앱 시작 → 앱 오픈 광고 확인
- [ ] 홈 화면 → 배너 광고 확인
- [ ] 화면 전환 → 전면 광고 확인 (조건 만족 시)

### 5. Supabase 제어 테스트
```sql
-- 배너 광고 끄기 테스트
UPDATE ad_policy 
SET ad_banner_enabled = false 
WHERE app_id = 'com.sweetapps.pocketchord';

-- 5분 이내 배너 사라지는지 확인

-- 다시 켜기
UPDATE ad_policy 
SET ad_banner_enabled = true 
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

## 🎯 독립성 테스트 (핵심!)

### 시나리오: 팝업 OFF + 광고 ON
```sql
-- 팝업 끄기
UPDATE app_policy 
SET is_active = false 
WHERE app_id = 'com.sweetapps.pocketchord';

-- 광고는 켜기 (이미 켜져있음)
UPDATE ad_policy 
SET is_active = true 
WHERE app_id = 'com.sweetapps.pocketchord';
```

**예상 결과:**
- ✅ 팝업 안 나옴
- ✅ 광고는 정상적으로 나옴 ← **이게 핵심!**

---

## 📊 현재 상태

```
✅ Supabase: ad_policy 테이블 생성 완료
✅ 초기 데이터: 설정 완료 (모든 광고 활성화)
🔄 앱 빌드: 진행 중...
⏳ 테스트: 대기 중...
```

---

## 💡 운영 팁

### 자주 사용할 쿼리

#### 현재 광고 정책 확인 (Release)
```sql
SELECT 
  is_active,
  ad_app_open_enabled,
  ad_interstitial_enabled,
  ad_banner_enabled,
  ad_interstitial_max_per_hour,
  ad_interstitial_max_per_day
FROM ad_policy 
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 현재 광고 정책 확인 (Debug)
```sql
SELECT 
  is_active,
  ad_app_open_enabled,
  ad_interstitial_enabled,
  ad_banner_enabled,
  ad_interstitial_max_per_hour,
  ad_interstitial_max_per_day
FROM ad_policy 
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

#### 모든 빌드 타입 확인
```sql
SELECT 
  app_id,
  is_active,
  ad_app_open_enabled,
  ad_interstitial_enabled,
  ad_banner_enabled
FROM ad_policy 
ORDER BY app_id;
```

#### 모든 광고 끄기 (Release만)
```sql
UPDATE ad_policy 
SET is_active = false 
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Debug 빌드에서만 테스트 (Release는 유지)
```sql
-- Debug에서만 배너 끄기
UPDATE ad_policy 
SET ad_banner_enabled = false 
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- Release는 그대로 유지됨
```

#### 모든 광고 켜기 (모든 빌드)
```sql
UPDATE ad_policy 
SET is_active = true;
```

#### 특정 광고만 끄기 (Release)
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

---

**작성일**: 2025-11-08  
**상태**: ✅ Supabase 완료, 앱 빌드 진행 중

