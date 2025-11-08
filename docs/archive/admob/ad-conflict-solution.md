# 앱 오프닝 광고와 배너 광고 충돌 해결

## 문제 상황

앱 오프닝 광고가 전면으로 표시될 때 배너 광고도 함께 보여서 두 광고가 겹치는 문제가 발생했습니다.

### 문제 원인
- 앱 오프닝 광고: 전면 광고로 전체 화면을 덮음
- 배너 광고: MainActivity의 상단에 계속 표시
- → 두 광고가 동시에 표시되어 겹침

---

## ✅ 해결 방법

**앱 오프닝 광고가 표시되는 동안 배너 광고를 숨김**

### 구현 원리

```
앱 오프닝 광고 표시 시작
    ↓
PocketChordApplication.isShowingAppOpenAd = true
    ↓
MainActivity가 StateFlow 관찰
    ↓
배너 광고 숨김
    ↓
앱 오프닝 광고 닫힘
    ↓
PocketChordApplication.isShowingAppOpenAd = false
    ↓
배너 광고 다시 표시
```

---

## 🔧 구현 세부사항

### 1. PocketChordApplication

**StateFlow로 광고 표시 상태 공유**

```kotlin
class PocketChordApplication : Application() {
    // 앱 오프닝 광고 표시 상태
    private val _isShowingAppOpenAd = MutableStateFlow(false)
    val isShowingAppOpenAd: StateFlow<Boolean> = _isShowingAppOpenAd.asStateFlow()
    
    fun setAppOpenAdShowing(isShowing: Boolean) {
        _isShowingAppOpenAd.value = isShowing
    }
}
```

**주요 포인트:**
- `MutableStateFlow`: 내부에서만 수정 가능
- `StateFlow`: 외부에서는 읽기 전용으로 노출
- Compose에서 `collectAsState()`로 관찰 가능

---

### 2. AppOpenAdManager

**광고 표시/닫힘 시 Application 상태 업데이트**

```kotlin
private fun showAdNow(activity: Activity, onAdDismissed: () -> Unit) {
    appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
        override fun onAdShowedFullScreenContent() {
            // 광고가 표시될 때
            (application as? PocketChordApplication)?.setAppOpenAdShowing(true)
        }

        override fun onAdDismissedFullScreenContent() {
            // 광고가 닫힐 때
            (application as? PocketChordApplication)?.setAppOpenAdShowing(false)
        }

        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
            // 광고 표시 실패 시에도
            (application as? PocketChordApplication)?.setAppOpenAdShowing(false)
        }
    }
}
```

**타이밍:**
- `onAdShowedFullScreenContent`: 광고가 화면에 나타날 때
- `onAdDismissedFullScreenContent`: 사용자가 광고를 닫을 때
- `onAdFailedToShowFullScreenContent`: 광고 표시 실패 시

---

### 3. MainActivity

**StateFlow 관찰 및 배너 숨김 처리**

```kotlin
setContent {
    PocketChordTheme {
        // ...existing code...
        
        // 앱 오프닝 광고 표시 상태 관찰
        val app = context.applicationContext as PocketChordApplication
        val isShowingAppOpenAd by app.isShowingAppOpenAd.collectAsState()
        
        // ...existing code...
        
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            // 배너 광고: 앱 오프닝 광고 표시 중이 아닐 때만
            if (isBannerEnabled && !isSplash && !isShowingAppOpenAd) {
                TopBannerAd()
            }
            // ...existing code...
        }
    }
}
```

**조건:**
```kotlin
isBannerEnabled        // 배너 광고 활성화됨
&& !isSplash           // 스플래시 화면 아님
&& !isShowingAppOpenAd // 앱 오프닝 광고 표시 중 아님
```

---

## 🎯 동작 흐름

### 사용자 시나리오

```
1. 앱 사용 중 (배너 광고 표시 O)
   ↓
2. 홈 버튼으로 백그라운드
   ↓
3. 앱 아이콘으로 돌아오기
   ↓
4. 앱 오프닝 광고 표시 시작
   → isShowingAppOpenAd = true
   → 배너 광고 즉시 숨김 ✅
   ↓
5. 사용자가 광고 보는 중
   (배너 없이 깔끔한 전면 광고)
   ↓
6. 사용자가 광고 닫기
   → isShowingAppOpenAd = false
   → 배너 광고 다시 표시 ✅
   ↓
7. 앱 계속 사용 (배너 광고 표시 O)
```

---

## 🧪 테스트 방법

### 시나리오 1: 기본 동작 확인

```
1. 앱 실행
2. 배너 광고가 상단에 표시되는지 확인 ✅
3. 홈 버튼으로 백그라운드
4. 앱으로 돌아오기
5. 앱 오프닝 광고가 전면으로 표시 ✅
6. 배너 광고가 숨겨졌는지 확인 ✅
7. 광고 닫기
8. 배너 광고가 다시 나타나는지 확인 ✅
```

### 시나리오 2: 테스트 모드로 빠른 확인

```
1. 설정 → 개발 도구
2. "앱 오프닝 광고 테스트" ON
3. 백그라운드 복귀 반복
4. 매번 배너가 숨겨지는지 확인
```

### 로그 확인

```bash
adb logcat | findstr "AppOpenAdManager"
```

**정상 동작 로그:**
```
앱 오프닝 광고를 표시합니다
광고가 표시되었습니다           ← 이 시점에 배너 숨김
(사용자가 광고 보는 중)
광고가 닫혔습니다               ← 이 시점에 배너 다시 표시
```

---

## 💡 장점

### 1. 사용자 경험 개선
```
✅ 광고가 겹치지 않음
✅ 깔끔한 전면 광고
✅ 자연스러운 전환
```

### 2. 광고 정책 준수
```
✅ 여러 광고가 동시에 표시되지 않음
✅ AdMob 정책 위반 방지
✅ 광고 성과 향상 가능
```

### 3. 간단한 구현
```
✅ StateFlow로 상태 공유
✅ Compose에서 자동 반응
✅ 추가 라이브러리 불필요
```

---

## 🔍 기술적 세부사항

### StateFlow vs LiveData

**StateFlow 선택 이유:**
- Compose와 완벽한 호환
- `collectAsState()`로 간단한 관찰
- Coroutine 기반으로 효율적
- 초기값 필수로 안전

### collectAsState()

```kotlin
val isShowingAppOpenAd by app.isShowingAppOpenAd.collectAsState()
```

**동작:**
1. StateFlow의 현재 값을 State로 변환
2. 값이 변경되면 자동으로 Recompose
3. Compose 생명주기에 맞춰 자동 구독/해제

### 안전한 캐스팅

```kotlin
(application as? PocketChordApplication)?.setAppOpenAdShowing(true)
```

**이유:**
- `as?`: 캐스팅 실패 시 null 반환
- `?.`: null-safe 호출
- 다른 Application 클래스 사용 시에도 크래시 방지

---

## 📊 수정된 파일 요약

### 1. PocketChordApplication.kt
```diff
+ import kotlinx.coroutines.flow.MutableStateFlow
+ import kotlinx.coroutines.flow.StateFlow
+ import kotlinx.coroutines.flow.asStateFlow

+ private val _isShowingAppOpenAd = MutableStateFlow(false)
+ val isShowingAppOpenAd: StateFlow<Boolean> = _isShowingAppOpenAd.asStateFlow()
+ 
+ fun setAppOpenAdShowing(isShowing: Boolean) {
+     _isShowingAppOpenAd.value = isShowing
+ }
```

### 2. AppOpenAdManager.kt
```diff
+ import com.sweetapps.pocketchord.PocketChordApplication

  override fun onAdShowedFullScreenContent() {
+     (application as? PocketChordApplication)?.setAppOpenAdShowing(true)
  }
  
  override fun onAdDismissedFullScreenContent() {
+     (application as? PocketChordApplication)?.setAppOpenAdShowing(false)
  }
  
  override fun onAdFailedToShowFullScreenContent(adError: AdError) {
+     (application as? PocketChordApplication)?.setAppOpenAdShowing(false)
  }
```

### 3. MainActivity.kt
```diff
+ val app = context.applicationContext as PocketChordApplication
+ val isShowingAppOpenAd by app.isShowingAppOpenAd.collectAsState()

- if (isBannerEnabled && !isSplash) {
+ if (isBannerEnabled && !isSplash && !isShowingAppOpenAd) {
      TopBannerAd()
  }
```

---

## ⚠️ 주의사항

### 1. 다른 전면 광고도 동일하게 처리 가능

전면광고(Interstitial Ad)에서도 같은 방식 적용 가능:

```kotlin
// InterstitialAdManager에서도
override fun onAdShowedFullScreenContent() {
    (context.applicationContext as? PocketChordApplication)
        ?.setShowingInterstitialAd(true)
}
```

### 2. 여러 광고 타입 관리

```kotlin
class PocketChordApplication : Application() {
    private val _isShowingAnyFullScreenAd = MutableStateFlow(false)
    val isShowingAnyFullScreenAd: StateFlow<Boolean> = _isShowingAnyFullScreenAd.asStateFlow()
    
    fun setFullScreenAdShowing(isShowing: Boolean) {
        _isShowingAnyFullScreenAd.value = isShowing
    }
}

// 모든 전면 광고에서 호출
appOpenAdManager: setFullScreenAdShowing(true/false)
interstitialAdManager: setFullScreenAdShowing(true/false)
```

---

## 🎉 결과

### Before (문제)
```
┌─────────────────────────────┐
│ 배너 광고                     │ ← 겹침!
├─────────────────────────────┤
│                             │
│   앱 오프닝 광고 (전면)       │
│                             │
│                             │
└─────────────────────────────┘
```

### After (해결)
```
앱 오프닝 광고 표시 중:
┌─────────────────────────────┐
│                             │
│   앱 오프닝 광고 (전면)       │
│   (배너 광고 숨김)            │
│                             │
└─────────────────────────────┘

광고 닫힌 후:
┌─────────────────────────────┐
│ 배너 광고                     │ ← 다시 표시
├─────────────────────────────┤
│                             │
│   앱 콘텐츠                  │
│                             │
└─────────────────────────────┘
```

---

## ✅ 체크리스트

- [x] PocketChordApplication에 StateFlow 추가
- [x] AppOpenAdManager에서 상태 업데이트
- [x] MainActivity에서 StateFlow 관찰
- [x] 배너 표시 조건에 추가
- [x] 빌드 성공 확인
- [x] 테스트 시나리오 작성

---

**앱 오프닝 광고와 배너 광고 충돌이 해결되었습니다!** 🎉

이제 앱 오프닝 광고가 표시될 때 배너 광고가 자동으로 숨겨지고,
광고가 닫히면 다시 나타나서 깔끔한 사용자 경험을 제공합니다!

*해결일: 2025년 11월 2일*
*빌드 상태: ✅ SUCCESS*

