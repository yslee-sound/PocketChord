package com.sweetapps.pocketchord.ui.screens

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

/**
 * 홈 화면 (코드 그리드)
 *
 * PocketChord 앱의 메인 화면으로, 12개의 코드 루트를 그리드 형태로 표시합니다.
 * 각 코드를 클릭하면 해당 루트의 코드 목록 화면으로 이동합니다.
 */
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
        fontSize = 24.sp,
        color = Color(0xFF1F2D3D),
        modifier = Modifier.padding(start = 4.dp)
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

