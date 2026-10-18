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
 * Anatomically improved Canvas hand illustration v2.
 *
 * Improvements over v1:
 *  - 3-segment phalanges per finger (proximal / middle / distal) with independent rotation
 *  - Fingernail on distal phalanx (nail plate, lunula, highlight)
 *  - Knuckle fold lines at each joint
 *  - Palm has trapezoid shape with thenar + hypothenar eminences
 *  - Thumb positioned correctly on radial side with correct abduction arc
 *  - Palm crease lines and drop shadow for depth
 *  - Wrist rotation applies to whole hand assembly via pivot transform
 */
@Composable
fun GestureIllustration(
    gesture:  GestureLabel,
    modifier: Modifier = Modifier
) {
    val spec = fingerSpec(gesture)

    val thumbCurl  by animateFloatAsState(spec.thumb,      tween(500, easing = FastOutSlowInEasing), label = "th")
    val indexCurl  by animateFloatAsState(spec.index,      tween(500, easing = FastOutSlowInEasing), label = "ix")
    val middleCurl by animateFloatAsState(spec.middle,     tween(500, easing = FastOutSlowInEasing), label = "md")
    val ringCurl   by animateFloatAsState(spec.ring,       tween(500, easing = FastOutSlowInEasing), label = "rg")
    val pinkyCurl  by animateFloatAsState(spec.pinky,      tween(500, easing = FastOutSlowInEasing), label = "pk")
    val wristAngle by animateFloatAsState(spec.wristAngle, tween(600, easing = FastOutSlowInEasing), label = "wr")

    val skinBase   = Color(0xFFF5C4A0)
    val skinMid    = Color(0xFFE8A87C)
    val skinDark   = Color(0xFFD4844A)
    val nailColor  = Color(0xFFFFE8E8)
    val nailBorder = Color(0xFFD4956A)
    val backdropC  = McColors.HeroCardEnd.copy(alpha = 0.45f)
    val coralAcc   = McColors.Coral

    Canvas(modifier = modifier.size(140.dp)) {
        val w = size.width
        val h = size.height

        drawCircle(color = backdropC, radius = w * 0.47f, center = Offset(w * 0.50f, h * 0.52f))

        val pivotX = w * 0.50f
        val pivotY = h * 0.82f

        withTransform({ rotate(degrees = wristAngle, pivot = Offset(pivotX, pivotY)) }) {

            // Drop shadow
            drawOval(
                color   = Color.Black.copy(alpha = 0.08f),
                topLeft = Offset(w * 0.20f, h * 0.72f),
                size    = Size(w * 0.60f, h * 0.10f)
            )

            val palmTop  = h * 0.52f
            val palmBot  = h * 0.80f
            val palmCx   = w * 0.50f
            val palmTopW = w * 0.42f
            val palmBotW = w * 0.36f

            // Palm body
            drawRoundRect(
                color        = skinBase,
                topLeft      = Offset(palmCx - palmTopW / 2, palmTop),
                size         = Size(palmTopW, palmBot - palmTop),
                cornerRadius = CornerRadius(w * 0.08f)
            )
            // Thenar eminence
            drawOval(
                color   = skinMid.copy(alpha = 0.30f),
                topLeft = Offset(palmCx - palmTopW / 2 - w * 0.03f, palmTop + h * 0.06f),
                size    = Size(w * 0.20f, h * 0.13f)
            )
            // Hypothenar eminence
            drawOval(
                color   = skinMid.copy(alpha = 0.22f),
                topLeft = Offset(palmCx + palmTopW / 2 - w * 0.17f, palmTop + h * 0.08f),
                size    = Size(w * 0.15f, h * 0.09f)
            )
            // Palm crease lines
            drawLine(skinDark.copy(alpha = 0.12f),
                Offset(palmCx - palmTopW * 0.35f, palmTop + h * 0.05f),
                Offset(palmCx + palmTopW * 0.28f, palmTop + h * 0.07f), 1.5f, cap = StrokeCap.Round)
            drawLine(skinDark.copy(alpha = 0.09f),
                Offset(palmCx - palmTopW * 0.30f, palmTop + h * 0.11f),
                Offset(palmCx + palmTopW * 0.22f, palmTop + h * 0.12f), 1.2f, cap = StrokeCap.Round)

            // 4 main fingers
            val knuckleY    = palmTop + h * 0.01f
            val fingerPitch = palmTopW / 4.2f

            data class FC(val cx: Float, val curl: Float, val lens: List<Float>, val fw: Float)
            val fingers = listOf(
                FC(palmCx - fingerPitch * 1.45f, indexCurl,  listOf(0.135f, 0.108f, 0.082f), 0.076f),
                FC(palmCx - fingerPitch * 0.45f, middleCurl, listOf(0.150f, 0.118f, 0.090f), 0.082f),
                FC(palmCx + fingerPitch * 0.48f, ringCurl,   listOf(0.136f, 0.106f, 0.080f), 0.076f),
                FC(palmCx + fingerPitch * 1.42f, pinkyCurl,  listOf(0.103f, 0.080f, 0.062f), 0.064f),
            )
            fingers.forEach { f ->
                drawSegmentedFinger(f.cx, knuckleY, f.curl, f.lens.map { it * h }, f.fw * w,
                    skinBase, skinMid, skinDark, nailColor, nailBorder, false)
            }

            // Thumb
            val thumbBX = palmCx - palmTopW * 0.52f
            val thumbBY = palmTop + h * 0.10f
            val thumbBaseAngle = -62f + thumbCurl * 82f
            drawSegmentedFinger(thumbBX, thumbBY, thumbCurl, listOf(h*0.093f, h*0.080f, h*0.062f), w*0.088f,
                skinBase, skinMid, skinDark, nailColor, nailBorder, true, thumbBaseAngle)

            // Gesture accents
            when (gesture) {
                GestureLabel.PINCH ->
                    drawCircle(coralAcc, 5.dp.toPx(), Offset(palmCx - palmTopW * 0.36f, knuckleY - h * 0.07f))
                GestureLabel.POINT ->
                    drawCircle(coralAcc, 4.dp.toPx(), Offset(palmCx - fingerPitch * 1.45f, knuckleY - h * 0.32f))
                GestureLabel.THUMBS_UP ->
                    drawCircle(coralAcc, 4.dp.toPx(), Offset(thumbBX - w * 0.09f, thumbBY - h * 0.21f))
                GestureLabel.POWER_GRASP ->
                    drawArc(coralAcc.copy(alpha = 0.18f), 200f, 140f, false,
                        Offset(palmCx - palmTopW * 0.50f, knuckleY - h * 0.05f),
                        Size(palmTopW, h * 0.10f), style = Stroke(2.5f, cap = StrokeCap.Round))
                else -> Unit
            }

            // Wrist stub
            drawRoundRect(
                color        = skinMid.copy(alpha = 0.65f),
                topLeft      = Offset(palmCx - palmBotW * 0.48f, palmBot - h * 0.01f),
                size         = Size(palmBotW * 0.96f, h * 0.07f),
                cornerRadius = CornerRadius(w * 0.06f)
            )
        }
    }
}

private fun DrawScope.drawSegmentedFinger(
    cx: Float, baseY: Float,
    curl: Float,
    segLengths: List<Float>,
    width: Float,
    skinBase: Color, skinMid: Color, skinDark: Color,
    nailColor: Color, nailBorder: Color,
    isThumb: Boolean = false,
    baseAngle: Float = 0f
) {
    if (segLengths.any { it < 2f }) return
    val proxAngle = if (isThumb) baseAngle else curl * 70f
    val midAngle  = curl * 50f
    val distAngle = curl * 30f
    var cx2 = cx; var cy2 = baseY

    segLengths.forEachIndexed { idx, segLen ->
        val segAngle = when (idx) {
            0 -> proxAngle
            1 -> proxAngle + midAngle
            else -> proxAngle + midAngle + distAngle
        }
        val rad = Math.toRadians(segAngle.toDouble())
        val dx  = (-sin(rad) * segLen).toFloat()
        val dy  = (-cos(rad) * segLen).toFloat()
        val segW = width * (1f - idx * 0.11f)

        withTransform({ rotate(degrees = segAngle, pivot = Offset(cx2, cy2)) }) {
            drawRoundRect(skinBase, Offset(cx2 - segW / 2, cy2 - segLen), Size(segW, segLen), CornerRadius(segW * 0.42f))
            // Side gradient shading
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(skinMid.copy(0f), skinMid.copy(0.16f), skinMid.copy(0f)),
                    cx2 - segW / 2, cx2 + segW / 2),
                topLeft = Offset(cx2 - segW / 2, cy2 - segLen), size = Size(segW, segLen), cornerRadius = CornerRadius(segW * 0.42f))
            // Knuckle fold
            if (idx < 2) {
                val fy = cy2 - segLen * 0.08f
                drawLine(skinDark.copy(0.18f), Offset(cx2 - segW * 0.33f, fy), Offset(cx2 + segW * 0.33f, fy), 1.2f, cap = StrokeCap.Round)
                drawLine(skinDark.copy(0.09f), Offset(cx2 - segW * 0.26f, fy - segLen * 0.04f), Offset(cx2 + segW * 0.26f, fy - segLen * 0.04f), 0.8f, cap = StrokeCap.Round)
            }
            // Fingernail on distal
            if (idx == 2) {
                val nailW = segW * 0.66f; val nailH = segLen * 0.38f
                val nailTop = cy2 - segLen * 0.88f
                drawRoundRect(nailColor, Offset(cx2 - nailW / 2, nailTop), Size(nailW, nailH), CornerRadius(nailW * 0.40f))
                drawRoundRect(nailBorder.copy(0.32f), Offset(cx2 - nailW / 2, nailTop), Size(nailW, nailH), CornerRadius(nailW * 0.40f), style = Stroke(0.8f))
                drawLine(Color.White.copy(0.52f), Offset(cx2 - nailW * 0.16f, nailTop + nailH * 0.18f), Offset(cx2 + nailW * 0.06f, nailTop + nailH * 0.52f), 1.0f, cap = StrokeCap.Round)
                drawArc(Color.White.copy(0.42f), 180f, 180f, false,
                    Offset(cx2 - nailW * 0.28f, nailTop + nailH * 0.55f), Size(nailW * 0.56f, nailH * 0.36f))
            }
        }
        cx2 += dx; cy2 += dy
    }
}

private data class FingerSpec(
    val thumb: Float, val index: Float, val middle: Float, val ring: Float, val pinky: Float,
    val wristAngle: Float = 0f
)

private fun fingerSpec(g: GestureLabel): FingerSpec = when (g) {
    GestureLabel.REST        -> FingerSpec(0.10f, 0.10f, 0.10f, 0.10f, 0.10f)
    GestureLabel.OPEN_HAND   -> FingerSpec(0.00f, 0.00f, 0.00f, 0.00f, 0.00f)
    GestureLabel.POWER_GRASP -> FingerSpec(0.92f, 0.95f, 0.95f, 0.95f, 0.95f)
    GestureLabel.PINCH       -> FingerSpec(0.05f, 0.05f, 0.90f, 0.92f, 0.92f)
    GestureLabel.POINT       -> FingerSpec(0.88f, 0.00f, 0.92f, 0.92f, 0.92f)
    GestureLabel.WRIST_FLEX  -> FingerSpec(0.10f, 0.10f, 0.10f, 0.10f, 0.10f, wristAngle = 28f)
    GestureLabel.WRIST_EXT   -> FingerSpec(0.10f, 0.10f, 0.10f, 0.10f, 0.10f, wristAngle = -28f)
    GestureLabel.THUMBS_UP   -> FingerSpec(0.00f, 0.92f, 0.92f, 0.92f, 0.92f)
}
