# 🚨 중요: Release Test 올바른 사용법

## ✅ 핵심 개념

### 당신이 맞습니다!

**실제 사용자에게 영향 없이 테스트하려면:**

```
Supabase 테이블:
┌────────────────────────────────────────┐
│ app_id: com.sweetapps.pocketchord      │ ← Release용 (실제 사용자)
│ is_active: false  ← 비활성화!          │
└────────────────────────────────────────┘

┌────────────────────────────────────────┐
│ app_id: com.sweetapps.pocketchord.     │ ← Release Test용 (테스트)
│         releasetest                     │
│ is_active: true  ← 활성화!             │
└────────────────────────────────────────┘
```

## 🔧 코드 수정 완료

### build.gradle.kts 수정됨

```kotlin
create("releaseTest") {
    // ✅ Release Test 전용 app_id
    buildConfigField(
        "String",
        "SUPABASE_APP_ID",
        "\"com.sweetapps.pocketchord.releasetest\""  // ✅ 수정 완료!
    )
    
    // Application ID도 다름
    applicationIdSuffix = ".releasetest"
}
```

## 📝 다음 단계

### 1. Supabase에 Release Test용 데이터 추가

```sql
INSERT INTO app_policy (
    app_id,
    is_active,
    active_popup_type,
    content,
    download_url,
    min_supported_version,
    latest_version_code
) VALUES (
    'com.sweetapps.pocketchord.releasetest',  -- ✅ Release Test 전용!
    true,
    'force_update',
    '[테스트] 필수 업데이트가 있습니다.',
    'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord',
    3,
    3
)
ON CONFLICT (app_id) 
DO UPDATE SET
    is_active = EXCLUDED.is_active,
    active_popup_type = EXCLUDED.active_popup_type,
    content = EXCLUDED.content,
    download_url = EXCLUDED.download_url,
    min_supported_version = EXCLUDED.min_supported_version,
    latest_version_code = EXCLUDED.latest_version_code;
```

### 2. Release용은 비활성화

```sql
UPDATE app_policy
SET is_active = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

### 3. 재빌드 및 테스트

```bash
# 1. Clean 빌드
.\gradlew.bat clean

# 2. Release Test 빌드
.\gradlew.bat assembleReleaseTest

# 3. 설치
.\gradlew.bat installReleaseTest

# 4. 실행
adb shell am start -n com.sweetapps.pocketchord.releasetest/com.sweetapps.pocketchord.MainActivity

# 5. 로그 확인
adb logcat -d | Select-String "SUPABASE_APP_ID"
```

**예상 로그**:
```
D/HomeScreen: SUPABASE_APP_ID=com.sweetapps.pocketchord.releasetest
```

## 🎯 최종 확인

### 각 빌드 타입의 SUPABASE_APP_ID

| 빌드 | SUPABASE_APP_ID | 실제 사용자 영향 |
|------|-----------------|-----------------|
| debug | `com.sweetapps.pocketchord.debug` | ❌ 없음 |
| **releaseTest** | `com.sweetapps.pocketchord.releasetest` | ❌ **없음!** |
| release | `com.sweetapps.pocketchord` | ✅ 있음 |

## ⚠️ 이전 설명 수정

제가 이전에 잘못 설명한 부분:
- ❌ "Release Test가 Release와 같은 app_id를 써야 실제 환경 테스트"
- ✅ **올바름**: "Release Test가 자기만의 app_id를 써야 안전하게 테스트"

**당신 말이 100% 맞습니다!** 🎉

---

**작성일**: 2025-11-08  
**상태**: ✅ 코드 수정 완료 → Supabase 설정 후 테스트

