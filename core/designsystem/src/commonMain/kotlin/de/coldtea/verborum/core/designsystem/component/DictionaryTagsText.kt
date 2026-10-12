package de.coldtea.verborum.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.coldtea.verborum.core.localization.strings
import de.coldtea.verborum.core.localization.tagLabelOf

/**
 * A dictionary's tags as one quiet line of translated labels: "A1 · Food & drink · IELTS".
 *
 * Shared rather than written per screen so a dictionary's tags read identically wherever it is
 * shown — the library's own details screen and a Forum listing are the same dictionary. Draws
 * nothing when there are no tags, so a caller can hand it the list unconditionally.
 */
@Composable
fun DictionaryTagsText(tagCodes: List<String>, modifier: Modifier = Modifier) {
    if (tagCodes.isEmpty()) return

    val tagStrings = strings

    Text(
        text = tagCodes.joinToString(" · ") { code -> tagLabelOf(code, tagStrings) },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
