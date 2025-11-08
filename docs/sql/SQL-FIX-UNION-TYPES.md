# SQL 오류 수정 완료

**수정일**: 2025-11-09  
**파일**: `RELEASE-TEST-CHECKLIST.md`  
**오류**: UNION 타입 불일치

---

## 🎯 이 SQL의 목적

### 무엇을 확인하는가?

**4개 팝업 정책 테이블의 현재 상태를 한눈에 확인**하기 위한 SQL입니다.

```
목적: 릴리즈 테스트를 시작하기 전에
      각 팝업이 어떤 상태인지 빠르게 파악
```

### 왜 필요한가?

릴리즈 테스트를 시작할 때, 다음을 알아야 합니다:

1. **어떤 팝업이 활성화되어 있는가?**
   - emergency가 켜져 있으면 → 다른 팝업이 안 보임
   - update가 높은 버전이면 → 업데이트 팝업이 뜸
   - notice가 활성화되어 있으면 → 공지 팝업이 뜸

2. **현재 설정이 무엇인가?**
   - update: 목표 버전이 얼마인지, 강제인지 선택인지
   - notice: 현재 버전이 몇 번인지
   - emergency: 활성화되어 있는지

3. **테스트 시작 전 상태를 기록**
   - 테스트 후 원래대로 복구하기 위해
   - 어떤 상태에서 시작했는지 문서화

---

## 📊 실제 사용 예시

### 시나리오 1: 테스트 시작 전

```sql
-- 이 SQL을 실행하면:
SELECT 'emergency_policy' as table_name, 
       CAST(is_active AS TEXT) as is_active, 
       LEFT(content, 30) as content_preview 
FROM emergency_policy 
WHERE app_id = 'com.sweetapps.pocketchord'
UNION ALL
...
```

**결과**:
```
table_name          | is_active | content_preview
--------------------+-----------+----------------------------------
emergency_policy    | false     | ⚠️ [테스트] 이 앱은...
update_policy       | true      | target:1 force:false
notice_policy       | true      | v1: 환영합니다! 🎉
```

**이것의 의미**:
- ✅ emergency: **꺼져있음** (정상, 평상시)
- ✅ update: **켜져있지만** target=1 (현재 버전과 같거나 낮음 → 팝업 안 뜸)
- ✅ notice: **켜져있음**, 버전 1 (신규 사용자에게 표시됨)

**판단**: 
```
✅ 테스트 가능한 상태!
- emergency 테스트 시작 가능 (비활성화 상태)
- update 테스트 시작 가능 (낮은 버전)
- notice 테스트 시작 가능 (버전 1)
```

---

### 시나리오 2: 예상치 못한 상태 발견

**결과**:
```
table_name          | is_active | content_preview
--------------------+-----------+----------------------------------
emergency_policy    | true      | 🚨 긴급! 서비스 종료...
update_policy       | true      | target:999 force:true
notice_policy       | true      | v5: 11월 이벤트
```

**이것의 의미**:
- ⚠️ emergency: **켜져있음!** (테스트 중이거나 실제 긴급 상황)
- ⚠️ update: target=999 (매우 높음 → 모든 사용자에게 팝업)
- ⚠️ notice: 버전 5 (이미 여러 번 업데이트됨)

**판단**:
```
⚠️ 테스트 시작 전 정리 필요!
1. emergency를 꺼야 함 (아니면 다른 팝업이 안 보임)
2. update target을 낮춰야 함 (아니면 계속 업데이트 팝업)
3. notice 버전을 확인 필요 (SharedPreferences에 v5까지 저장되어 있을 수 있음)
```

---

## 💡 왜 UNION으로 합쳐서 보는가?

### 3개 테이블을 하나의 결과로

**이유 1: 한눈에 비교**
```
따로 보면:
SELECT * FROM emergency_policy; → 스크롤
SELECT * FROM update_policy;    → 스크롤
SELECT * FROM notice_policy;    → 스크롤
→ 불편함! 비교하기 어려움

UNION으로 합치면:
→ 한 화면에 3개 모두 표시
→ 상태 비교 쉬움
```

**이유 2: 릴리즈 테스트 체크리스트에서 사용**
```
체크리스트 흐름:
1. ✅ 이 SQL 실행 → 현재 상태 확인
2. ✅ 상태 기록
3. ✅ 테스트 시작
4. ✅ 테스트 완료 후 이 SQL 다시 실행 → 원래대로 복구 확인
```

**이유 3: 문서화**
```
테스트 리포트:
- 시작 시 상태: emergency=false, update=1, notice=v1
- 테스트 완료 후 상태: emergency=false, update=1, notice=v1
- ✅ 원래 상태로 복구 확인!
```

---

## 🔍 각 컬럼의 의미

### emergency_policy
```sql
LEFT(content, 30) as content_preview
```
- **확인**: 어떤 긴급 메시지가 설정되어 있는지
- **이유**: 실수로 테스트용 메시지가 프로덕션에 있는지 확인

### update_policy
```sql
CONCAT('target:', target_version_code, ' force:', is_force_update)
```
- **target**: 목표 버전 (현재 앱 버전과 비교)
- **force**: true면 강제, false면 선택적
- **확인**: 
  - target=1 → 업데이트 팝업 안 뜸 (정상)
  - target=999 → 업데이트 팝업 뜸 (테스트 중?)

### notice_policy
```sql
CONCAT('v', notice_version, ': ', LEFT(title, 20))
```
- **v1**: 버전 1
- **title**: 어떤 공지인지 제목
- **확인**:
  - v1 → 신규 사용자에게만 표시
  - v5 → 여러 번 업데이트된 공지

---

## ✅ 실전 사용 흐름

### Step 1: 테스트 시작 전
```sql
-- 이 SQL 실행
SELECT 'emergency_policy' ...

-- 결과 복사해서 기록
emergency_policy | false | ⚠️ [테스트]...
update_policy    | true  | target:1 force:false
notice_policy    | true  | v1: 환영합니다!
```

### Step 2: 테스트 진행
```
- emergency 테스트: is_active를 true로 변경
- update 테스트: target을 999로 변경
- notice 테스트: version을 2로 변경
```

### Step 3: 테스트 완료 후
```sql
-- 같은 SQL 다시 실행
SELECT 'emergency_policy' ...

-- 결과 확인
emergency_policy | false | ⚠️ [테스트]... ✅ 복구됨!
update_policy    | true  | target:1 force:false ✅ 복구됨!
notice_policy    | true  | v1: 환영합니다! ✅ 복구됨!
```

---

## 🎯 요약

### 이 SQL의 역할

| 역할 | 설명 |
|------|------|
| **상태 스냅샷** | 현재 4개 팝업의 상태를 찍어둠 |
| **테스트 준비 확인** | 테스트 가능한 상태인지 판단 |
| **비교 기준** | 테스트 후 원래대로 복구되었는지 비교 |
| **문서화** | 테스트 리포트에 상태 기록 |

### 왜 체크리스트에 포함되었는가?

```
릴리즈 테스트 = 4개 팝업을 모두 테스트

문제: 테스트 중에 설정을 바꿈
     → emergency를 켜고, update를 999로 올리고...
     → 테스트 끝나면 원래대로 돌려놔야 함

해결: 
1. 시작 전: 이 SQL로 현재 상태 기록 📸
2. 테스트: 마음껏 바꿔가며 테스트
3. 완료 후: 이 SQL로 다시 확인 → 원래대로 복구 ✅
```

---

## 🔴 발생한 오류

```
ERROR: 42804: UNION types boolean and text cannot be matched
LINE 7: SELECT 'update_policy', CAST(is_active AS TEXT),
```

**원인**: PostgreSQL UNION에서 모든 SELECT문의 컬럼 타입이 일치해야 함
- 첫 번째 SELECT: `is_active` (BOOLEAN)
- 두 번째 SELECT: `CAST(is_active AS TEXT)` (TEXT)
- 타입 불일치!

---

## ✅ 수정 내용

### 1. 초기 상태 확인 SQL (사전 준비 섹션)

**Before**:
```sql
SELECT 'emergency_policy' as table_name, is_active,  -- ❌ BOOLEAN
       LEFT(content, 30) as content_preview 
FROM emergency_policy
```

**After**:
```sql
SELECT 'emergency_policy' as table_name, 
       CAST(is_active AS TEXT) as is_active,  -- ✅ TEXT
       LEFT(content, 30) as content_preview 
FROM emergency_policy
```

---

### 2. 최종 상태 확인 SQL (최종 확인 섹션)

**Before**:
```sql
SELECT 
    'emergency_policy' as policy,
    is_active,        -- ❌ BOOLEAN
    is_dismissible,   -- ❌ BOOLEAN
    LEFT(content, 30) as preview
...
UNION ALL
SELECT 
    'notice_policy',
    CAST(is_active AS TEXT),
    NULL,             -- ❌ NULL
    ...
```

**After**:
```sql
SELECT 
    'emergency_policy' as policy,
    CAST(is_active AS TEXT) as is_active,     -- ✅ TEXT
    CAST(is_dismissible AS TEXT) as detail,   -- ✅ TEXT
    LEFT(content, 30) as preview
...
UNION ALL
SELECT 
    'notice_policy',
    CAST(is_active AS TEXT),
    CAST(NULL AS TEXT),                       -- ✅ TEXT
    ...
```

---

## 📊 타입 일치 규칙

### UNION 요구사항
```sql
-- ❌ 잘못된 예
SELECT 1, TRUE, 'text'    -- INT, BOOLEAN, TEXT
UNION ALL
SELECT 2, 'yes', 'text'   -- INT, TEXT, TEXT (BOOLEAN ≠ TEXT)

-- ✅ 올바른 예
SELECT 1, CAST(TRUE AS TEXT), 'text'    -- INT, TEXT, TEXT
UNION ALL
SELECT 2, 'yes', 'text'                 -- INT, TEXT, TEXT (모두 일치)
```

---

## 🧪 테스트

### 수정된 SQL 실행
```sql
-- 초기 상태 확인 (사전 준비 섹션)
SELECT 'emergency_policy' as table_name, 
       CAST(is_active AS TEXT) as is_active, 
       LEFT(content, 30) as content_preview 
FROM emergency_policy 
WHERE app_id = 'com.sweetapps.pocketchord'
UNION ALL
SELECT 'update_policy', 
       CAST(is_active AS TEXT), 
       CONCAT('target:', target_version_code, ' force:', is_force_update)
FROM update_policy 
WHERE app_id = 'com.sweetapps.pocketchord'
UNION ALL
SELECT 'notice_policy', 
       CAST(is_active AS TEXT), 
       CONCAT('v', notice_version, ': ', LEFT(title, 20))
FROM notice_policy 
WHERE app_id = 'com.sweetapps.pocketchord';
```

**예상 결과**:
```
table_name          | is_active | content_preview
--------------------+-----------+------------------
emergency_policy    | false     | ⚠️ [테스트]...
update_policy       | true      | target:1 force:false
notice_policy       | true      | v1: 환영합니다! 🎉
```

---

## ✅ 수정 완료

- ✅ `RELEASE-TEST-CHECKLIST.md` 수정
- ✅ 초기 상태 확인 SQL 수정
- ✅ 최종 상태 확인 SQL 수정
- ✅ 모든 BOOLEAN → TEXT 캐스팅
- ✅ NULL → CAST(NULL AS TEXT)

---

## 🚀 이제 실행 가능

체크리스트의 모든 SQL이 오류 없이 실행됩니다!

**테스트**: Supabase SQL Editor에서 실행하세요!

