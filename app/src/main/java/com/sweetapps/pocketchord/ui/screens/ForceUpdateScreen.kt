package com.sweetapps.pocketchord.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 강제 업데이트 팝업 컴포넌트
 * 여러 앱에서 재사용 가능하도록 설계됨
 */
@Composable
fun ForceUpdateDialog(
    title: String = "앱 업데이트",
    description: String = "새로운 기능 추가, 더 빠른 속도, 버그 해결 등이 포함된 업데이트를 사용할 수 있습니다. 업데이트는 대부분 1분 내에 완료됩니다.",
    buttonText: String = "업데이트",
    features: List<String>? = null,
    onUpdateClick: () -> Unit,
    isDismissible: Boolean = false
) {
    Dialog(
        onDismissRequest = { if (isDismissible) onUpdateClick() },
        properties = DialogProperties(
            dismissOnBackPress = isDismissible,
            dismissOnClickOutside = isDismissible,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E3A3A))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 로봇 일러스트레이션
                RobotIllustration()

                Spacer(modifier = Modifier.height(40.dp))

                // 제목
                Text(
                    text = title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 설명
                Text(
                    text = description,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // 기능 목록 (옵션)
                features?.let {
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        it.forEach { feature ->
                            FeatureItem(feature)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // 업데이트 버튼
                Button(
                    onClick = onUpdateClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A7FFF)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = buttonText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun RobotIllustration() {
    // 애니메이션 효과
    val infiniteTransition = rememberInfiniteTransition(label = "robot")
    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .size(200.dp)
            .rotate(rotation),
        contentAlignment = Alignment.Center
    ) {
        // 로봇 몸체
        Box(
            modifier = Modifier
                .size(140.dp, 160.dp)
                .background(
                    Color(0xFF6B8E8E),
                    RoundedCornerShape(20.dp)
                )
        ) {
            // 렌치 아이콘 영역
            Box(
                modifier = Modifier
                    .size(80.dp, 60.dp)
                    .align(Alignment.Center)
                    .background(
                        Color(0xFF4A6B6B),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 렌치 표시 (간단한 도형으로)
                Box(
                    modifier = Modifier
                        .size(40.dp, 8.dp)
                        .rotate(45f)
                        .background(Color.White, RoundedCornerShape(4.dp))
                )
            }
        }

        // 로봇 머리
        Box(
            modifier = Modifier
                .size(100.dp, 80.dp)
                .offset(y = (-100).dp)
                .background(
                    Color(0xFF8BADB0),
                    RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
                )
        ) {
            // 눈
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .align(Alignment.Center)
                    .background(Color.White, RoundedCornerShape(15.dp))
            ) {
                Box(
                    modifier = Modifier
                        .size(15.dp)
                        .align(Alignment.Center)
                        .background(Color(0xFF2C4A4A), RoundedCornerShape(7.dp))
                )
            }
        }

        // 안테나
        Box(
            modifier = Modifier
                .size(4.dp, 30.dp)
                .offset(y = (-130).dp)
                .background(Color(0xFF6B8E8E))
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .offset(y = (-145).dp)
                .background(Color(0xFFFF4A8E), RoundedCornerShape(6.dp))
        )

        // 팔 (왼쪽)
        Box(
            modifier = Modifier
                .size(60.dp, 12.dp)
                .offset(x = (-80).dp, y = (-20).dp)
                .background(Color(0xFF6B8E8E), RoundedCornerShape(6.dp))
        )
        Box(
            modifier = Modifier
                .size(16.dp)
                .offset(x = (-110).dp, y = (-20).dp)
                .background(Color(0xFF4A7FFF), RoundedCornerShape(8.dp))
        )

        // 팔 (오른쪽)
        Box(
            modifier = Modifier
                .size(60.dp, 12.dp)
                .offset(x = 80.dp, y = (-20).dp)
                .background(Color(0xFF6B8E8E), RoundedCornerShape(6.dp))
        )
        Box(
            modifier = Modifier
                .size(16.dp)
                .offset(x = 110.dp, y = (-20).dp)
                .background(Color(0xFF4A7FFF), RoundedCornerShape(8.dp))
        )
    }
}

@Composable
private fun FeatureItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(Color(0xFF4A7FFF), RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}