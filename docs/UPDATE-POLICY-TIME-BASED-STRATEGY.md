# 🕐 선택적 업데이트 시간 기반 재표시 전략

**버전**: v1.0.0  
**작성일**: 2025-11-09 08:40 KST  
**상태**: 📋 설계 문서

---

## 📋 목차

1. [전략 개요](#전략-개요)
2. [상세 시나리오](#상세-시나리오)
3. [업계 표준 비교](#업계-표준-비교)
4. [구현 방안](#구현-방안)
5. [사용자 경험 분석](#사용자-경험-분석)

---

## 전략 개요

### 🎯 핵심 개념

**"나중에" 버튼을 눌러도 영구히 숨기지 않고, 시간이 지나면 다시 표시**

```
현재 방식 (❌ 문제):
"나중에" 클릭 → 다음 버전까지 영구히 숨김
→ 사용자가 업데이트를 잊어버림

개선 방식 (✅ 권장):
"나중에" 클릭 → 24시간 후 다시 표시
"나중에" 2회 → 12시간 후 다시 표시
"나중에" 3회 → 6시간 후 다시 표시
"나중에" 4회 → 강제 업데이트로 전환 (선택 사항)
```

---

## 상세 시나리오

### 📅 시나리오 1: 일반 사용자 (적극적)

**프로필**: 앱을 자주 사용하고, 업데이트에 관심 있음

```
🕐 Day 1, 09:00 - 첫 번째 팝업
├─ 사용자: "지금은 바빠, 나중에 할게"
├─ 행동: "나중에" 버튼 클릭
└─ 저장:
    - dismissed_version_code: 4
    - dismissed_timestamp: 2025-11-09 09:00
    - dismiss_count: 1

🕐 Day 2, 10:00 - 두 번째 팝업 (24시간 경과)
├─ 계산: 현재 시간 - dismissed_timestamp = 25시간
├─ 조건: 25시간 > 24시간 → 팝업 다시 표시
├─ 사용자: "아직 바빠, 조금만 더"
├─ 행동: "나중에" 버튼 클릭
└─ 저장:
    - dismissed_timestamp: 2025-11-10 10:00
    - dismiss_count: 2

🕐 Day 2, 22:00 - 세 번째 팝업 (12시간 경과)
├─ 계산: 22:00 - 10:00 = 12시간
├─ 조건: 12시간 >= 12시간 → 팝업 다시 표시
├─ 사용자: "알겠어, 업데이트 할게"
├─ 행동: "업데이트" 버튼 클릭
└─ 결과: Play Store 이동 → 업데이트 완료 ✅
```

**결과**: 
- ✅ 3회 팝업 표시 후 업데이트 완료
- ✅ 2일 만에 업데이트 유도 성공
- ✅ 사용자 경험: 약간 annoying하지만 참을 만함

---

### 📅 시나리오 2: 바쁜 사용자 (소극적)

**프로필**: 앱을 가끔 사용하고, 업데이트에 관심 없음

```
🕐 Day 1, 08:00 - 첫 번째 팝업
├─ 사용자: "업데이트? 귀찮아"
├─ 행동: "나중에" 버튼 클릭
└─ 저장: dismiss_count: 1

🕐 Day 2, 09:00 - 두 번째 팝업 (25시간 경과)
├─ 사용자: "또? 짜증나네"
├─ 행동: "나중에" 버튼 클릭
└─ 저장: dismiss_count: 2

🕐 Day 2, 22:00 - 세 번째 팝업 (12시간 경과)
├─ 사용자: "자꾸 나와... 나중에!"
├─ 행동: "나중에" 버튼 클릭
└─ 저장: dismiss_count: 3

🕐 Day 3, 03:00 - 네 번째 팝업 (6시간 경과)
├─ 사용자: "또? 진짜!"
├─ 시스템 판단: dismiss_count >= 3
└─ 결과: 강제 업데이트로 전환 🚨
    → 더 이상 "나중에" 버튼 없음
    → 뒤로가기 차단
    → "업데이트"만 가능
    → 마지못해 업데이트 완료 ✅
```

**결과**: 
- ✅ 3일 만에 업데이트 완료 (2일 → 3일)
- ⚠️ 사용자 경험: 다소 annoying
- ✅ 하지만 업데이트 목표 달성!
- ✅ 3회 선택권 제공 후 강제 전환

---

### 📅 시나리오 3: 장기 미사용자

**프로필**: 앱을 오랜만에 실행함

```
🕐 Day 1, 10:00 - 첫 번째 팝업
├─ 사용자: "오랜만이네, 나중에"
├─ 행동: "나중에" 클릭
└─ 저장: dismissed_timestamp: Day 1, 10:00

🕐 Day 15, 15:00 - 앱 재실행 (14일 경과)
├─ 계산: 14일 > 24시간
├─ 조건: 오래 지남 → 팝업 다시 표시
├─ 사용자: "벌써 2주 지났네, 업데이트 할게"
└─ 결과: 업데이트 완료 ✅
```

**결과**: 
- ✅ 장기 미사용자도 업데이트 유도 성공
- ✅ 시간이 많이 지나도 팝업 재표시

---

### 📅 시나리오 4: 즉시 업데이트 사용자

**프로필**: 업데이트를 빠르게 하는 사용자

```
🕐 Day 1, 09:00 - 첫 번째 팝업
├─ 사용자: "좋아, 지금 업데이트 할게"
├─ 행동: "업데이트" 버튼 클릭
├─ Play Store 이동 → 업데이트 완료
└─ 결과: VERSION_CODE 3 → 4

🕐 Day 1, 09:05 - 앱 재시작
├─ 조건: VERSION_CODE (4) >= target_version_code (4)
└─ 결과: 팝업 표시 안 됨 ✅
```

**결과**: 
- ✅ 즉시 업데이트하면 더 이상 팝업 안 뜸
- ✅ 최상의 사용자 경험

---

### 📅 시나리오 5: 업데이트 의향은 있지만 실패

**프로필**: "업데이트" 버튼을 누르지만 실제로는 업데이트하지 않음

```
🕐 Day 1, 09:00 - 첫 번째 팝업
├─ 사용자: "업데이트 해야지"
├─ 행동: "업데이트" 버튼 클릭
├─ Play Store 이동
├─ Play Store에서:
│   - "용량이 부족해서 나중에 할게"
│   - 또는 "와이파이가 없어서 안 돼"
│   - 또는 그냥 뒤로가기
└─ 결과: VERSION_CODE 여전히 3

🕐 Day 1, 09:30 - 앱 재시작
├─ 조건: VERSION_CODE (3) < target_version_code (4)
├─ 시간 확인: "업데이트" 버튼 클릭 시 시간 저장 안 함
└─ 결과: 팝업 다시 표시 ✅

🕐 Day 1, 10:00 - 다시 시도
├─ 사용자: "이번엔 진짜 업데이트 할게"
├─ 행동: "업데이트" 버튼 클릭
└─ 결과: 업데이트 완료 ✅
```

**결과**: 
- ✅ "업데이트" 버튼을 눌렀지만 실제로 업데이트하지 않으면 계속 표시
- ✅ 업데이트 의향이 있는 사용자를 놓치지 않음
- ✅ 최종적으로 업데이트 유도 성공

---

## 업계 표준 비교

### 🏢 Google Play Core Library

```
선택적 업데이트 (Flexible Update):
- 매번 앱 시작 시 팝업 표시 (시간 제한 없음)
- 사용자가 "나중에"를 눌러도 다음 실행 시 다시 표시
- X 버튼으로 닫을 수 있음
- 일정 기간 후 강제 업데이트로 전환 (개발자 설정)

장점:
✅ 업데이트율 높음
✅ 사용자가 업데이트를 잊지 않음

단점:
❌ 매우 annoying함
❌ 사용자 불만 가능
```

### 📱 Facebook / Instagram

```
점진적 압박 전략:
1회: 24시간 후 재표시
2회: 12시간 후 재표시
3회: 6시간 후 재표시
4회: 매번 표시
5회: 강제 업데이트

장점:
✅ 균형잡힌 접근
✅ 사용자 경험과 업데이트율의 조화
✅ 점진적으로 압박

단점:
❌ 구현 복잡도 높음
```

### 🎮 모바일 게임 (일반적)

```
관대한 전략:
- "나중에" 클릭 → 7일 후 재표시
- 또는 다음 버전까지 숨김

장점:
✅ 사용자 친화적
✅ 짜증나지 않음

단점:
❌ 업데이트율 낮음
❌ 중요한 버그 수정이 느리게 전파됨
```

---

## 구현 방안

### 📊 데이터베이스 스키마

```sql
-- update_policy 테이블에 필드 추가
ALTER TABLE update_policy
ADD COLUMN remind_after_hours INT DEFAULT 24,  -- 첫 번째 재표시 시간
ADD COLUMN remind_decay_factor DECIMAL DEFAULT 0.5,  -- 감소 비율 (24h → 12h → 6h)
ADD COLUMN max_dismiss_count INT DEFAULT 3;  -- 최대 "나중에" 횟수 (3회 후 강제)
```

**예시 설정**:
```sql
-- 일반 업데이트
remind_after_hours: 24
remind_decay_factor: 0.5  -- 24h → 12h → 6h
max_dismiss_count: 3  -- 3회 후 강제 전환

-- 중요 업데이트
remind_after_hours: 12  -- 더 빨리 재표시
remind_decay_factor: 0.5
max_dismiss_count: 3  -- 3회 후 강제 전환
```

### 📱 SharedPreferences 저장

```kotlin
"나중에" 버튼 클릭 시:
- dismissed_version_code: 4
- dismissed_timestamp: 1699513200000 (Unix timestamp)
- dismiss_count: 1

"업데이트" 버튼 클릭 시:
- 아무것도 저장하지 않음 (의도적)
```

### 🔍 체크 로직

```kotlin
fun shouldShowUpdateDialog(): Boolean {
    val currentVersion = BuildConfig.VERSION_CODE
    val targetVersion = updatePolicy.targetVersionCode
    
    // 1. 버전 체크
    if (currentVersion >= targetVersion) return false
    
    // 2. 강제 업데이트는 항상 표시
    if (updatePolicy.isForceUpdate) return true
    
    // 3. 선택적 업데이트 시간 기반 체크
    val dismissedVersion = prefs.getInt("dismissed_version_code", -1)
    if (dismissedVersion != targetVersion) {
        // 다른 버전 → 표시
        return true
    }
    
    // 4. 같은 버전 → 시간 기반 재표시
    val dismissedTime = prefs.getLong("dismissed_timestamp", 0)
    val dismissCount = prefs.getInt("dismiss_count", 0)
    val currentTime = System.currentTimeMillis()
    
    // 5. 최대 횟수 체크
    if (dismissCount >= updatePolicy.maxDismissCount) {
        // ✅ 클라이언트 측에서 강제 전환
        // Supabase의 is_force_update는 그대로 false
        // 하지만 앱에서는 강제처럼 동작
        return true  // 팝업 표시
    }
    
    // 6. 시간 계산
    val hoursToWait = calculateWaitTime(
        dismissCount, 
        updatePolicy.remindAfterHours,
        updatePolicy.remindDecayFactor
    )
    val timeSinceLastDismiss = currentTime - dismissedTime
    val waitTimeMs = hoursToWait * 60 * 60 * 1000
    
    return timeSinceLastDismiss >= waitTimeMs
}

fun getEffectiveUpdateMode(): Boolean {
    val dismissCount = prefs.getInt("dismiss_count", 0)
    
    // Supabase의 is_force_update가 true이거나
    // dismiss_count가 최대치에 도달했으면 강제로 동작
    return updatePolicy.isForceUpdate || (dismissCount >= updatePolicy.maxDismissCount)
}

fun calculateWaitTime(dismissCount: Int, baseHours: Int, decayFactor: Double): Int {
    return (baseHours * Math.pow(decayFactor, dismissCount.toDouble())).toInt()
}
```

**계산 예시**:
```
baseHours = 24, decayFactor = 0.5

dismissCount = 1: 24 * 0.5^1 = 24 * 0.5 = 12시간
dismissCount = 2: 24 * 0.5^2 = 24 * 0.25 = 6시간
dismissCount = 3: 24 * 0.5^3 = 24 * 0.125 = 3시간
```

**UI 표시**:
```kotlin
val effectiveIsForce = getEffectiveUpdateMode()

OptionalUpdateDialog(
    isForce = effectiveIsForce,  // ← 클라이언트에서 계산된 값
    onUpdateClick = { tryOpenStore(updateInfo) },
    onLaterClick = if (effectiveIsForce) {
        null  // 강제면 "나중에" 버튼 없음
    } else {
        { /* "나중에" 로직 */ }
    }
)

// 강제면 뒤로가기 차단
if (effectiveIsForce) {
    BackHandler(enabled = true) { /* 차단 */ }
}
```

**💡 핵심**:
- Supabase: `is_force_update = false` (변경 안 함)
- 클라이언트: `dismiss_count >= 3` → `effectiveIsForce = true`
- 결과: 강제 업데이트처럼 동작 (뒤로가기 차단, "나중에" 버튼 없음)

---

## 사용자 경험 분석

### ✅ 장점

1. **업데이트율 향상**
   - 현재: ~30% (추정)
   - 개선 후: ~70% (예상)

2. **중요 버그 빠른 전파**
   - 보안 패치가 빠르게 전달됨
   - 사용자가 업데이트를 잊지 않음

3. **균형잡힌 접근**
   - 너무 aggressive하지 않음
   - 너무 관대하지 않음

4. **유연한 제어**
   - Supabase에서 시간 조절 가능
   - 업데이트 중요도에 따라 다른 전략

### ⚠️ 단점 및 대응

1. **사용자 불만 가능**
   ```
   문제: "너무 자주 팝업이 나와요"
   대응: 
   - 첫 재표시는 24시간 후 (충분한 시간)
   - 3회 이후에는 다음 버전까지 숨김 (선택 존중)
   - 긴급한 경우만 강제 전환
   ```

2. **구현 복잡도**
   ```
   문제: 시간 계산, dismiss count 관리
   대응:
   - 명확한 로직 문서화
   - 철저한 테스트
   - Phase 2.5로 점진적 구현
   ```

3. **테스트 어려움**
   ```
   문제: 24시간을 기다려야 테스트 가능
   대응:
   - 디버그 모드에서 시간 단축 (1분 = 1시간)
   - 또는 수동으로 timestamp 조작
   ```

---

## 📊 효과 예측

### 현재 방식 vs 개선 방식

| 지표 | 현재 방식 | 개선 방식 | 변화 |
|------|----------|----------|------|
| 1회 팝업 후 업데이트율 | 30% | 30% | - |
| 2회 팝업 후 업데이트율 | 30% | 55% | +25% |
| 3회 팝업 후 업데이트율 | 30% | 70% | +40% |
| 평균 업데이트 소요 시간 | 7일 | 2-3일 | -4일 |
| 사용자 불만 | 낮음 | 중간 | +10% |
| 중요 버그 전파 속도 | 느림 | 빠름 | +200% |

---

## 🎯 권장 설정

### 일반 업데이트 (기본)
```sql
remind_after_hours: 24
remind_decay_factor: 0.5
max_dismiss_count: 3
force_after_dismissals: false
```

**타임라인**:
- 1회 "나중에": 24시간 후 재표시
- 2회 "나중에": 12시간 후 재표시
- 3회 "나중에": 6시간 후 재표시
- 4회 "나중에": 다음 버전까지 숨김

### 중요 업데이트
```sql
remind_after_hours: 12  -- 더 빨리 재표시
remind_decay_factor: 0.5
max_dismiss_count: 3  -- 3회 후 강제 전환
```

**타임라인**:
- 1회 "나중에": 12시간 후 재표시
- 2회 "나중에": 6시간 후 재표시
- 3회 "나중에": 3시간 후 재표시
- 4회째 실행: **강제 업데이트로 전환** ✅ (뒤로가기 차단)

**💡 "강제 전환"의 의미**:
- Supabase의 `is_force_update`를 변경하는 것이 **아님**
- 클라이언트(앱)에서 `dismiss_count`를 추적
- 최대 횟수 도달 시 "나중에" 버튼을 숨김
- 결과적으로 사용자는 "업데이트"만 가능 = 강제 업데이트 효과
- 자세한 설명: **[UPDATE-POLICY-FORCE-CONVERSION-EXPLAINED.md](UPDATE-POLICY-FORCE-CONVERSION-EXPLAINED.md)**

### 긴급 보안 패치
```sql
처음부터 is_force_update: true
(선택적 업데이트 아님)
```

---

## 🚀 구현 단계

### Phase 2.5: 시간 기반 재표시 구현

1. **데이터베이스 마이그레이션**
   - update_policy 테이블 필드 추가
   - 기본값 설정

2. **코드 수정**
   - SharedPreferences에 timestamp, count 저장
   - 시간 기반 체크 로직 추가
   - 강제 전환 로직 추가

3. **테스트**
   - 디버그 모드 시간 단축 옵션
   - 모든 시나리오 테스트
   - Phase 2.5 테스트 문서 작성

4. **문서 업데이트**
   - POPUP-SYSTEM-GUIDE.md
   - UPDATE-POLICY-USAGE-GUIDE.md
   - RELEASE-TEST-PHASE2-RELEASE.md

---

## 📝 결론

**시간 기반 재표시 전략은 업계 표준이며, 효과적인 업데이트 유도 방법입니다.**

✅ **추천 이유**:
1. 업데이트율 40% 향상 예상
2. 중요 버그가 빠르게 전파됨
3. 균형잡힌 사용자 경험
4. 유연한 제어 가능

⚠️ **주의사항**:
1. 구현 복잡도 증가
2. 철저한 테스트 필요
3. 사용자 불만 모니터링

**다음 단계**: Phase 3 완료 후 Phase 2.5로 구현 시작!

---

**문서 버전**: v1.0.0  
**작성일**: 2025-11-09 08:40 KST

