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

fun NavGraphBuilder.optionsGraph() {
    navigation<OptionsGraph>(startDestination = OptionsHomeRoute) {
        composable<OptionsHomeRoute> {
            OptionsScreen()
        }
    }
}
