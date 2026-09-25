package com.ian.myocontrol.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val McDarkColorScheme = darkColorScheme(
    primary          = McColors.Accent,
    onPrimary        = McColors.Background,
    primaryContainer = McColors.AccentContainer,
    secondary        = McColors.AccentDim,
    background       = McColors.Background,
    surface          = McColors.Surface,
    surfaceVariant   = McColors.SurfaceVariant,
    error            = McColors.Error,
    onBackground     = McColors.TextPrimary,
    onSurface        = McColors.TextPrimary,
    onSurfaceVariant = McColors.TextSecondary,
    outline          = McColors.Border,
)

private val McTypography = Typography(
    displayLarge  = TextStyle(fontSize = 57.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium= TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    titleLarge    = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium),
    titleMedium   = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyLarge     = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium    = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall     = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelLarge    = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium   = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall    = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun MyoControlTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = McDarkColorScheme,
        typography  = McTypography,
        content     = content
    )
}
