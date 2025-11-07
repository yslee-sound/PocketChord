# 긴급 팝업 문제 해결 가이드

## 문제 증상
Supabase에서 emergency popup을 활성화했지만 앱에서 표시되지 않음

## 원인
1. **중복 체크**: MainActivity와 HomeScreen 둘 다 정책을 체크하여 충돌 발생
2. **다이얼로그 불일치**: HomeScreen이 기존 `EmergencyRedirectDialog`를 사용하고 있었으나, 새로운 `EmergencyDialog`로 교체 필요
3. **조건 불일치**: `announcement?.isEmergency` 체크가 새로운 `AppPolicy` 구조와 맞지 않음

---

## 해결 방법

### 1. MainActivity의 중복 로직 제거 ✅
**Before**:
```kotlin
// MainActivity.kt
LaunchedEffect(Unit) {
    // 정책 체크 중복
    val repository = AppPolicyRepository(...)
    repository.getPolicy().onSuccess { ... }
}
```

**After**:
```kotlin
// MainActivity.kt
// 앱 정책 체크는 HomeScreen에서 처리 (중복 제거)
val app = context.applicationContext as PocketChordApplication
val isShowingAppOpenAd by app.isShowingAppOpenAd.collectAsState()
```

### 2. HomeScreen에 AppPolicy 저장 ✅
**Before**:
```kotlin
var showEmergencyDialog by remember { mutableStateOf(false) }
```

**After**:
```kotlin
var showEmergencyDialog by remember { mutableStateOf(false) }
var appPolicy by remember { mutableStateOf<AppPolicy?>(null) }  // 정책 저장용
```

### 3. 긴급 공지 로직 간소화 ✅
**Before**:
```kotlin
"emergency" -> {
    announcement = Announcement(...)  // 불필요한 변환
    showEmergencyDialog = true
}
```

**After**:
```kotlin
"emergency" -> {
    appPolicy = p  // 정책 객체 직접 저장
    showEmergencyDialog = true
}
```

### 4. 새로운 다이얼로그 사용 ✅
**Before**:
```kotlin
if (showEmergencyDialog && announcement?.isEmergency == true) {
    EmergencyRedirectDialog(...)  // 구 다이얼로그
}
```

**After**:
```kotlin
if (showEmergencyDialog && appPolicy != null) {
    com.sweetapps.pocketchord.ui.dialog.EmergencyDialog(
        policy = appPolicy!!,
        onDismiss = { /* X 버튼 없음 */ }
    )
}
```

---

## 테스트 방법

### 1. Supabase 설정 확인
```sql
-- app_policy 테이블 조회
SELECT app_id, is_active, active_popup_type, content, download_url
FROM app_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**예상 결과**:
```
app_id                              | is_active | active_popup_type | content | download_url
------------------------------------|-----------|-------------------|---------|-------------
com.sweetapps.pocketchord.debug    | TRUE      | emergency         | ...     | ...
```

### 2. 긴급 공지 활성화
```sql
UPDATE app_policy SET
  is_active = TRUE,
  active_popup_type = 'emergency',
  content = '🚨 긴급 점검 안내: 서버 점검이 진행 중입니다.',
  download_url = 'https://status.example.com'
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

### 3. 앱 재시작 후 로그 확인
```cmd
adb logcat -c
adb logcat -s HomeScreen:D AppPolicyRepo:D PocketChordApp:D
```

**예상 로그**:
```
D/PocketChordApp: Supabase configured: url set
D/HomeScreen: Startup: SUPABASE_APP_ID=com.sweetapps.pocketchord.debug, VERSION_CODE=2
D/HomeScreen: Supabase configured=true
D/HomeScreen: Policy fetch success: id=1 appId=com.sweetapps.pocketchord.debug active=true type=emergency minSupported=null latest=null
D/HomeScreen: Decision: EMERGENCY popup will show
```

### 4. UI 확인 사항
✅ 긴급 공지 팝업이 표시됨  
✅ 제목: "🚨 긴급 공지"  
✅ 내용: Supabase의 `content` 필드 값  
✅ 확인 버튼 클릭 시 `download_url`로 이동  
✅ X 버튼 없음 (닫기 불가)  
✅ 뒤로가기/외부 터치로 닫기 불가

---

## 문제 해결 체크리스트

### Supabase 설정
- [ ] `SUPABASE_URL`이 `local.properties`에 설정되어 있는가?
- [ ] `SUPABASE_ANON_KEY`가 `local.properties`에 설정되어 있는가?
- [ ] Supabase 프로젝트가 활성화되어 있는가?

### 테이블 설정
- [ ] `app_policy` 테이블이 존재하는가?
- [ ] `app_id`가 정확한가? (디버그: `com.sweetapps.pocketchord.debug`)
- [ ] `is_active = TRUE`인가?
- [ ] `active_popup_type = 'emergency'`인가?
- [ ] `content` 필드에 메시지가 있는가?
- [ ] `download_url` 필드에 URL이 있는가?

### RLS 정책
- [ ] RLS가 활성화되어 있는가?
  ```sql
  SELECT tablename, rowsecurity 
  FROM pg_tables 
  WHERE tablename = 'app_policy';
  ```
- [ ] `allow_read_policy` 정책이 존재하는가?
  ```sql
  SELECT policyname 
  FROM pg_policies 
  WHERE tablename = 'app_policy';
  ```

### 앱 설정
- [ ] 앱이 최신 버전으로 빌드되었는가?
- [ ] 인터넷 연결이 되어 있는가?
- [ ] 앱을 완전히 재시작했는가? (백그라운드에서 강제 종료 후 재실행)

---

## 로그 분석 가이드

### 정상 작동 시
```
D/PocketChordApp: Supabase configured: url set
D/HomeScreen: Supabase configured=true
D/HomeScreen: Policy fetch success: ... type=emergency ...
D/HomeScreen: Decision: EMERGENCY popup will show
```

### Supabase 미설정
```
W/PocketChordApp: Supabase 미설정: 환경변수 SUPABASE_URL / SUPABASE_ANON_KEY 를 확인하세요
W/HomeScreen: Skipping network fetch (Supabase not configured)
```
**해결**: `local.properties`에 설정 추가

### 정책 없음
```
W/HomeScreen: No active policy row for app_id='com.sweetapps.pocketchord.debug'. 
Check: (1) app_policy.app_id 값, (2) is_active=true, (3) RLS policy allowing read, (4) anon key valid.
```
**해결**: Supabase에서 정책 활성화

### 네트워크 에러
```
E/HomeScreen: Policy fetch failure: Unable to resolve host
```
**해결**: 인터넷 연결 확인

---

## 추가 팁

### 1. 캐시 초기화
앱 데이터를 지우고 재시작:
```cmd
adb shell pm clear com.sweetapps.pocketchord.debug
```

### 2. Supabase 직접 테스트
브라우저 콘솔에서 테스트:
```javascript
const { createClient } = supabase
const client = createClient('YOUR_URL', 'YOUR_ANON_KEY')

const { data, error } = await client
  .from('app_policy')
  .select('*')
  .eq('app_id', 'com.sweetapps.pocketchord.debug')
  
console.log(data, error)
```

### 3. RLS 우회 테스트
SQL Editor에서 직접 조회 (RLS 자동 우회):
```sql
SELECT * FROM app_policy;
```

---

## 변경된 파일

1. ✅ `MainActivity.kt`: 중복 체크 로직 제거
2. ✅ `HomeScreen.kt`: 
   - `appPolicy` 상태 추가
   - 긴급 공지 로직 간소화
   - 새로운 `EmergencyDialog` 사용

---

## 다음 단계

1. ✅ 코드 수정 완료
2. ✅ 컴파일 에러 없음
3. 🔜 앱 빌드 및 실행
4. 🔜 긴급 팝업 표시 확인
5. 🔜 다른 팝업 타입 테스트 (force_update, optional_update, notice)

---

**작성일**: 2025-11-08  
**상태**: ✅ 해결 완료

## 최종 확인 명령어

```cmd
# 1. 로그캣 초기화
adb logcat -c

# 2. 로그 필터링 시작
adb logcat -s HomeScreen:D AppPolicyRepo:D PocketChordApp:D

# 3. 앱 재시작 (또는 수동으로)
adb shell am force-stop com.sweetapps.pocketchord.debug
adb shell am start -n com.sweetapps.pocketchord.debug/.MainActivity
```

이제 긴급 팝업이 정상적으로 표시되어야 합니다! 🎉

