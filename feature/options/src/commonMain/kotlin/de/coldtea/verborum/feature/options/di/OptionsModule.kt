package de.coldtea.verborum.feature.options.di

import de.coldtea.verborum.feature.options.ui.OptionsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Only the view model: `AuthService` is `core:auth`, wired once in the app's `coreModule`. */
val optionsModule: Module = module {
    viewModelOf(::OptionsViewModel)
}
