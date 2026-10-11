package de.coldtea.verborum.feature.bibliotheca.common.domain

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.feature.bibliotheca.common.FakeDictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.FakeWordRepository
import de.coldtea.verborum.feature.bibliotheca.common.dictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.UploadPendingChangesUseCase
import de.coldtea.verborum.feature.bibliotheca.common.unauthorized
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull
import de.coldtea.verborum.feature.bibliotheca.common.fakeImportedService
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.feature.bibliotheca.common.FakeProfileService
import de.coldtea.verborum.feature.bibliotheca.common.data.InMemoryImportedDictionaryStore
import de.coldtea.verborum.feature.bibliotheca.common.importedDictionary
import de.coldtea.verborum.feature.bibliotheca.common.importedWord

class SyncServiceTest {

    private val words = FakeWordRepository()

    private fun syncService(
        repository: FakeDictionaryRepository,
        signedInAs: String?,
        wordRepository: FakeWordRepository = words,
        imported: ImportedDictionaryService = fakeImportedService(),
    ) = SyncService(
        activeUser = ActiveUserUseCase { signedInAs },
        syncDictionariesUseCase = SyncUserDictionariesUseCase(repository),
        dictionaryRepository = repository,
        wordRepository = wordRepository,
        uploadPendingChanges = UploadPendingChangesUseCase(repository, wordRepository),
        importedDictionaryService = imported,
    )

    @Test
    fun `signed out there is nothing to reconcile which is not an error`() = runTest {
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

        assertEquals(Outcome.Success(Unit), service.syncDictionary("dictionary-7"))
        assertEquals("dictionary-7", words.pulledDictionaryId)
    }

    @Test
    fun `signed out there are no words to pull either`() = runTest {
        val service = syncService(FakeDictionaryRepository(), signedInAs = null)

        assertEquals(Outcome.Success(Unit), service.syncDictionary("dictionary-7"))
        assertNull(words.pulledDictionaryId)
    }

    @Test
    fun `the server's sharing flag wins even on a row with unsynced edits`() = runTest {
        // The merge deliberately leaves a locally edited row alone so offline work is not lost. That
        // protection must not cover `isPublic`: the server flips it by event when the user joins or
        // leaves, so a row keeping its local copy would push the stale value on the next upload,
        // silently undo the join — and then be refused, and retried, forever.
        val repository = FakeDictionaryRepository(
            initial = listOf(dictionary("a").copy(isPublic = false, isSynced = false)),
            pullResult = Outcome.Success(listOf(dictionary("a").copy(isPublic = true))),
        )

        syncService(repository, signedInAs = "user-42").syncDictionaries()

        assertTrue(repository.allRows().single().isPublic)
    }

    @Test
    fun `a vault that cannot be reached does not fail the dictionary sync`() = runTest {
        val repository = FakeDictionaryRepository(listOf(dictionary("a")))
        val offlineVault = fakeImportedService(
            profileService = FakeProfileService(
                vaultResult = Outcome.Failure(VerborumError.Network()),
            ),
        )

        val outcome = syncService(repository, signedInAs = "user-42", imported = offlineVault)
            .syncDictionaries()

        // The Forum lives in a different service; its being unreachable must not make the user's own
        // library look broken.
        assertEquals(Outcome.Success(Unit), outcome)
    }

    @Test
    fun `a downloaded dictionary is never in the upload set`() = runTest {
        // Unsynced, so there is genuinely something for the upload pass to pick up — against which
        // the absence of the downloaded one actually means something.
        val repository = FakeDictionaryRepository(
            listOf(dictionary("mine").copy(isSynced = false)),
        )
        val store = InMemoryImportedDictionaryStore()
        store.save(importedDictionary("theirs"), listOf(importedWord("w1")))

        syncService(
            repository,
            signedInAs = "user-42",
            imported = fakeImportedService(store = store),
        ).syncDictionaries()

        // Every write to someone else's dictionary is a 403. The separate tables are what make this
        // structurally true rather than a filter somebody has to remember.
        assertEquals(listOf("mine"), repository.uploaded.map { it.dictionaryId }.distinct())
    }
}
