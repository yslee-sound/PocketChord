# 업데이트 다이얼로그 통합 가이드

## 개요

`OptionalUpdateDialog`는 Supabase의 `update_info` 테이블에서 `is_force` 필드 값에 따라 강제 업데이트와 선택적 업데이트를 모두 처리할 수 있는 통합 다이얼로그입니다.

## 주요 기능

- **강제 업데이트 모드 (`isForce = true`)**
  - 닫기 버튼 숨김
  - "나중에" 버튼 숨김
  - 뒤로 가기/외부 클릭으로 닫기 불가
  - 사용자가 반드시 업데이트를 수행해야 함

- **선택적 업데이트 모드 (`isForce = false`)**
  - X 닫기 버튼 표시
  - "나중에" 버튼 표시
  - 뒤로 가기/외부 클릭으로 닫기 가능
  - 사용자가 업데이트를 나중으로 미룰 수 있음

## Supabase 테이블 구조

```sql
CREATE TABLE update_info (
    id BIGINT PRIMARY KEY,
    version_code INTEGER NOT NULL UNIQUE,
    version_name TEXT NOT NULL,
    app_id TEXT NOT NULL,
    is_force BOOLEAN DEFAULT false,  -- 이 필드가 다이얼로그 동작을 결정
    release_notes TEXT NOT NULL,
    released_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    download_url TEXT
);
```

## 사용 예시

### 1. Repository에서 업데이트 정보 가져오기

```kotlin
// UpdateInfoRepository.kt
class UpdateInfoRepository(private val client: SupabaseClient) {
    suspend fun checkUpdateRequired(currentVersionCode: Int): Result<UpdateInfo?> = runCatching {
        val list = client.from("update_info")
            .select()
            .decodeList<UpdateInfo>()

        val latest = list.maxByOrNull { it.versionCode }
        if (latest != null && latest.versionCode > currentVersionCode) latest else null
    }
}
```

### 2. ViewModel에서 업데이트 확인

```kotlin
class MainViewModel(
    private val updateInfoRepository: UpdateInfoRepository
) : ViewModel() {
    
    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()
    
    fun checkForUpdate(currentVersionCode: Int) {
        viewModelScope.launch {
            updateInfoRepository.checkUpdateRequired(currentVersionCode)
                .onSuccess { update ->
                    _updateInfo.value = update
                }
                .onFailure { error ->
                    Log.e("MainViewModel", "Failed to check update", error)
                }
        }
    }
}
```

### 3. Composable에서 다이얼로그 표시

```kotlin
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val updateInfo by viewModel.updateInfo.collectAsState()
    
    // 앱 시작 시 업데이트 확인
    LaunchedEffect(Unit) {
        viewModel.checkForUpdate(BuildConfig.VERSION_CODE)
    }
    
    // 업데이트 정보가 있으면 다이얼로그 표시
    updateInfo?.let { update ->
        OptionalUpdateDialog(
            isForce = update.isForce,  // Supabase의 is_force 값 사용
            title = "앱 업데이트",
            updateButtonText = "지금 업데이트",
            version = update.versionName,
            features = null,
            onUpdateClick = {
                // Play Store로 이동
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("market://details?id=${context.packageName}")
                    setPackage("com.android.vending")
                }
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    // Play Store 앱이 없으면 웹 브라우저로 열기
                    val webIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                    }
                    context.startActivity(webIntent)
                }
            },
            onLaterClick = if (update.isForce) null else {
                {
                    // 선택적 업데이트인 경우에만 닫기 가능
                    viewModel.dismissUpdate()
                }
            }
        )
    }
}
```

### 4. 간단한 사용 예시 (Navigation)

```kotlin
// MainActivity.kt
composable("update_dialog") {
    val context = LocalContext.current
    
    // 예시: Supabase에서 가져온 UpdateInfo
    val updateInfo = remember { 
        UpdateInfo(
            versionCode = 11,
            versionName = "1.1.0",
            appId = "com.sweetapps.pocketchord",
            isForce = true,  // 강제 업데이트
            releaseNotes = "성능 향상\n버그 수정\n새로운 기능 추가"
        )
    }
    
    OptionalUpdateDialog(
        isForce = updateInfo.isForce,
        title = "앱 업데이트",
        updateButtonText = "지금 업데이트",
        version = updateInfo.versionName,
        onUpdateClick = {
            // Play Store로 이동
            val intent = Intent(Intent.ACTION_VIEW, 
                Uri.parse("market://details?id=${context.packageName}"))
            context.startActivity(intent)
        },
        onLaterClick = if (updateInfo.isForce) null else {
            { navController.popBackStack() }
        }
    )
}
```

## 파라미터 설명

| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| `isForce` | Boolean | false | 강제 업데이트 여부 (Supabase의 `is_force` 값) |
| `title` | String | 동적 | 다이얼로그 제목 |
| `description` | String | 동적 | 업데이트 설명 |
| `updateButtonText` | String | 동적 | 업데이트 버튼 텍스트 |
| `laterButtonText` | String | "나중에" | 나중에 버튼 텍스트 (선택적 모드에서만 표시) |
| `features` | List<String>? | null | 주요 기능 목록 (옵션) |
| `version` | String? | null | 버전 번호 (옵션) |
| `estimatedTime` | String? | null | 예상 소요 시간 (옵션) |
| `onUpdateClick` | () -> Unit | - | 업데이트 버튼 클릭 시 동작 |
| `onLaterClick` | (() -> Unit)? | null | 나중에 버튼 클릭 시 동작 (선택적 모드) |

## 동작 방식

1. **Supabase에서 업데이트 정보 조회**
   - `UpdateInfoRepository.checkUpdateRequired()`를 호출하여 현재 버전보다 높은 버전이 있는지 확인

2. **업데이트 정보 분석**
   - `is_force` 필드가 `true`면 강제 업데이트
   - `is_force` 필드가 `false`면 선택적 업데이트

3. **다이얼로그 표시**
   - `OptionalUpdateDialog`를 호출하고 `isForce` 파라미터에 `is_force` 값 전달
   - 다이얼로그가 자동으로 적절한 UI와 동작을 선택

4. **사용자 액션 처리**
   - **업데이트 클릭**: Play Store로 이동
   - **나중에 클릭** (선택적 모드만): 다이얼로그 닫기
   - **강제 모드**: 닫기 불가, 반드시 업데이트 수행

## 마이그레이션 가이드

### 기존 ForceUpdateDialog 사용 코드

```kotlin
// Before
ForceUpdateDialog(
    title = "앱 업데이트",
    description = "새로운 업데이트가 있습니다.",
    buttonText = "업데이트",
    showCloseButton = false,
    onUpdateClick = { /* ... */ },
    onDismiss = { /* ... */ }
)
```

### 통합 OptionalUpdateDialog로 변경

```kotlin
// After
OptionalUpdateDialog(
    isForce = true,  // showCloseButton의 반대 값
    title = "앱 업데이트",
    description = "새로운 업데이트가 있습니다.",
    updateButtonText = "업데이트",
    onUpdateClick = { /* ... */ },
    onLaterClick = null  // 강제 모드에서는 null
)
```

## 테스트 시나리오

### 강제 업데이트 테스트
1. Supabase에서 `is_force = true`로 레코드 생성
2. 앱 실행
3. 다이얼로그가 표시되는지 확인
4. 닫기 버튼과 "나중에" 버튼이 없는지 확인
5. 뒤로 가기/외부 클릭이 동작하지 않는지 확인

### 선택적 업데이트 테스트
1. Supabase에서 `is_force = false`로 레코드 생성
2. 앱 실행
3. 다이얼로그가 표시되는지 확인
4. X 닫기 버튼과 "나중에" 버튼이 표시되는지 확인
5. 뒤로 가기/외부 클릭으로 닫을 수 있는지 확인

## 주의사항

- `isForce = true`일 때는 반드시 `onLaterClick = null`로 설정
- 강제 업데이트 모드에서는 사용자가 다이얼로그를 닫을 수 없으므로 신중하게 사용
- Play Store 링크는 반드시 테스트하여 정상 동작 확인
- 네트워크 오류 처리를 위한 fallback 로직 구현 권장
