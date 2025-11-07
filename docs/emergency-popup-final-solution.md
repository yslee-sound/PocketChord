# ✅ 긴급 팝업 문제 - 최종 해결 방법

## 진단 결과

**앱 로그 분석**:
```
D/AppPolicyRepo: Query returned 0 rows  ← 정책이 조회되지 않음!
W/AppPolicyRepo: ❌ No policy found!
```

**원인**: Supabase `app_policy` 테이블에 정책이 없거나 `is_active = FALSE`

---

## 즉시 실행할 SQL 쿼리

### ✅ 해결 방법: Supabase SQL Editor에서 아래 쿼리 실행

```sql
-- 1단계: 정책 생성 또는 업데이트 (한 번에 해결)
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

### ✅ 확인 쿼리

```sql
-- 정책이 제대로 생성되었는지 확인
SELECT 
    id,
    app_id,
    is_active,
    active_popup_type,
    content,
    created_at
FROM app_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**예상 결과**:
```
| id | app_id                              | is_active | active_popup_type | content          |
|----|-------------------------------------|-----------|-------------------|------------------|
| 1  | com.sweetapps.pocketchord.debug     | TRUE      | emergency         | 🚨 긴급 점검...  |
```

---

## 앱 재시작 및 테스트

### 1. 앱 재시작
```cmd
adb shell am force-stop com.sweetapps.pocketchord.debug
adb shell am start -n com.sweetapps.pocketchord.debug/com.sweetapps.pocketchord.MainActivity
```

### 2. 로그 확인
```cmd
adb logcat -d -s AppPolicyRepo:* HomeScreen:* | findstr /C:"Policy found" /C:"EMERGENCY"
```

**성공 시 예상 로그**:
```
D/AppPolicyRepo: Query returned 1 rows
D/AppPolicyRepo: ✅ Policy found:
D/AppPolicyRepo:   - id: 1
D/AppPolicyRepo:   - app_id: com.sweetapps.pocketchord.debug
D/AppPolicyRepo:   - is_active: true
D/AppPolicyRepo:   - active_popup_type: emergency
D/HomeScreen: ===== Policy Loaded Successfully =====
D/HomeScreen: Policy active_popup_type: emergency
D/HomeScreen: Decision: EMERGENCY popup will show
```

### 3. UI 확인
- ✅ 긴급 팝업이 화면에 즉시 표시됨
- ✅ 제목: "🚨 긴급 공지"
- ✅ 내용: "🚨 긴급 점검 안내: 서버 점검이 진행 중입니다..."
- ✅ 확인 버튼 클릭 시 `https://example.com/status` 이동
- ✅ X 버튼 없음 (닫기 불가)

---

## 문제가 계속되면

### 문제 1: SQL 쿼리 실행 후에도 조회 안 됨

**원인**: RLS 정책이 차단

**해결**:
```sql
-- RLS 정책 확인
SELECT policyname, cmd, qual 
FROM pg_policies 
WHERE tablename = 'app_policy';

-- 정책이 없거나 잘못되어 있으면 재생성
ALTER TABLE app_policy ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "allow_read_policy" ON app_policy;
CREATE POLICY "allow_read_policy"
ON app_policy
FOR SELECT
USING (is_active = TRUE);
```

### 문제 2: app_id 불일치

**확인**:
```sql
-- 현재 데이터베이스의 app_id 확인
SELECT app_id, length(app_id) as len FROM app_policy;

-- 예상: com.sweetapps.pocketchord.debug (35자)
```

앱에서 기대하는 값: `com.sweetapps.pocketchord.debug`  
데이터베이스의 실제 값: (위 쿼리 결과 확인)

만약 다르면:
```sql
-- app_id 수정
UPDATE app_policy 
SET app_id = 'com.sweetapps.pocketchord.debug'
WHERE id = 1;  -- 또는 적절한 id
```

### 문제 3: SUPABASE_ANON_KEY 문제

**확인**:
```cmd
adb logcat -d -s PocketChordApp:* | findstr "Supabase"
```

**예상**:
```
I/PocketChordApp: Supabase configured: url set
```

**문제 발생 시**:
```
W/PocketChordApp: Supabase 미설정
```

**해결**: `local.properties` 파일 확인
```properties
SUPABASE_URL=https://bajurdtglfaiqilnpamt.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 빠른 테스트 (RLS 우회)

**임시로 RLS를 비활성화하여 정책이 보이는지 테스트**:

```sql
-- ⚠️ 테스트 전용 - 프로덕션에서는 절대 사용 금지!
ALTER TABLE app_policy DISABLE ROW LEVEL SECURITY;
```

앱 재시작 후 정책이 조회되면:
- ✅ **원인**: RLS 정책 문제
- ✅ **해결**: RLS 정책 재생성 (위의 문제 1 참고)

**테스트 후 반드시 재활성화**:
```sql
ALTER TABLE app_policy ENABLE ROW LEVEL SECURITY;
```

---

## 전체 재설정 (최후의 수단)

모든 것이 실패하면 테이블을 완전히 재생성:

```sql
-- ⚠️ 경고: 모든 데이터가 삭제됩니다!
DROP TABLE IF EXISTS app_policy CASCADE;

-- docs/supabase-app-policy-hybrid.md의 전체 SQL 재실행
-- (테이블 생성 + RLS 정책 + 초기 데이터)
```

---

## 성공 체크리스트

- [ ] Supabase SQL Editor에서 INSERT 쿼리 실행 완료
- [ ] `SELECT` 쿼리로 정책 확인 (`is_active = TRUE`)
- [ ] RLS 정책 확인 (`allow_read_policy` 존재)
- [ ] 앱 재시작
- [ ] 로그에서 "✅ Policy found" 확인
- [ ] 로그에서 "Decision: EMERGENCY popup will show" 확인
- [ ] 화면에 긴급 팝업 표시 확인

---

## 요약

**현재 상태**: 정책이 데이터베이스에 없거나 비활성화됨  
**해결 방법**: 위의 INSERT 쿼리 실행  
**소요 시간**: 1분 이내  
**예상 결과**: 즉시 긴급 팝업 표시

**작성일**: 2025-11-08  
**상태**: 🎯 해결 방법 제공 완료

---

## 다음 단계

1. ✅ Supabase SQL Editor 열기
2. ✅ 위의 INSERT 쿼리 실행
3. ✅ SELECT 쿼리로 확인
4. ✅ 앱 재시작
5. ✅ 긴급 팝업 확인

이제 위의 SQL 쿼리를 실행하면 즉시 문제가 해결됩니다! 🚀

