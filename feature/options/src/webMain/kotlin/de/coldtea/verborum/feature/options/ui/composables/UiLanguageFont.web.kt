package de.coldtea.verborum.feature.options.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import de.coldtea.verborum.core.designsystem.theme.fontFamilyForLanguage

@Composable
internal actual fun fontFamilyForUiLanguage(languageCode: String): FontFamily? =
    fontFamilyForLanguage(languageCode)
