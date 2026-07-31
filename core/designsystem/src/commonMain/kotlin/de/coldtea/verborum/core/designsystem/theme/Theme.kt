package de.coldtea.verborum.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    background = VerborumColors.DarkBackground,
    surface = VerborumColors.DarkSurface,
    surfaceVariant = VerborumColors.DarkSurfaceAlt,
    onBackground = VerborumColors.DarkText,
    onSurface = VerborumColors.DarkText,
    primary = VerborumColors.DarkAccent,
    secondary = VerborumColors.DarkGold,
    onPrimary = Color.White,
    onSecondary = Color.White,
    outline = VerborumColors.DarkBorder,
    onSurfaceVariant = VerborumColors.DarkTextSecondary,
    tertiary = VerborumColors.DarkTextTertiary,
    // Required Material3 colors
    primaryContainer = VerborumColors.DarkSurface,
    onPrimaryContainer = VerborumColors.DarkText,
    secondaryContainer = VerborumColors.DarkSurface,
    onSecondaryContainer = VerborumColors.DarkText,
    tertiaryContainer = VerborumColors.DarkSurface,
    onTertiaryContainer = VerborumColors.DarkText,
    error = Color(0xFFCF6679),
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inversePrimary = VerborumColors.LightAccent,
    inverseSurface = VerborumColors.LightSurface,
    inverseOnSurface = VerborumColors.LightText,
    surfaceTint = VerborumColors.DarkAccent,
    outlineVariant = VerborumColors.DarkBorder,
    scrim = Color.Black
)

private val LightColorScheme = lightColorScheme(
    background = VerborumColors.LightBackground,
    surface = VerborumColors.LightSurface,
    surfaceVariant = VerborumColors.LightSurfaceAlt,
    onBackground = VerborumColors.LightText,
    onSurface = VerborumColors.LightText,
    primary = VerborumColors.LightAccent,
    secondary = VerborumColors.LightGold,
    onPrimary = Color.White,
    onSecondary = Color.White,
    outline = VerborumColors.LightBorder,
    onSurfaceVariant = VerborumColors.LightTextSecondary,
    tertiary = VerborumColors.LightTextTertiary,
    // Required Material3 colors
    primaryContainer = VerborumColors.LightSurface,
    onPrimaryContainer = VerborumColors.LightText,
    secondaryContainer = VerborumColors.LightSurface,
    onSecondaryContainer = VerborumColors.LightText,
    tertiaryContainer = VerborumColors.LightSurface,
    onTertiaryContainer = VerborumColors.LightText,
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    inversePrimary = VerborumColors.DarkAccent,
    inverseSurface = VerborumColors.DarkSurface,
    inverseOnSurface = VerborumColors.DarkText,
    surfaceTint = VerborumColors.LightAccent,
    outlineVariant = VerborumColors.LightBorder,
    scrim = Color.Black
)

/**
 * The type scale as the platform can actually render it.
 *
 * iOS draws with the system fonts, which cover every script the app supports. The web target draws
 * to a canvas with no system fonts behind it at all: whatever is not in the bundled typeface comes
 * out as empty boxes, so the web actual supplies one that covers the app's scripts.
 */
@Composable
internal expect fun verborumTypography(): Typography

/**
 * The single Material 3 theme for the whole app. Every entry point (iOS, web)
 * and every preview wraps its content in this.
 */
@Composable
fun VerborumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = verborumTypography(),
        content = content,
    )
}
