package de.coldtea.verborum.core.designsystem.component

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.Flow

/**
 * The app-wide [SnackbarHostState], provided once by the navigation shell so any screen can surface
 * a message without threading the host through its call site — mirrors
 * [LocalVerborumTopBarController]. Defaults to a throwaway instance so screen previews (which do
 * not provide one) keep working.
 */
val LocalSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }

/**
 * Collects [messages] — typically a view model's effect flow, mapped to text — and shows each on the
 * shared snackbar, so error reporting stays uniform instead of each screen re-implementing it.
 */
@Composable
fun ShowSnackbarMessages(messages: Flow<String>) {
    val hostState = LocalSnackbarHostState.current
    LaunchedEffect(messages) {
        messages.collect { message ->
            hostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }
}
