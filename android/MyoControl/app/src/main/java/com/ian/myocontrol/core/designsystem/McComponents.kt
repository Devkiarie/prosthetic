package com.ian.myocontrol.core.designsystem

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ian.myocontrol.core.theme.McColors

// ─────────────────────────────────────────────────────────────────────────────
// McCard — elevated white card (light) / dark surface card (dark mode)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun McCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = modifier.shadow(elevation = 3.dp, shape = RoundedCornerShape(20.dp)),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content  = content
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// McConfidenceBar — animated rounded progress bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun McConfidenceBar(
    confidence:  Float,            // 0.0 – 1.0
    modifier:    Modifier = Modifier,
    height:      Dp = 8.dp,
    trackColor:  Color = MaterialTheme.colorScheme.surfaceVariant,
    fillColor:   Color = McColors.Coral
) {
    val animatedProgress by animateFloatAsState(
        targetValue    = confidence.coerceIn(0f, 1f),
        animationSpec  = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label          = "confidence"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(RoundedCornerShape(height / 2))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(fillColor.copy(alpha = 0.7f), fillColor)
                    )
                )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// McChannelBar — signal quality indicator row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun McChannelBar(
    label:        String,
    rms:          Float,
    channelColor: Color,
    modifier:     Modifier = Modifier
) {
    val quality = when {
        rms >= 0.8f -> "EXCELLENT"
        rms >= 0.6f -> "GOOD"
        rms >= 0.4f -> "FAIR"
        else        -> "POOR"
    }
    val qualityColor = when {
        rms >= 0.8f -> McColors.Success
        rms >= 0.6f -> McColors.Coral
        rms >= 0.4f -> McColors.Warning
        else        -> McColors.Error
    }

    Row(
        modifier          = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.labelMedium,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp)
        )
        McConfidenceBar(
            confidence   = rms,
            modifier     = Modifier.weight(1f),
            height       = 6.dp,
            fillColor    = channelColor
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text     = quality,
            style    = MaterialTheme.typography.labelSmall,
            color    = qualityColor,
            modifier = Modifier.width(68.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// McStatusDot — animated pulsing connection indicator
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun McStatusDot(
    connected: Boolean,
    modifier:  Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = if (connected) 0.4f else 1f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )
    val color = if (connected) McColors.Success else McColors.Error

    Box(
        modifier = modifier
            .size(10.dp)
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = if (connected) 1f else alpha))
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// McStatCard — small 2-line metric tile
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun McStatCard(
    value:      String,
    label:      String,
    modifier:   Modifier = Modifier,
    valueColor: Color = McColors.Coral
) {
    McCard(modifier = modifier) {
        Text(
            text       = value,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            color      = valueColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
