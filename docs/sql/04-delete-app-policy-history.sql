-- ============================================
-- app_policy_history 테이블 삭제
-- 작성일: 2025-11-09
-- 목적: 사용하지 않는 히스토리 테이블 정리
-- ============================================

-- ✅ app_policy_history는 즉시 삭제 가능
-- 이유: 코드에서 사용하지 않음

DROP TABLE IF EXISTS public.app_policy_history CASCADE;

-- 완료!
SELECT 'app_policy_history 테이블 삭제 완료!' as status;

-- ⚠️ 참고: app_policy는 아직 삭제하면 안 됩니다!
-- 이유: HomeScreen.kt에서 fallback으로 사용 중
-- 가이드: docs/app-policy-cleanup-guide.md 참조

