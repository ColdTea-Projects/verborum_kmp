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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.localization.strings

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
    /**
     * What going back leads to — "Back to dictionaries", "Exit test".
     *
     * The web top bar spells this out beside its arrow, where there is room for it. iOS ignores it:
     * a bare chevron is the platform's own convention and a label beside it reads as clutter.
     */
    val backLabel: String? = null,
    /**
     * Override the title's font family — needed on web where the canvas has no system fonts and a
     * user-authored title may contain scripts (CJK, Arabic) the default Noto Sans Latin does not
     * carry. Null means "use the typography's own family".
     */
    val titleFontFamily: FontFamily? = null,
    /**
     * Optional composable rendered in place of [subtitle] when a plain string is not enough —
     * for example an animated word count that flashes on change.
     */
    val subtitleContent: (@Composable () -> Unit)? = null,
)

/**
 * Holds the top bar content the currently visible screen has registered.
 *
 * Registrations are token-based because screens overlap: during a navigation transition the incoming
 * screen registers while the outgoing one is still on its way out. A token means the departing screen
 * can only ever clear *its own* registration, never the one that replaced it.
 */
class VerborumTopBarController {

    var state: VerborumTopBarState by mutableStateOf(VerborumTopBarState())
        private set

    private var liveRegistration: Long = 0

    /** Registers [state] as the current header and returns the token identifying it. */
    fun register(state: VerborumTopBarState): Long {
        this.state = state
        liveRegistration += 1

        return liveRegistration
    }

    /**
     * Clears the header, but only if [token] is still the live registration — so a screen leaving
     * the composition cannot blank the header of the screen that has already replaced it.
     */
    fun unregister(token: Long) {
        if (token == liveRegistration) state = VerborumTopBarState()
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
 *
 * The registration lives exactly as long as the screen does: a `DisposableEffect`, so the header
 * appears with the screen and goes with it. The shell deliberately does **not** clear the header on
 * destination changes — the destination arrives a frame after the screen has already registered, so
 * clearing there wiped a registration that nothing would re-run.
 */
@Composable
fun RegisterTopBar(
    title: String,
    subtitle: String? = null,
    showBackButton: Boolean = true,
    action: VerborumTopBarAction? = null,
    backLabel: String? = null,
    titleFontFamily: FontFamily? = null,
    subtitleContent: (@Composable () -> Unit)? = null,
) {
    val controller = LocalVerborumTopBarController.current

    // Keyed on the action's identity rather than the object, since its onClick is a fresh lambda
    // each recomposition; that lambda closes over remembered state, so a "stale" one still works.
    // The same applies to subtitleContent — it is remembered by the caller so it only changes when
    // the underlying content actually changes (word count update), not on every recomposition.
    DisposableEffect(
        controller,
        title,
        subtitle,
        showBackButton,
        action?.contentDescription,
        backLabel,
        titleFontFamily,
        subtitleContent,
    ) {
        val token = controller.register(
            VerborumTopBarState(title, subtitle, showBackButton, action, backLabel, titleFontFamily, subtitleContent),
        )

        onDispose { controller.unregister(token) }
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
                    contentDescription = strings.back,
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
                fontFamily = state.titleFontFamily,
            )

            if (state.subtitleContent != null) {
                state.subtitleContent()
            } else {
                state.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.extraSmall),
                    )
                }
            }
        }

        state.action?.let { action ->
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
