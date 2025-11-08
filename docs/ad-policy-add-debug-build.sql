-- ============================================
-- ad_policy 테이블에 Debug 빌드 데이터 추가
-- ============================================
-- 작성일: 2025-11-08
-- 목적: Debug 빌드에서도 광고 정책 테스트 가능하도록 설정
-- ============================================

-- Debug 빌드용 데이터 삽입
INSERT INTO ad_policy (
  app_id,
  is_active,
  ad_app_open_enabled,
  ad_interstitial_enabled,
  ad_banner_enabled,
  ad_interstitial_max_per_hour,
  ad_interstitial_max_per_day
) VALUES (
  'com.sweetapps.pocketchord.debug',
  true,    -- 광고 정책 활성화
  true,    -- 앱 오픈 광고 ON
  true,    -- 전면 광고 ON
  true,    -- 배너 광고 ON
  2,       -- 시간당 최대 2회 (보수적)
  15       -- 하루 최대 15회 (보수적)
)
ON CONFLICT (app_id) DO UPDATE
SET
  is_active = EXCLUDED.is_active,
  ad_app_open_enabled = EXCLUDED.ad_app_open_enabled,
  ad_interstitial_enabled = EXCLUDED.ad_interstitial_enabled,
  ad_banner_enabled = EXCLUDED.ad_banner_enabled,
  ad_interstitial_max_per_hour = EXCLUDED.ad_interstitial_max_per_hour,
  ad_interstitial_max_per_day = EXCLUDED.ad_interstitial_max_per_day;

-- 확인: 모든 app_id 조회
SELECT
  app_id,
  is_active,
  ad_app_open_enabled,
  ad_interstitial_enabled,
  ad_banner_enabled,
  ad_interstitial_max_per_hour,
  ad_interstitial_max_per_day,
  created_at
FROM ad_policy
ORDER BY app_id;

-- ============================================
-- 예상 결과
-- ============================================
-- 2개의 행이 반환되어야 함:
-- 1. com.sweetapps.pocketchord (Release)
-- 2. com.sweetapps.pocketchord.debug (Debug)
-- ============================================

