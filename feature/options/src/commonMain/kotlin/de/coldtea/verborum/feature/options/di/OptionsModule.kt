package de.coldtea.verborum.feature.options.di

import de.coldtea.verborum.feature.options.ui.OptionsViewModel
import de.coldtea.verborum.feature.options.ui.profile.ProfileViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The tab's view models. Their collaborators all come from `core:*`, wired once in the app's
 * `coreModule` — `AuthService`, and `ProfileService` for the Profile screen.
 */
val optionsModule: Module = module {
    // Built by hand rather than with `viewModelOf`, because the local library is optional — only
    // iOS binds one, and `getOrNull` is what lets web go without.
    viewModel {
        OptionsViewModel(
            authService = get(),
            languageSettings = get(),
            localLibrary = getOrNull(),
            localCache = get(),
        )
    }

    viewModelOf(::ProfileViewModel)
}
