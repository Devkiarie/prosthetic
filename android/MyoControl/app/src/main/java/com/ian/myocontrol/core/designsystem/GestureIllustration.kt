package com.ian.myocontrol.core.designsystem

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.dp
import com.ian.myocontrol.core.theme.McColors
import com.ian.myocontrol.domain.model.GestureLabel
import kotlin.math.*

/**
 * Dynamic Canvas hand illustration — draws a different hand pose for each
 * GestureLabel using pure Compose drawing primitives.
 *
 * The illustration animates smoothly whenever [gesture] changes.
 * Each finger is a rounded rectangle at a configurable angle/length/position.
 */
@Composable
fun GestureIllustration(
    gesture:  GestureLabel,
    modifier: Modifier = Modifier
) {
    // Animate finger curl values (0f = fully open, 1f = fully curled)
    val fingerSpec = AnimationSpec(gesture)

    val thumbCurl  by animateFloatAsState(fingerSpec.thumb,  tween(500, easing = FastOutSlowInEasing), label = "th")
    val indexCurl  by animateFloatAsState(fingerSpec.index,  tween(500, easing = FastOutSlowInEasing), label = "ix")
    val middleCurl by animateFloatAsState(fingerSpec.middle, tween(500, easing = FastOutSlowInEasing), label = "md")
    val ringCurl   by animateFloatAsState(fingerSpec.ring,   tween(500, easing = FastOutSlowInEasing), label = "rg")
    val pinkyCurl  by animateFloatAsState(fingerSpec.pinky,  tween(500, easing = FastOutSlowInEasing), label = "pk")
    val wristAngle by animateFloatAsState(fingerSpec.wristAngle, tween(500, easing = FastOutSlowInEasing), label = "wr")

    val skinColor    = McColors.DecorPeach.copy(alpha = 1f)
    val skinDark     = Color(0xFFD4956A)
    val coralAccent  = McColors.Coral

    Canvas(modifier = modifier.size(120.dp)) {
        val w = size.width
        val h = size.height

        // ── Decorative backdrop circle ──────────────────────────────────────
        drawCircle(
            color  = McColors.HeroCardEnd.copy(alpha = 0.5f),
            radius = w * 0.45f,
            center = Offset(w / 2f, h / 2f)
        )

        // ── Wrist pivot and palm ────────────────────────────────────────────
        val palmCx = w * 0.50f
        val palmCy = h * 0.62f
        val palmW  = w * 0.38f
        val palmH  = h * 0.28f

        // Apply wrist tilt via rotation
        withTransform({
            rotate(degrees = wristAngle, pivot = Offset(palmCx, palmCy + palmH * 0.3f))
        }) {

            // Palm body
            drawRoundRect(
                color        = skinColor,
                topLeft      = Offset(palmCx - palmW / 2, palmCy - palmH / 2),
                size         = Size(palmW, palmH),
                cornerRadius = CornerRadius(palmW * 0.3f)
            )

            // Knuckle shadow line
            drawLine(
                color       = skinDark.copy(alpha = 0.2f),
                start       = Offset(palmCx - palmW * 0.4f, palmCy - palmH * 0.35f),
                end         = Offset(palmCx + palmW * 0.4f, palmCy - palmH * 0.35f),
                strokeWidth = 2.dp.toPx(),
                cap         = StrokeCap.Round
            )

            // ── Fingers (thumb separate, 4 fingers from knuckle line) ──────
            val knuckleLine = palmCy - palmH * 0.35f
            val knuckleGap  = palmW / 4.0f
            val fingerW     = w * 0.085f

            // 4 fingers: index, middle, ring, pinky
            val curls       = listOf(indexCurl, middleCurl, ringCurl, pinkyCurl)
            val baseXOffset = listOf(-1.5f, -0.5f, 0.5f, 1.5f)
            val fingerLengths = listOf(0.28f, 0.32f, 0.28f, 0.22f) // relative to h

            curls.forEachIndexed { i, curl ->
                val baseX = palmCx + baseXOffset[i] * knuckleGap
                val maxLen = h * fingerLengths[i]
                val len    = maxLen * (1f - curl * 0.75f)      // curled = shorter visible segment
                val angle  = 0f + curl * 55f * if (i == 0) 1f else -1f  // slightly fan outward when open

                drawFinger(
                    cx        = baseX,
                    baseY     = knuckleLine,
                    length    = len,
                    width     = fingerW,
                    curlAngle = angle,
                    color     = skinColor,
                    shadow    = skinDark
                )
            }

            // Thumb — positioned on the side
            val thumbBaseX = palmCx - palmW * 0.52f
            val thumbBaseY = palmCy + palmH * 0.05f
            val thumbLen   = h * 0.18f * (1f - thumbCurl * 0.6f)
            val thumbAngle = -55f + thumbCurl * 70f  // 0=extended outward, 1=tucked

            drawFinger(
                cx        = thumbBaseX,
                baseY     = thumbBaseY,
                length    = thumbLen,
                width     = fingerW * 1.15f,
                curlAngle = thumbAngle,
                color     = skinColor,
                shadow    = skinDark,
                isThumb   = true
            )

            // ── Gesture-specific accent dot (coral) ──────────────────────────
            when (gesture) {
                GestureLabel.PINCH -> {
                    // Small coral dot where thumb tip meets index tip
                    val dotX = palmCx - palmW * 0.35f
                    val dotY = knuckleLine - h * 0.12f
                    drawCircle(coralAccent, radius = 5.dp.toPx(), center = Offset(dotX, dotY))
                }
                GestureLabel.THUMBS_UP -> {
                    // Coral accent on thumb tip
                    val dotX = thumbBaseX - thumbLen * 0.6f
                    val dotY = thumbBaseY - thumbLen * 0.2f
                    drawCircle(coralAccent, radius = 5.dp.toPx(), center = Offset(dotX, dotY))
                }
                GestureLabel.POINT -> {
                    // Coral tip on index finger
                    val tipX = palmCx - palmW * 0.95f
                    val tipY = knuckleLine - h * 0.30f
                    drawCircle(coralAccent, radius = 4.dp.toPx(), center = Offset(tipX, tipY))
                }
                else -> Unit
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Draw a single finger segment as a rotated rounded rect
// ──────────────────────────────────────────────────────────────────────────────
private fun DrawScope.drawFinger(
    cx: Float, baseY: Float, length: Float, width: Float,
    curlAngle: Float, color: Color, shadow: Color, isThumb: Boolean = false
) {
    if (length < 2f) return
    withTransform({
        val pivotX = cx
        val pivotY = baseY
        rotate(degrees = curlAngle, pivot = Offset(pivotX, pivotY))
    }) {
        // Finger body
        drawRoundRect(
            color        = color,
            topLeft      = Offset(cx - width / 2, baseY - length),
            size         = Size(width, length),
            cornerRadius = CornerRadius(width * 0.45f)
        )
        // Subtle shading line along finger
        drawLine(
            color       = shadow.copy(alpha = 0.15f),
            start       = Offset(cx + width * 0.25f, baseY - length * 0.9f),
            end         = Offset(cx + width * 0.25f, baseY - length * 0.1f),
            strokeWidth = 1.5f,
            cap         = StrokeCap.Round
        )
        // Knuckle crease dot
        if (!isThumb) {
            drawCircle(
                color  = shadow.copy(alpha = 0.18f),
                radius = width * 0.18f,
                center = Offset(cx, baseY - length * 0.35f)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Finger curl specs per gesture
// ──────────────────────────────────────────────────────────────────────────────
private data class AnimationSpec(
    val thumb:      Float,
    val index:      Float,
    val middle:     Float,
    val ring:       Float,
    val pinky:      Float,
    val wristAngle: Float = 0f
)

private fun AnimationSpec(gesture: GestureLabel): AnimationSpec = when (gesture) {
    GestureLabel.REST        -> AnimationSpec(0.15f, 0.15f, 0.15f, 0.15f, 0.15f)
    GestureLabel.OPEN_HAND   -> AnimationSpec(0.0f,  0.0f,  0.0f,  0.0f,  0.0f)
    GestureLabel.POWER_GRASP -> AnimationSpec(0.9f,  0.95f, 0.95f, 0.95f, 0.95f)
    GestureLabel.PINCH       -> AnimationSpec(0.0f,  0.0f,  0.85f, 0.90f, 0.90f)
    GestureLabel.POINT       -> AnimationSpec(0.85f, 0.0f,  0.90f, 0.90f, 0.90f)
    GestureLabel.WRIST_FLEX  -> AnimationSpec(0.15f, 0.15f, 0.15f, 0.15f, 0.15f, wristAngle = 30f)
    GestureLabel.WRIST_EXT   -> AnimationSpec(0.15f, 0.15f, 0.15f, 0.15f, 0.15f, wristAngle = -30f)
    GestureLabel.THUMBS_UP   -> AnimationSpec(0.0f,  0.90f, 0.90f, 0.90f, 0.90f)
}
