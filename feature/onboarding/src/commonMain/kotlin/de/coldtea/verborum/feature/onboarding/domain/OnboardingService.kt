package de.coldtea.verborum.feature.onboarding.domain

import de.coldtea.verborum.feature.onboarding.data.OnboardingRepository

class OnboardingService(
    private val repository: OnboardingRepository,
) {
    /** True once the tour has been finished; it is only shown unprompted before that. */
    suspend fun isCompleted(): Boolean = repository.isCompleted()

    suspend fun complete() = repository.setCompleted()
}
