package de.coldtea.verborum.core.profile

import de.coldtea.verborum.core.auth.UserIdentity
import de.coldtea.verborum.core.network.VerborumJson
import de.coldtea.verborum.core.profile.data.UserApi
import de.coldtea.verborum.core.profile.data.VaultApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json

internal val testIdentity = UserIdentity(
    subject = "765a81ed-2612-4dcc-bf7a-3d5fecf3d0d6",
    email = "anna@example.test",
    displayName = "Anna Bauer",
)

/** One scripted answer, in the order the test expects the calls to happen. */
internal data class Reply(val status: HttpStatusCode, val body: String = "{}")

internal data class RecordedRequest(val method: String, val path: String, val body: String)

/**
 * Drives the `ms_user` endpoints through a [MockEngine] rather than a hand-written fake of the API
 * class.
 *
 * Deliberate: what is under test here is *which status means what, and what gets sent* — and that is
 * the whole contract with ms_user. A fake API class would let a test state an `Outcome` outright, so
 * it could never catch a mapping that branches on the wrong status, or a request body that encodes a
 * `null` where the field has to be **absent** to mean "unchanged".
 *
 * Replies are consumed in order; once they run out the last one repeats, so a test only scripts the
 * answers it actually cares about.
 */
internal class UserServiceHarness(private vararg val replies: Reply) {

    private val recorded = mutableListOf<RecordedRequest>()

    /** Every request made, in order. */
    val requests: List<RecordedRequest> get() = recorded

    private var index = 0

    private val client: HttpClient = HttpClient(
        MockEngine { request ->
            recorded += RecordedRequest(
                method = request.method.value,
                path = request.url.encodedPath,
                body = (request.body as? OutgoingContent.ByteArrayContent)?.bytes()?.decodeToString().orEmpty(),
            )

            val reply = replies.getOrElse(index) { replies.last() }
            index++

            respond(
                content = reply.body,
                status = reply.status,
                headers = headersOf("Content-Type", "application/json"),
            )
        },
    ) {
        install(ContentNegotiation) { json(VerborumJson) }
    }

    val userApi = UserApi(client)

    val vaultApi = VaultApi(client)
}

/** The shape `GET users/me` answers with. */
internal fun profileBody(
    id: String = "4140c0de-0000-4000-8000-0000000000bb",
    email: String = "anna@example.test",
    displayName: String? = "Anna Bauer",
    accepted: Boolean = false,
    version: String? = null,
): String = buildString {
    append("""{"id":"$id","email":"$email"""")
    displayName?.let { append(""","displayName":"$it"""") }
    append(""","marketplaceAgreementAccepted":$accepted""")
    version?.let { append(""","marketplaceAgreementVersion":"$it"""") }
    append("}")
}

/** The error body every service answers a non-2xx with (marketplace guide §7). */
internal fun errorBody(error: String, detail: String): String =
    """{"status":400,"error":"$error","errorDetail":"$detail"}"""
