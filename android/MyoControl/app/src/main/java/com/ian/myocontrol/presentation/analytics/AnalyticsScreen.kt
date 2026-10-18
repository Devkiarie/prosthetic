package com.ian.myocontrol.presentation.analytics

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ian.myocontrol.core.theme.McColors
import com.ian.myocontrol.domain.model.GestureLabel

/**
 * AnalyticsScreen — session performance metrics.
 *
 * Displays:
 * - Overall accuracy + F1 for the current session
 * - Per-gesture accuracy bars
 * - Confusion matrix heatmap (8x8)
 * - Session history (last 5 sessions)
 *
 * Data is currently simulated to show layout.
 * Phase 5: wire to Room DB GestureSessionEntity for real accumulated stats.
 */

private val GESTURE_NAMES = GestureLabel.values().map { it.displayName }

@Composable
fun AnalyticsScreen() {
    // Simulated stats (replace with Room DB query in Phase 5)
    val perGestureAccuracy = remember {
        mapOf(
            "Rest" to 0.87f, "Open Hand" to 0.72f, "Power Grasp" to 0.81f,
            "Pinch" to 0.68f, "Point" to 0.74f, "Wrist Flex" to 0.83f,
            "Wrist Extend" to 0.80f, "Thumbs Up" to 0.75f
        )
    }
    val overallAccuracy = perGestureAccuracy.values.average().toFloat()
    val sessionCount = 3

    // Simulated confusion matrix (8x8 normalised)
    val confMatrix = remember {
        Array(8) { row -> FloatArray(8) { col ->
            when {
                row == col -> 0.65f + (row * 0.03f) % 0.25f  // diagonal = correct
                (row == 0 && col == 1) || (row == 1 && col == 0) -> 0.12f  // rest/open confusion
                (row == 3 && col == 2) || (row == 2 && col == 3) -> 0.08f  // pinch/power
                else -> (0.02f + (row * col * 0.003f) % 0.04f)
            }
        }}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text("Analytics", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text("Gesture classification performance",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(16.dp))

        // Summary row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Accuracy", "%.0f%%".format(overallAccuracy * 100),
                McColors.Accent, Modifier.weight(1f))
            StatTile("Macro F1", "%.3f".format(overallAccuracy * 0.97f),
                Color(0xFF4CAF50), Modifier.weight(1f))
            StatTile("Sessions", "$sessionCount",
                Color(0xFF2196F3), Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        // Per-gesture accuracy bars
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Per-gesture accuracy", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                perGestureAccuracy.forEach { (name, acc) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(90.dp), maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface)
                        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)) {
                            Box(Modifier.fillMaxHeight()
                                .fillMaxWidth(acc)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when {
                                        acc >= 0.80f -> Color(0xFF4CAF50)
                                        acc >= 0.65f -> McColors.Accent
                                        else         -> Color(0xFFFF9800)
                                    }
                                ))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("%.0f%%".format(acc * 100),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(34.dp),
                            textAlign = TextAlign.End)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Confusion matrix
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Confusion matrix (predicted × actual)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold)
                Text("Rows = predicted, Columns = actual. Diagonal = correct.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                ConfusionMatrixHeatmap(confMatrix, Modifier.fillMaxWidth())
            }
        }

        Spacer(Modifier.height(14.dp))

        // Model info
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Model info", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                listOf(
                    "Architecture" to "MLP 24→128→64→8 (INT8)",
                    "Size" to "38.2 KB (< 100 KB target)",
                    "Cross-subject F1" to "0.601",
                    "Post-calibration target" to "≥ 0.85",
                    "Dataset" to "NinaPro DB5 (10 subjects)"
                ).forEach { (k, v) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(k, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(148.dp))
                        Text(v, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun StatTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.10f), modifier = modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ConfusionMatrixHeatmap(matrix: Array<FloatArray>, modifier: Modifier = Modifier) {
    val shortNames = listOf("Rest", "Open", "Power", "Pinch", "Point", "WFlex", "WExt", "Thumb")
    val accent = McColors.Accent

    Column(modifier) {
        // Column headers
        Row {
            Spacer(Modifier.width(38.dp))
            shortNames.forEach { name ->
                Text(name, style = MaterialTheme.typography.labelSmall, fontSize = 7.sp,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(2.dp))
        matrix.forEachIndexed { row, rowData ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(shortNames[row], style = MaterialTheme.typography.labelSmall,
                    fontSize = 7.sp, modifier = Modifier.width(38.dp), maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                rowData.forEachIndexed { col, value ->
                    Box(
                        Modifier.weight(1f).aspectRatio(1f)
                            .padding(1.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (row == col) accent.copy(alpha = value.coerceIn(0f, 1f))
                                else Color.Red.copy(alpha = (value * 1.5f).coerceIn(0f, 0.5f))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("%.0f".format(value * 100),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 6.sp,
                            color = if (value > 0.3f) Color.White else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}
