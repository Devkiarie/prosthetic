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
 * Hero gesture card — warm peach gradient, decorative backdrop circle,
 * dynamic hand illustration (Canvas), animated gesture name + confidence bar.
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
        // Decorative backdrop circle
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 20.dp)
                .clip(CircleShape)
                .background(McColors.Coral.copy(alpha = 0.08f))
        )

        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Left: hand illustration (dynamic Canvas) ─────────────────────
            GestureIllustration(
                gesture  = label,
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // ── Right: name + confidence + latency ───────────────────────────
            Column(modifier = Modifier.weight(1f)) {

                // Gesture name — slides on change
                AnimatedContent(
                    targetState = label.displayName,
                    transitionSpec = {
                        slideInVertically { it / 2 } + fadeIn() togetherWith
                        slideOutVertically { -it / 2 } + fadeOut()
                    },
                    label = "gesture_name"
                ) { name ->
                    Text(
                        text       = name.uppercase(),
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color      = McColors.TextPrimary,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Big confidence number
                AnimatedContent(
                    targetState = "${"%.0f".format(confidence * 100)}%",
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                    },
                    label = "confidence_pct"
                ) { pct ->
                    Text(
                        text       = pct,
                        fontSize   = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = confidenceColor
                    )
                }

                Text(
                    text          = "CONFIDENCE",
                    style         = MaterialTheme.typography.labelSmall,
                    color         = McColors.TextSecondary,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Confidence bar
                McConfidenceBar(
                    confidence = confidence,
                    fillColor  = confidenceColor,
                    modifier   = Modifier.fillMaxWidth(),
                    height     = 6.dp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text  = "${latency} ms latency",
                    style = MaterialTheme.typography.labelSmall,
                    color = McColors.TextSecondary
                )
            }
        }
    }
}
