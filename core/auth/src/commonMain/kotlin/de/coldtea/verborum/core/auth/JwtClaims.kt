package de.coldtea.verborum.core.auth

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import de.coldtea.verborum.core.network.VerborumJson

/** The identity claims the client needs; never used for an authorization decision. */
data class UserIdentity(
    val subject: String,
    val email: String? = null,
    val displayName: String? = null,
)

/**
 * Reads standard claims out of a JWT **without verifying its signature** — the client only needs
 * `sub`/`email`/name to know who is signed in, and the services do the real validation. Never treat
 * a claim read here as trusted for anything that grants access.
 */
object JwtClaims {

    /** Null for anything that is not a decodable JWT with a `sub` claim. */
    fun identityOf(accessToken: String?, idToken: String? = null): UserIdentity? {
        val subject = claims(accessToken)?.string("sub") ?: return null
        val profile = claims(idToken)

        return UserIdentity(
            subject = subject,
            email = profile?.string("email"),
            displayName = profile?.string("name") ?: profile?.string("preferred_username"),
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun claims(jwt: String?): JsonObject? {
        if (jwt.isNullOrBlank()) return null

        val payload = jwt.split(".").getOrNull(1) ?: return null

        // A malformed or non-JWT token is an expected input here, not a programmer error.
        return runCatching {
            val decoded = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
                .decode(payload)
            VerborumJson.parseToJsonElement(decoded.decodeToString()).jsonObject
        }.getOrNull()
    }

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
}
