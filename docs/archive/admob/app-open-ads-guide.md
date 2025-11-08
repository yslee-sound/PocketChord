# 앱 오프닝 광고 구현 가이드

## 개요
PocketChord 앱에 **앱 오프닝 광고(App Open Ad)**를 추가했습니다. 이는 사용자가 앱을 실행하거나 백그라운드에서 돌아올 때 표시되는 전면광고입니다.

---

## 구현 내용

### 1. 새로 추가된 파일

#### `AppOpenAdManager.kt`
앱 오프닝 광고를 자동으로 관리하는 클래스입니다:

**주요 기능:**
- ✅ 앱 시작 시 광고 미리 로드
- ✅ 백그라운드에서 돌아올 때 자동 표시
- ✅ 첫 실행 시에는 광고 표시 안 함 (사용자 경험 고려)
- ✅ 광고 만료 시간: 4시간
- ✅ Activity 생명주기 자동 추적

#### `PocketChordApplication.kt`
Application 클래스로 앱 전역 초기화를 담당합니다:

**기능:**
- ✅ AdMob SDK 초기화
- ✅ 앱 오프닝 광고 매니저 초기화
- ✅ 자동으로 광고 생명주기 관리

### 2. 수정된 파일

#### `AndroidManifest.xml`
```xml
<application
    android:name=".PocketChordApplication"  <!-- 추가됨 -->
    ...>
```

#### `MainActivity.kt`
- MobileAds.initialize 제거 (Application에서 처리)

#### `build.gradle.kts`
- lifecycle-process 의존성 추가

---

## 작동 방식

### 앱 오프닝 광고 표시 시나리오

#### ✅ 표시되는 경우

1. **백그라운드에서 복귀**
   ```
   사용자가 앱을 사용 중
   → 홈 버튼 눌러 백그라운드로 이동
   → 다시 앱으로 돌아옴
   → 💡 광고 표시!
   ```

2. **앱 재실행**
   ```
   앱 완전 종료
   → 시간이 지남 (4시간 이내)
   → 앱 다시 실행
   → 💡 광고 표시!
   ```

#### ❌ 표시되지 않는 경우

1. **첫 실행**
   ```
   앱 설치 후 첫 실행
   → ❌ 광고 표시 안 함
   → 스플래시 화면 → 메인 화면
   ```

2. **광고 만료**
   ```
   마지막 광고 로드 후 4시간 경과
   → ❌ 광고 표시 안 함
   → 새 광고 로드 시도
   ```

3. **스플래시 화면**
   ```
   앱 시작 시 스플래시 화면
   → ❌ 광고 표시 안 함
   ```

---

## 설정값

### `AppOpenAdManager.kt`

```kotlin
companion object {
    // 테스트 광고 ID
    private const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"
    
    // 광고 만료 시간: 4시간
    private const val AD_TIMEOUT_MS = 4 * 60 * 60 * 1000L
}
```

### 광고 만료 시간 조정

광고는 로드 후 일정 시간이 지나면 만료됩니다. 필요 시 조정 가능:

```kotlin
// 더 짧게 (2시간)
private const val AD_TIMEOUT_MS = 2 * 60 * 60 * 1000L

// 더 길게 (6시간)
private const val AD_TIMEOUT_MS = 6 * 60 * 60 * 1000L
```

---

## 실제 배포 시 변경 사항

### AppOpenAdManager.kt
```kotlin
// 현재 (테스트)
private const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"

// 변경 → (실제 앱 오프닝 광고 ID)
private const val AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/1234567890"
```

---

## 생명주기 관리

### Application 레벨에서 자동 관리

```kotlin
class PocketChordApplication : Application() {
    private lateinit var appOpenAdManager: AppOpenAdManager
    
    override fun onCreate() {
        super.onCreate()
        // 자동으로 모든 Activity 생명주기 추적
        appOpenAdManager = AppOpenAdManager(this)
    }
}
```

### 자동 감지되는 이벤트

1. **Activity 시작/종료** - 자동 추적
2. **포그라운드/백그라운드 전환** - 자동 감지
3. **광고 로드/표시** - 자동 관리

---

## 디버깅

### 로그 확인

```bash
adb logcat | findstr "AppOpenAdManager"
```

**주요 로그 메시지:**

```
앱 오프닝 광고 로드 성공
앱이 포그라운드로 왔습니다 (백그라운드에서 복귀)
첫 실행이므로 광고를 표시하지 않습니다
광고가 표시되었습니다
광고가 닫혔습니다
```

---

## 테스트 시나리오

### 1. 첫 실행 테스트
```
1. 앱 삭제 (완전 초기화)
2. 앱 재설치
3. 앱 실행
4. ✅ 확인: 광고가 표시되지 않음
5. ✅ 확인: 스플래시 → 메인 화면 진행
```

### 2. 백그라운드 복귀 테스트
```
1. 앱 실행 (메인 화면 표시)
2. 홈 버튼으로 백그라운드 이동
3. 5초 대기
4. 앱 아이콘 터치하여 복귀
5. ✅ 확인: 광고 표시됨
```

### 3. 앱 재실행 테스트
```
1. 앱 실행
2. 뒤로가기로 앱 종료
3. 즉시 앱 재실행
4. ✅ 확인: 광고 표시됨 (광고가 만료되지 않았으면)
```

---

## 광고 우선순위

PocketChord 앱의 광고 우선순위:

1. **앱 오프닝 광고** (최우선) - 백그라운드 복귀 시
2. **전면광고** - 화면 전환 시 (60초 간격, 3회 전환)
3. **배너 광고** - 상단에 항상 표시

---

## 사용자 경험 최적화

### ✅ 좋은 점

1. **첫 인상 보호**
   - 첫 실행 시 광고 없음
   - 사용자가 앱을 먼저 경험

2. **자연스러운 타이밍**
   - 백그라운드에서 복귀할 때만 표시
   - 사용 중 방해하지 않음

3. **자동 관리**
   - 개발자가 코드에서 직접 호출할 필요 없음
   - Application 레벨에서 자동 처리

### ⚠️ 주의사항

1. **과도한 광고 방지**
   - 4시간 만료 시간으로 너무 자주 표시되지 않음
   - 첫 실행 제외

2. **로딩 시간**
   - 광고 로드에 시간이 걸릴 수 있음
   - 앱 시작 시 미리 로드하여 대비

---

## 문제 해결

### Q: 광고가 표시되지 않아요

**체크리스트:**
- [ ] 인터넷 연결 확인
- [ ] 첫 실행이 아닌지 확인
- [ ] 백그라운드에서 복귀했는지 확인
- [ ] 로그에서 "광고 로드 성공" 확인
- [ ] 4시간 이내에 광고가 로드되었는지 확인

### Q: 첫 실행에서 광고가 나와요

→ 의도된 동작이 아닙니다. `isFirstLaunch` 플래그가 제대로 작동하는지 확인하세요.

### Q: 너무 자주 광고가 나와요

→ `AD_TIMEOUT_MS` 값을 늘리세요 (예: 6시간, 8시간).

### Q: 백그라운드에서 복귀해도 광고가 안 나와요

**확인 사항:**
1. 광고가 로드되었는지 확인 (로그)
2. 4시간 이내에 광고가 로드되었는지 확인
3. ProcessLifecycleOwner가 제대로 작동하는지 확인

---

## 추가 커스터마이징

### 특정 화면에서 광고 제외

```kotlin
override fun onActivityStarted(activity: Activity) {
    // 특정 화면 제외
    if (activity.javaClass.simpleName in listOf("Splash", "Tutorial", "Payment")) {
        return
    }
    currentActivity = activity
}
```

### 광고 표시 빈도 제한

```kotlin
private var lastAdShowTime = 0L

fun showAdIfAvailable(activity: Activity, onAdDismissed: () -> Unit = {}) {
    // 최소 10분 간격
    if (System.currentTimeMillis() - lastAdShowTime < 10 * 60 * 1000) {
        onAdDismissed()
        return
    }
    
    // ... 기존 코드
    lastAdShowTime = System.currentTimeMillis()
}
```

---

## 성과 예상

### DAU 1,000명 기준

**앱 오프닝 광고:**
- 백그라운드 복귀율: 30%
- 일일 노출: 약 300회
- 월 노출: 약 9,000회

**전체 광고 (배너 + 전면 + 오프닝):**
- 일일 총 노출: 약 1,600회
- 월 총 노출: 약 48,000회

---

## 참고 자료

- [AdMob App Open Ads 공식 문서](https://developers.google.com/admob/android/app-open)
- [ProcessLifecycleOwner 문서](https://developer.android.com/reference/androidx/lifecycle/ProcessLifecycleOwner)
- [Application.ActivityLifecycleCallbacks](https://developer.android.com/reference/android/app/Application.ActivityLifecycleCallbacks)

---

## 요약

✅ **AppOpenAdManager**: 자동 광고 관리  
✅ **PocketChordApplication**: 전역 초기화  
✅ **첫 실행 제외**: 사용자 경험 고려  
✅ **백그라운드 복귀**: 자동 감지 및 표시  
✅ **4시간 만료**: 적절한 빈도 유지  

**앱 오프닝 광고가 성공적으로 통합되었습니다!** 🎉

