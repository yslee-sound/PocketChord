package com.sweetapps.pocketchord.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Update

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
                // 업데이트 일러스트레이션 (아이콘 기반)
                UpdateIllustration()

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
private fun UpdateIllustration() {
    // 은은한 맥동 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "update")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(160.dp)
            .scale(scale)
            .background(Color(0xFF355C5C), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // 중심 업데이트 아이콘
        Icon(
            imageVector = Icons.Filled.Update,
            contentDescription = "업데이트",
            tint = Color.White,
            modifier = Modifier.size(88.dp)
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