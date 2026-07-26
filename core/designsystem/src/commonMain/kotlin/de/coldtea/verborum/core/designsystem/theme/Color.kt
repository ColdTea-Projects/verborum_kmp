package de.coldtea.verborum.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val Ink = Color(0xFF1B1B1F)
private val Parchment = Color(0xFFFDF7EE)
private val Sepia = Color(0xFF6D5644)
private val SepiaLight = Color(0xFFE8D8C6)
private val Verdigris = Color(0xFF3F7D6E)
private val VerdigrisLight = Color(0xFFB9E4D8)

internal val VerborumLightColors = lightColorScheme(
    primary = Sepia,
    onPrimary = Color.White,
    primaryContainer = SepiaLight,
    onPrimaryContainer = Ink,
    secondary = Verdigris,
    onSecondary = Color.White,
    secondaryContainer = VerdigrisLight,
    onSecondaryContainer = Ink,
    background = Parchment,
    onBackground = Ink,
    surface = Parchment,
    onSurface = Ink,
)

internal val VerborumDarkColors = darkColorScheme(
    primary = SepiaLight,
    onPrimary = Ink,
    primaryContainer = Sepia,
    onPrimaryContainer = Color.White,
    secondary = VerdigrisLight,
    onSecondary = Ink,
    secondaryContainer = Verdigris,
    onSecondaryContainer = Color.White,
    background = Ink,
    onBackground = Parchment,
    surface = Ink,
    onSurface = Parchment,
)
