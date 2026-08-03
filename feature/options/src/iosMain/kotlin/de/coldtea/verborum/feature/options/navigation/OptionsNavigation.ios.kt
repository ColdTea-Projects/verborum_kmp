package de.coldtea.verborum.feature.options.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import de.coldtea.verborum.feature.options.ui.LanguagePickerScreen

internal actual fun NavGraphBuilder.registerLanguagePickerRoutes() {
    composable<LanguagePickerRoute> {
        LanguagePickerScreen()
    }
}
