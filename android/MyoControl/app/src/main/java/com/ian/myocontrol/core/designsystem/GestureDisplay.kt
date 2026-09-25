package com.ian.myocontrol.core.designsystem

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ian.myocontrol.core.theme.McColors
import com.ian.myocontrol.domain.model.GestureLabel
import com.ian.myocontrol.domain.model.GestureResult

/**
 * Maps each GestureLabel to a descriptive unicode symbol.
 * Safe on all API levels — no emoji.
 */
fun gestureSymbol(label: GestureLabel): String = when (label) {
    GestureLabel.REST        -> " -- "
    GestureLabel.OPEN_HAND   -> "[ ]"
    GestureLabel.POWER_GRASP -> "[X]"
    GestureLabel.PINCH       -> "(.)"
    GestureLabel.POINT       -> "[>]"
    GestureLabel.WRIST_FLEX  -> "[v]"
    GestureLabel.WRIST_EXT   -> "[^]"
    GestureLabel.THUMBS_UP   -> "[+]"
}

/**
 * Hero gesture card — warm peach gradient background, decorative circle backdrop,
 * animated gesture name + confidence bar.
 * Matches the reference "Open Hand / 94% CONFIDENCE" card style.
 */
@Composable
fun GestureDisplayCard(
    gestureResult: GestureResult?,
    modifier: Modifier = Modifier
) {
    val label      = gestureResult?.let { GestureLabel.fromClass(it.gestureClass) } ?: GestureLabel.REST
    val confidence = gestureResult?.confidence ?: 0f
    val latency    = gestureResult?.latencyMs ?: 0

    val confidenceColor = when {
        confidence >= 0.90f -> McColors.Success
        confidence >= 0.75f -> McColors.Coral
        confidence >= 0.60f -> McColors.Warning
        else                -> McColors.Error
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(McColors.HeroCardStart, McColors.HeroCardEnd)
                )
            )
            .padding(20.dp)
    ) {
        // Decorative backdrop circle (low-opacity, behind content)
        Box(
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.CenterStart)
                .offset(x = (-20).dp)
                .clip(CircleShape)
                .background(McColors.Coral.copy(alpha = 0.10f))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {

            // Symbol — monospace, crossfades on change
            AnimatedContent(
                targetState = gestureSymbol(label),
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                },
                label = "gesture_symbol"
            ) { symbol ->
                Text(
                    text       = symbol,
                    fontSize   = 52.sp,
                    fontWeight = FontWeight.Light,
                    color      = McColors.Coral,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Gesture name — slides up on change
            AnimatedContent(
                targetState = label.displayName,
                transitionSpec = {
                    slideInVertically { it / 2 } + fadeIn() togetherWith
                    slideOutVertically { -it / 2 } + fadeOut()
                },
                label = "gesture_name"
            ) { name ->
                Text(
                    text      = name.uppercase(),
                    style     = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color     = McColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Confidence bar
            McConfidenceBar(
                confidence = confidence,
                fillColor  = confidenceColor,
                modifier   = Modifier.fillMaxWidth(),
                height     = 8.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Confidence % row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text  = "CONFIDENCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = McColors.TextSecondary,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text       = "${"%.1f".format(confidence * 100)}%",
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = confidenceColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Latency row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text  = "Latency",
                    style = MaterialTheme.typography.labelSmall,
                    color = McColors.TextSecondary
                )
                Text(
                    text  = "${latency} ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = McColors.TextSecondary
                )
            }
        }
    }
}
