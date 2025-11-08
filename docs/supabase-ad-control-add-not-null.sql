-- ============================================
-- AdMob 광고 컬럼 NOT NULL 제약 조건 추가
-- ============================================
-- 작성일: 2025-01-08
-- 목적: 기존 테이블에 NOT NULL 제약 조건 추가
-- 주의: 이미 스키마를 적용한 경우에만 실행
-- ============================================

-- 실행 전 확인 사항:
-- 1. app_policy 테이블에 ad_* 컬럼이 이미 존재하는가?
-- 2. NULL 값이 있는 레코드가 있는가?

-- 0. 현재 상태 확인
SELECT
  app_id,
  ad_app_open_enabled,
  ad_interstitial_enabled,
  ad_banner_enabled,
  ad_interstitial_max_per_hour,
  ad_interstitial_max_per_day
FROM app_policy
WHERE ad_app_open_enabled IS NULL
   OR ad_interstitial_enabled IS NULL
   OR ad_banner_enabled IS NULL
   OR ad_interstitial_max_per_hour IS NULL
   OR ad_interstitial_max_per_day IS NULL;

-- 위 쿼리 결과가 있다면 아래 단계 1 실행 필요
-- 결과가 없다면 단계 1 스킵하고 단계 2로 이동

-- ============================================
-- 1단계: NULL 값을 기본값으로 업데이트 (필수!)
-- ============================================

-- 모든 NULL 값을 기본값으로 변경
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

-- 확인: 이제 NULL이 없어야 함
SELECT COUNT(*) as null_count
FROM app_policy
WHERE ad_app_open_enabled IS NULL
   OR ad_interstitial_enabled IS NULL
   OR ad_banner_enabled IS NULL;
-- 결과가 0이어야 다음 단계 진행 가능!

-- ============================================
-- 2단계: NOT NULL 제약 조건 추가
-- ============================================

-- 광고 ON/OFF 컬럼에 NOT NULL 추가
ALTER TABLE app_policy
ALTER COLUMN ad_app_open_enabled SET NOT NULL;

ALTER TABLE app_policy
ALTER COLUMN ad_interstitial_enabled SET NOT NULL;

ALTER TABLE app_policy
ALTER COLUMN ad_banner_enabled SET NOT NULL;

-- 빈도 제한 컬럼에 NOT NULL 추가
ALTER TABLE app_policy
ALTER COLUMN ad_interstitial_max_per_hour SET NOT NULL;

ALTER TABLE app_policy
ALTER COLUMN ad_interstitial_max_per_day SET NOT NULL;

-- ============================================
-- 3단계: 결과 확인
-- ============================================

-- 테이블 구조 확인 (is_nullable이 'NO'여야 함)
SELECT
  column_name,
  data_type,
  column_default,
  is_nullable  -- 'NO'여야 함
FROM information_schema.columns
WHERE table_name = 'app_policy'
AND column_name LIKE 'ad_%'
ORDER BY ordinal_position;

-- 데이터 확인
SELECT
  app_id,
  is_active,
  ad_app_open_enabled,
  ad_interstitial_enabled,
  ad_banner_enabled,
  ad_interstitial_max_per_hour,
  ad_interstitial_max_per_day
FROM app_policy;

-- ============================================
-- 테스트: NULL 값 삽입 시도 (실패해야 정상)
-- ============================================

-- 이제 NULL을 넣으려고 하면 에러가 발생해야 함
-- UPDATE app_policy
-- SET ad_banner_enabled = NULL
-- WHERE app_id = 'com.sweetapps.pocketchord';
-- → 예상 에러: null value in column "ad_banner_enabled" violates not-null constraint

-- ============================================
-- 완료!
-- ============================================

-- NOT NULL 제약 조건이 추가되었습니다.
-- 이제 광고 컬럼은 반드시 true 또는 false 값만 가질 수 있습니다.

