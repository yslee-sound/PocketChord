package com.sweetapps.pocketchord.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * 재사용 가능한 스플래시 화면 컴포넌트
 * 여러 앱에서 사용할 수 있도록 커스터마이징 가능
 */
@Composable
fun SplashScreen(
    logoResId: Int? = null,
    appName: String? = null,
    tagline: String? = null,
    backgroundColor: Color = Color.White,
    logoTint: Color? = null,
    showLoadingIndicator: Boolean = true,
    loadingIndicatorColor: Color = Color(0xFF4A7FFF),
    duration: Long = 2500L,
    onSplashFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    // 페이드 인 애니메이션
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "alpha"
    )

    // 스케일 애니메이션
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(duration)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .alpha(alphaAnim)
                .padding(32.dp)
        ) {
            // 로고
            logoResId?.let { resId ->
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(120.dp)
                        .alpha(alphaAnim),
                    contentScale = ContentScale.Fit,
                    colorFilter = logoTint?.let {
                        androidx.compose.ui.graphics.ColorFilter.tint(it)
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }

            // 앱 이름
            appName?.let { name ->
                Text(
                    text = name,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    modifier = Modifier.alpha(alphaAnim)
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // 태그라인
            tagline?.let { tag ->
                Text(
                    text = tag,
                    fontSize = 16.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.alpha(alphaAnim)
                )

                Spacer(modifier = Modifier.height(32.dp))
            }

            // 로딩 인디케이터
            if (showLoadingIndicator) {
                Spacer(modifier = Modifier.height(if (appName != null || tagline != null) 24.dp else 32.dp))
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(40.dp)
                        .alpha(alphaAnim),
                    color = loadingIndicatorColor,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

/**
 * 그라디언트 배경을 가진 스플래시 화면
 */
@Composable
fun SplashScreenGradient(
    logoResId: Int? = null,
    appName: String? = null,
    tagline: String? = null,
    gradientColors: List<Color> = listOf(
        Color(0xFF4A7FFF),
        Color(0xFF6B5FFF)
    ),
    contentColor: Color = Color.White,
    showLoadingIndicator: Boolean = true,
    duration: Long = 2500L,
    onSplashFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(duration)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = gradientColors
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .alpha(alphaAnim)
                .padding(32.dp)
        ) {
            logoResId?.let { resId ->
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(120.dp)
                        .alpha(alphaAnim),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(32.dp))
            }

            appName?.let { name ->
                Text(
                    text = name,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    modifier = Modifier.alpha(alphaAnim)
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            tagline?.let { tag ->
                Text(
                    text = tag,
                    fontSize = 16.sp,
                    color = contentColor.copy(alpha = 0.9f),
                    modifier = Modifier.alpha(alphaAnim)
                )
            }

            if (showLoadingIndicator) {
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(40.dp)
                        .alpha(alphaAnim),
                    color = contentColor,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

/**
 * 미니멀 스플래시 화면 (로고만)
 */
@Composable
fun SplashScreenMinimal(
    logoResId: Int,
    backgroundColor: Color = Color.White,
    logoSize: Int = 120,
    duration: Long = 2000L,
    onSplashFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 600,
            easing = FastOutSlowInEasing
        ),
        label = "alpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.9f,
        animationSpec = tween(
            durationMillis = 600,
            easing = FastOutSlowInEasing
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(duration)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = logoResId),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(logoSize.dp)
                .alpha(alphaAnim),
            contentScale = ContentScale.Fit
        )
    }
}