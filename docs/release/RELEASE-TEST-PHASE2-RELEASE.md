# 릴리즈 테스트 - Phase 2 (Update Policy)

**버전**: v3.0.0  
**최종 업데이트**: 2025-11-10  
**app_id**: `com.sweetapps.pocketchord` (프로덕션)  
**포함 내용**: Update Policy 가이드 + 테스트

---

## 📋 목차

1. [Update Policy 개념](#1-update-policy-개념)
2. [Phase 2 테스트](#2-phase-2-테스트)
3. [운영 가이드](#3-운영-가이드)
4. [문제 해결](#4-문제-해결)
5. [체크리스트](#5-체크리스트)

---

## 1. Update Policy 개념

### 1.1 target_version_code란?

**Play Store에 올릴 다음 버전의 versionCode입니다.**

```
현재 앱: versionCode = 10
다음 업데이트: versionCode = 11

→ target_version_code = 11 로 설정
→ 버전 10 사용자에게 "업데이트하세요" 팝업 표시
```

### 1.2 업데이트 타입

| 타입 | 설정 | 사용자 경험 | 사용 시기 |
|------|------|-----------|----------|
| **선택적 업데이트** | `is_force_update = false` | "나중에" 버튼 있음 | 일반 업데이트 |
| **강제 업데이트** | `is_force_update = true` | "나중에" 버튼 없음, 뒤로가기 차단 | 중요 버그, 보안 이슈 |

### 1.3 시간 기반 재표시 (Phase 2.5)

**"나중에" 클릭 후 일정 시간이 지나면 다시 팝업 표시**

| 필드 | 설명 | 운영값 |
|------|------|--------|
| `reshow_interval_hours` | 재표시 간격 (시간) | 24 (권장) |
| `max_later_count` | 최대 "나중에" 횟수 | 3 (권장) |
| `reshow_interval_seconds` | 테스트용 (초) | NULL (운영 필수) |
| `reshow_interval_minutes` | 테스트용 (분) | NULL (운영 필수) |

**동작**:
```
1~3회차: "나중에" 클릭 → 24시간 후 재표시
4회차: laterCount >= 3 → 강제 전환 (나중에 버튼 숨김)
```

**⚠️ 중요**: Phase 2.5는 별도 문서 참조:
- [RELEASE-TEST-PHASE2.5-SETUP.md](RELEASE-TEST-PHASE2.5-SETUP.md)
- [RELEASE-TEST-PHASE2.5-SCENARIOS.md](RELEASE-TEST-PHASE2.5-SCENARIOS.md)

---

## 2. Phase 2 테스트

### 2.1 목표

`update_policy` 동작 검증:
- 강제 업데이트 (뒤로가기 차단)
- 선택적 업데이트 ("나중에" 버튼)
- SharedPreferences 추적

**소요 시간**: 약 15분

---

### 2.2 시나리오 1: 강제 업데이트

#### SQL
```sql
-- 강제 업데이트 활성화
UPDATE update_policy
SET is_active = true,
    target_version_code = 100,  -- 현재보다 높게
    is_force_update = true,
    release_notes = '• 중요 보안 패치\n• 필수 업데이트',
    download_url = 'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord'
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 검증
- [ ] 앱 실행 → 업데이트 팝업 표시
- [ ] 제목: "새 버전 사용 가능"
- [ ] 배지: "필수" 표시
- [ ] **"나중에" 버튼 없음** ⭐
- [ ] release_notes 내용 표시
- [ ] 뒤로가기 **차단됨** (테스트 필요)
- [ ] "업데이트" 버튼 클릭 → Play Store 이동

---

### 2.3 시나리오 2: 선택적 업데이트

#### SQL
```sql
-- 선택적 업데이트 활성화
UPDATE update_policy
SET is_active = true,
    target_version_code = 100,
    is_force_update = false,
    release_notes = '• 다크 모드 추가\n• 성능 개선',
    download_url = 'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord'
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 검증
- [ ] 앱 실행 → 업데이트 팝업 표시
- [ ] **"나중에" 버튼 있음** ⭐
- [ ] "업데이트" 버튼 있음
- [ ] "나중에" 클릭 → 팝업 닫힘
- [ ] 앱 재실행 → 팝업 **표시 안 됨** (SharedPreferences 추적)

#### SharedPreferences 확인
```bash
# 추적 데이터 확인 (선택사항)
adb shell run-as com.sweetapps.pocketchord cat /data/data/com.sweetapps.pocketchord/shared_prefs/update_policy_prefs.xml
```

---

### 2.4 시나리오 3: SharedPreferences 초기화

#### 목적
"나중에"로 숨긴 팝업을 다시 표시하려면 추적 데이터 삭제 필요

#### 방법 1: 앱 데이터 전체 삭제 (권장)
```bash
adb shell pm clear com.sweetapps.pocketchord
```

#### 방법 2: SharedPreferences만 삭제
```bash
adb shell run-as com.sweetapps.pocketchord rm -r /data/data/com.sweetapps.pocketchord/shared_prefs/
```

#### 검증
- [ ] 초기화 실행
- [ ] 앱 재실행
- [ ] 팝업 **다시 표시됨** ✅

---

### 2.5 정리: 비활성화

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

## 3. 운영 가이드

### 3.1 새 버전 출시 절차

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

### 3.2 상황별 설정

| 상황 | is_force_update | 설명 |
|------|----------------|------|
| 일반 기능 추가 | false | "나중에" 가능, 사용자 친화적 |
| 버그 수정 | false | "나중에" 가능 |
| 중요 버그 | true | "업데이트"만 가능 |
| 보안 이슈 | true | 즉시 강제 업데이트 |

---

### 3.3 긴급 상황 대응

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

## 4. 문제 해결

### 4.1 팝업이 표시되지 않음

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
adb shell pm clear com.sweetapps.pocketchord
```

---

### 4.2 "나중에" 후 영구히 숨김

**원인**: SharedPreferences에 추적됨 (정상 동작)

**해결** (재표시가 필요한 경우):
- 앱 데이터 삭제
- 또는 `target_version_code` 증가

---

## 5. 체크리스트

### 5.1 테스트 완료 여부

| 시나리오 | 결과 | 비고 |
|----------|------|------|
| 강제 업데이트 | ⬜ PASS / ⬜ FAIL | |
| 선택적 업데이트 | ⬜ PASS / ⬜ FAIL | |
| SharedPreferences 초기화 | ⬜ PASS / ⬜ FAIL | |
| 정리 (비활성화) | ⬜ PASS / ⬜ FAIL | |

### 5.2 발견된 이슈

```
1. _____________________________________________
2. _____________________________________________
3. _____________________________________________
```

---

## 📚 관련 문서

- **[RELEASE-TEST-CHECKLIST.md](RELEASE-TEST-CHECKLIST.md)** - 전체 릴리즈 테스트
- **[RELEASE-TEST-PHASE2.5-SETUP.md](RELEASE-TEST-PHASE2.5-SETUP.md)** - Phase 2.5: 시간 기반 재표시
- **[RELEASE-TEST-PHASE2.5-SCENARIOS.md](RELEASE-TEST-PHASE2.5-SCENARIOS.md)** - Phase 2.5: 시나리오 테스트
- **[RELEASE-TEST-PHASE1-RELEASE.md](RELEASE-TEST-PHASE1-RELEASE.md)** - Phase 1: Emergency (팝업 시스템 개요)

---

**문서 버전**: v3.0.0 (UPDATE-POLICY-GUIDE 통합)  
**마지막 수정**: 2025-11-10
