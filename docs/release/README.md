# 📦 릴리즈 관련 문서

이 폴더에는 PocketChord 앱의 릴리즈와 배포에 관련된 모든 문서가 있습니다.

---

## 📋 문서 목록

### 체크리스트 & 가이드
- **[RELEASE-TEST-CHECKLIST.md](RELEASE-TEST-CHECKLIST.md)** - 릴리즈 테스트 전체 체크리스트
- **[DEPLOYMENT-CHECKLIST.md](DEPLOYMENT-CHECKLIST.md)** - 배포 전 체크리스트
- **[a_RELEASE_SIGNING.md](a_RELEASE_SIGNING.md)** - 릴리즈 서명 가이드

### Phase별 테스트 문서

#### Phase 1: Emergency Policy
- **[RELEASE-TEST-PHASE1-RELEASE.md](RELEASE-TEST-PHASE1-RELEASE.md)**
- 긴급 공지 테스트
- 최우선 순위 팝업

#### Phase 2: Update Policy
- **[RELEASE-TEST-PHASE2-RELEASE.md](RELEASE-TEST-PHASE2-RELEASE.md)** - 기본 업데이트 테스트
- **[RELEASE-TEST-PHASE2.5-SETUP.md](RELEASE-TEST-PHASE2.5-SETUP.md)** - 시간 기반 재표시 설정
- **[RELEASE-TEST-PHASE2.5-SCENARIOS.md](RELEASE-TEST-PHASE2.5-SCENARIOS.md)** - 시간 기반 재표시 시나리오
- **[RELEASE-TEST-PHASE2.5-ADVANCED.md](RELEASE-TEST-PHASE2.5-ADVANCED.md)** - 시간 기반 재표시 고급/문제해결

#### Phase 3: Notice Policy
- **[RELEASE-TEST-PHASE3-RELEASE.md](RELEASE-TEST-PHASE3-RELEASE.md)**
- 일반 공지 테스트

#### Phase 4: Priority Test
- **[RELEASE-TEST-PHASE4-RELEASE.md](RELEASE-TEST-PHASE4-RELEASE.md)**
- 팝업 우선순위 테스트
- 통합 시나리오

#### Phase 5: Ad Policy
- **[RELEASE-TEST-PHASE5-RELEASE.md](RELEASE-TEST-PHASE5-RELEASE.md)**
- 광고 제어 시스템 테스트
- App Open / Interstitial / Banner 제어

---

## 🚀 빠른 시작

### 처음 릴리즈 테스트를 시작하는 경우
1. **[RELEASE-TEST-CHECKLIST.md](RELEASE-TEST-CHECKLIST.md)** - 전체 흐름 파악
2. **Phase 1~5 순서대로 진행**
3. **[DEPLOYMENT-CHECKLIST.md](DEPLOYMENT-CHECKLIST.md)** - 배포 전 최종 확인

### 특정 Phase만 테스트하는 경우
- Emergency: Phase 1
- Update: Phase 2 + Phase 2.5
- Notice: Phase 3
- 우선순위: Phase 4
- 광고: Phase 5

---

## 📊 테스트 프로세스

```
1. 초기 상태 확인
   ↓
2. Phase 1: Emergency 테스트
   ↓
3. Phase 2: Update 기본 테스트
   ↓
4. Phase 2.5: Update 시간 재표시 테스트
   ↓
5. Phase 3: Notice 테스트
   ↓
6. Phase 4: 우선순위 테스트
   ↓
7. Phase 5: Ad Policy 테스트
   ↓
8. 최종 확인 및 복구
   ↓
9. 배포 체크리스트 확인
   ↓
10. 릴리즈 승인 ✅
```

---

## 🎯 중요 참고사항

### Phase 2.5 (시간 기반 재표시)
Phase 2.5는 3개 문서로 구성되어 있습니다:
1. **SETUP** - DB 설정 및 초기값
2. **SCENARIOS** - 시나리오별 테스트 (S1~S6)
3. **ADVANCED** - 에지 케이스 및 문제 해결

**권장 순서**: SETUP → SCENARIOS → ADVANCED

### 운영 환경 주의사항
- ⚠️ Phase 2.5 테스트 후 반드시 운영 설정으로 복구
  - `reshow_interval_seconds` = NULL
  - `reshow_interval_minutes` = NULL
  - `reshow_interval_hours` = 24

---

## 📚 관련 문서 (docs 루트)

릴리즈 테스트 외 참고 문서:
- **[../POPUP-SYSTEM-GUIDE.md](POPUP-SYSTEM-GUIDE.md)** - 팝업 시스템 전체 가이드
- **[../UPDATE-POLICY-GUIDE.md](UPDATE-POLICY-GUIDE.md)** - Update Policy 완전 가이드
- **[../TEST-ENVIRONMENT-GUIDE.md](TEST-ENVIRONMENT-GUIDE.md)** - 테스트 환경 설정

---

**폴더 생성일**: 2025-11-10  
**문서 수**: 11개  
**관리자**: PocketChord Dev Team

