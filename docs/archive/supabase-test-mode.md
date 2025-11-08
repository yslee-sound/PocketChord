# 공지사항 테스트 모드 설정

## ✅ 테스트 모드 활성화 완료!

홈 화면의 공지사항이 이제 **항상 표시**됩니다. 닫아도 다시 홈 화면으로 돌아오면 계속 볼 수 있습니다.

---

## 🔧 변경 사항

### 1. 중복 방지 기능 비활성화
```kotlin
// 이전: 이미 본 공지사항 확인
val lastShownId = prefs.getLong("last_announcement_id", -1)
if (ann.id != lastShownId) {
    // 표시
}

// 현재: 항상 표시 ✅
announcement = ann
showAnnouncementDialog = true
```

### 2. SharedPreferences 저장 비활성화
```kotlin
// 이전: 본 것으로 표시
prefs.edit {
    putLong("last_announcement_id", id)
}

// 현재: 저장하지 않음 ✅
// 주석 처리됨
```

---

## 🧪 테스트 방법

### 1. 앱 시작
- 홈 화면 로드 시 공지사항 자동 표시

### 2. 공지사항 닫기
- "확인" 버튼 또는 X 버튼으로 닫기

### 3. 다시 보기
- 홈 화면에서 다른 화면으로 이동 (예: 설정)
- 다시 홈 화면으로 돌아오기
- 공지사항이 다시 표시됨 ✅

### 4. 로그 확인
```
D/HomeScreen: ✅ [TEST MODE] Showing announcement: ...
D/HomeScreen: ⚠️ [TEST MODE] Dialog dismissed without saving ID
```

---

## ⚠️ 프로덕션 배포 전 필수 작업

테스트 완료 후 **반드시** 중복 방지 기능을 다시 활성화해야 합니다!

### 복원 방법

`HomeScreen.kt`에서 두 곳을 수정:

#### 1. LaunchedEffect 섹션
```kotlin
// ⚠️ 테스트 모드 주석 제거하고 아래 코드로 복원
LaunchedEffect(Unit) {
    val prefs = context.getSharedPreferences("announcement_prefs", MODE_PRIVATE)
    
    try {
        val repository = AnnouncementRepository(supabase, "com.sweetapps.pocketchord")
        
        repository.getLatestAnnouncement()
            .onSuccess { result ->
                result?.let { ann ->
                    // 중복 방지 기능 복원
                    val lastShownId = prefs.getLong("last_announcement_id", -1)
                    
                    if (ann.id != lastShownId) {
                        announcement = ann
                        showAnnouncementDialog = true
                    }
                }
            }
    } catch (e: Exception) {
        Log.e("HomeScreen", "Exception", e)
    }
}
```

#### 2. onDismiss 섹션
```kotlin
onDismiss = {
    // SharedPreferences 저장 복원
    announcement?.id?.let { id ->
        val prefs = context.getSharedPreferences("announcement_prefs", MODE_PRIVATE)
        prefs.edit {
            putLong("last_announcement_id", id)
        }
    }
    showAnnouncementDialog = false
}
```

---

## 🔄 빠른 전환 방법

### 테스트 모드 → 프로덕션 모드

1. `HomeScreen.kt` 파일 열기
2. `// ⚠️ 테스트 모드` 주석 찾기 (2곳)
3. 주석 처리된 코드의 주석 해제
4. 테스트 코드 제거 또는 주석 처리

### 또는 BuildConfig 사용 (권장)

더 나은 방법은 BuildConfig로 자동 전환:

```kotlin
LaunchedEffect(Unit) {
    val prefs = context.getSharedPreferences("announcement_prefs", MODE_PRIVATE)
    
    repository.getLatestAnnouncement()
        .onSuccess { result ->
            result?.let { ann ->
                // 디버그 빌드에서는 항상 표시, 릴리즈 빌드에서는 중복 방지
                val shouldShow = if (BuildConfig.DEBUG) {
                    true  // 테스트 모드: 항상 표시
                } else {
                    val lastShownId = prefs.getLong("last_announcement_id", -1)
                    ann.id != lastShownId  // 프로덕션: 중복 방지
                }
                
                if (shouldShow) {
                    announcement = ann
                    showAnnouncementDialog = true
                }
            }
        }
}

onDismiss = {
    // 릴리즈 빌드에서만 저장
    if (!BuildConfig.DEBUG) {
        announcement?.id?.let { id ->
            prefs.edit {
                putLong("last_announcement_id", id)
            }
        }
    }
    showAnnouncementDialog = false
}
```

이렇게 하면:
- **Debug 빌드**: 항상 표시 (테스트용)
- **Release 빌드**: 한 번만 표시 (프로덕션)

---

## 📋 체크리스트

### 테스트 중
- [x] 중복 방지 기능 비활성화
- [x] SharedPreferences 저장 비활성화
- [x] 로그에 TEST MODE 표시
- [ ] 공지사항 디자인 확인
- [ ] 다양한 텍스트 길이 테스트
- [ ] 이미지 표시 테스트

### 프로덕션 배포 전
- [ ] 중복 방지 기능 다시 활성화
- [ ] SharedPreferences 저장 다시 활성화
- [ ] TEST MODE 로그 제거
- [ ] BuildConfig.DEBUG 조건 추가 (선택)
- [ ] 릴리즈 빌드로 최종 테스트

---

## 💡 팁

### SharedPreferences 초기화
수동으로 공지사항을 다시 보고 싶을 때:

```kotlin
// 설정 화면 등에 디버그 버튼 추가
Button(onClick = {
    val prefs = context.getSharedPreferences("announcement_prefs", MODE_PRIVATE)
    prefs.edit {
        remove("last_announcement_id")
    }
    Toast.makeText(context, "공지사항 초기화됨", Toast.LENGTH_SHORT).show()
}) {
    Text("공지사항 초기화 (디버그)")
}
```

### ADB로 SharedPreferences 확인
```bash
# SharedPreferences 내용 확인
adb shell run-as com.sweetapps.pocketchord cat shared_prefs/announcement_prefs.xml

# SharedPreferences 삭제
adb shell run-as com.sweetapps.pocketchord rm shared_prefs/announcement_prefs.xml
```

---

## ✅ 현재 상태

- **모드**: 테스트 모드 ✅
- **중복 방지**: 비활성화 ✅
- **동작**: 홈 화면 방문할 때마다 공지사항 표시

**프로덕션 배포 전 반드시 복원하세요!** ⚠️

---

이제 마음껏 테스트할 수 있습니다! 🎉

