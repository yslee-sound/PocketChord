# 배너 광고 구분선 및 레이아웃 수정

## 문제점
1. 배너 광고 아래 구분선이 없어짐
2. 배너 광고 위치가 변형됨
3. 배너 광고가 상태바와 너무 붙어있음 (여백 부족)

## 해결 방법

### 1. 상태바 패딩 처리
- 고정된 30dp Spacer 제거
- `windowInsetsPadding`을 사용하여 실제 시스템바 높이에 자동 대응
- EdgeToEdge 모드에서 권장되는 방식

```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
        )
) {
    // 배너 광고 영역
}
```

### 2. TopBannerAd 수정
- 배너 광고를 Box로 감싸서 흰색 배경 제공
- 상태바와의 적절한 여백(8dp) 추가
- 광고 하단에만 HorizontalDivider 추가하여 시각적 구분
- elevation 없이 깔끔한 디자인

```kotlin
@Composable
fun TopBannerAd() {
    val testAdUnitId = "ca-app-pub-3940256099942544/6300978111"
    var adView by remember { mutableStateOf<com.google.android.gms.ads.AdView?>(null) }
    val bannerHeight = 50.dp

    Column(modifier = Modifier.fillMaxWidth()) {
        // 상태바와의 여백
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bannerHeight)
                .background(Color.White)
        ) {
            AndroidView(...)
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = Color(0xFFE0E0E0)
        )
    }
}
```

### 3. TopBannerAdPlaceholder 수정
- 광고가 비활성화되어도 동일한 레이아웃 유지
- 동일한 8dp 상단 여백 적용
- Placeholder에도 동일한 구분선 추가

```kotlin
@Composable
fun TopBannerAdPlaceholder() {
    val bannerHeight = 50.dp
    Column(modifier = Modifier.fillMaxWidth()) {
        // 상태바와의 여백
        Spacer(modifier = Modifier.height(8.dp))
        
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(bannerHeight)
                .background(Color.White)
        )
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = Color(0xFFE0E0E0)
        )
    }
}
```

## 변경사항
- ✅ windowInsetsPadding으로 상태바 영역 자동 처리 (권장 방식)
- ✅ 배너 광고 상단에 8dp 여백 추가 (상태바와 적절한 간격)
- ✅ 배너 광고를 Box로 감싸서 심플한 흰색 배경 제공
- ✅ 배너 광고 하단에만 구분선(HorizontalDivider) 추가
- ✅ 상단에는 그림자나 구분선 없이 깔끔한 디자인
- ✅ Placeholder도 동일한 여백과 구분선 적용하여 일관성 유지

## 효과
1. 상태바와 배너 광고 사이에 적절한 여백(8dp) 확보
2. windowInsetsPadding으로 다양한 기기에서 안전하게 대응
3. 배너 광고 하단에만 구분선으로 콘텐츠 영역과 명확히 구분
4. 광고 활성화/비활성화 시에도 동일한 레이아웃 유지
5. 시각적으로 더 깔끔하고 미니멀한 UI 제공
6. Material Design 가이드라인에 맞는 배너 광고 배치

