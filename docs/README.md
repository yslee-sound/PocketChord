# 📚 PocketChord 문서 가이드

**프로젝트**: PocketChord  
**업데이트**: 2025-11-09  
**문서 개수**: 35개 (리팩토링 완료 + 테스트 가이드 추가)

---

## 🎯 릴리즈 전 필수 확인

### 📋 테스트 체크리스트
- **[RELEASE-TEST-QUICK.md](RELEASE-TEST-QUICK.md)** ⭐⭐⭐ - 빠른 체크리스트 (15분)
- **[RELEASE-TEST-CHECKLIST.md](RELEASE-TEST-CHECKLIST.md)** ⭐⭐ - 완전 체크리스트 (30-40분)

**테스트 항목**:
```
✅ emergency_policy (긴급 팝업)
✅ update_policy (강제/선택적 업데이트)
✅ notice_policy (공지사항 버전 관리)
✅ 우선순위 로직 검증
✅ 프로덕션 상태 확인
```

---

## 🚀 팝업 정책 시스템 (완료! ✅)

### 빠른 시작
- **[QUICK-REFERENCE.md](QUICK-REFERENCE.md)** ⭐⭐⭐ - 빠른 참조 가이드
- **[IMPLEMENTATION-PLAN.md](IMPLEMENTATION-PLAN.md)** ⭐⭐ - 단계별 구현 계획

### 완료 문서
- **[phase-complete-all.md](phase-complete-all.md)** ⭐ - 전체 완료 가이드
- **[app-policy-removal-complete.md](app-policy-removal-complete.md)** - app_policy 제거 완료

### 분석 문서
- [popup-tracking-analysis.md](popup-tracking-analysis.md) - 4가지 팝업 통합 분석
- [update-policy-redesign.md](update-policy-redesign.md) - 업데이트 정책 재설계
- [notice-policy-redesign.md](notice-policy-redesign.md) - 공지사항 정책 재설계

**최종 구조**:
```
✅ 3개 테이블 분리 완료
  ├─ update_policy: target_version_code (단일 필드)
  ├─ notice_policy: notice_version (버전 관리)
  └─ emergency_policy: is_dismissible (Google Play 준수)

✅ app_policy 제거 완료
✅ 우선순위: emergency > update > notice
```

---

## 🎯 빠른 시작

### 신규 개발자
```
1. ads-guide.md 읽기 (광고 시스템 이해)
2. supabase-guide-complete.md 읽기 (Supabase 설정)
3. QUICKSTART-AD-POLICY-SEPARATION.md (광고 정책 배포)
```

### 배포 담당자
```
1. release-guide.md (Release 빌드)
2. DEPLOYMENT-CHECKLIST.md (배포 체크리스트)
```

---

## 📋 핵심 문서

### 🎯 광고 시스템 (최신)

#### 통합 가이드
**`ads-guide.md`** ⭐ 
- App Open, Interstitial, Banner 광고 통합 가이드
- Supabase 제어 방법
- 빈도 제한
- 문제 해결

#### 광고 정책 분리
**`ad-policy-separation-implementation-complete.md`**
- 방안 1(테이블 분리) 구현 가이드
- 팝업과 광고 독립 제어

**`QUICKSTART-AD-POLICY-SEPARATION.md`**
- 5분 빠른 시작

**`IMPLEMENTATION-SUMMARY.md`**
- 전체 요약

#### SQL 스크립트
**`ad-policy-table-creation.sql`** ⭐
- ad_policy 테이블 생성 (Release + Debug)

**`ad-policy-add-debug-build.sql`**
- Debug 데이터만 추가

**`app-policy-remove-ad-columns.sql`**
- app_policy 정리 (선택사항)

**`supabase-ad-control-schema.sql`**
- app_policy 광고 컬럼 추가 (레거시 참고용)

---

### 🗄️ Supabase

#### 통합 가이드
**`supabase-guide-complete.md`** ⭐
- Supabase 설정 및 사용
- app_policy, ad_policy 테이블
- RLS 설정
- 문제 해결

#### 공지사항
**`supabase-announcement-dialog.md`**
- 공지사항 다이얼로그

**`supabase-announcement-management.md`**
- 공지사항 관리

**`supabase-announcement-viewed-tracking.md`**
- 읽음 여부 추적

#### 참고
**`SUPABASE-ID-COLUMN-GUIDE.md`**
- id 컬럼 이해하기

**`SUPABASE-TABLE-CREATION-SUCCESS.md`**
- 테이블 생성 확인 및 운영 가이드

**`force-update-logic-analysis.md`**
- 강제 업데이트 로직

---

### 🚀 배포

#### 통합 가이드
**`release-guide.md`** ⭐
- Release 빌드 방법
- Keystore 설정
- Play Store 업로드
- 문제 해결

#### 체크리스트
**`DEPLOYMENT-CHECKLIST.md`**
- 배포 전 체크리스트

---

### 🎨 코드/화음

**`chords-db-architecture.md`**
- 화음 DB 구조

**`chords-db-implementation-plan.md`**
- 구현 계획

**`chords-seed-format.md`**
- 시드 데이터 형식

**`chords-owner-decisions.md`**
- 설계 결정 사항

---

### 🔧 기타

**`app-policy-ad-policy-separation-analysis.md`**
- 광고 정책 분리 분석

**`APP-POLICY-CLEANUP-GUIDE.md`**
- app_policy 정리 가이드 (선택사항)

**`string-numbering.md`**
- 문자열 넘버링

**`app-version-with-build-type.md`**
- 앱 버전 표시

**`update-checklist.md`**
- 업데이트 체크리스트

**`update-dialog-integration.md`**
- 업데이트 다이얼로그

---

## 📁 archive 폴더

정리된 레거시 문서들:
- `archive/admob/` - AdMob 관련 구버전
- `archive/emergency-popup/` - 긴급 팝업 관련
- `archive/release-test/` - Release 테스트 관련
- `archive/debug-fixes/` - 디버그 수정 관련
- `archive/supabase-app-policy/` - app_policy 구버전
- `archive/rls/` - RLS 임시 문제 관련

**용도**: 변경 이력 추적, 참고용

---

## 🎯 시나리오별 가이드

### 처음 시작
```
1. README.md 읽기 (이 문서)
2. ads-guide.md (광고 시스템)
3. supabase-guide-complete.md (Supabase)
```

### Supabase 설정
```
1. supabase-guide-complete.md (설정 방법)
2. ad-policy-table-creation.sql (테이블 생성)
3. SUPABASE-TABLE-CREATION-SUCCESS.md (확인)
```

### Release 배포
```
1. release-guide.md (빌드 방법)
2. DEPLOYMENT-CHECKLIST.md (체크리스트)
```

### 문제 해결
```
1. 해당 가이드의 "문제 해결" 섹션 참고
2. archive 폴더에서 관련 문서 검색
```

---

## 📊 문서 구조

```
docs/
├── README.md (이 문서)
│
├── 🎯 광고 시스템
│   ├── ads-guide.md (통합 가이드) ⭐
│   ├── ad-policy-separation-implementation-complete.md
│   ├── QUICKSTART-AD-POLICY-SEPARATION.md
│   ├── IMPLEMENTATION-SUMMARY.md
│   ├── ad-policy-table-creation.sql ⭐
│   ├── ad-policy-add-debug-build.sql
│   ├── app-policy-remove-ad-columns.sql
│   └── supabase-ad-control-schema.sql (참고)
│
├── 🗄️ Supabase
│   ├── supabase-guide-complete.md (통합 가이드) ⭐
│   ├── SUPABASE-ID-COLUMN-GUIDE.md
│   ├── SUPABASE-TABLE-CREATION-SUCCESS.md
│   ├── supabase-announcement-*.md (3개)
│   └── force-update-logic-analysis.md
│
├── 🚀 배포
│   ├── release-guide.md (통합 가이드) ⭐
│   └── DEPLOYMENT-CHECKLIST.md
│
├── 🎨 코드/화음
│   ├── chords-db-architecture.md
│   ├── chords-db-implementation-plan.md
│   ├── chords-seed-format.md
│   └── chords-owner-decisions.md
│
├── 🔧 기타
│   ├── app-policy-ad-policy-separation-analysis.md
│   ├── APP-POLICY-CLEANUP-GUIDE.md
│   ├── string-numbering.md
│   ├── app-version-with-build-type.md
│   ├── update-checklist.md
│   └── update-dialog-integration.md
│
└── 📦 archive/ (레거시)
    ├── admob/
    ├── emergency-popup/
    ├── release-test/
    ├── debug-fixes/
    ├── supabase-app-policy/
    └── rls/
```

---

## 🔍 문서 찾기

### 광고 관련
→ `ads-guide.md`

### Supabase 관련
→ `supabase-guide-complete.md`

### Release 빌드
→ `release-guide.md`

### 배포 준비
→ `DEPLOYMENT-CHECKLIST.md`

### id 컬럼이 뭔가요?
→ `SUPABASE-ID-COLUMN-GUIDE.md`

### 구버전 문서
→ `archive/` 폴더

---

## ❓ FAQ

### Q: 문서가 너무 많아요!
**A**: 핵심 3개만 보세요:
1. `ads-guide.md` (광고)
2. `supabase-guide-complete.md` (Supabase)
3. `release-guide.md` (배포)

### Q: 어느 문서부터 읽어야 하나요?
**A**: 역할에 따라:
- 개발자: `ads-guide.md` → `supabase-guide-complete.md`
- 배포자: `release-guide.md` → `DEPLOYMENT-CHECKLIST.md`

### Q: archive 폴더는 뭔가요?
**A**: 정리된 레거시 문서들입니다. 삭제하지 말고 참고용으로 보관하세요.

### Q: SQL 파일은 어떤 걸 실행하나요?
**A**: `ad-policy-table-creation.sql` 하나만 실행하면 됩니다.

### Q: 통합 가이드가 뭔가요?
**A**: 여러 개별 문서를 하나로 합친 최신 가이드입니다.
- `ads-guide.md`: 광고 관련 통합
- `supabase-guide-complete.md`: Supabase 통합
- `release-guide.md`: Release 통합

---

## 📝 정리 내역 (2025-11-08)

### 변경 사항
- 87개 → 30개 문서로 축소
- 중복/레거시 문서 archive 이동
- 통합 가이드 3개 생성

### 통합된 문서
1. **광고 가이드** → `ads-guide.md`
   - app-open-ads-guide.md
   - interstitial-ads-guide.md
   - admob-setup-guide.md

2. **Supabase 가이드** → `supabase-guide-complete.md`
   - supabase-guide.md
   - supabase-implementation.md
   - supabase-test-guide.md

3. **Release 가이드** → `release-guide.md`
   - release-build-guide.md
   - a_RELEASE_SIGNING.md
   - release-signing-setup-complete.md

### archive로 이동 (57개)
- AdMob 관련: 8개
- 긴급 팝업: 7개
- Release 테스트: 7개
- 디버그 수정: 9개
- Supabase 앱 정책: 10개
- RLS: 3개
- 기타: 13개

---

**작성일**: 2025-11-08  
**마지막 정리**: 2025-11-08  
**버전**: 2.0 (대규모 정리 완료)

