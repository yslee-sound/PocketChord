# 🎯 긴급 팝업 표시 - 즉시 실행 가이드

## 현재 상태 ✅

**API 설정**: 정상 ✅
- SUPABASE_URL: `https://bajurdtglfaiqilnpamt.supabase.co`
- SUPABASE_ANON_KEY: 설정됨 ✅
- 앱-Supabase 연결: 성공 ✅

**문제**: 데이터베이스에 정책 데이터가 없음 ❌

---

## ⚡ 즉시 실행할 3단계

### 1️⃣ Supabase 웹사이트 접속

브라우저에서 접속:
```
https://supabase.com/dashboard/project/bajurdtglfaiqilnpamt
```

로그인 후 **SQL Editor** 클릭

---

### 2️⃣ SQL 쿼리 실행

아래 쿼리를 복사해서 SQL Editor에 붙여넣고 **RUN** 클릭:

```sql
-- 긴급 팝업 정책 생성
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
    '🚨 긴급 점검 안내: 서버 점검이 진행 중입니다. 잠시 후 다시 시도해주세요.',
    'https://example.com/status'
)
ON CONFLICT (app_id) 
DO UPDATE SET
    is_active = TRUE,
    active_popup_type = 'emergency',
    content = '🚨 긴급 점검 안내: 서버 점검이 진행 중입니다. 잠시 후 다시 시도해주세요.',
    download_url = 'https://example.com/status';
```

**실행 결과**:
```
Success. No rows returned
```
또는
```
INSERT 0 1
```

---

### 3️⃣ 확인 쿼리 실행

같은 SQL Editor에서 확인:

```sql
-- 정책이 제대로 생성되었는지 확인
SELECT * FROM app_policy 
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**예상 결과**:
```
id | app_id                           | is_active | active_popup_type | content
1  | com.sweetapps.pocketchord.debug | true      | emergency         | 🚨 긴급 점검...
```

이 결과가 보이면 **성공!** ✅

---

## 📱 앱에서 확인

SQL 실행 후 즉시:

```cmd
adb shell am force-stop com.sweetapps.pocketchord.debug
adb shell am start -n com.sweetapps.pocketchord.debug/com.sweetapps.pocketchord.MainActivity
```

또는 **앱을 수동으로 재시작**하세요.

---

## 🎉 성공 확인

앱을 열면:
- ✅ 화면에 긴급 팝업이 즉시 표시됨
- ✅ 제목: "🚨 긴급 공지"
- ✅ 내용: "🚨 긴급 점검 안내: 서버 점검이 진행 중입니다..."
- ✅ 확인 버튼 있음
- ✅ X 버튼 없음 (닫을 수 없음)

---

## ❓ SQL 실행 중 에러가 발생하면

### 에러 1: `relation "app_policy" does not exist`

**의미**: 테이블이 아예 없음

**해결**: 테이블 생성 SQL 실행 (docs/supabase-app-policy-hybrid.md 참고)

간단 버전:
```sql
-- ENUM 생성
CREATE TYPE popup_type AS ENUM ('emergency', 'force_update', 'optional_update', 'notice', 'none');

-- 테이블 생성
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

-- RLS 활성화
ALTER TABLE app_policy ENABLE ROW LEVEL SECURITY;

CREATE POLICY "allow_read_policy"
ON app_policy FOR SELECT
USING (is_active = TRUE);
```

그 다음 위의 INSERT 쿼리 재실행.

---

### 에러 2: `invalid input value for enum popup_type: "emergency"`

**의미**: ENUM 타입이 없음

**해결**:
```sql
CREATE TYPE popup_type AS ENUM ('emergency', 'force_update', 'optional_update', 'notice', 'none');
```

그 다음 INSERT 재실행.

---

## 🔍 로그로 확인

```cmd
adb logcat -d -s AppPolicyRepo:* HomeScreen:*
```

**성공 시 로그**:
```
D/AppPolicyRepo: Query returned 1 rows          ← 정책 조회 성공!
D/AppPolicyRepo: ✅ Policy found:
D/AppPolicyRepo:   - active_popup_type: emergency
D/HomeScreen: Decision: EMERGENCY popup will show
```

---

## 📝 요약

| 항목 | 상태 |
|------|------|
| API 키/URL | ✅ 정상 |
| Supabase 연결 | ✅ 성공 |
| 정책 데이터 | ❌ 없음 → **SQL 실행 필요** |

**해결 방법**: 위의 INSERT SQL 쿼리를 Supabase SQL Editor에서 실행하면 끝!

**소요 시간**: 2분

---

## 🚀 다음 단계

1. ✅ Supabase 웹사이트 접속
2. ✅ SQL Editor 열기
3. ✅ INSERT 쿼리 실행
4. ✅ SELECT로 확인
5. ✅ 앱 재시작
6. ✅ 팝업 확인

**모든 설정이 정상입니다. SQL만 실행하면 됩니다!** 🎯

