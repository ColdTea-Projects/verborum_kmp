package de.coldtea.verborum.core.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * Maximum content measures. A browser window can be far wider than any comfortable line length or
 * control, so screens cap their content rather than letting it stretch across a desktop monitor.
 */
object ContentWidth {
    /**
     * A screen's main content column, sized like a phone in portrait.
     *
     * On a phone this changes nothing — the viewport is narrower, so the content simply fills it. On a
     * landscape window it is what keeps the content a *vertical* column instead of stretching rows
     * into wide bands: the same shape on a desktop as on a phone.
     *
     * Chosen to match the widest common phone viewport (~430dp), so no layout is ever squeezed
     * narrower than the device it was designed for.
     */
    val column = 600.dp

    /** Buttons, fields and other single-column controls. */
    val form = 400.dp

    /** Body copy — beyond this a line becomes hard to track back from. */
    val reading = 720.dp

    /**
     * The redesigned web panes. Unlike [column] these are not phone-shaped: the web app has a
     * persistent sidebar and lays its content out as a desktop page, so each screen gets the measure
     * its own design calls for rather than one shared cap.
     */
    object Web {
        /** The dictionary list — wide enough for a multi-column card grid. */
        val list = 1040.dp

        /** The dictionary detail: practice tiles over the word list. */
        val detail = 840.dp

        /** The word form, whose two language cards sit side by side. */
        val wordForm = 900.dp

        /**
         * The dictionary form. Wide enough for the name and both language selects to sit abreast;
         * the form itself drops them back to a column when the window cannot spare the width.
         */
        val dictionaryForm = 900.dp

        /** The test: one question card, read straight down. */
        val test = 640.dp
    }
}
