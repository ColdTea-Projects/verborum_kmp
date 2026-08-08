package de.coldtea.verborum.feature.bibliotheca.common.domain

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.feature.bibliotheca.common.FakeDictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.FakeWordRepository
import de.coldtea.verborum.feature.bibliotheca.common.dictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.UploadPendingChangesUseCase
import de.coldtea.verborum.feature.bibliotheca.common.word
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UploadPendingChangesUseCaseTest {

    @Test
    fun `a row the server has not seen is pushed`() = runTest {
        val dictionaries = FakeDictionaryRepository(
            listOf(dictionary("a").copy(isSynced = false)),
        )
        val words = FakeWordRepository(listOf(word("w", dictionaryId = "a").copy(isSynced = false)))

        UploadPendingChangesUseCase(dictionaries, words)()

        assertEquals(listOf("a"), dictionaries.uploaded.map { it.dictionaryId })
        assertEquals(listOf("w"), words.uploaded.map { it.wordId })
    }

    @Test
    fun `an already synced row is left alone`() = runTest {
        // `dictionary()` and `word()` are synced by default, which is what a pull produces.
        val dictionaries = FakeDictionaryRepository(listOf(dictionary("a")))
        val words = FakeWordRepository(listOf(word("w", dictionaryId = "a")))

        UploadPendingChangesUseCase(dictionaries, words)()

        assertTrue(dictionaries.uploaded.isEmpty())
        assertTrue(words.uploaded.isEmpty())
    }

    @Test
    fun `a pushed row stops being pending`() = runTest {
        val dictionaries = FakeDictionaryRepository(
            listOf(dictionary("a").copy(isSynced = false)),
        )
        val upload = UploadPendingChangesUseCase(dictionaries, FakeWordRepository())

        upload()
        upload()

        // The second pass has nothing left to do, or every sync would re-send the whole library.
        assertEquals(listOf("a"), dictionaries.uploaded.map { it.dictionaryId })
    }

    @Test
    fun `a tombstoned row is deleted remotely and then dropped`() = runTest {
        val dictionaries = FakeDictionaryRepository(
            listOf(dictionary("a").copy(isDeleted = true)),
        )

        UploadPendingChangesUseCase(dictionaries, FakeWordRepository())()

        assertTrue(dictionaries.allRows().isEmpty())
    }

    @Test
    fun `a tombstone whose delete fails is kept for the next pass`() = runTest {
        val dictionaries = FakeDictionaryRepository(
            listOf(dictionary("a").copy(isDeleted = true)),
            deleteResult = Outcome.Failure(VerborumError.Network()),
        )

        UploadPendingChangesUseCase(dictionaries, FakeWordRepository())()

        // Still tombstoned rather than restored: the user deleted it, the server just has not heard.
        assertEquals(listOf("a"), dictionaries.allRows().map { it.dictionaryId })
        assertTrue(dictionaries.allRows().single().isDeleted)
    }

    @Test
    fun `a word in a dictionary being deleted is not deleted twice`() = runTest {
        // The dictionary's delete takes its words with it in one request; sending a per-word delete
        // as well would be a wasted round trip against rows the server has already dropped.
        val dictionaries = FakeDictionaryRepository(
            listOf(dictionary("a").copy(isDeleted = true)),
        )
        val words = FakeWordRepository(
            listOf(word("w", dictionaryId = "a").copy(isDeleted = true)),
        )

        UploadPendingChangesUseCase(dictionaries, words)()

        assertTrue(words.allRows().isEmpty())
    }
}
