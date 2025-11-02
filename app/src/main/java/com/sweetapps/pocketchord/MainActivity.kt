package com.sweetapps.pocketchord

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Color as AndroidColor
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.sweetapps.pocketchord.ui.theme.PocketChordTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.isFinite
import androidx.lifecycle.lifecycleScope
import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.layout.FlowRow
import android.media.ToneGenerator
import android.media.AudioManager
import kotlinx.coroutines.delay
import android.media.AudioTrack
import android.media.AudioAttributes
import android.media.AudioFormat
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.draw.scale
import androidx.core.content.edit

// Ads
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdSize
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import com.sweetapps.pocketchord.ads.InterstitialAdManager
import com.sweetapps.pocketchord.BuildConfig

class MainActivity : ComponentActivity() {
    // 전면광고 매니저
    private lateinit var interstitialAdManager: InterstitialAdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = AndroidColor.WHITE
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = true
        try {
            controller.isAppearanceLightNavigationBars = true
            window.navigationBarColor = AndroidColor.WHITE
        } catch (_: Exception) {}


        // 전면광고 매니저 초기화
        interstitialAdManager = InterstitialAdManager(this)

        setContent {
            PocketChordTheme {
                val navController = rememberNavController()
                // 아이콘 변경 시 즉시 하단바가 갱신되도록 버전 스테이트 보관
                var iconPrefsVersion by remember { mutableStateOf(0) }
                // 배너 광고 표시 여부 변경 시 즉시 반영되도록 버전 스테이트 보관
                var adPrefsVersion by remember { mutableStateOf(0) }
                // 긴급 안내 팝업 설정 변경 즉시 반영용 버전 스테이트
                var dialogPrefsVersion by remember { mutableStateOf(0) }
                val context = LocalContext.current
                val adPrefs = remember(adPrefsVersion) { context.getSharedPreferences("ads_prefs", android.content.Context.MODE_PRIVATE) }
                val isBannerEnabled = remember(adPrefsVersion) { adPrefs.getBoolean("banner_ads_enabled", true) }

                // 앱 오프닝 광고 표시 상태 관찰
                val app = context.applicationContext as PocketChordApplication
                val isShowingAppOpenAd by app.isShowingAppOpenAd.collectAsState()

                // 현재 라우트에 따라 상단 배너/하단바를 숨기기 위한 상태
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val isSplash = currentRoute == "splash"

                // 전면광고 표시를 위한 이전 라우트 추적
                var previousRoute by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(currentRoute) {
                    // 특정 화면에서 돌아올 때 전면광고 표시
                    if (previousRoute != null && currentRoute != null) {
                        // 코드 상세 화면에서 홈으로 돌아올 때
                        if (previousRoute?.startsWith("chord_list/") == true && currentRoute == "home") {
                            interstitialAdManager.tryShowAd(this@MainActivity)
                        }
                        // 메트로놈/튜너에서 홈으로 돌아올 때
                        else if ((previousRoute == "metronome" || previousRoute == "tuner") && currentRoute == "home") {
                            interstitialAdManager.tryShowAd(this@MainActivity)
                        }
                        // 설정 화면 진입 시
                        else if (previousRoute == "more" && currentRoute == "settings") {
                            interstitialAdManager.tryShowAd(this@MainActivity)
                        }
                    }
                    previousRoute = currentRoute
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (!isSplash) {
                            BottomNavigationBar(navController, prefsVersion = iconPrefsVersion)
                        }
                    },
                    containerColor = Color.White
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                            )
                    ) {

                        // 배너 광고 영역: 모든 화면에서 항상 고정 (스플래시 포함)
                        // 단, 앱 오프닝 광고가 표시 중일 때는 배너 광고 숨김 (광고 겹침 방지)
                        if (isSplash || isShowingAppOpenAd) {
                            // 스플래시 또는 앱 오프닝 광고 표시 중: 빈 공간만
                            TopBannerAdPlaceholder()
                        } else {
                            // 다른 화면: 배너 활성화 시 광고 표시
                            if (isBannerEnabled) {
                                TopBannerAd()
                            } else {
                                TopBannerAdPlaceholder()
                            }
                        }
                        NavHost(
                            navController = navController,
                            startDestination = "splash",
                            modifier = Modifier.weight(1f)
                        ) {
                            composable("splash") {
                                com.sweetapps.pocketchord.ui.screens.SplashScreen(
                                    logoResId = R.drawable.ic_logo_temp,
                                    appName = null,  // 로고만 표시
                                    tagline = null,  // 로고만 표시
                                    onSplashFinished = {
                                        navController.navigate("home") {
                                            popUpTo("splash") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                            composable("home") { Log.d("NavDebug", "Entered route: home"); MainScreen(navController) }
                            composable("metronome") { com.sweetapps.pocketchord.ui.screens.MetronomeProScreen() }
                            composable("tuner") { com.sweetapps.pocketchord.ui.screens.GuitarTunerScreen() }
                            composable("search") { Log.d("NavDebug", "Entered route: search"); SearchResultScreen() }
                            composable("search_chord") { Log.d("NavDebug", "Entered route: search_chord"); SearchChordScreen() }
                            composable(
                                route = "chord_list/{root}",
                                arguments = listOf(navArgument("root") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val encoded = backStackEntry.arguments?.getString("root")
                                val root = encoded?.let { Uri.decode(it) } ?: "C"
                                Log.d("NavDebug", "Entered route: chord_list/$root (encoded=$encoded)")
                                ChordListScreen(navController = navController, root = root, onBack = { navController.popBackStack() })
                            }
                            composable("more") { MoreScreen() }
                            composable("settings") {
                                BasicSettingsScreen(
                                    navController,
                                    onIconsChanged = { iconPrefsVersion++ },
                                    onAdsPrefChanged = { adPrefsVersion++ },
                                    onDialogPrefsChanged = { dialogPrefsVersion++ }
                                )
                            }
                            // 아이콘 선택 화면 라우트
                            composable("icon_picker") { IconPickerScreen(onPicked = { iconPrefsVersion++ }, onBack = { navController.popBackStack() }) }
                            // 라벨 편집 화면 라우트
                            composable("label_editor") { LabelEditorScreen(onChanged = { iconPrefsVersion++ }, onBack = { navController.popBackStack() }) }
                            // 강제 업데이트 다이얼로그 라우트
                            composable("force_update") {
                                com.sweetapps.pocketchord.ui.screens.ForceUpdateDialog(
                                    title = "앱 업데이트",
                                    description = "새로운 기능 추가, 더 빠른 속도, 버그 해결 등이 포함된 업데이트를 사용할 수 있습니다. 업데이트는 대부분 1분 내에 완료됩니다.",
                                    buttonText = "업데이트",
                                    features = listOf(
                                        "코드 라이브러리 최신화",
                                        "성능 향상 및 버그 수정",
                                        "UI 사용성 개선"
                                    ),
                                    estimatedTime = "약 1분",
                                    showCloseButton = false,
                                    onUpdateClick = { navController.popBackStack() }
                                )
                            }
                            // 선택적 업데이트 다이얼로그 라우트
                            // 앱에서 지금 보이는 문구를 바꾸고 싶다”면 가장 먼저 여기 값을 바꾸면 됩니다.
                            composable("optional_update") {
                                com.sweetapps.pocketchord.ui.screens.OptionalUpdateDialog(
                                    title = "새 버전 사용 가능",
                                    description = "더 나은 경험을 위해 최신 버전으로 업데이트하는 것을 권장합니다. 새로운 기능과 개선사항을 확인해보세요.",
                                    updateButtonText = "지금 업데이트",
                                    laterButtonText = "나중에",
                                    // features = listOf("안정성 향상", "작은 버그 수정"),
                                    version = null,
                                    onUpdateClick = { navController.popBackStack() },
                                    onLaterClick = { navController.popBackStack() }
                                )
                            }
                            // 긴급 전환 안내 팝업 라우트
                            composable("emergency_redirect") {
                                val ctx = LocalContext.current
                                val dialogPrefs = remember(dialogPrefsVersion) { ctx.getSharedPreferences("dialog_prefs", android.content.Context.MODE_PRIVATE) }
                                val allowDismiss = remember(dialogPrefsVersion) { dialogPrefs.getBoolean("emergency_dialog_dismissible", false) }
                                com.sweetapps.pocketchord.ui.screens.EmergencyRedirectDialog(
                                    title = "중요 안내",
                                    description = "현재 앱의 서비스가 종료되어 더 이상 사용할 수 없습니다. 새로운 앱을 설치하여 계속 이용해주세요.",
                                    newAppName = "PocketChord 2",
                                    newAppPackage = "com.sweetapps.pocketchord2",
                                    buttonText = "새 앱 설치하기",
                                    supportUrl = "https://example.com/faq",
                                    supportButtonText = "자세한 내용 보기",
                                    canMigrateData = true,
                                    isDismissible = allowDismiss,
                                    onDismiss = { navController.popBackStack() }
                                )
                            }
                            // 공지사항 다이얼로그 라우트
                            composable("notice") {
                                com.sweetapps.pocketchord.ui.screens.NoticeDialog(
                                    title = "새로운 기능 출시",
                                    description = "더욱 편리해진 새로운 기능을 만나보세요. 이번 업데이트에서는 사용자 경험을 개선하고 다양한 새로운 기능을 추가했습니다.",
                                    imageUrl = null,
                                    onDismiss = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            try { com.sweetapps.pocketchord.data.ensureOrReseedOnAppUpdate(this@MainActivity) } catch (_: Exception) {}
        }
    }
}

@Composable
fun TopBannerAd() {
    // Official AdMob test banner ad unit ID
    val testAdUnitId = "ca-app-pub-3940256099942544/6300978111"
    // Keep a reference to destroy AdView when disposed
    var adView by remember { mutableStateOf<com.google.android.gms.ads.AdView?>(null) }
    // 배너 광고의 표준 높이 (50dp)
    val bannerHeight = 50.dp

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 상태바와의 여백
        Spacer(modifier = Modifier.height(8.dp))

        // 배너 광고 영역 (흰색 배경)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bannerHeight)
                .background(Color.White)
        ) {
            AndroidView(
                factory = { context ->
                    com.google.android.gms.ads.AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        setAdUnitId(testAdUnitId)
                        loadAd(AdRequest.Builder().build())
                        adView = this
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bannerHeight)
            )
        }
        // 배너 광고 하단 구분선만
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = Color(0xFFE0E0E0)
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            try { adView?.destroy() } catch (_: Exception) {}
            adView = null
        }
    }
}

@Composable
fun TopBannerAdPlaceholder() {
    // 배너 광고가 없을 때 같은 크기의 빈 공간
    val bannerHeight = 50.dp
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 상태바와의 여백
        Spacer(modifier = Modifier.height(8.dp))

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(bannerHeight)
                .background(Color.White)
        )
        // 배너 광고 영역 아래 구분선 (광고 없을 때도 동일하게)
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = Color(0xFFE0E0E0)
        )
    }
}

@Composable
fun MainScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FB))
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        TopBar()
        Spacer(modifier = Modifier.height(24.dp))
        ChordGrid(navController)
    }
}

@Composable
fun TopBar() {
    // Branded card-like header similar to the mock
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Purple circular badge with play icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF6F4EF6), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "PocketChord",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = Color(0xFF1F2D3D)
            )
        }
    }
}

@Composable
fun ChordButton(chord: String, modifier: Modifier = Modifier) {
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

@Composable
fun ChordGrid(navController: NavHostController) {
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
                                Log.d("NavDebug", "Click: navigating to ${route} from grid (chord=${chord}, root=${root})")
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

@Composable
private fun FancyNavIcon(
    icon: ImageVector,
    selected: Boolean,
    contentDescription: String? = null
) {
    val unselectedTint = Color(0xFF9AA7B5)
    if (selected) {
        val shape = RoundedCornerShape(14.dp)
        val gradient = Brush.linearGradient(
            colors = listOf(Color(0xFF8A6CFF), Color(0xFF6F4EF6))
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(8.dp, shape = shape, clip = false)
                .clip(shape)
                .background(gradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = contentDescription, tint = Color.White)
        }
    } else {
        // Keep the same 40.dp footprint to prevent label position shifting
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = contentDescription, tint = unselectedTint)
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController, prefsVersion: Int = 0) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current
    val prefs = remember(prefsVersion) { context.getSharedPreferences("nav_icons", android.content.Context.MODE_PRIVATE) }
    val labelPrefs = remember(prefsVersion) { context.getSharedPreferences("nav_labels", android.content.Context.MODE_PRIVATE) }

    // 각 탭의 아이콘 후보 목록
    fun candidates(route: String): List<ImageVector> = when (route) {
        "home" -> listOf(Icons.Filled.MusicNote, Icons.Filled.Home, Icons.Filled.Star)
        "metronome" -> listOf(
            Icons.Filled.Alarm,
            Icons.Filled.Timer,
            Icons.Filled.AccessTime,
            Icons.Filled.Timelapse,
            Icons.Filled.AvTimer,
            Icons.Filled.HourglassEmpty,
            Icons.Filled.WatchLater,
            Icons.Filled.Speed
        )
        "tuner" -> listOf(Icons.Filled.Equalizer, Icons.Filled.Tune, Icons.Filled.GraphicEq)
        "more" -> listOf(Icons.Filled.MoreHoriz, Icons.Filled.Menu, Icons.Filled.Apps)
        "settings" -> listOf(Icons.Filled.Settings, Icons.Filled.Build, Icons.Filled.Tune)
        else -> listOf(Icons.Filled.Circle)
    }

    data class Item(val route: String, val label: String)
    val baseItems = listOf(
        Item("home", "코드"),
        Item("metronome", "메트로놈"),
        Item("tuner", "튜너"),
        Item("more", "더보기"),
        Item("settings", "설정")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
    ) {
        HorizontalDivider(color = Color(0x1A000000))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            baseItems.forEach { item ->
                val selected = currentRoute == item.route
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(targetValue = if (pressed) 0.96f else 1f, label = "nav_item_scale")

                val iconIdx = prefs.getInt(item.route, 0).coerceAtLeast(0)
                val iconList = candidates(item.route)
                val icon = iconList.getOrElse(iconIdx) { iconList.first() }
                val label = (labelPrefs.getString(item.route, item.label) ?: item.label).ifBlank { item.label }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .scale(scale)
                        .clickable(
                            interactionSource = interaction,
                            indication = null
                        ) {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                }
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    FancyNavIcon(icon = icon, selected = selected, contentDescription = label)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) Color(0xFF6F4EF6) else Color(0xFF9AA7B5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultScreen() {
    val chordName = "Cmaj7"
    val tags = listOf("C", "Major 7", "b5")
    val codeCards = listOf(
        FretDiagramData("TYARRRI", listOf(1, 3, 5, 7, 8, 0), fingers = listOf(0,2,3,0,1,0)),
        FretDiagramData("AOTTTOURAL", listOf(2, 4, 6, 8, 10, 0), fingers = listOf(0,1,3,4,0,0))
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로가기",
                tint = Color(0xFF31455A),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "검색 결과: $chordName",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF31455A)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tags.forEach {
                AssistChip(
                    onClick = {},
                    label = { Text(it, color = Color(0xFF31455A), fontWeight = FontWeight.Bold) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFFE3F0FF)
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 코드 카드 UI는 제거됨. 간단히 코드 이름과 다이어그램 링크(예시)를 나열합니다.
            codeCards.forEach { data ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = data.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    // 간단한 소형 다이어그램을 우측에 표시
                    FretDiagramImage()
                }
            }
        }
    }
}

@Composable
fun SearchChordScreen() {
    val types = listOf("Major", "minor", "dim", "aug")
    val tensions = listOf("7", "M7", "6", "sus4", "9", "11", "13")
    val options = listOf("add9", "b5", "#5", "b9")
    val selectedRoot = "C"
    val selectedType = "Major"
    val selectedTension = "M7"
    val selectedOptions = listOf("add9")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 상단 바
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* TODO: 뒤로가기 */ }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기", tint = Color(0xFF31455A))
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("코드 검색", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF31455A))
            Spacer(modifier = Modifier.weight(1f))
            Text("Reset", color = Color(0xFF00C6A2), fontWeight = FontWeight.Bold, modifier = Modifier.clickable { /* TODO: 리셋 */ })
        }
        Spacer(modifier = Modifier.height(16.dp))
        // 코드명 및 설명
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Cmaj7(add9)", fontWeight = FontWeight.Bold, fontSize = 32.sp, color = Color(0xFF31455A))
            Spacer(modifier = Modifier.height(8.dp))
            Text("아래 버튼을 눌러 코드를 조합해 보세요.", fontSize = 16.sp, color = Color(0xFF8CA0B3))
        }
        Spacer(modifier = Modifier.height(24.dp))
        // 루트 노트
        SectionTitle("루트 노트 (Root Note)")
        OutlinedButton(
            onClick = {},
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF00C6A2), contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(48.dp).padding(vertical = 8.dp, horizontal = 8.dp)
        ) {
            Text(selectedRoot, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        // 타입
        SectionTitle("타입 (Type)")
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            types.forEach {
                OutlinedButton(
                    onClick = {},
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (it == selectedType) Color(0xFF00C6A2) else Color.White,
                        contentColor = if (it == selectedType) Color.White else Color(0xFF31455A)
                    ),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Text(it, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // 텐션
        SectionTitle("텐션 (Tensions)")
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tensions.forEach {
                OutlinedButton(
                    onClick = {},
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (it == selectedTension) Color(0xFF00C6A2) else Color.White,
                        contentColor = if (it == selectedTension) Color.White else Color(0xFF31455A)
                    ),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Text(it, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // 추가 옵션
        SectionTitle("추가 옵션 (Options)")
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach {
                OutlinedButton(
                    onClick = {},
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selectedOptions.contains(it)) Color(0xFF00C6A2) else Color.White,
                        contentColor = if (selectedOptions.contains(it)) Color.White else Color(0xFF31455A)
                    ),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Text(it, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        // 하단 코드 찾기 버튼
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            Button(
                onClick = { /* TODO: 코드 찾기 */ },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA800)),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("코드 찾기", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF31455A), modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
}

// 코드 다이어그램 데이터 예시
// positions: stringCount 길이, -1=뮤트, 0=open, >0=프렛번호
// fingers: 동일 길이의 리스트, 0=표시안함, >0=핑거링 숫자
data class FretDiagramData(val name: String, val positions: List<Int>, val fingers: List<Int>? = null)

// helper: parse CSV like "-1,3,2,0,1,0" into internal positions List<Int> index0=lowest string
fun parseCsvToPositions(csv: String): List<Int> {
    // Parse CSV into ints and return as-is. Storage order in DB/seed is 6→1 and UI expects the same.
    return csv.split(",").mapNotNull { it.trim().toIntOrNull() }
}

// helper: parse barresJson (seed/DB) into ExplicitBarre list
fun parseBarresJson(json: String?): List<ExplicitBarre>? {
    if (json.isNullOrBlank()) return null
    return try {
        val arr = org.json.JSONArray(json)
        val list = mutableListOf<ExplicitBarre>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val fret = o.optInt("fret", 0)
            val finger = o.optInt("finger", 0)
            var fromS = o.optInt("fromString", 0)
            var toS = o.optInt("toString", 0)
            if (fret > 0 && finger > 0 && fromS in 1..6 && toS in 1..6) {
                if (fromS > toS) { val tmp = fromS; fromS = toS; toS = tmp }
                list.add(ExplicitBarre(fret, finger, fromS, toS))
            }
        }
        if (list.isEmpty()) null else list
    } catch (_: Throwable) { null }
}

// Build grouped sections for a given root according to requested order
private data class ChordSection(val title: String, val items: List<com.sweetapps.pocketchord.data.ChordWithVariants>)
private fun buildChordSectionsForRoot(root: String, all: List<com.sweetapps.pocketchord.data.ChordWithVariants>): List<ChordSection> {
    // Templates expressed for root C; replace leading 'C' with the actual root
    val groups = listOf(
        "Major" to listOf("C", "Cadd9", "CM7", "CM7(9)", "C6", "C6(9)"),
        "minor" to listOf("Cm", "Cm7", "Cm7(9)", "Cm7(11)", "Cm6", "CmM7", "Cm7(b5)", "Cdim7"),
        "7th" to listOf("C7", "C7(9)", "C7(13)", "C7(b5)", "Csus4", "C7sus4", "Caug", "C7aug")
    )
    fun toName(template: String): String = if (template.startsWith("C")) root + template.removePrefix("C") else template
    val byName = all.associateBy { it.chord.name }
    val used = mutableSetOf<Long>()
    val sections = mutableListOf<ChordSection>()
    for ((title, templates) in groups) {
        val names = templates.map(::toName)
        val items = names.mapNotNull { nm ->
            val m = byName[nm]
            if (m != null) {
                used.add(m.chord.id); m
            } else null
        }
        if (items.isNotEmpty()) sections.add(ChordSection(title, items))
    }
    // Remaining chords not covered above → 기타(이름순)
    val others = all.filter { used.contains(it.chord.id).not() }.sortedBy { it.chord.name }
    if (others.isNotEmpty()) sections.add(ChordSection("기타", others))
    return sections
}

@Composable
fun ChordListScreen(
    navController: NavHostController,
    root: String,
    onBack: () -> Unit = {},
    uiParams: DiagramUiParams = defaultDiagramUiParams()
) {
    val context = LocalContext.current
    val db = com.sweetapps.pocketchord.data.AppDatabase.getInstance(context)
    val chordFlow = db.chordDao().getChordsByRoot(root)
    val chordWithVariants by chordFlow.collectAsState(initial = emptyList())
    var isSeeding by remember { mutableStateOf(false) }

    val perRootAsset = remember(root) { com.sweetapps.pocketchord.data.seedAssetFileNameForRoot(root) }

    // ensure missing variants for this root (insert-only)
    LaunchedEffect(root) {
        try {
            isSeeding = true
            // 1) per-root 파일 우선 시딩 (프록시 형식 지원)
            com.sweetapps.pocketchord.data.ensureChordsForRoot(context, root, perRootAsset)
            // 2) 안전 폴백: 통합 파일에서도 시딩 (중복은 내부에서 방지)
            com.sweetapps.pocketchord.data.ensureChordsForRoot(context, root)
        } catch (t: Throwable) {
            android.util.Log.w("ChordListScreen", "ensureChordsForRoot failed", t)
        } finally {
            isSeeding = false
        }
    }

    // 필터 상태: 단일 선택, null이면 전체 표시
    var selectedFilter by remember { mutableStateOf<String?>(null) }
    val availableFilters = listOf("Major", "minor", "7th")

    // 섹션 구성 및 필터 적용
    val allSections = remember(chordWithVariants, root) { buildChordSectionsForRoot(root, chordWithVariants) }
    val sectionsToShow = remember(allSections, selectedFilter) {
        if (selectedFilter == null) allSections else allSections.filter { it.title == selectedFilter }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)
    ) {
        if (isSeeding) {
            // simple loader overlay
            Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        // Top app bar with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기", tint = Color(0xFF31455A))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "$root 코드", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF31455A))
        }
        // visual separation between top title and content — darker gray for better contrast
        HorizontalDivider(color = Color(0xFFBDBDBD), thickness = 1.dp)

        // 필터 칩(단일 선택 / 토글 해제)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, top = 8.dp)
        ) {
            availableFilters.forEach { f ->
                val selected = selectedFilter == f
                FilterChip(
                    selected = selected,
                    onClick = { selectedFilter = if (selected) null else f },
                    label = {
                        Text(
                            text = f,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    trailingIcon = if (selected) {
                        { Icon(Icons.Filled.Close, contentDescription = "해제", tint = Color.White) }
                    } else null,
                    shape = RoundedCornerShape(22.dp),
                    border = null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFFF1F3F4),
                        labelColor = Color(0xFF1F1F1F),
                        selectedContainerColor = Color(0xFF1F1F1F),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier
                        .height(40.dp)
                        .padding(end = 10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        // 총 개수(필터 적용 반영)
        val totalVariants = sectionsToShow.sumOf { section -> section.items.sumOf { it.variants.size } }
        Text(text = "총 ${totalVariants}개", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(start = 16.dp))

        Spacer(modifier = Modifier.height(8.dp))

        // 섹션별 리스트 (필터 반영)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(DEFAULT_LIST_ITEM_SPACING_DP)
        ) {
            sectionsToShow.forEach { section ->
                item(key = "header_${section.title}") {
                    Text(
                        text = section.title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color(0xFF31455A),
                        modifier = Modifier.padding(start = 16.dp, bottom = 6.dp, top = 8.dp)
                    )
                }
                // Show every variant as an individual row so chords with multiple shapes (e.g., C) appear multiple times
                items(
                    items = section.items.flatMap { cwv -> cwv.variants.map { v -> cwv to v } },
                    key = { pair -> pair.second.id }
                ) { pair ->
                    val cwv = pair.first
                    val variant = pair.second
                    val chordName = cwv.chord.name
                    val positions = variant.positionsCsv.let { parseCsvToPositions(it) }
                    val fingers = variant.fingersCsv?.let { parseCsvToPositions(it) } ?: List(6) { 0 }
                    // debug log
                    try {
                        Log.d("ChordDiag", "chord=${chordName} csvPositions=${variant.positionsCsv} parsedPositions=${positions} csvFingers=${variant.fingersCsv} parsedFingers=${fingers}")
                    } catch (_: Exception) {}

                    val desiredDiagramWidth = uiParams.diagramMaxWidthDp ?: 220.dp
                    val diagramHeightForList = uiParams.diagramHeightDp ?: uiParams.diagramMinHeightDp
                    val itemHeight = maxOf(uiParams.nameBoxSizeDp, diagramHeightForList)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        if (uiParams.diagramAnchor == DiagramAnchor.Left) {
                            Box(modifier = Modifier.width(desiredDiagramWidth).height(diagramHeightForList)) {
                                val explicitBarres = parseBarresJson(variant.barresJson)
                                FretboardDiagramOnly(
                                    modifier = Modifier.fillMaxSize(),
                                    uiParams = uiParams,
                                    positions = positions,
                                    fingers = fingers,
                                    firstFretIsNut = variant.firstFretIsNut,
                                    invertStrings = false,
                                    explicitBarres = explicitBarres
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Box(modifier = Modifier.size(uiParams.nameBoxSizeDp).background(DEFAULT_NAME_BOX_COLOR, shape = RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                val chordFontSize = (uiParams.nameBoxSizeDp.value * uiParams.nameBoxFontScale).sp
                                Text(text = chordName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = chordFontSize)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            Box(modifier = Modifier.size(uiParams.nameBoxSizeDp).background(DEFAULT_NAME_BOX_COLOR, shape = RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                val chordFontSize = (uiParams.nameBoxSizeDp.value * uiParams.nameBoxFontScale).sp
                                Text(text = chordName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = chordFontSize)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Box(modifier = Modifier.width(desiredDiagramWidth).height(diagramHeightForList)) {
                                val explicitBarres = parseBarresJson(variant.barresJson)
                                FretboardDiagramOnly(
                                    modifier = Modifier.fillMaxSize(),
                                    uiParams = uiParams,
                                    positions = positions,
                                    fingers = fingers,
                                    firstFretIsNut = variant.firstFretIsNut,
                                    invertStrings = false,
                                    explicitBarres = explicitBarres
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
         }
     }
}

@Composable
fun FretboardCard(
    chordName: String,
    modifier: Modifier = Modifier,
    uiParams: DiagramUiParams = defaultDiagramUiParams(), // centralized UI params
    fretLabelProvider: ((Int) -> String?)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp), // 카드 외부 여백 약간 증가
        shape = RoundedCornerShape(6.dp), // 더 둥글게
        colors = CardDefaults.cardColors(containerColor = DEFAULT_CARD_BACKGROUND_COLOR),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        // 카드 높이에 따라 다이어그램 크기를 계산하려면 BoxWithConstraints를 사용
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // BoxWithConstraints 범위에서 카드/다이어그램 크기 계산
            val measuredCardHeight = this.maxHeight
            val defaultCardHeight = 140.dp
            // Use uiParams.cardHeightDp as authoritative when provided so Preview and runtime compute identically
            val effectiveCardHeight = uiParams.cardHeightDp ?: if (measuredCardHeight.isFinite) measuredCardHeight else defaultCardHeight
            // allow fixed diagram height via uiParams; otherwise derive from card height but respect min height
            val diagramHeight = uiParams.diagramHeightDp ?: (effectiveCardHeight - 20.dp).coerceAtLeast(uiParams.diagramMinHeightDp)

            // 카드 내부 상단에 여백을 위한 Spacer 추가
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(5.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(effectiveCardHeight)
                        .padding(start = 8.dp, top = 8.dp, end = 0.dp, bottom = 8.dp),
                     verticalAlignment = Alignment.Top
                  ) {
                    // collect sample DB-style positions & fingers
                    val dbPositionsForC = mapOf(1 to 0, 2 to 1, 3 to 0, 4 to 2, 5 to 3, 6 to -1)
                    val dbFingersForC = mapOf(1 to 0, 2 to 1, 3 to 0, 4 to 2, 5 to 3, 6 to 0)
                    val positionsForC = dbMapToInternalPositions(dbPositionsForC, stringCount = 6, defaultFret = -1)
                    val fingersForC = dbMapToInternalPositions(dbFingersForC, stringCount = 6, defaultFret = 0)

                    // reserved space used to compute total inner width (text area + gap). Do not include Row padding here.
                    val textAreaWidth = 32.dp
                    val gapBetween = 8.dp
                    val reserved = textAreaWidth + gapBetween

                    // compute desired (diagram) width from height-based calculation and configured max
                    val heightBasedWidth = measuredCardHeight * (140f / 96f)
                    val desiredWidth = uiParams.diagramMaxWidthDp?.let { mw -> if (heightBasedWidth > mw) mw else heightBasedWidth } ?: heightBasedWidth

                    // total inner width that contains text + gap + diagram + right inset
                    val innerTotal = reserved + desiredWidth + uiParams.diagramRightInsetDp

                    // Align the fixed-width inner row to the card's end so the trailing inset becomes the gap to the card edge
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Row(modifier = Modifier.width(innerTotal).height(effectiveCardHeight), verticalAlignment = Alignment.Top) {
                            // chord name (fixed width)
                            Text(text = chordName, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Color.Black, modifier = Modifier.width(textAreaWidth).padding(start = 4.dp))
                            Spacer(modifier = Modifier.width(gapBetween))
                            Box(modifier = Modifier.width(desiredWidth).height(diagramHeight)) {
                                if (chordName == "C") {
                                    FretboardDiagramOnly(modifier = Modifier.fillMaxSize(), uiParams = uiParams, positions = positionsForC, fingers = fingersForC, firstFretIsNut = true, fretLabelProvider = fretLabelProvider, invertStrings = false)
                                } else {
                                    // default empty/mute positions for non-sample chords in preview
                                    val defaultPositions = List(6) { -1 }
                                    val defaultFingers = List(6) { 0 }
                                    FretboardDiagramOnly(modifier = Modifier.fillMaxSize(), uiParams = uiParams, positions = defaultPositions, fingers = defaultFingers, firstFretIsNut = true, invertStrings = false)
                                }
                            }
                            Spacer(modifier = Modifier.width(uiParams.diagramRightInsetDp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BasicSettingsScreen(navController: NavHostController, onIconsChanged: () -> Unit, onAdsPrefChanged: () -> Unit, onDialogPrefsChanged: () -> Unit) {
    val context = LocalContext.current
    val adPrefs = remember { context.getSharedPreferences("ads_prefs", android.content.Context.MODE_PRIVATE) }
    var isBannerEnabled by remember { mutableStateOf(adPrefs.getBoolean("banner_ads_enabled", true)) }
    var isInterstitialEnabled by remember { mutableStateOf(adPrefs.getBoolean("interstitial_ads_enabled", true)) }
    var isAppOpenTestMode by remember { mutableStateOf(adPrefs.getBoolean("app_open_test_mode", false)) }
    // 긴급 안내 팝업 X 버튼 허용 토글용 프리퍼런스/상태
    val dialogPrefs = remember { context.getSharedPreferences("dialog_prefs", android.content.Context.MODE_PRIVATE) }
    var allowEmergencyDismiss by remember { mutableStateOf(dialogPrefs.getBoolean("emergency_dialog_dismissible", false)) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "설정", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "앱 업데이트 시 코드 데이터가 자동으로 동기화됩니다.")
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { navController.navigate("icon_picker") }) {
                Icon(Icons.Filled.Brush, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("하단 아이콘 바꾸기")
            }
            OutlinedButton(onClick = { navController.navigate("label_editor") }) {
                Icon(Icons.Filled.Edit, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("탭 라벨 바꾸기")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // 강제 업데이트 화면 보기 버튼
        OutlinedButton(onClick = { navController.navigate("force_update") }) {
            Icon(Icons.Filled.SystemUpdate, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("강제 업데이트 보기")
        }
        Spacer(modifier = Modifier.height(8.dp))
        // 선택적 업데이트 팝업 보기 버튼
        OutlinedButton(onClick = { navController.navigate("optional_update") }) {
            Icon(Icons.Filled.SystemUpdateAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("선택적 업데이트 보기")
        }
        Spacer(modifier = Modifier.height(8.dp))
        // 긴급 전환 안내 팝업 보기 + X 버튼 허용 스위치(옆에 배치)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { navController.navigate("emergency_redirect") }) {
                Icon(Icons.Filled.Warning, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("긴급 전환 안내 보기")
            }
            Spacer(Modifier.width(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("X 버튼 허용", modifier = Modifier.padding(end = 6.dp))
                Switch(
                    checked = allowEmergencyDismiss,
                    onCheckedChange = { ch ->
                        dialogPrefs.edit { putBoolean("emergency_dialog_dismissible", ch) }
                        allowEmergencyDismiss = ch
                        onDialogPrefsChanged()
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // 공지사항 팝업 보기 버튼
        OutlinedButton(onClick = { navController.navigate("notice") }) {
            Icon(Icons.Filled.Info, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("공지사항 보기")
        }

        // 개발 도구 (디버깅 목적) - 배너/전면 광고 테스트
        if (BuildConfig.DEBUG) {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "🛠️ 개발 도구", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6B35))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "디버그 빌드에서만 표시됩니다",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFF6B35)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 배너 광고 토글 (디버그 전용)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "배너 광고 테스트")
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (isBannerEnabled) "활성화됨" else "비활성화됨",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7C8C)
                    )
                }
                Switch(
                    checked = isBannerEnabled,
                    onCheckedChange = { checked ->
                        adPrefs.edit { putBoolean("banner_ads_enabled", checked) }
                        isBannerEnabled = checked
                        onAdsPrefChanged()
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 전면 광고 토글 (디버그 전용)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "전면 광고 로그")
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (isInterstitialEnabled) "로그 활성화" else "로그 비활성화",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7C8C)
                    )
                }
                Switch(
                    checked = isInterstitialEnabled,
                    onCheckedChange = { checked ->
                        adPrefs.edit { putBoolean("interstitial_ads_enabled", checked) }
                        isInterstitialEnabled = checked
                        onAdsPrefChanged()
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 앱 오프닝 광고 테스트 모드 토글 (디버그 전용)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "앱 오프닝 광고 테스트")
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (isAppOpenTestMode) "항상 표시 (정책 무시)" else "정책에 맞춰 표시",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7C8C)
                    )
                }
                Switch(
                    checked = isAppOpenTestMode,
                    onCheckedChange = { checked ->
                        adPrefs.edit { putBoolean("app_open_test_mode", checked) }
                        isAppOpenTestMode = checked
                        onAdsPrefChanged()
                    }
                )
            }
        }
    }
}

// 간단한 더보기 화면 복원
@Composable
fun MoreScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(text = "더보기", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF31455A))
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "추가 메뉴가 여기에 표시됩니다.")
    }
}

// 아이콘 선택 화면: 각 탭별 후보 아이콘을 미리보기로 보여주고 선택값을 SharedPreferences에 저장
@Composable
fun IconPickerScreen(onPicked: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nav_icons", android.content.Context.MODE_PRIVATE) }

    data class Item(val route: String, val label: String)
    val entries = listOf(
        Item("home", "코드"),
        Item("metronome", "메트로놈"),
        Item("tuner", "튜너"),
        Item("more", "더보기"),
        Item("settings", "설정")
    )

    fun candidates(route: String): List<ImageVector> = when (route) {
        "home" -> listOf(Icons.Filled.MusicNote, Icons.Filled.Home, Icons.Filled.Star)
        "metronome" -> listOf(
            Icons.Filled.Alarm,
            Icons.Filled.Timer,
            Icons.Filled.AccessTime,
            Icons.Filled.Timelapse,
            Icons.Filled.AvTimer,
            Icons.Filled.HourglassEmpty,
            Icons.Filled.WatchLater,
            Icons.Filled.Speed
        )
        "tuner" -> listOf(Icons.Filled.Equalizer, Icons.Filled.Tune, Icons.Filled.GraphicEq)
        "more" -> listOf(Icons.Filled.MoreHoriz, Icons.Filled.Menu, Icons.Filled.Apps)
        "settings" -> listOf(Icons.Filled.Settings, Icons.Filled.Build, Icons.Filled.Tune)
        else -> listOf(Icons.Filled.Circle)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기") }
            Spacer(Modifier.width(8.dp))
            Text("하단 아이콘 선택", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        HorizontalDivider(color = Color(0x1A000000))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(entries) { item ->
                val current = prefs.getInt(item.route, 0)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(item.label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color(0xFF31455A))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        candidates(item.route).forEachIndexed { idx, icon ->
                            val selected = current == idx
                            val shape = RoundedCornerShape(14.dp)
                            val gradient = if (selected) Brush.linearGradient(listOf(Color(0xFF8A6CFF), Color(0xFF6F4EF6))) else null
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(shape)
                                    .then(if (gradient != null) Modifier.background(gradient) else Modifier.background(Color(0xFFF1F3F4)))
                                    .border(width = if (selected) 0.dp else 1.dp, color = Color(0x22000000), shape = shape)
                                    .clickable {
                                        prefs.edit { putInt(item.route, idx) }
                                        onPicked()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = if (selected) Color.White else Color(0xFF5A6B7C))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LabelEditorScreen(onChanged: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nav_labels", android.content.Context.MODE_PRIVATE) }
    data class Item(val route: String, val defaultLabel: String)
    val entries = listOf(
        Item("home", "코드"),
        Item("metronome", "메트로놈"),
        Item("tuner", "튜너"),
        Item("more", "더보기"),
        Item("settings", "설정")
    )

    // 편집용 상태: prefs의 현재값을 초기값으로 사용
    val states = remember {
        entries.associate { it.route to mutableStateOf(prefs.getString(it.route, it.defaultLabel) ?: it.defaultLabel) }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기") }
            Spacer(Modifier.width(8.dp))
            Text("탭 라벨 편집", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        HorizontalDivider(color = Color(0x1A000000))

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(entries) { item ->
                val textState = states[item.route] ?: remember { mutableStateOf(item.defaultLabel) }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(item.defaultLabel, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF6B7C8C))
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = textState.value,
                            onValueChange = { textState.value = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(item.defaultLabel) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            prefs.edit { putString(item.route, textState.value) }
                            onChanged()
                        }) {
                            Icon(Icons.Filled.Save, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("저장")
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = {
                            prefs.edit { remove(item.route) }
                            textState.value = item.defaultLabel
                            onChanged()
                        }) {
                            Text("기본값")
                        }
                    }
                }
            }
        }
    }
}
