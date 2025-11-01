package com.sweetapps.pocketchord.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.log2

// --- Data ---

data class GuitarString(
    val name: String,
    val frequency: Double,
    val note: String
)

// --- Screen ---

@Composable
fun GuitarTunerScreen() {
    val guitarStrings = listOf(
        GuitarString("6번줄", 82.41, "E2"),
        GuitarString("5번줄", 110.0, "A2"),
        GuitarString("4번줄", 146.83, "D3"),
        GuitarString("3번줄", 196.0, "G3"),
        GuitarString("2번줄", 246.94, "B3"),
        GuitarString("1번줄", 329.63, "E4"),
    )

    var selectedString by remember { mutableStateOf(guitarStrings[0]) }
    var isListening by remember { mutableStateOf(false) }
    var currentFrequency by remember { mutableStateOf(0.0) }
    var cents by remember { mutableStateOf(0.0) }
    var isTuned by remember { mutableStateOf(false) }

    val audioProcessor = remember { AudioProcessor() }

    // Permission
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )

    // Ensure stop on dispose
    DisposableEffect(Unit) { onDispose { audioProcessor.stopListening() } }

    // Calculate cents difference
    LaunchedEffect(currentFrequency, selectedString) {
        if (currentFrequency > 0) {
            cents = 1200 * log2(currentFrequency / selectedString.frequency)
            isTuned = abs(cents) < 5.0
        }
    }

    // Audio processing lifecycle
    LaunchedEffect(isListening, hasPermission) {
        if (isListening && hasPermission) {
            audioProcessor.startListening { frequency -> currentFrequency = frequency }
        } else {
            audioProcessor.stopListening()
            currentFrequency = 0.0
            cents = 0.0
            isTuned = false
        }
    }

    // Colors to match light design
    val pageBg = Color(0xFFF5F4EE)
    val cardBg = Color.White
    val cardBorder = Color(0xFFE5E3DA)
    val textPrimary = Color(0xFF21211C)
    val textSecondary = Color(0xFF8A8A80)
    val chipBorder = Color(0xFFDCD9CE)
    val chipSelected = Color(0xFF23231F)
    val buttonDark = Color(0xFF23231F)
    val gaugeArc = Color(0xFFE6E5DB)
    val gaugeTick = Color(0xFFC9C7BD)
    val needleColor = Color(0xFF3A3A34)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .padding(16.dp)
    ) {
        OutlinedCard(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, cardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header restored: icon + title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = textPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("기타 튜너", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                }

                Spacer(Modifier.height(18.dp))

                // Big note + freq
                val bigNote = selectedString.note.takeWhile { it.isLetter() }.uppercase()
                Text(bigNote, fontSize = 72.sp, fontWeight = FontWeight.ExtraBold, color = textPrimary)
                Spacer(Modifier.height(4.dp))
                Text("${selectedString.frequency} Hz", color = textSecondary)

                Spacer(Modifier.height(12.dp))

                // Gauge
                MinimalGauge(
                    cents = cents,
                    isListening = isListening,
                    arcColor = gaugeArc,
                    tickColor = gaugeTick,
                    needleColor = needleColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )

                Spacer(Modifier.height(8.dp))

                // Status text
                val statusText = when {
                    !isListening -> "대기 중"
                    isTuned && currentFrequency > 0 -> "정확"
                    currentFrequency <= 0 -> "대기 중"
                    cents > 5 -> "높음"
                    cents < -5 -> "낮음"
                    else -> "미세 조정"
                }
                Text(statusText, color = textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

                Spacer(Modifier.height(12.dp))

                // Start/Stop button (dark)
                Button(
                    onClick = {
                        if (!hasPermission) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) else isListening = !isListening
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonDark, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp)
                ) {
                    Icon(if (isListening) Icons.Filled.MicOff else Icons.Filled.Mic, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isListening) "중지" else "튜닝 시작", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(10.dp))

                // Footer help text
                Text(
                    text = "튜닝할 기타 줄을 선택하고 시작 버튼을 누르세요\n기타를 연주하면 자동으로 음정을 감지합니다",
                    color = textSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TunerChip(
    label: String,
    sub: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
    borderColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) selectedColor else Color.White,
        tonalElevation = if (selected) 0.dp else 1.dp,
        shadowElevation = if (selected) 0.dp else 1.dp,
        border = if (selected) null else BorderStroke(1.dp, borderColor)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(min = 56.dp)
                .padding(vertical = 10.dp, horizontal = 14.dp)
        ) {
            Text(label, color = if (selected) Color.White else textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(sub, color = if (selected) Color(0xFFE6E6E0) else secondaryTextColor, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MinimalGauge(
    cents: Double,
    isListening: Boolean,
    arcColor: Color,
    tickColor: Color,
    needleColor: Color,
    modifier: Modifier = Modifier,
    maxCents: Double = 50.0,
    sweepDeg: Float = 180f,
    needleMaxDeg: Float = 60f,
    stroke: Float = 24f,
    knobRadius: Float = 6f,
    rotorSize: Dp = 36.dp,
) {
    val needleAngle by animateFloatAsState(
        targetValue = ((cents.coerceIn(-maxCents, maxCents) / maxCents) * needleMaxDeg).toFloat(),
        label = "needle_angle",
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
    val displayAngle = if (isListening) needleAngle else 0f

    val shaftColor = Color(0xFF5F5F59)
    val hubColor = Color(0xFF8A8A80)
    val centerTickColor = needleColor

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.68f)
            val radius = size.width * 0.36f

            // base arc
            drawArc(
                color = arcColor,
                startAngle = 180f,
                sweepAngle = sweepDeg,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )

            // ticks outside the arc
            val arcOuter = radius + stroke / 2f
            val tickBase = arcOuter + 6f // gap outside arc
            val centerTickLen = 18f
            val sideTickLen = 12f
            val tickAngles = listOf(-30f, -15f, 0f, 15f, 30f)
            tickAngles.forEach { a ->
                rotate(a, center) {
                    val isCenter = a == 0f
                    val startR = tickBase
                    val endR = tickBase + if (isCenter) centerTickLen else sideTickLen
                    drawLine(
                        color = if (isCenter) centerTickColor else tickColor,
                        start = Offset(center.x, center.y - startR),
                        end = Offset(center.x, center.y - endR),
                        strokeWidth = if (isCenter) 3.2f else 2.2f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // needle shaft thicker
            rotate(displayAngle, center) {
                val shaftEnd = radius * 0.83f
                drawLine(
                    color = shaftColor,
                    start = center,
                    end = Offset(center.x, center.y - shaftEnd),
                    strokeWidth = 14f,
                    cap = StrokeCap.Round
                )
            }

            // top knob
            val top = Offset(center.x, center.y - radius)
            drawCircle(color = needleColor, radius = knobRadius, center = top)

            // bottom foot as thick arc
            val footR = radius * 0.26f
            val footOffsetY = radius * 0.06f
            drawArc(
                color = needleColor,
                startAngle = 205f,
                sweepAngle = 130f,
                useCenter = false,
                style = Stroke(width = 18f, cap = StrokeCap.Round),
                topLeft = Offset(center.x - footR, center.y - footR + footOffsetY),
                size = androidx.compose.ui.geometry.Size(footR * 2, footR * 2)
            )
            drawCircle(color = hubColor, radius = 10f, center = center)
        }
    }
}

// --- Audio Processing ---

private class AudioProcessor {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val sampleRate = 44100
    private val minBufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    @android.annotation.SuppressLint("MissingPermission")
    @androidx.annotation.RequiresPermission(Manifest.permission.RECORD_AUDIO)
    suspend fun startListening(onFrequencyDetected: (Double) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                val bufferSize = if (minBufferSize > 0) minBufferSize else sampleRate
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.DEFAULT,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
                audioRecord?.startRecording()
                isRecording = true

                val buffer = ShortArray(bufferSize)
                while (isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        val freq = detectFrequency(buffer, read)
                        if (freq > 0) {
                            withContext(Dispatchers.Main) { onFrequencyDetected(freq) }
                        }
                    }
                }
            } catch (_: Exception) {
                // ignore, typical when stopping/releasing
            }
        }
    }

    fun stopListening() {
        isRecording = false
        try { audioRecord?.stop() } catch (_: Throwable) {}
        try { audioRecord?.release() } catch (_: Throwable) {}
        audioRecord = null
    }

    private fun detectFrequency(buffer: ShortArray, size: Int): Double {
        if (size <= 0) return 0.0
        val normalized = FloatArray(size) { idx -> buffer[idx] / 32768f }
        // RMS gate
        val rms = kotlin.math.sqrt(normalized.fold(0.0) { acc, f -> acc + (f * f) } / size)
        if (rms < 0.01) return 0.0

        val maxLag = size / 2
        var maxCorrelation = 0f
        var bestLag = 0
        for (lag in 20..maxLag) {
            var corr = 0f
            var i = 0
            while (i + lag < size) {
                corr += normalized[i] * normalized[i + lag]
                i++
            }
            if (corr > maxCorrelation) {
                maxCorrelation = corr
                bestLag = lag
            }
        }
        return if (bestLag > 0) sampleRate.toDouble() / bestLag else 0.0
    }
}
