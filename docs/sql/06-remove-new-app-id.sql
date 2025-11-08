-- ============================================
-- emergency_policy에서 new_app_id 컬럼 제거
-- 작성일: 2025-11-09
-- 이유: redirect_url만 있으면 충분함 (new_app_id는 불필요한 fallback)
-- ============================================

-- 1. 현재 컬럼 목록 확인 (new_app_id 존재 여부 체크)
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'emergency_policy'
  AND table_schema = 'public'
ORDER BY ordinal_position;

-- 2. new_app_id 컬럼 삭제 (없으면 무시)
ALTER TABLE public.emergency_policy
DROP COLUMN IF EXISTS new_app_id;

-- 3. 삭제 후 확인 (new_app_id가 목록에 없어야 함)
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'emergency_policy'
  AND table_schema = 'public'
ORDER BY ordinal_position;

-- 4. 코멘트 업데이트
COMMENT ON TABLE public.emergency_policy IS '긴급 상황 팝업 정책 (앱 차단, 서비스 종료 등) - redirect_url로 이동';

-- ============================================
-- 완료!
-- 이제 redirect_url만 사용하면 됩니다.
--
-- 참고: 이미 new_app_id가 삭제되었다면
--       "DROP COLUMN IF EXISTS"로 인해 오류 없이 건너뜁니다.
-- ============================================



