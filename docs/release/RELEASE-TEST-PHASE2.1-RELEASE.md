# 릴리즈 테스트 - Phase 2 (Update Policy)

**버전**: v4.0  
**최종 업데이트**: 2025-11-10  
**소요 시간**: 약 15분

---

## 📋 목차

1. [Update Policy 개념](#1-update-policy-개념)
2. [Phase 2 테스트](#2-phase-2-테스트)
3. [운영 가이드](#3-운영-가이드)
4. [문제 해결](#4-문제-해결)
5. [체크리스트](#5-체크리스트)

---

## 1 Update Policy 개념

### 1 target_version_code란?

**Play Store에 올릴 다음 버전의 versionCode입니다.**

```
현재 앱: versionCode = 10
다음 업데이트: versionCode = 11

→ target_version_code = 11 로 설정
→ 버전 10 사용자에게 "업데이트하세요" 팝업 표시
```

### 2 업데이트 타입

| 타입 | DB 설정 | 1~3회차 | 4회차 (laterCount >= 3) | 시간 기반 설정 | 사용 시기 |
|------|---------|---------|------------------------|--------------|----------|
| **원본 강제 업데이트** | `is_force_update = true` | "나중에" ❌ | - | ❌ 설정 무관 | 중요 버그, 보안 이슈 |
| **선택적 업데이트** | `is_force_update = false` | "나중에" ✅ | "나중에" ❌<br>(동적 강제 전환) | ✅ `reshow_interval_*`<br>✅ `max_later_count` | 일반 업데이트 |

**💡 핵심 차이점**: 
- **원본 강제 업데이트** (`is_force_update = true`):
  - DB 설정부터 강제
  - 처음부터 "나중에" 버튼이 **아예 없음**
  - `max_later_count` 무관 (재표시 로직 자체가 작동 안 함)
  
- **선택적 → 동적 강제 전환** (`is_force_update = false` + `laterCount >= max_later_count`):
  - DB는 `is_force_update = false`로 유지
  - 1~3회차: "나중에" 버튼 있음 (사용자 친화적)
  - 4회차부터: 클라이언트에서 `isForce = true`로 동적 전환
  - "나중에" 버튼 사라짐

---

## 2 Phase 2 테스트

### 목표

- 강제 업데이트 (뒤로가기 차단)
- 선택적 업데이트 ("나중에" 버튼)
- SharedPreferences 추적 - "나중에" 클릭 횟수 및 재표시 시간 관리

**동작**:
```
1~3회차: "나중에" 클릭 → 24시간 후 재표시
4회차: laterCount >= 3 → 강제 전환 (나중에 버튼 숨김)
```

---

### 2 시나리오 1: 강제 업데이트

#### SQL
```sql
-- 강제 업데이트 활성화
UPDATE update_policy
SET is_active = true,
    target_version_code = 4,  -- 현재보다 높게
    is_force_update = true,
    release_notes = '중요 업데이트',
    download_url = 'https://play.google.com/'
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

#### 검증
- [ ] 앱 실행 → 업데이트 팝업 표시
- [ ] 제목: "새 버전 사용 가능"
- [ ] **"나중에" 버튼 없음** ⭐
- [ ] release_notes 내용 표시
- [ ] 뒤로가기 **차단됨** (테스트 필요)
- [ ] "업데이트" 버튼 클릭 → Play Store 이동
- [ ] **참고**: `reshow_interval_hours`, `max_later_count` 설정 무관 (항상 즉시 표시)

---

### 3 시나리오 2: 선택적 업데이트 (디버그 테스트)

**💡 핵심 동작**: 
- DB: `is_force_update = false` 유지 (변경 없음)
- 1~3회차: "나중에" 버튼 있음
- 4회차: `laterCount >= 3` 도달 → **클라이언트에서** `isForce = true`로 동적 전환
- 결과: "나중에" 버튼 사라짐

**시간 기반 설정**:
- "나중에" 클릭 후 → `reshow_interval_seconds` 경과 시 재표시
- `laterCount >= max_later_count` 도달 시 → 강제 업데이트로 자동 전환

#### SQL
```sql
-- 선택적 업데이트 활성화 (디버그용 - 60초 간격)
UPDATE update_policy
SET is_active = true,
    target_version_code = 4,
    is_force_update = false,
    release_notes = '선택적 업데이트 3회 -> 강제',
    download_url = 'https://play.google.com/',
    reshow_interval_seconds = 60,  -- 60초 후 재표시 (테스트용)
    max_later_count = 3             -- 3회 클릭 후 강제 업데이트로 전환
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

#### 검증

**1회차 테스트**:
- [ ] 앱 실행 → 업데이트 팝업 표시
- [ ] **"나중에" 버튼 있음** ⭐
- [ ] "업데이트" 버튼 있음
- [ ] "나중에" 클릭 → 팝업 닫힘
- [ ] 앱 재실행 → 팝업 **표시 안 됨** (SharedPreferences 추적)
- [ ] **60초 대기** ⏱️
- [ ] 60초 후 앱 재실행 → 팝업 **다시 표시됨** ✅

**2회차 테스트**:
- [ ] "나중에" 클릭 → 팝업 닫힘
- [ ] **60초 대기** ⏱️
- [ ] **추가 테스트**: 60초 경과 후 다른 화면(코드 등) → 홈 화면 복귀
  - [ ] 팝업 **표시 안 됨** (정상) ✅
  - [ ] 앱을 완전히 종료 → 재시작 → 팝업 **표시됨** (정상) ✅
- [ ] 60초 후 앱 재실행 → 팝업 **다시 표시됨** ✅

**3회차 테스트**:
- [ ] "나중에" 클릭 → 팝업 닫힘 (총 3회 클릭)
- [ ] **60초 대기** ⏱️
- [ ] 60초 후 앱 재실행 → 팝업 **다시 표시됨** ⭐
- [ ] **"나중에" 버튼 없음** (강제 업데이트로 전환됨)
- [ ] laterCount = 3 도달 → 클라이언트에서 `isForce = true`로 동적 전환
- [ ] **참고**: DB는 여전히 `is_force_update = false`, 클라이언트에서만 전환

**⚠️ 운영 환경**: `reshow_interval_hours = 24`로 설정 필요

---

#### 📊 SharedPreferences 추적 데이터 확인 (선택사항)

**목적**: "나중에" 클릭 횟수와 시간 추적이 정상 작동하는지 확인

**확인할 데이터**:
- `update_dismissed_time`: 마지막 "나중에" 클릭 시간 (밀리초)
- `update_later_count`: "나중에" 클릭 횟수
- `dismissedVersionCode`: 마지막으로 숨긴 버전 코드

**명령어**:
```bash
# 업데이트 추적 데이터 확인
adb -s emulator-5554 shell run-as com.sweetapps.pocketchord.debug cat /data/data/com.sweetapps.pocketchord.debug/shared_prefs/update_preferences.xml

# 만약 "more than one device/emulator" 오류가 발생하면:
# 1. 연결된 디바이스 목록 확인
adb devices

# 2. 목록에서 사용할 에뮬레이터 확인 (예: emulator-5554)
# 3. -s 옵션으로 디바이스 지정
adb -s <디바이스명> shell run-as com.sweetapps.pocketchord.debug cat /data/data/com.sweetapps.pocketchord.debug/shared_prefs/update_preferences.xml
```

**예상 출력**:
```xml
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <long name="update_dismissed_time" value="1762757809072" />
    <int name="dismissedVersionCode" value="4" />
    <int name="update_later_count" value="3" />
</map>
```

**확인 포인트**:
- ✅ `update_later_count`가 "나중에" 클릭 횟수와 일치하는가?
- ✅ `update_dismissed_time`이 최근 클릭 시간과 일치하는가?
- ✅ 3회 클릭 후 `update_later_count = 3`인가?

---

### 4 시나리오 3: SharedPreferences 초기화

#### 목적
"나중에"로 숨긴 팝업을 다시 표시하려면 추적 데이터 삭제 필요

#### 방법 1: 앱 데이터 전체 삭제 (권장)
```bash
adb -s emulator-5554 shell pm clear com.sweetapps.pocketchord.debug
```

#### 방법 2: SharedPreferences만 삭제
```bash
adb -s emulator-5554 shell run-as com.sweetapps.pocketchord.debug rm -r /data/data/com.sweetapps.pocketchord.debug/shared_prefs/
```

#### 검증
- [ ] 초기화 실행
- [ ] 앱 재실행
- [ ] 팝업 **다시 표시됨** ✅

---

### 5 정리: 비활성화

#### SQL
```sql
-- 업데이트 팝업 비활성화
UPDATE update_policy
SET is_active = false,
    target_version_code = 1
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 검증
- [ ] 앱 재실행 → 팝업 **표시 안 됨**
- [ ] Phase 2 완료! ✅

---

## 3 운영 가이드

### 1 새 버전 출시 절차

#### Step 1: 새 버전 빌드
```
현재 Play Store: versionCode = 10
새 APK 빌드: versionCode = 11
```

#### Step 2: Supabase 설정 (출시 전)

**일반 업데이트 (권장)**:
```sql
UPDATE update_policy
SET target_version_code = 11,
    is_force_update = false,
    release_notes = '• 새로운 기능 추가\n• 버그 수정',
    is_active = true
WHERE app_id = 'com.sweetapps.pocketchord';
```

**중요 업데이트 (강제)**:
```sql
UPDATE update_policy
SET target_version_code = 11,
    is_force_update = true,
    release_notes = '• 중요 보안 패치\n• 필수 업데이트',
    is_active = true
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 3: Play Store 출시
- APK 업로드 (versionCode = 11)
- 심사 통과 후 배포

#### Step 4: 사용자 경험
```
버전 10 사용자: 업데이트 팝업 표시
버전 11로 업데이트: 팝업 표시 안 됨 (자동 초기화)
```

---

### 2 상황별 설정

| 상황 | is_force_update | 설명 |
|------|----------------|------|
| 일반 기능 추가 | false | "나중에" 가능, 사용자 친화적 |
| 버그 수정 | false | "나중에" 가능 |
| 중요 버그 | true | "업데이트"만 가능 |
| 보안 이슈 | true | 즉시 강제 업데이트 |

---

### 3 긴급 상황 대응

#### 상황: 배포된 버전에 심각한 버그 발견

```sql
-- 즉시 강제 업데이트로 전환
UPDATE update_policy
SET is_force_update = true,
    release_notes = '• 긴급 버그 수정\n• 필수 업데이트'
WHERE app_id = 'com.sweetapps.pocketchord';
```

**효과**: 
- ✅ 즉시 적용 (앱 재시작 시)
- ✅ "나중에"로 숨긴 사용자도 강제로 변경

---

## 4 문제 해결

### 1 팝업이 표시되지 않음

**확인**:
```sql
SELECT app_id, is_active, target_version_code, is_force_update
FROM update_policy
WHERE app_id = 'com.sweetapps.pocketchord';
```

**체크리스트**:
1. ✅ `is_active = true`인가?
2. ✅ `target_version_code`가 현재 앱 버전보다 높은가?
3. ✅ 앱을 완전히 재시작했는가?
4. ✅ SharedPreferences에 이미 추적되어 있지 않은가?

**해결**:
```sql
-- 활성화 및 높은 버전으로 설정
UPDATE update_policy
SET is_active = true,
    target_version_code = 100
WHERE app_id = 'com.sweetapps.pocketchord';
```

```bash
# SharedPreferences 초기화
adb -s emulator-5554 shell pm clear com.sweetapps.pocketchord.debug
```

---

### 2 "나중에" 후 영구히 숨김

**원인**: SharedPreferences에 추적됨 (정상 동작)

**해결** (재표시가 필요한 경우):
- 앱 데이터 삭제
- 또는 `target_version_code` 증가

---

### 3 화면 전환 시 팝업이 다시 표시됨 (✅ 해결됨)

**이전 현상** (수정됨): 
- "나중에" 클릭 후 60초(또는 24시간) 경과
- 앱을 재시작하지 않고 다른 화면(코드 등) → 홈 화면 복귀
- 팝업이 다시 표시됨

**원인**: 
- `LaunchedEffect(Unit)` 사용
- HomeScreen이 **재구성(recomposition)될 때마다** 실행됨

**해결 방법** (✅ 적용 완료):
```kotlin
// 팝업 체크 완료 플래그 추가 (화면 전환 시에도 유지)
val hasCheckedPopups = rememberSaveable { mutableStateOf(false) }

LaunchedEffect(Unit) {
    if (hasCheckedPopups.value) {
        return@LaunchedEffect  // 이미 체크했으면 건너뜀
    }
    
    // ... 팝업 체크 로직 ...
    
    finally {
        hasCheckedPopups.value = true  // 체크 완료 표시
    }
}
```

**핵심 포인트**:
- `rememberSaveable` 사용: 화면 전환(코드 → 홈) 시에도 플래그 유지
- `LaunchedEffect(Unit)`: 컴포저블 생성 시 한 번만 실행
- 플래그 체크: 이미 체크했으면 즉시 return

**수정 후 동작**:
- ✅ 앱 시작 시: 팝업 체크 실행
- ✅ 화면 전환 (코드 → 홈): 팝업 체크 **건너뜀** (재표시 안 됨)
- ✅ 앱 재시작 시: 팝업 체크 다시 실행 (정상 재표시)

**테스트 방법**:
1. 선택적 업데이트 팝업 표시 → "나중에" 클릭
2. 60초 대기
3. 코드 화면 → 홈 화면 복귀 → 팝업 **표시 안 됨** ✅
4. 앱 완전 종료 → 재시작 → 팝업 **표시됨** ✅

---

## 5 체크리스트

### 1 테스트 완료 여부

| 시나리오 | 결과 | 비고 |
|----------|------|------|
| 강제 업데이트 | ☐ PASS / ☐ FAIL | |
| 선택적 업데이트 | ☐ PASS / ☐ FAIL | |
| SharedPreferences 초기화 | ☐ PASS / ☐ FAIL | |
| 정리 (비활성화) | ☐ PASS / ☐ FAIL | |

---

**문서 버전**: v3.0.0 (UPDATE-POLICY-GUIDE 통합)  
**마지막 수정**: 2025-11-10
