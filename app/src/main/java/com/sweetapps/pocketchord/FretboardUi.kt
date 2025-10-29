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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.min

// 다이어그램 관련 컴포저블 정리 파일

@Composable
fun FretDiagramImage() {
    Canvas(modifier = Modifier.size(100.dp)) {
        // horizontal frets
        for (i in 0..4) {
            drawLine(Color.Black, start = Offset(0f, i * 20f), end = Offset(100f, i * 20f), strokeWidth = 2f)
        }
        // vertical strings
        for (i in 0..4) {
            drawLine(Color.Black, start = Offset(i * 25f, 0f), end = Offset(i * 25f, 80f), strokeWidth = 2f)
        }
        // sample dots
        drawCircle(Color.Black, radius = 7f, center = Offset(25f, 40f))
        drawCircle(Color.Black, radius = 7f, center = Offset(50f, 20f))
        drawCircle(Color.Black, radius = 7f, center = Offset(75f, 60f))
        // nut
        drawRect(Color.Black, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(15f, 80f))
    }
}

@Composable
fun FretboardDiagram(
    chordName: String,
    positions: List<Int>, // stringCount 길이: -1=mute, 0=open, n>0 fret number
    fingers: List<Int>? = null, // same length, 0=hide
    modifier: Modifier = Modifier,
    uiParams: DiagramUiParams = DefaultDiagramUiParams,
    firstFretIsNut: Boolean = true,
    diagramWidth: Dp? = null,
    diagramHeight: Dp? = null
) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(chordName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF31455A), modifier = Modifier.padding(bottom = 6.dp))

        // Use BoxWithConstraints so we can draw grid in Canvas and overlay Compose markers (Text in colored Boxes)
        // Use BoxWithConstraints so we can draw grid in Canvas and overlay Compose markers (Text in colored Boxes)
        val defaultWidth = diagramWidth ?: 200.dp
        val defaultHeight = diagramHeight ?: 120.dp
        val boxModifier = Modifier.width(defaultWidth).height(defaultHeight).then(modifier) // caller modifier (if contains size) will override defaults
        BoxWithConstraints(
            modifier = boxModifier,
            contentAlignment = Alignment.TopStart
        ) {
            val boxW = maxWidth
            val boxH = maxHeight
            val density = LocalDensity.current
            val boxWpx = with(density) { boxW.toPx() }
            val boxHpx = with(density) { boxH.toPx() }
            val leftInsetPx = with(density) { uiParams.leftInsetDp.toPx() }
            val openOffsetPx = with(density) { uiParams.openMarkerOffsetDp.toPx() }
            val computedNutPx = uiParams.nutWidthDp?.let { with(density) { it.toPx() } } ?: (boxWpx * uiParams.nutWidthFactor)
            val nutPx = if (firstFretIsNut) computedNutPx else 0f
            val fretCount = 5
            val stringCount = 6
            // available width for frets = total width - leftInset - nut width
            val availableWidth = (boxWpx - leftInsetPx - nutPx).coerceAtLeast(0f)
            val fretSpacingPx = if (fretCount > 0) availableWidth / fretCount else 0f
            val stringSpacingPx = if (stringCount > 1) boxHpx / (stringCount - 1) else boxHpx

            Canvas(modifier = Modifier.matchParentSize()) {
                // background
                drawRect(Color.White, size = size)
                // nut or vertical frets (shifted by leftInset)
                for (f in 0..fretCount) {
                    if (f == 0 && firstFretIsNut) {
                        drawRect(Color.Black, topLeft = Offset(leftInsetPx, 0f), size = androidx.compose.ui.geometry.Size(nutPx, size.height))
                    } else {
                        val x = leftInsetPx + nutPx + f * fretSpacingPx
                        drawLine(Color.Gray, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = with(density) { uiParams.verticalLineWidthDp.toPx() })
                    }
                }
                // horizontal strings (start after leftInset)
                for (s in 0 until stringCount) {
                    val y = s * stringSpacingPx
                    drawLine(Color.Gray, start = Offset(leftInsetPx, y), end = Offset(size.width, y), strokeWidth = with(density) { uiParams.horizontalLineWidthDp.toPx() })
                }

                // draw markers (circles + finger numbers) directly on Canvas for pixel-perfect positioning
                positions.forEachIndexed { stringIdx, fretNum ->
                    val y = (stringCount - 1 - stringIdx) * stringSpacingPx
                    when {
                        fretNum > 0 -> {
                            val x = leftInsetPx + nutPx + (fretNum - 0.5f) * fretSpacingPx
                            // marker radius derived from UI params
                            val radius = min(fretSpacingPx, stringSpacingPx) * uiParams.markerRadiusFactor
                            drawCircle(color = Color(0xFF339CFF), center = Offset(x, y), radius = radius)
                            val finger = fingers?.getOrNull(stringIdx) ?: 0
                            if (finger > 0) {
                                // draw centered text using nativeCanvas
                                drawContext.canvas.nativeCanvas.apply {
                                    val paint = android.graphics.Paint().apply {
                                        color = android.graphics.Color.WHITE
                                        textSize = radius * uiParams.markerTextScale
                                        isFakeBoldText = true
                                        textAlign = android.graphics.Paint.Align.CENTER
                                    }
                                    val baseline = y + (paint.descent() - paint.ascent()) / 2 - paint.descent()
                                    drawText(finger.toString(), x, baseline, paint)
                                }
                            }
                        }
                        // Compute open marker radius first, then derive mute 'X' size so both visually match
                        fretNum == 0 -> {
                            val x = leftInsetPx - with(density) { uiParams.openMarkerOffsetDp.toPx() }
                            val minSpacing = min(fretSpacingPx, stringSpacingPx)
                            val openRadius = minSpacing * uiParams.openMarkerSizeFactor
                            val strokeWOpen = with(density) { uiParams.openMarkerStrokeDp.toPx() }
                            drawCircle(color = Color.Transparent, center = Offset(x, y), radius = openRadius)
                            drawCircle(color = Color.Black, center = Offset(x, y), radius = openRadius, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWOpen))
                        }
                        fretNum < 0 -> {
                            val minSpacing = min(fretSpacingPx, stringSpacingPx)
                            // compute open radius and scale mute half-size so diagonal ~ open diameter
                            val openFactor = uiParams.openMarkerSizeFactor.takeIf { it > 0f } ?: 0.0001f
                            val openRadius = minSpacing * uiParams.openMarkerSizeFactor
                            val muteScale = uiParams.muteMarkerSizeFactor / openFactor
                            val muteHalf = openRadius * 0.70710677f * muteScale
                            val x = leftInsetPx - with(density) { uiParams.muteMarkerOffsetDp.toPx() }
                            val strokeW = with(density) { uiParams.muteMarkerStrokeDp.toPx() }
                            val inset = muteHalf * uiParams.muteMarkerInsetFactor
                            // draw lines from corners inside by inset to achieve balanced visual weight
                            drawLine(Color.Black, start = Offset(x - muteHalf + inset, y - muteHalf + inset), end = Offset(x + muteHalf - inset, y + muteHalf - inset), strokeWidth = strokeW)
                            drawLine(Color.Black, start = Offset(x - muteHalf + inset, y + muteHalf - inset), end = Offset(x + muteHalf - inset, y - muteHalf + inset), strokeWidth = strokeW)
                        }
                     }
                 }
             }
         }
     }
 }

@Composable
fun FretboardDiagramOnly(
    modifier: Modifier = Modifier,
    uiParams: DiagramUiParams = DefaultDiagramUiParams,
    stringStrokeWidthDp: Dp? = null,
    positions: List<Int>? = null,
    fingers: List<Int>? = null,
    firstFretIsNut: Boolean = true
) {
    // Small variant: fills given modifier size
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val boxWpx = with(density) { maxWidth.toPx() }
        val boxHpx = with(density) { maxHeight.toPx() }
        val leftInsetPx = with(density) { uiParams.leftInsetDp.toPx() }
        val openOffsetPx = with(density) { uiParams.openMarkerOffsetDp.toPx() }
        val computedNutPx = uiParams.nutWidthDp?.let { with(density) { it.toPx() } } ?: (boxWpx * uiParams.nutWidthFactor)
        val nutPx = if (firstFretIsNut) computedNutPx else 0f
        val fretCount = 5
        val stringCount = 6
        val availableWidth = (boxWpx - leftInsetPx - nutPx).coerceAtLeast(0f)
        val fretSpacingPx = if (fretCount > 0) availableWidth / fretCount else 0f
        val stringSpacingPx = if (stringCount > 1) boxHpx / (stringCount - 1) else boxHpx

        Canvas(modifier = Modifier.matchParentSize()) {
             drawRect(Color.White, size = size)
             for (f in 0..fretCount) {
                 if (f == 0 && firstFretIsNut) drawRect(Color.Black, topLeft = Offset(leftInsetPx, 0f), size = androidx.compose.ui.geometry.Size(nutPx, size.height))
                 else {
                     val x = leftInsetPx + nutPx + f * fretSpacingPx
                     drawLine(Color.Black, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = with(density) { uiParams.verticalLineWidthDp.toPx() })
                 }
             }
              for (s in 0 until stringCount) {
                  val y = s * stringSpacingPx
                 drawLine(Color.Black, start = Offset(leftInsetPx, y), end = Offset(size.width, y), strokeWidth = stringStrokeWidthDp?.let { with(density) { it.toPx() } } ?: with(density) { uiParams.horizontalLineWidthDp.toPx() })
              }

             // draw markers directly on canvas (positions indexed low->high strings: [6th..1st])
             positions?.let { posList ->
                 posList.forEachIndexed { si, fn ->
                     val y = (stringCount - 1 - si) * stringSpacingPx
                     when {
                         fn > 0 -> {
                             val x = leftInsetPx + nutPx + (fn - 0.5f) * fretSpacingPx
                             val radius = min(fretSpacingPx, stringSpacingPx) * uiParams.markerRadiusFactor
                             drawCircle(color = Color(0xFF339CFF), center = Offset(x, y), radius = radius)
                             val finger = fingers?.getOrNull(si) ?: 0
                             if (finger > 0) {
                                 drawContext.canvas.nativeCanvas.apply {
                                     val paint = android.graphics.Paint().apply {
                                         color = android.graphics.Color.WHITE
                                         textSize = radius * uiParams.markerTextScale
                                         isFakeBoldText = true
                                         textAlign = android.graphics.Paint.Align.CENTER
                                     }
                                     val baseline = y + (paint.descent() - paint.ascent()) / 2 - paint.descent()
                                     drawText(finger.toString(), x, baseline, paint)
                                 }
                             }
                         }
                         fn == 0 -> {
                            // open marker uses open-specific params
                            val x = leftInsetPx - with(density) { uiParams.openMarkerOffsetDp.toPx() }
                            val minSpacing = min(fretSpacingPx, stringSpacingPx)
                            val openRadius = minSpacing * uiParams.openMarkerSizeFactor
                            val strokeWOpen = with(density) { uiParams.openMarkerStrokeDp.toPx() }
                            drawCircle(color = Color.Transparent, center = Offset(x, y), radius = openRadius)
                            drawCircle(color = Color.Black, center = Offset(x, y), radius = openRadius, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWOpen))
                         }
                         fn < 0 -> {
                            // mute marker derived from open radius to visually match sizes
                            val minSpacing = min(fretSpacingPx, stringSpacingPx)
                            val openFactor = uiParams.openMarkerSizeFactor.takeIf { it > 0f } ?: 0.0001f
                            val openRadius = minSpacing * uiParams.openMarkerSizeFactor
                            val muteScale = uiParams.muteMarkerSizeFactor / openFactor
                            val muteHalf = openRadius * 0.70710677f * muteScale
                            val x = leftInsetPx - with(density) { uiParams.muteMarkerOffsetDp.toPx() }
                            val strokeW = with(density) { uiParams.muteMarkerStrokeDp.toPx() }
                            val inset = muteHalf * uiParams.muteMarkerInsetFactor
                            drawLine(Color.Black, start = Offset(x - muteHalf + inset, y - muteHalf + inset), end = Offset(x + muteHalf - inset, y + muteHalf - inset), strokeWidth = strokeW)
                            drawLine(Color.Black, start = Offset(x - muteHalf + inset, y + muteHalf - inset), end = Offset(x + muteHalf - inset, y - muteHalf + inset), strokeWidth = strokeW)
                         }
                     }
                 }
             }
         }
     }
 }

@Preview(name = "Fretboard Card Preview (single)", showBackground = true, widthDp = 360, heightDp = 240)
@Composable
fun PreviewFretboardCard_Single() {
    val previewParams = DefaultDiagramUiParams.copy(nutWidthFactor = 0.06f)
    Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.TopCenter) {
        FretboardCard(
            chordName = "C",
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            uiParams = previewParams
        )
    }
}
