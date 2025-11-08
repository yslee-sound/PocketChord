# 긴급 팝업 표시 안 됨 - 진단 및 해결

## 현재 상태
**로그 분석 결과**:
```
D/AppPolicyRepo: No active policy found for app_id=com.sweetapps.pocketchord.debug (RLS may be filtering)
W/HomeScreen: No active policy row for app_id='com.sweetapps.pocketchord.debug'
```

✅ **Supabase 연결**: 정상  
✅ **앱 버전**: 2  
✅ **app_id**: `com.sweetapps.pocketchord.debug`  
❌ **정책 조회**: 실패 (정책이 없거나 RLS가 차단)

---

## 즉시 실행할 SQL 쿼리

### 1️⃣ 정책 존재 여부 확인
Supabase SQL Editor에서 실행:

```sql
-- RLS를 우회하여 모든 정책 조회 (관리자 권한)
SELECT 
    id,
    app_id,
    is_active,
    active_popup_type,
    content,
    download_url,
    created_at
FROM app_policy;
```

**예상 결과**:
- 행이 없으면 → **정책 레코드가 아예 없음**
- 행이 있지만 `is_active = FALSE` → **정책이 비활성화됨**
- 행이 있고 `is_active = TRUE` → **RLS 정책 문제**

---

### 2️⃣ 정책이 없는 경우 - 생성
```sql
-- 테스트용 정책 생성
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
ON CONFLICT (app_id) DO UPDATE SET
    is_active = TRUE,
    active_popup_type = 'emergency',
    content = '🚨 긴급 점검 안내: 서버 점검이 진행 중입니다. 잠시 후 다시 시도해주세요.',
    download_url = 'https://example.com/status';
```

---

### 3️⃣ 정책이 있지만 비활성화된 경우 - 활성화
```sql
UPDATE app_policy
SET 
    is_active = TRUE,
    active_popup_type = 'emergency',
    content = '🚨 긴급 점검 안내: 서버 점검이 진행 중입니다.',
    download_url = 'https://example.com/status'
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

---

### 4️⃣ RLS 정책 확인
```sql
-- RLS 활성화 여부 확인
SELECT 
    schemaname, 
    tablename, 
    rowsecurity 
FROM pg_tables 
WHERE tablename = 'app_policy';

-- 예상: rowsecurity = true
```

```sql
-- RLS 정책 목록 확인
SELECT 
    policyname, 
    cmd, 
    qual 
FROM pg_policies 
WHERE tablename = 'app_policy';

-- 예상: 
-- policyname: allow_read_policy
-- cmd: SELECT
-- qual: (is_active = true)
```

---

### 5️⃣ RLS 정책이 없는 경우 - 생성
```sql
-- RLS 활성화
ALTER TABLE app_policy ENABLE ROW LEVEL SECURITY;

-- 읽기 정책 생성
DROP POLICY IF EXISTS "allow_read_policy" ON app_policy;
CREATE POLICY "allow_read_policy"
ON app_policy
FOR SELECT
USING (is_active = TRUE);
```

---

## 테스트 절차

### Step 1: SQL 실행
위의 쿼리를 Supabase SQL Editor에서 실행하여 정책을 생성/활성화

### Step 2: 앱 재시작
```cmd
adb shell am force-stop com.sweetapps.pocketchord.debug
adb logcat -c
adb logcat -s HomeScreen:* AppPolicyRepo:* PocketChordApp:*
```

다른 터미널에서:
```cmd
adb shell am start -n com.sweetapps.pocketchord.debug/com.sweetapps.pocketchord.MainActivity
```

### Step 3: 로그 확인
**성공 시 예상 로그**:
```
D/AppPolicyRepo: ===== Policy Fetch Started =====
D/AppPolicyRepo: Target app_id: com.sweetapps.pocketchord.debug
D/AppPolicyRepo: Query returned 1 rows
D/AppPolicyRepo: ✅ Policy found:
D/AppPolicyRepo:   - id: 1
D/AppPolicyRepo:   - app_id: com.sweetapps.pocketchord.debug
D/AppPolicyRepo:   - is_active: true
D/AppPolicyRepo:   - active_popup_type: emergency
D/AppPolicyRepo:   - content: 🚨 긴급 점검 안내...
D/HomeScreen: ===== Policy Loaded Successfully =====
D/HomeScreen: Current app version: 2
D/HomeScreen: Policy active_popup_type: emergency
D/HomeScreen: Decision: EMERGENCY popup will show
```

---

## 자주 발생하는 문제

### 문제 1: app_id 불일치
**증상**: SQL에서는 보이는데 앱에서는 안 보임

**해결**:
```sql
-- 정확한 app_id 확인
SELECT app_id, length(app_id), is_active 
FROM app_policy;

-- 예상: com.sweetapps.pocketchord.debug (35자)
```

공백이나 특수문자가 있는지 확인!

### 문제 2: is_active = FALSE
**증상**: 정책은 있지만 RLS가 차단

**해결**:
```sql
UPDATE app_policy 
SET is_active = TRUE 
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

### 문제 3: RLS 정책이 없음
**증상**: SQL Editor에서는 보이지만 앱에서는 안 보임

**해결**: 위의 5️⃣ 실행

### 문제 4: anon key 문제
**증상**: 모든 쿼리가 실패

**해결**:
1. Supabase 프로젝트 Settings → API
2. `anon` `public` key 복사
3. `local.properties` 업데이트:
   ```properties
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_ANON_KEY=your-anon-key-here
   ```
4. 앱 재빌드

---

## 빠른 진단 체크리스트

Supabase SQL Editor에서 순서대로 실행:

```sql
-- ✅ 1. 테이블 존재 확인
SELECT COUNT(*) FROM app_policy;

-- ✅ 2. app_id 확인
SELECT app_id FROM app_policy;

-- ✅ 3. is_active 확인
SELECT app_id, is_active FROM app_policy;

-- ✅ 4. 전체 정보 확인
SELECT * FROM app_policy 
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- ✅ 5. RLS 확인
SELECT tablename, rowsecurity 
FROM pg_tables 
WHERE tablename = 'app_policy';
```

---

## 강제 테스트 (RLS 우회)

**임시로 RLS를 비활성화하여 테스트**:

```sql
-- ⚠️ 테스트 전용 - 프로덕션에서는 절대 사용 금지!
ALTER TABLE app_policy DISABLE ROW LEVEL SECURITY;
```

앱 재시작 후 정책이 조회되면:
- ✅ **원인**: RLS 정책 문제
- ✅ **해결**: RLS 정책 재생성 (위의 5️⃣)

테스트 후 반드시 재활성화:
```sql
ALTER TABLE app_policy ENABLE ROW LEVEL SECURITY;
```

---

## 최종 확인 명령어

```cmd
# 1. 앱 완전 종료
adb shell am force-stop com.sweetapps.pocketchord.debug

# 2. 로그 초기화
adb logcat -c

# 3. 로그 시작 (터미널 1)
adb logcat -s HomeScreen:* AppPolicyRepo:* PocketChordApp:*

# 4. 앱 실행 (터미널 2)
adb shell am start -n com.sweetapps.pocketchord.debug/com.sweetapps.pocketchord.MainActivity
```

---

## 다음 단계

1. ✅ 위의 SQL 쿼리 실행
2. ✅ 정책 생성/활성화 확인
3. ✅ 앱 재시작
4. ✅ 로그에서 "✅ Policy found" 확인
5. ✅ 긴급 팝업 표시 확인

**작성일**: 2025-11-08  
**상태**: 🔍 진단 완료, SQL 실행 대기

