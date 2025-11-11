# 🚀 PocketChord 팝업 시스템 가이드

**버전**: v2.6.0  
**최종 업데이트**: 2025-11-09 09:00 KST  
**상태**: ✅ 구현 완료 (Phase 2.5 설계 완료 - 간소화)

---

## 📝 버전 히스토리

### v2.6.0 (2025-11-09 09:00) 🎯 최종 구현 방식 확정
- ✅ **3회 "나중에" 후 강제 전환으로 간소화**
  - 경고 메시지 제거 (구현 복잡도 감소)
  - 3회까지 선택권 제공, 4회째 강제 전환
  - "나중에" 버튼 숨김 + 뒤로가기 차단
- ✅ 모든 문서에 간소화된 구현 방식 반영
- ✅ `max_dismiss_count = 3` 고정

### v2.5.0 (2025-11-09 08:50) 💡 강제 전환 메커니즘 설명
- ✅ **강제 전환 메커니즘 상세 문서 작성** (UPDATE-POLICY-FORCE-CONVERSION-EXPLAINED.md)
  - "Supabase는 변경 안 하는데 어떻게 강제로 전환?" 질문에 대한 답변
  - 클라이언트 측 동적 전환 메커니즘 설명
  - 3가지 구현 방법 비교 (버튼 숨기기, 경고 표시, 완전 전환)
  - 실제 시나리오 및 코드 예시
- ✅ 시간 기반 전략 문서에 강제 전환 로직 추가

### v2.4.0 (2025-11-09 08:40) 📋 Phase 2.5 설계
- ✅ **시간 기반 재표시 전략 문서 작성** (UPDATE-POLICY-TIME-BASED-STRATEGY.md)
  - 5가지 상세 시나리오 제공
  - 업계 표준 비교 분석
  - 구현 방안 및 효과 예측
  - 권장 설정 가이드
- ✅ 현재 선택적 업데이트의 문제점 명시 ("나중에" 1회로 영구 숨김)
- ✅ Phase 2.5 개선 계획 추가

### v2.3.0 (2025-11-09 08:30) 🔍 동작 명확화
- ✅ **선택적 업데이트의 "업데이트" 버튼 동작 명확화**
  - "업데이트" 버튼 클릭 시 `dismissedVersionCode`를 저장하지 **않음**
  - Play Store에서 업데이트하지 않으면 앱 재시작 시 팝업이 **다시 표시됨**
  - 실제로 업데이트하면 VERSION_CODE가 증가하여 팝업이 자동으로 사라짐
- ✅ Logcat에 `showUpdateDialog` 로그 추가

### v2.2.0 (2025-11-09 08:15) 🔥 Phase 2 완료
- ✅ **update_policy에서 message 필드 제거** (release_notes로 통합)
- ✅ **download_url NOT NULL 및 기본값 설정** (`https://play.google.com/`)
- ✅ 현재 앱 버전코드 반영 (VERSION_CODE = 3)
- ✅ SharedPreferences 전체 삭제 방법 추가 (`rm -r`)
- ✅ 선택적 업데이트 테스트 가이드 개선
- ✅ Phase 2 릴리즈 테스트 문서 v2.1.0 완성

### v2.1.0 (2025-11-09 06:35)
- ✅ emergency_policy에 button_text 필드 추가
- ✅ button_text NOT NULL 제약 조건 (기본값: "확인")
- ✅ Supabase에서 버튼 텍스트 설정 가능

### v2.0.0 (2025-11-09)
- ✅ emergency_policy에서 new_app_id 필드 제거 (redirect_url만 사용)
- ✅ update_policy 사용 가이드 작성 (UPDATE-POLICY-USAGE-GUIDE.md)
- ✅ 테스트용 숫자(999, 1000) 가이드 제거
- ✅ 실제 운영 방법만 문서화

### v1.0.0 (2025-11-08)
- ✅ 4개 테이블 분리 완료 (emergency, update, notice, ad)
- ✅ app_policy 테이블 제거
- ✅ 우선순위 로직 구현
- ✅ Phase별 릴리즈 테스트 문서 작성

---

## 📋 목차

1. [시스템 개요](#시스템-개요)
2. [4개 테이블 구조](#4개-테이블-구조)
3. [빠른 참조](#빠른-참조)
4. [릴리즈 테스트](#릴리즈-테스트)

---

## 시스템 개요

### ✅ 최종 구조

```
4개 테이블로 책임 분리 완료!

1. emergency_policy  (긴급 상황)
2. update_policy     (업데이트)
3. notice_policy     (공지사항)
4. ad_policy         (광고 설정)
```

### 🎯 우선순위

```
1순위: emergency_policy (최우선!)
   ↓ 없으면
2순위: update_policy
   ↓ 없으면
3순위: notice_policy
```

---

## 4개 테이블 구조

### 1️⃣ emergency_policy

**목적**: 긴급 상황 (앱 차단, 서비스 종료 등)

```sql
CREATE TABLE emergency_policy (
    id BIGINT PRIMARY KEY,
    app_id TEXT NOT NULL,
    is_active BOOLEAN DEFAULT FALSE,
    content TEXT NOT NULL,
    redirect_url TEXT,
    button_text TEXT NOT NULL DEFAULT '확인',  -- ⭐ 버튼 텍스트 (필수)
    is_dismissible BOOLEAN DEFAULT TRUE  -- ⭐ Google Play 준수
);
```

**핵심 필드**:
- `is_dismissible`: X 버튼 제어 (Google Play 정책 준수)
- `redirect_url`: Play Store 링크 또는 웹 페이지
- `button_text`: 버튼 텍스트 (필수, 기본값: "확인")
- 추적 없음 (매번 표시)

**사용 예시**:
```sql
-- 긴급 상황 활성화
UPDATE emergency_policy 
SET is_active = true,
    is_dismissible = true,
    content = '⚠️ 이 앱은 더 이상 지원되지 않습니다.',
    redirect_url = 'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord.v2',
    button_text = '새 앱 다운로드'
WHERE app_id = 'com.sweetapps.pocketchord';

-- 단순 공지 (redirect_url 없음)
UPDATE emergency_policy 
SET is_active = true,
    is_dismissible = true,
    content = '✅ 시스템 점검이 완료되었습니다.',
    redirect_url = NULL,
    button_text = '확인'  -- 기본값
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

### 2️⃣ update_policy

**목적**: 앱 업데이트 (강제/선택적)

```sql
CREATE TABLE update_policy (
    id BIGINT PRIMARY KEY,
    app_id TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    target_version_code INT NOT NULL,      -- ⭐ 단일 필드
    is_force_update BOOLEAN DEFAULT FALSE, -- ⭐ 강제/선택
    release_notes TEXT,                     -- 업데이트 내용
    download_url TEXT NOT NULL DEFAULT 'https://play.google.com/'  -- ⭐ 필수
);
```

**핵심 필드**:
- `target_version_code`: 목표 버전 (단일 필드로 단순화!)
- `is_force_update`: true=강제, false=선택적
- `release_notes`: 업데이트 메시지 (message 제거, release_notes로 통합)
- `download_url`: Play Store 링크 (필수, 기본값: `https://play.google.com/`)

**사용 예시**:
```sql
-- 강제 업데이트 (현재 버전 3 → 4로 업데이트)
UPDATE update_policy 
SET is_active = true,
    target_version_code = 4,  -- 현재 3보다 높게
    is_force_update = true,
    release_notes = '• 중요 보안 패치\n• 필수 기능 추가',
    download_url = 'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord'
WHERE app_id = 'com.sweetapps.pocketchord';

-- 선택적 업데이트 (기본값 사용)
UPDATE update_policy 
SET is_active = true,
    target_version_code = 4,
    is_force_update = false,
    release_notes = '• 다크 모드 추가\n• 성능 개선'
    -- download_url 생략 (기본값 사용)
WHERE app_id = 'com.sweetapps.pocketchord';

-- 업데이트 비활성화 (현재 버전과 같게)
UPDATE update_policy 
SET target_version_code = 3  -- 현재 버전과 같게 → 팝업 안 뜸
WHERE app_id = 'com.sweetapps.pocketchord';
```

**💡 중요**:
- `target_version_code`는 **현재 앱 버전보다 높아야** 업데이트 팝업이 표시됨
- **선택적 업데이트 (현재 구현)**:
  - "나중에" 버튼이 있으며, 클릭 시 `dismissedVersionCode` 저장
  - 한 번 "나중에"를 누르면 같은 버전의 팝업이 다시 표시되지 않음
  - **"업데이트" 버튼 클릭 시**: `dismissedVersionCode`를 저장하지 **않음**
    - → Play Store로 이동하고 업데이트하지 않으면, 앱 재시작 시 팝업이 **다시 표시됨** ✅
    - → 실제로 업데이트하면 `VERSION_CODE`가 증가하여 팝업이 표시되지 않음
- **강제 업데이트**:
  - X 버튼이 없으며, 뒤로가기가 차단됨
  - `dismissedVersionCode` 추적하지 않음 (매번 표시)

**⚠️ 개선 필요 (Phase 2.5 예정)**:
- 현재 "나중에" 1회 클릭 시 다음 버전까지 영구히 숨김 → **너무 느슨함**
- **시간 기반 재표시 전략** 도입 예정 (24시간 후 재표시)
- 자세한 내용: **[UPDATE-POLICY-TIME-BASED-STRATEGY.md](UPDATE-POLICY-TIME-BASED-STRATEGY.md)** 참조

---

### 3️⃣ notice_policy

**목적**: 일반 공지사항 (이벤트, 신규 기능 등)

```sql
CREATE TABLE notice_policy (
    id BIGINT PRIMARY KEY,
    app_id TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    title TEXT,
    content TEXT NOT NULL,
    notice_version INT NOT NULL DEFAULT 1,  -- ⭐ 버전 관리
    image_url TEXT,
    action_url TEXT
);
```

**핵심 필드**:
- `notice_version`: 버전 관리로 명시적 제어!

**사용 예시**:
```sql
-- 오타 수정 (버전 유지 → 재표시 안 됨)
UPDATE notice_policy 
SET content = '수정된 내용'
WHERE app_id = 'com.sweetapps.pocketchord';
-- notice_version은 그대로!

-- 새 공지 (버전 증가 → 모두에게 재표시)
UPDATE notice_policy 
SET title = '2월 이벤트',
    content = '새 이벤트!',
    notice_version = 2  -- 증가!
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

### 4️⃣ ad_policy

**목적**: 광고 설정

```sql
CREATE TABLE ad_policy (
    id BIGINT PRIMARY KEY,
    app_id TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    ad_app_open_enabled BOOLEAN DEFAULT TRUE,
    ad_interstitial_enabled BOOLEAN DEFAULT TRUE,
    ad_banner_enabled BOOLEAN DEFAULT TRUE,
    ad_interstitial_max_per_hour INT DEFAULT 2,
    ad_interstitial_max_per_day INT DEFAULT 15
);
```

---

## 빠른 참조

### 🔥 긴급 상황 발동

```sql
UPDATE emergency_policy 
SET is_active = true,
    is_dismissible = true,  -- X 버튼 허용
    content = '긴급 메시지'
WHERE app_id = 'your.app.id';
```

### 🔄 강제 업데이트

```sql
UPDATE update_policy 
SET is_active = true,
    target_version_code = 4,  -- 현재 버전(3)보다 높게
    is_force_update = true,
    release_notes = '• 중요 보안 패치\n• 필수 업데이트',
    download_url = 'https://play.google.com/store/apps/details?id=your.app.id'
WHERE app_id = 'your.app.id';
```

### 🔔 선택적 업데이트

```sql
UPDATE update_policy 
SET is_active = true,
    target_version_code = 4,  -- 현재 버전(3)보다 높게
    is_force_update = false,
    release_notes = '• 새로운 기능 추가\n• 성능 개선'
    -- download_url 생략 시 기본값 사용: https://play.google.com/
WHERE app_id = 'your.app.id';
```

### 📢 새 공지 (버전 증가)

```sql
UPDATE notice_policy 
SET title = '신규 공지',
    content = '내용',
    notice_version = notice_version + 1
WHERE app_id = 'your.app.id';
```

### 📺 광고 끄기

```sql
UPDATE ad_policy 
SET ad_interstitial_enabled = false
WHERE app_id = 'your.app.id';
```

---

## 릴리즈 테스트

### 📋 테스트 문서

**Phase별 상세 가이드**:
1. `release/RELEASE-TEST-PHASE1-RELEASE.md` - Emergency 테스트
2. `release/RELEASE-TEST-PHASE2-RELEASE.md` - Update 테스트
3. `release/RELEASE-TEST-PHASE3-RELEASE.md` - Notice 테스트
4. `release/RELEASE-TEST-PHASE4-RELEASE.md` - 우선순위 + 최종 확인
5. `release/RELEASE-TEST-PHASE5-RELEASE.md` - Ad Policy 테스트

**빠른 체크리스트**:
- `RELEASE-TEST-QUICK.md` (15분)
- `RELEASE-TEST-CHECKLIST.md` (전체 30-40분)

### 🎯 테스트 프로세스

```
1. 초기 상태 확인 (스냅샷)
   ↓
2. Phase 1: Emergency 테스트
   ↓
3. Phase 2: Update 테스트
   ↓
4. Phase 3: Notice 테스트
   ↓
5. Phase 4: 우선순위 테스트
   ↓
6. Phase 5: Ad Policy 테스트
   ↓
7. 최종 확인 및 복구
   ↓
8. 릴리즈 승인 ✅
```

---

## 🎉 완료!

모든 팝업 시스템이 구현되고 테스트 준비가 완료되었습니다!

**다음 단계**: 릴리즈 테스트 문서를 따라 검증하세요!

---

**문서 버전**: v2.6.0  
**마지막 수정**: 2025-11-09 09:00 KST
