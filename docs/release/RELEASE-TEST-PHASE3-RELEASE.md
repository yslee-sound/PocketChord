# 릴리즈 테스트 - Phase 3 (Notice Policy)

**버전**: v3.0  
**최종 업데이트**: 2025-11-10  
**소요 시간**: 약 10-15분

---

## 📋 목차

1. [Notice Policy 개념](#1-notice-policy-개념)
2. [Phase 3 테스트](#2-phase-3-테스트)
3. [버전 관리 가이드](#3-버전-관리-가이드)
4. [체크리스트](#4-체크리스트)

---

## 1 Notice Policy 개념

### 1 테이블 구조

**핵심 필드**:
- `is_active`: 공지 ON/OFF
- `notice_version`: 버전 번호 (추적용)
- `title`, `content`: 공지 내용

### 2 버전 추적 메커니즘

**SharedPreferences 추적**:
```
사용자가 notice_version = 251109를 본 경우
→ SharedPreferences에 "251109" 저장
→ 동일 버전(251109) 재표시 안 됨
→ 새 버전(251110)이면 다시 표시
```

**특징**:
- ✅ 사용자별 추적
- ✅ 버전만 증가하면 자동 재표시
- ✅ X 버튼으로 닫기 가능

---

## 2 Phase 3 테스트

### 1 목표

- 공지 표시
- 버전 추적 (동일 버전 재표시 안 됨)
- 버전 증가 시 재표시


---

### 2 시나리오 1: 공지 활성화

#### SQL
```sql
-- 공지 활성화
UPDATE notice_policy
SET is_active = true,
    title = '서비스 안내',
    content = '중요 공지입니다. 앱을 최신 버전으로 유지해 주세요.',
    notice_version = 251109  -- YYMMDD 형식 권장
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 검증
- [ ] 앱 실행 → 공지 팝업 표시
- [ ] 제목: "서비스 안내"
- [ ] **X 버튼 있음**
- [ ] 내용 확인
- [ ] X 버튼 클릭 → 팝업 닫힘
- [ ] 앱 재실행 → 팝업 **표시 안 됨** (추적됨)

---

### 3 시나리오 2: 내용 수정 (버전 유지)

#### 목적
동일 버전으로 내용만 수정하면 이미 본 사용자에게 재표시되지 않음 (오타 수정용)

#### SQL
```sql
-- 오타 수정 (버전 유지)
UPDATE notice_policy 
SET content = '수정된 내용입니다. 오타를 바로잡았습니다.'
WHERE app_id = 'com.sweetapps.pocketchord';
-- notice_version은 그대로 251109
```

#### 검증
- [ ] 앱 재실행
- [ ] 팝업 **표시 안 됨** ✅ (이미 251109 버전을 봄)

**💡 Tip**: 오타 수정은 버전 변경하지 말 것!

---

### 4 시나리오 3: 새 공지 (버전 증가)

#### SQL
```sql
-- 새 공지 (버전 증가)
UPDATE notice_policy
SET title = '🎉 11월 이벤트',
    content = '11월 특별 이벤트가 시작되었습니다! 참여하세요.',
    notice_version = 251110  -- 이전(251109)보다 큰 값
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 검증
- [ ] 앱 재실행
- [ ] 팝업 **다시 표시됨** ✅ (새 버전 251110)
- [ ] 제목: "🎉 11월 이벤트"
- [ ] X 버튼 클릭
- [ ] 앱 재실행 → 팝업 **표시 안 됨** (251110 추적됨)

---

### 5 정리: 비활성화

#### SQL
```sql
-- 공지 비활성화
UPDATE notice_policy
SET is_active = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 검증
- [ ] 앱 재실행 → 팝업 **표시 안 됨**
- [ ] Phase 3 완료! ✅

---

## 3 버전 관리 가이드

### 1 버전 번호 형식 (권장)

| 형식 | 예시 | 용도 | 장점 |
|------|------|------|------|
| **YYMMDD** | 251109 | 일반적 (하루 1건) | 간단, 직관적 |
| **YYMMDDHH** | 25110915 | 하루 여러 건 | 시간까지 구분 |
| **자동 증가** | +1 방식 | 간단한 증가 | 실수 방지 |

### 2 버전 관리 규칙

**✅ DO**:
```sql
-- 날짜 기반 (권장)
UPDATE notice_policy SET notice_version = 251110;

-- 자동 증가 (안전)
UPDATE notice_policy SET notice_version = notice_version + 1;
```

**❌ DON'T**:
```sql
-- 자릿수 혼합 금지 (비교 오류 발생)
UPDATE notice_policy SET notice_version = 251109;  -- 6자리
UPDATE notice_policy SET notice_version = 2511091; -- 7자리 ❌
```

### 3 운영 시나리오

#### 시나리오 1: 신규 공지
```sql
UPDATE notice_policy
SET is_active = true,
    title = '새로운 기능 안내',
    content = '...',
    notice_version = 251111  -- 오늘 날짜
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 시나리오 2: 오타 발견
```sql
-- 버전 변경 없이 수정 (이미 본 사용자는 안 봄)
UPDATE notice_policy
SET content = '수정된 내용'
WHERE app_id = 'com.sweetapps.pocketchord';
-- notice_version 그대로
```

#### 시나리오 3: 모든 사용자에게 다시 알림
```sql
-- 버전 증가 (모든 사용자에게 재표시)
UPDATE notice_policy
SET title = '긴급 공지',
    content = '중요한 변경사항',
    notice_version = notice_version + 1
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

## 4 체크리스트

### 1 테스트 완료 여부

| 시나리오 | 결과 | 비고 |
|----------|------|------|
| 공지 활성화 | ⬜ PASS / ⬜ FAIL | |
| 내용 수정 (버전 유지) | ⬜ PASS / ⬜ FAIL | |
| 새 공지 (버전 증가) | ⬜ PASS / ⬜ FAIL | |
| 정리 (비활성화) | ⬜ PASS / ⬜ FAIL | |

### 2 발견된 이슈

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

---

**문서 버전**: v2.0.0 (구조 개선 및 가이드 통합)  
**마지막 수정**: 2025-11-10
```sql
-- 3-4. Notice 정리 (초기화) - 릴리즈 & 디버그
UPDATE notice_policy
SET is_active = false,
    notice_version = 251109
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

---

**문서 버전**: v1.6.0  
**마지막 수정**: 2025-11-09 16:39:35 KST
