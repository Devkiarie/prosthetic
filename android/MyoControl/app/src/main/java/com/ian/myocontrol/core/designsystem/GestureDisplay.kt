package com.ian.myocontrol.core.designsystem

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
 * Maps each GestureLabel to a text symbol.
 * No emojis in source -- these are unicode block characters + arrows used as
 * symbolic hand representations. Safe to use in Text() on all API levels.
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
 * The large gesture display card shown in the center of HomeScreen.
 * Animates between gesture transitions with a crossfade.
 */
@Composable
fun GestureDisplayCard(
    gestureResult: GestureResult?,
    modifier: Modifier = Modifier
) {
    val label = gestureResult?.let { GestureLabel.fromClass(it.gestureClass) } ?: GestureLabel.REST
    val confidence = gestureResult?.confidence ?: 0f
    val latency = gestureResult?.latencyMs ?: 0

    val confidenceColor = when {
        confidence >= 0.90f -> McColors.Success
        confidence >= 0.75f -> McColors.Accent
        confidence >= 0.60f -> McColors.Warning
        else                -> McColors.Error
    }

    McCard(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(McColors.Accent.copy(alpha = 0.3f), McColors.Border)
                ),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Symbol (crossfade on change)
            AnimatedContent(
                targetState = gestureSymbol(label),
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                },
                label = "gesture_symbol"
            ) { symbol ->
                Text(
                    text = symbol,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Light,
                    color = McColors.Accent,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Gesture name (crossfade on change)
            AnimatedContent(
                targetState = label.displayName,
                transitionSpec = {
                    slideInVertically { it / 2 } + fadeIn() togetherWith
                    slideOutVertically { -it / 2 } + fadeOut()
                },
                label = "gesture_name"
            ) { name ->
                Text(
                    text = name.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = McColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Confidence bar + label
            McConfidenceBar(
                confidence = confidence,
                fillColor  = confidenceColor,
                modifier   = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Confidence",
                    style = MaterialTheme.typography.labelSmall,
                    color = McColors.TextSecondary
                )
                Text(
                    text = "${"%.1f".format(confidence * 100)}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = confidenceColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Latency",
                    style = MaterialTheme.typography.labelSmall,
                    color = McColors.TextSecondary
                )
                Text(
                    text = "${latency} ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = McColors.TextSecondary
                )
            }
        }
    }
}
