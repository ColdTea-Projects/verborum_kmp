package de.coldtea.verborum.feature.options.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/**
 * The face that can draw a language's own name — "日本語", "العربية".
 *
 * iOS has system fonts covering every script and needs nothing; the web canvas has none at all, so
 * the picker would list empty boxes for exactly the languages a reader is scanning for.
 */
@Composable
internal expect fun fontFamilyForUiLanguage(languageCode: String): FontFamily?
