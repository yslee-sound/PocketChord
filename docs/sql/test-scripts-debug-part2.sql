-- ============================================
-- 릴리즈 테스트 SQL 스크립트 - 2부 (디버그용)
-- app_id: com.sweetapps.pocketchord.debug
--
-- 포함 내용:
-- - Phase 2: Update 테스트 (후반부)
-- - Phase 3: Notice 테스트
-- - Phase 4: 우선순위 테스트
-- - 최종 상태 확인
-- - 전체 초기화
-- ============================================

-- ===== Phase 2: Update 테스트 (2/2) =====

-- 2-3. 버전 더 높게 (추가 테스트)
UPDATE update_policy
SET target_version_code = 1000
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 2-4. Update 정리 (원래대로)
UPDATE update_policy
SET target_version_code = 1
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- ===== Phase 3: Notice 테스트 =====

-- 3-1. 현재 버전 확인
SELECT notice_version, title, is_active
FROM notice_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 3-2. 오타 수정 (버전 유지)
UPDATE notice_policy
SET content = '[DEBUG] PocketChord를 이용해 주셔서 정말 감사합니다!'
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 3-3. 새 공지 (버전 증가)
UPDATE notice_policy
SET title = '🎉 [DEBUG] 11월 이벤트',
    content = '[DEBUG] 11월 특별 이벤트가 시작되었습니다!\n많은 참여 부탁드립니다.',
    notice_version = 2
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 3-4. Notice 정리 (원래대로)
UPDATE notice_policy
SET title = '[DEBUG] 환영합니다! 🎉',
    content = '[DEBUG] PocketChord를 이용해 주셔서 감사합니다!',
    notice_version = 1
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- ===== Phase 4: 우선순위 테스트 =====

-- 4-1. Emergency + Update 동시 활성화
UPDATE emergency_policy
SET is_active = true,
    content = '🚨 [DEBUG 우선순위 테스트] 긴급'
WHERE app_id = 'com.sweetapps.pocketchord.debug';

UPDATE update_policy
SET target_version_code = 999,
    is_force_update = true
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 4-2. 정리
UPDATE emergency_policy
SET is_active = false
WHERE app_id = 'com.sweetapps.pocketchord.debug';

UPDATE update_policy
SET target_version_code = 1
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- ===== 최종 상태 확인 =====
SELECT
    'emergency_policy' as policy,
    CAST(is_active AS TEXT) as is_active,
    CAST(is_dismissible AS TEXT) as detail,
    LEFT(content, 30) as preview
FROM emergency_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug'
UNION ALL
SELECT
    'update_policy',
    CAST(is_active AS TEXT),
    CAST(is_force_update AS TEXT),
    CONCAT('target:', target_version_code)
FROM update_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug'
UNION ALL
SELECT
    'notice_policy',
    CAST(is_active AS TEXT),
    CAST(NULL AS TEXT),
    CONCAT('v', notice_version, ': ', LEFT(title, 20))
FROM notice_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug'
UNION ALL
SELECT
    'ad_policy',
    CAST(is_active AS TEXT),
    CAST(NULL AS TEXT),
    CONCAT('open:', ad_app_open_enabled, ' inter:', ad_interstitial_enabled, ' banner:', ad_banner_enabled)
FROM ad_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- ===== 전체 초기화 (평상시 상태) =====
UPDATE emergency_policy
SET is_active = false
WHERE app_id = 'com.sweetapps.pocketchord.debug';

UPDATE update_policy
SET target_version_code = 1,
    is_force_update = false
WHERE app_id = 'com.sweetapps.pocketchord.debug';

UPDATE notice_policy
SET is_active = true,
    title = '[DEBUG] 환영합니다! 🎉',
    content = '[DEBUG] PocketChord를 이용해 주셔서 감사합니다!',
    notice_version = 1
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- ============================================
-- ✅ 2부 완료
-- ============================================

