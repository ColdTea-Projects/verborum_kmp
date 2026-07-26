package de.coldtea.verborum.feature.bibliotheca.dictionarylist.domain

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.FakeDictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.dictionary
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.domain.usecase.DeleteDictionaryUseCase
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.unauthorized
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeleteDictionaryUseCaseTest {

    @Test
    fun `a confirmed delete removes the row for good`() = runTest {
        val repository = FakeDictionaryRepository(listOf(dictionary("a"), dictionary("b")))

        val outcome = DeleteDictionaryUseCase(repository)("a")

        assertEquals(Outcome.Success(Unit), outcome)
        assertEquals(listOf("b"), repository.allRows().map { it.dictionaryId })
    }

    @Test
    fun `a refused delete puts the row back rather than losing it silently`() = runTest {
        val repository = FakeDictionaryRepository(
            initial = listOf(dictionary("a")),
            deleteResult = unauthorized,
        )

        val outcome = DeleteDictionaryUseCase(repository)("a")

        assertEquals(unauthorized, outcome)
        // Still present and no longer tombstoned, so the list shows it again.
        assertEquals(listOf("a"), repository.allRows().map { it.dictionaryId })
        assertTrue(repository.allRows().none { it.isDeleted })
    }
}
