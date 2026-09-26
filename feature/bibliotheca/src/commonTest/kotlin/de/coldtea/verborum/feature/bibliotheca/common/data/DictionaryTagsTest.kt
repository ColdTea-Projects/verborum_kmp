package de.coldtea.verborum.feature.bibliotheca.common.data

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.feature.bibliotheca.common.dictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.Dictionary
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tags are the one part of a dictionary the service keeps as a resource of its own: they are not in
 * the dictionary payload, and a pull replaces the stored row with the server's version. So without
 * their own round trip a tag is invisible the moment anything syncs — which is how tags came to
 * survive only until the page was reloaded.
 */
class DictionaryTagsTest {

    @Test
    fun `pulling tags puts them on the stored dictionary`() = runTest {
        val store = InMemoryDictionaryStore()
        store.merge(listOf(dictionary("a")))
        val repository = NetworkDictionaryRepository(
            api = tagApi(holding = listOf("A1", "TRAVEL")),
            store = store,
        )

        val outcome = repository.pullTags("a")

        assertEquals(Outcome.Success(listOf("A1", "TRAVEL")), outcome)
        assertEquals(listOf("A1", "TRAVEL"), store.find("a")?.tags)
    }

    @Test
    fun `a dictionary pull keeps the tags the device already had`() = runTest {
        // The service sends dictionaries without their tags, so a pull that trusted the payload
        // wholesale erased every tag on the device — on iOS, off a real database.
        val store = InMemoryDictionaryStore()
        store.merge(listOf(dictionary("a").copy(tags = listOf("A1"))))
        val repository = NetworkDictionaryRepository(
            api = dictionaryApiHolding(dictionary("a")),
            store = store,
        )

        repository.pullDictionaries("user-42")

        assertEquals(listOf("A1"), store.find("a")?.tags)
    }

    @Test
    fun `saving sends only the difference and one call per tag`() = runTest {
        // The service adds and removes tags one at a time, so a save has to work out which — and
        // re-adding one it already has answers with a conflict rather than the state asked for.
        val calls = mutableListOf<String>()
        val repository = NetworkDictionaryRepository(
            api = tagApi(holding = listOf("A1", "TRAVEL"), record = calls),
            store = InMemoryDictionaryStore(),
        )

        repository.save(dictionary("a").copy(tags = listOf("A1", "FOOD")), isNew = false)

        assertEquals(
            listOf("POST /dictionaries/a/tags FOOD", "DELETE /dictionaries/a/tags/TRAVEL"),
            calls,
        )
    }
}

/** A backend that answers the dictionary list with [rows], as the service sends them: without tags. */
private fun dictionaryApiHolding(vararg rows: Dictionary) =
    DictionaryApi(
        HttpClient(
            MockEngine {
                val body = rows.joinToString(",", "[", "]") {
                    """{"dictionaryId":"${it.dictionaryId}","userId":"${it.userId}","name":"${it.name}"}"""
                }

                respond(body, HttpStatusCode.OK, headersOf("Content-Type", ContentType.Application.Json.toString()))
            },
        ) {
            install(ContentNegotiation) { json() }
        },
    )

/**
 * A backend holding [holding] against dictionary `a`, recording the tag writes it is asked for.
 * Everything else answers empty, which is all the dictionary calls in these tests need.
 */
private fun tagApi(holding: List<String>, record: MutableList<String> = mutableListOf()) = DictionaryApi(
    HttpClient(
        MockEngine { request ->
            val path = request.url.encodedPath
            val json = headersOf("Content-Type", ContentType.Application.Json.toString())

            when {
                path.endsWith("/tags") && request.method == HttpMethod.Get ->
                    respond(holding.joinToString(",", "[", "]") { """{"tag":"$it"}""" }, HttpStatusCode.OK, json)

                path.endsWith("/tags") && request.method == HttpMethod.Post -> {
                    val body = (request.body as TextContent).text
                    val tag = Json.decodeFromString<DictionaryTagRequest>(body).tag

                    record += "POST $path $tag"
                    respond("{}", HttpStatusCode.OK, json)
                }

                request.method == HttpMethod.Delete -> {
                    record += "DELETE $path"
                    respond("{}", HttpStatusCode.OK, json)
                }

                else -> respond("{}", HttpStatusCode.OK, json)
            }
        },
    ) {
        install(ContentNegotiation) { json() }
    },
)
