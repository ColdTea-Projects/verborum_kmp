package de.coldtea.verborum.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Color Scheme
object VerborumColors {
    // Light Theme
    val LightBackground = Color(0xFFFFFFFF)
    val LightSurface = Color(0xFFF5F5F5)
    val LightSurfaceAlt = Color(0xFFECECEC)
    val LightText = Color(0xFF1A1A1A)
    val LightTextSecondary = Color(0xFF666666)
    val LightTextTertiary = Color(0xFF999999)
    val LightAccent = Color(0xFFC41E3A)
    val LightGold = Color(0xFFD4AF37)
    val LightBorder = Color(0xFFE0E0E0)

    // Dark Theme
    val DarkBackground = Color(0xFF0F0F0F)
    val DarkSurface = Color(0xFF1A1A1A)
    val DarkSurfaceAlt = Color(0xFF252525)
    val DarkText = Color(0xFFE8E8E8)
    val DarkTextSecondary = Color(0xFFA0A0A0)
    val DarkTextTertiary = Color(0xFF707070)
    val DarkAccent = Color(0xFFE63946)
    val DarkGold = Color(0xFFE6C547)
    val DarkBorder = Color(0xFF333333)
}

/**
 * The on-screen keyboard's own palette.
 *
 * It keeps one dark treatment in both themes, the way an operating system's keyboard does: the panel
 * floats over the page as *chrome* rather than content, and a keyboard that changed colour with the
 * page would read as part of the form underneath it. Defined here rather than in the feature so the
 * values stay in one place with the rest of the palette.
 */
object KeyboardColors {
    val panel = VerborumColors.DarkSurface
    val key = VerborumColors.DarkSurfaceAlt

    /** A hairline a shade lighter than the key, so each one reads as its own cap. */
    val keyBorder = VerborumColors.DarkBorder
    val keyText = VerborumColors.DarkText
    val mutedText = VerborumColors.DarkTextSecondary

    /** Enter, and the selected language tab. */
    val accent = VerborumColors.LightAccent

    /** Shift, which is a mode and wants a colour of its own rather than the accent's. */
    val shift = VerborumColors.LightGold
    val onAccent = VerborumColors.LightBackground
}
