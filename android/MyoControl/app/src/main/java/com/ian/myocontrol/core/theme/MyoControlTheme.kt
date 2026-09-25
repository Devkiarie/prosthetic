package com.ian.myocontrol.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.ian.myocontrol.R

// ── Google Fonts (Manrope — same as ForceMultiplier) ─────────────────────────
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs
)
val ManropeFont       = GoogleFont("Manrope")
val ManropeFontFamily = FontFamily(
    Font(googleFont = ManropeFont, fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = ManropeFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = ManropeFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = ManropeFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = ManropeFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = ManropeFont, fontProvider = provider, weight = FontWeight.ExtraBold),
)

// ── App theme enum ────────────────────────────────────────────────────────────
enum class AppTheme { SYSTEM, LIGHT, DARK }

// ── Typography (Manrope) ─────────────────────────────────────────────────────
internal val McTypography = Typography(
    displayLarge   = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 48.sp, letterSpacing = (-1.5).sp),
    headlineLarge  = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.Bold,      fontSize = 26.sp),
    headlineSmall  = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.Bold,      fontSize = 22.sp),
    titleLarge     = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.SemiBold,  fontSize = 20.sp),
    titleMedium    = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.SemiBold,  fontSize = 16.sp),
    titleSmall     = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.SemiBold,  fontSize = 14.sp),
    bodyLarge      = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.Normal,    fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium     = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.Normal,    fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall      = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.Normal,    fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge     = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.SemiBold,  fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium    = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.SemiBold,  fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall     = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.Medium,    fontSize = 11.sp, letterSpacing = 0.5.sp),
)

// ── Light color scheme (cream default) ───────────────────────────────────────
private val McLightColorScheme = lightColorScheme(
    primary            = McColors.Coral,
    onPrimary          = McColors.LightSurface,
    primaryContainer   = McColors.CoralContainer,
    onPrimaryContainer = McColors.LightOnSurface,
    secondary          = McColors.Success,
    onSecondary        = McColors.LightOnSurface,
    background         = McColors.LightBg,
    onBackground       = McColors.LightOnSurface,
    surface            = McColors.LightSurface,
    onSurface          = McColors.LightOnSurface,
    surfaceVariant     = McColors.LightSurface2,
    onSurfaceVariant   = McColors.LightMuted,
    outline            = McColors.LightOutline,
    error              = McColors.Error,
    onError            = McColors.LightSurface,
)

// ── Dark color scheme (FM absolute black) ────────────────────────────────────
private val McDarkColorScheme = darkColorScheme(
    primary            = McColors.Coral,
    onPrimary          = McColors.DarkBg,
    primaryContainer   = McColors.CoralDim,
    onPrimaryContainer = McColors.DarkOnSurface,
    secondary          = McColors.FMGreen,
    onSecondary        = McColors.DarkBg,
    background         = McColors.DarkBg,
    onBackground       = McColors.DarkOnSurface,
    surface            = McColors.DarkSurface,
    onSurface          = McColors.DarkOnSurface,
    surfaceVariant     = McColors.DarkSurface2,
    onSurfaceVariant   = McColors.DarkMuted,
    outline            = McColors.DarkOutline,
    error              = McColors.Error,
    onError            = McColors.DarkOnSurface,
)

// ── Theme composable ─────────────────────────────────────────────────────────
@Composable
fun MyoControlTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (appTheme) {
        AppTheme.DARK   -> true
        AppTheme.LIGHT  -> false
        AppTheme.SYSTEM -> systemDark
    }
    MaterialTheme(
        colorScheme = if (useDark) McDarkColorScheme else McLightColorScheme,
        typography  = McTypography,
        content     = content
    )
}
