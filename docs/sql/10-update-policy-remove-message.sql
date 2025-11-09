-- ============================================
-- update_policy message 제거 및 download_url 필수화
-- 작성일: 2025-11-09 07:00 KST
-- 목적: message 필드 제거, download_url NOT NULL 및 기본값 설정
-- ============================================

-- 1. 현재 상태 확인
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'update_policy'
  AND table_schema = 'public'
  AND column_name IN ('message', 'download_url')
ORDER BY ordinal_position;

-- 2. 모든 download_url 데이터를 기본값으로 업데이트 (NULL이거나 빈 문자열인 경우)
UPDATE public.update_policy
SET download_url = 'https://play.google.com/'
WHERE download_url IS NULL
   OR download_url = ''
   OR download_url LIKE '%/store/apps/details%';  -- 기존 상세 페이지 링크도 초기화

-- 3. download_url 기본값 설정
ALTER TABLE public.update_policy
ALTER COLUMN download_url DROP DEFAULT;  -- 기존 기본값 제거

ALTER TABLE public.update_policy
ALTER COLUMN download_url SET DEFAULT 'https://play.google.com/';

-- 4. download_url을 NOT NULL로 변경
ALTER TABLE public.update_policy
ALTER COLUMN download_url SET NOT NULL;

-- 5. message 컬럼 삭제
ALTER TABLE public.update_policy
DROP COLUMN IF EXISTS message;

-- 6. 코멘트 업데이트
COMMENT ON COLUMN public.update_policy.download_url IS 'Play Store 링크 (필수, 기본값: https://play.google.com/)';
COMMENT ON COLUMN public.update_policy.release_notes IS '릴리즈 노트 (업데이트 메시지 포함)';

-- 7. 결과 확인
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'update_policy'
  AND table_schema = 'public'
ORDER BY ordinal_position;

-- ============================================
-- 완료!
-- - message 필드 제거됨
-- - download_url NOT NULL (기본값: https://play.google.com/)
--
-- 사용 예시:
-- UPDATE update_policy
-- SET release_notes = '• 새로운 기능\n• 버그 수정',
--     download_url = 'https://play.google.com/store/apps/details?id=...'
-- WHERE app_id = 'com.sweetapps.pocketchord';
-- ============================================

