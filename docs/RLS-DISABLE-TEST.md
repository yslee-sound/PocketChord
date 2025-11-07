# 🚨 긴급: RLS 테스트 필요

## 현재 상황

Clean 빌드 후에도:
```
D/AppPolicyRepo: Query returned 0 rows
```

**결론**: RLS 정책이 데이터를 차단하고 있습니다!

---

## ⚡ 즉시 실행

### Supabase SQL Editor에서:

```sql
-- RLS 임시 비활성화
ALTER TABLE app_policy DISABLE ROW LEVEL SECURITY;
```

### 그 다음 바로 앱 재시작:

```cmd
adb shell am force-stop com.sweetapps.pocketchord.debug
adb shell am start -n com.sweetapps.pocketchord.debug/com.sweetapps.pocketchord.MainActivity
```

### 3초 후 로그 확인:

```cmd
timeout /t 3
adb logcat -d -s AppPolicyRepo:* | findstr "Query returned"
```

---

## 예상 결과

### ✅ 성공 시 (RLS가 원인)
```
D/AppPolicyRepo: Query returned 1 rows
D/AppPolicyRepo: ✅ Policy found:
```

→ **RLS 정책 설정에 문제가 있음**

### ❌ 실패 시 (여전히 0 rows)
```
D/AppPolicyRepo: Query returned 0 rows
```

→ **다른 문제 (app_id 불일치, 네트워크 등)**

---

## RLS 비활성화로 성공하면

### 원인 확인:

RLS 정책의 `USING` 조건을 확인:

```sql
SELECT policyname, cmd, qual 
FROM pg_policies 
WHERE tablename = 'app_policy';
```

현재 설정: `(is_active = true)`

### 문제 가능성:

1. **데이터 타입 불일치**: `is_active`가 boolean이 아닐 수 있음
2. **대소문자 문제**: PostgreSQL은 대소문자를 구분

### 해결:

```sql
-- RLS 재활성화
ALTER TABLE app_policy ENABLE ROW LEVEL SECURITY;

-- 정책 재생성 (더 관대한 조건)
DROP POLICY IF EXISTS "allow_read_policy" ON app_policy;

CREATE POLICY "allow_read_policy"
ON app_policy
FOR SELECT
USING (true);  -- 모든 행 허용 (테스트용)
```

**테스트 후 성공하면**:

```sql
-- 원래대로 수정
DROP POLICY IF EXISTS "allow_read_policy" ON app_policy;

CREATE POLICY "allow_read_policy"
ON app_policy
FOR SELECT
USING (is_active = true);
```

---

## 대안: app_id 컬럼 타입 확인

```sql
-- 컬럼 정보 확인
SELECT 
    column_name, 
    data_type,
    character_maximum_length 
FROM information_schema.columns 
WHERE table_name = 'app_policy';
```

---

## 빠른 명령어 모음

```cmd
# 1. Supabase: RLS 비활성화 실행

# 2. 앱 재시작
adb shell am force-stop com.sweetapps.pocketchord.debug && adb shell am start -n com.sweetapps.pocketchord.debug/com.sweetapps.pocketchord.MainActivity

# 3. 로그 확인 (3초 후)
timeout /t 3 && adb logcat -d -s AppPolicyRepo:* HomeScreen:*
```

---

**다음 단계**: 위의 RLS 비활성화 SQL을 실행하고 즉시 앱을 재시작하여 결과를 확인해주세요!

