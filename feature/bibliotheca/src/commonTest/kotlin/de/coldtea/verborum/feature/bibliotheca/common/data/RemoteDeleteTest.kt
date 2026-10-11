package de.coldtea.verborum.feature.bibliotheca.common.data

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.feature.bibliotheca.common.FakeWordRepository
import de.coldtea.verborum.feature.bibliotheca.common.dictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.DeleteDictionaryUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.DeleteOutcome
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * What a delete does when the server says the row is not there.
 *
 * The rule: a 404 to a delete is the state the request was asking for, not a refusal. Reading it as
 * one made the row come back on screen and left the sync replaying the same delete for good.
 */
class RemoteDeleteTest {

    @Test
    fun `a row the server no longer has counts as deleted`() = runTest {
        val repository = NetworkDictionaryRepository(
            api = missingApi(),
            store = InMemoryDictionaryStore(),
        )

        assertIs<Outcome.Success<Unit>>(repository.deleteRemotely("a"))
    }

    @Test
    fun `deleting a row the server has already dropped does not bring it back`() = runTest {
        // The user-visible half: 404 is not >= 500, so it read as a refusal, and a refusal lifts the
        // tombstone — the dictionary reappeared in the list and the screen reported a failure.
        val store = InMemoryDictionaryStore()
        store.merge(listOf(dictionary("a")))
        val deleteDictionary = DeleteDictionaryUseCase(
            NetworkDictionaryRepository(api = missingApi(), store = store),
            FakeWordRepository(),
        )

        val outcome = deleteDictionary("a")

        assertEquals(DeleteOutcome.DELETED, outcome)
        assertTrue(store.dictionaries.first().isEmpty())
    }

    @Test
    fun `a tombstone the server has already honoured stops being pending`() = runTest {
        // The sync half: the tombstone only goes when the delete reports success, so a 404 left it
        // standing and every later sync replayed the same request.
        val store = InMemoryDictionaryStore()
        store.merge(listOf(dictionary("a")))
        val repository = NetworkDictionaryRepository(api = missingApi(), store = store)
        repository.markDeleted("a")

        if (repository.deleteRemotely("a") is Outcome.Success) repository.removeLocally("a")

        assertTrue(repository.tombstoned().isEmpty())
    }

    @Test
    fun `a delete the server actually refused still brings the row back`() = runTest {
        // The behaviour that must survive the change: 400 is a refusal, and a refusal has to restore
        // the row rather than let it vanish from a list that never changed.
        val store = InMemoryDictionaryStore()
        store.merge(listOf(dictionary("a")))
        val deleteDictionary = DeleteDictionaryUseCase(
            NetworkDictionaryRepository(api = refusedApi(), store = store),
            FakeWordRepository(),
        )

        val outcome = deleteDictionary("a")

        assertEquals(DeleteOutcome.REFUSED, outcome)
        assertEquals(listOf("a"), store.dictionaries.first().map { it.dictionaryId })
    }

    @Test
    fun `a delete that never reached the server keeps its tombstone for the next sync`() = runTest {
        val store = InMemoryDictionaryStore()
        store.merge(listOf(dictionary("a")))
        val deleteDictionary = DeleteDictionaryUseCase(
            NetworkDictionaryRepository(api = unreachableApi(), store = store),
            FakeWordRepository(),
        )

        val outcome = deleteDictionary("a")

        // Gone from view, still on the device, and queued — the offline delete path.
        assertEquals(DeleteOutcome.PENDING, outcome)
        assertTrue(store.dictionaries.first().isEmpty())
        assertEquals(listOf("a"), store.tombstoned().map { it.dictionaryId })
    }
}

/** A backend that has no such row — what a repeated or already-applied delete meets. */
private fun missingApi() = dictionaryApiAnswering(HttpStatusCode.NotFound)

/** A backend that answered, and refused. */
private fun refusedApi() = dictionaryApiAnswering(HttpStatusCode.BadRequest)

private fun dictionaryApiAnswering(status: HttpStatusCode) = DictionaryApi(
    HttpClient(MockEngine { respondError(status) }) {
        install(ContentNegotiation) { json() }
    },
)

private fun unreachableApi() = DictionaryApi(
    HttpClient(MockEngine { throw IllegalStateException("offline") }) {
        install(ContentNegotiation) { json() }
    },
)
