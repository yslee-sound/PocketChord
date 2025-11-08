# 🔥 RLS 정책 문제 해결

## 현재 상황

✅ **데이터 확인됨**:
- `app_id: com.sweetapps.pocketchord.debug`
- `is_active: TRUE`
- `active_popup_type: emergency`

❌ **앱에서 조회 실패**:
```
D/AppPolicyRepo: Query returned 0 rows
```

**원인**: RLS (Row Level Security) 정책이 잘못 설정되었거나 없음

---

## 🎯 즉시 실행할 SQL

Supabase SQL Editor에서 순서대로 실행:

### 1단계: 현재 RLS 상태 확인

```sql
-- RLS 활성화 여부 확인
SELECT tablename, rowsecurity 
FROM pg_tables 
WHERE tablename = 'app_policy';
```

**예상 결과**:
```
tablename   | rowsecurity
------------|------------
app_policy  | true
```

---

### 2단계: 현재 정책 확인

```sql
-- 현재 정책 목록
SELECT policyname, cmd, qual 
FROM pg_policies 
WHERE tablename = 'app_policy';
```

**문제 시나리오**:
- 결과가 비어있음 → 정책이 없음
- `qual`이 잘못됨 → 정책이 잘못 설정됨

---

### 3단계: RLS 정책 재생성 (해결책)

```sql
-- 기존 정책 삭제
DROP POLICY IF EXISTS "allow_read_policy" ON app_policy;

-- 새 정책 생성
CREATE POLICY "allow_read_policy"
ON app_policy
FOR SELECT
USING (is_active = true);
```

---

### 4단계: 테스트 (임시)

만약 위 방법이 안 되면, 임시로 RLS를 완전히 비활성화:

```sql
-- ⚠️ 테스트 전용 - 프로덕션에서는 사용 금지!
ALTER TABLE app_policy DISABLE ROW LEVEL SECURITY;
```

---

## 📱 앱 재시작 및 확인

```cmd
adb shell am force-stop com.sweetapps.pocketchord.debug
adb shell am start -n com.sweetapps.pocketchord.debug/com.sweetapps.pocketchord.MainActivity
adb logcat -d -s AppPolicyRepo:* | findstr "Query returned"
```

**성공 시**:
```
D/AppPolicyRepo: Query returned 1 rows  ← 성공!
```

---

## 🔍 디버깅: RLS 우회 테스트

RLS가 원인인지 확인:

```sql
-- RLS 비활성화
ALTER TABLE app_policy DISABLE ROW LEVEL SECURITY;
```

앱 재시작 후:
- ✅ `Query returned 1 rows` → RLS 정책 문제 확정
- ❌ 여전히 0 rows → 다른 문제

**RLS 정책 문제가 확인되면**:

```sql
-- RLS 재활성화
ALTER TABLE app_policy ENABLE ROW LEVEL SECURITY;

-- 올바른 정책 생성
DROP POLICY IF EXISTS "allow_read_policy" ON app_policy;
CREATE POLICY "allow_read_policy"
ON app_policy
FOR SELECT
USING (is_active = true);
```

---

## ⚡ 빠른 해결 (한 번에 실행)

```sql
-- 모든 문제를 한 번에 해결
-- Supabase SQL Editor에서 전체 복사 후 실행

-- 1. 기존 정책 제거
DROP POLICY IF EXISTS "allow_read_policy" ON app_policy;

-- 2. RLS 활성화
ALTER TABLE app_policy ENABLE ROW LEVEL SECURITY;

-- 3. 새 정책 생성
CREATE POLICY "allow_read_policy"
ON app_policy
FOR SELECT
USING (is_active = true);

-- 4. 확인
SELECT policyname, cmd, qual 
FROM pg_policies 
WHERE tablename = 'app_policy';
```

**예상 결과**:
```
policyname        | cmd    | qual
------------------|--------|------------------
allow_read_policy | SELECT | (is_active = true)
```

---

## 📝 체크리스트

- [ ] SQL 1-2단계 실행 (현재 상태 확인)
- [ ] SQL 3단계 실행 (정책 재생성)
- [ ] 정책 확인 쿼리 실행
- [ ] 앱 재시작
- [ ] 로그에서 "Query returned 1 rows" 확인
- [ ] 화면에 팝업 표시 확인

---

## 🚀 결론

**문제**: RLS 정책이 없거나 잘못 설정됨  
**해결**: 위의 "빠른 해결" SQL을 Supabase SQL Editor에서 실행  
**소요 시간**: 30초

이제 위의 SQL을 실행하고 앱을 재시작하면 팝업이 표시됩니다! 🎉

