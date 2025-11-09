-- ============================================
-- 릴리즈 테스트 SQL 스크립트 - 1부 (릴리즈용)
-- app_id: com.sweetapps.pocketchord
--
-- 포함 내용:
-- - 초기 상태 확인
-- - Phase 1: Emergency 테스트
-- - Phase 2: Update 테스트 (전반부)
-- ============================================

-- ===== 초기 상태 확인 =====
SELECT 'emergency_policy' as table_name,
       CAST(is_active AS TEXT) as is_active,
       LEFT(content, 30) as content_preview
FROM emergency_policy
WHERE app_id = 'com.sweetapps.pocketchord'
UNION ALL
SELECT 'update_policy',
       CAST(is_active AS TEXT),
       CONCAT('target:', target_version_code, ' force:', is_force_update)
FROM update_policy
WHERE app_id = 'com.sweetapps.pocketchord'
UNION ALL
SELECT 'notice_policy',
       CAST(is_active AS TEXT),
       CONCAT('v', notice_version, ': ', LEFT(title, 20))
FROM notice_policy
WHERE app_id = 'com.sweetapps.pocketchord'
UNION ALL
SELECT 'ad_policy',
       CAST(is_active AS TEXT),
       CONCAT('open:', ad_app_open_enabled, ' inter:', ad_interstitial_enabled, ' banner:', ad_banner_enabled)
FROM ad_policy
WHERE app_id = 'com.sweetapps.pocketchord';

-- ===== Phase 1: Emergency 테스트 =====

-- 1-1. Emergency 활성화 (X 버튼 있음)
UPDATE emergency_policy
SET is_active = true,
    is_dismissible = true,
    content = '🚨 [테스트] 긴급 테스트입니다. X 버튼으로 닫을 수 있습니다.'
WHERE app_id = 'com.sweetapps.pocketchord';

-- 1-2. Emergency 수정 (X 버튼 없음)
UPDATE emergency_policy
SET is_dismissible = false,
    content = '🚨 [테스트] 이 앱은 더 이상 지원되지 않습니다. 새 앱을 설치해야 합니다.'
WHERE app_id = 'com.sweetapps.pocketchord';

-- 1-3. Emergency 비활성화 (정리)
UPDATE emergency_policy
SET is_active = false
WHERE app_id = 'com.sweetapps.pocketchord';

-- ===== Phase 2: Update 테스트 (1/2) =====

-- 2-1. 강제 업데이트 활성화
UPDATE update_policy
SET is_active = true,
    target_version_code = 4,
    is_force_update = true,
    release_notes = '• [테스트] 중요 보안 패치\n• [테스트] 필수 기능 추가'
WHERE app_id = 'com.sweetapps.pocketchord';

-- 2-2. 선택적 업데이트로 변경
UPDATE update_policy
SET is_force_update = false,
    release_notes = '• [테스트] 다크 모드 추가\n• [테스트] 성능 개선\n• [테스트] UI 업데이트'
WHERE app_id = 'com.sweetapps.pocketchord';

-- ============================================
-- 🔽 2부로 계속 (test-scripts-release-part2.sql)
-- ============================================


-- 2-3. 버전 더 높게 (추가 테스트)
UPDATE update_policy
SET target_version_code = 5
WHERE app_id = 'com.sweetapps.pocketchord';

-- 2-4. Update 정리 (원래대로)
UPDATE update_policy
SET target_version_code = 3
WHERE app_id = 'com.sweetapps.pocketchord';

-- ===== Phase 3: Notice 테스트 =====

-- 3-1. 현재 버전 확인
SELECT notice_version, title, is_active
FROM notice_policy
WHERE app_id = 'com.sweetapps.pocketchord';

-- 3-2. 오타 수정 (버전 유지)
UPDATE notice_policy
SET content = 'PocketChord를 이용해 주셔서 정말 감사합니다!'
WHERE app_id = 'com.sweetapps.pocketchord';

-- 3-3. 새 공지 (버전 증가)
UPDATE notice_policy
SET title = '🎉 11월 이벤트',
    content = '11월 특별 이벤트가 시작되었습니다!\n많은 참여 부탁드립니다.',
    notice_version = 2
WHERE app_id = 'com.sweetapps.pocketchord';

-- 3-4. Notice 정리 (원래대로)
UPDATE notice_policy
SET title = '환영합니다! 🎉',
    content = 'PocketChord를 이용해 주셔서 감사합니다!\n더 나은 서비스를 제공하기 위해 노력하겠습니다.',
    notice_version = 1
WHERE app_id = 'com.sweetapps.pocketchord';

-- ===== Phase 4: 우선순위 테스트 =====

-- 4-1. Emergency + Update 동시 활성화
UPDATE emergency_policy
SET is_active = true,
    content = '🚨 [우선순위 테스트] 긴급'
WHERE app_id = 'com.sweetapps.pocketchord';

UPDATE update_policy
SET target_version_code = 999,
    is_force_update = true
WHERE app_id = 'com.sweetapps.pocketchord';

-- 4-2. 정리
UPDATE emergency_policy
SET is_active = false
WHERE app_id = 'com.sweetapps.pocketchord';

UPDATE update_policy
SET target_version_code = 3
WHERE app_id = 'com.sweetapps.pocketchord';

-- ===== 최종 상태 확인 =====
SELECT
    'emergency_policy' as policy,
    CAST(is_active AS TEXT) as is_active,
    CAST(is_dismissible AS TEXT) as detail,
    LEFT(content, 30) as preview
FROM emergency_policy
WHERE app_id = 'com.sweetapps.pocketchord'
UNION ALL
SELECT
    'update_policy',
    CAST(is_active AS TEXT),
    CAST(is_force_update AS TEXT),
    CONCAT('target:', target_version_code)
FROM update_policy
WHERE app_id = 'com.sweetapps.pocketchord'
UNION ALL
SELECT
    'notice_policy',
    CAST(is_active AS TEXT),
    CAST(NULL AS TEXT),
    CONCAT('v', notice_version, ': ', LEFT(title, 20))
FROM notice_policy
WHERE app_id = 'com.sweetapps.pocketchord'
UNION ALL
SELECT
    'ad_policy',
    CAST(is_active AS TEXT),
    CAST(NULL AS TEXT),
    CONCAT('open:', ad_app_open_enabled, ' inter:', ad_interstitial_enabled, ' banner:', ad_banner_enabled)
FROM ad_policy
WHERE app_id = 'com.sweetapps.pocketchord';

-- ===== 전체 초기화 (평상시 상태) =====
UPDATE emergency_policy
SET is_active = false
WHERE app_id = 'com.sweetapps.pocketchord';

UPDATE update_policy
SET target_version_code = 3,
    is_force_update = false
WHERE app_id = 'com.sweetapps.pocketchord';

UPDATE notice_policy
SET is_active = true,
    title = '환영합니다! 🎉',
    content = 'PocketChord를 이용해 주셔서 감사합니다!\n더 나은 서비스를 제공하기 위해 노력하겠습니다.',
    notice_version = 1
WHERE app_id = 'com.sweetapps.pocketchord';
