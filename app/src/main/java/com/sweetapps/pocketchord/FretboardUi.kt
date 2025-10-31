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

@Suppress("unused")
@Composable
fun FretboardDiagram(
    chordName: String,
    positions: List<Int>, // stringCount 길이: -1=mute, 0=open, n>0 fret number
    modifier: Modifier = Modifier,
    fingers: List<Int>? = null, // same length, 0=hide
    uiParams: DiagramUiParams = DefaultDiagramUiParams,
    firstFretIsNut: Boolean = true,
    diagramWidth: Dp? = null,
    diagramHeight: Dp? = null,
    fretCount: Int = 4,
    // when true, positions index 0 is treated as the top string (string 1), otherwise index 0 = lowest string
    invertStrings: Boolean = false,
    // optional provider so callers (DB) can supply custom labels per fret index. Return null to skip.
    fretLabelProvider: ((Int) -> String?)? = null
    ,
    // when false, don't add the default outer padding so callers (list rows) can align top edges
    useCardPadding: Boolean = true
) {
    val baseModifier = if (useCardPadding) {
        modifier.background(Color.White, RoundedCornerShape(12.dp)).padding(8.dp)
    } else {
        modifier.background(Color.White, RoundedCornerShape(12.dp))
    }

    Column(
        modifier = baseModifier,
        horizontalAlignment = Alignment.Start
    ) {
        if (chordName.isNotBlank()) {
            Text(chordName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF31455A), modifier = Modifier.padding(bottom = 6.dp))
        }

        // Use BoxWithConstraints so we can draw grid in Canvas and overlay Compose markers (Text in colored Boxes)
        // Use BoxWithConstraints so we can draw grid in Canvas and overlay Compose markers (Text in colored Boxes)
        val defaultWidth = diagramWidth ?: uiParams.diagramMaxWidthDp ?: 200.dp
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
            val labelAreaPx = with(density) { uiParams.fretLabelAreaDp.toPx() }
            val leftInsetPx = with(density) { (uiParams.leftInsetDp + uiParams.diagramShiftDp).toPx() }
            val markerOffsetPx = with(density) { uiParams.markerOffsetDp.toPx() }
            val computedNutPx = uiParams.nutWidthDp?.let { with(density) { it.toPx() } } ?: (boxWpx * uiParams.nutWidthFactor)
            // compute startFret based on positive fret positions and firstFretIsNut flag
            val positiveFrets = positions.filter { it > 0 }
            val startFret = if (firstFretIsNut || positiveFrets.isEmpty() || (positiveFrets.minOrNull() ?: Int.MAX_VALUE) <= 1) {
                1
            } else {
                var s = positiveFrets.minOrNull() ?: 1
                val fMax = positiveFrets.maxOrNull() ?: s
                if (fMax > s + fretCount - 1) s = fMax - fretCount + 1
                if (s < 2) s = 2
                s
            }
            // show nut only when startFret == 1 and caller expects a nut
            val nutPx = if (startFret == 1 && firstFretIsNut) computedNutPx else 0f
            // use provided fretCount
            val stringCount = 6
            // available width for frets = total width - leftInset - nut width
            val rightInsetPx = with(density) { uiParams.diagramRightInsetDp.toPx() }
            // IMPORTANT: subtract reservedNutPx (not nutPx) so spacing is stable regardless of whether the nut is drawn
            val availableWidth = (boxWpx - leftInsetPx - nutPx - rightInsetPx).coerceAtLeast(0f)
            // compute fret spacing so 'fretCount' frets fit inside the available width
            // reserve `lastFretVisibleFraction` worth of one-fret spacing for the final fret's visible width
            val spacingDiv = if (fretCount > 0) (fretCount + uiParams.lastFretVisibleFraction) else 1f
            val fretSpacingPx = if (spacingDiv > 0f) availableWidth / spacingDiv else 0f
            // reserve vertical area at bottom for fret labels
            val contentHeightPx = (boxHpx - labelAreaPx).coerceAtLeast(0f)
            val stringSpacingPx = contentHeightPx / (stringCount - 1)

            Canvas(modifier = Modifier.matchParentSize()) {
                // background
                drawRect(Color.White, size = size)
                // nut or vertical frets (shifted by leftInset)
                // compute stroke half-width and a safe right-edge clamp that respects right inset
                val vStrokeHalf = with(density) { uiParams.verticalLineWidthDp.toPx() } / 2f
                val maxFretX = (size.width - rightInsetPx - vStrokeHalf).coerceAtLeast(leftInsetPx + nutPx)
                // draw frets for f = 0..fretCount (inclusive) so we have nut + 'fretCount' frets visible
                for (f in 0..fretCount) {
                    if (f == 0 && firstFretIsNut) {
                        // draw nut only when requested
                        drawRect(Color.Black, topLeft = Offset(leftInsetPx, 0f), size = androidx.compose.ui.geometry.Size(nutPx, size.height))
                    } else {
                        val xRaw = leftInsetPx + nutPx + f * fretSpacingPx
                        // clamp into visible canvas so the final fret isn't dropped
                        val x = xRaw.coerceAtMost(maxFretX)
                        // ensure fret is drawn only if it lies to the right of the nut area
                        if (x > leftInsetPx + nutPx + 0.5f) {
                            drawLine(Color.Gray, start = Offset(x, 0f), end = Offset(x, contentHeightPx), strokeWidth = with(density) { uiParams.verticalLineWidthDp.toPx() })
                        }
                    }
                }

                // horizontal strings — align start with nut so strings and fret positions share the same origin
                val stringStartX = leftInsetPx + nutPx
                val stringLineEndX = (size.width - rightInsetPx).coerceAtLeast(stringStartX)
                for (s in 0 until stringCount) {
                    val y = s * stringSpacingPx
                    drawLine(Color.Gray, start = Offset(stringStartX, y), end = Offset(stringLineEndX, y), strokeWidth = with(density) { uiParams.horizontalLineWidthDp.toPx() })
                }

                // detect barres: contiguous runs of strings with same positive fret and same finger (>0)
                data class Barre(val fret: Int, val finger: Int, val startIdx: Int, val endIdx: Int)
                val barres = mutableListOf<Barre>()
                run {
                    // scan positions left-to-right by index to find contiguous runs
                    var idx = 0
                    while (idx < positions.size) {
                        val f = positions[idx]
                        val finger = fingers?.getOrNull(idx) ?: 0
                        if (f > 0 && finger > 0) {
                            var j = idx + 1
                            while (j < positions.size && positions[j] == f && (fingers?.getOrNull(j) ?: 0) == finger) j++
                            val len = j - idx
                            if (len >= 2) {
                                barres.add(Barre(fret = f, finger = finger, startIdx = idx, endIdx = j - 1))
                            }
                            idx = j
                        } else {
                            idx++
                        }
                    }
                }

                // create a set of string indices that are part of any barre so we can skip drawing individual dots there
                val stringsInBarre = barres.flatMap { it.startIdx..it.endIdx }.toSet()

                // draw markers (circles + finger numbers) directly on Canvas for pixel-perfect positioning
                // but first draw barres behind single-dot markers
                barres.forEach { barre ->
                    val rel = barre.fret - startFret
                    if (rel in 0 until fretCount) {
                        // determine vertical span for the barre (y coords depend on invertStrings)
                        val startY = if (invertStrings) barre.startIdx * stringSpacingPx else (stringCount - 1 - barre.startIdx) * stringSpacingPx
                        val endY = if (invertStrings) barre.endIdx * stringSpacingPx else (stringCount - 1 - barre.endIdx) * stringSpacingPx
                        val topY = min(startY, endY)
                        val bottomY = maxOf(startY, endY)
                        val centerX = leftInsetPx + nutPx + (rel + 0.5f) * fretSpacingPx
                        // barre rectangle dimensions
                        val padding = with(density) { 4.dp.toPx() }
                        val rectLeft = centerX - fretSpacingPx * 0.45f
                        val rectRight = centerX + fretSpacingPx * 0.45f
                        val rectTop = topY - padding
                        val rectBottom = bottomY + padding
                        val rectHeight = (rectBottom - rectTop).coerceAtLeast(with(density) { 8.dp.toPx() })
                        val corner = rectHeight / 2f
                        // draw rounded rect
                        drawRoundRect(color = Color(0xFF339CFF), topLeft = Offset(rectLeft, rectTop), size = androidx.compose.ui.geometry.Size(rectRight - rectLeft, rectHeight), cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner))
                        // draw finger number centered on barre (use same visual as markers)
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = (rectHeight * 0.6f).coerceAtMost(with(density) { 18.dp.toPx() })
                                isFakeBoldText = true
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            val baseline = rectTop + rectHeight / 2f - (paint.descent() + paint.ascent()) / 2f
                            drawText(barre.finger.toString(), centerX, baseline, paint)
                        }
                    }
                }

                positions.forEachIndexed { stringIdx, fretNum ->
                     val y = if (invertStrings) {
                         // treat index0 as top string
                         stringIdx * stringSpacingPx
                     } else {
                         // treat index0 as lowest string
                         (stringCount - 1 - stringIdx) * stringSpacingPx
                     }
                     when {
                        fretNum > 0 -> {
                            // skip drawing single-dot markers for strings that are part of a detected barre
                            if (stringsInBarre.contains(stringIdx)) {
                                // if this string is in a barre, skip individual marker
                            } else {
                                // draw only if this fret lies within the visible window [startFret, startFret + fretCount - 1]
                                val rel = fretNum - startFret
                                if (rel in 0 until fretCount) {
                                    val x = leftInsetPx + nutPx + (rel + 0.5f) * fretSpacingPx
                                    // marker radius derived from UI params with a minimum px size for visibility
                                    val candidate = min(fretSpacingPx, stringSpacingPx) * uiParams.markerRadiusFactor
                                    val minRadiusPx = with(density) { 6.dp.toPx() }
                                    val radius = kotlin.math.max(candidate, minRadiusPx)
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
                            }
                        }
                        // Compute open marker radius first, then derive mute 'X' size so both visually match
                        fretNum == 0 -> {
                            val x = leftInsetPx - markerOffsetPx
                            val minSpacing = min(fretSpacingPx, stringSpacingPx)
                            val openRadius = minSpacing * uiParams.openMarkerSizeFactor
                            val strokeWOpen = with(density) { uiParams.openMarkerStrokeDp.toPx() }
                            drawCircle(color = Color.Transparent, center = Offset(x, y), radius = openRadius)
                            drawCircle(color = Color.Black, center = Offset(x, y), radius = openRadius, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWOpen))
                        }
                        else -> {
                            val minSpacing = min(fretSpacingPx, stringSpacingPx)
                            // compute open radius and scale mute half-size so diagonal ~ open diameter
                            val openFactor = uiParams.openMarkerSizeFactor.takeIf { it > 0f } ?: 0.0001f
                            val openRadius = minSpacing * uiParams.openMarkerSizeFactor
                            val muteScale = uiParams.muteMarkerSizeFactor / openFactor
                            val muteHalf = openRadius * 0.70710677f * muteScale
                            val x = leftInsetPx - markerOffsetPx
                            val strokeW = with(density) { uiParams.muteMarkerStrokeDp.toPx() }
                            val inset = muteHalf * uiParams.muteMarkerInsetFactor
                            // draw lines from corners inside by inset to achieve balanced visual weight
                            drawLine(Color.Black, start = Offset(x - muteHalf + inset, y - muteHalf + inset), end = Offset(x + muteHalf - inset, y + muteHalf - inset), strokeWidth = strokeW)
                            drawLine(Color.Black, start = Offset(x - muteHalf + inset, y + muteHalf - inset), end = Offset(x + muteHalf - inset, y - muteHalf + inset), strokeWidth = strokeW)
                        }
                    }
                }
                // Draw fret labels in reserved label area below contentHeightPx
                val textSizePx = with(density) { uiParams.fretLabelTextSp.sp.toPx() }
                // show numeric labels for the visible frets (map canvas f index to absolute fret num)
                for (f in 1 until fretCount) {
                    val absoluteFret = startFret + (f - 1)
                    val label = fretLabelProvider?.invoke(absoluteFret) ?: absoluteFret.toString()
                    if (label.isEmpty()) continue
                    val xLabel = (leftInsetPx + nutPx + f * fretSpacingPx).coerceAtMost(maxFretX)
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = textSizePx
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        val baseline = contentHeightPx + (labelAreaPx + (paint.descent() - paint.ascent())) / 2 - paint.descent()
                        drawText(label, xLabel, baseline, paint)
                    }
                }
             }
         }
     }
 }

 // For the small variant we also need barre detection and skip single markers on barre strings
 // We'll reuse the logic: detect barresSmall then draw barresSmall and skip single dots inside them
 // (mirrors the large variant above)
 @Composable
 fun FretboardDiagramOnly(
    positions: List<Int>, // stringCount 길이: -1=mute, 0=open, n>0 fret number
    modifier: Modifier = Modifier,
    fingers: List<Int>? = null, // same length, 0=hide
    uiParams: DiagramUiParams = DefaultDiagramUiParams,
    firstFretIsNut: Boolean = true,
    diagramWidth: Dp? = null,
    diagramHeight: Dp? = null,
    fretCount: Int = 4,
    // when true, positions index 0 is treated as the top string (string 1), otherwise index 0 = lowest string
    invertStrings: Boolean = false,
    // optional provider so callers (DB) can supply custom labels per fret index. Return null to skip.
    fretLabelProvider: ((Int) -> String?)? = null
) {
    // same content as FretboardDiagram above, but without chord name
    // Pass the modifier through unchanged so callers control size/constraints.
    FretboardDiagram(
        chordName = "",
        positions = positions,
        modifier = modifier,
        fingers = fingers,
        uiParams = uiParams,
        firstFretIsNut = firstFretIsNut,
        diagramWidth = diagramWidth,
        diagramHeight = diagramHeight,
        fretCount = fretCount,
        invertStrings = invertStrings,
        fretLabelProvider = fretLabelProvider,
        useCardPadding = false
    )
}
