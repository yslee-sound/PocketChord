# 전면광고 빠른 시작 가이드

## 기본 사용법

### 1. 광고 매니저 초기화

```kotlin
class MainActivity : ComponentActivity() {
    private lateinit var interstitialAdManager: InterstitialAdManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // AdMob SDK 초기화
        MobileAds.initialize(this) {}
        
        // 전면광고 매니저 초기화
        interstitialAdManager = InterstitialAdManager(this)
    }
}
```

### 2. 광고 표시

#### 방법 1: 자동 조건 체크와 함께 표시
```kotlin
// 화면 전환 자동 기록 + 조건 충족 시 광고 표시
interstitialAdManager.tryShowAd(activity)
```

#### 방법 2: 수동 조건 체크
```kotlin
// 화면 전환만 기록
interstitialAdManager.recordScreenTransition()

// 나중에 광고 표시 (조건 충족 시)
if (interstitialAdManager.showAd(activity)) {
    Log.d("App", "광고가 표시되었습니다")
} else {
    Log.d("App", "광고 표시 조건 미달")
}
```

## 광고 표시 조건

광고는 다음 **모든** 조건을 충족해야 표시됩니다:

1. ✅ 광고가 미리 로드되어 있음
2. ✅ 마지막 광고 표시로부터 60초 이상 경과
3. ✅ 화면 전환 횟수 3회 이상
4. ✅ 설정에서 전면광고가 활성화되어 있음

## 커스터마이징

### 광고 빈도 조정

`InterstitialAdManager.kt` 파일에서:

```kotlin
companion object {
    // 광고 표시 간격 변경
    private const val AD_INTERVAL_SECONDS = 60  // 기본값: 60초
    
    // 최소 화면 전환 횟수 변경
    private const val MIN_SCREEN_TRANSITIONS = 3  // 기본값: 3회
}
```

**추천 설정:**
- **보수적**: 90초 간격, 5회 전환
- **일반**: 60초 간격, 3회 전환
- **적극적**: 30초 간격, 2회 전환

### 광고 ID 변경

실제 앱 배포 시 테스트 ID를 실제 AdMob ID로 변경:

```kotlin
// 테스트 ID (개발 중)
private const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

// 실제 ID (배포 시)
private const val AD_UNIT_ID = "ca-app-pub-1234567890123456/1234567890"
```

## Navigation Compose와 통합

### 화면 전환 감지

```kotlin
val navBackStackEntry by navController.currentBackStackEntryAsState()
val currentRoute = navBackStackEntry?.destination?.route

var previousRoute by remember { mutableStateOf<String?>(null) }

LaunchedEffect(currentRoute) {
    if (previousRoute != null && currentRoute != null) {
        // 특정 화면 전환에서 광고 표시
        if (previousRoute == "detail" && currentRoute == "home") {
            interstitialAdManager.tryShowAd(activity)
        }
    }
    previousRoute = currentRoute
}
```

### 추천 광고 표시 시점

✅ **좋은 시점:**
- 콘텐츠 조회 완료 후 메인으로 돌아갈 때
- 기능 사용 완료 후
- 자연스러운 화면 전환 시점

❌ **피해야 할 시점:**
- 앱 첫 실행 시
- 사용자 입력 중
- 중요한 작업 진행 중
- 결제/구매 프로세스 중

## 디버깅

### 로그 확인

```kotlin
// InterstitialAdManager에서 출력되는 로그
adb logcat | findstr "InterstitialAdManager"
```

**주요 로그 메시지:**
- `전면광고 로드 성공`: 광고가 성공적으로 로드됨
- `전면광고 표시됨`: 광고가 사용자에게 표시됨
- `광고 간격 미달`: 아직 광고 표시 간격이 충분하지 않음
- `화면 전환 횟수 미달`: 화면 전환 횟수가 부족함

### 테스트 기기 설정

AdMob 콘솔에서 테스트 기기 추가:
1. AdMob 콘솔 로그인
2. 앱 설정 → 테스트 기기
3. 디바이스 ID 추가

## 성능 최적화

### 광고 미리 로드

광고 매니저는 자동으로 다음 광고를 미리 로드합니다:

```kotlin
override fun onAdDismissedFullScreenContent() {
    interstitialAd = null
    // 광고가 닫힌 후 자동으로 다음 광고 로드
    loadAd()
}
```

### 메모리 관리

Activity 종료 시 리소스 정리는 자동으로 처리되지만,
필요 시 수동으로 관리할 수 있습니다:

```kotlin
override fun onDestroy() {
    super.onDestroy()
    // 필요 시 추가 정리 작업
}
```

## FAQ

### Q: 광고가 즉시 표시되지 않아요
A: 광고는 로딩 시간이 필요합니다. `InterstitialAdManager`는 앱 시작 시 자동으로 광고를 미리 로드하지만, 네트워크 상태에 따라 몇 초가 걸릴 수 있습니다.

### Q: 테스트 중 광고가 안 나와요
A: 
1. 인터넷 연결 확인
2. 로그에서 로드 실패 원인 확인
3. 테스트 기기 등록 확인
4. 조건 충족 여부 확인 (시간 간격, 화면 전환 횟수)

### Q: 사용자가 광고를 너무 많이 본다고 하는데요
A: `AD_INTERVAL_SECONDS`와 `MIN_SCREEN_TRANSITIONS` 값을 늘려서 광고 빈도를 줄이세요.

### Q: 특정 화면에서만 광고를 표시하고 싶어요
A: Navigation LaunchedEffect에서 원하는 화면 전환 조건만 남기고 나머지는 제거하세요.

### Q: 광고 수익이 안 나와요
A: 테스트 광고 ID를 사용 중이면 수익이 발생하지 않습니다. 실제 AdMob 광고 단위 ID로 변경해야 합니다.

## 다음 단계

1. ✅ 전면광고 기본 통합 완료
2. 🔄 실제 AdMob 계정 생성 및 광고 단위 ID 발급
3. 🔄 테스트 및 A/B 테스트로 최적 빈도 찾기
4. 🔄 사용자 피드백 수집
5. 🔄 Analytics 연동으로 성과 측정
6. 🔄 GDPR/CCPA 동의 시스템 구현 (필요시)

## 참고 자료

- [AdMob 공식 문서](https://developers.google.com/admob/android/interstitial)
- [Jetpack Compose와 AdMob](https://developers.google.com/admob/android/compose)
- [광고 정책](https://support.google.com/admob/answer/6128543)

