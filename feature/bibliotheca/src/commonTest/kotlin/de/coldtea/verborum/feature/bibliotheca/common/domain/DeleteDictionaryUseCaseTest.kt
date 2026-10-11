package de.coldtea.verborum.feature.bibliotheca.common.domain

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.feature.bibliotheca.common.FakeDictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.dictionary
import de.coldtea.verborum.feature.bibliotheca.common.FakeWordRepository
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.DeleteDictionaryUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.DeleteOutcome
import de.coldtea.verborum.feature.bibliotheca.common.unauthorized
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeleteDictionaryUseCaseTest {

    @Test
    fun `a confirmed delete removes the row for good`() = runTest {
        val repository = FakeDictionaryRepository(listOf(dictionary("a"), dictionary("b")))

        val outcome = DeleteDictionaryUseCase(repository, FakeWordRepository())("a")

        assertEquals(DeleteOutcome.DELETED, outcome)
        assertEquals(listOf("b"), repository.allRows().map { it.dictionaryId })
    }

    @Test
    fun `a refused delete puts the row back rather than losing it silently`() = runTest {
        val repository = FakeDictionaryRepository(
            initial = listOf(dictionary("a")),
            deleteResult = unauthorized,
        )

        val outcome = DeleteDictionaryUseCase(repository, FakeWordRepository())("a")

        // `Unauthorized` is a refusal, not a transport fault, so the row comes back.
        assertEquals(DeleteOutcome.REFUSED, outcome)
        // Still present and no longer tombstoned, so the list shows it again.
        assertEquals(listOf("a"), repository.allRows().map { it.dictionaryId })
        assertTrue(repository.allRows().none { it.isDeleted })
    }
}
