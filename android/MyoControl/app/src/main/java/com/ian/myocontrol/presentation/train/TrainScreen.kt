package com.ian.myocontrol.presentation.train

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ian.myocontrol.core.designsystem.GestureIllustration
import com.ian.myocontrol.core.theme.McColors
import com.ian.myocontrol.domain.model.GestureLabel

/**
 * TrainScreen — 3-minute calibration session UI.
 *
 * Protocol (matches Android MYOCONTROL_DESIGN.md Phase 4):
 *  Step 0: Idle — "Start Calibration" prompt
 *  Step 1: Instruction — show gesture to perform with illustration
 *  Step 2: Recording — countdown + pulsing indicator
 *  Step 3: Complete — summary
 *
 * When ESP32 is connected, tapping Record sends CMD_RECORD (0x02) via
 * CHAR_CALIBRATION_CMD. For now the UI runs in demo mode with a local timer.
 */

private val CALIBRATION_GESTURES = listOf(
    GestureLabel.REST,
    GestureLabel.OPEN_HAND,
    GestureLabel.POWER_GRASP,
    GestureLabel.PINCH,
    GestureLabel.POINT,
    GestureLabel.WRIST_FLEX,
    GestureLabel.WRIST_EXT,
    GestureLabel.THUMBS_UP,
)
private const val RECORD_SECONDS = 5
private const val REPS_PER_GESTURE = 3

@Composable
fun TrainScreen() {
    var step              by remember { mutableIntStateOf(0) }   // 0=idle, 1=instructing, 2=recording, 3=done
    var gestureIdx        by remember { mutableIntStateOf(0) }
    var repIdx            by remember { mutableIntStateOf(0) }
    var countdown         by remember { mutableIntStateOf(RECORD_SECONDS) }
    var isRecording       by remember { mutableStateOf(false) }
    var completedGestures by remember { mutableIntStateOf(0) }

    // Countdown timer during recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            countdown = RECORD_SECONDS
            repeat(RECORD_SECONDS) {
                kotlinx.coroutines.delay(1000L)
                countdown--
            }
            isRecording = false
            repIdx++
            if (repIdx >= REPS_PER_GESTURE) {
                repIdx = 0
                completedGestures++
                if (gestureIdx + 1 >= CALIBRATION_GESTURES.size) {
                    step = 3 // done
                } else {
                    gestureIdx++
                    step = 1
                }
            } else {
                step = 1 // next rep
            }
        }
    }

    // Pulse animation for recording indicator
    val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.4f, targetValue = 1f, label = "a",
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse)
    )

    val currentGesture = CALIBRATION_GESTURES.getOrElse(gestureIdx) { GestureLabel.REST }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text("Calibration", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth())
        Text("3-minute gesture training session",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(20.dp))

        when (step) {
            // ── IDLE ─────────────────────────────────────────────────────────
            0 -> {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null,
                            tint = McColors.Accent, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Ready to calibrate?", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "You will perform ${CALIBRATION_GESTURES.size} gestures × $REPS_PER_GESTURE reps each " +
                            "(${RECORD_SECONDS}s per rep). Total: ~${CALIBRATION_GESTURES.size * REPS_PER_GESTURE * RECORD_SECONDS / 60} min.",
                            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("Attach electrodes before starting.\nFlex your forearm to verify signal on Monitor tab.",
                            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { step = 1; gestureIdx = 0; repIdx = 0; completedGestures = 0 },
                            colors = ButtonDefaults.buttonColors(containerColor = McColors.Accent),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Start Calibration", fontWeight = FontWeight.SemiBold) }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // All 8 gesture previews
                Text("Gestures you will perform", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                CALIBRATION_GESTURES.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { g ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                GestureIllustration(g, Modifier.size(64.dp))
                                Text(g.displayName, style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp, textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── INSTRUCTING ───────────────────────────────────────────────────
            1 -> {
                // Progress
                LinearProgressIndicator(
                    progress = { (gestureIdx * REPS_PER_GESTURE + repIdx).toFloat() / (CALIBRATION_GESTURES.size * REPS_PER_GESTURE) },
                    modifier = Modifier.fillMaxWidth(),
                    color = McColors.Accent,
                    trackColor = McColors.Accent.copy(alpha = 0.15f)
                )
                Spacer(Modifier.height(4.dp))
                Text("Gesture ${gestureIdx + 1} / ${CALIBRATION_GESTURES.size}  ·  Rep ${repIdx + 1} / $REPS_PER_GESTURE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(24.dp))

                // Big gesture illustration
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GestureIllustration(currentGesture, Modifier.size(160.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(currentGesture.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = McColors.Accent)
                        Spacer(Modifier.height(6.dp))
                        Text(gestureDescription(currentGesture),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text("Hold this position and press Record",
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { step = 2; isRecording = true },
                    colors = ButtonDefaults.buttonColors(containerColor = McColors.Accent),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Record  (${RECORD_SECONDS}s)", fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { step = 0 }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── RECORDING ─────────────────────────────────────────────────────
            2 -> {
                Spacer(Modifier.height(32.dp))
                Box(contentAlignment = Alignment.Center) {
                    GestureIllustration(currentGesture, Modifier.size(160.dp))
                    // Pulsing outer ring
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = McColors.Accent.copy(alpha = pulseAlpha * 0.15f),
                        modifier = Modifier.size(180.dp)
                    ) {}
                }
                Spacer(Modifier.height(24.dp))
                Text("RECORDING", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = McColors.Accent,
                    modifier = Modifier.alpha(pulseAlpha))
                Spacer(Modifier.height(8.dp))
                Text(currentGesture.displayName, style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                // Big countdown
                Text("$countdown", style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold, color = McColors.Accent)
                Text("seconds remaining", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Text("Hold still — maintain the gesture",
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }

            // ── DONE ──────────────────────────────────────────────────────────
            3 -> {
                Spacer(Modifier.height(40.dp))
                Icon(Icons.Default.CheckCircle, contentDescription = null,
                    tint = Color(0xFF4CAF50), modifier = Modifier.size(72.dp))
                Spacer(Modifier.height(16.dp))
                Text("Calibration Complete!", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("${CALIBRATION_GESTURES.size} gestures × $REPS_PER_GESTURE reps recorded.",
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text("The ESP32 will use these recordings to personalise the classifier for your muscles.",
                    style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { step = 0; gestureIdx = 0; repIdx = 0; completedGestures = 0 },
                    colors = ButtonDefaults.buttonColors(containerColor = McColors.Accent),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Recalibrate") }
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

private fun gestureDescription(g: GestureLabel): String = when (g) {
    GestureLabel.REST        -> "Relax your hand completely.\nLet it rest naturally."
    GestureLabel.OPEN_HAND   -> "Spread all fingers wide.\nExtend thumb outward."
    GestureLabel.POWER_GRASP -> "Close your fist firmly.\nAll fingers curl around an imaginary object."
    GestureLabel.PINCH       -> "Touch thumb tip to index finger tip.\nOther fingers curled away."
    GestureLabel.POINT       -> "Extend index finger only.\nOther fingers and thumb curled."
    GestureLabel.WRIST_FLEX  -> "Flex your wrist downward (palm toward forearm).\nFingers relaxed."
    GestureLabel.WRIST_EXT   -> "Extend your wrist upward (back of hand toward forearm).\nFingers relaxed."
    GestureLabel.THUMBS_UP   -> "Extend thumb only, upward.\nOther four fingers curled."
}
