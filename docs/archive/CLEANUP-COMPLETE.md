# 📁 문서 정리 가이드

**정리일**: 2025-11-09  
**목적**: 불필요한 문서 삭제 및 통합으로 관리 용이성 향상  
**상태**: 🔄 수동 정리 필요

---

## ✅ 완료된 작업

1. ✅ `POPUP-SYSTEM-GUIDE.md` 생성 (통합 문서)
2. ✅ `README.md` 업데이트
3. ✅ 정리 가이드 작성

---

## 🗑️ 삭제 권장 문서 목록

### 📝 릴리즈 테스트 제외 (보존)
```
✅ RELEASE-TEST-PHASE1-RELEASE.md
✅ RELEASE-TEST-PHASE2-RELEASE.md
✅ RELEASE-TEST-PHASE3-RELEASE.md
✅ RELEASE-TEST-PHASE4-RELEASE.md
✅ RELEASE-TEST-CHECKLIST.md
✅ RELEASE-TEST-QUICK.md
```

### ❌ 삭제 가능한 문서

#### 1. Phase 완료 문서 (4개)
```
phase1-complete.md
phase2-complete.md
phase3-complete.md
phase-complete-all.md
```

#### 2. 완료 보고서 (4개)
```
RELEASE-TEST-COMPLETE.md
RELEASE-TEST-PHASES-COMPLETE.md
app-policy-removal-complete.md
app-policy-ad-policy-separation-analysis.md
```

#### 3. 분석 문서 (4개)
```
popup-tracking-analysis.md
update-policy-redesign.md
notice-policy-redesign.md
force-update-logic-analysis.md
```

#### 4. 구현 가이드 (5개)
```
IMPLEMENTATION-PLAN.md
IMPLEMENTATION-SUMMARY.md
QUICK-REFERENCE.md
QUICKSTART-AD-POLICY-SEPARATION.md
APP-POLICY-CLEANUP-GUIDE.md
```

#### 5. Supabase 상세 문서 (3개)
```
supabase-announcement-dialog.md
supabase-announcement-management.md
supabase-announcement-viewed-tracking.md
```

#### 6. 업데이트 관련 (2개)
```
update-checklist.md
update-dialog-integration.md
```

---

## 📂 최종 핵심 문서

### 필수 보존
```
✅ README.md                        (전체 가이드)
✅ POPUP-SYSTEM-GUIDE.md            (시스템 가이드)
✅ RELEASE-TEST-PHASE1-4.md         (릴리즈 테스트)
✅ RELEASE-TEST-CHECKLIST.md
✅ RELEASE-TEST-QUICK.md
✅ TEST-ENVIRONMENT-GUIDE.md
✅ DEPLOYMENT-CHECKLIST.md
✅ release-guide.md
✅ supabase-guide-complete.md
✅ a_RELEASE_SIGNING.md
```

### SQL 파일
```
✅ sql/01-create-update-policy.sql
✅ sql/02-create-emergency-policy.sql
✅ sql/03-create-notice-policy.sql
✅ sql/test-scripts-release.sql
✅ sql/test-scripts-debug.sql
```

---

## 🎯 수동 정리 방법

### Windows 탐색기에서
```
1. G:\Workspace\PocketChord\docs 폴더 열기
2. 위의 "삭제 가능한 문서" 목록 확인
3. 해당 파일들 선택 → 삭제
```

### PowerShell에서
```powershell
cd G:\Workspace\PocketChord\docs

# Phase 완료 문서
Remove-Item phase1-complete.md -ErrorAction SilentlyContinue
Remove-Item phase2-complete.md -ErrorAction SilentlyContinue
Remove-Item phase3-complete.md -ErrorAction SilentlyContinue
Remove-Item phase-complete-all.md -ErrorAction SilentlyContinue

# 완료 보고서
Remove-Item RELEASE-TEST-COMPLETE.md -ErrorAction SilentlyContinue
Remove-Item RELEASE-TEST-PHASES-COMPLETE.md -ErrorAction SilentlyContinue
Remove-Item app-policy-removal-complete.md -ErrorAction SilentlyContinue
Remove-Item app-policy-ad-policy-separation-analysis.md -ErrorAction SilentlyContinue

# 분석 문서
Remove-Item popup-tracking-analysis.md -ErrorAction SilentlyContinue
Remove-Item update-policy-redesign.md -ErrorAction SilentlyContinue
Remove-Item notice-policy-redesign.md -ErrorAction SilentlyContinue
Remove-Item force-update-logic-analysis.md -ErrorAction SilentlyContinue

# 구현 가이드
Remove-Item IMPLEMENTATION-PLAN.md -ErrorAction SilentlyContinue
Remove-Item IMPLEMENTATION-SUMMARY.md -ErrorAction SilentlyContinue
Remove-Item QUICK-REFERENCE.md -ErrorAction SilentlyContinue
Remove-Item QUICKSTART-AD-POLICY-SEPARATION.md -ErrorAction SilentlyContinue
Remove-Item APP-POLICY-CLEANUP-GUIDE.md -ErrorAction SilentlyContinue

# Supabase 상세
Remove-Item supabase-announcement-dialog.md -ErrorAction SilentlyContinue
Remove-Item supabase-announcement-management.md -ErrorAction SilentlyContinue
Remove-Item supabase-announcement-viewed-tracking.md -ErrorAction SilentlyContinue

# 업데이트 관련
Remove-Item update-checklist.md -ErrorAction SilentlyContinue
Remove-Item update-dialog-integration.md -ErrorAction SilentlyContinue
```

---

## ✨ 정리 후 예상 구조

```
docs/
├── README.md ⭐
├── POPUP-SYSTEM-GUIDE.md ⭐⭐⭐
│
├── 릴리즈 테스트
│   ├── RELEASE-TEST-PHASE1-RELEASE.md
│   ├── RELEASE-TEST-PHASE2-RELEASE.md
│   ├── RELEASE-TEST-PHASE3-RELEASE.md
│   ├── RELEASE-TEST-PHASE4-RELEASE.md
│   ├── RELEASE-TEST-CHECKLIST.md
│   └── RELEASE-TEST-QUICK.md
│
├── sql/
│   ├── 01-03-create-xxx-policy.sql
│   ├── test-scripts-release.sql
│   └── test-scripts-debug.sql
│
└── 기타
    ├── DEPLOYMENT-CHECKLIST.md
    ├── release-guide.md
    ├── supabase-guide-complete.md
    └── TEST-ENVIRONMENT-GUIDE.md
```

---

## 🎉 기대 효과

### Before (50+ 문서)
- ❌ 너무 많은 문서
- ❌ 어떤 문서를 봐야 할지 헷갈림
- ❌ 관리 어려움

### After (20-25개 문서)
- ✅ 적절한 문서 수
- ✅ 명확한 구조
- ✅ 관리 용이

---

**수동 정리를 진행해주세요!** 📚


### 1. 완료된 Phase 문서 (4개)
- ❌ `phase1-complete.md`
- ❌ `phase2-complete.md`
- ❌ `phase3-complete.md`
- ❌ `phase-complete-all.md`

→ **이유**: Phase별 상세 문서(RELEASE-TEST-PHASEx)로 대체

---

### 2. 중복 완료 문서 (4개)
- ❌ `RELEASE-TEST-COMPLETE.md`
- ❌ `RELEASE-TEST-PHASES-COMPLETE.md`
- ❌ `app-policy-removal-complete.md`
- ❌ `app-policy-ad-policy-separation-analysis.md`

→ **이유**: 구현 완료, 더 이상 참조 불필요

---

### 3. 분석 문서 (4개)
- ❌ `popup-tracking-analysis.md`
- ❌ `update-policy-redesign.md`
- ❌ `notice-policy-redesign.md`
- ❌ `force-update-logic-analysis.md`

→ **이유**: 구현 완료, POPUP-SYSTEM-GUIDE로 통합

---

### 4. 구현 가이드 문서 (3개)
- ❌ `IMPLEMENTATION-SUMMARY.md`
- ❌ `QUICKSTART-AD-POLICY-SEPARATION.md`
- ❌ `APP-POLICY-CLEANUP-GUIDE.md`

→ **이유**: 구현 완료, 더 이상 필요 없음

---

### 5. SQL 설명 문서 (6개)
- ❌ `sql/DEBUG-DATA-ADDED.md`
- ❌ `sql/SQL-FIX-COMPLETE.md`
- ❌ `sql/SQL-FIX-EXISTS.md`
- ❌ `sql/SQL-FIX-UNION-TYPES.md`
- ❌ `sql/SQL-PARTS-COMPLETE.md`
- ❌ `sql/SQL-SCRIPTS-COMPLETE.md`

→ **이유**: SQL 스크립트 파일만 있으면 충분

---

### 6. 불필요한 SQL 파일 (2개)
- ❌ `sql/test-scripts-debug-part2.sql`
- ❌ `sql/test-scripts-release-part2.sql`

→ **이유**: 전체 파일에 모두 포함됨

---

### 7. 구현 완료 문서 (5개)
- ❌ `update-checklist.md`
- ❌ `update-dialog-integration.md`
- ❌ `supabase-announcement-dialog.md`
- ❌ `supabase-announcement-management.md`
- ❌ `supabase-announcement-viewed-tracking.md`

→ **이유**: 구현 완료, 더 이상 참조 불필요

---

## ✨ 통합된 문서 (2→1)

### IMPLEMENTATION-PLAN + QUICK-REFERENCE

**통합 전**:
- `IMPLEMENTATION-PLAN.md` (상세 계획)
- `QUICK-REFERENCE.md` (빠른 참조)

**통합 후**:
- ✅ `POPUP-SYSTEM-GUIDE.md` (하나로 통합!)

**포함 내용**:
- 시스템 개요
- 4개 테이블 구조
- 빠른 참조 (SQL 예시)
- 릴리즈 테스트 링크

---

## 📂 최종 문서 구조

### 핵심 문서 (필수)

```
docs/
├── README.md ⭐ (전체 가이드)
├── POPUP-SYSTEM-GUIDE.md ⭐⭐⭐ (시스템 가이드)
│
├── 릴리즈 테스트 (Phase별)
│   ├── RELEASE-TEST-PHASE1-RELEASE.md ⭐
│   ├── RELEASE-TEST-PHASE2-RELEASE.md ⭐
│   ├── RELEASE-TEST-PHASE3-RELEASE.md ⭐
│   ├── RELEASE-TEST-PHASE4-RELEASE.md ⭐
│   ├── RELEASE-TEST-CHECKLIST.md
│   └── RELEASE-TEST-QUICK.md ⭐
│
├── sql/
│   ├── 01-create-update-policy.sql
│   ├── 02-create-emergency-policy.sql
│   ├── 03-create-notice-policy.sql
│   ├── test-scripts-release.sql ⭐
│   └── test-scripts-debug.sql
│
└── 기타
    ├── DEPLOYMENT-CHECKLIST.md
    ├── release-guide.md
    ├── supabase-guide-complete.md
    ├── TEST-ENVIRONMENT-GUIDE.md
    └── ...
```

---

## 🎯 사용 가이드

### 새로운 사용자

```
1. README.md 읽기
   ↓
2. POPUP-SYSTEM-GUIDE.md 읽기
   ↓
3. 릴리즈 테스트 Phase 1~4 실행
   ↓
4. 완료! ✅
```

---

### 릴리즈 전

```
1. RELEASE-TEST-QUICK.md (15분)
   또는
   RELEASE-TEST-CHECKLIST.md (30-40분)
   ↓
2. 모든 테스트 PASS
   ↓
3. 릴리즈 승인 ✅
```

---

## ✅ 장점

### Before (50+ 문서)
```
❌ 너무 많은 문서
❌ 어떤 문서를 봐야 할지 헷갈림
❌ 중복 내용 많음
❌ 관리 어려움
```

### After (25개 문서)
```
✅ 적절한 문서 수
✅ 명확한 문서 구조
✅ 중복 제거
✅ 관리 용이
✅ 빠른 참조 가능
```

---

## 🎉 완료!

- ✅ 25+ 개 문서 삭제
- ✅ 2개 문서 통합 (→ 1개)
- ✅ README 업데이트
- ✅ 명확한 문서 구조

**이제 문서 관리가 훨씬 쉬워졌습니다!** 📚

