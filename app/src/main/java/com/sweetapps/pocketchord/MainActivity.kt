package com.sweetapps.pocketchord

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.painterResource
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Ensure status bar background is white and icons are dark for readability at runtime
        // Use WindowCompat/WindowInsetsControllerCompat for backward-compatible control
        window.statusBarColor = AndroidColor.WHITE
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = true
        // Optionally make navigation bar light as well
        try {
            controller.isAppearanceLightNavigationBars = true
            window.navigationBarColor = AndroidColor.WHITE
        } catch (_: Exception) {
            // ignore on older platforms where this may not be supported
        }
        setContent {
            PocketChordTheme {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = { BottomNavigationBar(navController) },
                    containerColor = Color.White
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            Log.d("NavDebug", "Entered route: home")
                            MainScreen(navController)
                        }
                        composable("metronome") { com.sweetapps.pocketchord.ui.screens.MetronomeProScreen() }
                        composable("tuner") { com.sweetapps.pocketchord.ui.screens.GuitarTunerScreen() }
                        composable("search") {
                            Log.d("NavDebug", "Entered route: search")
                            SearchResultScreen()
                        }
                        composable("search_chord") {
                            Log.d("NavDebug", "Entered route: search_chord")
                            SearchChordScreen()
                        }
                        // Declare argument type explicitly; we'll decode on read
                        composable(
                            route = "chord_list/{root}",
                            arguments = listOf(navArgument("root") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val encoded = backStackEntry.arguments?.getString("root")
                            val root = encoded?.let { Uri.decode(it) } ?: "C"
                            Log.d("NavDebug", "Entered route: chord_list/$root (encoded=$encoded)")
                            ChordListScreen(navController = navController, root = root, onBack = { navController.popBackStack() })
                        }
                        // Optional settings screen remains, but without any seeding controls
                        composable("settings") { BasicSettingsScreen() }
                     }
                 }
             }
         }

        // On-app-update reseed only
        lifecycleScope.launchWhenCreated {
             try { com.sweetapps.pocketchord.data.ensureOrReseedOnAppUpdate(this@MainActivity) } catch (_: Exception) {}
         }
     }
}

@Composable
fun MainScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        TopBar()
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "코드를 선택하세요",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF31455A)
        )
        Spacer(modifier = Modifier.height(32.dp))
        ChordGrid(navController)
    }
}

@Composable
fun TopBar() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_media_play), // 임시 아이콘
            contentDescription = null,
            tint = Color(0xFF31455A),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "PocketChord",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Color(0xFF31455A)
        )
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
fun ChordButton(chord: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color.White, RoundedCornerShape(20.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = chord,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Color(0xFF31455A)
        )
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(containerColor = Color.Transparent) {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "홈") },
            label = { Text("홈") },
            selected = currentRoute == "home",
            onClick = { navController.navigate("home") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Alarm, contentDescription = "메트로놈") },
            label = { Text("메트로놈") },
            selected = currentRoute == "metronome",
            onClick = { navController.navigate("metronome") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Equalizer, contentDescription = "튜너") },
            label = { Text("튜너") },
            selected = currentRoute == "tuner",
            onClick = { navController.navigate("tuner") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Star, contentDescription = "즐겨찾기") },
            label = { Text("즐겨찾기") },
            selected = currentRoute == "favorites", // 아직 미구현
            onClick = { /* TODO: 즐겨찾기 구현 */ }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Settings, contentDescription = "설정") },
            label = { Text("설정") },
            selected = currentRoute == "settings",
            onClick = { navController.navigate("settings") }
        )
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
fun BasicSettingsScreen() {
    // Minimal settings placeholder — removed manual seeding and toggles
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "설정", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "앱 업데이트 시 코드 데이터가 자동으로 동기화됩니다.")
    }
}

// --- New screens ---
@Composable
fun MetronomeScreen() {
    var bpm by remember { mutableStateOf(100f) }
    var playing by remember { mutableStateOf(false) }
    var beatInBar by remember { mutableStateOf(4) }
    var currentBeat by remember { mutableStateOf(1) }

    // Tone generator for click sounds
    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }
    DisposableEffect(Unit) {
        onDispose { try { toneGen.release() } catch (_: Throwable) {} }
    }

    LaunchedEffect(playing, bpm, beatInBar) {
        currentBeat = 1
        if (playing) {
            val intervalMs = (60_000f / bpm).toLong().coerceAtLeast(40L)
            while (playing) {
                // accent first beat
                val tone = if (currentBeat == 1) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP
                try { toneGen.startTone(tone, 60) } catch (_: Throwable) {}
                currentBeat = if (currentBeat >= beatInBar) 1 else currentBeat + 1
                delay(intervalMs)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("메트로놈", fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(16.dp))
        // Beat indicators
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(beatInBar) { idx ->
                val active = playing && (idx + 1) == currentBeat
                Box(modifier = Modifier.size(18.dp).background(if (active) Color(0xFFFF6F00) else Color(0xFFB0BEC5), RoundedCornerShape(50)))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("BPM: ${bpm.toInt()}")
        Slider(
            value = bpm,
            onValueChange = { bpm = it.coerceIn(40f, 240f) },
            valueRange = 40f..240f
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { bpm = (bpm - 1).coerceAtLeast(40f) }) { Text("-1") }
            OutlinedButton(onClick = { bpm = (bpm + 1).coerceAtMost(240f) }) { Text("+1") }
            OutlinedButton(onClick = { bpm = (bpm - 5).coerceAtLeast(40f) }) { Text("-5") }
            OutlinedButton(onClick = { bpm = (bpm + 5).coerceAtMost(240f) }) { Text("+5") }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("박자 수: ")
            Row {
                listOf(2,3,4,6).forEach { n ->
                    FilterChip(
                        selected = beatInBar == n,
                        onClick = { beatInBar = n; currentBeat = 1 },
                        label = { Text("${n}") },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = { playing = !playing }, colors = ButtonDefaults.buttonColors(
            containerColor = if (playing) Color(0xFFD32F2F) else Color(0xFF00C853)
        )) {
            Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(if (playing) "정지" else "시작")
        }
    }
}

@Composable
fun TunerScreen() {
    // Standard guitar open-string reference tones
    val strings = listOf(
        "E(6) 82.41Hz" to 82.41,
        "A(5) 110Hz" to 110.0,
        "D(4) 146.83Hz" to 146.83,
        "G(3) 196.00Hz" to 196.0,
        "B(2) 246.94Hz" to 246.94,
        "E(1) 329.63Hz" to 329.63
    )
    var selected by remember { mutableStateOf(strings[5]) }
    var track by remember { mutableStateOf<AudioTrack?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    fun stop() {
        try { track?.stop() } catch (_: Throwable) {}
        try { track?.release() } catch (_: Throwable) {}
        track = null
        isPlaying = false
    }

    DisposableEffect(Unit) { onDispose { stop() } }

    fun createLoopingTone(freqHz: Double): AudioTrack? {
        val sampleRate = 44_100
        val durationSec = 1.0 // short but looped
        val frameCount = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(frameCount) { i ->
            val t = i / sampleRate.toDouble()
            val v = kotlin.math.sin(2.0 * Math.PI * freqHz * t)
            (v * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val at = AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(format)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(buffer.size * 2)
                .build()
            at.write(buffer, 0, buffer.size)
            // loop forever
            at.setLoopPoints(0, buffer.size, -1)
            at
        } catch (t: Throwable) {
            Log.w("Tuner", "AudioTrack create failed", t)
            null
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("튜너 (기준음 재생)", fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(16.dp))
        // selection chips
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            strings.forEach { pair ->
                FilterChip(
                    selected = selected == pair,
                    onClick = { selected = pair },
                    label = { Text(pair.first) }
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                stop()
                val newTrack = createLoopingTone(selected.second)
                newTrack?.play()
                track = newTrack
                isPlaying = newTrack != null
            }) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("재생")
            }
            OutlinedButton(onClick = { stop() }) {
                Icon(Icons.Filled.Pause, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("정지")
            }
        }
        Spacer(Modifier.height(8.dp))
        if (isPlaying) Text("재생 중: ${selected.first}")
    }
}
