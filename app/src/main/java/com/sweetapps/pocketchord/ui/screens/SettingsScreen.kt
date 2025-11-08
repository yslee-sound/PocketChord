package com.sweetapps.pocketchord.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sweetapps.pocketchord.BuildConfig
import androidx.core.net.toUri

@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val appVersion = "${BuildConfig.VERSION_NAME}.${BuildConfig.BUILD_TYPE}"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "설정",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Settings Items
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 앱 버전
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "앱 버전",
                    subtitle = appVersion,
                    showArrow = false,
                    onClick = null
                )

                // 문의 하기
                SettingsItem(
                    icon = Icons.Default.Email,
                    title = "문의 하기",
                    subtitle = "개발자에게 문의하기",
                    showArrow = true,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = "mailto:support@pocketchord.com".toUri()
                            putExtra(Intent.EXTRA_SUBJECT, "PocketChord 문의")
                        }
                        try { context.startActivity(intent) } catch (_: Exception) {}
                    }
                )

                // 앱 평가하기
                SettingsItem(
                    icon = Icons.Default.Star,
                    title = "앱 평가하기",
                    subtitle = "Play 스토어에서 평가하기",
                    showArrow = true,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = "market://details?id=${context.packageName}".toUri()
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // Play 스토어 앱이 없는 경우 웹 브라우저로 열기
                            val webIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()
                            }
                            try { context.startActivity(webIntent) } catch (_: Exception) {}
                        }
                    }
                )

                // 디버그 설정 진입 (하위 스크린)
                SettingsItem(
                    icon = Icons.Default.BugReport,
                    title = "디버그 모드",
                    subtitle = "광고/아이콘/업데이트 도구",
                    showArrow = true,
                    onClick = { navController.navigate("debug_settings") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Info Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PocketChord",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6B7280)
                )
                Text(
                    text = "© 2025 All rights reserved",
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // 하단 네비게이션 바는 앱 스캐폴드에서 제공되므로, 이 화면에서는 별도 표시하지 않습니다.
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    showArrow: Boolean,
    onClick: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3F4F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            if (showArrow) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
