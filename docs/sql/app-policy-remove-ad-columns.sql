-- ============================================
-- app_policy 테이블에서 광고 관련 컬럼 제거
-- ============================================
-- 작성일: 2025-11-08
-- 목적: ad_policy 테이블로 분리 완료 후 app_policy 정리
-- 참조: ad-policy-separation-implementation-complete.md
-- ============================================

-- ⚠️ 주의사항
-- 1. ad_policy 테이블이 먼저 생성되어 있어야 함
-- 2. 앱이 AdPolicyRepository를 사용하도록 업데이트되어 있어야 함
-- 3. 이 작업은 되돌릴 수 없으므로 신중히 진행
-- 4. 운영 환경에서는 사용자가 적은 시간대에 실행 권장

-- ============================================
-- Step 1: 백업 (선택사항이지만 강력 권장)
-- ============================================

-- 현재 app_policy 데이터 백업
CREATE TABLE app_policy_backup_20251108 AS
SELECT * FROM app_policy;

-- 백업 확인
SELECT COUNT(*) FROM app_policy_backup_20251108;

-- ============================================
-- Step 2: 광고 관련 컬럼 제거
-- ============================================

-- 광고 ON/OFF 컬럼 제거
ALTER TABLE app_policy
DROP COLUMN IF EXISTS ad_app_open_enabled,
DROP COLUMN IF EXISTS ad_interstitial_enabled,
DROP COLUMN IF EXISTS ad_banner_enabled;

-- 광고 빈도 제한 컬럼 제거
ALTER TABLE app_policy
DROP COLUMN IF EXISTS ad_interstitial_max_per_hour,
DROP COLUMN IF EXISTS ad_interstitial_max_per_day;

-- ============================================
-- Step 3: 확인
-- ============================================

-- 테이블 구조 확인 (광고 컬럼이 없어야 함)
SELECT column_name, data_type, column_default, is_nullable
FROM information_schema.columns
WHERE table_name = 'app_policy'
ORDER BY ordinal_position;

-- 예상 결과: 광고 관련 컬럼 (ad_로 시작하는 컬럼)이 없어야 함
-- 남아있어야 할 컬럼:
--   - id
--   - created_at
--   - app_id
--   - is_active
--   - active_popup_type
--   - content
--   - download_url
--   - min_supported_version
--   - latest_version_code

-- ============================================
-- Step 4: 데이터 확인
-- ============================================

-- app_policy는 팝업만 관리
SELECT
  app_id,
  is_active,
  active_popup_type,
  content
FROM app_policy
WHERE app_id = 'com.sweetapps.pocketchord';

-- ad_policy는 광고만 관리
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

-- ============================================
-- 롤백 (문제 발생 시)
-- ============================================

-- 주의: 롤백하려면 앱도 다시 이전 버전으로 돌려야 함!

-- 광고 컬럼 복원
-- ALTER TABLE app_policy
-- ADD COLUMN ad_app_open_enabled BOOLEAN DEFAULT true NOT NULL,
-- ADD COLUMN ad_interstitial_enabled BOOLEAN DEFAULT true NOT NULL,
-- ADD COLUMN ad_banner_enabled BOOLEAN DEFAULT true NOT NULL,
-- ADD COLUMN ad_interstitial_max_per_hour INT DEFAULT 2 NOT NULL,
-- ADD COLUMN ad_interstitial_max_per_day INT DEFAULT 15 NOT NULL;

-- 백업에서 광고 데이터 복원
-- UPDATE app_policy SET
--   ad_app_open_enabled = backup.ad_app_open_enabled,
--   ad_interstitial_enabled = backup.ad_interstitial_enabled,
--   ad_banner_enabled = backup.ad_banner_enabled,
--   ad_interstitial_max_per_hour = backup.ad_interstitial_max_per_hour,
--   ad_interstitial_max_per_day = backup.ad_interstitial_max_per_day
-- FROM app_policy_backup_20251108 backup
-- WHERE app_policy.app_id = backup.app_id;

-- ============================================
-- 정리 (완료 후 상당 시간이 지나면)
-- ============================================

-- 백업 테이블 삭제 (충분한 시간이 지나고 문제 없으면)
-- DROP TABLE IF EXISTS app_policy_backup_20251108;

-- ============================================
-- 최종 확인 체크리스트
-- ============================================

-- [ ] ad_policy 테이블이 생성되어 있음
-- [ ] 앱이 AdPolicyRepository를 사용하도록 업데이트됨
-- [ ] 새 버전 앱 배포 완료
-- [ ] 사용자들이 새 버전으로 업데이트함 (충분한 시간 경과)
-- [ ] 광고가 정상 작동함 (ad_policy 사용)
-- [ ] 백업 완료
-- [ ] 위 SQL 실행
-- [ ] 테이블 구조 확인 (광고 컬럼 없음)
-- [ ] 앱 정상 동작 확인

-- ============================================
-- 실행 타이밍 권장사항
-- ============================================

-- 1. 즉시 실행 가능한 경우:
--    - 아직 운영 서비스 전 (개발/테스트 단계)
--    - 사용자가 없거나 매우 적음

-- 2. 나중에 실행 권장:
--    - 운영 서비스 중
--    - 새 버전 앱 배포 후 1-2주 경과
--    - 대부분의 사용자가 업데이트 완료
--    - 광고가 ad_policy로 정상 작동 확인

-- 3. 실행하지 않아도 됨:
--    - 컬럼이 남아있어도 앱은 정상 작동
--    - 단지 테이블이 조금 지저분할 뿐
--    - 향후 데이터베이스 마이그레이션 때 정리 가능

