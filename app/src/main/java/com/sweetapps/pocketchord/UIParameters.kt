package com.sweetapps.pocketchord

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Diagram UI parameters to centralize visual tuning for fret diagrams.
 * Adjust defaults here to affect all diagrams that consume these params.
 */
data class DiagramUiParams(
    val nutWidthFactor: Float = 0.06f, // fraction of diagram width when nutWidthDp is null
    val nutWidthDp: Dp? = null, // if set, overrides nutWidthFactor
    val markerRadiusFactor: Float = 0.28f, // fraction of min(fretSpacing, stringSpacing)
    val markerTextScale: Float = 1.2f, // multiplier for marker text size relative to radius
    val verticalLineWidthDp: Dp = 2.dp, // stroke for vertical frets
    val horizontalLineWidthDp: Dp = 2.dp, // stroke for horizontal frets (strings)
    val stringStrokeWidthDp: Dp = 1.6.dp // fallback stroke for strings if separately exposed
)

val DefaultDiagramUiParams = DiagramUiParams()

