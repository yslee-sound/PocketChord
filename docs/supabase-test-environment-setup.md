# Supabase 테스트 환경 설정 가이드

BuildConfig를 사용하여 디버그/릴리즈 환경을 자동으로 분리하는 방법입니다.

---

## 🎯 목표

- ✅ 디버그 빌드: 테스트 공지만 표시
- ✅ 릴리즈 빌드: 실제 공지만 표시
- ✅ 자동 전환: 수동 변경 불필요
- ✅ 실수 방지: 배포 시 테스트 공지 노출 차단

---

## 📝 설정 방법

### 1단계: build.gradle.kts 수정

**위치**: `app/build.gradle.kts`

```kotlin
android {
    // ...existing code...
    
    buildTypes {
        debug {
            // 디버그 빌드 설정
            applicationIdSuffix = ".debug"  // 패키지명에 .debug 추가 (선택사항)
            
            // Supabase app_id 설정
            buildConfigField(
                "String", 
                "SUPABASE_APP_ID", 
                "\"com.sweetapps.pocketchord.debug\""
            )
        }
        
        release {
            // 릴리즈 빌드 설정
            minifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Supabase app_id 설정
            buildConfigField(
                "String", 
                "SUPABASE_APP_ID", 
                "\"com.sweetapps.pocketchord\""
            )
        }
    }
    
    // BuildConfig 활성화
    buildFeatures {
        buildConfig = true
    }
}
```

### 2단계: AnnouncementRepository.kt 수정

**위치**: `app/src/main/java/.../data/supabase/repository/AnnouncementRepository.kt`

```kotlin
import com.sweetapps.pocketchord.BuildConfig  // import 추가

class AnnouncementRepository(
    private val client: SupabaseClient,
    private val appId: String = BuildConfig.SUPABASE_APP_ID  // 변경!
) {
    // ...existing code...
}
```

### 3단계: HomeScreen.kt 확인

**위치**: `app/src/main/java/.../ui/screens/HomeScreen.kt`

```kotlin
// 특별히 변경할 것 없음!
// Repository 생성 시 자동으로 BuildConfig 값 사용
val repository = AnnouncementRepository(
    supabase
    // appId 파라미터 생략 시 자동으로 BuildConfig.SUPABASE_APP_ID 사용
)
```

### 4단계: Supabase에 테스트 데이터 추가

```sql
-- 디버그 빌드용 (개발자만 보임)
INSERT INTO announcements (app_id, title, content, is_active)
VALUES (
    'com.sweetapps.pocketchord.debug',
    '🧪 디버그 모드',
    '이것은 디버그 빌드에서만 보이는 공지입니다.

개발자 전용 테스트 공지입니다.',
    true
);

-- 릴리즈 빌드용 (실제 사용자가 봄)
INSERT INTO announcements (app_id, title, content, is_active)
VALUES (
    'com.sweetapps.pocketchord',
    '🎉 PocketChord에 오신 것을 환영합니다!',
    '언제 어디서나 기타 코드를 학습하세요.

• 300개 이상의 코드
• 메트로놈 & 튜너
• 즐겨찾기 기능

즐거운 연습 되세요! 🎸',
    true
);
```

---

## 🧪 테스트 방법

### 디버그 빌드 테스트

```bash
# 방법 1: Android Studio
Run > Run 'app' (Shift + F10)

# 방법 2: 터미널
.\gradlew installDebug
```

**예상 결과**:
- 공지사항: "🧪 디버그 모드" 표시 ✅
- Logcat: `appId=com.sweetapps.pocketchord.debug`

### 릴리즈 빌드 테스트

```bash
# 방법 1: Android Studio
Build > Select Build Variant > release
Run > Run 'app'

# 방법 2: 터미널
.\gradlew installRelease
```

**예상 결과**:
- 공지사항: "🎉 PocketChord에 오신 것을 환영합니다!" 표시 ✅
- Logcat: `appId=com.sweetapps.pocketchord`

---

## 📊 환경별 동작 비교

| 빌드 타입 | app_id | 표시되는 공지 | 사용자 |
|-----------|--------|---------------|---------|
| **Debug** | `com.sweetapps.pocketchord.debug` | 🧪 디버그 모드 | 개발자 |
| **Release** | `com.sweetapps.pocketchord` | 🎉 환영합니다! | 실제 사용자 |

---

## 🔍 BuildConfig 확인 방법

### Logcat으로 확인

```kotlin
// HomeScreen.kt 또는 MainActivity.kt에 임시 로그 추가
Log.d("BuildConfig", "SUPABASE_APP_ID = ${BuildConfig.SUPABASE_APP_ID}")
Log.d("BuildConfig", "DEBUG = ${BuildConfig.DEBUG}")
Log.d("BuildConfig", "BUILD_TYPE = ${BuildConfig.BUILD_TYPE}")
```

**디버그 빌드 출력**:
```
D/BuildConfig: SUPABASE_APP_ID = com.sweetapps.pocketchord.debug
D/BuildConfig: DEBUG = true
D/BuildConfig: BUILD_TYPE = debug
```

**릴리즈 빌드 출력**:
```
D/BuildConfig: SUPABASE_APP_ID = com.sweetapps.pocketchord
D/BuildConfig: DEBUG = false
D/BuildConfig: BUILD_TYPE = release
```

---

## 🚀 실전 운영 시나리오

### 시나리오 1: 새 공지사항 테스트

**1. 디버그용 공지 작성**
```sql
INSERT INTO announcements (app_id, title, content, is_active)
VALUES (
    'com.sweetapps.pocketchord.debug',
    '🧪 [테스트] 버전 2.0 출시',
    '[초안] 새 버전이 출시되었습니다.

• 기능 1
• 기능 2

내용 확인 후 실제 공지로 전환 예정',
    true
);
```

**2. 디버그 빌드로 확인**
- Android Studio에서 디버그 실행
- 공지사항 디자인/문구 확인
- 팀원들에게 피드백 요청

**3. 피드백 반영**
```sql
UPDATE announcements
SET 
    title = '🧪 [테스트] 버전 2.0 출시 안내',
    content = '[수정] 훨씬 나아진 새 버전!

✨ 주요 기능
• 개선된 기능 1
• 완전히 새로운 기능 2

지금 바로 업데이트하세요!'
WHERE app_id = 'com.sweetapps.pocketchord.debug'
  AND is_active = true;
```

**4. 최종 승인 후 실제 공지 등록**
```sql
-- 테스트 공지 비활성화
UPDATE announcements 
SET is_active = false 
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 실제 공지 등록
INSERT INTO announcements (app_id, title, content, is_active)
VALUES (
    'com.sweetapps.pocketchord',  -- 실제 app_id
    '🎉 버전 2.0 출시 안내',
    '훨씬 나아진 새 버전!

✨ 주요 기능
• 개선된 기능 1
• 완전히 새로운 기능 2

지금 바로 업데이트하세요!',
    true
);
```

**5. 릴리즈 빌드로 최종 확인**
- Build Variant를 release로 변경
- 실제 공지가 보이는지 확인

---

### 시나리오 2: 긴급 공지 테스트

**1. 긴급 상황 발생**
```sql
-- 먼저 디버그로 테스트
INSERT INTO announcements (app_id, title, content, is_active)
VALUES (
    'com.sweetapps.pocketchord.debug',
    '⚠️ [테스트] 긴급 점검',
    '테스트: 긴급 점검 안내문',
    true
);
```

**2. 내용 확인 후 즉시 실제 공지로 전환**
```sql
-- 실제 공지 등록
INSERT INTO announcements (app_id, title, content, is_active)
VALUES (
    'com.sweetapps.pocketchord',
    '⚠️ 긴급 점검 안내',
    '현재 일부 기능에 문제가 발생했습니다...',
    true
);
```

---

## 💡 추가 환경 설정 (선택사항)

### Staging 환경 추가

```kotlin
// build.gradle.kts
android {
    buildTypes {
        debug { /* ...existing... */ }
        
        // Staging 환경 추가
        create("staging") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".staging"
            
            buildConfigField(
                "String", 
                "SUPABASE_APP_ID", 
                "\"com.sweetapps.pocketchord.staging\""
            )
        }
        
        release { /* ...existing... */ }
    }
}
```

**Supabase 데이터**:
```sql
-- Staging 환경용
INSERT INTO announcements (app_id, title, content, is_active)
VALUES (
    'com.sweetapps.pocketchord.staging',
    '🔧 스테이징 환경',
    'QA 테스트용 공지입니다.',
    true
);
```

---

## ⚠️ 주의사항

### 1. 기존 코드와의 호환성

**이전 방식 (하드코딩)**:
```kotlin
val repository = AnnouncementRepository(
    supabase,
    "com.sweetapps.pocketchord"  // 하드코딩
)
```

**새 방식 (BuildConfig)**:
```kotlin
val repository = AnnouncementRepository(
    supabase
    // 기본값으로 BuildConfig.SUPABASE_APP_ID 사용
)
```

### 2. 테스트 데이터 정리

```sql
-- 정기적으로 오래된 테스트 데이터 삭제
DELETE FROM announcements 
WHERE app_id = 'com.sweetapps.pocketchord.debug'
  AND created_at < NOW() - INTERVAL '30 days';
```

### 3. 프로덕션 배포 체크리스트

- [ ] 릴리즈 빌드로 테스트
- [ ] 실제 공지가 표시되는지 확인
- [ ] 디버그 공지가 표시되지 않는지 확인
- [ ] BuildConfig.SUPABASE_APP_ID 값 확인

---

## 🔧 트러블슈팅

### 문제 1: BuildConfig를 찾을 수 없음

**오류**:
```
Unresolved reference: BuildConfig
```

**해결**:
```kotlin
// build.gradle.kts에 추가
android {
    buildFeatures {
        buildConfig = true
    }
}
```

그리고 Sync Project with Gradle Files (Ctrl+Shift+O)

---

### 문제 2: app_id가 변경되지 않음

**원인**: 빌드 캐시

**해결**:
```bash
# 클린 빌드
.\gradlew clean
.\gradlew build

# 또는 Android Studio
Build > Clean Project
Build > Rebuild Project
```

---

### 문제 3: 디버그/릴리즈 둘 다 같은 공지 표시

**원인**: Supabase에 두 환경 데이터가 모두 없음

**확인**:
```sql
-- app_id별 데이터 확인
SELECT app_id, COUNT(*) as count, MAX(is_active) as has_active
FROM announcements
WHERE app_id LIKE 'com.sweetapps.pocketchord%'
GROUP BY app_id;
```

**예상 결과**:
```
app_id                                  | count | has_active
----------------------------------------|-------|------------
com.sweetapps.pocketchord               |   5   |   true
com.sweetapps.pocketchord.debug         |   3   |   true
```

---

## ✅ 완료!

이제 디버그/릴리즈 빌드가 자동으로 다른 공지사항을 표시합니다!

**핵심 정리**:
1. ✅ `build.gradle.kts`에서 `SUPABASE_APP_ID` 설정
2. ✅ `AnnouncementRepository`에서 `BuildConfig.SUPABASE_APP_ID` 사용
3. ✅ Supabase에 각 환경별 데이터 추가
4. ✅ 테스트 후 배포

**장점**:
- 🎯 자동 환경 분리
- 🛡️ 배포 시 실수 방지
- 🧪 안전한 테스트 환경
- 🚀 프로덕션 준비 완료

문서: `docs/supabase-test-environment-setup.md`

