-- ============================================
-- update_policy 진단 쿼리
-- 작성일: 2025-11-09 07:30 KST
-- 목적: 업데이트 팝업이 표시되지 않는 원인 진단
-- ============================================

-- 1. update_policy 현재 상태 확인
SELECT
    app_id,
    is_active,
    target_version_code,
    is_force_update,
    LEFT(release_notes, 30) as release_notes_preview,
    download_url
FROM public.update_policy
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug')
ORDER BY app_id;

-- ============================================
-- 체크리스트 (위 결과를 보고 확인):
--
-- ✅ is_active = true 인가?
--    → false면 팝업이 표시되지 않음!
--    → true로 변경 필요
--
-- ✅ target_version_code가 4 이상인가?
--    → 현재 앱 버전(3)보다 높아야 팝업 표시
--    → 3 이하면 팝업이 표시되지 않음
--
-- ✅ download_url이 'https://play.google.com/' 인가?
--    → NULL이면 안 됨
--
-- 예상 결과 (정상):
-- app_id                           | is_active | target_version_code | is_force_update
-- com.sweetapps.pocketchord        | true      | 4                   | true (또는 false)
-- com.sweetapps.pocketchord.debug  | true      | 4                   | true (또는 false)
--
-- ============================================

-- 2. 문제가 있다면 수정하기

-- 2-1. is_active가 false인 경우
UPDATE public.update_policy
SET is_active = true
WHERE app_id = 'com.sweetapps.pocketchord.debug'
  AND is_active = false;

-- 2-2. target_version_code가 낮은 경우
UPDATE public.update_policy
SET target_version_code = 4
WHERE app_id = 'com.sweetapps.pocketchord.debug'
  AND target_version_code <= 3;

-- 2-3. 전체를 확실하게 설정 (강제 업데이트)
UPDATE public.update_policy
SET is_active = true,
    target_version_code = 4,
    is_force_update = true,
    release_notes = '• [DEBUG] 테스트 업데이트',
    download_url = 'https://play.google.com/'
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 3. 결과 재확인
SELECT
    app_id,
    is_active,
    target_version_code,
    is_force_update,
    LEFT(release_notes, 30) as release_notes_preview,
    download_url
FROM public.update_policy
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug')
ORDER BY app_id;

-- ============================================
-- 완료!
--
-- 만약 위 쿼리를 실행했는데도 팝업이 표시되지 않는다면:
--
-- 1. 앱을 완전히 종료하고 재시작했는지 확인
-- 2. Logcat에서 로그 확인:
--    Filter: tag:HomeScreen
--
--    예상 로그:
--    "Phase 2: Trying update_policy"
--    "✅ update_policy found: targetVersion=4, isForce=true"
--    "Decision: FORCE UPDATE from update_policy (target=4)"
--
-- 3. BuildConfig.VERSION_CODE 확인:
--    현재 앱의 versionCode가 3인지 확인
--    (build.gradle.kts에서 확인)
--
-- ============================================

