-- ============================================
-- download_url 기본값 강제 설정
-- 작성일: 2025-11-09 07:10 KST
-- 목적: download_url 기본값을 확실하게 변경
-- ============================================

-- 1. 현재 기본값 확인
SELECT column_name, column_default
FROM information_schema.columns
WHERE table_name = 'update_policy'
  AND column_name = 'download_url';

-- 2. 모든 기존 데이터를 기본값으로 업데이트
UPDATE public.update_policy
SET download_url = 'https://play.google.com/';

-- 3. 기존 기본값 완전히 제거
ALTER TABLE public.update_policy
ALTER COLUMN download_url DROP DEFAULT;

-- 4. 새 기본값 설정
ALTER TABLE public.update_policy
ALTER COLUMN download_url SET DEFAULT 'https://play.google.com/';

-- 5. 결과 확인
SELECT column_name, column_default
FROM information_schema.columns
WHERE table_name = 'update_policy'
  AND column_name = 'download_url';

-- 6. 실제 데이터 확인
SELECT app_id, download_url
FROM public.update_policy
ORDER BY app_id;

-- ============================================
-- 완료!
-- 모든 download_url이 https://play.google.com/로 설정됨
-- ============================================

