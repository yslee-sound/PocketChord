-- ============================================
-- app_policy 및 app_policy_history 완전 삭제
-- 작성일: 2025-11-09
-- 목적: 기존 테이블 정리 및 신규 테이블로 완전 전환
-- ============================================

-- 1. app_policy_history 삭제
DROP TABLE IF EXISTS public.app_policy_history CASCADE;

-- 2. app_policy 삭제
DROP TABLE IF EXISTS public.app_policy CASCADE;

-- 완료!
SELECT 'app_policy 및 app_policy_history 삭제 완료!' as status;
SELECT '신규 테이블(update_policy, emergency_policy, notice_policy)만 사용합니다.' as info;

