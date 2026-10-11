package de.coldtea.verborum.feature.bibliotheca.common

import de.coldtea.verborum.core.network.VerborumJson
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryApi
import de.coldtea.verborum.feature.bibliotheca.common.data.ImportedDictionaryStore
import de.coldtea.verborum.feature.bibliotheca.common.data.InMemoryImportedDictionaryStore
import de.coldtea.verborum.feature.bibliotheca.common.data.WordApi
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.TimeProvider
import de.coldtea.verborum.core.profile.domain.ProfileService
import de.coldtea.verborum.feature.bibliotheca.common.domain.ImportedDictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.ImportedDictionaryService
import de.coldtea.verborum.feature.bibliotheca.common.domain.ImportedWord
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.FetchImportedDictionaryContent
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.ImportedContent
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.NetworkImportedDictionaryContent
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json

internal fun importedDictionary(
    id: String,
    publisherId: String = "publisher-1",
    publisherName: String? = "Anna Bauer",
    name: String = "Shared $id",
    fromLang: String = "EN",
    toLang: String = "DE",
    importedAt: Long = 1_000L,
) = ImportedDictionary(
    dictionaryId = id,
    publisherId = publisherId,
    publisherName = publisherName,
    name = name,
    fromLang = fromLang,
    toLang = toLang,
    importedAt = importedAt,
)

internal fun importedWord(
    id: String,
    dictionaryId: String = "d1",
    level: Int = 0,
    createdAt: Long = 0L,
) = ImportedWord(
    wordId = id,
    dictionaryId = dictionaryId,
    word = id,
    wordMeta = """{"lang":"en"}""",
    translation = "$id-translation",
    translationMeta = """{"lang":"de"}""",
    level = level,
    createdAt = createdAt,
    updatedAt = createdAt,
)

/**
 * The `ms_dictionary` reads a download depends on, scripted by **path** rather than by call order.
 *
 * By path because the service makes three requests per dictionary and the vault refresh makes them
 * for several dictionaries: an order-based script would encode the implementation's request sequence
 * into every test, and then break whenever that order changed for a reason nobody cared about.
 */
internal class SharedDictionaryBackend(
    /** `dictionaryId` → the owner's dictionary payload, or absent for one that is unreadable. */
    private val dictionaries: Map<String, String> = emptyMap(),
    /** `dictionaryId` → its words payload. */
    private val words: Map<String, String> = emptyMap(),
    /** `dictionaryId` → its tag payload. */
    private val tags: Map<String, String> = emptyMap(),
    /** Ids whose every read fails at the transport, modelling a flaky connection. */
    private val unreachable: Set<String> = emptySet(),
) {
    private val _requestedPaths = mutableListOf<String>()

    val requestedPaths: List<String> get() = _requestedPaths

    private val client = HttpClient(
        MockEngine { request ->
            val path = request.url.encodedPath
            _requestedPaths += path

            val id = path.substringAfterLast('/')
            if (unreachable.any { it in path }) throw IllegalStateException("offline")

            when {
                path.startsWith("/dictionaries/dictionary/") ->
                    dictionaries[id]?.let { ok(it) } ?: notFound()

                path.endsWith("/tags") ->
                    tags[path.removePrefix("/dictionaries/").removeSuffix("/tags")]?.let { ok(it) }
                        ?: ok("[]")

                path.startsWith("/words/dictionary/") -> ok(words[id] ?: "[]")

                else -> notFound()
            }
        },
    ) {
        install(ContentNegotiation) { json(VerborumJson) }
    }

    private fun MockRequestHandleScope.ok(body: String) =
        respond(body, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))

    private fun MockRequestHandleScope.notFound() = respond(
        content = """{"status":404,"error":"RecordNotFoundException"}""",
        status = HttpStatusCode.NotFound,
        headers = headersOf("Content-Type", "application/json"),
    )

    val fetchContent: FetchImportedDictionaryContent = NetworkImportedDictionaryContent(
        dictionaryApi = DictionaryApi(client),
        wordApi = WordApi(client),
    )

    fun service(
        store: ImportedDictionaryStore = InMemoryImportedDictionaryStore(),
        profileService: ProfileService = FakeProfileService(),
        now: Long = 10_000L,
    ) = ImportedDictionaryService(
        store = store,
        profileService = profileService,
        fetchContent = fetchContent,
        time = TimeProvider { now },
    )
}

/**
 * An imported-dictionary service with **no network at all** — the content fetch answers from a map.
 *
 * What the view-model tests use. A mock HTTP engine driven from a view model's scope does not settle
 * under `advanceUntilIdle` on Kotlin/Native, so a test built on one passes on JS and hangs on iOS;
 * `SharedDictionaryBackend` stays for the tests that are *about* the fetch and await it directly.
 */
internal fun offlineImportedService(
    store: ImportedDictionaryStore = InMemoryImportedDictionaryStore(),
    profileService: ProfileService = FakeProfileService(),
    /** `dictionaryId` → what the owner currently has, or absent for one that is unreadable. */
    owners: Map<String, ImportedContent> = emptyMap(),
    now: Long = 10_000L,
) = ImportedDictionaryService(
    store = store,
    profileService = profileService,
    fetchContent = { dictionaryId, _, _ -> Outcome.Success(owners[dictionaryId]) },
    time = TimeProvider { now },
)

/** The owner's dictionary as ms_dictionary answers it. */
internal fun sharedDictionaryBody(
    id: String,
    userId: String = "publisher-1",
    name: String = "Shared $id",
    fromLang: String = "EN",
    toLang: String = "DE",
) = """{"dictionaryId":"$id","userId":"$userId","name":"$name","isPublic":true,""" +
    """"fromLang":"$fromLang","toLang":"$toLang"}"""

/** Its words, whose `level` is always null — it is the owner's, never the reader's. */
internal fun sharedWordsBody(vararg ids: String, dictionaryId: String = "d1") =
    ids.joinToString(prefix = "[", postfix = "]") { id ->
        """{"wordId":"$id","dictionaryId":"$dictionaryId","word":"$id",""" +
            """"translation":"$id-t","level":null}"""
    }
