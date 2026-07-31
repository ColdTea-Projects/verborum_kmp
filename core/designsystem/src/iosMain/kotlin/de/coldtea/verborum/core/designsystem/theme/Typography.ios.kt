package de.coldtea.verborum.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable

/** iOS renders with the system fonts, which already cover every script the app supports. */
@Composable
internal actual fun verborumTypography(): Typography = Typography
