# 🚀 릴리즈 테스트 체크리스트

**버전**: v3.0  
**최종 업데이트**: 2025-11-10  
**소요 시간**: 약 40-50분

---

## 📦 Release 폴더 문서

| 문서 | 버전 | 설명 |
|------|------|------|
| **RELEASE-TEST-CHECKLIST.md** | v3.0 | 이 문서 (전체 가이드) |
| **RELEASE-TEST-PHASE1-RELEASE.md** | v3.0 | Emergency + 팝업 시스템 개요 |
| **RELEASE-TEST-PHASE2.1-RELEASE.md** | v4.0 | Update + 가이드 |
| **RELEASE-TEST-PHASE2.2-SETUP.md** | - | Update 시간 재표시 설정 |
| **RELEASE-TEST-PHASE2.3-ADVANCED.md** | - | Update 고급 테스트 |
| **RELEASE-TEST-PHASE2.4-SCENARIOS.md** | - | Update 시나리오 테스트 |
| **RELEASE-TEST-PHASE3-RELEASE.md** | v3.0 | Notice + 버전 관리 |
| **RELEASE-TEST-PHASE4-RELEASE.md** | v3.0 | 우선순위 테스트 |
| **RELEASE-TEST-PHASE5-RELEASE.md** | v3.0 | Ad Policy + 배포 |
| **a_RELEASE_SIGNING.md** | - | 릴리즈 서명 |

---

## 📋 목차

1. [사전 준비](#1-사전-준비)
2. [Phase 1: Emergency Policy](#2-phase-1-emergency-policy)
3. [Phase 2: Update Policy](#3-phase-2-update-policy)
4. [Phase 3: Notice Policy](#4-phase-3-notice-policy)
5. [Phase 4: 우선순위 테스트](#5-phase-4-우선순위-테스트)
6. [Phase 5: 종합 시나리오](#6-phase-5-종합-시나리오)
7. [최종 확인](#7-최종-확인)

**💡 Tip**: Phase별 상세 내용은 각 PHASE 문서 참조

---

## 1 사전 준비

### 테스트 환경 선택

| 빌드 타입 | app_id | 용도 |
|----------|--------|------|
| **릴리즈** ✅ | `com.sweetapps.pocketchord` | 실제 사용자 환경 검증 (권장) |
| Debug | `com.sweetapps.pocketchord.debug` | 개발 중 빠른 테스트 |

### 준비 사항

- [ ] Supabase 대시보드 접속
- [ ] Android Studio 프로젝트 열기
- [ ] Logcat 준비 (필터: "HomeScreen")
- [ ] 테스트 기기 연결 확인

### 초기 상태 확인

```sql
-- 현재 상태 확인
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
```

**기록**:
```
emergency: is_active = _____
update: target = _____, force = _____
notice: version = _____
ad_policy: is_active = _____
```

---

## 2 Phase 1: Emergency Policy

**상세 테스트**: 👉 **[PHASE1 문서](RELEASE-TEST-PHASE1-RELEASE.md)**

### 빠른 체크리스트

| 항목 | 확인 |
|------|------|
| X 버튼 있음 (is_dismissible=true) | ⬜ |
| X 버튼 없음 (is_dismissible=false) | ⬜ |
| 뒤로가기 차단 (강제 모드) | ⬜ |
| 재실행 시 다시 표시 (추적 없음) | ⬜ |
| 정리 (비활성화) | ⬜ |

---

## 3 Phase 2: Update Policy

**상세 테스트**: 👉 **[PHASE2.1 문서](RELEASE-TEST-PHASE2.1-RELEASE.md)**

### 빠른 체크리스트

| 항목 | 확인 |
|------|------|
| 강제 업데이트 (is_force_update=true) | ⬜ |
| 선택적 업데이트 (is_force_update=false) | ⬜ |
| "나중에" 클릭 후 추적 | ⬜ |
| SharedPreferences 초기화 | ⬜ |
| 정리 (target_version_code=1) | ⬜ |

**⚠️ Phase 2.2 (시간 재표시)**: 별도 테스트 필요 시
- [PHASE2.2-SETUP.md](RELEASE-TEST-PHASE2.2-SETUP.md)
- [PHASE2.4-SCENARIOS.md](RELEASE-TEST-PHASE2.4-SCENARIOS.md)

---

## 4 Phase 3: Notice Policy

**상세 테스트**: 👉 **[PHASE3 문서](RELEASE-TEST-PHASE3-RELEASE.md)**

### 빠른 체크리스트

| 항목 | 확인 |
|------|------|
| 공지 활성화 및 표시 | ⬜ |
| 오타 수정 (버전 유지) → 재표시 안 됨 | ⬜ |
| 새 공지 (버전 증가) → 재표시됨 | ⬜ |
| 정리 (비활성화) | ⬜ |

---

## 5 Phase 4: 우선순위 테스트

**상세 테스트**: 👉 **[PHASE4 문서](RELEASE-TEST-PHASE4-RELEASE.md)**

### 빠른 체크리스트

| 항목 | 확인 |
|------|------|
| Emergency + Update → Emergency만 표시 | ⬜ |
| Update + Notice → Update만 표시 | ⬜ |
| 모두 비활성화 → 팝업 없음 | ⬜ |

---

## 6 Phase 5: 종합 시나리오
FROM notice_policy 
WHERE app_id = 'com.sweetapps.pocketchord'
UNION ALL
SELECT 'ad_policy', 
       CAST(is_active AS TEXT), 
       CONCAT('open:', ad_app_open_enabled, ' inter:', ad_interstitial_enabled, ' banner:', ad_banner_enabled)
FROM ad_policy 
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 🔧 디버그 버전

```sql
-- 현재 상태 확인 (디버그용)
SELECT 'emergency_policy' as table_name, 
       CAST(is_active AS TEXT) as is_active, 
       LEFT(content, 30) as content_preview 
FROM emergency_policy 
WHERE app_id = 'com.sweetapps.pocketchord.debug'
UNION ALL
SELECT 'update_policy', 
       CAST(is_active AS TEXT), 
       CONCAT('target:', target_version_code, ' force:', is_force_update)
FROM update_policy 
WHERE app_id = 'com.sweetapps.pocketchord.debug'
UNION ALL
SELECT 'notice_policy', 
       CAST(is_active AS TEXT), 
       CONCAT('v', notice_version, ': ', LEFT(title, 20))
FROM notice_policy 
WHERE app_id = 'com.sweetapps.pocketchord.debug'
UNION ALL
SELECT 'ad_policy', 
       CAST(is_active AS TEXT), 
       CONCAT('open:', ad_app_open_enabled, ' inter:', ad_interstitial_enabled, ' banner:', ad_banner_enabled)
FROM ad_policy 
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**결과 예시**:
```
table_name          | is_active | content_preview
--------------------+-----------+----------------------------------
emergency_policy    | false     | ⚠️ [테스트] 이 앱은...
update_policy       | true      | target:1 force:false
notice_policy       | true      | v1: 환영합니다! 🎉
ad_policy           | true      | open:true inter:true banner:true
```

**이것의 의미**:
- `emergency: false` → 평상시 상태 (꺼져있음) ✅
- `update: target:1` → 현재 버전과 같거나 낮음 (팝업 안 뜸) ✅  
- `notice: v1` → 신규 사용자에게만 표시됨 ✅
- `ad_policy: open/inter/banner` → 각 광고 타입 활성화 여부 ✅

**기록**:
```
emergency: is_active = _____
update: is_active = _____, target = _____
notice: is_active = _____, version = _____
ad_policy: is_active = _____, open = _____, inter = _____, banner = _____
```

---

## 🔥 Phase 1: emergency_policy 테스트

**목적**: Emergency Policy 동작 검증 (최우선 팝업)

**상세 테스트**: 👉 **[RELEASE-TEST-PHASE1-RELEASE.md](RELEASE-TEST-PHASE1-RELEASE.md)**

### 빠른 체크리스트

| 항목 | 확인 |
|------|------|
| X 버튼 있음 (is_dismissible=true) | ⬜ |
| X 버튼 없음 (is_dismissible=false) | ⬜ |
| 뒤로가기 차단 (강제 모드) | ⬜ |
| 재실행 시 다시 표시 (추적 없음) | ⬜ |
| 정리 (비활성화) | ⬜ |

---

## 🔄 Phase 2: update_policy 테스트
- [ ] **예상**: 긴급 팝업 다시 표시 (추적 안 함!)
- [ ] ✅ 긁급 팝업 **다시 표시됨** ⭐

#### Logcat 확인
```
예상 로그:
✅ "Phase 2: Checking emergency_policy"
✅ "emergency_policy found: isDismissible=true"
✅ "Decision: EMERGENCY from emergency_policy"
✅ "Displaying EmergencyRedirectDialog from emergency_policy"
```

- [ ] ✅ 로그 확인 완료

---

### 시나리오 1-2: 긴급 팝업 (X 버튼 없음)

#### Step 1: 수정
```sql
UPDATE emergency_policy 
SET is_dismissible = false,
    content = '🚨 [테스트] 이 앱은 더 이상 지원되지 않습니다. 새 앱을 설치해야 합니다.'
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 2: 앱 실행
- [ ] 앱 완전 종료
- [ ] 앱 재실행

#### Step 3: 검증
- [ ] ✅ 긴급 팝업 표시됨
- [ ] ✅ **X 버튼 없음** ⭐
- [ ] ✅ 뒤로가기 버튼 막힘 (테스트 해보기)
- [ ] ✅ "새 앱 설치하기" 버튼만 있음

#### Step 4: 정리
```sql
-- 테스트 완료 후 비활성화
UPDATE emergency_policy 
SET is_active = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

- [ ] ✅ 비활성화 완료

---

## 🔄 Phase 2: update_policy 테스트

### 시나리오 2-1: 강제 업데이트

#### Step 1: 활성화
```sql
UPDATE update_policy 
SET is_active = true,
    target_version_code = 999,  -- 현재 버전보다 높게
    is_force_update = true,
    message = '[테스트] 필수 업데이트가 있습니다',
    release_notes = '• [테스트] 중요 보안 패치\n• [테스트] 필수 기능 추가'
---

## 🔄 Phase 2: update_policy 테스트

**목적**: Update Policy 동작 검증 (강제/선택적 업데이트)

**상세 테스트**: 👉 **[RELEASE-TEST-PHASE2.1-RELEASE.md](RELEASE-TEST-PHASE2.1-RELEASE.md)**

### 빠른 체크리스트

| 항목 | 확인 |
|------|------|
| 강제 업데이트 (is_force_update=true) | ⬜ |
| 선택적 업데이트 (is_force_update=false) | ⬜ |
| "나중에" 클릭 후 추적 | ⬜ |
| SharedPreferences 초기화 | ⬜ |
| 정리 (target_version_code=1) | ⬜ |

**⚠️ Phase 2.2~2.4 (시간 기반 재표시)**: 별도 문서 참조
- [PHASE2.2-SETUP.md](RELEASE-TEST-PHASE2.2-SETUP.md)
- [PHASE2.3-ADVANCED.md](RELEASE-TEST-PHASE2.3-ADVANCED.md)
- [PHASE2.4-SCENARIOS.md](RELEASE-TEST-PHASE2.4-SCENARIOS.md)

---

## 📢 Phase 3: notice_policy 테스트

**목적**: Notice Policy 동작 검증 (버전 기반 추적)

**상세 테스트**: 👉 **[RELEASE-TEST-PHASE3-RELEASE.md](RELEASE-TEST-PHASE3-RELEASE.md)**

### 빠른 체크리스트

| 항목 | 확인 |
|------|------|
| 공지 활성화 및 표시 | ⬜ |
| 오타 수정 (버전 유지) → 재표시 안 됨 | ⬜ |
| 새 공지 (버전 증가) → 재표시됨 | ⬜ |
| 정리 (비활성화) | ⬜ |

---

## 🎯 Phase 4: 우선순위 테스트

**목적**: 팝업 우선순위 로직 검증 (emergency > update > notice)

**상세 테스트**: 👉 **[RELEASE-TEST-PHASE4-RELEASE.md](RELEASE-TEST-PHASE4-RELEASE.md)**

### 빠른 체크리스트

| 항목 | 확인 |
|------|------|
| Emergency + Update → Emergency만 표시 | ⬜ |
| Update + Notice → Update만 표시 | ⬜ |
| 모두 비활성화 → 팝업 없음 | ⬜ |

---

## 🎬 Phase 5: 종합 시나리오
WHERE app_id = 'com.sweetapps.pocketchord';
-- notice_version은 변경하지 않음!
```

#### Step 2: 앱 실행
- [ ] 앱 완전 종료
- [ ] 앱 재실행

#### Step 3: 검증
- [ ] **예상**: 공지 팝업 표시 **안 됨** ⭐
- [ ] ✅ 팝업 표시 안 됨 (버전이 같으므로)

#### Logcat 확인
```
예상 로그:
✅ "Notice already viewed (version=1), skipping"
```

- [ ] ✅ 로그 확인 완료

---

### 시나리오 3-3: 새 공지 (버전 증가)

#### Step 1: 버전 증가
```sql
-- 새 공지: 버전 증가!
UPDATE notice_policy 
SET title = '🎉 11월 이벤트',
    content = '11월 특별 이벤트가 시작되었습니다!\n많은 참여 부탁드립니다.',
    notice_version = 2  -- 버전 증가! ⭐
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 2: 앱 실행
- [ ] 앱 완전 종료
- [ ] 앱 재실행

#### Step 3: 검증
- [ ] **예상**: 공지 팝업 **다시 표시됨** ⭐
- [ ] ✅ 팝업 다시 표시됨
- [ ] ✅ 새 제목: "🎉 11월 이벤트"
- [ ] ✅ 새 내용 표시됨

#### Step 4: X 클릭 후 재실행
- [ ] X 버튼 클릭
- [ ] 앱 완전 종료
- [ ] 앱 재실행
- [ ] **예상**: 공지 팝업 표시 **안 됨** (버전 2 추적됨)
- [ ] ✅ 팝업 표시 안 됨

#### Logcat 확인
```
예상 로그:
✅ "Decision: NOTICE from notice_policy (version=2)"
✅ "Marked notice version 2 as viewed"
```

- [ ] ✅ 로그 확인 완료

#### Step 5: 정리
```sql
-- 원래대로 복구
UPDATE notice_policy 
SET title = '환영합니다! 🎉',
    content = 'PocketChord를 이용해 주셔서 감사합니다!\n더 나은 서비스를 제공하기 위해 노력하겠습니다.',
    notice_version = 1
WHERE app_id = 'com.sweetapps.pocketchord';
```

- [ ] ✅ 정리 완료

---

## 🎯 Phase 4: 우선순위 테스트

### 시나리오 4-1: emergency > update

#### Step 1: 두 정책 모두 활성화
```sql
-- emergency 활성화
UPDATE emergency_policy 
SET is_active = true,
    content = '🚨 [우선순위 테스트] 긴급'
WHERE app_id = 'com.sweetapps.pocketchord';

-- update 활성화
UPDATE update_policy 
SET target_version_code = 999,
    is_force_update = true
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 2: 앱 실행
- [ ] 앱 데이터 삭제
- [ ] 앱 실행

#### Step 3: 검증
- [ ] **예상**: **emergency 팝업만** 표시 ⭐
- [ ] ✅ emergency 팝업 표시됨
- [ ] ✅ update 팝업 표시 안 됨

#### Logcat 확인
```
예상 로그:
✅ "Decision: EMERGENCY from emergency_policy"
✅ "return@LaunchedEffect" (다른 팝업 건너뜀)
```

- [ ] ✅ 로그 확인 완료

#### Step 4: 정리
```sql
UPDATE emergency_policy SET is_active = false WHERE app_id = 'com.sweetapps.pocketchord';
```

---

### 시나리오 4-2: update > notice

#### Step 1: 두 정책 모두 활성화
```sql
-- update 활성화
UPDATE update_policy 
SET target_version_code = 999,
    is_force_update = false  -- 선택적
WHERE app_id = 'com.sweetapps.pocketchord';

-- notice는 이미 활성화되어 있음 (확인)
SELECT is_active FROM notice_policy WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 2: 앱 실행
- [ ] 앱 데이터 삭제
- [ ] 앱 실행

#### Step 3: 검증
- [ ] **예상**: **update 팝업만** 표시 ⭐
- [ ] ✅ update 팝업 표시됨
- [ ] ✅ notice 팝업 표시 안 됨

#### Step 4: "나중에" 클릭 후 재실행
- [ ] "나중에" 클릭
- [ ] 앱 완전 종료
- [ ] 앱 재실행

#### Step 5: 검증
- [ ] **예상**: **notice 팝업** 표시 ⭐
- [ ] ✅ notice 팝업 표시됨 (update 추적되었으므로)

#### Logcat 확인
```
예상 로그:
✅ "update_policy exists but no update needed" (dismissed)
✅ "Decision: NOTICE from notice_policy"
```

- [ ] ✅ 로그 확인 완료

#### Step 6: 정리
```sql
UPDATE update_policy SET target_version_code = 1 WHERE app_id = 'com.sweetapps.pocketchord';
```

---

## 🎬 Phase 5: 종합 시나리오 테스트

### 시나리오 5-1: 완전 깨끗한 상태

#### Step 1: 모든 정책 비활성화
```sql
UPDATE emergency_policy SET is_active = false WHERE app_id = 'com.sweetapps.pocketchord';
UPDATE update_policy SET target_version_code = 1 WHERE app_id = 'com.sweetapps.pocketchord';
UPDATE notice_policy SET is_active = false WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 2: 앱 실행
- [ ] 앱 데이터 삭제
- [ ] 앱 실행

#### Step 3: 검증
- [ ] ✅ **팝업 없음** ⭐
- [ ] ✅ 홈 화면 바로 표시
- [ ] ✅ 코드 그리드 정상 작동

#### Logcat 확인
```
예상 로그:
✅ "emergency_policy not found or error"
✅ "update_policy exists but no update needed"
✅ "notice_policy not found or error" 또는 "is_active=false"
```

- [ ] ✅ 로그 확인 완료

---

### 시나리오 5-2: 실제 프로덕션 상태

#### Step 1: 프로덕션 설정
```sql
-- emergency: 비활성화 (평상시)
UPDATE emergency_policy SET is_active = false WHERE app_id = 'com.sweetapps.pocketchord';

-- update: 낮은 버전 (업데이트 없음)
UPDATE update_policy 
SET target_version_code = 1,  -- 현재 버전과 같거나 낮게
    is_force_update = false
WHERE app_id = 'com.sweetapps.pocketchord';

-- notice: 활성화 (평상시)
UPDATE notice_policy 
SET is_active = true,
    title = '환영합니다! 🎉',
    content = 'PocketChord를 이용해 주셔서 감사합니다!',
    notice_version = 1
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 2: 신규 사용자 시뮬레이션
- [ ] 앱 데이터 삭제 (신규 사용자)
- [ ] 앱 실행

#### Step 3: 검증
- [ ] ✅ **notice 팝업만** 표시
- [ ] ✅ 환영 메시지 표시
- [ ] ✅ X 클릭 후 홈 화면

#### Step 4: 기존 사용자 시뮬레이션
- [ ] X 클릭
- [ ] 앱 완전 종료
- [ ] 앱 재실행

#### Step 5: 검증
- [ ] ✅ **팝업 없음**
- [ ] ✅ 홈 화면 바로 표시

- [ ] ✅ 시나리오 완료

---

## 🎯 최종 확인

### 데이터베이스 상태 확인

**목적**: 테스트 완료 후 모든 설정이 원래대로 복구되었는지 확인합니다.

```sql
-- 최종 상태 확인
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
```

**프로덕션(평상시) 상태 기준**:
```
emergency: is_active = false (긴급 상황 없음)
update: target = 1 (현재 버전과 같거나 낮음)
notice: is_active = true, version = 1 (기본 환영 메시지)
ad_policy: is_active = true, 광고 타입별 설정 (프로덕션 설정)
```

**확인 항목**:
- [ ] emergency가 비활성화 되어 있는가?
- [ ] update target이 1 (또는 현재 버전 이하)인가?
- [ ] notice가 버전 1로 복구되었는가?
- [ ] ad_policy 광고 설정이 프로덕션 상태인가?
- [ ] 테스트용 메시지가 남아있지 않은가?

**최종 상태 기록**:
```
emergency: is_active = false (평상시)
update: target = 1 (평상시)
notice: is_active = true, version = 1 (평상시)
ad_policy: is_active = true, 광고 설정 = 프로덕션 (평상시)
```

---

### 코드 체크리스트

- [ ] ✅ 컴파일 에러 없음
- [ ] ✅ 경고만 있음 (정상)
- [ ] ✅ 모든 import 정상
- [ ] ✅ HomeScreen.kt 정상 작동

---

### 기능 체크리스트

---

## 7 최종 확인

### 핵심 동작 확인

| Policy | 핵심 확인 사항 | 완료 |
|--------|---------------|------|
| **Emergency** | 최우선 표시, X 버튼 제어, 추적 없음 | ⬜ |
| **Update** | 강제/선택적 모드, 버전 추적 | ⬜ |
| **Notice** | 버전 추적, 버전 증가 시 재표시 | ⬜ |
| **우선순위** | emergency > update > notice | ⬜ |

---

## 📊 테스트 결과 요약

| Phase | 항목 | 결과 | 비고 |
|-------|------|------|------|
| Phase 1 | Emergency (X 있음/없음) | ⬜ PASS / ⬜ FAIL | |
| Phase 2 | Update (강제/선택적) | ⬜ PASS / ⬜ FAIL | |
| Phase 3 | Notice (버전 관리) | ⬜ PASS / ⬜ FAIL | |
| Phase 4 | 우선순위 | ⬜ PASS / ⬜ FAIL | |
| Phase 5 | 종합 시나리오 | ⬜ PASS / ⬜ FAIL | |

### 발견된 이슈

```
1. _____________________________________________
2. _____________________________________________
```

---

## ✅ 릴리즈 승인

**승인 조건**:
- [ ] 모든 Phase 테스트 PASS
- [ ] 이슈 0개 또는 모두 해결됨
- [ ] 프로덕션 상태 확인 완료

**최종 승인**:
- [ ] **릴리즈 준비 완료** 🚀

**테스트 완료 일시**: _______________  
**테스터**: _______________

---

**문서 버전**: v3.0  
**마지막 수정**: 2025-11-10

## 📝 부록

### 빠른 초기화 스크립트

```sql
-- 모든 정책을 평상시 상태로 초기화
UPDATE emergency_policy 
SET is_active = false 
WHERE app_id = 'com.sweetapps.pocketchord';

UPDATE update_policy 
SET target_version_code = 1,
    is_force_update = false
WHERE app_id = 'com.sweetapps.pocketchord';

UPDATE notice_policy 
SET is_active = true,
    title = '환영합니다! 🎉',
    content = 'PocketChord를 이용해 주셔서 감사합니다!\n더 나은 서비스를 제공하기 위해 노력하겠습니다.',
    notice_version = 1
WHERE app_id = 'com.sweetapps.pocketchord';
```

### 앱 데이터 초기화 방법

**Android 기기**:
1. 설정 → 앱 → PocketChord
2. 저장공간 → 데이터 삭제

**에뮬레이터**:
```bash
adb shell pm clear com.sweetapps.pocketchord
```

---

**🎉 테스트 체크리스트 완료!**

이 체크리스트를 따라하면 모든 시나리오를 빠짐없이 테스트할 수 있습니다!

