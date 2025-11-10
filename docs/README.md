# 📚 PocketChord 문서 가이드

**프로젝트**: PocketChord  
**최종 업데이트**: 2025-11-10  
**문서 상태**: ✅ 대규모 정리 완료

---

## 🚀 빠른 시작

### 처음 방문하셨나요?

#### 1️⃣ 팝업 시스템 이해하기
👉 **[release/RELEASE-TEST-PHASE1-RELEASE.md](release/RELEASE-TEST-PHASE1-RELEASE.md)** ⭐⭐⭐

- 4개 팝업 시스템 (Emergency, Update, Notice, Ad)
- 우선순위 및 동작 원리
- 빠른 참조 가이드

#### 2️⃣ 릴리즈 테스트
👉 **[release/RELEASE-TEST-CHECKLIST.md](release/RELEASE-TEST-CHECKLIST.md)** ⭐⭐⭐

- Phase 1~5 테스트 가이드
- 배포 체크리스트
- 릴리즈 서명

#### 3️⃣ Update Policy 가이드
👉 **[release/RELEASE-TEST-PHASE2.1-RELEASE.md](release/RELEASE-TEST-PHASE2.1-RELEASE.md)** ⭐

- 업데이트 정책 완전 가이드
- 시간 기반 재표시 (Phase 2.2~2.4)
- 강제 전환 메커니즘

---

## 📁 문서 구조

### docs/ (루트 폴더)

```
docs/
├── README.md (이 문서)
├── DOCUMENT-CLEANUP-REPORT.md (문서 정리 보고서)
│
├── release/ (릴리즈 관련 문서 - 11개)
│   ├── RELEASE-TEST-CHECKLIST.md (테스트 환경 선택 + release 폴더 안내)
│   ├── RELEASE-TEST-PHASE1-RELEASE.md (팝업 시스템 개요 포함)
│   ├── RELEASE-TEST-PHASE2.1-RELEASE.md (Update Policy 가이드 포함)
│   ├── RELEASE-TEST-PHASE3~5-RELEASE.md
│   ├── RELEASE-TEST-PHASE2.2-SETUP.md
│   ├── RELEASE-TEST-PHASE2.3-ADVANCED.md
│   ├── RELEASE-TEST-PHASE2.4-SCENARIOS.md
│   └── a_RELEASE_SIGNING.md
│
├── 기타 가이드 (5개)
│   ├── SUPABASE-ID-COLUMN-GUIDE.md
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
| **[release/RELEASE-TEST-PHASE1-RELEASE.md](release/RELEASE-TEST-PHASE1-RELEASE.md)** | 팝업 시스템 개요 + Emergency 테스트 | 전체 개발자 |
| **[release/RELEASE-TEST-PHASE2.1-RELEASE.md](release/RELEASE-TEST-PHASE2.1-RELEASE.md)** | Update Policy 가이드 + 테스트 | 업데이트 관리자 |
| **[release/RELEASE-TEST-CHECKLIST.md](release/RELEASE-TEST-CHECKLIST.md)** | 전체 릴리즈 테스트 + 폴더 안내 | 릴리즈 담당자/QA |

---

### 📦 릴리즈 관련 (release 폴더)

릴리즈 관련 모든 문서는 **[release/](release/)** 폴더에 있습니다.

#### 체크리스트 & 가이드
- **[RELEASE-TEST-CHECKLIST.md](release/RELEASE-TEST-CHECKLIST.md)** - 전체 체크리스트
- **[a_RELEASE_SIGNING.md](release/a_RELEASE_SIGNING.md)** - 릴리즈 서명

#### Phase별 테스트
1. **[Phase 1](release/RELEASE-TEST-PHASE1-RELEASE.md)** - Emergency Policy
2. **[Phase 2.1](release/RELEASE-TEST-PHASE2.1-RELEASE.md)** - Update Policy
3. **[Phase 2.2~2.4](release/RELEASE-TEST-PHASE2.2-SETUP.md)** - Update 시간 재표시 (3개 문서)
4. **[Phase 3](release/RELEASE-TEST-PHASE3-RELEASE.md)** - Notice Policy
5. **[Phase 4](release/RELEASE-TEST-PHASE4-RELEASE.md)** - 우선순위 테스트
6. **[Phase 5](release/RELEASE-TEST-PHASE5-RELEASE.md)** - Ad Policy 테스트

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
2. release/RELEASE-TEST-PHASE1-RELEASE.md (팝업 시스템 이해)
3. release/RELEASE-TEST-CHECKLIST.md (테스트 환경 확인)
```

### 릴리즈 담당자
```
1. release/RELEASE-TEST-CHECKLIST.md (전체 가이드)
2. Phase 1~5 순서대로 테스트
```

### Update Policy 관리
```
1. release/RELEASE-TEST-PHASE2.1-RELEASE.md (완전 가이드)
2. release/RELEASE-TEST-PHASE2.2-SETUP.md (시간 재표시 설정)
3. release/RELEASE-TEST-PHASE2.3-ADVANCED.md (고급 테스트)
4. release/RELEASE-TEST-PHASE2.4-SCENARIOS.md (시나리오 테스트)
```

### 광고 제어
```
1. release/RELEASE-TEST-PHASE5-RELEASE.md
```

---

## 📝 최근 변경 사항 (2025-11-10)

### ✅ 대규모 문서 정리 완료

#### 정리 결과
- **이전**: 38개 문서 (혼재)
- **현재**: 24개 문서 (정리됨)
- **감소**: ▼ 14개 (37%)

#### 주요 변경
1. **release/ 폴더 생성** ✨
   - 릴리즈 관련 문서 15개 이동
   - 명확한 구조 확립

2. **문서 통합** ✨
   - POPUP-SYSTEM-GUIDE → PHASE1 (팝업 시스템 개요 통합)
   - UPDATE-POLICY-GUIDE → PHASE2.1 (Update Policy 가이드 통합) ✨
   - TEST-ENVIRONMENT-GUIDE → CHECKLIST (테스트 환경 통합)
   - 중복 제거 및 내용 강화

3. **archive 이동**
   - 완료된 작업 문서 11개
   - 레거시 문서 정리

4. **신규 문서 생성** ✨
   - release/README.md (릴리즈 가이드)
   - RELEASE-TEST-PHASE1-RELEASE.md v2.0 (팝업 시스템 개요 포함)
   - RELEASE-TEST-PHASE2.1-RELEASE.md v3.0 (Update Policy 가이드 포함)
   - RELEASE-TEST-PHASE5-RELEASE.md v2.0 (배포 체크리스트 포함)

---

## ❓ FAQ

### Q: 어느 문서부터 읽어야 하나요?
**A**: 역할에 따라:
- **개발자**: release/RELEASE-TEST-PHASE1-RELEASE.md → release/RELEASE-TEST-PHASE2.1-RELEASE.md
- **릴리즈 담당자**: release/README.md → Phase 1~5
- **테스터**: release/RELEASE-TEST-CHECKLIST.md (테스트 환경 설명 포함)

### Q: release 폴더는 무엇인가요?
**A**: 릴리즈와 배포에 관련된 모든 문서가 모여있는 폴더입니다.
- Phase별 테스트 문서 (1~5)
- 체크리스트
- 배포 가이드

### Q: Phase 2.2~2.4가 무엇인가요?
**A**: Update Policy의 시간 기반 재표시 기능입니다.
- "나중에" 클릭 후 일정 시간 경과 시 재표시
- 3개 문서로 구성 (SETUP, ADVANCED, SCENARIOS)

### Q: 문서가 너무 많아요!
**A**: 핵심 2개만 보세요:
1. **release/RELEASE-TEST-PHASE1-RELEASE.md** (팝업 시스템)
2. **release/RELEASE-TEST-PHASE2.1-RELEASE.md** (업데이트 정책)
3. **release/RELEASE-TEST-CHECKLIST.md** (릴리즈 테스트)

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
| 팝업 시스템 전체 | release/RELEASE-TEST-PHASE1-RELEASE.md |
| 업데이트 정책 | release/RELEASE-TEST-PHASE2.1-RELEASE.md |
| 릴리즈 테스트 | release/RELEASE-TEST-CHECKLIST.md |
| 광고 제어 | release/RELEASE-TEST-PHASE5-RELEASE.md |
| 배포 준비 | release/RELEASE-TEST-PHASE5-RELEASE.md (섹션 5) |
| 테스트 환경 | release/RELEASE-TEST-CHECKLIST.md (섹션 1) |
| Supabase 설정 | release/RELEASE-TEST-PHASE5-RELEASE.md (섹션 5.1) |
| 문서 정리 내역 | DOCUMENT-CLEANUP-REPORT.md |

---

## 📊 통계

- **총 문서 수**: 19개 (docs 8개 + release 11개)
- **메인 가이드**: 3개
- **릴리즈 문서**: 11개 (Phase별 문서 + 체크리스트)
- **기타**: 5개

---

## 🔗 외부 링크

- **Supabase Console**: [프로젝트 대시보드](https://supabase.com/dashboard)
- **Play Console**: [앱 관리](https://play.google.com/console)

---

**작성일**: 2025-11-10  
**마지막 정리**: 2025-11-10  
**버전**: 9.0 (서브섹션 넘버링 완료 - 최종)

---

## 🎊 최종 정리 완료!

**문서 구조**:
- ✅ **총 문서**: 19개 (docs 8개 + release 11개)
- ✅ **모든 Phase 문서 버전 통일**: v3.0+ (PHASE2는 v4.0)
- ✅ **넘버링 체계**: 메인(1, 2, 3...) + 서브(1, 2, 3...)
- ✅ **체크리스트 완전 재구성**: 각 Phase 문서 참조
- ✅ **문서 구조 100% 일관성**: 모든 Phase 동일한 포맷

**넘버링 체계**:
```
## 1 메인 섹션
### 1 서브섹션
### 2 서브섹션

## 2 메인 섹션
### 1 서브섹션
### 2 서브섹션
```

---

**작성일**: 2025-11-08  
**마지막 정리**: 2025-11-08  
**버전**: 2.0 (대규모 정리 완료)

