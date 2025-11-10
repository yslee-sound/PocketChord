-- ============================================
-- ad_policy RLS 정책 수정 (근본 해결)
-- ============================================
-- 작성일: 2025-11-11
-- 목적: is_active를 정상적으로 사용 가능하도록 RLS 정책 수정
-- 문제: 기존 RLS가 is_active = false인 행을 숨겨서 비즈니스 로직과 충돌
-- 해결: RLS를 보안 용도로만 사용하고, is_active는 애플리케이션에서 처리
-- ============================================

-- 1. 기존 RLS 정책들 제거 (모든 가능한 정책 이름)
DROP POLICY IF EXISTS "ad_policy_select" ON ad_policy;
DROP POLICY IF EXISTS "ad_policy_select_all" ON ad_policy;

-- 2. 새로운 RLS 정책: 모든 행 조회 가능 (보안용이 아닌 public 테이블)
CREATE POLICY "ad_policy_select_all" ON ad_policy
  FOR SELECT USING (true);

-- 설명:
-- - 이제 is_active = false인 행도 조회 가능
-- - 애플리케이션 코드에서 is_active를 체크하여 사용 여부 결정
-- - RLS는 본래 목적(보안)이 아닌 경우 제거하는 것이 맞음

-- 3. 확인
SELECT app_id, is_active, ad_banner_enabled
FROM ad_policy;
-- 이제 is_active = false인 행도 보임

-- ============================================
-- 적용 후 동작:
-- ============================================
-- 1. Supabase에서 is_active = false 설정
-- 2. 앱에서 정책 조회 성공 (행이 반환됨)
-- 3. 앱 코드에서 policy.isActive 체크
-- 4. false이면 광고 비활성화
-- ============================================

