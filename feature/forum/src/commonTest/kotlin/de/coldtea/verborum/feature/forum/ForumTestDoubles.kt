package de.coldtea.verborum.feature.forum

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.core.network.VerborumJson
import de.coldtea.verborum.core.profile.domain.ProfileService
import de.coldtea.verborum.core.profile.domain.UserProfile
import de.coldtea.verborum.core.profile.domain.VaultEntry
import de.coldtea.verborum.feature.forum.data.SharedDictionaryApi
import de.coldtea.verborum.feature.forum.data.MarketplaceApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** One scripted answer. The last one repeats, so a test scripts only what it cares about. */
internal data class Reply(val status: HttpStatusCode, val body: String = "{}")

/**
 * `ms_marketplace`, scripted in order.
 *
 * Driven through a [MockEngine] rather than a fake API class because what is under test here is
 * *which status means what*, and that is the whole contract with the marketplace. A fake API would
 * let a test state the outcome directly and so could never catch a rule that branches on the wrong
 * code — or a query that comma-joins a parameter the server splits on commas.
 */
internal class MarketplaceBackend(private vararg val replies: Reply) {

    private val _requests = mutableListOf<Url>()

    /** Every request made, in order, so a test can assert on the query it built. */
    val requests: List<Url> get() = _requests

    private var index = 0

    val api = MarketplaceApi(
        HttpClient(
            MockEngine { request ->
                _requests += request.url

                val reply = replies.getOrElse(index) { replies.last() }
                index++

                respond(reply.body, reply.status, headersOf("Content-Type", "application/json"))
            },
        ) {
            install(ContentNegotiation) { json(VerborumJson) }
        },
    )
}

internal val forbidden = Reply(
    HttpStatusCode.Forbidden,
    """{"status":403,"error":"ForbiddenOperationException"}""",
)

internal val notFound = Reply(
    HttpStatusCode.NotFound,
    """{"status":404,"error":"RecordNotFoundException"}""",
)

internal val serverFault = Reply(
    HttpStatusCode.InternalServerError,
    """{"status":500,"error":"Exception","errorDetail":"Internal server error"}""",
)

/** A page of listings as the marketplace answers it. */
internal fun pageBody(vararg ids: String, hasNext: Boolean = false): String {
    val items = ids.joinToString(",") { id ->
        """{"dictionaryId":"$id","publisherId":"pub-1","publisherName":"Anna Bauer",""" +
            """"name":"Dictionary $id","fromLang":"EN","toLang":"DE","tags":["travel"],""" +
            """"importCount":3,"ratingAverage":4.3,"ratingCount":12,""" +
            """"publishedAt":"2026-07-19T17:01:21.303971Z"}"""
    }

    return """{"items":[$items],"page":0,"size":15,"hasNext":$hasNext}"""
}

/**
 * A `ms_dictionary` that is simply not there.
 *
 * The download tests never read a dictionary's contents — the import is what they are about — so an
 * unreachable one keeps them honest: a test that started passing because of a content fetch would
 * be testing the wrong thing.
 */
internal fun offlineSharedDictionaryApi() = SharedDictionaryApi(
    HttpClient(MockEngine { respondError(HttpStatusCode.ServiceUnavailable) }) {
        install(ContentNegotiation) { json(VerborumJson) }
    },
)

/** Nobody signed in, which is all the default-filter use case needs to contribute nothing. */
internal object NoProfile : ProfileService {
    override val profile: Flow<UserProfile?> = flowOf(null)
    override val current: UserProfile? = null
    override suspend fun refreshProfile(): Outcome<UserProfile> =
        Outcome.Failure(VerborumError.Unauthorized)

    override suspend fun userId(): String? = null
    override suspend fun updateProfile(displayName: String?, accepted: Boolean?): Outcome<Unit> =
        Outcome.Success(Unit)

    override suspend fun vault(): Outcome<List<VaultEntry>> = Outcome.Success(emptyList())
    override suspend fun removeFromVault(dictionaryId: String): Outcome<Unit> = Outcome.Success(Unit)
    override fun clear() = Unit
}
