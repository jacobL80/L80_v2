package com.jacobleighty.musictracker.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val pageBg: Color,
    val cardBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDim: Color,
    val textDimmer: Color,
    val border: Color,
    val borderInput: Color,
    val surfaceHover: Color,
    val surfaceAlt: Color,
    // Category accent-light backgrounds
    val musicAccentLight: Color,
    val concertAccentLight: Color,
    val tvAccentLight: Color,
    val runAccentLight: Color,
    // Section-specific backgrounds
    val musicExpectedBg: Color,
    val tvExpectedBg: Color,
    // Music chart colors
    val chartBand: Color,
    val chartGrid: Color,
    val chartBase: Color,
)

val LightAppColors = AppColors(
    pageBg           = Color(0xFFFAF9F7),
    cardBg           = Color(0xFFFFFFFF),
    textPrimary      = Color(0xFF1A1A1A),
    textSecondary    = Color(0xFF888888),
    textDim          = Color(0xFFBBBBBB),
    textDimmer       = Color(0xFFCCCCCC),
    border           = Color(0xFFE8E8E8),
    borderInput      = Color(0xFFDDDDDD),
    surfaceHover     = Color(0xFFF5F5F5),
    surfaceAlt       = Color(0xFFFAFAFA),
    musicAccentLight = Color(0xFFFFF4E6),
    concertAccentLight = Color(0xFFE8F7FB),
    tvAccentLight    = Color(0xFFF3EEFF),
    runAccentLight   = Color(0xFFEDF7E8),
    musicExpectedBg  = Color(0xFFF2FAFD),
    tvExpectedBg     = Color(0xFFF5F0FF),
    chartBand        = Color(0xFFF5F3F0),
    chartGrid        = Color(0xFFF0EEEB),
    chartBase        = Color(0xFFCEC9C3),
)

val DarkAppColors = AppColors(
    pageBg           = Color(0xFF121212),
    cardBg           = Color(0xFF1C1C1C),
    textPrimary      = Color(0xFFE5E5E5),
    textSecondary    = Color(0xFF9A9A9A),
    textDim          = Color(0xFF555555),
    textDimmer       = Color(0xFF444444),
    border           = Color(0xFF2A2A2A),
    borderInput      = Color(0xFF383838),
    surfaceHover     = Color(0xFF242424),
    surfaceAlt       = Color(0xFF181818),
    musicAccentLight = Color(0x23EC6F00),  // rgba(236,111,0,0.14)
    concertAccentLight = Color(0x231696B6), // rgba(22,150,182,0.14)
    tvAccentLight    = Color(0x237C3AED),  // rgba(124,58,237,0.14)
    runAccentLight   = Color(0x2347A025),  // rgba(71,160,37,0.14)
    musicExpectedBg  = Color(0x1A1696B6),  // rgba(22,150,182,0.10)
    tvExpectedBg     = Color(0x1A7C3AED),  // rgba(124,58,237,0.10)
    chartBand        = Color(0xFF212121),
    chartGrid        = Color(0xFF2A2A2A),
    chartBase        = Color(0xFF404040),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

var currentAppColors: AppColors = LightAppColors
