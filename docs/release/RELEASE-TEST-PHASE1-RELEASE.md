# 릴리즈 테스트 SQL 스크립트 - Phase 1 (Emergency Policy)

**버전**: v2.0.0  
**최종 업데이트**: 2025-11-10  
**app_id**: `com.sweetapps.pocketchord` (프로덕션)  
**포함 내용**: Emergency 테스트 + 팝업 시스템 개요

---

## 📋 목차

1. [팝업 시스템 개요](#1-팝업-시스템-개요)
2. [Phase 1 테스트](#2-phase-1-테스트)
3. [체크리스트](#3-체크리스트)

---

## 1. 팝업 시스템 개요

### 1.1 전체 구조

PocketChord는 **4개 독립 테이블**로 팝업을 관리합니다:

| 테이블 | 목적 | 우선순위 |
|--------|------|----------|
| **emergency_policy** | 긴급 상황 (앱 차단, 서비스 종료) | 1순위 (최우선) |
| **update_policy** | 앱 업데이트 (강제/선택적) | 2순위 |
| **notice_policy** | 일반 공지 (이벤트, 신규 기능) | 3순위 |
| **ad_policy** | 광고 제어 | - |

### 1.2 우선순위 로직

```
앱 실행 시:
1. emergency_policy 확인 → 있으면 표시하고 종료
2. 없으면 → update_policy 확인 → 있으면 표시하고 종료
3. 없으면 → notice_policy 확인 → 있으면 표시하고 종료
4. 없으면 → 팝업 없이 정상 진입
```

### 1.3 emergency_policy 테이블 구조

```sql
CREATE TABLE emergency_policy (
    id BIGINT PRIMARY KEY,
    app_id TEXT NOT NULL,
    is_active BOOLEAN DEFAULT FALSE,
    title TEXT,
    content TEXT NOT NULL,
    redirect_url TEXT,
    button_text TEXT NOT NULL DEFAULT '확인',
    is_dismissible BOOLEAN DEFAULT TRUE  -- X 버튼 제어
);
```

**핵심 필드**:
- `is_active`: 긴급 상황 ON/OFF
- `is_dismissible`: `true` = X 버튼 있음, `false` = X 버튼 없음 (강제)
- `redirect_url`: Play Store 링크 또는 웹 페이지
- `button_text`: 버튼 텍스트 (예: "확인", "설치", "새 앱 다운로드")

**특징**:
- ✅ **최우선 순위** (다른 모든 팝업보다 먼저 표시)
- ✅ **추적 없음** (매번 표시됨)
- ✅ **Google Play 정책 준수** (is_dismissible로 제어)

---

## 2. Phase 1 테스트

### 2.1 목표

`emergency_policy`의 동작 검증:
- 표시 우선순위 (최우선)
- X 버튼 있음/없음 동작
- 버튼 클릭 시 redirect 동작

**소요 시간**: 약 10분

---

## 2. Phase 1 테스트

### 2.1 목표
### 2.2 시나리오 1: 긴급공지 (X 버튼 있음)

#### SQL
```sql
-- 긴급공지 활성화 (닫기 가능)
UPDATE emergency_policy
SET is_active = true,
    is_dismissible = true,
    title = '긴급공지',
    content = '긴급 테스트입니다. X 버튼으로 닫을 수 있습니다.',
    button_text = '확인',
    redirect_url = NULL
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 검증
- [ ] 앱 실행 → 긴급 팝업 **즉시 표시**
- [ ] 제목: "🚨 긴급공지"
- [ ] 배지: "긴급" 표시
- [ ] **X 버튼 있음** (우측 상단)
- [ ] 내용 확인
- [ ] "확인" 버튼 있음
- [ ] X 버튼 클릭 → 팝업 닫힘
- [ ] 앱 재실행 → 긴급 팝업 **다시 표시됨** (추적 없음)

---

### 2.3 시나리오 2: 긴급공지 (X 버튼 없음 - 강제)

#### SQL
```sql
-- 긴급공지 활성화 (닫기 불가)
UPDATE emergency_policy
SET is_active = true,
    is_dismissible = false,
    title = '긴급공지',
    content = '이 앱은 더 이상 지원되지 않습니다. 새 앱을 설치하세요.',
    button_text = '새 앱 설치',
    redirect_url = 'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord'
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 검증
- [ ] 앱 실행 → 긴급 팝업 표시
- [ ] **X 버튼 없음** ⭐
- [ ] 뒤로가기 버튼 **차단됨** (테스트 필요)
- [ ] "새 앱 설치" 버튼만 있음
- [ ] 버튼 클릭 → Play Store로 이동

---

### 2.4 정리: 비활성화

#### SQL
```sql
-- 긴급공지 비활성화
UPDATE emergency_policy
SET is_active = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 검증
- [ ] 앱 재실행 → 팝업 **표시 안 됨**
- [ ] Phase 1 완료! ✅

---

## 3. 체크리스트
## 3. 체크리스트

### 3.1 테스트 완료 여부

| 시나리오 | 결과 | 비고 |
|----------|------|------|
| X 버튼 있음 | ⬜ PASS / ⬜ FAIL | |
| X 버튼 없음 (강제) | ⬜ PASS / ⬜ FAIL | |
| 정리 (비활성화) | ⬜ PASS / ⬜ FAIL | |

### 3.2 발견된 이슈

```
1. _____________________________________________
2. _____________________________________________
3. _____________________________________________
```

---

## 📚 관련 문서

- **[RELEASE-TEST-CHECKLIST.md](RELEASE-TEST-CHECKLIST.md)** - 전체 릴리즈 테스트
- **[RELEASE-TEST-PHASE2-RELEASE.md](RELEASE-TEST-PHASE2-RELEASE.md)** - Phase 2: Update Policy

---

**문서 버전**: v2.0.0 (팝업 시스템 개요 통합)  
**마지막 수정**: 2025-11-10
