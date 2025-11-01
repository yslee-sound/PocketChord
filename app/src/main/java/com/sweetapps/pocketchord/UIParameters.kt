package com.sweetapps.pocketchord

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sweetapps.pocketchord.ui.theme.PocketChordTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp

// Single source-of-truth for name-box sizing so changing these updates both app and previews
val DEFAULT_NAME_BOX_SIZE_DP: Dp = 60.dp
// default proportional scale used to derive font size from the name box size (boxPx * scale -> fontSp)
val DEFAULT_NAME_BOX_FONT_SCALE: Float = 0.28f //0.45
// Single control for diagram base size: change this one constant to adjust diagram max width and default height used across app & previews
val DEFAULT_DIAGRAM_BASE_DP: Dp = 500.dp
// derive a sensible default diagram height from the single base value so changing one number is enough
val DEFAULT_DIAGRAM_HEIGHT_DP: Dp = (DEFAULT_DIAGRAM_BASE_DP * 0.32f) // ~96.dp when base is 300.dp
// minimum diagram height when deriving from card height
val DEFAULT_DIAGRAM_MIN_HEIGHT_DP: Dp = 72.dp

// 카드 위아래 간격
val DEFAULT_LIST_ITEM_SPACING_DP: Dp = 20.dp //56

// default card background color (use this to change all card backgrounds). Picked from user's attachment.
val DEFAULT_CARD_BACKGROUND_COLOR = Color(0xFFFF0058)

// default color used for the orange name box shown in chord lists. Set from user's attachment color.
val DEFAULT_NAME_BOX_COLOR = DEFAULT_CARD_BACKGROUND_COLOR

// Anchor option to position the diagram inside its container. Left = diagram sits to left, Right = to right.
enum class DiagramAnchor { Left, Right }

/**
 * Diagram UI parameters to centralize visual tuning for fret diagrams.
 * Adjust defaults here to affect all diagrams that consume these params.
 */
data class DiagramUiParams(
    val nutWidthFactor: Float = 0.03f, // fraction of diagram width when nutWidthDp is null
    val nutWidthDp: Dp? = null, // if set, overrides nutWidthFactor
    val markerRadiusFactor: Float = 0.40f, // fraction of min(fretSpacing, stringSpacing)
    val markerTextScale: Float = 1.5f, // multiplier for marker text size relative to radius
    // left inset: space from the canvas left edge to the nut's left edge
    val leftInsetDp: Dp = 4.dp,
    // how far (center) outside the nut to place open/mute markers (single control for both)
    val markerOffsetDp: Dp = 14.dp,
    // stroke widths for open/mute markers (allows material-like thin outlines)
    val openMarkerStrokeDp: Dp = 1.dp,
    // make mute stroke similar to open by default to improve perceived weight
    val muteMarkerStrokeDp: Dp = 1.dp,
    // size factors used separately for open and mute markers
    val openMarkerSizeFactor: Float = 0.28f,
    val muteMarkerSizeFactor: Float = 0.28f,
    // Barre styling
    // Horizontal thickness as a fraction of one fret spacing (0~1). 0.6 gives a visually balanced width.
    val barreWidthFactor: Float = 0.60f,
    // Extra vertical margin added to the marker radius so the barre fully covers finger circles and numbers
    val barreExtraVerticalMarginDp: Dp = 6.dp,
    // Fill alpha for barre rectangle
    val barreAlpha: Float = 0.25f,
    // when true, contiguous strings with the same fret and finger will be drawn as a single barre rectangle.
    // default false so each string gets its own round marker even for the same finger.
    val drawBarreAsRectangle: Boolean = true,
    // inset factor applied when drawing the mute 'X' (fraction of half-size)
    // use zero inset by default so the 'X' diagonal matches the circle diameter visually
    val muteMarkerInsetFactor: Float = 0f,
    val verticalLineWidthDp: Dp = 1.dp, // stroke for vertical frets
    val horizontalLineWidthDp: Dp = 1.dp, // stroke for horizontal frets (strings)
    // reserved vertical space below strings to render fret numbers/labels
    val fretLabelAreaDp: Dp = 18.dp,
    // label text size in sp
    val fretLabelTextSp: Float = 12f,
    // global shift applied to the whole diagram drawing area (positive -> right, negative -> left)
    // Restore original default shift so diagrams align as in initial design.
    val diagramShiftDp: Dp = 30.dp,
    // right inset: space to reserve on the right side of the card/container so diagram doesn't touch the edge
    val diagramRightInsetDp: Dp = 0.dp,
    // optional maximum width to cap the diagram's width. If null, no explicit cap is applied.
    // Set to null by default so callers can opt-in; DefaultDiagramUiParams below provides a sane app-wide default.
    val diagramMaxWidthDp: Dp? = null,

    // UNIFIED visible window controls
    // total number of frets to show across the entire diagram (integer + fractional part)
    // Example: 5.0f -> show exactly 5 frets; 4.5f -> show 4.5 frets in total.
    val totalVisibleFrets: Float = 4.5f,
    // how much of the left side fractional part to allocate to the left edge when the diagram starts at the nut
    // Typical charts show no left stub with a nut, so default is 0f.
    val leftFractionWhenNutStart: Float = 0f,
    // how much of the left side fractional part to allocate to the left edge when the diagram starts at a fret (no nut)
    // Default 0.3f gives a small stub on the left and the remainder of the fractional part goes to the right.
    val leftFractionWhenFretStart: Float = 0.2f,

    // Legacy knobs (kept for compatibility; ignored when totalVisibleFrets is used by drawing logic)
    // fraction to reserve/show of the first visible fret on the left when not starting at nut.
    val firstFretVisibleFraction: Float = 0.4f,
    // fraction of one fret spacing reserved for the final (last) fret's visible horizontal width.
    val lastFretVisibleFraction: Float = 0.6f,


    // optional default card height (if set, used when a parent doesn't impose a finite height)
    val cardHeightDp: Dp? = null,
    // optional default/fixed diagram height (if set, diagram will use this height instead of being derived from card height)
    val diagramHeightDp: Dp? = DEFAULT_DIAGRAM_HEIGHT_DP,
    // minimum diagram height to enforce when deriving from card height
    val diagramMinHeightDp: Dp = DEFAULT_DIAGRAM_MIN_HEIGHT_DP,
    // size (width/height) of the orange name box shown in chord lists
    val nameBoxSizeDp: Dp = DEFAULT_NAME_BOX_SIZE_DP,
    // proportional scale used to compute font size from nameBoxSizeDp at runtime
    val nameBoxFontScale: Float = DEFAULT_NAME_BOX_FONT_SCALE,
    // anchor to position the diagram inside its available container. Use DefaultDiagramUiParams to set app-wide default.
    val diagramAnchor: DiagramAnchor = DiagramAnchor.Right,
    // when true, show debug overlay text on the canvas (defaults false so previews are clean)
    val debugOverlay: Boolean = false,
 ) {
    // Helper to split totalVisibleFrets into integer and left/right fractional parts according to start mode
    data class VisibleWindow(val intFrets: Int, val leftFrac: Float, val rightFrac: Float, val total: Float)
    fun visibleWindow(firstFretIsNut: Boolean): VisibleWindow {
        val total = totalVisibleFrets.coerceAtLeast(0f)
        val intPart = kotlin.math.floor(total).toInt()
        val frac = total - intPart
        if (frac > 0f) {
            // Normal case: distribute the fractional part to the left (up to desired) and the remainder to the right
            val desiredLeft = if (firstFretIsNut) leftFractionWhenNutStart else leftFractionWhenFretStart
            val left = kotlin.math.min(frac, desiredLeft.coerceIn(0f, 1f))
            val right = (frac - left).coerceIn(0f, 1f)
            return VisibleWindow(intFrets = intPart, leftFrac = left, rightFrac = right, total = total)
        } else {
            // Edge case: total is an integer. For nut-start, keep no fractional edges by default.
            if (firstFretIsNut) {
                return VisibleWindow(intFrets = intPart, leftFrac = 0f, rightFrac = 0f, total = total)
            }
            // Fret-start: prefer explicit leftFractionWhenFretStart when configured (so integer totals like 5.0
            // respect the user's left/right split). If not set, fall back to legacy first/last prefs.
            val desiredLeft = leftFractionWhenFretStart.coerceIn(0f, 1f)
            if (desiredLeft > 1e-6f) {
                val left = desiredLeft
                val right = (1f - left).coerceIn(0f, 1f)
                val base = (intPart - 1).coerceAtLeast(0)
                return VisibleWindow(intFrets = base, leftFrac = left, rightFrac = right, total = total)
            }
            // Fallback: use legacy prefs normalized
            val lPref = firstFretVisibleFraction.coerceAtLeast(0f)
            val rPref = lastFretVisibleFraction.coerceAtLeast(0f)
            val sum = lPref + rPref
            return if (sum <= 1e-6f) {
                VisibleWindow(intFrets = intPart, leftFrac = 0f, rightFrac = 0f, total = total)
            } else {
                val left = (lPref / sum).coerceIn(0f, 1f)
                val right = (rPref / sum).coerceIn(0f, 1f)
                val base = (intPart - 1).coerceAtLeast(0)
                VisibleWindow(intFrets = base, leftFrac = left, rightFrac = right, total = total)
            }
        }
    }
}

// Provide a function to create the app-wide default DiagramUiParams. Using a function ensures
// changes to the underlying DEFAULT_DIAGRAM_BASE_DP are re-evaluated in Previews and at runtime.
fun defaultDiagramUiParams(): DiagramUiParams = DiagramUiParams(
    cardHeightDp = 200.dp,
    diagramRightInsetDp = 16.dp,
    // Use a larger default max width and set default height derived from DEFAULT_DIAGRAM_BASE_DP
    diagramMaxWidthDp = 320.dp,
    diagramHeightDp = DEFAULT_DIAGRAM_HEIGHT_DP,
    // keep legacy prefs aligned
    firstFretVisibleFraction = 0.4f,
    lastFretVisibleFraction = 0.6f
)

// Note: Keep only the Pixel 7 Pro preview so project preview list is minimal and matches the AVD used by the user.
@Preview(
    name = "Fretboard Preview (Pixel 7 Pro)",
    showBackground = true,
    device = Devices.PIXEL_7_PRO,
    // Render preview without system UI so it matches the app content area (status/navigation bars excluded)
    showSystemUi = false,
    fontScale = 1f
)
@Composable
fun Preview_Fretboard_Samples() {
    PocketChordTheme {
        // Recreate the list-row layout used in ChordListScreen so Preview matches runtime.
        // Use top padding similar to runtime LazyColumn content (24.dp vertical content padding + item top margin)
        Column(modifier = Modifier.padding(top = 24.dp, start = 12.dp, end = 12.dp)) {
            val uiParams = defaultDiagramUiParams().copy(debugOverlay = false)
            // Use seed-derived sample data so Preview matches the app's DB-driven diagrams
            val samples = listOf(
                // C: positions [-1,3,2,0,1,0], fingers [0,3,2,0,1,0]
                Triple("C", listOf(-1,3,2,0,1,0), listOf(0,3,2,0,1,0)),
                // Cm (seed): positions [-1,3,5,5,4,3], fingers [0,1,3,4,2,1]
                Triple("Cm", listOf(-1,3,5,5,4,3), listOf(0,1,3,4,2,1)),
                // C7: positions [-1,3,2,3,1,0], fingers [0,3,2,4,1,0]
                Triple("C7", listOf(-1,3,2,3,1,0), listOf(0,3,2,4,1,0))
            )
            samples.forEach { (name, positions, fingers) ->
                // compute diagram and item heights first (match runtime derivation), then derive width from height and cap by max width
                val diagramHeightForList = uiParams.diagramHeightDp ?: uiParams.diagramMinHeightDp
                val itemHeight = maxOf(uiParams.nameBoxSizeDp, diagramHeightForList)
                val heightBasedWidth = itemHeight * (140f / 96f)
                val desiredDiagramWidth = uiParams.diagramMaxWidthDp?.let { mw -> if (heightBasedWidth > mw) mw else heightBasedWidth } ?: heightBasedWidth
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .padding(horizontal = 0.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // name box (fixed size)
                    Box(modifier = Modifier.size(uiParams.nameBoxSizeDp).background(DEFAULT_NAME_BOX_COLOR, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        val chordFontSize = (uiParams.nameBoxSizeDp.value * uiParams.nameBoxFontScale).sp
                        Text(text = name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = chordFontSize)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.width(desiredDiagramWidth).height(diagramHeightForList)) {
                        // Preview: keep debug overlay off for all samples (clean final preview)
                        val perSampleParams = uiParams // debugOverlay remains false
                        FretboardDiagramOnly(modifier = Modifier.fillMaxSize(), uiParams = perSampleParams, positions = positions, fingers = fingers, firstFretIsNut = if (name == "Cm") false else true, invertStrings = false)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
