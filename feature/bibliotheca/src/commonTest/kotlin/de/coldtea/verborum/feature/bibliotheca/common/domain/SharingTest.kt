package de.coldtea.verborum.feature.bibliotheca.common.domain

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.feature.bibliotheca.common.FakeDictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.FakeProfileService
import de.coldtea.verborum.feature.bibliotheca.common.FakeWordRepository
import de.coldtea.verborum.feature.bibliotheca.common.dictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.DeleteDictionaryUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.DeleteOutcome
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.SetDictionarySharedUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.SharingChange
import de.coldtea.verborum.feature.bibliotheca.common.fakeSharingState
import de.coldtea.verborum.feature.bibliotheca.common.nonMemberProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The server's rule, mirrored: **a Forum member who has dictionaries must keep one shared.** Every
 * case here is a way the user can reach it, and the client has to answer the same way the backend
 * would — ideally without asking it first.
 */
class SharingTest {

    private val sharingRequired = Outcome.Failure(
        VerborumError.Http(400, "SharingRequiredException", "At least one dictionary must stay shared"),
    )

    @Test
    fun `a member's only shared dictionary is the last shared one`() = runTest {
        val repository = FakeDictionaryRepository(
            listOf(dictionary("a").copy(isPublic = true), dictionary("b")),
        )

        val state = fakeSharingState(repository)().first()

        assertTrue(state.isMember)
        assertTrue(state.isLastShared("a"))
        // "b" is private, so unsharing it is not even a question.
        assertFalse(state.isLastShared("b"))
    }

    @Test
    fun `with two shared dictionaries neither is the last`() = runTest {
        val repository = FakeDictionaryRepository(
            listOf(dictionary("a").copy(isPublic = true), dictionary("b").copy(isPublic = true)),
        )

        val state = fakeSharingState(repository)().first()

        assertFalse(state.isLastShared("a"))
        assertFalse(state.isLastShared("b"))
    }

    @Test
    fun `the rule does not apply to someone who is not a member`() = runTest {
        val repository = FakeDictionaryRepository(listOf(dictionary("a").copy(isPublic = true)))

        val state = fakeSharingState(repository, FakeProfileService(nonMemberProfile))().first()

        assertFalse(state.isMember)
        // A non-member's public dictionary is readable by id but not listed, so nothing obliges them.
        assertFalse(state.isLastShared("a"))
    }

    @Test
    fun `a member with nothing shared must share their next dictionary`() = runTest {
        val repository = FakeDictionaryRepository(listOf(dictionary("a")))

        val state = fakeSharingState(repository)().first()

        assertTrue(state.needsFirstShare)
    }

    @Test
    fun `a member who already shares one does not have to share the next`() = runTest {
        val repository = FakeDictionaryRepository(listOf(dictionary("a").copy(isPublic = true)))

        assertFalse(fakeSharingState(repository)().first().needsFirstShare)
    }

    @Test
    fun `sharing writes the local row only after the server agrees`() = runTest {
        val repository = FakeDictionaryRepository(listOf(dictionary("a")))

        val change = SetDictionarySharedUseCase(repository)("a", isShared = true)

        assertEquals(SharingChange.UPDATED, change)
        assertEquals(listOf("a" to true), repository.sharingRequests)
        assertTrue(repository.allRows().single().isPublic)
    }

    @Test
    fun `the last shared one cannot be unshared and the switch does not move`() = runTest {
        val repository = FakeDictionaryRepository(
            initial = listOf(dictionary("a").copy(isPublic = true)),
            setSharedResult = sharingRequired,
        )

        val change = SetDictionarySharedUseCase(repository)("a", isShared = false)

        assertEquals(SharingChange.LAST_SHARED, change)
        // Still shared locally: the row is written only on success, so there is nothing to undo.
        assertTrue(repository.allRows().single().isPublic)
    }

    @Test
    fun `a sharing change that never landed leaves the row alone`() = runTest {
        val repository = FakeDictionaryRepository(
            initial = listOf(dictionary("a")),
            setSharedResult = Outcome.Failure(VerborumError.Network()),
        )

        val change = SetDictionarySharedUseCase(repository)("a", isShared = true)

        assertEquals(SharingChange.FAILED, change)
        assertFalse(repository.allRows().single().isPublic)
    }

    @Test
    fun `deleting the last shared dictionary is refused and the row comes back`() = runTest {
        val repository = FakeDictionaryRepository(
            initial = listOf(dictionary("a").copy(isPublic = true), dictionary("b")),
            deleteResult = sharingRequired,
        )
        val words = FakeWordRepository()

        val outcome = DeleteDictionaryUseCase(repository, words)("a")

        assertEquals(DeleteOutcome.SHARING_REQUIRED, outcome)
        // The tombstone is lifted on both tables, so the dictionary and its words reappear together
        // — and the sync stops replaying a delete the server has already judged.
        assertTrue(repository.allRows().none { it.isDeleted })
        assertEquals(listOf("a"), words.restoredDictionaries)
    }

    @Test
    fun `a dictionary delete never sends a word delete of its own`() = runTest {
        val repository = FakeDictionaryRepository(listOf(dictionary("a")))
        val words = FakeWordRepository()

        DeleteDictionaryUseCase(repository, words)("a")

        // The server removes the words in the same transaction, *after* checking the sharing rule.
        // Deleting them first and then being refused destroys the words of a dictionary that still
        // exists — the data-loss bug this ordering exists to prevent.
        assertEquals(emptyList(), words.deletedDictionariesRemotely)
    }
}
