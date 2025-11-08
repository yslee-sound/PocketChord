# 전면광고 구현 가이드

## 개요
PocketChord 앱에 Google AdMob 전면광고(Interstitial Ad)를 효과적으로 통합했습니다.

## 구현 내용

### 1. 전면광고 관리 클래스 (`InterstitialAdManager.kt`)

#### 주요 기능:
- **자동 광고 로딩**: 앱 시작 시 미리 광고를 로드하여 빠른 표시
- **스마트 노출 빈도 제어**:
  - 최소 화면 전환 횟수: 3회
  - 광고 노출 간격: 60초 (1분)
- **사용자 설정 존중**: 설정에서 전면광고를 끌 수 있음

#### 설정값:
```kotlin
// 광고 노출 간격 (초)
private const val AD_INTERVAL_SECONDS = 60

// 광고 표시 전 최소 화면 전환 횟수  
private const val MIN_SCREEN_TRANSITIONS = 3
```

### 2. 광고 표시 시점

효과적인 사용자 경험을 위해 다음 시점에 광고를 표시합니다:

1. **코드 상세 화면 → 홈 화면** 전환 시
   - 사용자가 원하는 코드를 확인한 후 자연스러운 전환 시점

2. **메트로놈/튜너 → 홈 화면** 전환 시
   - 기능 사용 완료 후 돌아올 때

3. **더보기 → 설정 화면** 진입 시
   - 부가 기능 접근 시점

### 3. MainActivity 통합

```kotlin
class MainActivity : ComponentActivity() {
    private lateinit var interstitialAdManager: InterstitialAdManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // ...
        interstitialAdManager = InterstitialAdManager(this)
        
        // Navigation 라우트 변경 감지
        LaunchedEffect(currentRoute) {
            if (previousRoute?.startsWith("chord_list/") == true && currentRoute == "home") {
                interstitialAdManager.tryShowAd(this@MainActivity)
            }
            // ...
        }
    }
}
```

### 4. 개발자 도구 (디버그 전용)

설정 화면에서 광고 제어 옵션이 제공되지만, **디버그 빌드에서만** 표시됩니다:

- **배너 광고 테스트**: 배너 광고를 임시로 끄고 켤 수 있음 (테스트용)
- **전면 광고 로그**: 전면 광고 관련 로그 출력 제어 (디버깅용)

**릴리즈 빌드에서는 이 옵션들이 표시되지 않으며, 광고는 항상 활성화됩니다.**

설정은 `SharedPreferences`에 저장되어 디버그 세션 간에 유지됩니다.

## 테스트 ID

현재 테스트 광고 ID를 사용 중입니다:
```kotlin
private const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
```

## 실제 배포 시 변경 사항

### 1. AndroidManifest.xml
테스트 앱 ID를 실제 AdMob 앱 ID로 변경:
```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY" />
```

### 2. InterstitialAdManager.kt
테스트 광고 단위 ID를 실제 ID로 변경:
```kotlin
private const val AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY"
```

### 3. MainActivity.kt (TopBannerAd)
배너 광고 단위 ID도 실제 ID로 변경:
```kotlin
val adUnitId = "ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY"
```

## 광고 빈도 조정 방법

사용자 경험을 개선하거나 수익을 최적화하려면 `InterstitialAdManager.kt`에서 다음 값을 조정:

```kotlin
// 더 자주 표시하려면 값을 줄임 (예: 30초)
private const val AD_INTERVAL_SECONDS = 30

// 덜 자주 표시하려면 값을 늘림 (예: 5회)
private const val MIN_SCREEN_TRANSITIONS = 5
```

## 권장 사항

### 사용자 경험 우선
- 처음 앱 진입 시에는 광고를 표시하지 않음
- 일정 간격과 화면 전환 횟수를 충족해야 표시
- 설정에서 비활성화 가능

### A/B 테스트 고려
- 다양한 노출 빈도로 테스트하여 최적값 찾기
- 이탈률과 광고 수익의 균형점 찾기

### 분석 도구 연동
- Firebase Analytics 또는 AdMob 대시보드로 광고 성과 모니터링
- 사용자 피드백 수집

## 문제 해결

### 광고가 표시되지 않는 경우
1. 인터넷 연결 확인
2. AdMob 계정에서 광고 단위 활성화 확인
3. 로그 확인: `Log.d("InterstitialAdManager", ...)`
4. 광고 로드 실패 원인 확인

### 테스트 중 광고 표시 안 됨
- 테스트 기기를 AdMob에 등록했는지 확인
- 광고 로딩 시간이 필요 (앱 시작 후 몇 초 대기)

## 라이선스 및 정책

- Google AdMob 정책 준수 필수
- 앱 스토어 정책 확인 (Google Play, etc.)
- 사용자 동의 (GDPR, CCPA 등) 필요 시 추가 구현

