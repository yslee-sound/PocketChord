# 배너 광고 영역 고정으로 UI 변형 방지

## 문제 상황

배너 광고가 있을 때와 없을 때 UI가 변형되는 문제가 있었습니다.

### Before (문제)
```
배너 광고 ON:
┌─────────────────────┐
│ 배너 광고 (50dp)     │
├─────────────────────┤
│                     │
│ 앱 콘텐츠           │
│                     │
└─────────────────────┘

배너 광고 OFF:
┌─────────────────────┐
│ 앱 콘텐츠           │ ← UI가 위로 이동!
│                     │
│                     │
└─────────────────────┘
```

**문제점:**
- 배너 광고 ON/OFF에 따라 콘텐츠 위치가 변경됨
- 앱 오프닝 광고 표시 시 배너가 사라지면서 UI가 튀어오름
- 사용자 경험 저하

---

## ✅ 해결 방법

**배너 광고 영역을 항상 고정하고, 광고가 없을 때는 같은 크기의 빈 공간(Placeholder)을 표시**

### After (해결)
```
배너 광고 ON:
┌─────────────────────┐
│ 배너 광고 (50dp)     │ ← 고정 높이
├─────────────────────┤
│                     │
│ 앱 콘텐츠           │
│                     │
└─────────────────────┘

배너 광고 OFF:
┌─────────────────────┐
│ 빈 공간 (50dp)       │ ← 같은 높이 유지
├─────────────────────┤
│                     │
│ 앱 콘텐츠           │
│                     │
└─────────────────────┘
```

**개선점:**
- ✅ 배너 광고 유무와 관계없이 UI 위치 고정
- ✅ 앱 오프닝 광고 표시 시에도 UI 변형 없음
- ✅ 자연스러운 사용자 경험

---

## 🔧 구현 내용

### 1. TopBannerAd - 고정 높이 적용

```kotlin
@Composable
fun TopBannerAd() {
    val testAdUnitId = "ca-app-pub-3940256099942544/6300978111"
    var adView by remember { mutableStateOf<com.google.android.gms.ads.AdView?>(null) }
    
    // 배너 광고의 표준 높이 (50dp) - 고정
    val bannerHeight = 50.dp
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(bannerHeight)  // 고정 높이 설정
    ) {
        AndroidView(
            factory = { context ->
                com.google.android.gms.ads.AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    setAdUnitId(testAdUnitId)
                    loadAd(AdRequest.Builder().build())
                    adView = this
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(bannerHeight)  // AndroidView도 같은 높이
        )
    }
    // ...existing code...
}
```

**주요 변경점:**
- `wrapContentHeight()` 제거
- `height(50.dp)` 고정 높이 적용
- `HorizontalDivider` 제거 (불필요)

---

### 2. TopBannerAdPlaceholder - 빈 공간 추가

```kotlin
@Composable
fun TopBannerAdPlaceholder() {
    // 배너 광고가 없을 때 같은 크기의 빈 공간
    val bannerHeight = 50.dp
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(bannerHeight)
    )
}
```

**목적:**
- 배너 광고와 정확히 같은 크기의 빈 공간
- UI 레이아웃 일관성 유지

---

### 3. MainActivity - 조건부 표시 로직

```kotlin
Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
    // 배너 광고 영역: 항상 고정 (광고 유무와 관계없이)
    if (!isSplash) {
        // 배너 활성화 && 앱 오프닝 광고 표시 중 아님 → 광고 표시
        if (isBannerEnabled && !isShowingAppOpenAd) {
            TopBannerAd()
        } else {
            // 광고가 없을 때 → 같은 크기의 빈 공간
            TopBannerAdPlaceholder()
        }
    }
    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = Modifier.weight(1f)
    ) {
        // ...existing code...
    }
}
```

**로직:**
1. 스플래시 화면이 아니면 배너 영역 표시
2. 배너 활성화 && 앱 오프닝 광고 표시 중 아님 → **광고 표시**
3. 그 외의 경우 → **빈 공간 표시**

---

## 🎯 동작 시나리오

### 시나리오 1: 정상 사용 (배너 ON)

```
1. 앱 실행
   → 배너 광고 표시 (50dp)
   
2. 앱 콘텐츠 사용
   → UI 위치 고정
   
3. 백그라운드 복귀
   → 앱 오프닝 광고 표시
   → 배너 영역은 빈 공간으로 변경 (50dp 유지)
   → UI 위치 변화 없음 ✅
   
4. 광고 닫기
   → 배너 광고 다시 표시 (50dp)
   → UI 위치 변화 없음 ✅
```

### 시나리오 2: 배너 OFF (테스트 모드)

```
1. 설정 → 개발 도구
   → 배너 광고 테스트 OFF
   
2. 앱 화면
   → 빈 공간 표시 (50dp)
   → UI 위치는 배너 ON과 동일 ✅
   
3. 배너 광고 테스트 ON
   → 배너 광고 표시 (50dp)
   → UI 위치 변화 없음 ✅
```

### 시나리오 3: 스플래시 화면

```
1. 앱 첫 실행
   → 스플래시 화면 (배너 영역 없음)
   
2. 메인 화면 진입
   → 배너 영역 표시 (광고 또는 빈 공간)
```

---

## 💡 장점

### 1. UI 안정성
```
✅ 배너 광고 ON/OFF와 관계없이 레이아웃 고정
✅ 앱 오프닝 광고 표시 시에도 UI 변형 없음
✅ 설정 변경 시 튀는 현상 없음
```

### 2. 일관된 사용자 경험
```
✅ 콘텐츠 위치가 항상 동일
✅ 예측 가능한 UI
✅ 자연스러운 전환
```

### 3. 개발 편의성
```
✅ 단순한 구조
✅ 유지보수 용이
✅ 추가 계산 불필요
```

---

## 📏 표준 배너 광고 크기

### AdMob 배너 크기 참고

| 크기 | 설명 | 높이 |
|------|------|------|
| BANNER | 표준 배너 | 50dp |
| LARGE_BANNER | 큰 배너 | 100dp |
| MEDIUM_RECTANGLE | 중간 사각형 | 250dp |
| FULL_BANNER | 전체 배너 | 60dp |

**현재 사용:** BANNER (50dp)

---

## 🧪 테스트 방법

### 테스트 1: 배너 ON/OFF 전환

```
1. 앱 실행 (배너 ON)
2. 콘텐츠 위치 확인
3. 설정 → 개발 도구 → 배너 광고 테스트 OFF
4. ✅ 확인: 콘텐츠 위치가 그대로 유지됨
5. 배너 광고 테스트 ON
6. ✅ 확인: 콘텐츠 위치가 그대로 유지됨
```

### 테스트 2: 앱 오프닝 광고

```
1. 앱 실행 (배너 표시)
2. 콘텐츠 위치 기억
3. 백그라운드 → 복귀
4. 앱 오프닝 광고 표시
5. ✅ 확인: 콘텐츠 위치가 변하지 않음
6. 광고 닫기
7. ✅ 확인: 콘텐츠 위치가 그대로 유지됨
```

### 테스트 3: 스플래시 화면

```
1. 앱 재시작
2. 스플래시 화면 (배너 영역 없음)
3. 메인 화면 진입
4. ✅ 확인: 배너 영역 표시 (광고 또는 빈 공간)
```

---

## 🎨 시각적 비교

### Before (문제)
```
배너 ON → OFF 전환 시:

[배너 광고 50dp]     →     [빈 공간 없음]
─────────────────         ─────────────────
[콘텐츠]                   [콘텐츠] ← 위로 튐!
[콘텐츠]                   [콘텐츠]
[콘텐츠]                   [콘텐츠]
```

### After (해결)
```
배너 ON → OFF 전환 시:

[배너 광고 50dp]     →     [빈 공간 50dp]
─────────────────         ─────────────────
[콘텐츠]                   [콘텐츠] ← 위치 고정!
[콘텐츠]                   [콘텐츠]
[콘텐츠]                   [콘텐츠]
```

---

## 📊 수정된 코드 요약

### TopBannerAd
```diff
- modifier = Modifier.fillMaxWidth()
+ modifier = Modifier.fillMaxWidth().height(50.dp)

- modifier = Modifier.fillMaxWidth().wrapContentHeight()
+ modifier = Modifier.fillMaxWidth().height(50.dp)

- HorizontalDivider(color = Color(0x1A000000))
```

### TopBannerAdPlaceholder (새로 추가)
```kotlin
@Composable
fun TopBannerAdPlaceholder() {
    Spacer(modifier = Modifier.fillMaxWidth().height(50.dp))
}
```

### MainActivity
```diff
- if (isBannerEnabled && !isSplash && !isShowingAppOpenAd) {
-     TopBannerAd()
- }

+ if (!isSplash) {
+     if (isBannerEnabled && !isShowingAppOpenAd) {
+         TopBannerAd()
+     } else {
+         TopBannerAdPlaceholder()
+     }
+ }
```

---

## ⚙️ 기술적 세부사항

### 고정 높이 선택 이유

**50dp:**
- AdMob BANNER 표준 크기
- 모든 화면 크기에서 일관성
- 충분히 작아서 콘텐츠 방해 최소화

### Spacer vs Box

**Spacer 선택 이유:**
- 단순히 공간만 차지
- 렌더링 오버헤드 최소
- 의도가 명확

### 조건 분리

```kotlin
if (!isSplash) {  // 외부 조건
    if (조건) {
        광고
    } else {
        빈 공간
    }
}
```

**이유:**
- 스플래시 화면에서는 배너 영역 자체가 없음
- 명확한 조건 구분
- 유지보수 용이

---

## 🎉 결과

### UI 안정성 개선
```
✅ 배너 광고 유무와 관계없이 레이아웃 고정
✅ 광고 전환 시 UI 변형 없음
✅ 자연스러운 사용자 경험
```

### 개발 편의성
```
✅ 단순한 구조
✅ 예측 가능한 동작
✅ 디버깅 용이
```

---

**배너 광고 영역이 고정되어 UI 변형 문제가 해결되었습니다!** 🎉

이제 배너 광고가 있든 없든 앱 콘텐츠의 위치는 항상 동일하게 유지되어
사용자에게 안정적인 경험을 제공합니다!

*해결일: 2025년 11월 2일*
*빌드 상태: ✅ SUCCESS*

