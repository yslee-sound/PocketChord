-- ============================================
-- AdMob 광고 제어를 위한 app_policy 테이블 스키마 수정
-- ============================================
-- 작성일: 2025-01-08
-- 수정일: 2025-01-08 (NOT NULL 제약 조건 추가)
-- 목적: Supabase에서 AdMob 광고 ON/OFF 제어 및 빈도 제한
-- ============================================

-- 1. 필수 컬럼 추가 (광고 ON/OFF 제어)
ALTER TABLE app_policy
ADD COLUMN IF NOT EXISTS ad_app_open_enabled BOOLEAN DEFAULT true,
ADD COLUMN IF NOT EXISTS ad_interstitial_enabled BOOLEAN DEFAULT true,
ADD COLUMN IF NOT EXISTS ad_banner_enabled BOOLEAN DEFAULT true;

-- 2. 선택 컬럼 추가 (Interstitial Ad 빈도 제어)
ALTER TABLE app_policy
ADD COLUMN IF NOT EXISTS ad_interstitial_max_per_hour INT DEFAULT 2,
ADD COLUMN IF NOT EXISTS ad_interstitial_max_per_day INT DEFAULT 15;

-- 3. 기존 레코드의 NULL 값을 기본값으로 업데이트 (NOT NULL 적용 전 필수!)
UPDATE app_policy
SET
  ad_app_open_enabled = COALESCE(ad_app_open_enabled, true),
  ad_interstitial_enabled = COALESCE(ad_interstitial_enabled, true),
  ad_banner_enabled = COALESCE(ad_banner_enabled, true),
  ad_interstitial_max_per_hour = COALESCE(ad_interstitial_max_per_hour, 2),
  ad_interstitial_max_per_day = COALESCE(ad_interstitial_max_per_day, 15)
WHERE ad_app_open_enabled IS NULL
   OR ad_interstitial_enabled IS NULL
   OR ad_banner_enabled IS NULL
   OR ad_interstitial_max_per_hour IS NULL
   OR ad_interstitial_max_per_day IS NULL;

-- 4. NOT NULL 제약 조건 추가 (NULL 값 방지)
ALTER TABLE app_policy
ALTER COLUMN ad_app_open_enabled SET NOT NULL,
ALTER COLUMN ad_interstitial_enabled SET NOT NULL,
ALTER COLUMN ad_banner_enabled SET NOT NULL,
ALTER COLUMN ad_interstitial_max_per_hour SET NOT NULL,
ALTER COLUMN ad_interstitial_max_per_day SET NOT NULL;

-- 5. 테이블 구조 확인 (선택적으로 실행)
-- SELECT column_name, data_type, column_default, is_nullable
-- FROM information_schema.columns
-- WHERE table_name = 'app_policy'
-- AND column_name LIKE 'ad_%'
-- ORDER BY ordinal_position;

-- 6. 데이터 확인
SELECT
  app_id,
  is_active,
  ad_app_open_enabled,
  ad_interstitial_enabled,
  ad_banner_enabled,
  ad_interstitial_max_per_hour,
  ad_interstitial_max_per_day
FROM app_policy
WHERE app_id = 'com.sweetapps.pocketchord';

-- ============================================
-- 테스트 쿼리
-- ============================================

-- 모든 광고 비활성화 (테스트용)
-- UPDATE app_policy
-- SET
--   ad_app_open_enabled = false,
--   ad_interstitial_enabled = false,
--   ad_banner_enabled = false
-- WHERE app_id = 'com.sweetapps.pocketchord';

-- 모든 광고 활성화
-- UPDATE app_policy
-- SET
--   ad_app_open_enabled = true,
--   ad_interstitial_enabled = true,
--   ad_banner_enabled = true
-- WHERE app_id = 'com.sweetapps.pocketchord';

-- 특정 광고만 제어 (예: 배너 광고만 끄기)
-- UPDATE app_policy
-- SET ad_banner_enabled = false
-- WHERE app_id = 'com.sweetapps.pocketchord';

-- 빈도 제한 조정 (예: 더 보수적으로)
-- UPDATE app_policy
-- SET
--   ad_interstitial_max_per_hour = 2,
--   ad_interstitial_max_per_day = 15
-- WHERE app_id = 'com.sweetapps.pocketchord';

