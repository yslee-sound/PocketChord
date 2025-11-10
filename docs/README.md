# 📚 PocketChord 문서 가이드

**프로젝트**: PocketChord  
**최종 업데이트**: 2025-11-10  
**문서 상태**: ✅ 대규모 정리 완료

---

## 🚀 빠른 시작

### 처음 방문하셨나요?

#### 1️⃣ 팝업 시스템 이해하기
👉 **[POPUP-SYSTEM-GUIDE.md](POPUP-SYSTEM-GUIDE.md)** ⭐⭐⭐

- 4개 팝업 시스템 (Emergency, Update, Notice, Ad)
- 우선순위 및 동작 원리
- 빠른 참조 가이드

#### 2️⃣ 릴리즈 테스트
👉 **[release/README.md](release/README.md)** ⭐⭐⭐

- Phase 1~5 테스트 가이드
- 배포 체크리스트
- 릴리즈 서명

#### 3️⃣ Update Policy 가이드
👉 **[UPDATE-POLICY-GUIDE.md](release/UPDATE-POLICY-GUIDE.md)** ⭐

- 업데이트 정책 완전 가이드
- 시간 기반 재표시 (Phase 2.5)
- 강제 전환 메커니즘

---

## 📁 문서 구조

### docs/ (루트 폴더)

```
docs/
├── README.md (이 문서)
├── POPUP-SYSTEM-GUIDE.md (팝업 시스템 메인 가이드)
├── UPDATE-POLICY-GUIDE.md (Update Policy 완전 가이드)
├── TEST-ENVIRONMENT-GUIDE.md (테스트 환경)
├── DOCUMENT-CLEANUP-REPORT.md (문서 정리 보고서)
│
├── release/ (릴리즈 관련 문서 - 12개)
│   ├── README.md
│   ├── RELEASE-TEST-CHECKLIST.md
│   ├── RELEASE-TEST-PHASE1~5-RELEASE.md
│   ├── RELEASE-TEST-PHASE2.5-*.md (3개)
│   ├── DEPLOYMENT-CHECKLIST.md
│   └── a_RELEASE_SIGNING.md
│
├── Supabase (2개)
│   ├── SUPABASE-TABLE-CREATION-SUCCESS.md
│   └── SUPABASE-ID-COLUMN-GUIDE.md
│
├── 기타 가이드 (8개)
│   ├── ads-guide.md
│   ├── chords-db-*.md (4개)
│   ├── NEW-APP-ID-GUIDE.md
│   ├── app-version-with-build-type.md
│   └── string-numbering.md
│
├── archive/ (완료된 작업 문서)
└── sql/ (SQL 스크립트)
```

---

## 📚 핵심 문서

### 🎯 메인 가이드 (필수!)

| 문서 | 설명 | 대상 |
|------|------|------|
| **[POPUP-SYSTEM-GUIDE.md](POPUP-SYSTEM-GUIDE.md)** | 팝업 시스템 전체 가이드 | 전체 개발자 |
| **[UPDATE-POLICY-GUIDE.md](release/UPDATE-POLICY-GUIDE.md)** | Update Policy 완전 가이드 | 업데이트 관리자 |
| **[release/README.md](release/README.md)** | 릴리즈 테스트 가이드 | 릴리즈 담당자 |
| **[TEST-ENVIRONMENT-GUIDE.md](release/TEST-ENVIRONMENT-GUIDE.md)** | 테스트 환경 설정 | QA/테스터 |

---

### 📦 릴리즈 관련 (release 폴더)

릴리즈 관련 모든 문서는 **[release/](release/)** 폴더에 있습니다.

#### 체크리스트 & 가이드
- **[release/RELEASE-TEST-CHECKLIST.md](release/RELEASE-TEST-CHECKLIST.md)** - 전체 체크리스트
- **[release/DEPLOYMENT-CHECKLIST.md](release/DEPLOYMENT-CHECKLIST.md)** - 배포 체크리스트
- **[release/a_RELEASE_SIGNING.md](release/a_RELEASE_SIGNING.md)** - 릴리즈 서명

#### Phase별 테스트
1. **[Phase 1](release/RELEASE-TEST-PHASE1-RELEASE.md)** - Emergency Policy
2. **[Phase 2](release/RELEASE-TEST-PHASE2-RELEASE.md)** - Update Policy
3. **[Phase 2.5](release/RELEASE-TEST-PHASE2.5-SETUP.md)** - Update 시간 재표시 (3개 문서)
4. **[Phase 3](release/RELEASE-TEST-PHASE3-RELEASE.md)** - Notice Policy
5. **[Phase 4](release/RELEASE-TEST-PHASE4-RELEASE.md)** - 우선순위 테스트
6. **[Phase 5](release/RELEASE-TEST-PHASE5-RELEASE.md)** - Ad Policy 테스트

---

### 🗄️ Supabase

- **[SUPABASE-TABLE-CREATION-SUCCESS.md](release/SUPABASE-TABLE-CREATION-SUCCESS.md)** - 테이블 생성 및 운영

---

### 🔧 기타 가이드

- **[chords-db-architecture.md](chords-db-architecture.md)** - 코드 DB 아키텍처
- **[NEW-APP-ID-GUIDE.md](NEW-APP-ID-GUIDE.md)** - 앱 ID 변경 가이드
- **[app-version-with-build-type.md](app-version-with-build-type.md)** - 앱 버전 관리
- **[string-numbering.md](string-numbering.md)** - 문자열 넘버링

---

## 🎯 시나리오별 가이드

### 신규 개발자
```
1. README.md 읽기 (이 문서)
2. POPUP-SYSTEM-GUIDE.md (팝업 시스템 이해)
3. TEST-ENVIRONMENT-GUIDE.md (테스트 환경)
```

### 릴리즈 담당자
```
1. release/README.md (릴리즈 가이드)
2. release/RELEASE-TEST-CHECKLIST.md (체크리스트)
3. Phase 1~5 순서대로 테스트
4. release/DEPLOYMENT-CHECKLIST.md (배포 전 확인)
```

### Update Policy 관리
```
1. UPDATE-POLICY-GUIDE.md (완전 가이드)
2. release/RELEASE-TEST-PHASE2-RELEASE.md (기본 테스트)
3. release/RELEASE-TEST-PHASE2.5-*.md (시간 재표시)
```

### 광고 제어
```
1. release/RELEASE-TEST-PHASE5-RELEASE.md (광고 구현 + 테스트)
```

---

## 📝 최근 변경 사항 (2025-11-10)

### ✅ 대규모 문서 정리 완료

#### 정리 결과
- **이전**: 38개 문서 (혼재)
- **현재**: 25개 문서 (정리됨)
- **감소**: ▼ 13개 (34%)

#### 주요 변경
1. **release/ 폴더 생성** ✨
   - 릴리즈 관련 문서 16개 이동
   - 명확한 구조 확립

2. **문서 통합** ✨
   - UPDATE-POLICY 3개 → 1개 (UPDATE-POLICY-GUIDE.md)
   - ads-guide.md → RELEASE-TEST-PHASE5-RELEASE.md에 통합 ✨
   - 중복 제거 및 내용 강화

3. **archive 이동**
   - 완료된 작업 문서 11개
   - 레거시 문서 정리

4. **신규 문서 생성** ✨
   - release/README.md (릴리즈 가이드)
   - RELEASE-TEST-PHASE5-RELEASE.md (Ad Policy v2.0 - 구현 가이드 포함)
   - UPDATE-POLICY-GUIDE.md (통합 가이드)

---

## ❓ FAQ

### Q: 어느 문서부터 읽어야 하나요?
**A**: 역할에 따라:
- **개발자**: POPUP-SYSTEM-GUIDE.md → UPDATE-POLICY-GUIDE.md
- **릴리즈 담당자**: release/README.md → Phase 1~5
- **테스터**: TEST-ENVIRONMENT-GUIDE.md

### Q: release 폴더는 무엇인가요?
**A**: 릴리즈와 배포에 관련된 모든 문서가 모여있는 폴더입니다.
- Phase별 테스트 문서 (1~5)
- 체크리스트
- 배포 가이드

### Q: Phase 2.5가 무엇인가요?
**A**: Update Policy의 시간 기반 재표시 기능입니다.
- "나중에" 클릭 후 일정 시간 경과 시 재표시
- 3개 문서로 구성 (SETUP, SCENARIOS, ADVANCED)

### Q: 문서가 너무 많아요!
**A**: 핵심 3개만 보세요:
1. **POPUP-SYSTEM-GUIDE.md** (팝업 시스템)
2. **UPDATE-POLICY-GUIDE.md** (업데이트 정책)
3. **release/README.md** (릴리즈 테스트)

### Q: archive 폴더는 무엇인가요?
**A**: 완료된 작업 기록 문서들입니다.
- 삭제하지 말고 참고용으로 보관
- 변경 이력 추적에 유용

### Q: SQL 스크립트는 어디에 있나요?
**A**: `sql/` 폴더에 있습니다.
- 테이블 생성 스크립트
- 테스트용 SQL

---

## 🔍 문서 찾기

| 찾는 내용 | 문서 |
|----------|------|
| 팝업 시스템 전체 | POPUP-SYSTEM-GUIDE.md |
| 업데이트 정책 | UPDATE-POLICY-GUIDE.md |
| 릴리즈 테스트 | release/README.md |
| 광고 제어 | release/RELEASE-TEST-PHASE5-RELEASE.md |
| 배포 준비 | release/DEPLOYMENT-CHECKLIST.md |
| 테스트 환경 | TEST-ENVIRONMENT-GUIDE.md |
| Supabase 설정 | SUPABASE-TABLE-CREATION-SUCCESS.md |
| 문서 정리 내역 | DOCUMENT-CLEANUP-REPORT.md |

---

## 📊 통계

- **총 문서 수**: 25개 (docs 9개 + release 16개)
- **메인 가이드**: 4개
- **릴리즈 문서**: 16개
- **기타**: 5개

---

## 🔗 외부 링크

- **Supabase Console**: [프로젝트 대시보드](https://supabase.com/dashboard)
- **Play Console**: [앱 관리](https://play.google.com/console)

---

**작성일**: 2025-11-10  
**마지막 정리**: 2025-11-10  
**버전**: 3.0 (대규모 정리 및 release 폴더 생성)

**Supabase 테이블 생성**:
- `sql/01-create-update-policy.sql`
- `sql/02-create-emergency-policy.sql`
- `sql/03-create-notice-policy.sql`

**테스트용 SQL**:
- `sql/test-scripts-release.sql` (릴리즈용)
- `sql/test-scripts-debug.sql` (디버그용)

---

## 📖 기타 문서

### Supabase
- `supabase-guide-complete.md` - Supabase 완전 가이드
- `SUPABASE-TABLE-CREATION-SUCCESS.md` - 테이블 생성 성공 기록

### 배포 및 릴리즈
- `DEPLOYMENT-CHECKLIST.md` - 배포 체크리스트
- `release-guide.md` - 릴리즈 가이드
- `a_RELEASE_SIGNING.md` - 릴리즈 서명

### 기타
- `TEST-ENVIRONMENT-GUIDE.md` - 테스트 환경 선택 가이드
- `chords-db-architecture.md` - 코드 데이터베이스 아키텍처

---

## 🗂️ Archive

더 이상 사용하지 않지만 참고용으로 보관:
- `archive/` 폴더 참조

---

**최종 업데이트**: 2025-11-09  
**주요 변경**: 문서 정리 및 통합 완료

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

