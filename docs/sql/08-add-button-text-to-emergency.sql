-- ============================================
-- emergency_policy에 button_text 컬럼 추가
-- 작성일: 2025-11-09
-- 목적: 버튼 텍스트를 Supabase에서 설정 가능하도록
-- ============================================

-- 1. button_text 컬럼 추가 (임시로 NULL 허용)
ALTER TABLE public.emergency_policy
ADD COLUMN IF NOT EXISTS button_text TEXT;

-- 2. 기본값 설정 (기존 데이터용)
UPDATE public.emergency_policy
SET button_text = '확인'
WHERE button_text IS NULL;

-- 3. NOT NULL 제약 조건 추가 및 기본값 설정
ALTER TABLE public.emergency_policy
ALTER COLUMN button_text SET NOT NULL,
ALTER COLUMN button_text SET DEFAULT '확인';

-- 4. 코멘트 추가
COMMENT ON COLUMN public.emergency_policy.button_text IS '버튼 텍스트 (예: "새 앱 설치하기", "확인", "자세히 보기")';

-- 5. 확인
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'emergency_policy'
  AND table_schema = 'public'
  AND column_name = 'button_text';

-- ============================================
-- 완료!
-- 이제 Supabase에서 버튼 텍스트를 설정할 수 있습니다.
-- button_text는 NOT NULL이며 기본값은 "확인"입니다.
--
-- 사용 예시:
-- UPDATE emergency_policy
-- SET button_text = '확인'
-- WHERE app_id = 'com.sweetapps.pocketchord';
-- ============================================

