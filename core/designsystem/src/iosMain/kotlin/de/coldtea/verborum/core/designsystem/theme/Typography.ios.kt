package de.coldtea.verborum.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/** iOS renders with the system fonts, which already cover every script the app supports. */
@Composable
internal actual fun verborumTypography(): Typography = Typography

/** Nothing to load: the system fonts cover every script, so this is a plain pass-through. */
@Composable
internal actual fun ProvideScriptFonts(content: @Composable () -> Unit) = content()

/** The system fonts draw every script, so no script ever needs a family of its own here. */
@Composable
actual fun fontFamilyForLanguage(languageCode: String): FontFamily? = null

@Composable
actual fun fontFamilyForText(text: String, fallbackLanguageCode: String): FontFamily? = null
