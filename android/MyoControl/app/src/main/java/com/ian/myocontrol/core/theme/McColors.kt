package com.ian.myocontrol.core.theme

import androidx.compose.ui.graphics.Color

/**
 * MyoControl design-system color tokens.
 *
 * Light (default) = warm cream — reference UI
 * Dark (optional) = ForceMultiplier absolute black
 *
 * Semantic aliases at the bottom resolve to the light-mode values so they
 * can be used in non-Composable contexts (Scaffold containerColor, etc.).
 * Dynamic mode-switching is handled by MyoControlTheme / MaterialTheme.colorScheme.
 */
object McColors {

    // ── Brand accent: coral-salmon (primary CTA, active nav, hero) ────────────
    val Coral            = Color(0xFFF07860)
    val CoralDim         = Color(0xFFCC5540)
    val CoralContainer   = Color(0xFFFFF0ED)   // very light peach
    val CoralGlow        = Color(0x33F07860)   // 20% opacity coral

    // ── ForceMultiplier green (success states, dark-mode secondary) ───────────
    val FMGreen          = Color(0xFF2ED573)
    val FMGreenDim       = Color(0xFF1AAD57)

    // ── Semantic status ───────────────────────────────────────────────────────
    val Success          = Color(0xFF2ECC8A)
    val Warning          = Color(0xFFF5A623)
    val Error            = Color(0xFFE8705A)
    val ErrorDim         = Color(0xFFCC3322)
    val Info             = Color(0xFF60B8F0)

    // ── 4-channel waveform colours ────────────────────────────────────────────
    val Ch1              = Color(0xFFF07860)   // coral
    val Ch2              = Color(0xFF4ECBA0)   // mint
    val Ch3              = Color(0xFFA78BFA)   // lavender
    val Ch4              = Color(0xFF60B8F0)   // sky blue

    // ── LIGHT theme — cream default ───────────────────────────────────────────
    val LightBg          = Color(0xFFF9F6F2)
    val LightSurface     = Color(0xFFFFFFFF)
    val LightSurface2    = Color(0xFFF5F0EC)
    val LightOutline     = Color(0xFFEAE5E0)
    val LightOnSurface   = Color(0xFF1A1A1A)
    val LightMuted       = Color(0xFF888888)
    val LightDivider     = Color(0xFFEEEEEE)

    // ── DARK theme — FM absolute black ────────────────────────────────────────
    val DarkBg           = Color(0xFF080808)
    val DarkSurface      = Color(0xFF111111)
    val DarkSurface2     = Color(0xFF171717)
    val DarkOutline      = Color(0xFF262626)
    val DarkOnSurface    = Color(0xFFFFFFFF)
    val DarkMuted        = Color(0xFF6B6B6B)
    val DarkDivider      = Color(0xFF1F1F1F)

    // ── Hero card gradient (gesture display) ─────────────────────────────────
    val HeroCardStart    = Color(0xFFFFF5F2)   // near-white peach
    val HeroCardEnd      = Color(0xFFFAD5C8)   // warm peach

    // ── Decorative background blobs (home screen) ─────────────────────────────
    val DecorPeach       = Color(0xFFF5C4B0)
    val DecorBeige       = Color(0xFFE8D5C0)
    val DecorSage        = Color(0xFFC8D5C0)

    // ── Semantic aliases — light-mode defaults ────────────────────────────────
    // Use MaterialTheme.colorScheme.* in Composables for auto dark-mode support.
    // These constants are for Scaffold/NavigationBar containerColor parameters.
    val Accent           = Coral
    val AccentContainer  = CoralContainer
    val Background       = LightBg
    val Surface          = LightSurface
    val SurfaceVariant   = LightSurface2
    val Border           = LightOutline
    val TextPrimary      = LightOnSurface
    val TextSecondary    = LightMuted
}
