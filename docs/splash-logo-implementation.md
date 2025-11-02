# 스플래시 화면 로고 추가

## 변경 내용

스플래시 화면에 텍스트 대신 로고 이미지를 표시하도록 변경했습니다.

---

## 📊 Before vs After

### Before
```
스플래시 화면:
┌─────────────────┐
│                 │
│  PocketChord    │ ← 텍스트
│  코드 검색을     │
│  더 쉽게        │
│                 │
│  ⭕ 로딩...     │
└─────────────────┘
```

### After
```
스플래시 화면:
┌─────────────────┐
│                 │
│      🎸         │ ← 로고 이미지
│    (로고)       │
│                 │
│  ⭕ 로딩...     │
└─────────────────┘
```

---

## 🎨 추가된 로고

### 파일
`app/src/main/res/drawable/ic_logo_temp.xml`

### 디자인
- **형태**: 원형 배경 + 기타픽 + 음표 + "C" 문자
- **색상**: 보라색 배경 (#6F4EF6) + 흰색 요소
- **크기**: 120dp x 120dp
- **타입**: 벡터 드로어블 (XML)

### 구성 요소
```
┌─────────────┐
│   ⭕ 보라색   │
│   원형 배경   │
│             │
│   🎸 기타픽  │
│   🎵 음표    │
│   C (코드)   │
└─────────────┘
```

---

## 💻 코드 변경

### MainActivity.kt

**Before:**
```kotlin
composable("splash") {
    com.sweetapps.pocketchord.ui.screens.SplashScreen(
        appName = "PocketChord",
        tagline = "코드 검색을 더 쉽게",
        onSplashFinished = { /* ... */ }
    )
}
```

**After:**
```kotlin
composable("splash") {
    com.sweetapps.pocketchord.ui.screens.SplashScreen(
        logoResId = R.drawable.ic_logo_temp,
        appName = null,  // 로고만 표시
        tagline = null,  // 로고만 표시
        onSplashFinished = { /* ... */ }
    )
}
```

---

## 🔄 향후 로고 교체 방법

### 방법 1: XML 파일 수정 (간단)

`app/src/main/res/drawable/ic_logo_temp.xml` 파일을 수정:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="120dp"
    android:height="120dp"
    android:viewportWidth="120"
    android:viewportHeight="120">
    
    <!-- 여기에 새 로고 디자인 추가 -->
    
</vector>
```

### 방법 2: 이미지 파일로 교체 (권장)

1. **PNG/SVG 로고 준비**
   - 권장 크기: 512x512px 이상
   - 투명 배경 권장
   - 포맷: PNG 또는 SVG

2. **파일 추가**
   ```
   app/src/main/res/drawable/
   ├── ic_logo_temp.xml  ← 삭제
   └── ic_logo_temp.png  ← 추가 (또는 .webp)
   ```

3. **코드 변경 불필요**
   - `R.drawable.ic_logo_temp`는 자동으로 새 파일 참조

### 방법 3: 다른 파일명 사용

1. **새 로고 추가**
   ```
   app/src/main/res/drawable/
   ├── ic_logo_temp.xml     ← 기존 (임시)
   └── ic_logo_final.png    ← 새 로고
   ```

2. **MainActivity.kt 수정**
   ```kotlin
   logoResId = R.drawable.ic_logo_final,  // 변경
   ```

---

## 📐 로고 크기 가이드

### 현재 설정
```kotlin
// SplashScreen.kt
Image(
    painter = painterResource(id = resId),
    contentDescription = "App Logo",
    modifier = Modifier.size(120.dp),  // 현재 크기
    // ...
)
```

### 크기 조정 (필요 시)

**MainActivity.kt에서 크기 직접 지정:**
```kotlin
composable("splash") {
    com.sweetapps.pocketchord.ui.screens.SplashScreen(
        logoResId = R.drawable.ic_logo_temp,
        // ...
    )
}
```

**SplashScreen 컴포넌트 수정:**
```kotlin
// 크기를 파라미터로 받도록 수정 가능
fun SplashScreen(
    logoResId: Int? = null,
    logoSize: Dp = 120.dp,  // 추가
    // ...
)
```

---

## 🎨 로고 디자인 권장사항

### 브랜딩
```
✅ 앱 정체성 반영
✅ 기타/음악/코드 관련 요소
✅ 기억하기 쉬운 형태
```

### 기술적 요구사항
```
✅ 벡터 형식 (SVG → XML) 또는 고해상도 PNG
✅ 투명 배경 (선택사항)
✅ 최소 크기: 512x512px
✅ 단순하고 명확한 형태
```

### 색상
```
✅ 브랜드 컬러 사용
✅ 대비가 명확한 색상
✅ 다크모드 고려 (필요 시)
```

---

## 🔧 애니메이션

### 현재 적용된 애니메이션
```kotlin
// 페이드 인 애니메이션
val alphaAnim by animateFloatAsState(
    targetValue = if (startAnimation) 1f else 0f,
    animationSpec = tween(
        durationMillis = 800,
        easing = FastOutSlowInEasing
    )
)

// 스케일 애니메이션
val scaleAnim by animateFloatAsState(
    targetValue = if (startAnimation) 1f else 0.8f,
    animationSpec = tween(
        durationMillis = 800,
        easing = FastOutSlowInEasing
    )
)
```

### 효과
- ✅ 로고가 서서히 나타남 (페이드 인)
- ✅ 로고가 살짝 확대됨 (0.8 → 1.0)
- ✅ 부드러운 전환 (800ms)

---

## 📱 테스트 방법

### 확인 사항
```
1. 앱 실행
2. ✅ 스플래시 화면에 로고 표시 확인
3. ✅ 텍스트 없음 확인
4. ✅ 로딩 인디케이터 표시 확인
5. ✅ 2.5초 후 메인 화면 전환 확인
```

### 로고 크기 확인
```
1. 로고가 너무 크거나 작지 않은지 확인
2. 화면 중앙에 적절히 배치되었는지 확인
3. 애니메이션이 자연스러운지 확인
```

---

## 🎯 임시 로고 vs 최종 로고

### 임시 로고 (현재)
```
파일: ic_logo_temp.xml
타입: 벡터 드로어블
디자인: 기본 형태 (기타픽 + 음표 + C)
목적: 플레이스홀더
```

### 최종 로고 (향후)
```
파일: ic_logo_final.png/xml
타입: 고해상도 이미지 또는 정교한 벡터
디자인: 전문 디자이너 작업
목적: 실제 브랜드 정체성
```

---

## 📚 관련 파일

### 수정된 파일
1. ✅ **MainActivity.kt** - 스플래시 화면 설정 변경
2. ✅ **ic_logo_temp.xml** - 임시 로고 추가 (새 파일)

### 관련 파일 (수정 안 함)
- **SplashScreen.kt** - 재사용 가능한 컴포넌트 (수정 불필요)
- **ic_launcher_foreground.xml** - 앱 아이콘 (별도)

---

## 💡 추가 커스터마이징

### 배경색 변경
```kotlin
composable("splash") {
    com.sweetapps.pocketchord.ui.screens.SplashScreen(
        logoResId = R.drawable.ic_logo_temp,
        backgroundColor = Color(0xFF6F4EF6),  // 보라색 배경
        // ...
    )
}
```

### 로딩 인디케이터 색상 변경
```kotlin
composable("splash") {
    com.sweetapps.pocketchord.ui.screens.SplashScreen(
        logoResId = R.drawable.ic_logo_temp,
        loadingIndicatorColor = Color(0xFF6F4EF6),  // 보라색
        // ...
    )
}
```

### 로딩 인디케이터 숨김
```kotlin
composable("splash") {
    com.sweetapps.pocketchord.ui.screens.SplashScreen(
        logoResId = R.drawable.ic_logo_temp,
        showLoadingIndicator = false,  // 숨김
        // ...
    )
}
```

---

## ✅ 완료 체크리스트

- [x] 임시 로고 XML 파일 생성
- [x] MainActivity에 로고 적용
- [x] 텍스트 제거 (appName, tagline)
- [x] 빌드 성공 확인
- [x] 문서 작성

---

**스플래시 화면에 로고 이미지가 추가되었습니다!** 🎉

이제 앱 실행 시 텍스트 대신 로고가 표시되며,
나중에 `ic_logo_temp.xml` 파일을 교체하여 최종 로고로 변경할 수 있습니다!

*작업일: 2025년 11월 2일*
*빌드 상태: ✅ SUCCESS*

