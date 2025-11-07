# 🔍 최종 진단 및 해결

## 현재 상태 확인

✅ **RLS 정책**: 정상
- Policy: `allow_read_policy`
- USING: `(is_active = true)`
- Command: `SELECT`

✅ **데이터**: 정상
- `app_id: com.sweetapps.pocketchord.debug`
- `is_active: TRUE`
- `active_popup_type: emergency`

❌ **앱 조회**: 실패
- `Query returned 0 rows`

---

## 🎯 최종 테스트 SQL

Supabase SQL Editor에서 실행하여 데이터가 정말 조회되는지 확인:

```sql
-- 1. anon 키로 조회 테스트 (앱이 사용하는 것과 동일)
-- (Supabase SQL Editor는 기본적으로 service role을 사용하므로 이 테스트는 제한적)
SELECT * FROM app_policy 
WHERE app_id = 'com.sweetapps.pocketchord.debug' 
  AND is_active = true;
```

**예상 결과**: 1개 행이 반환되어야 함

---

## 🔧 문제 해결 시도

### 방법 1: app_id 공백 확인

```sql
-- app_id에 숨겨진 공백이 있는지 확인
SELECT 
    id,
    app_id,
    length(app_id) as len,
    is_active,
    active_popup_type,
    -- 앞뒤 공백 제거
    trim(app_id) as trimmed
FROM app_policy;
```

**확인 사항**:
- `app_id`의 길이가 35자인지 확인
- 앞뒤에 공백이 없는지 확인

만약 공백이 있으면:
```sql
UPDATE app_policy 
SET app_id = trim(app_id)
WHERE id = 1;
```

---

### 방법 2: 데이터 재입력

혹시 모를 문제를 해결하기 위해 데이터를 삭제하고 다시 입력:

```sql
-- 기존 데이터 삭제
DELETE FROM app_policy WHERE app_id LIKE '%pocketchord.debug%';

-- 새로 입력
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

-- 확인
SELECT id, app_id, is_active, active_popup_type FROM app_policy;
```

---

### 방법 3: RLS 임시 비활성화 테스트

RLS가 정말 문제인지 확인:

```sql
-- RLS 비활성화
ALTER TABLE app_policy DISABLE ROW LEVEL SECURITY;
```

**이 상태에서 앱 재시작하고 테스트**

성공하면:
```sql
-- RLS 재활성화
ALTER TABLE app_policy ENABLE ROW LEVEL SECURITY;
```

---

### 방법 4: BuildConfig 확인

앱이 사용하는 app_id 확인:

```cmd
adb logcat -d -s SupabaseTest:* | findstr "SUPABASE_APP_ID"
```

**예상**:
```
D/SupabaseTest: BuildConfig.SUPABASE_APP_ID: com.sweetapps.pocketchord.debug
```

---

## 📱 Clean 빌드 후 테스트

빌드가 완료되면:

```cmd
# 1. 앱 삭제
adb uninstall com.sweetapps.pocketchord.debug

# 2. 새로 설치
adb install G:\Workspace\PocketChord\app\build\outputs\apk\debug\app-debug.apk

# 3. 로그 초기화
adb logcat -c

# 4. 앱 실행
adb shell am start -n com.sweetapps.pocketchord.debug/com.sweetapps.pocketchord.MainActivity

# 5. 로그 확인 (5초 후)
timeout /t 5
adb logcat -d -s AppPolicyRepo:* HomeScreen:*
```

---

## 🔍 예상 원인

RLS 정책이 정상이고 데이터도 정상이라면, 가능한 원인:

1. **앱 캐시 문제** → Clean 빌드로 해결
2. **app_id 불일치** (공백, 대소문자 등) → SQL로 확인
3. **Supabase 동기화 지연** → 잠시 기다린 후 재시도
4. **네트워크 문제** → WiFi 재연결

---

## 🚀 최종 체크리스트

- [ ] Supabase에서 데이터 재확인 (공백 체크)
- [ ] Clean 빌드 완료 대기
- [ ] 앱 완전 삭제 후 재설치
- [ ] 로그 확인: "Query returned 1 rows"
- [ ] 화면에 팝업 표시 확인

---

**다음 단계**: Clean 빌드가 완료되면 앱을 재설치하고 테스트하겠습니다.

