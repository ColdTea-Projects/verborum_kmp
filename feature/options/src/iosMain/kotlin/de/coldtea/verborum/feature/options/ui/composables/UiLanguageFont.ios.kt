package de.coldtea.verborum.feature.options.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/** The system fonts already cover every script the picker lists. */
@Composable
internal actual fun fontFamilyForUiLanguage(languageCode: String): FontFamily? = null
