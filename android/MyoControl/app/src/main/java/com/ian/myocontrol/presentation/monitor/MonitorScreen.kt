package com.ian.myocontrol.presentation.monitor

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ian.myocontrol.core.theme.McColors
import kotlin.math.*
import kotlin.random.Random

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState

/**
 * MonitorScreen — live sEMG signal visualisation.
 *
 * Wired to MonitorViewModel which provides:
 * - Real BLE SignalMetrics when ESP32-S3 is connected
 * - Synthetic waveform in demo mode (shows what live signal looks like)
 */
@Composable
fun MonitorScreen(viewModel: MonitorViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val channelHistory = state.channelHistory
    val signalMetrics  = state.signalMetrics
    val isFakeMode     = state.isFakeMode
    val currentGesture = state.currentGesture

    val channelNames = listOf("CH1 — FCR", "CH2 — ED", "CH3 — FDS", "CH4 — BR")
    val channelColors = listOf(
        McColors.Ch1, McColors.Ch2, McColors.Ch3, McColors.Ch4
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("sEMG Monitor", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = McColors.Accent.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "2 kHz · 250 ms window",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = McColors.Accent
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = "Live forearm sEMG — 4 channels",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(14.dp))

        // One waveform card per channel
        channelHistory.forEachIndexed { ch, history ->
            WaveformCard(
                label     = channelNames[ch],
                color     = channelColors[ch],
                samples   = history.toList(),
                modifier  = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(6.dp))

        // Info card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Signal acquisition info", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                listOf(
                    "Sampling rate" to "2,000 Hz per channel",
                    "Window" to "250 ms (500 samples) at 80% overlap",
                    "ADC" to "ESP32-S3 ADC1 GPIO1–4, 12-bit, 0.15–2.45 V",
                    "Amplifier" to "INA128 G=51 → BPF 20-100 Hz → Notch 50 Hz"
                ).forEach { (k, v) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(k, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(120.dp))
                        Text(v, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }

        Spacer(Modifier.height(80.dp)) // nav bar clearance
    }
}

@Composable
private fun WaveformCard(
    label: String,
    color: Color,
    samples: List<Float>,
    modifier: Modifier = Modifier
) {
    // Compute RMS for quality indicator
    val rms = if (samples.isEmpty()) 0f else
        sqrt(samples.map { it * it }.average().toFloat())
    val quality = when {
        rms > 0.5f -> "ACTIVE"
        rms > 0.15f -> "GOOD"
        rms > 0.03f -> "WEAK"
        else -> "QUIET"
    }
    val qualityColor = when (quality) {
        "ACTIVE" -> McColors.Accent
        "GOOD"   -> Color(0xFF4CAF50)
        "WEAK"   -> Color(0xFFFF9800)
        else     -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape  = RoundedCornerShape(12.dp),
        color  = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(10.dp).clip(RoundedCornerShape(50))
                        .background(color)
                )
                Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.weight(1f))
                Text(quality, style = MaterialTheme.typography.labelSmall,
                    color = qualityColor, fontSize = 10.sp)
                Spacer(Modifier.width(6.dp))
                Text("RMS %.2f".format(rms), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            }

            Spacer(Modifier.height(8.dp))

            Canvas(modifier = Modifier.fillMaxWidth().height(56.dp)) {
                val w  = size.width
                val h2 = size.height / 2f
                val step = w / (samples.size - 1).coerceAtLeast(1)

                // Grid line at zero
                drawLine(color.copy(alpha = 0.08f), Offset(0f, h2), Offset(w, h2), 1f)

                // Waveform
                val path = Path()
                samples.forEachIndexed { i, v ->
                    val x = i * step
                    val y = h2 - v * h2 * 0.85f
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color, style = Stroke(1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // Fill under curve
                val fillPath = Path().also { fp ->
                    fp.addPath(path)
                    fp.lineTo((samples.size - 1) * step, h2)
                    fp.lineTo(0f, h2)
                    fp.close()
                }
                drawPath(fillPath, Brush.verticalGradient(
                    listOf(color.copy(alpha = 0.18f), color.copy(alpha = 0.02f)),
                    startY = 0f, endY = size.height))
            }
        }
    }
}
