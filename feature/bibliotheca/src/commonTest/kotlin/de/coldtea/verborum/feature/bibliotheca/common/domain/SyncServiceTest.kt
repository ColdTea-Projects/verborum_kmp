package de.coldtea.verborum.feature.bibliotheca.common.domain

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.FakeDictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.dictionary
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.unauthorized
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SyncServiceTest {

    private fun syncService(
        repository: FakeDictionaryRepository,
        signedInAs: String?,
    ) = SyncService(
        activeUser = ActiveUserUseCase { signedInAs },
        syncDictionariesUseCase = SyncUserDictionariesUseCase(repository),
    )

    @Test
    fun `signed out there is nothing to reconcile, which is not an error`() = runTest {
        val repository = FakeDictionaryRepository(listOf(dictionary("a")))

        val outcome = syncService(repository, signedInAs = null).syncDictionaries()

        assertEquals(Outcome.Success(Unit), outcome)
        // The endpoint is user-scoped, so a signed-out sync must not call it at all.
        assertNull(repository.pulledUserId)
    }

    @Test
    fun `a signed-in sync pulls the active user's dictionaries`() = runTest {
        val repository = FakeDictionaryRepository(listOf(dictionary("a")))

        val outcome = syncService(repository, signedInAs = "user-42").syncDictionaries()

        assertEquals(Outcome.Success(Unit), outcome)
        assertEquals("user-42", repository.pulledUserId)
    }

    @Test
    fun `a failed pull is reported rather than swallowed`() = runTest {
        val repository = FakeDictionaryRepository(pullResult = unauthorized)

        assertEquals(unauthorized, syncService(repository, signedInAs = "user-42").syncDictionaries())
    }
}
