package de.coldtea.verborum.feature.options.di

import de.coldtea.verborum.feature.options.ui.OptionsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Only the view model: `AuthService` is `core:auth`, wired once in the app's `coreModule`. */
val optionsModule: Module = module {
    // Built by hand rather than with `viewModelOf`, because the local library is optional — only
    // iOS binds one, and `getOrNull` is what lets web go without.
    viewModel {
        OptionsViewModel(
            authService = get(),
            languageSettings = get(),
            localLibrary = getOrNull(),
        )
    }
}
