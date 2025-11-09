-- ============================================
-- 디버그용 테스트 데이터 생성 (전체)
-- app_id: com.sweetapps.pocketchord.debug
-- ============================================

-- 먼저 기존 디버그 데이터 삭제 (깨끗하게 시작)
DELETE FROM public.emergency_policy WHERE app_id = 'com.sweetapps.pocketchord.debug';
DELETE FROM public.update_policy WHERE app_id = 'com.sweetapps.pocketchord.debug';
DELETE FROM public.notice_policy WHERE app_id = 'com.sweetapps.pocketchord.debug';
DELETE FROM public.ad_policy WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 1. emergency_policy 디버그 데이터
INSERT INTO public.emergency_policy (
    app_id,
    is_active,
    content,
    redirect_url,
    button_text,
    is_dismissible
) VALUES (
    'com.sweetapps.pocketchord.debug',
    false,
    '⚠️ [DEBUG 테스트] 이 앱은 더 이상 지원되지 않습니다. 새 앱을 설치해주세요.',
    NULL,
    '확인',
    true
);

-- 2. update_policy 디버그 데이터
INSERT INTO public.update_policy (
    app_id,
    is_active,
    target_version_code,
    is_force_update,
    release_notes,
    download_url
) VALUES (
    'com.sweetapps.pocketchord.debug',
    true,
    1,
    false,
    '• [DEBUG] 최신 버전으로 업데이트하세요',
    'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord.debug'
);

-- 3. notice_policy 디버그 데이터
INSERT INTO public.notice_policy (
    app_id,
    is_active,
    title,
    content,
    notice_version,
    image_url,
    action_url
) VALUES (
    'com.sweetapps.pocketchord.debug',
    true,
    '[DEBUG] 환영합니다! 🎉',
    '[DEBUG] PocketChord를 이용해 주셔서 감사합니다!',
    1,
    NULL,
    NULL
);

-- 4. ad_policy 디버그 데이터
INSERT INTO public.ad_policy (
    app_id,
    is_active,
    ad_app_open_enabled,
    ad_interstitial_enabled,
    ad_banner_enabled,
    ad_interstitial_max_per_hour,
    ad_interstitial_max_per_day
) VALUES (
    'com.sweetapps.pocketchord.debug',
    true,
    true,
    true,
    true,
    2,
    15
);

-- 5. 확인
SELECT 'emergency_policy' as table_name, COUNT(*) as count
FROM public.emergency_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug'
UNION ALL
SELECT 'update_policy', COUNT(*)
FROM public.update_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug'
UNION ALL
SELECT 'notice_policy', COUNT(*)
FROM public.notice_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug'
UNION ALL
SELECT 'ad_policy', COUNT(*)
FROM public.ad_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- ============================================
-- 완료!
-- 이제 디버그 버전 테스트가 가능합니다.
--
-- 참고:
-- - 이 스크립트는 매번 실행 가능합니다 (멱등성)
-- - 기존 디버그 데이터를 삭제하고 새로 생성합니다
-- ============================================

