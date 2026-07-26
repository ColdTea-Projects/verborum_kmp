package de.coldtea.verborum

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import de.coldtea.verborum.core.designsystem.theme.VerborumTheme
import de.coldtea.verborum.navigation.NavigationCentral

/**
 * The app shell for both platforms: the theme, then the navigation centre. It owns no feature
 * logic — every screen lives behind a feature module's nav graph.
 */
@Composable
fun App(navController: NavHostController = rememberNavController()) {
    VerborumTheme {
        NavigationCentral(navController)
    }
}
