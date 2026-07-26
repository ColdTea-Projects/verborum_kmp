package de.coldtea.verborum.core.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * Maximum content measures. A browser window can be far wider than any comfortable line length or
 * control, so screens cap their content rather than letting it stretch across a desktop monitor.
 */
object ContentWidth {
    /**
     * A screen's main content column. Deliberately narrow so a list reads as a vertical column on
     * every window: on a phone it simply fills the screen, and on a desktop it stays a column instead
     * of stretching rows into wide bands.
     *
     * Narrower than a phone viewport on purpose — the same layout at every size, rather than one that
     * changes shape with the window.
     */
    val column = 240.dp

    /** Buttons, fields and other single-column controls. */
    val form = 400.dp

    /** Body copy — beyond this a line becomes hard to track back from. */
    val reading = 720.dp
}
