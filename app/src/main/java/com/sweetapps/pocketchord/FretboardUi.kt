package com.sweetapps.pocketchord

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.*
import androidx.compose.material3.Slider
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.tooling.preview.Preview

// 다이어그램 관련 컴포저블을 이 파일에서 관리합니다.

@Composable
fun FretDiagram(data: FretDiagramData, stringStrokeWidthDp: Dp? = null) {
    // 예시: 5x6 격자, 점 위치는 data.positions
    Box(
        modifier = Modifier
            .size(160.dp)
            .background(Color.Black, RoundedCornerShape(12.dp))
    ) {
        // Convert optional Dp to px using LocalDensity
        val strokePx = stringStrokeWidthDp?.let { with(LocalDensity.current) { it.toPx() } }
        val defaultStroke = 2f

        // 수평(가로)선: 기존 레이아웃을 간단히 유지
        for (i in 0..5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.Gray)
                    .align(Alignment.TopStart)
                    .offset(y = (i * 32).dp)
            ) {}
        }

        // 수직(세로)선: Canvas로 그려 stroke 조절
        for (i in 0..4) {
            Canvas(modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .align(Alignment.TopStart)
                .offset(x = (i * 32).dp)) {
                val stroke = strokePx?.coerceAtLeast(0.5f) ?: defaultStroke
                drawLine(Color.Gray, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), strokeWidth = stroke)
            }
        }

        // 점(파란색 원 + 숫자)
        data.positions.forEachIndexed { idx, pos ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFF339CFF), shape = RoundedCornerShape(14.dp))
                    .align(Alignment.TopStart)
                    .offset(x = (pos * 24).dp, y = (idx * 24).dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = pos.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun FretDiagramImage() {
    Canvas(modifier = Modifier.size(100.dp)) {
        // 프렛 선
        for (i in 0..4) {
            drawLine(Color.Black, start = Offset(0f, i * 20f), end = Offset(100f, i * 20f), strokeWidth = 2f)
        }
        // 줄 선
        for (i in 0..4) {
            drawLine(Color.Black, start = Offset(i * 25f, 0f), end = Offset(i * 25f, 80f), strokeWidth = 2f)
        }
        // 검은 원(코드 포인트)
        drawCircle(Color.Black, radius = 7f, center = Offset(25f, 40f))
        drawCircle(Color.Black, radius = 7f, center = Offset(50f, 20f))
        drawCircle(Color.Black, radius = 7f, center = Offset(75f, 60f))
        // 바(Bar) 표시
        drawRect(Color.Black, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(15f, 80f))
        // X 표시
        drawLine(Color.Black, Offset(5f, 90f), Offset(20f, 105f), strokeWidth = 3f)
        drawLine(Color.Black, Offset(20f, 90f), Offset(5f, 105f), strokeWidth = 3f)
    }
}

@Composable
fun FretboardDiagram(
    chordName: String,
    positions: List<Int>, // 6개 줄의 각 프렛(0~4, -1은 미사용)
    bar: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = chordName,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color(0xFF31455A),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Canvas(modifier = Modifier.size(width = 180.dp, height = 120.dp)) {
            val fretCount = 5
            val stringCount = 6
            val fretSpacing = size.width / fretCount
            val stringSpacing = size.height / (stringCount - 1)
            // 배경
            drawRect(Color.White, size = size)
            // 프렛(세로줄)
            for (fret in 0..fretCount) {
                val x = fret * fretSpacing
                drawLine(
                    color = if (fret == 0) Color.Black else Color.Gray,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = if (fret == 0) 8f else 2f
                )
            }
            // 줄(가로줄)
            for (string in 0 until stringCount) {
                val y = string * stringSpacing
                drawLine(
                    color = Color.Gray,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 2f
                )
            }
            // 바코드(필요시)
            if (bar) {
                val barYStart = 0f
                val barYEnd = size.height
                val barX = fretSpacing / 2
                drawRoundRect(
                    color = Color.Black,
                    topLeft = Offset(barX - 10f, barYStart),
                    size = androidx.compose.ui.geometry.Size(20f, barYEnd),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                )
            }
            // 코드 포인트(검은 원)
            positions.forEachIndexed { stringIdx, fretNum ->
                if (fretNum > 0) {
                    val x = fretNum * fretSpacing
                    val y = stringIdx * stringSpacing
                    drawCircle(
                        color = Color.Black,
                        radius = 12f,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

@Composable
fun FretboardDiagramOnly(
    modifier: Modifier = Modifier,
    stringStrokeWidthDp: Dp? = null,
    nutWidthFactor: Float = 0.06f
) {
    val strokePx = stringStrokeWidthDp?.let { with(LocalDensity.current) { it.toPx() } }
    Box(
        modifier = modifier
            .background(Color.White)
            .padding(0.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val fretCount = 5
            val stringCount = 6
            val nutWidth = size.width * nutWidthFactor
            val fretSpacing = (size.width - nutWidth) / fretCount
            val stringSpacing = size.height / (stringCount - 1)
            // 배경
            drawRect(Color.White, size = size)
            // 프렛(세로줄)
            for (fret in 0..fretCount) {
                val x = nutWidth + fret * fretSpacing
                if (fret == 0) {
                    // Nut(너트)
                    drawLine(
                        color = Color.Black,
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = nutWidth
                    )
                } else {
                    // For vertical fret lines use a uniform thin stroke
                    drawLine(
                        color = Color.Black,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 2.0f
                    )
                }
            }
            // 줄(가로줄) — 각 줄마다 개별 stroke 적용 가능
            for (string in 0 until stringCount) {
                val y = string * stringSpacing
                val stroke = strokePx ?: 1.8f
                drawLine(
                    color = Color.Black,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = stroke
                )
            }
        }
    }
}

// `FretboardPreviewScreen` 함수는 사용자가 삭제 요청하여 제거했습니다.
// 필요하면 언제든지 다시 추가하거나 앱 네비게이션 라우트로 연결할 수 있습니다.

@Preview(name = "Fretboard Small", showBackground = true, widthDp = 320, heightDp = 200)
@Composable
fun PreviewFretboardSmall() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        FretboardDiagramOnly(
            modifier = Modifier
                .width(220.dp)
                .height(120.dp),
            stringStrokeWidthDp = 1.0.dp,
            nutWidthFactor = 0.05f
        )
    }
}

@Preview(name = "Fretboard Medium", showBackground = true, widthDp = 360, heightDp = 260)
@Composable
fun PreviewFretboardMedium() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        FretboardDiagramOnly(
            modifier = Modifier
                .width(260.dp)
                .height(160.dp),
            stringStrokeWidthDp = 1.6.dp,
            nutWidthFactor = 0.06f
        )
    }
}

@Preview(name = "Fretboard Large", showBackground = true, widthDp = 412, heightDp = 320)
@Composable
fun PreviewFretboardLarge() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        FretboardDiagramOnly(
            modifier = Modifier
                .width(320.dp)
                .height(200.dp),
            stringStrokeWidthDp = 2.4.dp,
            nutWidthFactor = 0.07f
        )
    }
}

@Preview(name = "CodeCard Preview", showBackground = true, widthDp = 360, heightDp = 240)
@Composable
fun PreviewCodeCard() {
    val sample = FretDiagramData("C Sample", listOf(1, 2, 3, 0, 0))
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CodeCard(sample)
    }
}
