# new_app_id 필드 제거 기록

**작성일**: 2025-11-09  
**작업**: emergency_policy에서 new_app_id 컬럼 제거  
**상태**: ✅ 완료

---

## 📋 삭제된 문서

1. ❌ `NEW-APP-ID-GUIDE.md` - new_app_id 사용 가이드
2. ❌ `NEW-APP-ID-REMOVAL-COMPLETE.md` - 제거 완료 보고서

**삭제 이유**: 
- `new_app_id` 컬럼이 Supabase와 코드에서 완전히 제거됨
- 더 이상 사용되지 않는 필드에 대한 문서는 불필요
- 혼란을 방지하기 위해 삭제

---

## 🔍 발견 사항

### 문제
```
emergency_policy의 new_app_id 필드가 불필요함을 발견
→ redirect_url만 있으면 new_app_id는 무시됨 (fallback일 뿐)
→ 사용자가 지적: "왜 불필요한 필드를 적어야 하나?"
```

### 조사 결과
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

**결론**: redirect_url만 있으면 new_app_id는 절대 사용되지 않음!

---

## ✅ 수행된 작업

### 1. SQL 마이그레이션 스크립트 생성
**파일**: `docs/sql/06-remove-new-app-id.sql`

```sql
ALTER TABLE public.emergency_policy 
DROP COLUMN IF EXISTS new_app_id;
```

### 2. 코드 수정

#### HomeScreen.kt
```kotlin
// Before
newAppPackage = emergencyPolicy!!.newAppId ?: "com.sweetapps.pocketchord"

// After
newAppPackage = "com.sweetapps.pocketchord"  // 기본값
```

#### EmergencyPolicy.kt
```kotlin
// newAppId 필드 제거
@SerialName("new_app_id")
val newAppId: String? = null,  // ← 이 줄 삭제됨
```

### 3. 문서 업데이트
- ✅ `POPUP-SYSTEM-GUIDE.md` - redirect_url만 사용하면 충분하다고 명시
- ✅ `02-create-emergency-policy.sql` - new_app_id 컬럼 제거

---

## 📊 최종 상태

### emergency_policy 테이블 (최종)

```sql
CREATE TABLE emergency_policy (
    id BIGINT PRIMARY KEY,
    app_id TEXT NOT NULL,
    is_active BOOLEAN DEFAULT FALSE,
    content TEXT NOT NULL,
    redirect_url TEXT,           -- ✅ 이것만 있으면 됨!
    is_dismissible BOOLEAN DEFAULT TRUE
);
```

**제거된 필드**: `new_app_id TEXT`

---

## 💡 교훈

### 배운 점
```
1. 사용자의 지적이 정확했음
   → "왜 redirect_url만 있어도 되는데 new_app_id를 적어야 하나?"

2. 코드를 자세히 분석한 결과
   → new_app_id는 redirect_url이 없을 때만 사용되는 fallback
   → 실무에서는 항상 redirect_url을 사용하므로 불필요

3. 불필요한 필드는 제거하는 것이 좋음
   → 혼란 감소
   → 코드 단순화
   → 유지보수 용이
```

### 권장 사항
```
✅ redirect_url만 사용
✅ 명확한 단일 책임 (redirect_url = 이동할 URL)
✅ fallback 로직 제거 (단순화)
```

---

## 📚 참고 문서

### 현재 사용 가능한 문서
- `POPUP-SYSTEM-GUIDE.md` - 전체 팝업 시스템 가이드
- `02-create-emergency-policy.sql` - 테이블 생성 SQL
- `06-remove-new-app-id.sql` - new_app_id 제거 SQL

### 삭제된 문서 (이 기록에서 확인 가능)
- `NEW-APP-ID-GUIDE.md` - 제거됨 (필드 삭제로 불필요)
- `NEW-APP-ID-REMOVAL-COMPLETE.md` - 제거됨 (이 기록으로 통합)

---

## 🎯 요약

### 작업 내용
```
1. new_app_id 필드가 불필요함을 발견 (사용자 지적)
2. 코드 분석으로 확인 (fallback일 뿐, redirect_url 우선)
3. SQL 마이그레이션 스크립트 생성
4. 코드에서 new_app_id 제거
5. 관련 문서 삭제 (혼란 방지)
```

### 최종 결론
```
✅ redirect_url만 사용하면 충분!
✅ new_app_id 제거 완료
✅ 코드 단순화 완료
✅ 불필요한 문서 삭제 완료
```

---

**작성일**: 2025-11-09  
**작성자**: GitHub Copilot  
**승인자**: 사용자 (불필요한 필드 지적)

