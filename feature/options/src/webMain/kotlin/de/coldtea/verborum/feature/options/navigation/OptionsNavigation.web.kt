package de.coldtea.verborum.feature.options.navigation

import androidx.navigation.NavGraphBuilder

/**
 * Web picks its app language inline in the Options dropdown, so the picker route has nothing to
 * register here — `LanguagePickerRoute` is only ever navigated to from the iOS actual of
 * `OptionsContent`.
 */
internal actual fun NavGraphBuilder.registerLanguagePickerRoutes() = Unit
