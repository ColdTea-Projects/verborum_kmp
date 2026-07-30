package de.coldtea.verborum.feature.onboarding.di

import de.coldtea.verborum.feature.onboarding.data.LocalCacheOnboardingRepository
import de.coldtea.verborum.feature.onboarding.data.OnboardingRepository
import de.coldtea.verborum.feature.onboarding.domain.OnboardingService
import de.coldtea.verborum.feature.onboarding.ui.OnboardingViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val onboardingModule: Module = module {
    single<OnboardingRepository> { LocalCacheOnboardingRepository(cache = get()) }
    factory { OnboardingService(repository = get()) }

    viewModelOf(::OnboardingViewModel)
}
