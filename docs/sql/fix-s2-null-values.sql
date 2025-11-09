-- Phase 2.5 S2 문제 해결: NULL 값 수정 (릴리즈 + 디버그)
-- 작성일: 2025-11-09 21:40:00 KST
-- 문제: targetVersion=null, isForce=null로 조회됨

-- ============================================================
-- 1. 현재 상태 확인 (릴리즈 + 디버그 모두)
-- ============================================================
SELECT app_id, target_version_code, is_force_update, is_active,
       reshow_interval_hours, max_later_count
FROM update_policy
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug')
ORDER BY app_id;

-- ============================================================
-- 2. 릴리즈 버전 수정
-- ============================================================
UPDATE update_policy
SET target_version_code = 10,
    is_force_update = false,
    is_active = true,
    reshow_interval_hours = 24,  -- 릴리즈: 24시간
    max_later_count = 3,
    release_notes = '• 성능 개선\n• 버그 수정'
WHERE app_id = 'com.sweetapps.pocketchord';

-- ============================================================
-- 3. 디버그 버전 수정
-- ============================================================
UPDATE update_policy
SET target_version_code = 10,
    is_force_update = false,
    is_active = true,
    reshow_interval_hours = 1,  -- 디버그: 1시간 (테스트용)
    max_later_count = 3,
    release_notes = '• [DEBUG] Phase 2.5 테스트 업데이트'
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- ============================================================
-- 4. 재확인 (릴리즈 + 디버그 모두)
-- ============================================================
SELECT app_id, target_version_code, is_force_update, is_active,
       reshow_interval_hours, max_later_count
FROM update_policy
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug')
ORDER BY app_id;

-- ============================================================
-- 기대 결과:
-- ============================================================
-- 릴리즈 (com.sweetapps.pocketchord):
--   target_version_code = 10 (NOT NULL)
--   is_force_update = false (NOT NULL)
--   is_active = true
--   reshow_interval_hours = 24
--   max_later_count = 3
--
-- 디버그 (com.sweetapps.pocketchord.debug):
--   target_version_code = 10 (NOT NULL)
--   is_force_update = false (NOT NULL)
--   is_active = true
--   reshow_interval_hours = 1 (테스트용 짧은 간격)
--   max_later_count = 3

