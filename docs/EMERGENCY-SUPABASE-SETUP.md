# 🚨 긴급: Supabase 테이블 생성 및 데이터 입력

## 현재 문제

앱 로그:
```
D/AppPolicyRepo: Query returned 0 rows
W/AppPolicyRepo: ❌ No policy found!
```

**원인**: Supabase 데이터베이스에 정책 데이터가 없음

---

## 🎯 해결 방법 (3가지 시나리오)

### ✅ 시나리오 1: 테이블은 있지만 데이터가 없는 경우

Supabase SQL Editor에서 실행:

```sql
INSERT INTO app_policy (
    app_id,
    is_active,
    active_popup_type,
    content,
    download_url
) VALUES (
    'com.sweetapps.pocketchord.debug',
    TRUE,
    'emergency',
    '🚨 긴급 점검 안내: 서버 점검이 진행 중입니다.',
    'https://example.com/status'
)
ON CONFLICT (app_id) 
DO UPDATE SET
    is_active = TRUE,
    active_popup_type = 'emergency',
    content = '🚨 긴급 점검 안내: 서버 점검이 진행 중입니다.',
    download_url = 'https://example.com/status';
```

---

### ✅ 시나리오 2: 테이블이 없는 경우

에러 메시지: `relation "app_policy" does not exist`

**전체 SQL을 한 번에 실행** (Supabase SQL Editor):

```sql
-- 1. ENUM 타입 생성
DROP TYPE IF EXISTS popup_type CASCADE;
CREATE TYPE popup_type AS ENUM (
  'emergency',
  'force_update',
  'optional_update',
  'notice',
  'none'
);

-- 2. 테이블 생성
DROP TABLE IF EXISTS app_policy CASCADE;
CREATE TABLE app_policy (
  id BIGSERIAL PRIMARY KEY,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  app_id TEXT NOT NULL UNIQUE,
  is_active BOOLEAN NOT NULL DEFAULT FALSE,
  active_popup_type popup_type NOT NULL DEFAULT 'none',
  content TEXT,
  download_url TEXT,
  min_supported_version INTEGER,
  latest_version_code INTEGER
);

-- 3. RLS 활성화
ALTER TABLE app_policy ENABLE ROW LEVEL SECURITY;

-- 4. 읽기 정책 생성
DROP POLICY IF EXISTS "allow_read_policy" ON app_policy;
CREATE POLICY "allow_read_policy"
ON app_policy
FOR SELECT
USING (is_active = TRUE);

-- 5. 초기 데이터 생성 (긴급 팝업 활성화)
INSERT INTO app_policy (
    app_id,
    is_active,
    active_popup_type,
    content,
    download_url
) VALUES (
    'com.sweetapps.pocketchord.debug',
    TRUE,
    'emergency',
    '🚨 긴급 점검 안내: 서버 점검이 진행 중입니다.',
    'https://example.com/status'
);

-- 릴리즈 버전용 (비활성 상태로 생성)
INSERT INTO app_policy (
    app_id,
    is_active,
    active_popup_type
) VALUES (
    'com.sweetapps.pocketchord',
    FALSE,
    'none'
);
```

---

### ✅ 시나리오 3: Supabase Table Editor 사용 (GUI)

1. Supabase Dashboard → **Table Editor**
2. 왼쪽에서 **"app_policy"** 테이블 선택
3. **"Insert row"** 버튼 클릭
4. 값 입력:
   - `app_id`: `com.sweetapps.pocketchord.debug`
   - `is_active`: `TRUE` (체크박스 선택)
   - `active_popup_type`: `emergency`
   - `content`: `🚨 긴급 점검 안내: 서버 점검이 진행 중입니다.`
   - `download_url`: `https://example.com/status`
5. **Save** 클릭

---

## 📱 확인 방법

### 1. SQL로 확인
```sql
SELECT * FROM app_policy 
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**예상 결과**:
```
id | app_id                           | is_active | active_popup_type | content
1  | com.sweetapps.pocketchord.debug | true      | emergency         | 🚨 긴급 점검...
```

### 2. 앱 재시작
```cmd
adb shell am force-stop com.sweetapps.pocketchord.debug
adb shell am start -n com.sweetapps.pocketchord.debug/com.sweetapps.pocketchord.MainActivity
```

### 3. 로그 확인
```cmd
adb logcat -d -s AppPolicyRepo:* | findstr "Query returned"
```

**성공 시**:
```
D/AppPolicyRepo: Query returned 1 rows  ← 성공!
D/AppPolicyRepo: ✅ Policy found:
```

**실패 시** (여전히):
```
D/AppPolicyRepo: Query returned 0 rows  ← 여전히 실패
```

---

## ⚠️ 여전히 0 rows면?

### RLS 정책 문제일 가능성

**임시 테스트: RLS 비활성화**

```sql
-- ⚠️ 테스트 전용!
ALTER TABLE app_policy DISABLE ROW LEVEL SECURITY;
```

앱 재시작 후:
- ✅ 정책이 조회되면 → RLS 정책 문제
- ❌ 여전히 안 되면 → 데이터 자체가 없거나 app_id 불일치

**테스트 후 반드시 재활성화**:
```sql
ALTER TABLE app_policy ENABLE ROW LEVEL SECURITY;
```

---

## 🔍 app_id 불일치 체크

```sql
-- 현재 DB의 모든 app_id 확인
SELECT 
    id, 
    app_id,
    length(app_id) as len,
    is_active,
    active_popup_type
FROM app_policy;
```

앱이 찾는 값: `com.sweetapps.pocketchord.debug` (35자)

만약 다르면 수정:
```sql
UPDATE app_policy 
SET app_id = 'com.sweetapps.pocketchord.debug'
WHERE id = 1;
```

---

## 📝 체크리스트

SQL 실행 전:
- [ ] Supabase 대시보드 접속 완료
- [ ] SQL Editor 열림
- [ ] SQL 복사 완료

SQL 실행 후:
- [ ] "Success" 또는 "INSERT 0 1" 메시지 확인
- [ ] SELECT 쿼리로 데이터 확인 (1 row)
- [ ] 앱 재시작
- [ ] 로그에서 "Query returned 1 rows" 확인
- [ ] 화면에 팝업 표시 확인

---

## 🚀 빠른 링크

- Supabase 프로젝트: https://supabase.com/dashboard/project/bajurdtglfaiqilnpamt
- SQL Editor 직접 접속: https://supabase.com/dashboard/project/bajurdtglfaiqilnpamt/sql/new

---

**중요**: API 키와 URL은 정상입니다. Supabase에 데이터만 추가하면 즉시 해결됩니다!

