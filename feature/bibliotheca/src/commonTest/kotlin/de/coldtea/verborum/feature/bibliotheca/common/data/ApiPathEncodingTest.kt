package de.coldtea.verborum.feature.bibliotheca.common.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * An id reaches these endpoints as a path segment, and the ids are not this client's to trust: they
 * arrive in server payloads, where `WordDto.wordId` is a nullable string with no shape enforced.
 *
 * Interpolated raw, an id carrying `/`, `?` or `#` stops being an id and becomes routing — it can
 * reach a different path, or turn the rest of the URL into a query. These pin every id to exactly
 * one segment.
 */
class ApiPathEncodingTest {

    @Test
    fun `an ordinary id is left as it is`() = runTest {
        assertEquals(
            "/dictionaries/9b271ecb-0c71-4d5a-abc9-1ec26bfc1f99",
            pathOf { api -> api.dictionariesOf("9b271ecb-0c71-4d5a-abc9-1ec26bfc1f99") },
        )
    }

    @Test
    fun `a slash in an id cannot reach another path`() = runTest {
        assertEquals("/dictionaries/a%2Fb", pathOf { api -> api.dictionariesOf("a/b") })
    }

    @Test
    fun `traversal in an id cannot climb out of the endpoint`() = runTest {
        assertEquals("/dictionaries/..%2F..%2Fadmin", pathOf { api -> api.dictionariesOf("../../admin") })
    }

    @Test
    fun `a question mark in an id cannot start a query`() = runTest {
        val url = urlOf { api -> api.dictionariesOf("a?role=admin") }

        assertEquals("/dictionaries/a%3Frole=admin", url.encodedPath)
        assertEquals("", url.encodedQuery)
    }

    @Test
    fun `a hash in an id cannot truncate the path`() = runTest {
        assertEquals("/dictionaries/a%23b", pathOf { api -> api.dictionariesOf("a#b") })
    }

    @Test
    fun `a deleted id is a single segment too`() = runTest {
        assertEquals("/dictionaries/a%2Fb", pathOf { api -> api.delete("a/b") })
    }

    @Test
    fun `the word endpoints pin their ids the same way`() = runTest {
        val recorded = mutableListOf<Url>()
        val words = WordApi(recordingClient(recorded))

        words.wordsOfDictionary("a/b")
        words.wordsOfUser("c/d")
        words.delete("e/f")
        words.deleteByDictionary("g/h")

        assertEquals(
            listOf(
                "/words/dictionary/a%2Fb",
                "/words/user/c%2Fd",
                "/words/e%2Ff",
                "/words/dictionary/g%2Fh",
            ),
            recorded.map { it.encodedPath },
        )
    }

    private suspend fun urlOf(call: suspend (DictionaryApi) -> Unit): Url {
        val recorded = mutableListOf<Url>()

        call(DictionaryApi(recordingClient(recorded)))

        return recorded.single()
    }

    private suspend fun pathOf(call: suspend (DictionaryApi) -> Unit): String =
        urlOf(call).encodedPath
}

/** Answers everything with 200 and keeps the URL each call actually asked for. */
private fun recordingClient(recorded: MutableList<Url>) = HttpClient(
    MockEngine { request ->
        recorded += request.url
        respondOk("[]")
    },
) {
    install(ContentNegotiation) { json() }
}
