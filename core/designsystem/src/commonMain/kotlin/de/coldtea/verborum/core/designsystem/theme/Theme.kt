package de.coldtea.verborum.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * The single Material 3 theme for the whole app. Every entry point (iOS, web)
 * and every preview wraps its content in this.
 */
@Composable
fun VerborumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) VerborumDarkColors else VerborumLightColors,
        content = content,
    )
}
