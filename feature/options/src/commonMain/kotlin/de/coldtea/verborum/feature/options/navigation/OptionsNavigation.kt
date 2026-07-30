package de.coldtea.verborum.feature.options.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import de.coldtea.verborum.feature.options.ui.OptionsScreen
import kotlinx.serialization.Serializable

/** The graph the shell references; its screens stay private to this feature. */
@Serializable
data object OptionsGraph

@Serializable
private data object OptionsHomeRoute

/**
 * [onHowToUseApp] is supplied by the shell — null leaves that row out. Passing it in keeps this
 * feature unaware of the tour it opens, which lives in a feature of its own.
 */
fun NavGraphBuilder.optionsGraph(onHowToUseApp: (() -> Unit)? = null) {
    navigation<OptionsGraph>(startDestination = OptionsHomeRoute) {
        composable<OptionsHomeRoute> {
            OptionsScreen(onHowToUseApp = onHowToUseApp)
        }
    }
}
