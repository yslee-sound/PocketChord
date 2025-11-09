-- ============================================
-- emergency_policy의 redirect_url 초기화
-- 작성일: 2025-11-09 06:40 KST
-- 목적: 테스트 데이터에 하드코딩된 redirect_url 제거
-- ============================================

-- 1. 현재 상태 확인
SELECT app_id, redirect_url, button_text
FROM public.emergency_policy
ORDER BY app_id;

-- 2. redirect_url을 NULL로 초기화
UPDATE public.emergency_policy
SET redirect_url = NULL
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');

-- 3. 결과 확인
SELECT app_id, redirect_url, button_text
FROM public.emergency_policy
ORDER BY app_id;

-- ============================================
-- 완료!
-- 이제 redirect_url이 NULL로 설정되었습니다.
--
-- 사용 시: 필요할 때 수동으로 입력하세요!
-- UPDATE emergency_policy
-- SET redirect_url = 'https://play.google.com/store/apps/details?id=...'
-- WHERE app_id = 'com.sweetapps.pocketchord';
-- ============================================

