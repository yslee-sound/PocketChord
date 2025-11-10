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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.sweetapps.pocketchord.data.supabase.model.UpdatePolicy
import com.sweetapps.pocketchord.data.supabase.model.EmergencyPolicy
import com.sweetapps.pocketchord.data.supabase.model.NoticePolicy
import com.sweetapps.pocketchord.data.supabase.repository.UpdatePolicyRepository
import com.sweetapps.pocketchord.data.supabase.repository.EmergencyPolicyRepository
import com.sweetapps.pocketchord.data.supabase.repository.NoticePolicyRepository
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
    var emergencyPolicy by remember { mutableStateOf<EmergencyPolicy?>(null) }  // 긴급 정책 저장용

    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var announcement by remember { mutableStateOf<Announcement?>(null) }
    var currentNoticeVersion by remember { mutableStateOf<Int?>(null) }  // 현재 표시 중인 notice 버전
    val context = LocalContext.current
    val app = context.applicationContext as PocketChordApplication
    val supabaseClient = app.supabase
    val updatePrefs: SharedPreferences = remember { context.getSharedPreferences("update_prefs", android.content.Context.MODE_PRIVATE) }
    val dismissedVersionCode = remember { mutableStateOf(updatePrefs.getInt("dismissed_version_code", -1)) }
    val gson = remember { Gson() }
    var showNetworkHelpDialog by remember { mutableStateOf(false) }

    // 팝업 체크 완료 플래그 (화면 재구성 및 화면 전환 시에도 유지)
    // rememberSaveable 사용으로 화면을 벗어났다 돌아와도 플래그 유지
    val hasCheckedPopups = rememberSaveable { mutableStateOf(false) }

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
    // hasCheckedPopups 플래그로 화면 재구성 시 중복 실행 방지
    LaunchedEffect(Unit) {
        if (hasCheckedPopups.value) {
            Log.d("HomeScreen", "Popup check already completed, skipping...")
            return@LaunchedEffect
        }
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

            // ===== Phase 1: emergency_policy 조회 시도 (최우선순위!) =====
            android.util.Log.d("HomeScreen", "===== Phase 1: Checking emergency_policy =====")
            var emergency: EmergencyPolicy? = null
            EmergencyPolicyRepository(supabaseClient)
                .getActiveEmergency()
                .onSuccess { policy ->
                    emergency = policy
                    android.util.Log.d("HomeScreen", "✅ emergency_policy found: isDismissible=${policy?.isDismissible}")
                }
                .onFailure { e ->
                    android.util.Log.w("HomeScreen", "⚠️ emergency_policy not found or error: ${e.message}")
                }

            // emergency가 있으면 최우선 처리 (다른 팝업 무시)
            emergency?.let { ep ->
                Log.d("HomeScreen", "Decision: EMERGENCY from emergency_policy")
                emergencyPolicy = ep
                showEmergencyDialog = true
                // 강제 업데이트 캐시 정리
                if (storedForceVersion != -1) {
                    updatePrefs.edit {
                        remove("force_required_version")
                        remove("force_update_info")
                    }
                }
                return@LaunchedEffect  // 긴급 상황이면 다른 팝업 무시
            }

            // ===== Phase 2: update_policy 조회 시도 (신규) =====
            android.util.Log.d("HomeScreen", "===== Phase 2: Trying update_policy =====")
            var updatePolicy: UpdatePolicy? = null
            UpdatePolicyRepository(supabaseClient)
                .getPolicy()
                .onSuccess { policy ->
                    updatePolicy = policy
                    android.util.Log.d("HomeScreen", "✅ update_policy found: targetVersion=${policy?.targetVersionCode}, isForce=${policy?.isForceUpdate}")
                }
                .onFailure { e ->
                    android.util.Log.w("HomeScreen", "⚠️ update_policy not found or error: ${e.message}")
                }

            // update_policy가 있으면 우선 처리
            updatePolicy?.let { up ->
                val currentVersion = com.sweetapps.pocketchord.BuildConfig.VERSION_CODE

                // Phase 2.5: 시간 기반 재표시 로직을 위한 SharedPreferences 읽기
                val updatePrefsFile = context.getSharedPreferences("update_preferences", android.content.Context.MODE_PRIVATE)
                val dismissedTime = updatePrefsFile.getLong("update_dismissed_time", 0L)
                val laterCount = updatePrefsFile.getInt("update_later_count", 0)
                val dismissedVersion = updatePrefsFile.getInt("dismissedVersionCode", -1)
                val now = System.currentTimeMillis()

                // Phase 2.5: 초/분/시간 단위 우선순위 적용 (테스트 편의)
                val reshowIntervalMs = when {
                    // 1순위: 초 단위 (초고속 테스트용)
                    up.reshowIntervalSeconds != null -> {
                        up.reshowIntervalSeconds.toLong() * 1000L
                    }
                    // 2순위: 분 단위 (빠른 테스트용)
                    up.reshowIntervalMinutes != null -> {
                        up.reshowIntervalMinutes.toLong() * 60 * 1000L
                    }
                    // 3순위: 시간 단위 (운영 환경)
                    else -> {
                        (up.reshowIntervalHours ?: 24) * 60 * 60 * 1000L
                    }
                }

                val maxLaterCount = up.maxLaterCount ?: 3
                val elapsed = now - dismissedTime

                when {
                    up.requiresForceUpdate(currentVersion) -> {
                        Log.d("HomeScreen", "Decision: FORCE UPDATE from update_policy (target=${up.targetVersionCode})")
                        updateInfo = UpdateInfo(
                            id = null,
                            versionCode = up.targetVersionCode,
                            versionName = "",
                            appId = com.sweetapps.pocketchord.BuildConfig.SUPABASE_APP_ID,
                            isForce = true,
                            releaseNotes = up.releaseNotes ?: "새로운 업데이트가 있습니다.",
                            releasedAt = null,
                            downloadUrl = up.downloadUrl
                        )
                        showUpdateDialog = true
                        updatePrefs.edit {
                            putInt("force_required_version", updateInfo!!.versionCode)
                            putString("force_update_info", gson.toJson(updateInfo!!))
                        }
                        return@LaunchedEffect  // 강제 업데이트면 다른 팝업 무시
                    }

                    up.recommendsOptionalUpdate(currentVersion) -> {
                        // 매 시작마다 현재 laterCount 로그
                        Log.d("UpdateLater", "📊 Current later count: $laterCount / $maxLaterCount")

                        // Phase 2.5: 최대 횟수 체크 (최우선 - 시간 경과와 무관하게 체크)
                        if (laterCount >= maxLaterCount) {
                            Log.d("UpdateLater", "🚨 Later count ($laterCount) >= max ($maxLaterCount), forcing update mode")
                            updateInfo = UpdateInfo(
                                id = null,
                                versionCode = up.targetVersionCode,
                                versionName = "",
                                appId = com.sweetapps.pocketchord.BuildConfig.SUPABASE_APP_ID,
                                isForce = true,  // 강제로 전환
                                releaseNotes = up.releaseNotes ?: "새로운 업데이트가 있습니다.",
                                releasedAt = null,
                                downloadUrl = up.downloadUrl
                            )
                            showUpdateDialog = true
                            // 강제 업데이트 캐시 저장
                            updatePrefs.edit {
                                putInt("force_required_version", updateInfo!!.versionCode)
                                putString("force_update_info", gson.toJson(updateInfo!!))
                            }
                            return@LaunchedEffect
                        }

                        // Phase 2.5: 시간 경과 체크
                        if (dismissedTime > 0 && elapsed >= reshowIntervalMs) {
                            // 시간이 경과했으므로 재표시
                            val intervalMsg = when {
                                up.reshowIntervalSeconds != null -> "${up.reshowIntervalSeconds}s"
                                up.reshowIntervalMinutes != null -> "${up.reshowIntervalMinutes}min"
                                else -> "${up.reshowIntervalHours ?: 24}h"
                            }
                            Log.d("UpdateLater", "⏱️ Update interval elapsed (>= $intervalMsg), reshow allowed")

                            // 선택적 업데이트 표시
                            updateInfo = UpdateInfo(
                                id = null,
                                versionCode = up.targetVersionCode,
                                versionName = "",
                                appId = com.sweetapps.pocketchord.BuildConfig.SUPABASE_APP_ID,
                                isForce = false,
                                releaseNotes = up.releaseNotes ?: "새로운 업데이트가 있습니다.",
                                releasedAt = null,
                                downloadUrl = up.downloadUrl
                            )
                            showUpdateDialog = true

                            // laterCount는 "나중에" 클릭 시 증가 (여기서는 증가하지 않음)
                            return@LaunchedEffect
                        }

                        // 시간 미경과: 버전 체크
                        if (dismissedVersion == up.targetVersionCode) {
                            // 같은 버전을 이미 "나중에" 한 경우 - 시간이 지나지 않았으므로 스킵
                            Log.d("UpdateLater", "⏸️ Update dialog skipped (dismissed version: $dismissedVersion, target: ${up.targetVersionCode})")
                        } else {
                            // 첫 표시 또는 새 버전 (dismissed된 적 없거나 다른 버전)
                            Log.d("HomeScreen", "Decision: OPTIONAL UPDATE from update_policy (target=${up.targetVersionCode})")
                            updateInfo = UpdateInfo(
                                id = null,
                                versionCode = up.targetVersionCode,
                                versionName = "",
                                appId = com.sweetapps.pocketchord.BuildConfig.SUPABASE_APP_ID,
                                isForce = false,
                                releaseNotes = up.releaseNotes ?: "새로운 업데이트가 있습니다.",
                                releasedAt = null,
                                downloadUrl = up.downloadUrl
                            )
                            showUpdateDialog = true
                            return@LaunchedEffect
                        }
                    }

                    else -> {
                        Log.d("HomeScreen", "update_policy exists but no update needed (current=$currentVersion >= target=${up.targetVersionCode})")

                        // Phase 2.5: 버전 업데이트 완료 시 추적 데이터 초기화
                        val updatePrefsFile = context.getSharedPreferences("update_preferences", android.content.Context.MODE_PRIVATE)
                        if (updatePrefsFile.contains("update_dismissed_time") ||
                            updatePrefsFile.contains("update_later_count") ||
                            updatePrefsFile.contains("dismissedVersionCode")) {
                            Log.d("UpdateLater", "🧹 Clearing old update tracking data (version updated)")
                            updatePrefsFile.edit {
                                remove("update_dismissed_time")
                                remove("update_later_count")
                                remove("dismissedVersionCode")
                            }
                        }
                    }
                }
            }

            // ===== Phase 3: notice_policy 조회 시도 (우선순위 3) =====
            android.util.Log.d("HomeScreen", "===== Phase 3: Checking notice_policy =====")
            var notice: NoticePolicy? = null
            NoticePolicyRepository(supabaseClient)
                .getActiveNotice()
                .onSuccess { policy ->
                    notice = policy
                    android.util.Log.d("HomeScreen", "✅ notice_policy found: version=${policy?.noticeVersion}, title=${policy?.title}")
                }
                .onFailure { e ->
                    android.util.Log.w("HomeScreen", "⚠️ notice_policy not found or error: ${e.message}")
                }

            // notice가 있으면 버전 기반 추적 확인
            notice?.let { n ->
                // 버전 기반 추적
                val prefs = context.getSharedPreferences("notice_prefs", android.content.Context.MODE_PRIVATE)
                val viewedVersions = prefs.getStringSet("viewed_notices", setOf()) ?: setOf()
                val identifier = "notice_v${n.noticeVersion}"

                if (viewedVersions.contains(identifier)) {
                    Log.d("HomeScreen", "Notice already viewed (version=${n.noticeVersion}), skipping")
                } else {
                    Log.d("HomeScreen", "Decision: NOTICE from notice_policy (version=${n.noticeVersion})")

                    // notice 버전을 state에 저장 (onDismiss에서 사용)
                    currentNoticeVersion = n.noticeVersion

                    announcement = Announcement(
                        id = n.id,
                        createdAt = n.createdAt,
                        appId = n.appId,
                        title = n.title ?: "공지사항",
                        content = n.content,
                        isActive = true,
                        kind = "announcement",
                        redirectUrl = null,  // action_url 필드 제거됨
                        dismissible = true
                    )
                    showAnnouncementDialog = true
                }
            }
        } catch (e: Exception) {
            Log.e("HomeScreen", "Exception while loading policy", e)
        } finally {
            // 팝업 체크 완료 플래그 설정 (화면 재구성 시 중복 실행 방지)
            hasCheckedPopups.value = true
            Log.d("HomeScreen", "Popup check completed, flag set to true")
        }
    }

    // ==================== 팝업 표시 (우선순위: emergency > update > notice) ====================

    android.util.Log.d("HomeScreen", "===== Popup Display Check =====")
    android.util.Log.d("HomeScreen", "showEmergencyDialog: $showEmergencyDialog")
    android.util.Log.d("HomeScreen", "showUpdateDialog: $showUpdateDialog")
    android.util.Log.d("HomeScreen", "showAnnouncementDialog: $showAnnouncementDialog")

    // 1순위: Emergency - emergency_policy 사용 (Phase 2)
    if (showEmergencyDialog && emergencyPolicy != null) {
        android.util.Log.d("HomeScreen", "✅ Displaying EmergencyRedirectDialog from emergency_policy")
        com.sweetapps.pocketchord.ui.dialogs.EmergencyRedirectDialog(
            title = "🚨 긴급공지",
            description = emergencyPolicy!!.content,
            newAppPackage = "com.sweetapps.pocketchord",  // 기본값 (redirect_url이 있으면 무시됨)
            redirectUrl = emergencyPolicy!!.redirectUrl,
            buttonText = emergencyPolicy!!.buttonText,  // ← DB에서 제어! (NOT NULL)
            isDismissible = emergencyPolicy!!.isDismissible,  // ← DB에서 제어!
            onDismiss = if (emergencyPolicy!!.isDismissible) {
                { showEmergencyDialog = false }
            } else {
                { /* X 버튼 없음 */ }
            },
            badgeText = "긴급"
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
                    // Phase 2.5: 시간 기반 추적 정보 저장
                    val updatePrefsFile = context.getSharedPreferences("update_preferences", android.content.Context.MODE_PRIVATE)
                    val currentLaterCount = updatePrefsFile.getInt("update_later_count", 0)
                    val newLaterCount = currentLaterCount + 1

                    updatePrefsFile.edit {
                        putLong("update_dismissed_time", System.currentTimeMillis())
                        putInt("update_later_count", newLaterCount)
                        putInt("dismissedVersionCode", updateInfo!!.versionCode)
                    }

                    // 기존 호환성 유지
                    updatePrefs.edit {
                        putInt("dismissed_version_code", updateInfo!!.versionCode)
                    }
                    dismissedVersionCode.value = updateInfo!!.versionCode
                    showUpdateDialog = false
                    Log.d("UpdateLater", "✋ Update dialog dismissed for code=${updateInfo!!.versionCode}")
                    Log.d("UpdateLater", "⏱️ Tracking: laterCount=$currentLaterCount→$newLaterCount, timestamp=${System.currentTimeMillis()}")
                }
            }
        )
    }
    // 3순위: Announcement (공지사항)
    else if (showAnnouncementDialog && announcement != null) {
        AnnouncementDialog(
            announcement = announcement!!,
            onDismiss = {
                // ===== Phase 3: notice_policy 버전 저장 =====
                currentNoticeVersion?.let { version ->
                    val prefs = context.getSharedPreferences("notice_prefs", android.content.Context.MODE_PRIVATE)
                    val viewedVersions = prefs.getStringSet("viewed_notices", setOf())
                        ?.toMutableSet() ?: mutableSetOf()

                    val identifier = "notice_v${version}"
                    viewedVersions.add(identifier)

                    prefs.edit {
                        putStringSet("viewed_notices", viewedVersions)
                    }

                    Log.d("HomeScreen", "Marked notice version $version as viewed")
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
