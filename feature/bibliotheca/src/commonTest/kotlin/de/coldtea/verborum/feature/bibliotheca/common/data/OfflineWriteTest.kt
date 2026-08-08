package de.coldtea.verborum.feature.bibliotheca.common.data

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.feature.bibliotheca.common.dictionary
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * What happens to a write when the backend cannot be reached.
 *
 * The rule these pin down: a request that never landed leaves the row on the device as pending, and
 * the screen is told the save worked — because it did, locally, and the next sync will carry it. A
 * request the server *refused* is rolled back instead, since retrying it would fail identically.
 */
class OfflineWriteTest {

    @Test
    fun `a save the server never received is kept and reported as a success`() = runTest {
        val store = InMemoryDictionaryStore()
        val repository = NetworkDictionaryRepository(api = unreachableApi(), store = store)

        val outcome = repository.save(dictionary("a"), isNew = true)

        assertIs<Outcome.Success<Unit>>(outcome)
        assertEquals(listOf("a"), store.dictionaries.first().map { it.dictionaryId })
    }

    @Test
    fun `a row kept that way is queued for the next upload`() = runTest {
        val store = InMemoryDictionaryStore()
        val repository = NetworkDictionaryRepository(api = unreachableApi(), store = store)

        repository.save(dictionary("a"), isNew = true)

        // isSynced = false is what the upload pass looks for, and what a merge protects.
        assertEquals(listOf("a"), repository.pendingUploads().map { it.dictionaryId })
        assertFalse(store.find("a")?.isSynced ?: true)
    }

    @Test
    fun `an edit the server never received keeps the new value`() = runTest {
        val store = InMemoryDictionaryStore()
        store.merge(listOf(dictionary("a", name = "old")))
        val repository = NetworkDictionaryRepository(api = unreachableApi(), store = store)

        repository.save(dictionary("a", name = "new"), isNew = false)

        assertEquals("new", store.find("a")?.name)
    }

    @Test
    fun `a save the server refused is rolled back and reported`() = runTest {
        val store = InMemoryDictionaryStore()
        val repository = NetworkDictionaryRepository(api = refusingApi(), store = store)

        val outcome = repository.save(dictionary("a"), isNew = true)

        assertIs<Outcome.Failure>(outcome)
        // Retrying a payload the backend has already judged would never succeed, so the row goes.
        assertTrue(store.dictionaries.first().isEmpty())
    }

    @Test
    fun `an edit the server refused goes back to what it was`() = runTest {
        val store = InMemoryDictionaryStore()
        store.merge(listOf(dictionary("a", name = "old")))
        val repository = NetworkDictionaryRepository(api = refusingApi(), store = store)

        val outcome = repository.save(dictionary("a", name = "new"), isNew = false)

        assertIs<Outcome.Failure>(outcome)
        assertEquals("old", store.find("a")?.name)
    }
}

/** A backend that cannot be reached at all — what `apiCall` maps onto `VerborumError.Network`. */
private fun unreachableApi() = DictionaryApi(
    HttpClient(MockEngine { throw IllegalStateException("offline") }) {
        install(ContentNegotiation) { json() }
    },
)

/** A backend that answered, and said no. */
private fun refusingApi() = DictionaryApi(
    HttpClient(MockEngine { respondError(HttpStatusCode.BadRequest) }) {
        install(ContentNegotiation) { json() }
    },
)
