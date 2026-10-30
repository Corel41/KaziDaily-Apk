package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class GlassThemeTokens(
    val isDark: Boolean,
    val background: Color,
    val surfaceGlass: Color,
    val surfaceElevated: Color,
    val borderGlass: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val mintAccent: Color,
    val mintContainer: Color,
    val cyanAccent: Color,
    val amberAccent: Color,
    val coralAccent: Color,
    val purpleAccent: Color,
)

val DarkGlassTokens = GlassThemeTokens(
    isDark = true,
    background = PitchBlack,
    surfaceGlass = DarkSurfaceGlass.copy(alpha = 0.78f),
    surfaceElevated = DarkSurfaceElevated.copy(alpha = 0.88f),
    borderGlass = DarkBorderGlass,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textMuted = DarkTextMuted,
    mintAccent = SoftMint,
    mintContainer = SoftMintContainer,
    cyanAccent = SoftIceCyan,
    amberAccent = SoftAmber,
    coralAccent = SoftCoral,
    purpleAccent = SoftPurple
)

val LightGlassTokens = GlassThemeTokens(
    isDark = false,
    background = FrostedWhite,
    surfaceGlass = LightSurfaceGlass.copy(alpha = 0.86f),
    surfaceElevated = LightSurfaceElevated.copy(alpha = 0.92f),
    borderGlass = LightBorderGlass,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textMuted = LightTextMuted,
    mintAccent = Color(0xFF10B981),
    mintContainer = LightMintContainer,
    cyanAccent = Color(0xFF0284C7),
    amberAccent = Color(0xFFD97706),
    coralAccent = Color(0xFFEF4444),
    purpleAccent = Color(0xFF7C3AED)
)

val LocalGlassTokens = staticCompositionLocalOf { DarkGlassTokens }

private val PitchBlackColorScheme = darkColorScheme(
    primary = SoftMint,
    onPrimary = Color(0xFF002315),
    primaryContainer = SoftMintContainer,
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = SoftIceCyan,
    onSecondary = Color(0xFF002234),
    tertiary = SoftAmber,
    onTertiary = Color(0xFF382300),
    background = PitchBlack,
    onBackground = DarkTextPrimary,
    surface = DarkSurfaceGlass,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = Color(0xFF2B3547),
    error = SoftCoral,
    onError = Color(0xFF450A0A)
)

private val FrostedWhiteColorScheme = lightColorScheme(
    primary = Color(0xFF10B981),
    onPrimary = Color.White,
    primaryContainer = LightMintContainer,
    onPrimaryContainer = Color(0xFF065F46),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    background = FrostedWhite,
    onBackground = LightTextPrimary,
    surface = LightSurfaceGlass,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFEF4444),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to pitch black AMOLED mode
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) PitchBlackColorScheme else FrostedWhiteColorScheme
    val glassTokens = if (darkTheme) DarkGlassTokens else LightGlassTokens

    CompositionLocalProvider(LocalGlassTokens provides glassTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
