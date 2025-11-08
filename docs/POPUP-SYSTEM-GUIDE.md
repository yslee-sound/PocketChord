# 🚀 PocketChord 팝업 시스템 가이드

**최종 업데이트**: 2025-11-09  
**상태**: ✅ 구현 완료

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
    is_dismissible BOOLEAN DEFAULT TRUE  -- ⭐ Google Play 준수
);
```

**핵심 필드**:
- `is_dismissible`: X 버튼 제어 (Google Play 정책 준수)
- `redirect_url`: Play Store 링크 또는 웹 페이지
- 추적 없음 (매번 표시)

**사용 예시**:
```sql
-- 긴급 상황 활성화
UPDATE emergency_policy 
SET is_active = true,
    is_dismissible = true,
    content = '⚠️ 이 앱은 더 이상 지원되지 않습니다.',
    redirect_url = 'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord.v2'
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
    message TEXT,
    release_notes TEXT
);
```

**핵심 필드**:
- `target_version_code`: 목표 버전 (단일 필드로 단순화!)
- `is_force_update`: true=강제, false=선택적

**사용 예시**:
```sql
-- 강제 업데이트
UPDATE update_policy 
SET target_version_code = 15,
    is_force_update = true
WHERE app_id = 'com.sweetapps.pocketchord';

-- 선택적 업데이트
UPDATE update_policy 
SET target_version_code = 15,
    is_force_update = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

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
SET target_version_code = 15,
    is_force_update = true
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
1. `RELEASE-TEST-PHASE1-RELEASE.md` - Emergency 테스트
2. `RELEASE-TEST-PHASE2-RELEASE.md` - Update 테스트
3. `RELEASE-TEST-PHASE3-RELEASE.md` - Notice 테스트
4. `RELEASE-TEST-PHASE4-RELEASE.md` - 우선순위 + 최종 확인

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
6. 최종 확인 및 복구
   ↓
7. 릴리즈 승인 ✅
```

---

## 🎉 완료!

모든 팝업 시스템이 구현되고 테스트 준비가 완료되었습니다!

**다음 단계**: 릴리즈 테스트 문서를 따라 검증하세요!

