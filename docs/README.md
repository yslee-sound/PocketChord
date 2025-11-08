# 📚 PocketChord 문서 가이드

**프로젝트**: PocketChord  
**업데이트**: 2025-11-08

---

## 📋 문서 분류

### 🎯 핵심 구현 가이드 (읽어야 할 문서)

#### 1. 광고 정책 분리 (최신)
- **`ad-policy-separation-implementation-complete.md`** ⭐ 메인 가이드
  - 방안 1(테이블 분리) 전체 구현 가이드
  - 배포 절차 포함
  - **"complete"**: 구현이 완료되었다는 의미 (작업 완료 보고서)
  
- **`QUICKSTART-AD-POLICY-SEPARATION.md`**
  - 5분 빠른 시작
  
- **`IMPLEMENTATION-SUMMARY.md`**
  - 전체 요약

#### 2. Supabase 관련
- **`SUPABASE-ID-COLUMN-GUIDE.md`**
  - id 컬럼 이해하기
  
- **`SUPABASE-TABLE-CREATION-SUCCESS.md`**
  - 테이블 생성 확인 및 운영 가이드

#### 3. 배포 관련
- **`DEPLOYMENT-CHECKLIST.md`**
  - 배포 전 체크리스트

#### 4. 레거시 (참고용)
- **`admob-supabase-control-IMPLEMENTATION-COMPLETE.md`**
  - AdMob Supabase 제어 구현 완료 보고서 (구버전)
  - **"COMPLETE"**: 해당 작업이 완료되었음을 표시
  
- **`admob-banner-auto-refresh-COMPLETE.md`**
  - 배너 광고 자동 새로고침 구현 완료 (구버전)

---

### 📌 "complete" 문서란?

**의미**: "구현 완료 보고서"

**용도**:
1. ✅ 작업이 완료되었음을 명시
2. 📋 구현 내용 정리
3. 🔍 나중에 변경 이력 추적
4. 📚 다른 개발자 참고용

**예시**:
- `ad-policy-separation-implementation-complete.md`
  → "광고 정책 분리 구현 완료"
  
- `admob-supabase-control-IMPLEMENTATION-COMPLETE.md`
  → "AdMob Supabase 제어 구현 완료"

**vs 다른 문서**:
- `QUICKSTART`: 빠른 시작 (5분)
- `SUMMARY`: 요약 (5분)
- `complete`: 상세 가이드 (15-30분) + 완료 보고서

---

### 📄 SQL 스크립트

#### 광고 정책 (ad_policy)
- **`ad-policy-table-creation.sql`** ⭐ 메인 스크립트
  - Release + Debug 데이터 포함
  - 기본값: 시간당 2회, 일일 15회
  
- **`ad-policy-add-debug-build.sql`**
  - Debug 데이터만 추가

#### 앱 정책 정리 (선택사항)
- **`app-policy-remove-ad-columns.sql`**
  - app_policy에서 광고 컬럼 제거
  - 가이드: `APP-POLICY-CLEANUP-GUIDE.md`

#### 기존 스키마 (참고용)
- `supabase-ad-control-schema.sql` - app_policy 광고 컬럼 추가 (구버전)
- `supabase-ad-control-add-not-null.sql` - NOT NULL 추가 (구버전)

---

### 🔧 기타 가이드

#### 정리 가이드
- **`APP-POLICY-CLEANUP-GUIDE.md`**
  - app_policy 테이블 정리 (선택사항)

#### 광고 설정 (레거시)
- `admob-supabase-control-IMPLEMENTATION-COMPLETE.md` - 구현 완료 (구버전)
- `admob-supabase-control-plan.md` - 계획 문서
- `admob-supabase-control-NEXT-STEPS.md` - 다음 단계

#### 앱 정책 분석
- `app-policy-ad-policy-separation-analysis.md` - 분리 분석 문서

---

## 🚀 빠른 시작

### 1. 신규 개발자 온보딩
```
1. IMPLEMENTATION-SUMMARY.md 읽기 (5분)
2. ad-policy-separation-implementation-complete.md 읽기 (15분)
3. SUPABASE-ID-COLUMN-GUIDE.md 읽기 (10분)
```

### 2. Supabase 테이블 생성
```
1. QUICKSTART-AD-POLICY-SEPARATION.md 참고
2. ad-policy-table-creation.sql 실행
3. SUPABASE-TABLE-CREATION-SUCCESS.md로 확인
```

### 3. 배포 준비
```
1. DEPLOYMENT-CHECKLIST.md 체크
2. 모든 항목 확인 후 배포
```

---

## 📊 문서 맵

```
방안 1 구현
├── ad-policy-separation-implementation-complete.md (메인)
├── QUICKSTART-AD-POLICY-SEPARATION.md (빠른 시작)
└── IMPLEMENTATION-SUMMARY.md (요약)

Supabase
├── ad-policy-table-creation.sql (테이블 생성)
├── SUPABASE-ID-COLUMN-GUIDE.md (id 이해)
└── SUPABASE-TABLE-CREATION-SUCCESS.md (확인 가이드)

배포
└── DEPLOYMENT-CHECKLIST.md (체크리스트)

선택사항
├── APP-POLICY-CLEANUP-GUIDE.md (정리 가이드)
└── app-policy-remove-ad-columns.sql (정리 SQL)

참고용 (레거시)
├── app-policy-ad-policy-separation-analysis.md
├── admob-supabase-control-*.md
└── supabase-ad-control-*.sql
```

---

## 🎯 시나리오별 문서

### 시나리오 1: 처음 시작하는 경우
1. `IMPLEMENTATION-SUMMARY.md` - 전체 이해
2. `ad-policy-separation-implementation-complete.md` - 상세 가이드
3. `QUICKSTART-AD-POLICY-SEPARATION.md` - 실행

### 시나리오 2: Supabase 설정
1. `QUICKSTART-AD-POLICY-SEPARATION.md` - 빠른 실행
2. `ad-policy-table-creation.sql` - SQL 실행
3. `SUPABASE-TABLE-CREATION-SUCCESS.md` - 확인

### 시나리오 3: 배포 준비
1. `DEPLOYMENT-CHECKLIST.md` - 체크리스트 확인
2. 모든 항목 체크 후 배포

### 시나리오 4: 테이블 정리 (선택)
1. `APP-POLICY-CLEANUP-GUIDE.md` - 가이드 읽기
2. 시나리오 선택
3. `app-policy-remove-ad-columns.sql` - SQL 실행 (필요시)

### 시나리오 5: 궁금한 게 있을 때
- **id가 뭔가요?** → `SUPABASE-ID-COLUMN-GUIDE.md`
- **app_id와 차이는?** → `SUPABASE-ID-COLUMN-GUIDE.md`
- **빈도 제한 기본값은?** → `ad-policy-table-creation.sql` 주석 참고

---

## 🗂️ 문서 네이밍 규칙

### 대문자 파일 (핵심 문서)
- `IMPLEMENTATION-SUMMARY.md` - 전체 요약
- `QUICKSTART-*.md` - 빠른 시작
- `DEPLOYMENT-CHECKLIST.md` - 체크리스트
- `SUPABASE-*.md` - Supabase 관련
- `APP-POLICY-*.md` - 앱 정책 관련

### 소문자 파일 (상세 가이드)
- `ad-policy-*.md` - 광고 정책 관련
- `admob-*.md` - AdMob 관련 (레거시)
- `supabase-*.md` - Supabase 관련 (레거시)

### SQL 파일
- `*-table-creation.sql` - 테이블 생성
- `*-add-*.sql` - 데이터 추가
- `*-remove-*.sql` - 정리

---

## 🔄 문서 버전

### 최신 (2025-11-08)
- ✅ 광고 정책 분리 (방안 1)
- ✅ 빈도 제한 기본값: 시간당 2회, 일일 15회
- ✅ Release + Debug 빌드 지원

### 레거시 (참고용)
- 📦 app_policy 통합 방식 (구버전)
- 📦 빈도 제한 기본값: 시간당 3회, 일일 20회 (구버전)

---

## ❓ FAQ

### Q: 어느 문서부터 읽어야 하나요?
**A**: `IMPLEMENTATION-SUMMARY.md` → `QUICKSTART-AD-POLICY-SEPARATION.md`

### Q: SQL은 어떤 걸 실행하나요?
**A**: `ad-policy-table-creation.sql` 하나만 실행하면 됩니다.

### Q: 레거시 문서는 삭제해도 되나요?
**A**: 아니요. 참고용으로 남겨두세요. 나중에 변경 이력 추적 시 유용합니다.

### Q: 문서가 너무 많아요!
**A**: 핵심 3개만 보세요:
1. `IMPLEMENTATION-SUMMARY.md`
2. `QUICKSTART-AD-POLICY-SEPARATION.md`
3. `DEPLOYMENT-CHECKLIST.md`

### Q: "complete" 문서는 뭔가요?
**A**: **"구현 완료 보고서"** 입니다.

**특징**:
- 📝 해당 작업이 완료되었음을 명시
- 📋 구현 내용 상세 정리
- 🔍 변경 이력 추적용
- 📚 다른 개발자 참고용

**예시**:
```
ad-policy-separation-implementation-complete.md
→ "광고 정책 분리 구현이 완료되었습니다"
→ 어떻게 구현했는지, 무엇을 했는지 상세히 기록
```

**언제 만드나요?**:
- 큰 작업이 끝났을 때
- 나중에 참고할 가치가 있을 때
- 팀원들에게 공유할 때

**vs 다른 문서**:
- `SUMMARY`: 간단 요약 (5분)
- `QUICKSTART`: 빠른 실행 (5분)
- `complete`: 상세 + 완료 증명 (15-30분)

---

## 📞 도움이 필요하면

1. **먼저 읽기**: `IMPLEMENTATION-SUMMARY.md`
2. **빠른 시작**: `QUICKSTART-AD-POLICY-SEPARATION.md`
3. **상세 가이드**: `ad-policy-separation-implementation-complete.md`
4. **체크리스트**: `DEPLOYMENT-CHECKLIST.md`

---

**작성일**: 2025-11-08  
**마지막 업데이트**: 2025-11-08  
**버전**: 1.0

