package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = CrispWhite,
    primaryContainer = CobaltContainer,
    onPrimaryContainer = OnCobaltContainer,
    secondary = RoyalPurple,
    onSecondary = CrispWhite,
    secondaryContainer = PurpleContainer,
    onSecondaryContainer = OnPurpleContainer,
    tertiary = SkyAccent,
    onTertiary = CrispWhite,
    tertiaryContainer = SkyContainer,
    onTertiaryContainer = OnSkyContainer,
    background = SoftAlabaster,
    onBackground = InkPrimary,
    surface = CrispWhite,
    onSurface = InkPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = InkSecondary,
    outline = OutlineSubtle,
    error = StatusError,
    onError = CrispWhite,
    errorContainer = StatusErrorContainer,
    onErrorContainer = StatusError
)

private val DarkColorScheme = darkColorScheme(
    primary = CobaltLightDark,
    onPrimary = CobaltDark,
    primaryContainer = CobaltPrimary,
    onPrimaryContainer = CrispWhite,
    secondary = PurpleLightDark,
    onSecondary = OnPurpleContainer,
    secondaryContainer = RoyalPurple,
    onSecondaryContainer = CrispWhite,
    tertiary = SkyLightDark,
    onTertiary = OnSkyContainer,
    tertiaryContainer = SkyAccent,
    onTertiaryContainer = CrispWhite,
    background = MidnightBg,
    onBackground = InkDarkPrimary,
    surface = MidnightSurface,
    onSurface = InkDarkPrimary,
    surfaceVariant = MidnightSurfaceHigh,
    onSurfaceVariant = InkDarkSecondary,
    outline = OutlineDarkSubtle,
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF7F1D1D),
    errorContainer = Color(0xFF991B1B),
    onErrorContainer = CrispWhite
)

private val MbSeekhoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MbSeekhoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MbSeekhoTypography,
        shapes = MbSeekhoShapes,
        content = content
    )
}
