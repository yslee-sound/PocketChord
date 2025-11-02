package com.sweetapps.pocketchord.ui.screens

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sweetapps.pocketchord.R
import kotlinx.coroutines.delay

/**
 * 간단한 컴포즈 스플래시 화면
 * - 중앙에 임시 로고(drawable/ic_logo_temp)
 * - 부드러운 페이드 + 스케일 인 애니메이션
 * - 지정된 지연 후 콜백으로 다음 화면으로 이동
 */
@Composable
fun SplashScreen(
    logoResId: Int = R.drawable.ic_logo_temp,
    appName: String? = null,
    tagline: String? = null,
    background: Brush = Brush.verticalGradient(
        colors = listOf(Color(0xFFF7F8FC), Color(0xFFFFFFFF))
    ),
    durationMs: Long = 1600,
    onSplashFinished: () -> Unit
) {
    // 애니메이션 상태
    var started by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (started) 1f else 0.86f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "splash_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 700, easing = EaseOutCubic),
        label = "splash_alpha"
    )

    LaunchedEffect(Unit) {
        started = true
        // 전체 스플래시 표시 시간
        delay(durationMs)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 로고 카드 (살짝 그림자/라운드)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .size(160.dp)
                    .scale(scale)
                    .alpha(alpha)
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = logoResId),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // 앱명/태그라인(옵션)
            if (!appName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = appName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF1F2D3D),
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.alpha(alpha)
                )
            }
            if (!tagline.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = tagline,
                    fontSize = 14.sp,
                    color = Color(0xFF6B7C8C),
                    modifier = Modifier.alpha(alpha)
                )
            }
        }

        // 하단 저작권/빌드 정보 등(선택) 넣고 싶으면 여기 배치 가능
    }
}

