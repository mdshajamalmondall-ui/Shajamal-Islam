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

private val NexoraDarkColorScheme = darkColorScheme(
    primary = CyanPulse,
    onPrimary = Color(0xFF001F26),
    primaryContainer = Color(0xFF0D3A4C),
    onPrimaryContainer = CyanBright,
    secondary = IndigoNeural,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF232758),
    onSecondaryContainer = IndigoSoft,
    tertiary = EmeraldLife,
    onTertiary = Color(0xFF002116),
    tertiaryContainer = Color(0xFF0A3828),
    onTertiaryContainer = Color(0xFF6EE7B7),
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerHigh = ObsidianSurfaceElevated,
    outline = ObsidianOutline,
    error = CrimsonSos,
    onError = Color.White,
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFCA5A5)
)

private val NexoraLightColorScheme = lightColorScheme(
    primary = CyanDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFFAFE),
    onPrimaryContainer = Color(0xFF0E3A47),
    secondary = IndigoDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = Color(0xFF1E1B4B),
    tertiary = EmeraldDeep,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF064E3B),
    background = DaylightBg,
    onBackground = TextPrimaryLight,
    surface = DaylightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = DaylightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerHigh = Color(0xFFF1F5F9),
    outline = DaylightOutline,
    error = CrimsonSos,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

val NexoraShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NexoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NexoraDarkColorScheme else NexoraLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NexoraTypography,
        shapes = NexoraShapes,
        content = content
    )
}
