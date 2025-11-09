# 🔄 선택적 업데이트 → 강제 업데이트 전환 메커니즘 설명

**버전**: v1.0.0  
**작성일**: 2025-11-09 08:45 KST  
**목적**: "N회 이후 강제 전환" 동작 원리 설명

---

## ❓ 질문

> **"Supabase에서 `is_force_update = false`로 설정했는데,  
> 어떻게 특정 횟수 이후에 강제 업데이트로 전환할 수 있나요?"**

---

## 💡 답변: 클라이언트 측 동적 전환

### 핵심 개념

**Supabase의 값을 변경하는 것이 아니라, 앱 내부에서 조건에 따라 강제 업데이트처럼 동작하도록 만듭니다.**

```
Supabase (서버):
├─ is_force_update: false  (변경 안 함)
└─ max_dismiss_count: 3

앱 (클라이언트):
├─ dismiss_count < 3 → 선택적 업데이트 다이얼로그 표시
│   ├─ "업데이트" 버튼
│   └─ "나중에" 버튼 ✅ (있음)
│
└─ dismiss_count >= 3 → 강제 업데이트처럼 표시
    ├─ "업데이트" 버튼
    └─ "나중에" 버튼 ❌ (숨김 또는 비활성화)
```

---

## 🔍 상세 설명

### 방법 1: "나중에" 버튼 숨기기 (추천)

**개념**:
- Supabase의 `is_force_update`는 그대로 `false`
- 하지만 앱에서 `dismiss_count`가 일정 횟수를 초과하면
- **"나중에" 버튼을 표시하지 않음**
- 결과적으로 사용자는 "업데이트"만 할 수 있음 = 강제 업데이트와 동일한 효과

**코드 예시**:
```kotlin
// Supabase에서 가져온 데이터
val isForceUpdate = updatePolicy.isForceUpdate  // false
val maxDismissCount = updatePolicy.maxDismissCount  // 3

// SharedPreferences에서 가져온 데이터
val dismissCount = prefs.getInt("dismiss_count", 0)  // 사용자가 "나중에"를 누른 횟수

// 다이얼로그 표시
OptionalUpdateDialog(
    // Supabase의 is_force_update가 true이거나
    // dismiss_count가 max를 초과했으면 강제처럼 동작
    isForce = isForceUpdate || (dismissCount >= maxDismissCount),
    
    onUpdateClick = { tryOpenStore(updateInfo) },
    
    // isForce가 true면 null, false면 람다 제공
    onLaterClick = if (isForceUpdate || dismissCount >= maxDismissCount) {
        null  // "나중에" 버튼 숨김
    } else {
        { /* "나중에" 로직 */ }
    }
)
```

**OptionalUpdateDialog 내부**:
```kotlin
@Composable
fun OptionalUpdateDialog(
    isForce: Boolean = false,  // ← 이 값이 true면
    // ...
    onLaterClick: (() -> Unit)? = null
) {
    // ...
    
    if (isForce) {
        // 강제 업데이트 레이아웃
        // "업데이트" 버튼만 표시
        Button(onClick = onUpdateClick) { Text("지금 업데이트") }
    } else {
        // 선택적 업데이트 레이아웃
        // "나중에" + "업데이트" 버튼
        Row {
            Button(onClick = { onLaterClick?.invoke() }) { Text("나중에") }
            Button(onClick = onUpdateClick) { Text("지금 업데이트") }
        }
    }
}
```

**결과**:
- ✅ Supabase의 `is_force_update`는 그대로 `false`
- ✅ 하지만 앱에서는 강제 업데이트처럼 동작
- ✅ 사용자는 "업데이트"만 할 수 있음

---

### 방법 2: 경고 메시지 표시 (부드러운 접근)

**개념**:
- "나중에" 버튼은 남겨두지만
- `dismiss_count`가 높아지면 경고 메시지 표시
- "이번이 마지막 기회입니다" 같은 메시지

**코드 예시**:
```kotlin
val warningMessage = when {
    dismissCount >= maxDismissCount - 1 -> "⚠️ 마지막 경고: 다음에는 필수 업데이트됩니다."
    dismissCount >= maxDismissCount - 2 -> "⚠️ 주의: 곧 필수 업데이트로 전환됩니다."
    else -> null
}

OptionalUpdateDialog(
    isForce = false,
    warningMessage = warningMessage,  // 경고 메시지 표시
    onUpdateClick = { tryOpenStore(updateInfo) },
    onLaterClick = {
        if (dismissCount >= maxDismissCount) {
            // 최대 횟수 도달 → 강제 전환
            // 다음부터는 강제 업데이트로 표시
            prefs.edit {
                putBoolean("force_update_triggered", true)
            }
        }
        // 일반적인 "나중에" 로직
        dismissedVersionCode.value = targetVersionCode
    }
)
```

**결과**:
- ✅ 사용자에게 미리 경고
- ✅ 부드러운 전환
- ✅ 사용자 불만 감소

---

### 방법 3: 완전 강제 전환 (가장 강력)

**개념**:
- `dismiss_count`가 최대치에 도달하면
- 다음 실행 시 다이얼로그의 `isForce`를 `true`로 강제 설정
- 뒤로가기도 차단

**코드 예시**:
```kotlin
val storedForceTriggered = prefs.getBoolean("force_update_triggered_v${targetVersionCode}", false)

val effectiveIsForce = updatePolicy.isForceUpdate || storedForceTriggered

OptionalUpdateDialog(
    isForce = effectiveIsForce,
    onUpdateClick = { tryOpenStore(updateInfo) },
    onLaterClick = if (effectiveIsForce) {
        null
    } else {
        {
            dismissCount++
            if (dismissCount >= maxDismissCount) {
                // 다음부터 강제 업데이트로 전환
                prefs.edit {
                    putBoolean("force_update_triggered_v${targetVersionCode}", true)
                }
            }
            // 일반 "나중에" 로직
        }
    }
)

// 강제 업데이트면 뒤로가기 차단
if (effectiveIsForce) {
    BackHandler(enabled = true) { /* 차단 */ }
}
```

**결과**:
- ✅ 완전한 강제 업데이트와 동일
- ✅ 뒤로가기 차단
- ✅ X 버튼 숨김
- ✅ 사용자는 업데이트만 가능

---

## 📊 세 가지 방법 비교

| 방법 | Supabase 변경 | 사용자 경험 | 구현 난이도 | 권장도 |
|------|--------------|------------|------------|--------|
| 1. "나중에" 버튼 숨기기 | ❌ 불필요 | 중간 (갑작스러움) | 낮음 | ⭐⭐⭐⭐ |
| 2. 경고 메시지 표시 | ❌ 불필요 | 좋음 (부드러움) | 중간 | ⭐⭐⭐⭐⭐ |
| 3. 완전 강제 전환 | ❌ 불필요 | 나쁨 (aggressive) | 높음 | ⭐⭐ |

**최종 선택**: **방법 1 (3회 후 버튼 숨기기 + 뒤로가기 차단)** 🎯
- 구현 간단
- 효과적인 업데이트 유도
- 3회까지는 선택권 제공

---

## 🎯 최종 구현 방식 (간소화)

### 핵심 전략
```
1-3회 "나중에": 선택적 업데이트 (버튼 2개)
4회째 실행: "업데이트" 버튼만 + 뒤로가기 차단
```

### 1. Supabase 스키마
```sql
CREATE TABLE update_policy (
    -- 기존 필드
    is_force_update BOOLEAN DEFAULT FALSE,
    target_version_code INT NOT NULL,
    
    -- 새로운 필드
    max_dismiss_count INT DEFAULT 3  -- 최대 "나중에" 횟수
);
```

### 2. SharedPreferences 저장
```kotlin
"나중에" 버튼 클릭 시:
- dismissed_version_code: 4
- dismissed_timestamp: timestamp
- dismiss_count: 1 (증가)
```

### 3. 체크 로직 (간소화)
```kotlin
fun shouldShowUpdateDialog(): Boolean {
    val currentVersion = BuildConfig.VERSION_CODE
    val targetVersion = updatePolicy.targetVersionCode
    
    if (currentVersion >= targetVersion) return false
    
    if (updatePolicy.isForceUpdate) return true
    
    val dismissedVersion = prefs.getInt("dismissed_version_code", -1)
    if (dismissedVersion != targetVersion) {
        return true
    }
    
    val dismissedTime = prefs.getLong("dismissed_timestamp", 0)
    val dismissCount = prefs.getInt("dismiss_count", 0)
    val currentTime = System.currentTimeMillis()
    
    // 최대 횟수 도달 시에도 팝업 표시 (강제로)
    val hoursToWait = calculateWaitTime(dismissCount, 24, 0.5)
    val timeSinceLastDismiss = currentTime - dismissedTime
    val waitTimeMs = hoursToWait * 60 * 60 * 1000
    
    return timeSinceLastDismiss >= waitTimeMs
}

fun getEffectiveIsForce(): Boolean {
    val dismissCount = prefs.getInt("dismiss_count", 0)
    val maxDismissCount = updatePolicy.maxDismissCount  // 3
    
    // Supabase 강제 설정 또는 최대 횟수 도달
    return updatePolicy.isForceUpdate || (dismissCount >= maxDismissCount)
}
```

### 4. UI 표시 (간소화)
```kotlin
val effectiveIsForce = getEffectiveIsForce()

OptionalUpdateDialog(
    isForce = effectiveIsForce,
    title = if (effectiveIsForce) "필수 업데이트" else "앱 업데이트",
    onUpdateClick = { tryOpenStore(updateInfo) },
    onLaterClick = if (effectiveIsForce) {
        null  // 강제면 "나중에" 버튼 숨김
    } else {
        {
            val currentDismissCount = prefs.getInt("dismiss_count", 0)
            prefs.edit {
                putInt("dismiss_count", currentDismissCount + 1)
                putInt("dismissed_version_code", updateInfo!!.versionCode)
                putLong("dismissed_timestamp", System.currentTimeMillis())
            }
            dismissedVersionCode.value = updateInfo!!.versionCode
            showUpdateDialog = false
        }
    }
)

// 강제면 뒤로가기 차단
if (effectiveIsForce) {
    BackHandler(enabled = true) { /* 차단 */ }
}
```

---

## 🔄 전체 플로우

### 시나리오: 3회 "나중에" 후 강제 전환 (간소화)

```
Day 1, 09:00 - 첫 번째 실행
├─ Supabase: is_force_update = false, max_dismiss_count = 3
├─ dismissCount = 0
├─ effectiveIsForce = false
├─ UI: ["나중에"] ["업데이트"]
└─ 사용자: "나중에" 클릭 → dismissCount = 1

Day 2, 10:00 - 두 번째 실행 (24시간 후)
├─ dismissCount = 1
├─ effectiveIsForce = false
├─ UI: ["나중에"] ["업데이트"]
└─ 사용자: "나중에" 클릭 → dismissCount = 2

Day 2, 22:00 - 세 번째 실행 (12시간 후)
├─ dismissCount = 2
├─ effectiveIsForce = false
├─ UI: ["나중에"] ["업데이트"]
└─ 사용자: "나중에" 클릭 → dismissCount = 3

Day 3, 04:00 - 네 번째 실행 (6시간 후)
├─ dismissCount = 3
├─ effectiveIsForce = true 🚨 (dismiss_count >= 3)
├─ UI: "필수 업데이트"
│      ["업데이트"] (버튼 하나만, 뒤로가기 차단)
└─ 사용자: "업데이트"만 가능 → 업데이트 완료 ✅
```

**간소화 포인트**:
- ✅ 경고 메시지 없음 (구현 단순화)
- ✅ 3회 "나중에" 허용
- ✅ 4회째 실행 시 바로 강제 전환
- ✅ `dismiss_count >= 3`만 체크

---

## 📝 정리

### ✅ 핵심 요점

1. **Supabase의 `is_force_update`를 변경하지 않음**
   - 서버 설정은 그대로 유지

2. **클라이언트 측에서 조건부로 강제 업데이트처럼 동작**
   - `dismiss_count` 추적 (최대 3회)
   - `dismiss_count >= 3` 시 "나중에" 버튼 숨김
   - 뒤로가기 차단

3. **간소화된 전략**
   - 1-3회: 선택적 업데이트
   - 4회째: 강제 전환 (경고 메시지 없이 바로)

4. **구현 복잡도 최소화**
   - `effectiveIsForce` 계산만 추가
   - 경고 메시지, 별도 상태 관리 불필요

---

## 🎯 장점

✅ **Supabase 변경 불필요**
- 서버 설정 그대로 유지
- 클라이언트만 수정

✅ **구현 간단**
- `effectiveIsForce` 계산만 추가
- 복잡한 경고 시스템 불필요

✅ **효과적인 업데이트 유도**
- 3회 기회 제공 (사용자 선택권)
- 4회째 필수화 (업데이트율 향상)

✅ **유연한 제어**
- Supabase에서 `max_dismiss_count` 조절 가능
- 앱별로 다른 설정 가능

---

**문서 버전**: v1.1.0  
**작성일**: 2025-11-09 08:45 KST  
**수정일**: 2025-11-09 09:00 KST  
**변경 사항**: 간소화 - 경고 메시지 제거, 3회 후 바로 강제 전환

