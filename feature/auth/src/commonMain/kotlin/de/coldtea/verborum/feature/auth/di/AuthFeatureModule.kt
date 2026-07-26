package de.coldtea.verborum.feature.auth.di

import de.coldtea.verborum.feature.auth.ui.LoginViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Only the view model: `AuthService` and the OAuth plumbing are `core:auth`, wired once in the
 * app's `coreModule`.
 */
val authFeatureModule: Module = module {
    viewModelOf(::LoginViewModel)
}
