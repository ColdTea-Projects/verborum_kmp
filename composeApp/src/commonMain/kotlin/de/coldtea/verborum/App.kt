package de.coldtea.verborum

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import de.coldtea.verborum.core.auth.AuthService
import de.coldtea.verborum.core.auth.SessionState
import de.coldtea.verborum.core.designsystem.component.LoadingState
import de.coldtea.verborum.core.designsystem.theme.VerborumTheme
import de.coldtea.verborum.core.localization.LanguageSettings
import de.coldtea.verborum.core.localization.LocalStrings
import de.coldtea.verborum.core.localization.stringsFor
import de.coldtea.verborum.feature.auth.ui.LoginScreen
import de.coldtea.verborum.feature.onboarding.ui.OnboardingGate
import de.coldtea.verborum.navigation.NavigationCentral
import org.koin.compose.koinInject

/**
 * The app shell for both platforms: the theme, then the login gate. It owns no feature logic —
 * every screen lives behind a feature module's nav graph.
 */
@Composable
fun App(
    navController: NavHostController = rememberNavController(),
    languageSettings: LanguageSettings = koinInject(),
) {
    val language by languageSettings.language.collectAsStateWithLifecycle()

    // One provider for the whole tree: every screen reads its words from here, so changing the
    // language in Options redraws the app in it without anything having to be reloaded.
    CompositionLocalProvider(LocalStrings provides stringsFor(language)) {
        VerborumTheme {
            AuthGate(navController)
        }
    }
}

/**
 * Chooses between the login wall and the app itself. Login is deliberately *not* a nav destination:
 * it is a wall in front of the whole graph, so no back stack can ever lead behind it.
 */
@Composable
private fun AuthGate(
    navController: NavHostController,
    authService: AuthService = koinInject(),
) {
    val sessionState by authService.sessionState.collectAsStateWithLifecycle()

    // Reads persisted tokens and finishes an OAuth redirect the web app was started with. Keyed on
    // the service so it runs once per app launch, not once per recomposition.
    LaunchedEffect(authService) {
        authService.initialize()
    }

    when (sessionState) {
        // Neither wall nor app until the stored session has been read, so an already signed-in
        // user never sees a flash of the login screen.
        SessionState.Unknown -> LoadingState()
        SessionState.SignedOut -> LoginScreen()
        // Signing out lives in the Options tab, so the shell only decides wall-or-app.
        // The tour, where a platform shows it at all, comes between signing in and the app.
        is SessionState.SignedIn -> OnboardingGate { NavigationCentral(navController) }
    }
}
