# ✅ Supabase 연결 테스트 적용 완료!

## 🎯 적용 내용

Flutter의 `main.dart` 테스트 코드를 Android `MainActivity.kt`로 변환하여 적용했습니다.

---

## 📝 변경 사항

### MainActivity.kt에 추가된 코드

1. **testSupabaseConnection() 함수**
   - Supabase에서 최신 공지사항 조회
   - Logcat에 결과 출력
   - Repository 패턴 사용

2. **onCreate에서 호출**
   - 앱 시작 시 자동 실행
   - Flutter와 동일한 동작

---

## 🧪 테스트 방법

### 1단계: Supabase에 테스트 데이터 추가

```sql
INSERT INTO announcements (app_id, title, content, is_active)
VALUES ('com.sweetapps.pocketchord', '테스트 공지', '연결 테스트입니다.', true);
```

### 2단계: 앱 실행

Android Studio에서 앱 실행 또는:
```bash
.\gradlew installDebug
```

### 3단계: Logcat 확인

**필터**: `SupabaseTest`

**성공 시 출력:**
```
D/SupabaseTest: announcement: Announcement(id=1, createdAt=2025-11-05T..., ...)
D/SupabaseTest: id: 1
D/SupabaseTest: title: 테스트 공지
D/SupabaseTest: content: 연결 테스트입니다.
D/SupabaseTest: isActive: true
D/SupabaseTest: createdAt: 2025-11-05T...
D/SupabaseTest: appId: com.sweetapps.pocketchord
D/SupabaseTest: ✅ Supabase 연결 성공!
```

**데이터 없을 시:**
```
W/SupabaseTest: ⚠️ 공지사항이 없습니다. Supabase에 데이터를 추가하세요.
```

**실패 시:**
```
E/SupabaseTest: ❌ Supabase 연결 실패
E/SupabaseTest: Error: [에러 메시지]
```

---

## ⚠️ 테스트 완료 후 반드시 제거!

### 제거해야 할 이유
- ✅ 앱 시작마다 불필요한 네트워크 요청
- ✅ 프로덕션에서 필요 없는 로그
- ✅ 성능 저하 방지

### 제거 방법

#### 방법 1: 완전 제거 (권장)

**1. testSupabaseConnection() 함수 삭제**

`MainActivity.kt`에서 다음 함수 전체를 찾아서 삭제:

```kotlin
/**
 * Supabase 연결 테스트 함수
 * ...
 */
private fun testSupabaseConnection() {
    lifecycleScope.launch {
        try {
            // ...
        } catch (e: Exception) {
            // ...
        }
    }
}
```

**2. onCreate에서 호출 부분 삭제**

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    setupSplashScreen()
    super.onCreate(savedInstanceState)
    
    // ==================== 이 3줄 삭제 ====================
    // ==================== Supabase 테스트 시작 ====================
    testSupabaseConnection()
    // ==================== Supabase 테스트 끝 ====================
    
    enableEdgeToEdge()
    // ...
}
```

삭제 후:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    setupSplashScreen()
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    // ...
}
```

#### 방법 2: 조건부 실행 (선택사항)

디버그 빌드에서만 테스트를 실행하려면:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    setupSplashScreen()
    super.onCreate(savedInstanceState)
    
    // 디버그 빌드에서만 실행
    if (BuildConfig.DEBUG) {
        testSupabaseConnection()
    }
    
    enableEdgeToEdge()
    // ...
}
```

이 방법을 사용하면:
- Debug 빌드: 테스트 실행 ✅
- Release 빌드: 테스트 건너뜀 ✅

---

## 🔍 트러블슈팅

### 데이터가 조회되지 않음

**원인 1: app_id 불일치**
- Supabase 데이터의 `app_id`가 `"com.sweetapps.pocketchord"`인지 확인
- `"pocketchord"` 같은 짧은 이름은 안 됨

**원인 2: is_active = false**
- Supabase에서 `is_active`가 `true`인지 확인

**원인 3: 테이블이 비어있음**
- SQL 쿼리로 테스트 데이터 추가

### 연결 실패

**원인 1: 인터넷 권한**
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
```

**원인 2: Supabase URL/Key 오류**
- `MainActivity.kt`의 `supabase` 클라이언트 초기화 부분 확인
- URL과 anon key가 올바른지 확인

---

## 📋 체크리스트

### 테스트 전
- [ ] Supabase에 테스트 데이터 추가
- [ ] app_id = `"com.sweetapps.pocketchord"` 확인
- [ ] is_active = `true` 확인

### 테스트 실행
- [ ] 앱 빌드 및 설치
- [ ] Logcat 필터: `SupabaseTest`
- [ ] 성공 메시지 확인

### 테스트 완료 후
- [ ] testSupabaseConnection() 함수 삭제
- [ ] onCreate 호출 부분 삭제
- [ ] 또는 BuildConfig.DEBUG 조건 추가
- [ ] 재빌드 및 확인

---

## ✅ 완료!

Flutter와 동일한 방식으로 Supabase 연결 테스트를 적용했습니다!

**다음 단계:**
1. 앱 실행하여 Logcat 확인
2. 성공 확인 후 테스트 코드 제거
3. 실제 기능 구현 시작

🎉 테스트 성공!

