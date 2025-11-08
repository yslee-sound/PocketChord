# new_app_id 컬럼 제거 완료!

**작성일**: 2025-11-09  
**목적**: emergency_policy에서 불필요한 new_app_id 컬럼 제거  
**상태**: ✅ 완료

---

## ✅ 완료된 작업

### 1. SQL 마이그레이션 스크립트 생성 ✅
**파일**: `docs/sql/06-remove-new-app-id.sql`

```sql
ALTER TABLE public.emergency_policy 
DROP COLUMN IF EXISTS new_app_id;
```

### 2. 코드 수정 ✅

#### HomeScreen.kt
```kotlin
// Before
newAppPackage = emergencyPolicy!!.newAppId ?: "com.sweetapps.pocketchord",

// After
newAppPackage = "com.sweetapps.pocketchord",  // 기본값 (redirect_url이 있으면 무시됨)
```

#### EmergencyPolicy.kt
```kotlin
// Before
@SerialName("new_app_id")
val newAppId: String? = null,

// After
// ← 필드 제거됨!
```

---

## 🚀 실행 방법

### 1. Supabase에서 SQL 실행

```bash
1. Supabase 대시보드 접속
2. SQL Editor 열기
3. docs/sql/06-remove-new-app-id.sql 내용 복사
4. 실행
```

### 2. 앱 빌드

```bash
./gradlew clean build
```

---

## 📊 변경 전/후 비교

### Before (3개 필드)
```sql
CREATE TABLE emergency_policy (
    ...
    content TEXT NOT NULL,
    redirect_url TEXT,
    new_app_id TEXT,          -- ← 불필요!
    is_dismissible BOOLEAN
);
```

### After (2개 필드) ✅
```sql
CREATE TABLE emergency_policy (
    ...
    content TEXT NOT NULL,
    redirect_url TEXT,        -- ← 이것만 있으면 충분!
    is_dismissible BOOLEAN
);
```

---

## 💡 왜 제거했나?

### 이유

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

**결론**: 
- `redirect_url`만 있으면 `new_app_id`는 **절대 사용되지 않음**
- 불필요한 필드이므로 제거

---

## 📝 사용법 (변경 후)

### 권장 방법 ✅

```sql
UPDATE emergency_policy 
SET is_active = true,
    content = '⚠️ 이 앱은 더 이상 지원되지 않습니다.\n새 버전을 설치해주세요.',
    redirect_url = 'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord.v2',
    is_dismissible = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

**결과**: 
- 버튼 클릭 → `redirect_url`로 이동
- `newAppPackage`는 기본값(`com.sweetapps.pocketchord`) 사용 (무시됨)

---

## ✅ 체크리스트

- [x] SQL 마이그레이션 스크립트 생성
- [x] HomeScreen.kt 수정 (newAppId 제거)
- [x] EmergencyPolicy.kt 수정 (필드 제거)
- [x] 컴파일 에러 확인 (없음 ✅)
- [ ] Supabase에서 SQL 실행 (사용자가 실행)
- [ ] 앱 테스트 (사용자가 테스트)

---

## 🎉 완료!

- ✅ 불필요한 `new_app_id` 컬럼 제거
- ✅ 코드 단순화
- ✅ `redirect_url`만 사용하면 됨!

**이제 Supabase에서 SQL을 실행하고 앱을 다시 빌드하세요!** 🚀

