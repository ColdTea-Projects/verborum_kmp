package de.coldtea.verborum.feature.options.di

import de.coldtea.verborum.feature.options.ui.OptionsViewModel
import de.coldtea.verborum.feature.options.ui.deleteaccount.DeleteAccountViewModel
import de.coldtea.verborum.feature.options.ui.profile.ProfileViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The tab's view models. Their collaborators all come from `core:*`, wired once in the app's
 * `coreModule` — `AuthService`, `ProfileService` for the Profile screen and `AccountDeletionService` for deleting the account.
 */
val optionsModule: Module = module {
    viewModel {
        OptionsViewModel(
            authService = get(),
            languageSettings = get(),
            localCache = get(),
        )
    }

    viewModelOf(::ProfileViewModel)

    viewModel {
        DeleteAccountViewModel(
            authService = get(),
            accountDeletionService = get(),
            localCache = get(),
        )
    }
}
