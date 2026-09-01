package de.coldtea.verborum.feature.onboarding.data

import de.coldtea.verborum.core.database.LocalCache

/**
 * Remembers whether the tour has been seen.
 *
 * Backed by `LocalCache`, which persists on iOS and is a no-op on web — and that suits both
 * behaviours exactly: iOS needs to show the tour once, while on web it is opened deliberately from
 * Options, so there is nothing to remember.
 *
 * "Once" means once per signed-in session, not once per device: signing out empties `LocalCache`
 * along with the library, so the next person to sign in is shown the tour rather than inheriting the
 * last one's answer.
 */
interface OnboardingRepository {
    suspend fun isCompleted(): Boolean
    suspend fun setCompleted()
}

class LocalCacheOnboardingRepository(
    private val cache: LocalCache,
) : OnboardingRepository {

    override suspend fun isCompleted(): Boolean = cache.get(KEY) == COMPLETED

    override suspend fun setCompleted() = cache.put(KEY, COMPLETED)

    private companion object {
        const val KEY = "onboarding.completed"
        const val COMPLETED = "true"
    }
}
