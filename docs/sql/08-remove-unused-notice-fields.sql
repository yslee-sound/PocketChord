-- =====================================================
-- notice_policy 테이블에서 사용하지 않는 필드 제거
-- =====================================================
--
-- 목적: image_url, action_url 필드 제거
-- 이유: 현재 사용하지 않으며, 향후에도 사용 계획 없음
--
-- 작성일: 2025-11-09
-- 버전: 1.0.0
--
-- =====================================================

-- 1. image_url 컬럼 삭제
ALTER TABLE notice_policy
DROP COLUMN IF EXISTS image_url;

-- 2. action_url 컬럼 삭제
ALTER TABLE notice_policy
DROP COLUMN IF EXISTS action_url;

-- =====================================================
-- 실행 후 확인
-- =====================================================

-- 테이블 구조 확인
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'notice_policy'
ORDER BY ordinal_position;

-- 예상 결과:
-- id               | integer               | NO
-- created_at       | timestamp             | YES
-- app_id           | text                  | NO
-- is_active        | boolean               | YES
-- title            | text                  | YES
-- content          | text                  | YES
-- notice_version   | integer               | YES
-- ❌ image_url     | (삭제됨)
-- ❌ action_url    | (삭제됨)

-- =====================================================
-- 롤백 (필요 시)
-- =====================================================

-- 만약 실수로 삭제했다면 다시 추가:
-- ALTER TABLE notice_policy
-- ADD COLUMN image_url TEXT;
--
-- ALTER TABLE notice_policy
-- ADD COLUMN action_url TEXT;

-- =====================================================
-- 완료!
-- =====================================================

