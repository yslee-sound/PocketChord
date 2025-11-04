# Supabase 공지사항 다이얼로그 사용 가이드

## 📋 개요

Flutter의 `AnnouncementDialog`를 Kotlin/Android로 변환하여 `NoticeDialog.kt`에 통합했습니다.

---

## 🎯 Flutter vs Kotlin 비교

### Flutter (Dart)
```dart
class AnnouncementDialog extends StatelessWidget {
  final Announcement announcement;
  
  static Future<void> show(
    BuildContext context,
    Announcement announcement,
  ) async {
    await showDialog(
      context: context,
      builder: (context) => AnnouncementDialog(announcement: announcement),
    );
  }
  
  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(announcement.title),
      content: Text(announcement.content),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: Text('확인'),
        ),
      ],
    );
  }
}

// 사용
AnnouncementDialog.show(context, announcement);
```

### Kotlin/Android ✅
```kotlin
@Composable
fun AnnouncementDialog(
    announcement: Announcement,
    onDismiss: () -> Unit,
    buttonText: String = "확인",
    showImage: Boolean = true
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(...)
    ) {
        // UI 구성
        Text(text = announcement.title)
        Text(text = announcement.content)
        Button(onClick = onDismiss) {
            Text(buttonText)
        }
    }
}

// 사용
AnnouncementDialog(
    announcement = announcement,
    onDismiss = { }
)
```

---

## 🚀 사용 방법

### 1. 기본 사용 (이미지 포함)

```kotlin
import com.sweetapps.pocketchord.ui.dialogs.AnnouncementDialog
import com.sweetapps.pocketchord.data.supabase.model.Announcement

@Composable
fun MyScreen() {
    var showDialog by remember { mutableStateOf(false) }
    var announcement by remember { mutableStateOf<Announcement?>(null) }
    
    // Supabase에서 공지사항 가져오기
    LaunchedEffect(Unit) {
        val repository = AnnouncementRepository(supabase)
        repository.getLatestAnnouncement()
            .onSuccess { 
                announcement = it
                if (it != null) {
                    showDialog = true
                }
            }
    }
    
    // 다이얼로그 표시
    if (showDialog && announcement != null) {
        AnnouncementDialog(
            announcement = announcement!!,
            onDismiss = { showDialog = false }
        )
    }
}
```

### 2. 간단한 버전 (이미지 없음)

```kotlin
if (showDialog && announcement != null) {
    SimpleAnnouncementDialog(
        announcement = announcement!!,
        onDismiss = { showDialog = false },
        buttonText = "닫기"
    )
}
```

### 3. MainActivity에서 사용

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            PocketChordTheme {
                var showAnnouncement by remember { mutableStateOf(false) }
                var announcement by remember { mutableStateOf<Announcement?>(null) }
                
                // 앱 시작 시 공지사항 확인
                LaunchedEffect(Unit) {
                    val repository = AnnouncementRepository(supabase)
                    repository.getLatestAnnouncement()
                        .onSuccess { result ->
                            result?.let {
                                announcement = it
                                showAnnouncement = true
                            }
                        }
                        .onFailure { error ->
                            Log.e("Announcement", "Failed to load", error)
                        }
                }
                
                // 메인 화면
                MainScreen()
                
                // 공지사항 다이얼로그
                if (showAnnouncement && announcement != null) {
                    AnnouncementDialog(
                        announcement = announcement!!,
                        onDismiss = { showAnnouncement = false }
                    )
                }
            }
        }
    }
}
```

### 4. 앱 시작 시 한 번만 표시

```kotlin
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val prefs = remember { 
        context.getSharedPreferences("announcement_prefs", Context.MODE_PRIVATE) 
    }
    
    var showAnnouncement by remember { mutableStateOf(false) }
    var announcement by remember { mutableStateOf<Announcement?>(null) }
    
    LaunchedEffect(Unit) {
        val repository = AnnouncementRepository(supabase)
        repository.getLatestAnnouncement()
            .onSuccess { result ->
                result?.let { ann ->
                    // 이미 본 공지사항인지 확인
                    val lastShownId = prefs.getLong("last_announcement_id", -1)
                    if (ann.id != lastShownId) {
                        announcement = ann
                        showAnnouncement = true
                    }
                }
            }
    }
    
    if (showAnnouncement && announcement != null) {
        AnnouncementDialog(
            announcement = announcement!!,
            onDismiss = {
                // 공지사항 본 것으로 표시
                announcement?.id?.let { id ->
                    prefs.edit().putLong("last_announcement_id", id).apply()
                }
                showAnnouncement = false
            }
        )
    }
}
```

---

## 📦 제공되는 Composable

### 1. AnnouncementDialog (기본)
```kotlin
@Composable
fun AnnouncementDialog(
    announcement: Announcement,      // Supabase 모델
    onDismiss: () -> Unit,          // 닫기 콜백
    buttonText: String = "확인",     // 버튼 텍스트
    showImage: Boolean = true       // 이미지 표시 여부
)
```

**특징:**
- Supabase `Announcement` 모델 직접 사용
- 이미지 포함 (16:9 비율)
- 우측 상단 X 버튼
- 스크롤 가능한 내용

### 2. SimpleAnnouncementDialog (간단)
```kotlin
@Composable
fun SimpleAnnouncementDialog(
    announcement: Announcement,
    onDismiss: () -> Unit,
    buttonText: String = "확인"
)
```

**특징:**
- 이미지 없는 텍스트 전용
- 더 간결한 UI

### 3. NoticeDialog (범용)
```kotlin
@Composable
fun NoticeDialog(
    title: String,
    description: String,
    imageUrl: String? = null,
    buttonText: String? = null,
    onDismiss: () -> Unit,
    onButtonClick: (() -> Unit)? = null,
    // ... 색상 커스터마이징 옵션
)
```

**특징:**
- 수동으로 title, description 전달
- Supabase 외 용도로도 사용 가능
- 색상 커스터마이징 지원

---

## 🎨 커스터마이징

### 버튼 텍스트 변경
```kotlin
AnnouncementDialog(
    announcement = announcement,
    onDismiss = { },
    buttonText = "닫기"
)
```

### 이미지 제거
```kotlin
AnnouncementDialog(
    announcement = announcement,
    onDismiss = { },
    showImage = false
)
```

### 다크 모드 대응
```kotlin
// 기존 NoticeDialogDark 사용
NoticeDialogDark(
    title = announcement.title,
    description = announcement.content,
    onDismiss = { }
)
```

---

## 📋 Supabase 데이터 구조

### Announcement 모델
```kotlin
@Serializable
data class Announcement(
    val id: Long?,
    val createdAt: String?,
    val appId: String = "com.sweetapps.pocketchord",
    val title: String,        // 다이얼로그 제목
    val content: String,      // 다이얼로그 내용
    val isActive: Boolean = true
)
```

### Supabase 테이블
```sql
CREATE TABLE announcements (
    id BIGINT PRIMARY KEY,
    created_at TIMESTAMP,
    app_id TEXT,
    title TEXT,           -- 제목
    content TEXT,         -- 내용
    is_active BOOLEAN
);
```

### 테스트 데이터 추가
```sql
INSERT INTO announcements (app_id, title, content, is_active)
VALUES (
    'com.sweetapps.pocketchord',
    '새로운 기능 출시',
    '더욱 편리해진 새로운 기능을 만나보세요. 이번 업데이트에서는 사용자 경험을 개선하고 다양한 새로운 기능을 추가했습니다.',
    true
);
```

---

## 🔧 트러블슈팅

### 다이얼로그가 표시되지 않음

**원인 1: 데이터 없음**
```kotlin
// 로그로 확인
repository.getLatestAnnouncement()
    .onSuccess { 
        Log.d("Announcement", "Result: $it")
        if (it == null) {
            Log.w("Announcement", "No announcement found")
        }
    }
```

**원인 2: is_active = false**
- Supabase에서 `is_active`가 `true`인지 확인

**원인 3: app_id 불일치**
- `app_id`가 `"com.sweetapps.pocketchord"`인지 확인

### 이미지가 표시되지 않음

현재 `R.drawable.notice_sample` 리소스를 사용합니다.
- `res/drawable/` 폴더에 `notice_sample.png` 추가 필요

---

## ✅ 체크리스트

### 구현 전
- [ ] Supabase에 공지사항 데이터 추가
- [ ] app_id 확인: `"com.sweetapps.pocketchord"`
- [ ] is_active = true 확인

### 구현
- [ ] `AnnouncementDialog` import
- [ ] `AnnouncementRepository` 생성
- [ ] LaunchedEffect로 데이터 로드
- [ ] State로 다이얼로그 표시 제어

### 테스트
- [ ] 앱 실행 시 다이얼로그 표시 확인
- [ ] "확인" 버튼으로 닫기 확인
- [ ] X 버튼으로 닫기 확인
- [ ] 백 버튼으로 닫기 확인

---

## 🎉 완료!

Flutter의 `AnnouncementDialog`를 Kotlin으로 완벽하게 구현했습니다!

**주요 차이점:**
- Flutter: `showDialog` 정적 메서드
- Kotlin: Composable 함수 + State 관리

**장점:**
- ✅ Supabase `Announcement` 모델 직접 사용
- ✅ 기존 UI 재사용
- ✅ 커스터마이징 가능
- ✅ 다크 모드 지원

이제 Supabase에서 공지사항을 관리하고 앱에 자동으로 표시할 수 있습니다! 🚀

