package de.coldtea.verborum.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import de.coldtea.verborum.core.designsystem.theme.Spacing

/** An optional icon button a screen can place on the right of the shared top bar. */
data class VerborumTopBarAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
)

/**
 * Content of the app's shared top bar. Screens do not draw their own header — they declare one
 * with [RegisterTopBar] and the single `Scaffold` in the navigation shell renders it.
 *
 * An empty [title] means "no chrome": neither the header nor the offline banner is drawn, which is
 * what a full-screen destination such as onboarding wants.
 */
data class VerborumTopBarState(
    val title: String = "",
    val subtitle: String? = null,
    val showBackButton: Boolean = false,
    val action: VerborumTopBarAction? = null,
)

/** Holds the top bar content the currently visible screen has registered. */
class VerborumTopBarController {

    var state: VerborumTopBarState by mutableStateOf(VerborumTopBarState())
        private set

    fun update(state: VerborumTopBarState) {
        this.state = state
    }

    /** Called by the shell when the destination changes, so no screen inherits another's header. */
    fun clear() {
        state = VerborumTopBarState()
    }
}

/**
 * Defaults to a throwaway controller so previews of individual screens (which do not provide one)
 * keep working; the real controller is provided by the navigation shell.
 */
val LocalVerborumTopBarController = staticCompositionLocalOf { VerborumTopBarController() }

/**
 * Declares the top bar for the enclosing screen. Tab roots pass [showBackButton] `false`; screens
 * navigated into pass `true`.
 */
@Composable
fun RegisterTopBar(
    title: String,
    subtitle: String? = null,
    showBackButton: Boolean = true,
    action: VerborumTopBarAction? = null,
) {
    val controller = LocalVerborumTopBarController.current
    // Keyed on the action's identity rather than the object, since its onClick is a fresh lambda
    // each recomposition; that lambda closes over remembered state, so a "stale" one still works.
    LaunchedEffect(title, subtitle, showBackButton, action?.contentDescription) {
        controller.update(VerborumTopBarState(title, subtitle, showBackButton, action))
    }
}

/**
 * The one header in the app, rendered by the navigation shell from whatever the visible screen
 * registered.
 */
@Composable
fun VerborumTopBar(
    state: VerborumTopBarState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** An app-level action supplied by the shell (sign out), shown after the screen's own. */
    appAction: VerborumTopBarAction? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            // Both hosts hand Compose the full screen, so the header owns its own top inset.
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            )
            .padding(
                start = if (state.showBackButton) Spacing.small else Spacing.large,
                end = Spacing.large,
                top = Spacing.small,
                bottom = Spacing.small,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.showBackButton) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = VerborumIcons.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(TopBarIconSize),
                )
            }
            Spacer(modifier = Modifier.width(Spacing.small))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            state.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.extraSmall),
                )
            }
        }

        listOfNotNull(state.action, appAction).forEach { action ->
            IconButton(onClick = action.onClick) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.contentDescription,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(TopBarIconSize),
                )
            }
        }
    }
}

private val TopBarIconSize = Spacing.large
