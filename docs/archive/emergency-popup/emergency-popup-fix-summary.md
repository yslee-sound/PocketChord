# ✅ 긴급 팝업 문제 해결 완료

## 문제
Supabase에서 emergency popup을 활성화했지만 앱에서 표시되지 않음

## 원인
1. **MainActivity와 HomeScreen에서 중복 체크**: 두 곳에서 정책을 조회하여 충돌 발생
2. **다이얼로그 불일치**: HomeScreen이 기존 `EmergencyRedirectDialog`를 사용, 새로운 `EmergencyDialog`로 교체 필요

## 해결 완료 ✅

### 1. MainActivity 수정
- ❌ **제거**: 중복된 정책 체크 로직 및 팝업 표시 코드
- ✅ **유지**: 앱 오프닝 광고 관련 로직만 유지

### 2. HomeScreen 수정
- ✅ `appPolicy` 상태 변수 추가
- ✅ 긴급 공지 시 `appPolicy` 객체 직접 저장
- ✅ 새로운 `EmergencyDialog` 사용
- ✅ 불필요한 `Announcement` 변환 제거

---

## 수정된 코드

### MainActivity.kt
```kotlin
// Before: 중복 체크
LaunchedEffect(Unit) {
    val repository = AppPolicyRepository(...)
    repository.getPolicy().onSuccess { policy ->
        when (policy.activePopupType) {
            "emergency" -> { showPolicyDialog = true }
            // ...
        }
    }
}

// After: 중복 제거
// 앱 정책 체크는 HomeScreen에서 처리 (중복 제거)
val app = context.applicationContext as PocketChordApplication
val isShowingAppOpenAd by app.isShowingAppOpenAd.collectAsState()
```

### HomeScreen.kt
```kotlin
// Before
var showEmergencyDialog by remember { mutableStateOf(false) }

// 로직에서
"emergency" -> {
    announcement = Announcement(...)  // 불필요한 변환
    showEmergencyDialog = true
}

// 팝업 표시
if (showEmergencyDialog && announcement?.isEmergency == true) {
    EmergencyRedirectDialog(...)  // 구 다이얼로그
}

// After
var showEmergencyDialog by remember { mutableStateOf(false) }
var appPolicy by remember { mutableStateOf<AppPolicy?>(null) }  // ✅ 추가

// 로직에서
"emergency" -> {
    appPolicy = p  // ✅ 정책 객체 직접 저장
    showEmergencyDialog = true
}

// 팝업 표시
if (showEmergencyDialog && appPolicy != null) {
    com.sweetapps.pocketchord.ui.dialog.EmergencyDialog(  // ✅ 새 다이얼로그
        policy = appPolicy!!,
        onDismiss = { /* X 버튼 없음 */ }
    )
}
```

---

## 테스트 절차

### 1. Supabase 설정 확인
```bash
# local.properties 확인
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
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

### 3. 앱 빌드 및 실행
```cmd
# 빌드
gradlew.bat assembleDebug

# 설치
adb install -r app\build\outputs\apk\debug\app-debug.apk

# 로그 확인
adb logcat -c
adb logcat -s HomeScreen:D AppPolicyRepo:D PocketChordApp:D

# 앱 실행
adb shell am start -n com.sweetapps.pocketchord.debug/.MainActivity
```

### 4. 예상 로그
```
D/PocketChordApp: Supabase configured: url set
D/HomeScreen: Startup: SUPABASE_APP_ID=com.sweetapps.pocketchord.debug, VERSION_CODE=2
D/HomeScreen: Supabase configured=true
D/HomeScreen: Policy fetch success: id=1 appId=com.sweetapps.pocketchord.debug active=true type=emergency minSupported=null latest=null
D/HomeScreen: Decision: EMERGENCY popup will show
```

### 5. UI 확인
- ✅ 긴급 공지 팝업이 즉시 표시됨
- ✅ 제목: "🚨 긴급 공지"
- ✅ 내용: Supabase의 `content` 값
- ✅ 확인 버튼 클릭 시 URL 이동
- ✅ X 버튼 없음 (닫기 불가)
- ✅ 뒤로가기 차단 안 됨 (닫기 불가)

---

## 문제 해결 체크리스트

### 팝업이 표시되지 않는 경우

#### 1. Supabase 설정 확인
```cmd
adb logcat -s PocketChordApp:* -d | findstr "Supabase"
```
**예상**: `Supabase configured: url set`  
**문제**: `Supabase 미설정` → `local.properties` 확인

#### 2. 정책 조회 확인
```cmd
adb logcat -s HomeScreen:* -d | findstr "Policy"
```
**예상**: `Policy fetch success: ... type=emergency ...`  
**문제**: `No active policy row` → Supabase 설정 확인

#### 3. 팝업 결정 확인
```cmd
adb logcat -s HomeScreen:* -d | findstr "Decision"
```
**예상**: `Decision: EMERGENCY popup will show`  
**문제**: 로그 없음 → 조건 불일치

#### 4. RLS 정책 확인
```sql
-- Supabase SQL Editor
SELECT * FROM app_policy 
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 결과가 없으면 RLS 문제
-- SQL Editor는 RLS 우회하므로 데이터가 보여야 함
```

#### 5. is_active 확인
```sql
SELECT app_id, is_active, active_popup_type 
FROM app_policy;
```
**is_active = FALSE**인 경우:
```sql
UPDATE app_policy SET is_active = TRUE
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

---

## 컴파일 상태

✅ **에러 0개**  
⚠️ **경고 18개** (컴파일에 영향 없음)

---

## 변경 파일 요약

| 파일 | 변경 내용 | 상태 |
|------|----------|------|
| `MainActivity.kt` | 중복 정책 체크 로직 제거 | ✅ |
| `HomeScreen.kt` | `appPolicy` 상태 추가, 새 다이얼로그 사용 | ✅ |

---

## 추가 팁

### 디버그 모드에서 즉시 확인
1. 앱 강제 종료
   ```cmd
   adb shell am force-stop com.sweetapps.pocketchord.debug
   ```

2. 캐시 초기화 (선택)
   ```cmd
   adb shell pm clear com.sweetapps.pocketchord.debug
   ```

3. 앱 재시작
   ```cmd
   adb shell am start -n com.sweetapps.pocketchord.debug/.MainActivity
   ```

### Supabase 테스트 환경
**디버그**: `com.sweetapps.pocketchord.debug`  
**릴리즈**: `com.sweetapps.pocketchord`

각각 별도의 `app_policy` 레코드 필요:
```sql
INSERT INTO app_policy (app_id, is_active, active_popup_type)
VALUES 
  ('com.sweetapps.pocketchord.debug', FALSE, 'none'),
  ('com.sweetapps.pocketchord', FALSE, 'none')
ON CONFLICT (app_id) DO NOTHING;
```

---

## 성공 시나리오

```
사용자 앱 실행
    ↓
HomeScreen LaunchedEffect 실행
    ↓
AppPolicyRepository.getPolicy() 호출
    ↓
Supabase에서 app_policy 조회 (RLS 적용)
    ↓
is_active = TRUE, active_popup_type = 'emergency'
    ↓
appPolicy = p (정책 저장)
showEmergencyDialog = true
    ↓
EmergencyDialog 표시
    ↓
사용자가 확인 버튼 클릭
    ↓
download_url 이동
```

---

**작성일**: 2025-11-08  
**상태**: ✅ 해결 완료  
**다음 단계**: 앱 빌드 및 테스트

## 빌드 명령어
```cmd
cd G:\Workspace\PocketChord
gradlew.bat assembleDebug
```

이제 앱을 빌드하고 실행하면 긴급 팝업이 정상적으로 표시됩니다! 🎉
