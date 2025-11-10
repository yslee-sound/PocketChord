# 릴리즈 테스트 - Phase 4 (우선순위 테스트)

**버전**: v2.0.0  
**최종 업데이트**: 2025-11-10  
**app_id**: `com.sweetapps.pocketchord` (프로덕션)  
**포함 내용**: 팝업 우선순위 검증

---

## 📋 목차

1. [우선순위 개념](#1-우선순위-개념)
2. [Phase 4 테스트](#2-phase-4-테스트)
3. [체크리스트](#3-체크리스트)

---

## 1. 우선순위 개념

### 1.1 팝업 우선순위 규칙

**앱 실행 시 확인 순서**:
```
1순위: emergency_policy
   ↓ 없으면
2순위: update_policy
   ↓ 없으면
3순위: notice_policy
   ↓ 없으면
팝업 없이 정상 진입
```

### 1.2 우선순위 동작

| 상황 | 표시되는 팝업 | 설명 |
|------|--------------|------|
| Emergency + Update 둘 다 ON | **Emergency만** | Emergency가 최우선 |
| Update + Notice 둘 다 ON | **Update만** | Update가 우선 |
| 모두 OFF | **없음** | 정상 진입 |

**핵심**: 
- ✅ 한 번에 **1개만** 표시
- ✅ 더 높은 우선순위가 있으면 낮은 것은 **절대 표시 안 됨**
- ✅ Emergency를 끄면 다음 우선순위(Update)가 표시됨

---

## 2. Phase 4 테스트

### 2.1 목표

팝업 우선순위 로직 검증:
- Emergency > Update
- Update > Notice
- 한 번에 1개만 표시

**소요 시간**: 약 10분

---

### 2.2 시나리오 1: Emergency가 최우선

#### 목적
Emergency + Update 둘 다 활성화 시 Emergency만 표시

#### SQL
```sql
-- Emergency 활성화
UPDATE emergency_policy 
SET is_active = true, 
    content = '🚨 긴급 우선순위 테스트',
    button_text = '확인'
WHERE app_id = 'com.sweetapps.pocketchord';

-- Update도 활성화
UPDATE update_policy 
SET is_active = true, 
    target_version_code = 100, 
    is_force_update = true,
    release_notes = '• 업데이트 테스트'
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 검증
- [ ] 앱 실행
- [ ] **Emergency 팝업만 표시** ✅
- [ ] Update 팝업은 **표시 안 됨** (Emergency가 우선)
- [ ] Logcat 확인: "Decision: EMERGENCY"

---

### 2.3 시나리오 2: Update > Notice

#### SQL
```sql
-- Emergency 비활성화
UPDATE emergency_policy 
SET is_active = false 
WHERE app_id = 'com.sweetapps.pocketchord';

-- Update 활성화
UPDATE update_policy 
SET is_active = true, 
    target_version_code = 100, 
    is_force_update = false,
    release_notes = '• 선택적 업데이트'
WHERE app_id = 'com.sweetapps.pocketchord';

-- Notice도 활성화
UPDATE notice_policy 
SET is_active = true, 
    title = '공지 테스트',
    content = '공지 내용',
    notice_version = 251109 
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 검증
- [ ] 앱 재실행
- [ ] **Update 팝업만 표시** ✅
- [ ] Notice 팝업은 **표시 안 됨** (Update가 우선)
- [ ] Logcat 확인: "Decision: UPDATE"

---

### 2.4 시나리오 3: 모두 비활성화

#### SQL
```sql
-- 모든 팝업 비활성화 (정상 상태)
UPDATE emergency_policy 
SET is_active = false 
WHERE app_id = 'com.sweetapps.pocketchord';

UPDATE update_policy 
SET is_active = false,
    target_version_code = 1
WHERE app_id = 'com.sweetapps.pocketchord';

UPDATE notice_policy 
SET is_active = false 
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 검증
- [ ] 앱 재실행
- [ ] **팝업 없음** ✅
- [ ] 홈 화면 바로 표시
- [ ] Phase 4 완료! ✅

---

## 3. 체크리스트

### 3.1 테스트 완료 여부

| 시나리오 | 결과 | 비고 |
|----------|------|------|
| Emergency > Update | ⬜ PASS / ⬜ FAIL | |
| Update > Notice | ⬜ PASS / ⬜ FAIL | |
| 모두 비활성화 | ⬜ PASS / ⬜ FAIL | |

### 3.2 우선순위 확인

**올바른 동작**:
- [ ] ✅ Emergency 활성화 시 다른 팝업 무시
- [ ] ✅ Update 활성화 시 Notice 무시
- [ ] ✅ 한 번에 1개만 표시
- [ ] ✅ 모두 OFF 시 팝업 없음

### 3.3 발견된 이슈

```
1. _____________________________________________
2. _____________________________________________
3. _____________________________________________
```

---

## 📚 관련 문서

- **[RELEASE-TEST-CHECKLIST.md](RELEASE-TEST-CHECKLIST.md)** - 전체 릴리즈 테스트
- **[RELEASE-TEST-PHASE1-RELEASE.md](RELEASE-TEST-PHASE1-RELEASE.md)** - Phase 1: Emergency (팝업 시스템 개요)
- **[RELEASE-TEST-PHASE2-RELEASE.md](RELEASE-TEST-PHASE2-RELEASE.md)** - Phase 2: Update Policy
- **[RELEASE-TEST-PHASE3-RELEASE.md](RELEASE-TEST-PHASE3-RELEASE.md)** - Phase 3: Notice Policy

---

**문서 버전**: v2.0.0 (구조 개선)  
**마지막 수정**: 2025-11-10
