package com.sweetapps.pocketchord

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sweetapps.pocketchord.ui.theme.PocketChordTheme
import androidx.navigation.compose.rememberNavController

// Single source-of-truth for name-box sizing so changing these updates both app and previews
val DEFAULT_NAME_BOX_SIZE_DP: Dp = 60.dp
// default proportional scale used to derive font size from the name box size (boxPx * scale -> fontSp)
val DEFAULT_NAME_BOX_FONT_SCALE: Float = 0.35f //0.45
// Single control for diagram base size: change this one constant to adjust diagram max width and default height used across app & previews
val DEFAULT_DIAGRAM_BASE_DP: Dp = 500.dp
// derive a sensible default diagram width and height from the single base value so changing one number is enough
val DEFAULT_DIAGRAM_MAX_WIDTH_DP: Dp = DEFAULT_DIAGRAM_BASE_DP
val DEFAULT_DIAGRAM_HEIGHT_DP: Dp = (DEFAULT_DIAGRAM_BASE_DP * 0.32f) // ~96.dp when base is 300.dp
// minimum diagram height when deriving from card height
val DEFAULT_DIAGRAM_MIN_HEIGHT_DP: Dp = 72.dp

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
    // use this when you want to move the whole diagram within its container independent of leftInsetDp
    val diagramShiftDp: Dp = 0.dp,
    // right inset: space to reserve on the right side of the card/container so diagram doesn't touch the edge
    val diagramRightInsetDp: Dp = 0.dp,
    // optional maximum width to cap the diagram's width. If null, no explicit cap is applied.
    // Set to null by default so callers can opt-in; DefaultDiagramUiParams below provides a sane app-wide default.
    val diagramMaxWidthDp: Dp? = null,
    // fraction of one fret spacing reserved for the final (last) fret's visible horizontal width.
    // Default 1.0 keeps the previous behavior which effectively used (fretCount + 1) spacing divisor.
    // Set to 0.5 to reserve only half a fret's width for the final fret, etc.
    val lastFretVisibleFraction: Float = 0.3f,
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
)

// Set an app-wide default maximum diagram width so changing this value updates Preview and devices
// Set default max width to 280.dp per user's request
val DefaultDiagramUiParams = DiagramUiParams(
    cardHeightDp = 200.dp,
    diagramRightInsetDp = 16.dp,
    diagramMaxWidthDp = DEFAULT_DIAGRAM_MAX_WIDTH_DP,
    diagramHeightDp = DEFAULT_DIAGRAM_HEIGHT_DP
)

// Note: Keep only the Pixel 7 Pro preview so project preview list is minimal and matches the AVD used by the user.
@Preview(
    name = "UIParams Preview (Pixel 7 Pro)",
    showBackground = true,
    device = Devices.PIXEL_7_PRO,
    showSystemUi = true,
    fontScale = 1f
)
@Composable
fun Preview_UIParameters_Pixel7Pro() {
    PocketChordTheme {
        ChordDetailScreen(root = "C") {}
    }
}

@Preview(
    name = "ChordList Preview (Pixel 7 Pro)",
    showBackground = true,
    device = Devices.PIXEL_7_PRO,
    showSystemUi = true,
    fontScale = 1f
)
@Composable
fun Preview_ChordList_Pixel7() {
    PocketChordTheme {
        val navController = rememberNavController()
        // Explicitly pass uiParams so Preview picks up changes reliably.
        ChordListScreen(navController = navController, root = "C", uiParams = DefaultDiagramUiParams)
    }
}
