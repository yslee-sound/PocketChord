package com.sweetapps.pocketchord

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sweetapps.pocketchord.ui.theme.PocketChordTheme

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
)

// Set an app-wide default maximum diagram width so changing this value updates Preview and devices
// Set default max width to 280.dp per user's request
val DefaultDiagramUiParams = DiagramUiParams(cardHeightDp = 200.dp, diagramRightInsetDp = 16.dp, diagramMaxWidthDp = 300.dp)

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
