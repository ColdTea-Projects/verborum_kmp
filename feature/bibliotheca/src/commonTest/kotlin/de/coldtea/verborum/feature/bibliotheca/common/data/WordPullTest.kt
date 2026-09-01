package de.coldtea.verborum.feature.bibliotheca.common.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * How a pulled word's `level` decides whether the row is considered in step with the server.
 *
 * The distinction these pin down: a level the server *sent* but this app cannot use is corrected and
 * pushed back, while a level the server never sent is not a claim about the word at all. Collapsing
 * the two marks the whole library unsynced on every pull, and the next sync then uploads every word
 * with `level = 0` over whatever the server holds.
 */
class WordPullTest {

    @Test
    fun `a usable level is kept and the row stays in step`() = runTest {
        val words = pull("""[{"wordId":"w1","level":5}]""")

        assertEquals(5, words.single().level)
        assertTrue(words.single().isSynced)
    }

    @Test
    fun `an absent level leaves the row in step rather than queueing it`() = runTest {
        // The regression: this marked the row unsynced, so a backend that simply does not report
        // level would have every pulled word queued, and the next sync would write 0 over all of it.
        val words = pull("""[{"wordId":"w1","word":"casa"}]""")

        assertEquals(0, words.single().level)
        assertTrue(words.single().isSynced)
    }

    @Test
    fun `an explicit null level is silence too`() = runTest {
        val words = pull("""[{"wordId":"w1","level":null}]""")

        assertTrue(words.single().isSynced)
    }

    @Test
    fun `a level this app could not have written is corrected and pushed back`() = runTest {
        val words = pull("""[{"wordId":"w1","level":"seven"},{"wordId":"w2","level":99}]""")

        assertEquals(listOf(0, 0), words.map { it.level })
        assertTrue(words.none { it.isSynced }, "a healed row has to be queued for upload")
    }

    @Test
    fun `a word with no id is dropped rather than stored under an empty one`() = runTest {
        // Two id-less rows would otherwise land on the same key, the second replacing the first.
        val words = pull("""[{"wordId":"w1"},{"word":"casa"},{"wordId":"","word":"perro"}]""")

        assertEquals(listOf("w1"), words.map { it.wordId })
    }

    @Test
    fun `only the healed rows are queued when a payload mixes both`() = runTest {
        val words = pull("""[{"wordId":"w1","level":3},{"wordId":"w2","level":"seven"}]""")

        assertTrue(words.first { it.wordId == "w1" }.isSynced)
        assertFalse(words.first { it.wordId == "w2" }.isSynced)
    }

    /** Runs a real pull through the store, which is where `isSynced` decides what gets uploaded. */
    private suspend fun pull(body: String): List<de.coldtea.verborum.feature.bibliotheca.common.domain.Word> {
        val store = InMemoryWordStore()
        val repository = NetworkWordRepository(api = WordApi(clientAnswering(body)), store = store)

        repository.pullWords("d1")

        return store.all().first()
    }
}

private fun clientAnswering(body: String) = HttpClient(
    MockEngine {
        respond(
            content = body,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    },
) {
    install(ContentNegotiation) { json() }
}
