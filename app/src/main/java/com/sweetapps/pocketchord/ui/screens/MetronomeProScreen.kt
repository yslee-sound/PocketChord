package com.sweetapps.pocketchord.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalView

// Time signature and preset models
data class TimeSignature(val beats: Int, val noteValue: Int)

data class TempoPreset(val name: String, val bpm: Int)

@Composable
fun MetronomeProScreen() {
    var bpm by remember { mutableStateOf(120) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentBeat by remember { mutableStateOf(0) }
    var timeSignature by remember { mutableStateOf(TimeSignature(4, 4)) }

    val tempoPresets = listOf(
        TempoPreset("Largo", 60),
        TempoPreset("Andante", 90),
        TempoPreset("Moderato", 120),
        TempoPreset("Allegro", 140),
        TempoPreset("Vivace", 180),
        TempoPreset("Presto", 200)
    )

    val timeSignatures = listOf(
        TimeSignature(2, 4),
        TimeSignature(3, 4),
        TimeSignature(4, 4),
        TimeSignature(5, 4),
        TimeSignature(6, 4)
    )

    // Tone generator for click sounds
    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }
    DisposableEffect(Unit) { onDispose { try { toneGen.release() } catch (_: Throwable) {} } }

    // Keep screen awake while playing (use LocalView to avoid casting Context to Activity)
    val view = LocalView.current
    DisposableEffect(isPlaying) {
        view.keepScreenOn = isPlaying
        onDispose { view.keepScreenOn = false }
    }

    // Metronome tick logic (sound + visual)
    LaunchedEffect(isPlaying, bpm, timeSignature) {
        if (isPlaying) {
            currentBeat = 0
            val beats = timeSignature.beats
            val interval = (60000 / bpm).toLong().coerceAtLeast(40L)
            while (isPlaying) {
                val tone = if (currentBeat % beats == 0) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP
                try { toneGen.startTone(tone, 60) } catch (_: Throwable) {}
                delay(interval)
                currentBeat = (currentBeat + 1) % beats
            }
        }
    }

    // Colors matching the screenshot style
    val bgSoft = Color(0xFFFAFAF7)
    val cardBorder = Color(0xFFE9E6DE)
    val cardRadius = 20.dp
    val textPrimary = Color(0xFF1F1F1F)
    val textSecondary = Color(0xFF8A8A80)
    val accentDark = Color(0xFF1F1F1F)
    val controlOutline = Color(0xFFE6E3DC)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgSoft)
            .padding(12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize(),
            shape = RoundedCornerShape(cardRadius),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, cardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "메트로놈",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "템포를 설정하고 연습하세요",
                    fontSize = 14.sp,
                    color = textSecondary
                )

                // Beat indicator — hollow circles
                Spacer(Modifier.height(18.dp))
                BeatIndicator(
                    currentBeat = currentBeat,
                    totalBeats = timeSignature.beats,
                    isPlaying = isPlaying,
                    activeColor = accentDark,
                    outlineColor = controlOutline
                )

                // BPM big text
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "$bpm",
                    fontSize = 88.sp,
                    fontWeight = FontWeight.Black,
                    color = textPrimary
                )
                Text(
                    text = "BPM",
                    fontSize = 14.sp,
                    color = textSecondary
                )

                // Step controls
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    OutlinedButton(
                        onClick = { if (bpm > 30) bpm-- },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, controlOutline),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = textPrimary),
                        modifier = Modifier.size(52.dp)
                    ) { Text("−", fontSize = 20.sp, fontWeight = FontWeight.SemiBold) }

                    OutlinedButton(
                        onClick = { if (bpm < 300) bpm++ },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, controlOutline),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = textPrimary),
                        modifier = Modifier.size(52.dp)
                    ) { Text("+", fontSize = 20.sp, fontWeight = FontWeight.SemiBold) }
                }

                // Slider + min/max labels
                Spacer(Modifier.height(14.dp))
                Slider(
                    value = bpm.toFloat(),
                    onValueChange = { bpm = it.roundToInt().coerceIn(30, 300) },
                    valueRange = 30f..300f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = textPrimary,
                        inactiveTrackColor = controlOutline,
                        thumbColor = textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("30", color = textSecondary, fontSize = 12.sp)
                    Text("300", color = textSecondary, fontSize = 12.sp)
                }

                // Time signature
                Spacer(Modifier.height(20.dp))
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                    Text("박자", color = textSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        timeSignatures.forEach { ts ->
                            val selected = timeSignature == ts
                            OutlinedButton(
                                onClick = { timeSignature = ts; currentBeat = 0 },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, controlOutline),
                                colors = if (selected) ButtonDefaults.outlinedButtonColors(
                                    containerColor = accentDark, contentColor = Color.White
                                ) else ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White, contentColor = textPrimary
                                ),
                                modifier = Modifier.height(40.dp).weight(1f)
                            ) {
                                Text("${ts.beats}/${ts.noteValue}", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Play / Pause big button
                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = {
                        isPlaying = !isPlaying
                        if (!isPlaying) currentBeat = 0
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentDark, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isPlaying) "일시정지" else "시작", fontWeight = FontWeight.Bold)
                }

                // Tempo presets grid
                Spacer(Modifier.height(22.dp))
                Column(Modifier.fillMaxWidth()) {
                    Text("템포 프리셋", color = textSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(10.dp))
                    PresetRow(
                        items = tempoPresets.take(3),
                        onClickPreset = { bpm = it },
                        controlOutline = controlOutline,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    PresetRow(
                        items = tempoPresets.drop(3),
                        onClickPreset = { bpm = it },
                        controlOutline = controlOutline,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun BeatIndicator(
    currentBeat: Int,
    totalBeats: Int,
    isPlaying: Boolean,
    activeColor: Color = Color(0xFF1F1F1F),
    outlineColor: Color = Color(0xFFE6E3DC)
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 8.dp)) {
        repeat(totalBeats) { index ->
            val isActive = isPlaying && index == currentBeat
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.1f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "beat_scale"
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .then(
                        if (isActive) Modifier.background(activeColor)
                        else Modifier.border(2.dp, outlineColor, CircleShape)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // no inner text to match the screenshot (empty circles)
            }
        }
    }
}

@Composable
fun TimeSignatureButton(
    timeSignature: TimeSignature,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // replaced by inline buttons in the main UI; keep for reuse if needed
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE6E3DC)),
        colors = if (isSelected) ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF1F1F1F), contentColor = Color.White
        ) else ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White, contentColor = Color(0xFF1F1F1F)
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "${timeSignature.beats}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = "${timeSignature.noteValue}", fontSize = 12.sp)
        }
    }
}

@Composable
fun TempoPresetButton(
    preset: TempoPreset,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE6E3DC)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1F1F1F))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = preset.name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(text = "${preset.bpm}", fontSize = 10.sp, color = Color(0xFF8A8A80))
        }
    }
}

@Composable
private fun PresetRow(
    items: List<TempoPreset>,
    onClickPreset: (Int) -> Unit,
    controlOutline: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { preset ->
            OutlinedButton(
                onClick = { onClickPreset(preset.bpm) },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, controlOutline),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = textPrimary),
                modifier = Modifier.weight(1f).height(40.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(preset.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("${preset.bpm}", fontSize = 11.sp, color = textSecondary)
                }
            }
        }
    }
}
