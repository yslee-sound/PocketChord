package com.sweetapps.pocketchord.ui.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
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
import androidx.core.net.toUri
import com.google.gson.Gson
import com.sweetapps.pocketchord.data.supabase.model.Announcement
import com.sweetapps.pocketchord.data.supabase.model.UpdateInfo
import com.sweetapps.pocketchord.data.supabase.model.AppPolicy
import com.sweetapps.pocketchord.data.supabase.repository.AppPolicyRepository
import com.sweetapps.pocketchord.ui.dialogs.AnnouncementDialog
import com.sweetapps.pocketchord.ui.dialogs.OptionalUpdateDialog
import com.sweetapps.pocketchord.ui.dialogs.EmergencyRedirectDialog
import android.content.Intent
import android.content.ActivityNotFoundException
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import com.sweetapps.pocketchord.PocketChordApplication

/**
 * 홈 화면 (코드 그리드)
 *
 * PocketChord 앱의 메인 화면으로, 12개의 코드 루트를 그리드 형태로 표시합니다.
 * 각 코드를 클릭하면 해당 루트의 코드 목록 화면으로 이동합니다.
 */
@Composable
fun MainScreen(navController: NavHostController) {
    // 팝업 상태 관리 (우선순위: emergency > update > notice)
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }

    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var announcement by remember { mutableStateOf<Announcement?>(null) }
    val context = LocalContext.current
    val app = context.applicationContext as PocketChordApplication
    val supabaseClient = app.supabase
    val updatePrefs: SharedPreferences = remember { context.getSharedPreferences("update_prefs", android.content.Context.MODE_PRIVATE) }
    val dismissedVersionCode = remember { mutableStateOf(updatePrefs.getInt("dismissed_version_code", -1)) }
    val gson = remember { Gson() }
    var showNetworkHelpDialog by remember { mutableStateOf(false) }

    // 스토어 열기 시도(보통/권장 UX 포함): 오프라인이면 도움말, market:// → https:// 폴백
    fun tryOpenStore(info: UpdateInfo) {
        if (!isOnline(context)) {
            showNetworkHelpDialog = true
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = (info.downloadUrl ?: "market://details?id=${context.packageName}").toUri()
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            val webIntent = Intent(Intent.ACTION_VIEW).apply {
                data = "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()
            }
            try {
                context.startActivity(webIntent)
            } catch (_: Exception) {
                showNetworkHelpDialog = true
            }
        }
    }

    // 강제 업데이트가 표시 중일 때 시스템 뒤로가기를 차단 (이중 안전장치)
    if (showUpdateDialog && (updateInfo?.isForce == true)) {
        BackHandler(enabled = true) { }
    }

    // 화면이 처음 표시될 때 팝업 확인 (우선순위: emergency > 강제업데이트 > 선택적 업데이트 > 공지)
    LaunchedEffect(Unit) {
        try {
            Log.d("HomeScreen", "Startup: SUPABASE_APP_ID=${com.sweetapps.pocketchord.BuildConfig.SUPABASE_APP_ID}, VERSION_CODE=${com.sweetapps.pocketchord.BuildConfig.VERSION_CODE}")
            Log.d("HomeScreen", "Supabase configured=${app.isSupabaseConfigured}")
            // 강제 업데이트 로컬 복원 (오프라인 대비)
            val storedForceVersion = updatePrefs.getInt("force_required_version", -1)
            var restoredForcedUpdate: UpdateInfo? = null
            if (storedForceVersion != -1 && storedForceVersion > com.sweetapps.pocketchord.BuildConfig.VERSION_CODE) {
                val json = updatePrefs.getString("force_update_info", null)
                restoredForcedUpdate = runCatching { json?.let { gson.fromJson(it, UpdateInfo::class.java) } }.getOrNull()
                    ?: UpdateInfo(
                        id = null,
                        versionCode = storedForceVersion,
                        versionName = "",
                        appId = com.sweetapps.pocketchord.BuildConfig.SUPABASE_APP_ID,
                        isForce = true,
                        releaseNotes = "",
                        releasedAt = null,
                        downloadUrl = null
                    )
            } else if (storedForceVersion != -1 && storedForceVersion <= com.sweetapps.pocketchord.BuildConfig.VERSION_CODE) {
                updatePrefs.edit { remove("force_required_version"); remove("force_update_info") }
            }

            if (!app.isSupabaseConfigured) {
                Log.w("HomeScreen", "Skipping network fetch (Supabase not configured)")
                // Supabase 설정 없음 → 네트워크 조회 생략, 강제 업데이트 복원만 표시
                restoredForcedUpdate?.let { upd ->
                    updateInfo = upd
                    showUpdateDialog = true
                }
                return@LaunchedEffect
            }

            // 단일 정책(app_policy) 조회
            var policy: AppPolicy? = null
            var policyError: Throwable? = null
            AppPolicyRepository(supabaseClient)
                .getPolicy()
                .onSuccess { policy = it; Log.d("HomeScreen", "Policy fetch success: ${policy?.let { p -> "id=${p.id} appId=${p.appId} active=${p.isActive} emergency=${p.emergencyIsActive} minSupported=${p.minSupportedVersion} latest=${p.latestVersionCode} updateActive=${p.updateIsActive} noticeActive=${p.noticeIsActive}" } ?: "null"}") }
                .onFailure { e -> policyError = e; Log.e("HomeScreen", "Policy fetch failure: ${e.message}", e) }
            if (policy == null) {
                Log.w("HomeScreen", "No active policy row for app_id='${com.sweetapps.pocketchord.BuildConfig.SUPABASE_APP_ID}'. Check: (1) app_policy.app_id 값, (2) is_active=true, (3) RLS policy allowing read, (4) anon key valid.")
                // 정책 없음 → 복원 강제 업데이트가 있으면 표시, 아니면 아무 것도 없음
                restoredForcedUpdate?.let { upd ->
                    updateInfo = upd
                    showUpdateDialog = true
                }
                return@LaunchedEffect
            }

            val p = policy!!
            // 1) 긴급 공지
            if (p.emergencyIsActive && !p.emergencyTitle.isNullOrBlank() && !p.emergencyContent.isNullOrBlank()) {
                Log.d("HomeScreen", "Decision: EMERGENCY popup will show")
                announcement = Announcement(
                    id = null,
                    createdAt = null,
                    appId = com.sweetapps.pocketchord.BuildConfig.SUPABASE_APP_ID,
                    title = p.emergencyTitle!!,
                    content = p.emergencyContent!!,
                    isActive = true,
                    kind = "emergency",
                    redirectUrl = p.emergencyRedirectUrl,
                    dismissible = p.emergencyDismissible
                )
                showEmergencyDialog = true
                // 정책이 유효하므로 이전 강제 캐시 정리
                if (storedForceVersion != -1) updatePrefs.edit { remove("force_required_version"); remove("force_update_info") }
                return@LaunchedEffect
            }

            // 2) 강제 업데이트
            if (p.requiresForceUpdate(com.sweetapps.pocketchord.BuildConfig.VERSION_CODE)) {
                Log.d("HomeScreen", "Decision: FORCE UPDATE popup (minSupported=${p.minSupportedVersion})")
                updateInfo = UpdateInfo(
                    id = null,
                    versionCode = p.minSupportedVersion ?: (com.sweetapps.pocketchord.BuildConfig.VERSION_CODE + 1),
                    versionName = "",
                    appId = com.sweetapps.pocketchord.BuildConfig.SUPABASE_APP_ID,
                    isForce = true,
                    releaseNotes = "",
                    releasedAt = null,
                    downloadUrl = p.downloadUrl
                )
                showUpdateDialog = true
                updatePrefs.edit {
                    putInt("force_required_version", updateInfo!!.versionCode)
                    putString("force_update_info", gson.toJson(updateInfo!!))
                }
                return@LaunchedEffect
            } else {
                // 강제 업데이트 조건 해제 → 캐시 제거
                if (storedForceVersion != -1) updatePrefs.edit { remove("force_required_version"); remove("force_update_info") }
            }

            // 3) 선택적 업데이트
            val optionalAllowed = p.updateIsActive && (p.latestVersionCode ?: 0) > com.sweetapps.pocketchord.BuildConfig.VERSION_CODE
            if (optionalAllowed && dismissedVersionCode.value != (p.latestVersionCode ?: -1)) {
                Log.d("HomeScreen", "Decision: OPTIONAL UPDATE popup (latest=${p.latestVersionCode})")
                updateInfo = UpdateInfo(
                    id = null,
                    versionCode = p.latestVersionCode!!,
                    versionName = "",
                    appId = com.sweetapps.pocketchord.BuildConfig.SUPABASE_APP_ID,
                    isForce = false,
                    releaseNotes = "",
                    releasedAt = null,
                    downloadUrl = p.downloadUrl
                )
                showUpdateDialog = true
                return@LaunchedEffect
            }

            // 4) 일반 공지
            if (p.noticeIsActive == true && !p.noticeTitle.isNullOrBlank() && !p.noticeContent.isNullOrBlank()) {
                Log.d("HomeScreen", "Decision: NOTICE popup (title='${p.noticeTitle}')")
                announcement = Announcement(
                    id = null,
                    createdAt = null,
                    appId = com.sweetapps.pocketchord.BuildConfig.SUPABASE_APP_ID,
                    title = p.noticeTitle!!,
                    content = p.noticeContent!!,
                    isActive = true,
                    kind = "announcement",
                    redirectUrl = null,
                    dismissible = true
                )
                showAnnouncementDialog = true
                return@LaunchedEffect
            }
            // 아무 조건도 해당되지 않음 → 아무 것도 표시하지 않음
        } catch (e: Exception) {
            Log.e("HomeScreen", "Exception while loading policy", e)
        }
    }

    // ==================== 팝업 표시 (우선순위: emergency > update > notice) ====================

    // 1순위: Emergency (향후 구현)
    if (showEmergencyDialog && announcement?.isEmergency == true) {
        val em = announcement!!
        com.sweetapps.pocketchord.ui.dialogs.EmergencyRedirectDialog(
            title = em.title,
            description = em.content,
            newAppPackage = "com.sweetapps.pocketchord2",
            redirectUrl = em.redirectUrl,
            isDismissible = em.dismissible,
            onDismiss = {
                showEmergencyDialog = false
            }
        )
    }
    // 2순위: Update
    else if (showUpdateDialog && updateInfo != null) {
        val features = remember(updateInfo) {
            // 릴리즈 노트를 줄 단위 bullet 로 분리 (빈 줄/공백 제거 + 선행 기호 제거)
            updateInfo!!.releaseNotes
                .split('\n')
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { line ->
                    line.removePrefix("- ").removePrefix("*").removePrefix("* ").removePrefix("• ")
                }
        }
        OptionalUpdateDialog(
            isForce = updateInfo!!.isForce,
            title = "앱 업데이트",
            updateButtonText = "지금 업데이트",
            features = if (features.isNotEmpty()) features else null,
            onUpdateClick = {
                tryOpenStore(updateInfo!!)
            },
            onLaterClick = if (updateInfo!!.isForce) null else {
                {
                    // 선택적 업데이트를 사용자가 닫았으므로 동일 versionCode 재표시 방지 저장
                    updatePrefs.edit {
                        putInt("dismissed_version_code", updateInfo!!.versionCode)
                    }
                    dismissedVersionCode.value = updateInfo!!.versionCode
                    showUpdateDialog = false
                    Log.d("HomeScreen", "Update dialog dismissed for code=${updateInfo!!.versionCode}")
                }
            }
        )
    }
    // 3순위: Announcement (공지사항)
    else if (showAnnouncementDialog && announcement != null) {
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

    // 네트워크 도움말 다이얼로그(보통/권장 UX)
    if (showNetworkHelpDialog) {
        AlertDialog(
            onDismissRequest = { /* 강제 업데이트 맥락에서도 닫기 버튼 제공하지 않음 */ },
            title = { Text("네트워크 문제") },
            text = {
                Text("인터넷에 연결되어 있지 않아 스토어를 열 수 없어요. 네트워크 설정을 확인한 뒤 다시 시도해 주세요.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showNetworkHelpDialog = false
                    // 네트워크 설정 화면 열기
                    runCatching { context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS)) }
                }) { Text("네트워크 설정 열기") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNetworkHelpDialog = false
                    updateInfo?.let { tryOpenStore(it) }
                }) { Text("다시 시도") }
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

fun isOnline(context: android.content.Context): Boolean {
    val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
           (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
}
