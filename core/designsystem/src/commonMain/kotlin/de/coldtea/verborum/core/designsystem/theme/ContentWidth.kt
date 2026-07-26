package de.coldtea.verborum.core.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * Maximum content measures. A browser window can be far wider than any comfortable line length or
 * control, so screens cap their content rather than letting it stretch across a desktop monitor.
 */
object ContentWidth {
    /** Buttons, fields and other single-column controls. */
    val form = 400.dp

    /** Body copy — beyond this a line becomes hard to track back from. */
    val reading = 720.dp
}
