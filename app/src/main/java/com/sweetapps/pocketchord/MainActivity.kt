package com.sweetapps.pocketchord

import android.os.Bundle
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sweetapps.pocketchord.ui.theme.PocketChordTheme
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.isFinite

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketChordTheme {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = { BottomNavigationBar(navController) },
                    containerColor = Color(0xFFEFF3F5)
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") { MainScreen(navController) }
                        composable("search") { SearchResultScreen() }
                        composable("search_chord") { SearchChordScreen() }
                        composable("chord_list/{root}") { backStackEntry ->
                            val root = backStackEntry.arguments?.getString("root") ?: "C"
                            ChordListScreen(navController, root = root) { navController.popBackStack() }
                        }
                        composable("chord_detail/{root}") { backStackEntry ->
                            val root = backStackEntry.arguments?.getString("root") ?: "C"
                            ChordDetailScreen(root = root) { navController.popBackStack() }
                        }
                    }
                }
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
        for (row in 0 until 4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                for (col in 0 until 3) {
                    val index = row * 3 + col
                    if (index < chords.size) {
                        val chord = chords[index]
                        ChordButton(
                            chord = chord,
                            modifier = Modifier.weight(1f).clickable {
                                val root = chord.substringBefore("-")
                                if (root == "C") {
                                    navController.navigate("chord_detail/C")
                                } else {
                                    navController.navigate("chord_list/$root")
                                }
                            }
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
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
            onClick = {}
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
            .background(Color(0xFFEFF3F5))
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
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
    val rootNotes = listOf("C", "D", "E", "F", "G", "A", "B")
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
            .background(Color(0xFFF7FAFC))
    ) {
        // 상단 바
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* TODO: 뒤로가기 */ }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로가기", tint = Color(0xFF31455A))
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
fun ChordListScreen(navController: NavHostController, root: String, onBack: () -> Unit = {}) {
    val chordList = when (root) {
        "C" -> listOf("C", "CM7", "Cm7", "C6")
        "D" -> listOf("D", "DM7", "Dm7", "D6")
        else -> listOf(root)
    }
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로가기", tint = Color.Gray)
                }
            }
        },
        containerColor = Color.Black
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black),
            contentPadding = PaddingValues(vertical = 2.dp, horizontal = 0.dp) // 여백을 더 줄임
        ) {
            items(chordList) { chordName ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp, horizontal = 8.dp) // 카드 위아래 여백을 더 줄임
                        .clickable {
                            if (root == "C" && chordName == "C") {
                                navController.navigate("chord_detail/C")
                            }
                        },
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCE6F7))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp), // 카드 내부 여백을 더 줄임
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = chordName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp, // 폰트 크기를 약간 줄임
                            color = Color.Black,
                            modifier = Modifier.width(50.dp) // 텍스트 영역을 더 줄임
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color.White)
                                .padding(4.dp)
                        ) {
                            FretDiagramImage()
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
    uiParams: DiagramUiParams = DefaultDiagramUiParams // centralized UI params
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp), // 카드 외부 여백 약간 증가
        shape = RoundedCornerShape(18.dp), // 더 둥글게
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        // 카드 높이에 따라 다이어그램 크기를 계산하려면 BoxWithConstraints를 사용
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cardHeight = this.maxHeight
            val effectiveCardHeight = if (cardHeight.isFinite) cardHeight else 140.dp
            val diagramHeight = (effectiveCardHeight - 20.dp).coerceAtLeast(72.dp)
            val diagramWidth = (diagramHeight * (140f / 96f)).coerceAtMost(220.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(effectiveCardHeight)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chordName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp, // 좌측 텍스트 더 크게
                    color = Color.Black,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // 우측 다이어그램: 카드 높이에 맞춰 크기가 결정됨
                // C 코드(첫 항목)에 대해 표준 핑거링을 더미로 표시
                // Define chord data in DB-style (1 = topmost string)
                val dbPositionsForC = mapOf(
                    1 to 0,  // string 1 (top) -> open
                    2 to 1,  // string 2 -> fret 1
                    3 to 0,  // string 3 -> open
                    4 to 2,  // string 4 -> fret 2
                    5 to 3,  // string 5 -> fret 3
                    6 to -1  // string 6 -> mute
                )
                val dbFingersForC = mapOf(
                    1 to 0,
                    2 to 1,
                    3 to 0,
                    4 to 2,
                    5 to 3,
                    6 to 0
                )
                val positionsForC = dbMapToInternalPositions(dbPositionsForC, stringCount = 6, defaultFret = -1)
                val fingersForC = dbMapToInternalPositions(dbFingersForC, stringCount = 6, defaultFret = 0)
                if (chordName == "C") {
                    FretboardDiagramOnly(
                        modifier = Modifier.size(width = diagramWidth, height = diagramHeight),
                        uiParams = uiParams,
                        positions = positionsForC,
                        fingers = fingersForC,
                        firstFretIsNut = true
                    )
                } else {
                    FretboardDiagramOnly(
                        modifier = Modifier.size(width = diagramWidth, height = diagramHeight),
                        uiParams = uiParams
                    )
                }
             }
         }
     }
 }

@Composable
fun ChordDetailScreen(root: String, onBack: () -> Unit = {}) {
    val chordList = when (root) {
        // C 루트의 경우 테스트용 더미 항목 10개를 표시하도록 확장
        "C" -> listOf(
            "C", "C6", "CM7", "Cm", "C7",
            "Cmaj7", "Cadd9", "C9", "C11", "C13"
        )
        "D" -> listOf("D", "D6", "DM7")
        else -> listOf(root)
    }
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로가기", tint = Color.Gray)
                }
                Text("${root} 코드 상세", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF31455A), modifier = Modifier.padding(start = 8.dp))
            }
        },
        containerColor = Color(0xFFEFF3F5)
    ) { innerPadding ->
        // 카드 크기를 첨부 이미지와 비슷하게 고정하여 배열을 맞춤
        val singleCardDp = 160.dp // 카드 높이를 고정(스크롤로 여러 항목 확인 가능)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(vertical = 12.dp) // 상하 여백을 조금 추가
        ) {
            items(chordList) { chordName ->
                FretboardCard(
                    chordName = chordName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(singleCardDp)
                        .padding(vertical = 2.dp, horizontal = 8.dp) // 아이템 간격 최소화
                )
            }
        }
    }
}
