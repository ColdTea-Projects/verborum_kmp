package de.coldtea.verborum

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import de.coldtea.verborum.core.auth.AuthService
import de.coldtea.verborum.core.auth.SessionState
import de.coldtea.verborum.core.designsystem.component.LoadingState
import de.coldtea.verborum.core.designsystem.theme.VerborumTheme
import de.coldtea.verborum.feature.auth.ui.LoginScreen
import de.coldtea.verborum.navigation.NavigationCentral
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * The app shell for both platforms: the theme, then the login gate. It owns no feature logic —
 * every screen lives behind a feature module's nav graph.
 */
@Composable
fun App(navController: NavHostController = rememberNavController()) {
    VerborumTheme {
        AuthGate(navController)
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
    val scope = rememberCoroutineScope()

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
        is SessionState.SignedIn -> NavigationCentral(
            onSignOut = { scope.launch { authService.signOut() } },
            navController = navController,
        )
    }
}
