# 강제 업데이트 로직 분석

## 📋 현재 상황 확인

당신이 경험한 상황:
```
1. 강제 업데이트 팝업 표시
2. "지금 업데이트" 버튼 클릭
3. Play Store (또는 테스트 링크)로 이동
4. 뒤로가기로 앱에 돌아옴
5. ❓ 강제 업데이트 팝업이 여전히 표시됨
```

## 🔍 코드 분석

### 1. 강제 업데이트 팝업 표시 조건

**HomeScreen.kt (라인 193-211)**:
```kotlin
"force_update" -> {
    // 2) 강제 업데이트
    if (p.requiresForceUpdate(currentVersion)) {
        Log.d("HomeScreen", "Decision: FORCE UPDATE popup (minSupported=${p.minSupportedVersion})")
        updateInfo = UpdateInfo(
            id = null,
            versionCode = p.minSupportedVersion ?: (currentVersion + 1),
            versionName = "",
            appId = BuildConfig.SUPABASE_APP_ID,
            isForce = true,
            releaseNotes = p.content ?: "",
            releasedAt = null,
            downloadUrl = p.downloadUrl
        )
        showUpdateDialog = true
        
        // ⚠️ 로컬 캐시에 저장 (중요!)
        updatePrefs.edit {
            putInt("force_required_version", updateInfo!!.versionCode)
            putString("force_update_info", gson.toJson(updateInfo!!))
        }
    }
}
```

### 2. `requiresForceUpdate()` 조건

**AppPolicy.kt**:
```kotlin
fun requiresForceUpdate(currentVersion: Int): Boolean {
    return minSupportedVersion != null && currentVersion < minSupportedVersion!!
}
```

**즉:**
```
현재 버전 < minSupportedVersion → 강제 업데이트 필요
```

### 3. 팝업이 사라지는 조건

#### ✅ 팝업이 사라지는 경우:

1. **앱 버전이 업데이트됨**
   ```kotlin
   // HomeScreen.kt 라인 102-105
   if (storedForceVersion != -1 && 
       storedForceVersion <= BuildConfig.VERSION_CODE) {
       // 현재 버전이 요구 버전보다 높거나 같음
       updatePrefs.edit { 
           remove("force_required_version")
           remove("force_update_info") 
       }
   }
   ```

2. **Supabase에서 정책이 비활성화됨**
   ```kotlin
   // HomeScreen.kt 라인 156-164
   if (policy == null) {
       // 정책이 없거나 is_active=false
       if (storedForceVersion != -1) {
           android.util.Log.w("HomeScreen", "⚠️ Clearing cached force update (no active policy)")
           updatePrefs.edit {
               remove("force_required_version")
               remove("force_update_info")
           }
       }
       return@LaunchedEffect
   }
   ```

3. **강제 업데이트 조건 해제**
   ```kotlin
   // HomeScreen.kt 라인 212-217
   else {
       // requiresForceUpdate() = false
       if (storedForceVersion != -1) updatePrefs.edit {
           remove("force_required_version")
           remove("force_update_info")
       }
   }
   ```

#### ❌ 팝업이 사라지지 않는 경우:

**"지금 업데이트" 버튼 클릭 후 돌아왔을 때:**
```kotlin
// OptionalUpdateDialog.kt 라인 39
onUpdateClick: () -> Unit  // 단순히 스토어만 열림
```

**버튼 클릭 시:**
```kotlin
// HomeScreen.kt 라인 310-312
onUpdateClick = {
    tryOpenStore(updateInfo!!)
    // ❌ showUpdateDialog = false 없음!
    // ❌ 캐시 삭제 없음!
}
```

## 🎯 핵심 답변

### Q: "업데이트 버튼 눌렀다가 돌아오면 팝업이 그대로 있는데, 실제로 업데이트해야만 없어지나요?"

**A: 네, 맞습니다! 💯**

### 현재 로직:

```
┌──────────────────────────────────────┐
│ 강제 업데이트 팝업 표시              │
├──────────────────────────────────────┤
│ 조건: currentVersion < minSupported  │
│                                       │
│ ✅ 저장: 로컬 캐시에 저장            │
│   - force_required_version           │
│   - force_update_info                │
└──────────────────────────────────────┘
            ↓
    [지금 업데이트] 클릭
            ↓
    Play Store 열림 (tryOpenStore)
            ↓
    뒤로가기로 앱 복귀
            ↓
    ❌ 팝업 여전히 표시!
    
왜? → showUpdateDialog = true 그대로
     → 로컬 캐시도 그대로
     → currentVersion도 그대로 (업데이트 안 함)
```

### 팝업이 사라지는 시점:

**오직 이 경우들만:**

1. ✅ **실제로 앱을 업데이트함**
   - Play Store에서 APK 다운로드 및 설치
   - `BuildConfig.VERSION_CODE`가 증가
   - 다음 실행 시: `currentVersion >= minSupportedVersion`
   - → 팝업 사라짐

2. ✅ **관리자가 Supabase에서 정책 비활성화**
   - `is_active = false`로 변경
   - 다음 실행 시: `policy == null`
   - → 로컬 캐시 삭제 → 팝업 사라짐

3. ✅ **관리자가 minSupportedVersion 값 낮춤**
   - 예: `minSupportedVersion = 3` → `2`로 변경
   - 다음 실행 시: `currentVersion >= minSupportedVersion`
   - → 팝업 사라짐

## 📊 상태 변화 다이어그램

```
[앱 시작]
    ↓
현재 버전: 2
필요 버전: 3
    ↓
조건: 2 < 3 ✅
    ↓
┌──────────────────────┐
│ 강제 업데이트 팝업   │
│ [지금 업데이트]      │  ← 클릭
└──────────────────────┘
         ↓
    Play Store 열림
    (앱은 백그라운드)
         ↓
    뒤로가기 (앱 포그라운드)
         ↓
    ❌ 버전 여전히 2
    ❌ 조건 여전히 2 < 3
         ↓
┌──────────────────────┐
│ 강제 업데이트 팝업   │  ← 다시 표시!
│ [지금 업데이트]      │
└──────────────────────┘
```

## 🔒 강제 업데이트 안전 장치

### 1. 로컬 캐시 저장
```kotlin
// 네트워크 없어도 강제 업데이트 유지
updatePrefs.edit {
    putInt("force_required_version", updateInfo!!.versionCode)
    putString("force_update_info", gson.toJson(updateInfo!!))
}
```

### 2. 뒤로가기 차단
```kotlin
// HomeScreen.kt 라인 87-89
if (showUpdateDialog && (updateInfo?.isForce == true)) {
    BackHandler(enabled = true) { }  // 뒤로가기 무시
}
```

### 3. Dialog 닫기 차단
```kotlin
// OptionalUpdateDialog.kt 라인 46-51
Dialog(
    onDismissRequest = {
        if (!isForce) {  // 강제 모드면 무시!
            onLaterClick?.invoke()
        }
    },
    properties = DialogProperties(
        dismissOnBackPress = !isForce,      // 강제 모드면 false
        dismissOnClickOutside = !isForce,   // 강제 모드면 false
        usePlatformDefaultWidth = false
    )
)
```

## ✅ 결론

### 당신이 경험한 동작이 **정상**입니다! ✅

**강제 업데이트의 목적**:
- 사용자가 반드시 업데이트하도록 강제
- 업데이트하지 않으면 앱 사용 불가
- Play Store에서 "뒤로가기"로 도망갈 수 없음

**팝업이 사라지는 유일한 방법**:
1. 실제로 앱 업데이트
2. 관리자가 정책 비활성화
3. 관리자가 요구 버전 낮춤

**Play Store에서 뒤로가기로 돌아온 경우**:
- ❌ 버전 변경 안 됨
- ❌ 팝업 조건 그대로
- ✅ 팝업 다시 표시 (의도된 동작!)

---

## 💡 추가 개선 아이디어 (선택사항)

만약 테스트 편의를 위해 "임시 스킵" 기능을 원한다면:

### 방법 1: 디버그 모드에서만 숨은 스킵 버튼
```kotlin
// OptionalUpdateDialog.kt에 추가 (디버그 빌드만)
if (BuildConfig.DEBUG && isForce) {
    // 10번 연속 탭하면 스킵 (테스트용)
    var tapCount by remember { mutableStateOf(0) }
    
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .clickable { 
                tapCount++
                if (tapCount >= 10) {
                    onLaterClick?.invoke()
                }
            }
    ) {
        Text(".", color = Color.Transparent)
    }
}
```

### 방법 2: 관리자 설정으로 테스트 모드
```sql
-- Supabase에 test_mode 컬럼 추가
ALTER TABLE app_policy ADD COLUMN test_mode BOOLEAN DEFAULT false;

-- 테스트 시에만 true
UPDATE app_policy SET test_mode = true WHERE app_id LIKE '%.debug';
```

하지만 **현재 로직은 의도대로 완벽하게 작동**하고 있습니다! 🎉

---

**작성일**: 2025-11-08  
**상태**: ✅ 로직 정상 작동 확인

