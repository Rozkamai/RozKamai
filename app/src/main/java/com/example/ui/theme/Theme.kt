package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = Color.Black,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = GoldLight,
    secondary = EmeraldPrimary,
    onSecondary = Color.White,
    background = SurfaceDark,
    onBackground = TextPrimaryDark,
    surface = CardBackgroundDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF283630),
    onSurfaceVariant = TextSecondaryDark,
    error = Color(0xFFEF5350),
    onError = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldLight,
    onPrimaryContainer = EmeraldDark,
    secondary = GoldDark,
    onSecondary = Color.White,
    secondaryContainer = GoldLight,
    onSecondaryContainer = Color(0xFF5D4037),
    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = CardBackgroundLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFE8F0EC),
    onSurfaceVariant = TextSecondaryLight,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun RozKamaiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
