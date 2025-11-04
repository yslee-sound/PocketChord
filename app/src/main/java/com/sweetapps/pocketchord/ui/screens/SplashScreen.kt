package com.sweetapps.pocketchord.ui.screens

import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

/**
 * 스플래시 화면 관련 유틸리티
 *
 * Android 12+ (API 31+)의 SplashScreen API를 사용하여
 * 앱 시작 시 스플래시 화면을 표시합니다.
 *
 * 사용법:
 * ```kotlin
 * class MainActivity : ComponentActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         setupSplashScreen()
 *         super.onCreate(savedInstanceState)
 *         // ...
 *     }
 * }
 * ```
 */

/**
 * 스플래시 화면 설정
 *
 * @return SplashScreen 인스턴스 (추가 커스터마이징 필요 시 사용)
 */
fun ComponentActivity.setupSplashScreen(): SplashScreen {
    return installSplashScreen().apply {
        // 스플래시 화면 표시 조건 설정 (필요시 주석 해제)
        // setKeepOnScreenCondition { false }

        // 스플래시 화면 종료 시 애니메이션 설정 (필요시 주석 해제)
        // setOnExitAnimationListener { splashScreenView ->
        //     // 커스텀 종료 애니메이션 구현
        //     splashScreenView.remove()
        // }
    }
}

/**
 * 스플래시 화면 표시 시간 연장 (데이터 로딩 시 사용)
 *
 * 예시:
 * ```kotlin
 * val splash = setupSplashScreen()
 * var keepSplash = true
 *
 * splash.setKeepOnScreenCondition { keepSplash }
 *
 * lifecycleScope.launch {
 *     loadInitialData()
 *     keepSplash = false
 * }
 * ```
 */
fun SplashScreen.keepOnScreenWhile(condition: () -> Boolean) {
    setKeepOnScreenCondition(condition)
}

/**
 * 스플래시 화면 종료 애니메이션 설정
 *
 * 예시:
 * ```kotlin
 * splash.setExitAnimation { splashView ->
 *     splashView.iconView?.let { icon ->
 *         icon.animate()
 *             .alpha(0f)
 *             .setDuration(300)
 *             .withEndAction { splashView.remove() }
 *             .start()
 *     } ?: splashView.remove()
 * }
 * ```
 */
fun SplashScreen.setExitAnimation(
    animation: (androidx.core.splashscreen.SplashScreenViewProvider) -> Unit
) {
    setOnExitAnimationListener { splashScreenView ->
        animation(splashScreenView)
    }
}

