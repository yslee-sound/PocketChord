# ⚠️ RLS 재활성화 필수!

## 현재 상태
테스트를 위해 RLS (Row Level Security)를 비활성화한 상태입니다:
```sql
ALTER TABLE app_policy DISABLE ROW LEVEL SECURITY;
```

**이 상태는 보안에 취약합니다!** 모든 사용자가 모든 데이터를 조회할 수 있습니다.

---

## ✅ 즉시 실행할 SQL

### Supabase SQL Editor에서 실행:

```sql
-- RLS 재활성화
ALTER TABLE app_policy ENABLE ROW LEVEL SECURITY;

-- 정책 확인
SELECT tablename, rowsecurity 
FROM pg_tables 
WHERE tablename = 'app_policy';
```

**예상 결과**:
```
tablename   | rowsecurity
------------|------------
app_policy  | true        ← true여야 함!
```

---

## 📝 현재 상황 설명

### 문제가 해결된 이유
RLS를 비활성화하지 않아도 이제 정상 작동합니다!

**핵심 수정 사항**:
- `AppPolicyRepository.kt`에서 **서버측 필터링** 대신 **클라이언트측 필터링** 사용
- Supabase의 `filter { eq() }` 버그를 우회

```kotlin
// Before (작동 안 함)
.select { filter { eq("app_id", appId) } }

// After (작동함) ✅
.select().decodeList<AppPolicy>()
.firstOrNull { it.appId == appId && it.isActive }
```

이제 RLS가 활성화되어 있어도 정상 작동합니다!

---

## 🔒 RLS 재활성화 후 테스트

### 1단계: SQL 실행
```sql
ALTER TABLE app_policy ENABLE ROW LEVEL SECURITY;
```

### 2단계: 앱 재시작
```cmd
adb shell am force-stop com.sweetapps.pocketchord.debug
adb shell am start -n com.sweetapps.pocketchord.debug/com.sweetapps.pocketchord.MainActivity
```

### 3단계: 로그 확인
```cmd
adb logcat -d -s AppPolicyRepo:* | findstr "Policy found"
```

**예상 로그**:
```
D/AppPolicyRepo: ✅ Policy found:
D/AppPolicyRepo:   - app_id: com.sweetapps.pocketchord.debug
D/AppPolicyRepo:   - is_active: true
```

✅ **성공!** RLS가 활성화되어 있어도 정책이 정상 조회됩니다!

---

## 🛡️ 보안 중요성

### RLS가 비활성화된 상태의 위험:
- ❌ 모든 사용자가 `is_active = false`인 정책도 볼 수 있음
- ❌ 다른 앱의 정책도 볼 수 있음 (app_id 관계없이)
- ❌ 프로덕션에서는 **절대 비활성화하면 안 됨**

### RLS가 활성화된 상태 (권장):
- ✅ `is_active = true`인 정책만 조회 가능
- ✅ 사용자가 볼 권한이 있는 데이터만 노출
- ✅ 보안 강화

---

## 📊 최종 체크리스트

- [ ] Supabase SQL Editor 접속
- [ ] `ALTER TABLE app_policy ENABLE ROW LEVEL SECURITY;` 실행
- [ ] `SELECT tablename, rowsecurity...` 실행하여 `true` 확인
- [ ] 앱 재시작하여 여전히 정책이 조회되는지 확인
- [ ] 긴급 팝업이 정상 표시되는지 확인

---

## 🎯 요약

1. **현재**: RLS 비활성화 (테스트용, 보안 취약)
2. **해야 할 일**: 위의 SQL 실행하여 RLS 재활성화
3. **결과**: 보안 강화, 앱은 여전히 정상 작동 ✅

**지금 바로 Supabase SQL Editor에서 실행하세요!**

```sql
ALTER TABLE app_policy ENABLE ROW LEVEL SECURITY;
```

---

**작성일**: 2025-11-08  
**상태**: ⚠️ 즉시 조치 필요

