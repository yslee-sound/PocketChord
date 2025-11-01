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
    uiParams: DiagramUiParams = defaultDiagramUiParams(),
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
    ,
    // whether to draw the white rounded card background. Some list/preview displays omit it to match AVD.
    drawBackground: Boolean = true,
) {
    val baseModifier = if (useCardPadding) {
        // add padding; draw background only when requested
        if (drawBackground) modifier.background(Color.White, RoundedCornerShape(12.dp)).padding(8.dp)
        else modifier.padding(8.dp)
    } else {
        if (drawBackground) modifier.background(Color.White, RoundedCornerShape(12.dp))
        else modifier
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
        // prefer explicit diagramHeight param, then uiParams.diagramHeightDp (derived from DEFAULT_DIAGRAM_BASE_DP), then fallback
        val defaultHeight = diagramHeight ?: uiParams.diagramHeightDp ?: 120.dp
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
            // reserve nut width for spacing even if we don't draw the nut, so diagrams align left
            val reservedNutPx = computedNutPx
            // collect positive frets (used to compute startFret below)
            val positiveFrets = positions.filter { it > 0 }
            // Unified visible window based on uiParams.totalVisibleFrets and caller's preference
            // Use caller's `firstFretIsNut` to decide how to split fractional parts; actual startFret is computed below
            // Decide whether this chord actually starts at a nut or at a fret based on positions
            // Decide chord starts at a fret (no nut) when the smallest positive fret is > 1.
            val chordStartsAtFret = positiveFrets.isNotEmpty() && (positiveFrets.minOrNull() ?: Int.MAX_VALUE) > 1
            val isNutStartPreference = !chordStartsAtFret
             val vw = uiParams.visibleWindow(isNutStartPreference)
             val baseFrets = vw.intFrets.coerceAtLeast(1) // at least 1 fret area to avoid divide-by-zero
             val leftFrac = vw.leftFrac.coerceIn(0f, 1f)
             val rightFrac = vw.rightFrac.coerceIn(0f, 1f)
             // determine actual startFret using the computed baseFrets so marker mapping aligns with visible window
             val startFret = if (firstFretIsNut || positiveFrets.isEmpty() || (positiveFrets.minOrNull() ?: Int.MAX_VALUE) <= 1) {
                1
            } else {
                var s = positiveFrets.minOrNull() ?: 1
                val fMax = positiveFrets.maxOrNull() ?: s
                if (fMax > s + baseFrets - 1) s = fMax - baseFrets + 1
                if (s < 2) s = 2
                s
            }
             // show nut only when startFret == 1 and caller expects a nut
             val nutPx = if (startFret == 1 && firstFretIsNut) computedNutPx else 0f
             // right inset and outer available width (from leftInset to right inset) must be known before computing fretSpacingPx
             val rightInsetPx = with(density) { uiParams.diagramRightInsetDp.toPx() }
             val outerAvailableWidth = (boxWpx - leftInsetPx - rightInsetPx).coerceAtLeast(0f)
             // If nut is shown we must exclude the reserved nut width from the grid available width so that
             // the visible grid (leftFrac + baseFrets + rightFrac) maps exactly to the space used by vertical frets.
             val gridAvailableWidth = if (nutPx > 0f) (outerAvailableWidth - reservedNutPx).coerceAtLeast(0f) else outerAvailableWidth
             val spacingDiv = (baseFrets + leftFrac + rightFrac).coerceAtLeast(1f)
             // Use the gridAvailableWidth so both nut-start and fret-start diagrams align to the same outer boundaries
             val fretSpacingPx = gridAvailableWidth / spacingDiv
             // use provided fretCount
            val stringCount = 6
             // compute fret spacing so 'fretCount' frets fit inside the available width
              // reserve vertical area at bottom for fret labels
              val contentHeightPx = (boxHpx - labelAreaPx).coerceAtLeast(0f)
              val stringSpacingPx = contentHeightPx / (stringCount - 1)

             Canvas(modifier = Modifier.matchParentSize()) {
                 // background (draw only when requested)
                 if (drawBackground) drawRect(Color.White, size = size)
                 // debug overlay: show chord name + computed window values plus uiParam prefs for verification
                 if (uiParams.debugOverlay) {
                     drawContext.canvas.nativeCanvas.apply {
                        val dbgPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.DKGRAY
                            textSize = with(density) { 10.sp.toPx() }
                            isFakeBoldText = true
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }
                        val dbgText0 = "chord=$chordName nut=$firstFretIsNut"
                        val dbgText1 = "total=${uiParams.totalVisibleFrets} base=$baseFrets start=$startFret"
                        val dbgText2 = "left=${"%.2f".format(leftFrac)} right=${"%.2f".format(rightFrac)}"
                        val dbgText3 = "leftPref=${uiParams.leftFractionWhenFretStart} firstPref=${uiParams.firstFretVisibleFraction} lastPref=${uiParams.lastFretVisibleFraction}"
                        // draw right-aligned in the canvas top-right with small padding
                        val pad = with(density) { 6.dp.toPx() }
                        val x = size.width - pad
                        drawText(dbgText0, x, 12f, dbgPaint)
                        drawText(dbgText1, x, 26f, dbgPaint)
                        drawText(dbgText2, x, 40f, dbgPaint)
                        drawText(dbgText3, x, 54f, dbgPaint)
                     }
                 }
                 // nut or vertical frets (shifted by leftInset)
                 // compute stroke half-width and a safe right-edge clamp that respects right inset
                 val vStrokeHalf = with(density) { uiParams.verticalLineWidthDp.toPx() } / 2f
                // Determine leftBoundX: if the diagram starts at a fret (no nut shown), anchor the grid to the leftInsetPx
                // so the grid spans exactly between leftInset..(width-rightInset). If the nut is shown, keep the
                // reservedNutPx to the left of the first vertical fret line.
                val originX = leftInsetPx + reservedNutPx
                // For fret-start diagrams, place the first vertical fret line shifted right by leftFrac * fretSpacing
                // so the left fractional stub extends to the leftInset. For nut-start, keep the origin including nut.
                val leftBoundX = if (startFret > 1) leftInsetPx + leftFrac * fretSpacingPx else originX
                // compute where the visible left stub (partial first fret) should start (leftInset for fret-start)
                val leftStubX = leftBoundX - leftFrac * fretSpacingPx
                 // clamp visible string start to not go beyond left inset
                 val stringStartX = leftStubX.coerceAtLeast(leftInsetPx)
                 val rightBoundX = leftBoundX + (baseFrets + rightFrac) * fretSpacingPx
                 val lastFullFretX = leftBoundX + (baseFrets) * fretSpacingPx
                 val canvasRightLimit = size.width - rightInsetPx - vStrokeHalf
                 val maxFretX = min(canvasRightLimit, lastFullFretX)
                 // draw vertical fret lines starting at leftBoundX so nut-start and fret-start align
                for (lineIdx in 0..baseFrets) {
                    val x = (leftBoundX + lineIdx * fretSpacingPx).coerceAtMost(maxFretX)
                    if (lineIdx == 0 && firstFretIsNut && startFret == 1) {
                        // draw nut rectangle at leftInsetPx (to the left of leftBoundX by reservedNutPx)
                        drawRect(Color.Black, topLeft = Offset(leftInsetPx, 0f), size = androidx.compose.ui.geometry.Size(nutPx, contentHeightPx))
                    } else {
                        drawLine(Color.Gray, start = Offset(x, 0f), end = Offset(x, contentHeightPx), strokeWidth = with(density) { uiParams.verticalLineWidthDp.toPx() })
                    }
                }
                // draw left fractional stub line (partial first fret) only in debug overlay; spacing still respects leftFrac
                if (uiParams.debugOverlay && startFret > 1 && leftFrac > 1e-6f) {
                    val stubXClamped = leftStubX.coerceAtLeast(leftInsetPx)
                    // Only draw if stub is distinct from the first full fret line
                    if (kotlin.math.abs(stubXClamped - leftBoundX) > 1f) {
                        drawLine(Color.LightGray, start = Offset(stubXClamped, 0f), end = Offset(stubXClamped, contentHeightPx), strokeWidth = with(density) { (uiParams.verticalLineWidthDp.toPx() * 0.8f).coerceAtLeast(0.5f) })
                    }
                }
                // draw an extra vertical boundary at the fractional right edge only in debug overlay; spacing still respects rightFrac
                if (uiParams.debugOverlay && rightFrac > 1e-6f) {
                    val rightEdgeX = leftBoundX + (baseFrets + rightFrac) * fretSpacingPx
                    val rightEdgeClamped = min(size.width - rightInsetPx, rightEdgeX)
                     // only draw if sufficiently different from the last full fret line
                    val lastFullLineX = leftBoundX + baseFrets * fretSpacingPx
                    if (kotlin.math.abs(rightEdgeClamped - lastFullLineX) > 1f) {
                        drawLine(Color.LightGray, start = Offset(rightEdgeClamped, 0f), end = Offset(rightEdgeClamped, contentHeightPx), strokeWidth = with(density) { (uiParams.verticalLineWidthDp.toPx() * 0.8f).coerceAtLeast(0.5f) })
                    }
                }

                 // horizontal strings — align start with nut so strings and fret positions share the same origin
                 // If diagram starts past the nut (startFret>1), extend the visible string lines a bit to the left
                 // so users see string stubs to indicate the fretboard continues left. Also draw a thin
                 // vertical fret boundary at the first visible fret to visually mark the fret edge.
                 val stringLineEndX = min(size.width - rightInsetPx, rightBoundX)
                  // No separate short marker here; f==0 above draws the full-height first visible fret.
                 for (s in 0 until stringCount) {
                     val y = s * stringSpacingPx
                     drawLine(Color.Gray, start = Offset(stringStartX, y), end = Offset(stringLineEndX, y), strokeWidth = with(density) { uiParams.horizontalLineWidthDp.toPx() })
                 }

                // Draw red debug boundaries at the canonical visible edges so different formats can be compared
                if (uiParams.debugOverlay) {
                    val dbgPaintW = with(density) { 2.dp.toPx() }
                    val leftEdgeX = leftInsetPx
                    val rightEdgeX = size.width - rightInsetPx
                    drawLine(Color.Red, start = Offset(leftEdgeX, 0f), end = Offset(leftEdgeX, contentHeightPx), strokeWidth = dbgPaintW)
                    drawLine(Color.Red, start = Offset(rightEdgeX, 0f), end = Offset(rightEdgeX, contentHeightPx), strokeWidth = dbgPaintW)
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

                // helper: compute center x for visible fret cell 'rel' (0-based) using leftBoundX as origin
                fun cellCenterX(rel: Int): Float {
                    return leftBoundX + (rel + 0.5f) * fretSpacingPx
                }

                 // draw markers (circles + finger numbers) directly on Canvas for pixel-perfect positioning
                 // but first draw barres behind single-dot markers
                 barres.forEach { barre ->
                     val rel = barre.fret - startFret
                     if (rel in 0 until baseFrets) {
                        // determine vertical span for the barre (y coords depend on invertStrings)
                        val startY = if (invertStrings) barre.startIdx * stringSpacingPx else (stringCount - 1 - barre.startIdx) * stringSpacingPx
                        val endY = if (invertStrings) barre.endIdx * stringSpacingPx else (stringCount - 1 - barre.endIdx) * stringSpacingPx
                        val topY = min(startY, endY)
                        val bottomY = maxOf(startY, endY)
                        val centerX = cellCenterX(rel)
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
                                 if (rel in 0 until baseFrets) {
                                      val x = cellCenterX(rel)
                                     // marker radius derived from UI params with a minimum px size for visibility
                                     val candidate = min(fretSpacingPx, stringSpacingPx) * uiParams.markerRadiusFactor
                                     val minRadiusPx = with(density) { 6.dp.toPx() }
                                     val radius = kotlin.math.max(candidate, minRadiusPx)
                                     drawCircle(color = Color(0xFF339CFF), center = Offset(x, y), radius = radius)
                                     val finger = fingers?.getOrNull(stringIdx) ?: 0
                                     if (finger > 0) {
                                         // draw centered text using nativeCanvas; align baseline so the visual text center equals circle center y
                                         drawContext.canvas.nativeCanvas.apply {
                                             val paint = android.graphics.Paint().apply {
                                                 color = android.graphics.Color.WHITE
                                                 // text size based on marker radius (no artificial minimum to keep optical centering)
                                                 textSize = radius * uiParams.markerTextScale
                                                 isFakeBoldText = true
                                                 textAlign = android.graphics.Paint.Align.CENTER
                                             }
                                             val txt = finger.toString()
                                             // use integer font metrics for stable baseline across environments
                                             val fmInt = paint.fontMetricsInt
                                             val baseline = y - (fmInt.ascent + fmInt.descent) / 2f
                                             drawText(txt, x, baseline, paint)
                                         }
                                     }
                                 }
                             }
                         }
                        // Compute open marker radius first, then derive mute 'X' size so both visually match
                        fretNum == 0 -> {
                            // Anchor open/mute markers to the left inset so their column aligns across nut-start and fret-start
                            val markerAnchorX = (leftInsetPx).coerceAtLeast(0f)
                            val x = (markerAnchorX - markerOffsetPx).coerceAtLeast(0f)
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
                            // Anchor open/mute markers to the left inset so their column aligns across formats
                            val markerAnchorX = (leftInsetPx).coerceAtLeast(0f)
                            val x = (markerAnchorX - markerOffsetPx).coerceAtLeast(0f)
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
                for (f in 1 until baseFrets + 1) {
                     val absoluteFret = startFret + (f - 1)
                     val label = fretLabelProvider?.invoke(absoluteFret) ?: absoluteFret.toString()
                     if (label.isEmpty()) continue
                     // If the diagram starts at a fret (no nut) we omit the first and last fret numeric labels (per request)
                     if (startFret > 1 && (f == 1 || f == baseFrets)) continue
                     // place label at the vertical fret line for this label (same as earlier behavior)
                     val xLabel = (leftBoundX + f * fretSpacingPx).coerceAtMost(maxFretX)
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

                // No numeric badge: when startFret > 1 we show left string stubs and a thin fret boundary line only.
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
    uiParams: DiagramUiParams = defaultDiagramUiParams(),
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
         useCardPadding = false,
         drawBackground = false
      )
 }
