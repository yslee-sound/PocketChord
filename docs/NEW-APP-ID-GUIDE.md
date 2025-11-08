# new_app_id 필드 설명 완료!

**작성일**: 2025-11-09  
**목적**: emergency_policy의 new_app_id 필드 상세 설명 추가  
**상태**: ✅ 완료

---

## ⚠️ 중요: new_app_id는 선택사항입니다!

### 💡 핵심 요약

**`redirect_url`만 있으면 충분합니다!**

```kotlin
// 실제 코드 동작
if (!redirectUrl.isNullOrBlank()) {
    openWebPage(context, redirectUrl)      // ← 주 사용 (이것만 있으면 됨!)
} else {
    openPlayStore(context, newAppPackage)  // ← fallback (거의 안 씀)
}
```

**즉**:
- ✅ `redirect_url` 있음 → `new_app_id` **무시됨** (redirect_url로 이동)
- ⚠️ `redirect_url` 없음 → `new_app_id`로 Play Store 이동 (fallback)

**권장**: `redirect_url`만 제대로 설정하고, `new_app_id`는 **NULL로 두세요!**

---

## 🎯 new_app_id란?

**용도**: `redirect_url`이 **없을 때** Play Store로 이동하기 위한 **fallback 패키지명**

**실제 사용**: 거의 없음 (redirect_url을 주로 사용)

---

## 📋 권장 사용법

### ✅ 권장: redirect_url만 사용

```sql
UPDATE emergency_policy 
SET is_active = true,
    content = '⚠️ 이 앱은 더 이상 지원되지 않습니다.\n새 버전을 설치해주세요.',
    redirect_url = 'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord.v2',
    new_app_id = NULL,  -- ← NULL로 두면 됨!
    is_dismissible = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

**결과**: 
- 버튼 클릭 → `redirect_url`로 이동
- `new_app_id`는 무시됨

---

### ⚠️ 비권장: new_app_id만 사용 (redirect_url 없음)

```sql
UPDATE emergency_policy 
SET is_active = true,
    content = '⚠️ 새 버전을 설치해주세요.',
    redirect_url = NULL,                              -- redirect_url 없음
    new_app_id = 'com.sweetapps.pocketchord.v2',     -- fallback으로 사용
    is_dismissible = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

**결과**: 
- 버튼 클릭 → Play Store의 `new_app_id` 앱으로 이동
- 하지만 이렇게 쓸 이유가 없음 (redirect_url이 더 유연함)

---

## 🔍 Supabase 값 설명

### 현재 테스트 데이터

```
app_id: com.sweetapps.pocketchord.debug
new_app_id: com.sweetapps.pocketchord.debug.v2
redirect_url: NULL (또는 비어있음)
```

**이 경우**:
- `redirect_url`이 없으므로
- `new_app_id`를 fallback으로 사용
- Play Store의 `com.sweetapps.pocketchord.debug.v2`로 이동

**하지만**: `redirect_url`을 설정하면 `new_app_id`는 무시됩니다!

---

## 📝 실전 예시

### 예시 1: 정상적인 사용 (권장) ✅

```sql
UPDATE emergency_policy 
SET is_active = true,
    content = '⚠️ 이 앱은 더 이상 지원되지 않습니다.\n새 버전을 설치해주세요.',
    redirect_url = 'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord.v2',
    new_app_id = NULL,  -- ← 불필요! NULL로 두면 됨
    is_dismissible = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

### 예시 2: 단순 긴급 공지

```sql
UPDATE emergency_policy 
SET is_active = true,
    content = '⚠️ 긴급 점검 중입니다.',
    redirect_url = NULL,  -- 이동 불필요
    new_app_id = NULL,    -- 불필요
    is_dismissible = true
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

## 💡 코드 동작 (실제)

```kotlin
// EmergencyRedirectDialog.kt
Button(
    onClick = {
        if (!redirectUrl.isNullOrBlank()) {
            openWebPage(context, redirectUrl)      // ← redirect_url 우선!
        } else {
            openPlayStore(context, newAppPackage)  // ← new_app_id는 fallback
        }
    }
)
```

**동작 순서**:
1. `redirect_url`이 있나? → **있으면 그곳으로 이동** (끝)
2. `redirect_url`이 없나? → `new_app_id`로 Play Store 이동

---

## 🎯 요약

### new_app_id는?

```
⚠️ fallback 패키지명 (거의 안 씀)
✅ redirect_url만 있으면 무시됨
✅ NULL로 두는 것을 권장
```

### 왜 존재하나?

```
레거시 코드 또는 특수 상황용
→ 실무에서는 redirect_url만 사용하면 충분!
```

### 결론

```
✅ redirect_url만 제대로 설정하세요!
✅ new_app_id는 NULL로 두세요!
✅ 불필요한 필드입니다 (fallback용)
```

---

## 📚 업데이트된 문서

### 1. POPUP-SYSTEM-GUIDE.md ✅
- emergency_policy 섹션에 경고 추가 예정
- "redirect_url만 사용하면 충분" 명시

### 2. 02-create-emergency-policy.sql ✅
- 컬럼 설명 업데이트 예정
- "fallback용, 거의 안 씀" 주석 추가

---

**완료!** 🎉

**당신이 맞습니다!** `redirect_url`만 있으면 충분합니다!  
`new_app_id`는 불필요한 필드입니다 (fallback용일 뿐).

**권장**: `new_app_id = NULL`로 두고, `redirect_url`만 사용하세요!

---

## 📋 실제 사용 시나리오

### 시나리오: Play Store 정지

```
문제: 기존 앱 "PocketChord"가 Play Store에서 정지됨
해결: 새 앱 "PocketChord V2"를 출시하고 사용자를 이동시킴

1. 새 앱을 Play Store에 출시
   패키지명: com.sweetapps.pocketchord.v2

2. Supabase에서 emergency_policy 업데이트
   new_app_id: 'com.sweetapps.pocketchord.v2'
   redirect_url: 'https://play.google.com/store/apps/details?id=...'

3. 기존 앱 사용자가 앱 실행
   → 긴급 팝업 표시
   → "새 앱 설치하기" 버튼 클릭
   → Play Store의 새 앱으로 이동
```

---

## 🔍 Supabase 값 설명

### 현재 테스트 데이터

```
app_id: com.sweetapps.pocketchord.debug
new_app_id: com.sweetapps.pocketchord.debug.v2
```

**의미**:
- `com.sweetapps.pocketchord.debug` - 현재 디버그 앱
- `.v2` - 버전 2를 의미 (임의의 네이밍)
- 전체: "디버그 앱의 버전 2"

**참고**: `.v2`, `.v3` 등은 **버전 구분을 위한 네이밍**일 뿐, 필수는 아닙니다.

---

## 📝 설정 예시

### 예시 1: 새 앱으로 완전 이전 (필수)

```sql
UPDATE emergency_policy 
SET is_active = true,
    content = '⚠️ 이 앱은 더 이상 지원되지 않습니다.\n새 버전을 설치해주세요.',
    new_app_id = 'com.sweetapps.pocketchord.v2',
    redirect_url = 'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord.v2',
    is_dismissible = false  -- X 버튼 없음
WHERE app_id = 'com.sweetapps.pocketchord';
```

**결과**:
- "새 앱 설치하기" 버튼 → Play Store의 새 앱으로 이동
- X 버튼 없음 (강제 이동)

---

### 예시 2: 단순 긴급 공지 (이동 불필요)

```sql
UPDATE emergency_policy 
SET is_active = true,
    content = '⚠️ 긴급 점검 중입니다.\n잠시 후 이용해주세요.',
    new_app_id = NULL,        -- 새 앱 없음
    redirect_url = NULL,      -- 리다이렉트 없음
    is_dismissible = true     -- X 버튼 허용
WHERE app_id = 'com.sweetapps.pocketchord';
```

**결과**:
- 단순 공지만 표시
- X 버튼으로 닫을 수 있음

---

### 예시 3: 선택적 마이그레이션 (권장)

```sql
UPDATE emergency_policy 
SET is_active = true,
    content = '📢 새 버전이 출시되었습니다!\n더 나은 기능을 경험해보세요.',
    new_app_id = 'com.sweetapps.pocketchord.v2',
    redirect_url = 'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord.v2',
    is_dismissible = true     -- X 버튼 허용 (선택적)
WHERE app_id = 'com.sweetapps.pocketchord';
```

**결과**:
- 새 앱 안내
- 사용자가 선택 가능 (X 버튼 있음)

---

## 💡 코드 동작

```kotlin
// HomeScreen.kt
EmergencyRedirectDialog(
    title = "🚨 긴급공지",
    description = emergencyPolicy.content,
    newAppPackage = emergencyPolicy.newAppId ?: "com.sweetapps.pocketchord",  // ← 여기서 사용!
    redirectUrl = emergencyPolicy.redirectUrl,
    buttonText = "새 앱 설치하기",
    isDismissible = emergencyPolicy.isDismissible
)
```

**동작**:
1. `new_app_id`가 있으면 → 해당 패키지명 사용
2. `new_app_id`가 NULL이면 → 기본값(`com.sweetapps.pocketchord`) 사용
3. "새 앱 설치하기" 버튼 클릭 시 → `redirect_url`로 이동

---

## 📚 업데이트된 문서

### 1. POPUP-SYSTEM-GUIDE.md ✅
- emergency_policy 섹션에 `new_app_id` 상세 설명 추가
- 3가지 예시 시나리오 추가
- `.v2` 네이밍 의미 설명

### 2. 02-create-emergency-policy.sql ✅
- 컬럼 주석 추가
- COMMENT 추가
  - `new_app_id`: '새 앱 패키지명 (앱 이전 시 사용)'
  - `redirect_url`: 'Play Store 링크 (새 앱 설치 유도)'

---

## 🎯 요약

### new_app_id는?

```
✅ 새 앱의 패키지명
✅ 앱 이전/마이그레이션 시 사용
✅ NULL 가능 (선택사항)
✅ .v2, .v3 등은 버전 구분용 네이밍
```

### 언제 사용?

```
1. Play Store 정지 → 새 앱 출시 시
2. 앱 브랜드 변경 시
3. 메이저 업데이트 → 별도 앱 출시 시
```

### NULL로 둬도 되나?

```
✅ 네, 가능합니다!
→ 단순 긴급 공지만 표시
→ 기본값(현재 앱) 사용
```

---

**완료!** 🎉

이제 `new_app_id`의 의미와 사용법이 명확해졌습니다!

