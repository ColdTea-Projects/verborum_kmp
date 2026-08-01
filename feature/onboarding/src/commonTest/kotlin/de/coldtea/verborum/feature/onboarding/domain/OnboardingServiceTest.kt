package de.coldtea.verborum.feature.onboarding.domain

import de.coldtea.verborum.core.database.LocalCache
import de.coldtea.verborum.feature.onboarding.data.LocalCacheOnboardingRepository
import de.coldtea.verborum.feature.onboarding.ui.model.OnboardingPage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingServiceTest {

    @Test
    fun `the tour has not been seen until it is completed`() = runTest {
        val service = OnboardingService(LocalCacheOnboardingRepository(InMemoryCache()))

        assertFalse(service.isCompleted())

        service.complete()

        assertTrue(service.isCompleted())
    }

    @Test
    fun `completion survives a new instance, which is what stops it reappearing`() = runTest {
        val cache = InMemoryCache()
        OnboardingService(LocalCacheOnboardingRepository(cache)).complete()

        // A fresh service over the same storage stands in for the next app launch.
        assertTrue(OnboardingService(LocalCacheOnboardingRepository(cache)).isCompleted())
    }

    @Test
    fun `storage that never persists reports the tour as unseen rather than seen`() = runTest {
        // This is web's `LocalCache`: a no-op. Erring towards "not completed" is the safe direction —
        // there, the tour is only ever opened deliberately, so nothing shows up uninvited either way.
        val service = OnboardingService(LocalCacheOnboardingRepository(NoOpCache))
        service.complete()

        assertFalse(service.isCompleted())
    }

    @Test
    fun `the tour ends on its last page, which is the one carrying the done button`() {
        assertEquals(4, OnboardingPage.entries.size)
        assertTrue(OnboardingPage.entries.last().isLast)
        assertFalse(OnboardingPage.entries.first().isLast)
        // A page's words now come from the string catalogue and are read inside composition, so
        // "every page says something" is asserted there — see UiLanguageTest.
    }
}

private class InMemoryCache : LocalCache {
    private val entries = mutableMapOf<String, String>()

    override suspend fun put(key: String, value: String) {
        entries[key] = value
    }

    override suspend fun get(key: String): String? = entries[key]

    override suspend fun remove(key: String) {
        entries.remove(key)
    }

    override suspend fun clear() = entries.clear()
}

private object NoOpCache : LocalCache {
    override suspend fun put(key: String, value: String) = Unit
    override suspend fun get(key: String): String? = null
    override suspend fun remove(key: String) = Unit
    override suspend fun clear() = Unit
}
