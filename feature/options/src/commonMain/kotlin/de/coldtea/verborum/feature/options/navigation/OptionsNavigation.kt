package de.coldtea.verborum.feature.options.navigation

import androidx.navigation.NavController
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
 * The app-language picker. iOS reaches it from the Options tab's language row; web picks its
 * language inline in a dropdown and never navigates here.
 */
@Serializable
internal data object LanguagePickerRoute

/**
 * [onHowToUseApp] is supplied by the shell — null leaves that row out. Passing it in keeps this
 * feature unaware of the tour it opens, which lives in a feature of its own.
 *
 * [registerLanguagePickerRoutes] is where the picker gets its destination; it is an expect because
 * the picker is an iOS screen and web's actual adds nothing.
 */
fun NavGraphBuilder.optionsGraph(
    navController: NavController,
    onHowToUseApp: (() -> Unit)? = null,
) {
    navigation<OptionsGraph>(startDestination = OptionsHomeRoute) {
        composable<OptionsHomeRoute> {
            OptionsScreen(
                onHowToUseApp = onHowToUseApp,
                onOpenLanguagePicker = { navController.navigate(LanguagePickerRoute) },
            )
        }

        registerLanguagePickerRoutes()
    }
}

/** The platforms' extra destinations. iOS registers the language picker; web has none. */
internal expect fun NavGraphBuilder.registerLanguagePickerRoutes()
