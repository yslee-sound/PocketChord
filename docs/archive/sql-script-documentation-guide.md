# SQL 스크립트 문서화 가이드

**날짜**: 2025-01-08  
**주제**: "문서화 가능"의 의미

---

## 🎯 핵심 개념

### "문서화 가능"이란?

**❌ 잘못된 이해:**
```
Supabase가 SQL 스크립트를 자동으로 저장해준다?
→ 아닙니다!
```

**✅ 올바른 의미:**
```
SQL 스크립트를 파일로 저장해두면
나중에 다시 사용하거나 공유할 수 있다!
```

---

## 🆚 UI vs SQL 스크립트 비교

### UI 방식 (Supabase 대시보드)

**작업 방법:**
```
1. Supabase 대시보드 열기
2. Table Editor → app_policy
3. 컬럼 클릭 (예: ad_banner_enabled)
4. "Allow Nullable" 토글 OFF
5. Save 버튼 클릭
```

**문제점:**
- ❌ 기록이 남지 않음
- ❌ 다른 사람에게 설명하기 어려움
- ❌ 다른 환경에서 재현 어려움
- ❌ 무엇을 바꿨는지 나중에 기억 안 남
- ❌ 실수 시 되돌리기 어려움

---

### SQL 스크립트 방식

**작업 방법:**
```sql
-- 파일: supabase-setup.sql
ALTER TABLE app_policy
ALTER COLUMN ad_banner_enabled SET NOT NULL;
```

**장점:**
- ✅ 파일로 저장됨
- ✅ Git으로 버전 관리 가능
- ✅ 팀원과 공유 쉬움
- ✅ 다른 환경에서 재현 가능
- ✅ 변경 이력 추적 가능
- ✅ 롤백 가능

---

## 📁 문서화의 실체

### 프로젝트 구조 예시

```
PocketChord/
├── docs/
│   ├── supabase-ad-control-schema.sql          ← 문서화!
│   ├── supabase-ad-control-add-not-null.sql    ← 문서화!
│   ├── admob-not-null-constraint-guide.md      ← 문서화!
│   └── sql-script-documentation-guide.md       ← 이 문서!
├── app/
│   └── src/
└── README.md
```

**이점:**
1. **저장소에 포함** - Git에서 관리
2. **버전 관리** - 언제 무엇을 바꿨는지 추적
3. **공유 가능** - 팀원에게 파일만 전달
4. **재사용** - 언제든 다시 실행 가능

---

## 🎬 실제 시나리오

### 시나리오 1: 팀원이 물어볼 때

**UI로 했을 경우:**
```
팀원: "DB 어떻게 설정했어요?"
나: "음... 그러니까... 
     Supabase 들어가서...
     컬럼 하나씩 열어서...
     Allow Nullable을... 
     어... 뭐였더라? 🤔"
```
→ 😰 설명하기 어렵고, 실수 가능성 높음

**SQL 파일이 있을 경우:**
```
팀원: "DB 어떻게 설정했어요?"
나: "이 파일 실행하면 돼요!"
    → docs/supabase-ad-control-add-not-null.sql
```
→ 😎 간단하고 명확!

---

### 시나리오 2: 새 환경 구축

**UI로 했을 경우:**
```
개발 DB 설정:
  1. Supabase 열기
  2. ad_banner_enabled 클릭
  3. Allow Nullable OFF
  4. Save
  5. ad_interstitial_enabled 클릭
  6. Allow Nullable OFF
  7. Save
  ...반복...

테스트 DB 설정:
  1. Supabase 열기
  2. ad_banner_enabled 클릭
  ...또 반복... 😫

운영 DB 설정:
  ...또또 반복... 😱
```
→ 실수 가능성 높고, 시간 낭비

**SQL 파일이 있을 경우:**
```
개발 DB: supabase-ad-control-add-not-null.sql 실행 ✅
테스트 DB: supabase-ad-control-add-not-null.sql 실행 ✅
운영 DB: supabase-ad-control-add-not-null.sql 실행 ✅
```
→ 일관성 보장, 빠르고 정확!

---

### 시나리오 3: 6개월 후

**UI로 했을 경우:**
```
나: "어? 이 컬럼은 왜 NOT NULL이지?"
나: "내가 설정한 거였나?"
나: "언제 했더라?"
나: "왜 했지?"
```
→ 😵 기억 안 남, 이력 추적 불가능

**SQL 파일 + Git이 있을 경우:**
```bash
$ git log docs/supabase-ad-control-add-not-null.sql

commit abc123def456
Date: 2025-01-08 21:30:00
Author: 나 <my@email.com>
Message: NOT NULL 제약 조건 추가
         
         광고는 true/false만 허용하도록 변경
         - null 값 방지
         - DB 레벨 안전성 보장
```
→ 😊 명확한 기록과 이유!

---

### 시나리오 4: 실수 복구

**UI로 했을 경우:**
```
나: "어? 실수로 잘못 바꿨네..."
나: "원래 뭐였더라?"
나: "되돌리려면 어떻게 해야 하지?"
```
→ 😱 복구 어려움

**SQL 파일 + Git이 있을 경우:**
```bash
# 이전 버전 확인
$ git diff HEAD~1 docs/supabase-ad-control-add-not-null.sql

# 롤백 스크립트 실행
ALTER TABLE app_policy
ALTER COLUMN ad_banner_enabled DROP NOT NULL;
```
→ 😎 쉽게 복구!

---

## 📊 비교표

| 항목 | UI 클릭 | SQL 스크립트 파일 |
|-----|---------|-----------------|
| **문서화** | ❌ 불가능 (기억에 의존) | ✅ 파일로 저장 |
| **공유** | ❌ 구두 설명 필요 | ✅ 파일만 전달 |
| **재현** | ❌ 다시 클릭해야 함 | ✅ 파일 실행 |
| **버전 관리** | ❌ 불가능 | ✅ Git 사용 가능 |
| **일관성** | ❌ 실수 가능 | ✅ 항상 동일 |
| **이력 추적** | ❌ 기록 없음 | ✅ 커밋 히스토리 |
| **롤백** | ❌ 어려움 | ✅ 쉬움 |
| **환경 복제** | ❌ 반복 작업 | ✅ 한 번만 작성 |
| **팀 협업** | ❌ 설명 필요 | ✅ 파일 공유 |
| **시간** | ❌ 환경마다 반복 | ✅ 한 번만 |

---

## 🛠️ 실전 적용

### 1. SQL 스크립트 작성

```sql
-- 파일: docs/database-setup.sql
-- 목적: 프로덕션 DB 초기 설정
-- 작성자: 나
-- 작성일: 2025-01-08

-- 1. NOT NULL 제약 조건 추가
ALTER TABLE app_policy
ALTER COLUMN ad_banner_enabled SET NOT NULL;

-- 2. 기본값 확인
SELECT * FROM app_policy;
```

**포인트:**
- 주석으로 목적 명시
- 작성자와 날짜 기록
- 단계별 설명

---

### 2. Git에 커밋

```bash
$ git add docs/database-setup.sql
$ git commit -m "Add: NOT NULL constraint for ad columns

- Prevent null values in ad_* columns
- Ensure ad state is always clear (true/false only)
- Add safety at DB level"

$ git push
```

**포인트:**
- 명확한 커밋 메시지
- 변경 이유 설명
- 영향 범위 명시

---

### 3. 팀원과 공유

```
팀원: "DB 설정 어떻게 해요?"
나: "docs/database-setup.sql 실행하면 됩니다!"
```

**또는 Slack/이메일:**
```
안녕하세요!
DB 설정 스크립트를 작성했습니다.

파일: docs/database-setup.sql
실행 방법:
1. Supabase SQL Editor 열기
2. 파일 내용 복사
3. 실행

질문 있으면 연락주세요!
```

---

### 4. 다른 환경에 적용

```
개발 환경 ✅
├── Supabase 개발 DB
└── docs/database-setup.sql 실행

테스트 환경 ✅
├── Supabase 테스트 DB
└── docs/database-setup.sql 실행

운영 환경 ✅
├── Supabase 운영 DB
└── docs/database-setup.sql 실행
```

**결과:**
- 모든 환경이 동일하게 설정됨
- 실수 없음
- 시간 절약

---

## 💡 베스트 프랙티스

### 1. 파일 이름 규칙

```
✅ 좋은 예:
- supabase-ad-control-schema.sql
- database-migration-2025-01-08.sql
- add-not-null-constraints.sql

❌ 나쁜 예:
- test.sql
- temp.sql
- 새파일1.sql
```

### 2. 주석 작성

```sql
-- ============================================
-- 파일명: supabase-ad-control-add-not-null.sql
-- 목적: 광고 컬럼에 NOT NULL 제약 조건 추가
-- 작성자: 홍길동
-- 작성일: 2025-01-08
-- 주의: NULL 값 제거 후 실행 필수!
-- ============================================

-- 1단계: NULL 값 확인
SELECT COUNT(*) FROM app_policy WHERE ad_banner_enabled IS NULL;

-- 2단계: NULL 값 제거
UPDATE app_policy
SET ad_banner_enabled = COALESCE(ad_banner_enabled, true)
WHERE ad_banner_enabled IS NULL;

-- 3단계: NOT NULL 제약 조건 추가
ALTER TABLE app_policy
ALTER COLUMN ad_banner_enabled SET NOT NULL;
```

### 3. Git 커밋 메시지

```bash
# 좋은 예
git commit -m "Add: NOT NULL constraints for ad columns

- Prevent null values in ad_banner_enabled
- Ensure clear ad state (true/false only)
- Add DB-level safety check

Closes #123"

# 나쁜 예
git commit -m "수정"
git commit -m "db"
git commit -m "테스트"
```

---

## 📚 문서 구조 예시

```
docs/
├── database/
│   ├── 01-initial-schema.sql         # 초기 스키마
│   ├── 02-add-ad-columns.sql         # 광고 컬럼 추가
│   ├── 03-add-not-null.sql           # NOT NULL 추가
│   └── README.md                     # 실행 순서 설명
├── guides/
│   ├── database-setup-guide.md       # DB 설정 가이드
│   └── sql-script-guide.md           # SQL 스크립트 가이드
└── README.md                          # 전체 문서 인덱스
```

---

## 🎉 결론

### "문서화 가능"의 진짜 의미

**Supabase가 저장해주는 게 아니라:**
```
내가 SQL 스크립트를 파일로 저장하고
Git으로 관리하는 것!
```

### 핵심 장점

1. **기록** - 무엇을 했는지 명확
2. **공유** - 팀원과 협업 쉬움
3. **재현** - 언제든 다시 실행
4. **일관성** - 모든 환경 동일
5. **안전** - 실수 방지 및 복구 가능

### 실천 방법

```
1. SQL 스크립트 작성 (.sql 파일)
2. 명확한 주석 추가
3. Git에 커밋
4. 팀원과 공유
5. 모든 환경에서 재사용
```

---

**작성일**: 2025-01-08  
**핵심 메시지**: SQL 스크립트 = 실행 가능한 문서! 📄✨

