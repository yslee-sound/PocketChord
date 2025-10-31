package com.sweetapps.pocketchord

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import com.sweetapps.pocketchord.data.importSeedFromAssets
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
import android.content.pm.ApplicationInfo
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Color as AndroidColor
import androidx.core.view.WindowCompat
import androidx.core.content.edit
import android.content.Context
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.sweetapps.pocketchord.ui.theme.PocketChordTheme
import androidx.compose.ui.unit.isFinite

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
                        composable("search") {
                            Log.d("NavDebug", "Entered route: search")
                            SearchResultScreen()
                        }
                        composable("search_chord") {
                            Log.d("NavDebug", "Entered route: search_chord")
                            SearchChordScreen()
                        }
                        composable("chord_list/{root}") { backStackEntry ->
                            val root = backStackEntry.arguments?.getString("root") ?: "C"
                            Log.d("NavDebug", "Entered route: chord_list/" + root)
                            ChordListScreen(navController = navController, root = root, onBack = { navController.popBackStack() })
                        }
                        composable("settings") {
                            SettingsScreen()
                        }
                     }
                 }
             }
         }

        // Seed database once on first app launch. Uses SharedPreferences to avoid duplicate runs.
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val prefs = getSharedPreferences("pocketchord_prefs", MODE_PRIVATE)
                val seededKey = "seeded_v1"
                val autoSeedKey = "auto_seed_enabled"
                val alreadySeeded = prefs.getBoolean(seededKey, false)
                val autoSeedEnabled = prefs.getBoolean(autoSeedKey, true)
                if (!alreadySeeded && autoSeedEnabled) {
                    val result = importSeedFromAssets(this@MainActivity, "chords_seed_template.json")
                    if (result.errors.isEmpty()) {
                        prefs.edit { putBoolean(seededKey, true) }
                        Log.d("SeedImport", "Seed import completed: insertedChords=${result.insertedChords} insertedVariants=${result.insertedVariants}")
                    } else {
                        Log.e("SeedImport", "Seed import encountered errors: ${result.errors}")
                    }
                } else {
                    Log.d("SeedImport", "Skipping import (alreadySeeded=$alreadySeeded, autoSeedEnabled=$autoSeedEnabled)")
                }
            } catch (e: Exception) {
                Log.e("SeedImport", "Seed import failed", e)
            }
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
    Column {
        chords.chunked(3).forEach { rowList ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowList.forEach { chord ->
                    ChordButton(
                        chord = chord,
                        modifier = Modifier.weight(1f).clickable {
                            val root = chord.substringBefore("-")
                            val route = "chord_list/$root"
                            Log.d("NavDebug", "Click: navigating to $route from grid (chord=$chord)")
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
    NavigationBar(containerColor = Color.Transparent) {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "홈") },
            label = { Text("홈") },
            selected = false,
            onClick = { navController.navigate("home") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Search, contentDescription = "검색") },
            label = { Text("검색") },
            selected = false,
            onClick = { navController.navigate("search_chord") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Star, contentDescription = "즐겨찾기") },
            label = { Text("즐겨찾기") },
            selected = false,
            onClick = {}
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Settings, contentDescription = "설정") },
            label = { Text("설정") },
            selected = false,
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

@Composable
fun ChordListScreen(
    navController: NavHostController,
    root: String,
    onBack: () -> Unit = {},
    uiParams: DiagramUiParams = DefaultDiagramUiParams
) {
    val chordList = when (root) {
        "C" -> listOf("C", "C6", "CM7", "Cm", "C7", "Cadd9", "C9", "C11", "C13")
        "D" -> listOf("D", "D6", "DM7")
        else -> listOf(root)
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)
    ) {
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

        // Filter chips (horizontal scroll)
        val filters = listOf("Major", "Minor", "Dominant", "Augmented")
        Row(modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 12.dp, top = 8.dp)
        ) {
            filters.forEach { f ->
                AssistChip(
                    onClick = {},
                    label = { Text(f) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFF1F3F4)),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "총 ${chordList.size}개", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(start = 16.dp))

        Spacer(modifier = Modifier.height(8.dp))

        // List of chords
        // Increase spacing so only ~3 items appear on typical phone screens while keeping item sizes unchanged.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(DEFAULT_LIST_ITEM_SPACING_DP)
        ) {
            items(chordList) { chordName ->
                // Plain list row (no outer card). Left: orange square showing chord name.
                // Right: fret diagram shown without border/background.
                // Use a Row where name box and diagram are independent: spacer keeps diagram fixed width
                // sample DB-style positions & fingers for C chord; convert to internal positions when needed
                val dbPositionsForC = mapOf(1 to 0, 2 to 1, 3 to 0, 4 to 2, 5 to 3, 6 to -1)
                val dbFingersForC = mapOf(1 to 0, 2 to 1, 3 to 0, 4 to 2, 5 to 3, 6 to 0)
                val positionsForC = dbMapToInternalPositions(dbPositionsForC, stringCount = 6, defaultFret = -1)
                val fingersForC = dbMapToInternalPositions(dbFingersForC, stringCount = 6, defaultFret = 0)

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
                            if (chordName == "C") {
                                FretboardDiagramOnly(modifier = Modifier.fillMaxSize(), uiParams = uiParams, positions = positionsForC, fingers = fingersForC, firstFretIsNut = true)
                            } else {
                                FretboardDiagramOnly(modifier = Modifier.fillMaxSize(), uiParams = uiParams)
                            }
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
                        Spacer(modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(modifier = Modifier.width(desiredDiagramWidth).height(diagramHeightForList)) {
                            if (chordName == "C") {
                                FretboardDiagramOnly(modifier = Modifier.fillMaxSize(), uiParams = uiParams, positions = positionsForC, fingers = fingersForC, firstFretIsNut = true)
                            } else {
                                FretboardDiagramOnly(modifier = Modifier.fillMaxSize(), uiParams = uiParams)
                            }
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
    uiParams: DiagramUiParams = DefaultDiagramUiParams, // centralized UI params
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
                                    FretboardDiagramOnly(modifier = Modifier.fillMaxSize(), uiParams = uiParams, positions = positionsForC, fingers = fingersForC, firstFretIsNut = true, fretLabelProvider = fretLabelProvider)
                                } else {
                                    FretboardDiagramOnly(modifier = Modifier.fillMaxSize(), uiParams = uiParams)
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
fun SettingsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("pocketchord_prefs", Context.MODE_PRIVATE)
    val initialAuto = prefs.getBoolean("auto_seed_enabled", true)
    var autoSeed by remember { mutableStateOf(initialAuto) }
    val summary = prefs.getString("last_seed_import_summary", "no summary") ?: "no summary"
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "디버그 설정", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        val isDebuggable = try { (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 } catch (_: Exception) { false }
        if (!isDebuggable) {
            Text("디버그 UI는 개발 빌드에서만 표시됩니다.")
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(text = "자동 시드 임포트 사용", modifier = Modifier.weight(1f))
            Switch(checked = autoSeed, onCheckedChange = {
                autoSeed = it
                prefs.edit { putBoolean("auto_seed_enabled", it) }
            })
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "마지막 임포트 요약:")
        Text(text = summary, fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            scope.launch(Dispatchers.IO) {
                try {
                    val result = importSeedFromAssets(context, "chords_seed_template.json")
                    if (result.errors.isEmpty()) {
                        prefs.edit { putBoolean("seeded_v1", true) }
                    }
                    // update summary read-back on UI thread
                    Log.d("SeedImport", "Force seed result: insertedChords=${result.insertedChords} insertedVariants=${result.insertedVariants} skipped=${result.skippedVariants}")
                } catch (t: Throwable) {
                    Log.e("SeedImport", "Forced seed failed", t)
                }
            }
        }) {
            Text("강제 재시드")
        }
    }
}
