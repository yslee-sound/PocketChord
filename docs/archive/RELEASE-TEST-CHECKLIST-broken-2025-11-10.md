WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 2: 최종 검증
- [ ] ✅ is_active = true
- [ ] ✅ ad_app_open_enabled = true
- [ ] ✅ ad_interstitial_enabled = true
- [ ] ✅ ad_banner_enabled = true
- [ ] ✅ max_per_hour = 2
- [ ] ✅ max_per_day = 15

#### Step 3: 실제 동작 확인
- [ ] 앱 재실행
- [ ] ✅ 모든 광고 정상 표시
- [ ] ✅ Phase 5 완료!

---

## 🎬 Phase 6: 종합 시나리오 테스트

## 🎬 Phase 6: 종합 시나리오 테스트

### 시나리오 6-1: 완전 깨끗한 상태
# 🚀 릴리즈 테스트 체크리스트

**작성일**: 2025-11-09  
**최종 업데이트**: 2025-11-10  
**목적**: 4개 테이블 팝업 시스템 완전 검증  
**소요 시간**: 약 30-40분  

---

## 📋 목차

1. [사전 준비](#사전-준비)
2. [테스트 환경 설정](#테스트-환경-설정)
3. [Phase 1: emergency_policy 테스트](#phase-1-emergency_policy-테스트)
4. [Phase 2: update_policy 테스트](#phase-2-update_policy-테스트)
5. [Phase 3: notice_policy 테스트](#phase-3-notice_policy-테스트)
6. [Phase 4: 우선순위 테스트](#phase-4-우선순위-테스트)
7. [Phase 5: 종합 시나리오 테스트](#phase-5-종합-시나리오-테스트)
8. [최종 확인](#최종-확인)

---

## ✅ 사전 준비

### 1. 테스트 환경 선택 ⭐

**중요**: 어떤 환경을 테스트할지 먼저 결정하세요!

#### 옵션 A: 프로덕션(릴리즈) 테스트 ✅ **권장**
### 시나리오 6-2: 실제 프로덕션 상태
app_id: 'com.sweetapps.pocketchord'
목적: 실제 사용자가 받을 릴리즈 빌드 검증
언제: 릴리즈 전 최종 검증
SQL 파일: docs/sql/test-scripts-release.sql ⭐
```

#### 옵션 B: 개발(Debug) 테스트
```
app_id: 'com.sweetapps.pocketchord.debug'
목적: 개발 중 빠른 테스트
언제: 개발 단계, 빠른 검증
SQL 파일: docs/sql/test-scripts-debug.sql ⭐
```

**빠른 시작**:
1. 위의 SQL 파일 중 하나를 선택
2. 파일 열기
3. 필요한 SQL 복사해서 Supabase에서 실행

**이 체크리스트의 기본값**: `'com.sweetapps.pocketchord'` (프로덕션)


---

### 2. Supabase 접속
- [ ] Supabase 대시보드 접속
- [ ] PocketChord 프로젝트 선택
- [ ] SQL Editor 열기 준비

### 3. Android Studio 준비
- [ ] 프로젝트 열기
- [ ] Logcat 준비 (필터: "HomeScreen")
- [ ] 테스트 기기/에뮬레이터 연결 확인
- [ ] **빌드 타입 확인**: Release 또는 Debug

---

### 4. 초기 상태 확인

**목적**: 테스트 시작 전 각 팝업의 현재 상태를 기록합니다.
- ✅ 어떤 팝업이 활성화되어 있는지
- ✅ 각 팝업의 설정 값 (버전, 강제 여부 등)
- ✅ 테스트 후 원래대로 복구하기 위한 기준점

#### 📋 릴리즈(프로덕션) 버전 ⭐ 권장

```sql
-- 현재 상태 확인 (릴리즈용)
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

### 시나리오 1-1: 긴급 팝업 (X 버튼 있음)

#### Step 1: 활성화
```sql
UPDATE emergency_policy 
SET is_active = true,
    is_dismissible = true,
    content = '🚨 [테스트] 긴급 테스트입니다. X 버튼으로 닫을 수 있습니다.'
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 2: 앱 실행
- [ ] 앱 완전 종료
- [ ] 앱 재실행
- [ ] **예상**: 긴급 팝업 즉시 표시

#### Step 3: 검증
- [ ] ✅ 긴급 팝업 표시됨
- [ ] ✅ 제목: "🚨 긴급공지"
- [ ] ✅ 배지: "긴급" 표시
- [ ] ✅ **X 버튼 있음** (우측 상단)
- [ ] ✅ 내용: 설정한 content 표시
- [ ] ✅ "새 앱 설치하기" 버튼 있음

#### Step 4: X 버튼 클릭
- [ ] X 버튼 클릭
- [ ] **예상**: 팝업 닫힘
- [ ] ✅ 팝업 닫힘
- [ ] ✅ 홈 화면 정상 표시

#### Step 5: 재실행 (추적 없음 확인)
- [ ] 앱 완전 종료
- [ ] 앱 재실행
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
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 2: 앱 실행
- [ ] 앱 완전 종료
- [ ] 앱 재실행

#### Step 3: 검증
- [ ] ✅ 강제 업데이트 팝업 표시
- [ ] ✅ 제목: "앱 업데이트"
- [ ] ✅ **"나중에" 버튼 없음** ⭐
- [ ] ✅ "지금 업데이트" 버튼만 있음
- [ ] ✅ 릴리즈 노트 표시됨
- [ ] ✅ 뒤로가기 차단됨 (테스트)

#### Step 4: 재실행 확인
- [ ] 앱 완전 종료 (업데이트 안 함)
- [ ] 앱 재실행
- [ ] **예상**: 강제 업데이트 팝업 다시 표시
- [ ] ✅ 팝업 다시 표시됨 ⭐

#### Logcat 확인
```
예상 로그:
✅ "Phase 1: Trying update_policy"
✅ "update_policy found: targetVersion=999, isForce=true"
✅ "Decision: FORCE UPDATE from update_policy"
```

- [ ] ✅ 로그 확인 완료

---

### 시나리오 2-2: 선택적 업데이트

#### Step 1: 수정
```sql
UPDATE update_policy 
SET is_force_update = false,  -- 선택적으로 변경
    message = '[테스트] 새로운 기능이 추가되었습니다',
    release_notes = '• [테스트] 다크 모드 추가\n• [테스트] 성능 개선\n• [테스트] UI 업데이트'
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 2: SharedPreferences 초기화
- [ ] 앱 데이터 삭제 (설정 → 앱 → PocketChord → 데이터 삭제)
- [ ] 또는: 앱 재설치

#### Step 3: 앱 실행
- [ ] 앱 실행

#### Step 4: 검증
- [ ] ✅ 선택적 업데이트 팝업 표시
- [ ] ✅ **"나중에" 버튼 있음** ⭐
- [ ] ✅ "지금 업데이트" 버튼 있음
- [ ] ✅ 릴리즈 노트 표시됨

#### Step 5: "나중에" 클릭
- [ ] "나중에" 버튼 클릭
- [ ] **예상**: 팝업 닫힘
- [ ] ✅ 팝업 닫힘

#### Step 6: 재실행 (추적 확인)
- [ ] 앱 완전 종료
- [ ] 앱 재실행
- [ ] **예상**: 업데이트 팝업 표시 **안 됨** ⭐
- [ ] ✅ 팝업 표시 안 됨 (추적됨)

#### Step 7: 버전 증가 테스트
```sql
-- 버전을 더 높게 변경
UPDATE update_policy 
SET target_version_code = 1000  -- 더 높은 버전
WHERE app_id = 'com.sweetapps.pocketchord';
```

- [ ] 앱 재실행
- [ ] **예상**: 업데이트 팝업 **다시 표시됨** ⭐
- [ ] ✅ 팝업 다시 표시됨 (새 버전)

#### Logcat 확인
```
예상 로그:
✅ "Decision: OPTIONAL UPDATE from update_policy (target=1000)"
```

- [ ] ✅ 로그 확인 완료

#### Step 8: 정리
```sql
-- 테스트 완료 후 target을 낮게
UPDATE update_policy 
SET target_version_code = 1
WHERE app_id = 'com.sweetapps.pocketchord';
```

- [ ] ✅ 정리 완료

---

## 📢 Phase 3: notice_policy 테스트

### 시나리오 3-1: 공지사항 표시

#### Step 1: 준비
```sql
-- 현재 상태 확인
SELECT notice_version, title, is_active 
FROM notice_policy 
WHERE app_id = 'com.sweetapps.pocketchord';
```

**기록**: `notice_version = _____`

#### Step 2: SharedPreferences 초기화
- [ ] 앱 데이터 삭제
- [ ] 또는: 앱 재설치

#### Step 3: 앱 실행
- [ ] 앱 실행

#### Step 4: 검증
- [ ] ✅ 공지사항 팝업 표시
- [ ] ✅ 제목 표시됨
- [ ] ✅ 내용 표시됨
- [ ] ✅ X 버튼 있음

#### Step 5: X 클릭
- [ ] X 버튼 클릭
- [ ] ✅ 팝업 닫힘

#### Step 6: 재실행 (추적 확인)
- [ ] 앱 완전 종료
- [ ] 앱 재실행
- [ ] **예상**: 공지 팝업 표시 **안 됨** ⭐
- [ ] ✅ 팝업 표시 안 됨 (추적됨)

#### Logcat 확인
```
예상 로그:
✅ "Phase 3: Checking notice_policy"
✅ "notice_policy found: version=1, title=환영합니다! 🎉"
✅ "Notice already viewed (version=1), skipping"
```

- [ ] ✅ 로그 확인 완료

---

### 시나리오 3-2: 오타 수정 (버전 유지)

#### Step 1: 오타 수정
```sql
-- content만 수정 (버전은 그대로!)
UPDATE notice_policy 
SET content = 'PocketChord를 이용해 주셔서 정말 감사합니다!'  -- 약간 수정
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

## 📱 Phase 5: ad_policy 테스트

**목적**: 광고 제어 시스템 검증 (App Open, Interstitial, Banner)

### 시나리오 5-1: 초기 상태 확인

#### Step 1: 현재 광고 설정 조회
```sql
SELECT app_id, is_active, 
       ad_app_open_enabled, 
       ad_interstitial_enabled, 
       ad_banner_enabled,
       ad_interstitial_max_per_hour,
       ad_interstitial_max_per_day
FROM ad_policy
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 2: 검증
- [ ] ✅ 테이블 데이터 존재 확인
- [ ] ✅ is_active = true
- [ ] ✅ 모든 광고 타입 enabled = true
- [ ] ✅ max_per_hour = 2
- [ ] ✅ max_per_day = 15

---

### 시나리오 5-2: 전체 광고 비활성화

#### Step 1: 전체 광고 OFF
```sql
UPDATE ad_policy
SET is_active = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 2: 앱 실행 및 검증
- [ ] 앱 완전 종료
- [ ] 앱 재실행
- [ ] ✅ App Open 광고 **표시 안 됨**
- [ ] 코드 여러 개 조회
- [ ] ✅ Interstitial 광고 **표시 안 됨**
- [ ] ✅ Banner 광고 **표시 안 됨**

#### Logcat 확인
```
예상 로그:
AdPolicy: is_active=false
AdMob: All ads disabled by policy
```
- [ ] ✅ 로그 확인 완료

#### Step 3: 정리
```sql
UPDATE ad_policy
SET is_active = true
WHERE app_id = 'com.sweetapps.pocketchord';
```
- [ ] ✅ 재활성화 완료

---

### 시나리오 5-3: App Open 광고 제어

#### Step 1: App Open만 비활성화
```sql
UPDATE ad_policy
SET ad_app_open_enabled = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 2: 앱 실행 및 검증
- [ ] 앱 완전 종료
- [ ] 앱 재실행
- [ ] ✅ App Open 광고 **표시 안 됨**
- [ ] 백그라운드 → 포그라운드
- [ ] ✅ App Open 광고 **표시 안 됨**
- [ ] 코드 조회 (3회)
- [ ] ✅ Interstitial 광고 **정상 표시**
- [ ] ✅ Banner 광고 **정상 표시**

#### Logcat 확인
```
AdPolicy: App Open enabled=false
AdMob: App Open Ad disabled by policy
```
- [ ] ✅ 로그 확인 완료

#### Step 3: 정리
```sql
UPDATE ad_policy
SET ad_app_open_enabled = true
WHERE app_id = 'com.sweetapps.pocketchord';
```
- [ ] ✅ 재활성화 완료

---

### 시나리오 5-4: Interstitial 광고 제어

#### Step 1: Interstitial만 비활성화
```sql
UPDATE ad_policy
SET ad_interstitial_enabled = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 2: 앱 실행 및 검증
- [ ] 앱 재실행
- [ ] ✅ App Open 광고 **정상 표시** (백그라운드 복귀 시)
- [ ] 코드 여러 개 조회 (3회 이상)
- [ ] ✅ Interstitial 광고 **표시 안 됨**
- [ ] ✅ Banner 광고 **정상 표시**

#### Logcat 확인
```
AdPolicy: Interstitial enabled=false
AdMob: Interstitial Ad disabled by policy
```
- [ ] ✅ 로그 확인 완료

#### Step 3: 정리
```sql
UPDATE ad_policy
SET ad_interstitial_enabled = true
WHERE app_id = 'com.sweetapps.pocketchord';
```
- [ ] ✅ 재활성화 완료

---

### 시나리오 5-5: Banner 광고 제어

#### Step 1: Banner만 비활성화
```sql
UPDATE ad_policy
SET ad_banner_enabled = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 2: 앱 실행 및 검증
- [ ] 앱 재실행
- [ ] ✅ App Open 광고 **정상 표시**
- [ ] ✅ 화면 하단 배너 **표시 안 됨**
- [ ] 코드 조회 (3회)
- [ ] ✅ Interstitial 광고 **정상 표시**

#### Logcat 확인
```
AdPolicy: Banner enabled=false
AdMob: Banner Ad disabled by policy
```
- [ ] ✅ 로그 확인 완료

#### Step 3: 정리
```sql
UPDATE ad_policy
SET ad_banner_enabled = true
WHERE app_id = 'com.sweetapps.pocketchord';
```
- [ ] ✅ 재활성화 완료

---

### 시나리오 5-6: 빈도 제한 테스트 (선택)

#### Step 1: 빈도 제한 낮추기
```sql
UPDATE ad_policy
SET ad_interstitial_max_per_hour = 1,
    ad_interstitial_max_per_day = 3
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 2: 테스트
- [ ] 앱 재실행
- [ ] 코드 조회 → 전면 광고 표시 (1회)
- [ ] 코드 조회 → 전면 광고 **표시 안 됨** (제한 도달)

#### Logcat 확인
```
InterstitialAdManager: Ad shown (1/1 per hour)
InterstitialAdManager: ⚠️ Hourly limit reached
```
- [ ] ✅ 로그 확인 완료

#### Step 3: 운영 설정 복구
```sql
UPDATE ad_policy
SET ad_interstitial_max_per_hour = 2,
    ad_interstitial_max_per_day = 15
WHERE app_id = 'com.sweetapps.pocketchord';
```
- [ ] ✅ 운영 설정 복구 완료

---

### 시나리오 5-7: 최종 확인

#### Step 1: 모든 광고 정상화 확인
```sql
SELECT app_id, is_active, 
       ad_app_open_enabled, 
       ad_interstitial_enabled, 
       ad_banner_enabled,
       ad_interstitial_max_per_hour,
       ad_interstitial_max_per_day
FROM ad_policy

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

#### Emergency Policy
- [ ] ✅ 최우선순위 작동 (다른 팝업 무시)
- [ ] ✅ is_dismissible=true → X 버튼 있음
- [ ] ✅ is_dismissible=false → X 버튼 없음
- [ ] ✅ 추적 없음 (매번 표시)
- [ ] ✅ 뒤로가기 차단

#### Update Policy
- [ ] ✅ 강제 업데이트: "나중에" 없음
- [ ] ✅ 선택적 업데이트: "나중에" 있음
- [ ] ✅ "나중에" 클릭 후 추적됨
- [ ] ✅ 새 버전 나오면 다시 표시
- [ ] ✅ 릴리즈 노트 표시
- [ ] ✅ 강제 업데이트 시 뒤로가기 차단

#### Notice Policy
- [ ] ✅ 버전 기반 추적
- [ ] ✅ 오타 수정 (버전 유지) → 재표시 안 됨
- [ ] ✅ 새 공지 (버전 증가) → 재표시됨
- [ ] ✅ X 클릭 후 추적됨

#### 우선순위
- [ ] ✅ emergency > update > notice
- [ ] ✅ 상위 우선순위 있으면 하위 무시

---

## 📊 테스트 결과 요약

### 테스트 통과 여부

| Phase | 테스트 항목 | 결과 | 비고 |
|-------|-----------|------|------|
| Phase 1 | emergency (X 있음) | ⬜ PASS / ⬜ FAIL | |
| Phase 1 | emergency (X 없음) | ⬜ PASS / ⬜ FAIL | |
| Phase 2 | 강제 업데이트 | ⬜ PASS / ⬜ FAIL | |
| Phase 2 | 선택적 업데이트 | ⬜ PASS / ⬜ FAIL | |
| Phase 3 | 공지 표시 | ⬜ PASS / ⬜ FAIL | |
| Phase 3 | 오타 수정 (버전 유지) | ⬜ PASS / ⬜ FAIL | |
| Phase 3 | 새 공지 (버전 증가) | ⬜ PASS / ⬜ FAIL | |
| Phase 4 | emergency > update | ⬜ PASS / ⬜ FAIL | |
| Phase 4 | update > notice | ⬜ PASS / ⬜ FAIL | |
| Phase 5 | 전체 광고 ON/OFF | ⬜ PASS / ⬜ FAIL | |
| Phase 5 | App Open 제어 | ⬜ PASS / ⬜ FAIL | |
| Phase 5 | Interstitial 제어 | ⬜ PASS / ⬜ FAIL | |
| Phase 5 | Banner 제어 | ⬜ PASS / ⬜ FAIL | |
| Phase 5 | 빈도 제한 (선택) | ⬜ PASS / ⬜ FAIL | |
| Phase 6 | 깨끗한 상태 | ⬜ PASS / ⬜ FAIL | |
| Phase 6 | 프로덕션 상태 | ⬜ PASS / ⬜ FAIL | |

### 발견된 이슈

```
1. _____________________________________________
2. _____________________________________________
3. _____________________________________________
```

---

## ✅ 릴리즈 승인

### 승인 조건
- [ ] ✅ 모든 Phase 테스트 PASS
- [ ] ✅ 이슈 0개 또는 모두 해결됨
- [ ] ✅ 프로덕션 상태 확인 완료
- [ ] ✅ 데이터베이스 상태 정상

### 최종 승인
- [ ] ✅ **릴리즈 준비 완료** 🚀

**테스트 완료 일시**: _______________  
**테스터**: _______________  
**승인자**: _______________

---

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

