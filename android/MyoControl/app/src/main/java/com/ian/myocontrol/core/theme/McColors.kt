package com.ian.myocontrol.core.theme

import androidx.compose.ui.graphics.Color

// Dark clinical / engineering palette
object McColors {
    // Backgrounds
    val Background        = Color(0xFF0A0F1E)
    val Surface           = Color(0xFF111827)
    val SurfaceVariant    = Color(0xFF1A2333)
    val CardBackground    = Color(0xFF151E2F)

    // Primary accent (cyan-blue)
    val Accent            = Color(0xFF00D2FF)
    val AccentDim         = Color(0xFF0099BB)
    val AccentContainer   = Color(0xFF002D3D)

    // Status
    val Success           = Color(0xFF00E676)
    val SuccessDim        = Color(0xFF00994D)
    val Warning           = Color(0xFFFFAB00)
    val Error             = Color(0xFFFF1744)
    val ErrorDim          = Color(0xFFAA0000)

    // Text
    val TextPrimary       = Color(0xFFECF0F1)
    val TextSecondary     = Color(0xFF8899AA)
    val TextDisabled      = Color(0xFF445566)

    // Channel colours (for 4-ch waveform)
    val Ch1               = Color(0xFF00D2FF)  // cyan
    val Ch2               = Color(0xFF00E676)  // green
    val Ch3               = Color(0xFFFFAB00)  // amber
    val Ch4               = Color(0xFFFF6B9D)  // rose

    // Border / divider
    val Border            = Color(0xFF1E2D40)
    val Divider           = Color(0xFF1A2530)
}
