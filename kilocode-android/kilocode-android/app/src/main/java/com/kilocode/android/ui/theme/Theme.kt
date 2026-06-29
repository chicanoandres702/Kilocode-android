package com.kilocode.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Typography ───────────────────────────────────────────────────────────────
// Using system default (San Francisco on Android → Roboto).
// Swap R.font.* for your bundled typeface if needed.
private val AppTypography = Typography(
    displayLarge  = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.W300, letterSpacing = (-0.5).sp, lineHeight = 40.sp),
    displayMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.W300, letterSpacing = (-0.2).sp, lineHeight = 34.sp),
    headlineLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.W500, letterSpacing = (-0.2).sp, lineHeight = 30.sp),
    headlineMedium= TextStyle(fontSize = 18.sp, fontWeight = FontWeight.W500, letterSpacing = (-0.1).sp, lineHeight = 26.sp),
    headlineSmall = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.W500, letterSpacing =   0.sp,   lineHeight = 24.sp),
    titleLarge    = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.W600, letterSpacing = (-0.1).sp, lineHeight = 26.sp),
    titleMedium   = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.W600, letterSpacing =   0.sp,   lineHeight = 22.sp),
    titleSmall    = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W600, letterSpacing =   0.1.sp, lineHeight = 20.sp),
    bodyLarge     = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.W400, letterSpacing =   0.sp,   lineHeight = 24.sp),
    bodyMedium    = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W400, letterSpacing =   0.sp,   lineHeight = 20.sp),
    bodySmall     = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.W400, letterSpacing =   0.1.sp, lineHeight = 18.sp),
    labelLarge    = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W500, letterSpacing =   0.1.sp, lineHeight = 18.sp),
    labelMedium   = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.W500, letterSpacing =   0.3.sp, lineHeight = 16.sp),
    labelSmall    = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.W500, letterSpacing =   0.4.sp, lineHeight = 14.sp),
)

// ── Dark scheme ──────────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary             = Brand,
    onPrimary           = Color.White,
    primaryContainer    = Color(0xFF2D2754),
    onPrimaryContainer  = BrandGlow,

    secondary           = SemanticSuccess,
    onSecondary         = Color(0xFF002114),
    secondaryContainer  = Color(0xFF003B22),
    onSecondaryContainer= Color(0xFF9BFFC4),

    tertiary            = SemanticWarning,
    onTertiary          = Color(0xFF1A0D00),

    error               = SemanticError,
    onError             = Color(0xFF1A0009),
    errorContainer      = SemanticErrorSurface,
    onErrorContainer    = Color(0xFFFFB3BB),

    background          = Ink900,
    onBackground        = Ink50,
    surface             = Ink800,
    onSurface           = Ink50,
    surfaceVariant      = Ink700,
    onSurfaceVariant    = Ink200,
    outline             = Ink600,
    outlineVariant      = Color(0xFF2A2A3C),
    inverseSurface      = Ink50,
    inverseOnSurface    = Ink900,
    inversePrimary      = BrandDim,
    surfaceTint         = Brand,
    scrim               = Color(0xCC000000),
)

// ── Light scheme ─────────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary             = BrandDim,
    onPrimary           = Color.White,
    primaryContainer    = Color(0xFFECE9FF),
    onPrimaryContainer  = Color(0xFF1E1660),

    secondary           = Color(0xFF1A7A55),
    onSecondary         = Color.White,
    secondaryContainer  = Color(0xFFCDFFE4),
    onSecondaryContainer= Color(0xFF002114),

    tertiary            = Color(0xFFA05C00),
    onTertiary          = Color.White,

    error               = Color(0xFFC0394A),
    onError             = Color.White,
    errorContainer      = Color(0xFFFFDADB),
    onErrorContainer    = Color(0xFF3C000A),

    background          = Color(0xFFF4F3FB),
    onBackground        = Color(0xFF11101E),
    surface             = Color(0xFFFFFFFF),
    onSurface           = Color(0xFF11101E),
    surfaceVariant      = Color(0xFFEDEBF8),
    onSurfaceVariant    = Color(0xFF48465E),
    outline             = Color(0xFFBAB8D0),
    outlineVariant      = Color(0xFFDEDCF2),
    inverseSurface      = Color(0xFF2D2C3D),
    inverseOnSurface    = Color(0xFFF3F0FF),
    inversePrimary      = BrandGlow,
)

// ── Theme composable ─────────────────────────────────────────────────────────
@Composable
fun KiloCodeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content     = content,
    )
}
