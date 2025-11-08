-- ============================================
-- ad_policy 테이블 생성 (방안 1: 테이블 분리)
-- ============================================
-- 작성일: 2025-01-08
-- 목적: 광고 정책을 app_policy에서 분리하여 독립적으로 제어
-- 참조: app-policy-ad-policy-separation-analysis.md
-- ============================================

-- 1. ad_policy 테이블 생성
CREATE TABLE IF NOT EXISTS ad_policy (
  id BIGSERIAL PRIMARY KEY,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  app_id TEXT UNIQUE NOT NULL,
  is_active BOOLEAN DEFAULT true NOT NULL,

  -- 광고 ON/OFF
  ad_app_open_enabled BOOLEAN DEFAULT true NOT NULL,
  ad_interstitial_enabled BOOLEAN DEFAULT true NOT NULL,
  ad_banner_enabled BOOLEAN DEFAULT true NOT NULL,

  -- 빈도 제한
  ad_interstitial_max_per_hour INT DEFAULT 2 NOT NULL,
  ad_interstitial_max_per_day INT DEFAULT 15 NOT NULL
);

-- 2. RLS (Row Level Security) 활성화
ALTER TABLE ad_policy ENABLE ROW LEVEL SECURITY;

-- 3. RLS 정책 생성: is_active = true인 레코드만 조회 가능
CREATE POLICY "ad_policy_select" ON ad_policy
  FOR SELECT USING (is_active = true);

-- 4. 인덱스 생성 (성능 최적화)
CREATE INDEX IF NOT EXISTS idx_ad_policy_app_id ON ad_policy(app_id);
CREATE INDEX IF NOT EXISTS idx_ad_policy_is_active ON ad_policy(is_active);

-- 5. 초기 데이터 삽입 (Release 및 Debug 빌드)
INSERT INTO ad_policy (
  app_id,
  is_active,
  ad_app_open_enabled,
  ad_interstitial_enabled,
  ad_banner_enabled,
  ad_interstitial_max_per_hour,
  ad_interstitial_max_per_day
) VALUES
-- Release 빌드 (실제 운영)
(
  'com.sweetapps.pocketchord',
  true,    -- 광고 정책 활성화
  true,    -- 앱 오픈 광고 ON
  true,    -- 전면 광고 ON
  true,    -- 배너 광고 ON
  2,       -- 시간당 최대 2회 (보수적)
  15       -- 하루 최대 15회 (보수적)
),
-- Debug 빌드 (개발/테스트)
(
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

-- 6. 테이블 구조 확인
-- SELECT column_name, data_type, column_default, is_nullable
-- FROM information_schema.columns
-- WHERE table_name = 'ad_policy'
-- ORDER BY ordinal_position;

-- 7. 데이터 확인
-- Release 빌드
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
WHERE app_id = 'com.sweetapps.pocketchord';

-- Debug 빌드
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
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 모든 데이터 확인
SELECT
  app_id,
  is_active,
  ad_app_open_enabled,
  ad_interstitial_enabled,
  ad_banner_enabled,
  ad_interstitial_max_per_hour,
  ad_interstitial_max_per_day
FROM ad_policy
ORDER BY app_id;

-- ============================================
-- 운영 시나리오 예제
-- ============================================

-- 시나리오 1: 팝업 OFF + 광고 ON
-- UPDATE app_policy
-- SET is_active = false
-- WHERE app_id = 'com.sweetapps.pocketchord';
--
-- UPDATE ad_policy
-- SET is_active = true
-- WHERE app_id = 'com.sweetapps.pocketchord';

-- 시나리오 2: 팝업 ON + 광고 OFF
-- UPDATE app_policy
-- SET is_active = true,
--     active_popup_type = 'notice',
--     content = '공지사항'
-- WHERE app_id = 'com.sweetapps.pocketchord';
--
-- UPDATE ad_policy
-- SET is_active = false
-- WHERE app_id = 'com.sweetapps.pocketchord';

-- 시나리오 3: 모두 OFF (명절 이벤트)
-- UPDATE app_policy SET is_active = false;
-- UPDATE ad_policy SET is_active = false;

-- 시나리오 4: 특정 광고만 제어
-- UPDATE ad_policy
-- SET ad_banner_enabled = false
-- WHERE app_id = 'com.sweetapps.pocketchord';

-- 시나리오 5: 빈도 제한 조정
-- UPDATE ad_policy
-- SET ad_interstitial_max_per_hour = 2,
--     ad_interstitial_max_per_day = 15
-- WHERE app_id = 'com.sweetapps.pocketchord';

-- ============================================
-- 롤백 (필요시)
-- ============================================
-- DROP POLICY IF EXISTS "ad_policy_select" ON ad_policy;
-- DROP TABLE IF EXISTS ad_policy CASCADE;

