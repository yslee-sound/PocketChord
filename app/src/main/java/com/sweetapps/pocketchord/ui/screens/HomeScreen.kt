package com.sweetapps.pocketchord.ui.screens

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.core.content.edit
import com.sweetapps.pocketchord.data.supabase.model.Announcement
import com.sweetapps.pocketchord.data.supabase.repository.AnnouncementRepository
import com.sweetapps.pocketchord.ui.dialogs.AnnouncementDialog

/**
 * 홈 화면 (코드 그리드)
 *
 * PocketChord 앱의 메인 화면으로, 12개의 코드 루트를 그리드 형태로 표시합니다.
 * 각 코드를 클릭하면 해당 루트의 코드 목록 화면으로 이동합니다.
 */
@Composable
fun MainScreen(navController: NavHostController) {
    // 공지사항 상태 관리
    var showAnnouncementDialog by remember { mutableStateOf(false) }
    var announcement by remember { mutableStateOf<Announcement?>(null) }
    val context = LocalContext.current

    // Flutter의 initState + addPostFrameCallback과 동일
    // 화면이 처음 표시될 때 공지사항 확인
    LaunchedEffect(Unit) {
        try {
            // Supabase에서 최신 공지사항 가져오기
            val repository = AnnouncementRepository(
                com.sweetapps.pocketchord.supabase,
                com.sweetapps.pocketchord.BuildConfig.SUPABASE_APP_ID
            )

            // 현재 사용 중인 appId 로그 출력 (디버깅용)
            Log.d("HomeScreen", "Using SUPABASE_APP_ID=${com.sweetapps.pocketchord.BuildConfig.SUPABASE_APP_ID}")

            repository.getLatestAnnouncement()
                .onSuccess { result ->
                    result?.let { ann ->
                        // ==================== Flutter 로직 적용 ====================
                        // _isViewed() 체크: 이미 본 공지사항인지 확인
                        val prefs = context.getSharedPreferences("announcement_prefs", android.content.Context.MODE_PRIVATE)
                        val viewedIds = prefs.getStringSet("viewed_announcements", setOf()) ?: setOf()

                        if (!viewedIds.contains(ann.id.toString())) {
                            // 본 적 없는 공지사항이면 표시
                            announcement = ann
                            showAnnouncementDialog = true
                            Log.d("HomeScreen", "✅ Showing new announcement: ${ann.title} (id=${ann.id})")
                        } else {
                            Log.d("HomeScreen", "⏭️ Announcement already viewed: id=${ann.id}")
                        }
                    } ?: run {
                        Log.d("HomeScreen", "No announcement found")
                    }
                }
                .onFailure { error ->
                    Log.e("HomeScreen", "Failed to load announcement", error)
                }
        } catch (e: Exception) {
            Log.e("HomeScreen", "Exception while loading announcement", e)
        }
    }

    // 공지사항 다이얼로그 표시
    if (showAnnouncementDialog && announcement != null) {
        AnnouncementDialog(
            announcement = announcement!!,
            onDismiss = {
                // ==================== Flutter의 _setViewed() 로직 적용 ====================
                announcement?.id?.let { id ->
                    val prefs = context.getSharedPreferences("announcement_prefs", android.content.Context.MODE_PRIVATE)

                    // 1. 기존의 공지사항 ID를 가져온다
                    val viewedIds = prefs.getStringSet("viewed_announcements", setOf())?.toMutableSet() ?: mutableSetOf()

                    // 2. 새 ID 추가 (contains 체크는 Set이 자동으로 처리)
                    viewedIds.add(id.toString())

                    // 3. 변경된 목록을 저장
                    prefs.edit {
                        putStringSet("viewed_announcements", viewedIds)
                    }

                    Log.d("HomeScreen", "✅ Marked announcement as viewed: id=$id")
                    Log.d("HomeScreen", "📋 Total viewed announcements: ${viewedIds.size}")
                }
                showAnnouncementDialog = false
            }
        )
    }

    // 기존 UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFD4E7F7),  // 위쪽 - 연한 하늘색
                        Color(0xFFE8F2FA)   // 아래쪽 - 더 밝은 하늘색
                    )
                )
            )
            .padding(start = 20.dp, end = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        TopBar()
        Spacer(modifier = Modifier.height(24.dp))
        ChordGrid(navController)
    }
}

/**
 * 상단 타이틀 바
 *
 * PocketChord 앱 이름을 간단한 텍스트로 표시합니다.
 */
@Composable
private fun TopBar() {
    Text(
        text = "PocketChord",
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        color = Color(0xFF1F2D3D)
    )
}

/**
 * 개별 코드 버튼
 *
 * @param chord 표시할 코드명 (예: "C", "C#-Db")
 * @param modifier Modifier
 */
@Composable
private fun ChordButton(chord: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = chord,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color(0xFF2F3B52)
            )
        }
    }
}

/**
 * 코드 그리드
 *
 * 12개의 코드 루트를 3x4 그리드로 표시합니다.
 * C, C#-Db, D, D#-Eb, E, F, F#-Gb, G, G#-Ab, A, A#-Bb, B
 */
@Composable
private fun ChordGrid(navController: NavHostController) {
    val chords = listOf(
        "C", "C#-Db", "D", "D#-Eb",
        "E", "F", "F#-Gb", "G",
        "G#-Ab", "A", "A#-Bb", "B"
    )

    // Map display names to root keys used in JSON
    fun getRoot(displayName: String): String {
        return when (displayName) {
            "C#-Db" -> "C#"
            "D#-Eb" -> "D#"
            "F#-Gb" -> "F#"
            "G#-Ab" -> "G#"
            "A#-Bb" -> "A#"
            else -> displayName
        }
    }

    Column {
        chords.chunked(3).forEach { rowList ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowList.forEach { chord ->
                    ChordButton(
                        chord = chord,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val root = getRoot(chord)
                                // encode to keep special characters (e.g., '#') safe in route path
                                val route = "chord_list/${Uri.encode(root)}"
                                Log.d("NavDebug", "Click: navigating to $route from grid (chord=$chord, root=$root)")
                                navController.navigate(route)
                            }
                    )
                }
                // fill remaining columns with spacers if row has less than 3 items
                if (rowList.size < 3) {
                    repeat(3 - rowList.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
