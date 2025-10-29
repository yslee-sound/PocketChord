package com.sweetapps.pocketchord

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*

/**
 * Diagram UI parameters to centralize visual tuning for fret diagrams.
 * Adjust defaults here to affect all diagrams that consume these params.
 */
data class DiagramUiParams(
    val nutWidthFactor: Float = 0.1f, // fraction of diagram width when nutWidthDp is null
    val nutWidthDp: Dp? = null, // if set, overrides nutWidthFactor
    val markerRadiusFactor: Float = 0.35f, // fraction of min(fretSpacing, stringSpacing)
    val markerTextScale: Float = 1.2f, // multiplier for marker text size relative to radius
    // left inset: space from the canvas left edge to the nut's left edge
    val leftInsetDp: Dp = 8.dp,
    // how far (center) outside the nut to place open/mute markers
    val openMarkerOffsetDp: Dp = 8.dp,
    val verticalLineWidthDp: Dp = 2.dp, // stroke for vertical frets
    val horizontalLineWidthDp: Dp = 2.dp, // stroke for horizontal frets (strings)
    val stringStrokeWidthDp: Dp = 1.6.dp // fallback stroke for strings if separately exposed
)

val DefaultDiagramUiParams = DiagramUiParams()

// Note: This preview uses FretboardDiagramOnly from FretboardUi.kt to visually show uiParams
@Preview(name = "UIParams Preview", showBackground = true, widthDp = 360, heightDp = 200)
@Composable
fun Preview_UIParameters() {
    val previewParams = DefaultDiagramUiParams.copy(
        nutWidthFactor = 0.06f,
        leftInsetDp = 18.dp,
        openMarkerOffsetDp = 10.dp,
        markerRadiusFactor = 0.45f,
        horizontalLineWidthDp = 3.dp
    )

    // Preview uses the same FretboardCard as the app; no need to prepare positions/fingers here.
    Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.TopCenter) {
        // render the same card preview as in FretboardUi
        FretboardCard(
            chordName = "C",
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            uiParams = previewParams
        )
    }
}
