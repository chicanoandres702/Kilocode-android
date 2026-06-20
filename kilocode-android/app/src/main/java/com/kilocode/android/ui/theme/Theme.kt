package com.kilocode.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Brand,
    secondary = BrandGlow,
    background = Ink900,
    surface = Ink800,
    surfaceVariant = Ink700,
    onPrimary = Ink50,
    onSecondary = Ink900,
    onBackground = Ink50,
    onSurface = Ink50,
    onSurfaceVariant = Ink200,
    error = SemanticError,
    onError = Ink50,
)

private val LightColorScheme = lightColorScheme(
    primary = BrandDim,
    secondary = Brand,
    background = Color(0xFFF3F5F9),
    surface = Color.White,
    surfaceVariant = Color(0xFFE2E7ED),
    onPrimary = Color.White,
    onSecondary = Ink900,
    onBackground = Ink900,
    onSurface = Ink900,
    onSurfaceVariant = Ink400,
    error = SemanticError,
    onError = Color.White,
)

@Composable
fun KiloCodeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
