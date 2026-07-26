package de.coldtea.verborum.feature.bibliotheca.common.domain

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.feature.bibliotheca.common.FakeDictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.FakeWordRepository
import de.coldtea.verborum.feature.bibliotheca.common.dictionary
import de.coldtea.verborum.feature.bibliotheca.common.unauthorized
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SyncServiceTest {

    private val words = FakeWordRepository()

    private fun syncService(
        repository: FakeDictionaryRepository,
        signedInAs: String?,
        wordRepository: FakeWordRepository = words,
    ) = SyncService(
        activeUser = ActiveUserUseCase { signedInAs },
        syncDictionariesUseCase = SyncUserDictionariesUseCase(repository),
        wordRepository = wordRepository,
    )

    @Test
    fun `signed out there is nothing to reconcile, which is not an error`() = runTest {
        val repository = FakeDictionaryRepository(listOf(dictionary("a")))

        val outcome = syncService(repository, signedInAs = null).syncDictionaries()

        assertEquals(Outcome.Success(Unit), outcome)
        // Both endpoints are user-scoped, so a signed-out sync must not call either.
        assertNull(repository.pulledUserId)
        assertNull(words.pulledUserId)
    }

    @Test
    fun `a signed-in sync pulls the active user's dictionaries`() = runTest {
        val repository = FakeDictionaryRepository(listOf(dictionary("a")))

        val outcome = syncService(repository, signedInAs = "user-42").syncDictionaries()

        assertEquals(Outcome.Success(Unit), outcome)
        assertEquals("user-42", repository.pulledUserId)
        // The words come in the same sync — that is what puts a count on each list row.
        assertEquals("user-42", words.pulledUserId)
    }

    @Test
    fun `a failed pull is reported rather than swallowed`() = runTest {
        val repository = FakeDictionaryRepository(pullResult = unauthorized)

        assertEquals(unauthorized, syncService(repository, signedInAs = "user-42").syncDictionaries())
    }

    @Test
    fun `a failed word pull does not fail the whole sync`() = runTest {
        // Without counts the list still works; turning the screen into an error would be worse.
        val wordsThatFail = FakeWordRepository(pullResult = unauthorized)
        val service = syncService(
            repository = FakeDictionaryRepository(listOf(dictionary("a"))),
            signedInAs = "user-42",
            wordRepository = wordsThatFail,
        )

        assertEquals(Outcome.Success(Unit), service.syncDictionaries())
    }

    @Test
    fun `the details screen syncs just its own dictionary's words`() = runTest {
        val service = syncService(FakeDictionaryRepository(), signedInAs = "user-42")

        assertEquals(Outcome.Success(Unit), service.syncDictionaryWords("dictionary-7"))
        assertEquals("dictionary-7", words.pulledDictionaryId)
    }

    @Test
    fun `signed out there are no words to pull either`() = runTest {
        val service = syncService(FakeDictionaryRepository(), signedInAs = null)

        assertEquals(Outcome.Success(Unit), service.syncDictionaryWords("dictionary-7"))
        assertNull(words.pulledDictionaryId)
    }
}
