# Supabase 연동 가이드 (3~7단계)

> PocketChord 앱에 Supabase 데이터베이스를 연동한 전체 과정 정리  
> 작업 일자: 2025-01-05

## 목차
1. [3단계: 의존성 설치](#3단계-의존성-설치)
2. [4단계: 인터넷 권한 추가](#4단계-인터넷-권한-추가)
3. [5단계: Supabase 클라이언트 초기화](#5단계-supabase-클라이언트-초기화)
4. [6단계: 데이터 모델 생성](#6단계-데이터-모델-생성)
5. [7단계: 데이터 조회 및 UI 표시](#7단계-데이터-조회-및-ui-표시)
6. [트러블슈팅](#트러블슈팅)

---

## 3단계: 의존성 설치

### 개요
Supabase Kotlin 클라이언트와 필요한 라이브러리를 프로젝트에 추가합니다.

### 작업 내용

#### 1) `gradle/libs.versions.toml` 수정

```toml
[versions]
# ...existing versions...
kotlin = "2.2.21"  # ⚠️ 중요: 2.2.21로 업그레이드 (Supabase 3.x 호환)
ktor = "3.3.1"
supabase = "3.2.6"

[libraries]
# ...existing libraries...
supabase-bom = { group = "io.github.jan-tennert.supabase", name = "bom", version.ref = "supabase" }
supabase-postgrest = { group = "io.github.jan-tennert.supabase", name = "postgrest-kt", version.ref = "supabase" }
ktor-client-android = { group = "io.ktor", name = "ktor-client-android", version.ref = "ktor" }

[plugins]
# ...existing plugins...
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

#### 2) `app/build.gradle.kts` 수정

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)  // ✅ 추가
}

dependencies {
    // ...existing dependencies...
    
    // Supabase 관련
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.android)
}
```

### 주요 개념

- **BOM (Bill of Materials)**: 여러 관련 라이브러리의 버전을 자동으로 맞춰주는 버전 카탈로그
- **kotlinx.serialization**: JSON ↔ Kotlin 객체 자동 변환을 위한 플러그인
- **Ktor client**: Supabase가 내부적으로 사용하는 HTTP 클라이언트 (Android 엔진 필요)

### 버전 호환성 중요 사항

⚠️ **Kotlin 버전 불일치 해결**
- 초기 Kotlin 2.0.21 사용 시 컴파일 에러 발생:
  ```
  Module was compiled with an incompatible version of Kotlin. 
  The binary version of its metadata is 2.2.0, expected version is 2.0.0.
  ```
- **해결**: Kotlin 버전을 2.2.21로 업그레이드
- **이유**: Supabase 3.2.6과 Ktor 3.3.1이 Kotlin 2.2.x로 빌드되어 있음

---

## 4단계: 인터넷 권한 추가

### 개요
Android 앱이 네트워크 통신을 하려면 매니페스트에 권한을 선언해야 합니다.

### 작업 내용

`app/src/main/AndroidManifest.xml` 수정:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- ✅ 추가: 인터넷 권한 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <application
        ...>
        ...
    </application>
</manifest>
```

### 위치
- `<manifest>` 태그 안
- `<application>` 태그 **바깥**

### 참고
- `INTERNET`: 필수 (네트워크 통신)
- `ACCESS_NETWORK_STATE`: 선택 (네트워크 상태 확인용)
- 런타임 권한 요청 불필요 (설치 시 자동 부여)

---

## 5단계: Supabase 클라이언트 초기화

### 개요
앱 전체에서 재사용할 Supabase 클라이언트를 한 번만 생성합니다.

### 작업 내용

`app/src/main/java/com/sweetapps/pocketchord/MainActivity.kt` 상단에 추가:

```kotlin
package com.sweetapps.pocketchord

// ...existing imports...

// Supabase 관련 import
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

// ✅ Supabase 클라이언트 생성 (전역, MainActivity 클래스 바깥)
val supabase = createSupabaseClient(
    supabaseUrl = "https://bajurdtglfaiqilnpamt.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." // Anon/Public 키
) {
    install(Postgrest)  // PostgREST 모듈 설치
}
```

### 위치
- 파일: `MainActivity.kt`
- 위치: **class MainActivity 바깥**, 파일 최상위
- 순서: import 구문들 → supabase 클라이언트 → 데이터 클래스 → MainActivity 클래스

### Supabase 대시보드에서 키 확인 방법

1. https://app.supabase.com 접속
2. 프로젝트 선택
3. **Settings** → **API**
4. **Project URL**: `supabaseUrl`에 사용
5. **anon/public key**: `supabaseKey`에 사용 (⚠️ service_role 키는 절대 앱에 넣지 말 것!)

### 보안 권장사항

**현재 상태**: 키가 코드에 하드코딩됨 (테스트 편의)

**운영 배포 시 권장**:
```kotlin
// BuildConfig 사용 예시 (build.gradle.kts에서 주입)
val supabase = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_ANON_KEY
) {
    install(Postgrest)
}
```

---

## 6단계: 데이터 모델 생성

### 개요
Supabase 테이블의 데이터를 Kotlin 객체로 매핑하기 위한 데이터 클래스를 정의합니다.

### 작업 내용

`MainActivity.kt`의 supabase 클라이언트 바로 아래에 추가:

```kotlin
@Serializable
data class Announcement(
    val id: Int,
    val title: String,
    val message: String? = null  // nullable 필드
)
```

### 위치
- supabase 클라이언트 생성 코드 바로 아래
- MainActivity 클래스 바깥 (파일 최상위)

### 주요 개념

**@Serializable**
- kotlinx.serialization 라이브러리의 애노테이션
- JSON 자동 직렬화/역직렬화 지원
- Supabase의 `.decodeList<T>()` 호출 시 필요

**필드 매핑 규칙**
- 기본: Kotlin 필드명 = DB 컬럼명
- 다를 경우: `@SerialName` 사용
  ```kotlin
  @Serializable
  data class User(
      @SerialName("user_id") val id: Int,  // DB: user_id → Kotlin: id
      val name: String
  )
  ```

### 데이터베이스 스키마 예시

```sql
-- announcements 테이블 (현재 사용 중)
create table public.announcements (
  id int primary key,
  title text not null,
  message text
);

-- instruments 테이블 (향후 사용 예정)
create table public.instruments (
  id int primary key,
  name text not null
);
```

---

## 7단계: 데이터 조회 및 UI 표시

### 개요
Compose UI에서 Supabase 데이터를 비동기로 가져와 LazyColumn으로 표시합니다.

### 작업 내용

#### 1) InstrumentsList 컴포저블 추가

`MainActivity.kt`에 추가 (6단계 데이터 클래스 아래):

```kotlin
@Composable
fun InstrumentsList() {
    var items by remember { mutableStateOf<List<Announcement>>(listOf()) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // 화면 진입 시 한 번만 실행
    LaunchedEffect(Unit) {
        try {
            // 백그라운드 스레드에서 네트워크 호출
            val result = withContext(Dispatchers.IO) {
                supabase.from("announcements")
                    .select()
                    .decodeList<Announcement>()
            }
            items = result
        } catch (e: Exception) {
            error = e.message ?: "알 수 없는 오류"
            android.util.Log.e("InstrumentsList", "Error loading data", e)
        } finally {
            isLoading = false
        }
    }
    
    // UI 상태별 분기
    when {
        isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        error != null -> Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("❌ 오류 발생", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Red)
            Spacer(Modifier.height(8.dp))
            Text(error ?: "", fontSize = 14.sp)
        }
        items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("데이터가 없습니다", fontSize = 16.sp, color = Color.Gray)
        }
        else -> LazyColumn(Modifier.fillMaxSize()) {
            items(items, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F8FB))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        item.message?.let { msg ->
                            Spacer(Modifier.height(4.dp))
                            Text(msg, fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
```

#### 2) NavHost에서 임시로 표시 (테스트용)

`MainActivity` 클래스의 `NavHost` 블록 수정:

```kotlin
NavHost(
    navController = navController,
    startDestination = "home",
    modifier = Modifier.weight(1f)
) {
    // 임시: Supabase 연동 테스트를 위해 홈에서 바로 표시
    composable("home") { InstrumentsList() }
    
    // ...other routes...
}
```

**원복 방법** (테스트 완료 후):
```kotlin
composable("home") { 
    Log.d("NavDebug", "Entered route: home")
    MainScreen(navController) 
}
```

### 주요 개념

**LaunchedEffect(Unit)**
- Composable이 처음 그려질 때 한 번만 실행
- 네트워크 호출 등 side-effect 처리에 사용
- `Unit` = 키가 변하지 않으므로 재실행 안 됨

**withContext(Dispatchers.IO)**
- 네트워크/DB 작업을 백그라운드 스레드에서 실행
- Main 스레드 블로킹 방지
- 결과는 자동으로 Main 스레드로 돌아옴 (Compose 상태 업데이트)

**상태 관리 패턴**
```
Loading → Success | Error
   ↓         ↓         ↓
 Spinner   Data    Error UI
```

### 예상 결과

**성공 시**:
- 앱 실행 → 로딩 스피너 → announcements 테이블 데이터가 카드 형태로 표시

**테이블이 비었을 때**:
- "데이터가 없습니다" 메시지 표시

**에러 발생 시**:
- 빨간 ❌와 함께 에러 메시지 표시
- Logcat에 상세 로그 출력

---

## 트러블슈팅

### 문제 1: Kotlin 버전 불일치 에러

**증상**:
```
Module was compiled with an incompatible version of Kotlin.
The binary version of its metadata is 2.2.0, expected version is 2.0.0.
```

**원인**: Supabase/Ktor 라이브러리가 Kotlin 2.2.x로 빌드됨

**해결**: `gradle/libs.versions.toml`에서 Kotlin 버전을 2.2.21로 업그레이드

---

### 문제 2: 테이블을 찾을 수 없음

**증상**:
```
PostgrestRestException: Could not find the table 'public.instruments' in the schema cache
Code: PGRST205
Hint: Perhaps you meant the table 'public.announcements'
```

**원인**: Supabase DB에 해당 테이블이 없음

**해결 방법**:

#### 옵션 A: 기존 테이블로 테스트 (즉시)
```kotlin
// announcements 테이블 사용
supabase.from("announcements").select().decodeList<Announcement>()
```

#### 옵션 B: 테이블 생성 (정식)
Supabase SQL Editor에서 실행:
```sql
create table if not exists public.instruments (
  id int primary key,
  name text not null
);

alter table public.instruments disable row level security;

insert into public.instruments (id, name) values
  (1, 'Guitar'),
  (2, 'Piano'),
  (3, 'Drums')
on conflict (id) do nothing;
```

---

### 문제 3: Row Level Security (RLS) 에러

**증상**: 테이블은 있는데 데이터가 안 보임

**원인**: RLS가 활성화되어 있고 정책이 없음

**임시 해결** (개발/테스트용):
```sql
alter table public.your_table disable row level security;
```

**운영 해결** (권장):
```sql
-- 모든 사용자에게 읽기 허용
create policy "Enable read access for all users" on public.your_table
  for select using (true);
```

---

### 문제 4: AdMob 쿠키 경고

**증상** (Logcat):
```
[ERROR:cookie_manager.cc(137)] Strict Secure Cookie policy does not allow 
setting a secure cookie for http://googleads.g.doubleclick.net/
```

**원인**: AdMob SDK가 HTTP에서 Secure 쿠키 설정 시도

**영향**: 없음 (광고 표시는 정상 작동)

**해결**: 무시 가능 (AdMob SDK 업데이트 시 자동 해결 예정)

---

## 체크리스트

연동 완료 전 최종 확인:

- [ ] Kotlin 버전이 2.2.21인가?
- [ ] `kotlin-serialization` 플러그인이 활성화되었나?
- [ ] `INTERNET` 권한이 AndroidManifest에 있나?
- [ ] Supabase URL/Key가 정확한가?
- [ ] 데이터 모델의 필드가 DB 컬럼과 일치하나?
- [ ] 테이블에 실제 데이터가 있나?
- [ ] RLS가 비활성화되었거나 정책이 설정되었나?
- [ ] 앱 실행 시 데이터가 표시되나?

---

## 참고 자료

- [Supabase Kotlin 공식 문서](https://supabase.com/docs/reference/kotlin/introduction)
- [kotlinx.serialization 가이드](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serialization-guide.md)
- [Ktor Client 문서](https://ktor.io/docs/client.html)

---

## 다음 단계

1. **ViewModel 분리**: UI에서 비즈니스 로직 분리
2. **에러 핸들링 개선**: Retry 로직, 네트워크 상태 확인
3. **캐싱**: Room DB와 연동해 오프라인 지원
4. **실시간 구독**: Supabase Realtime 모듈 활용
5. **보안 강화**: 키를 BuildConfig나 Remote Config로 이동

---

> 문서 작성: 2025-01-05  
> 최종 업데이트: 2025-01-05  
> 작성자: GitHub Copilot

