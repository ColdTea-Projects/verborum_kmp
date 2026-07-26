package de.coldtea.verborum.core.auth

/**
 * OpenID Connect endpoints for the Keycloak `verborum` realm, derived from the issuer.
 *
 * Kept as string concatenation rather than a discovery-document fetch so login costs no extra round
 * trip; the paths are stable Keycloak conventions. Mirrors `AuthConfig` in the Android app.
 *
 * A client id and an issuer URL are public values — they ship in the app bundle either way. Nothing
 * secret belongs in here: there is no client secret in a public OAuth client.
 */
data class AuthConfig(
    val issuer: String,
    val clientId: String,
    val redirectUri: String,
    /**
     * `offline_access` is requested explicitly — without it Keycloak issues no long-lived refresh
     * token and a client left alone for days is forced back to the login screen.
     */
    val scope: String = "openid profile email offline_access",
) {
    val authorizationEndpoint: String get() = "$issuer/protocol/openid-connect/auth"
    val tokenEndpoint: String get() = "$issuer/protocol/openid-connect/token"

    /** Hosted sign-up reuses the login flow against Keycloak's registrations endpoint. */
    val registrationEndpoint: String get() = "$issuer/protocol/openid-connect/registrations"
    val endSessionEndpoint: String get() = "$issuer/protocol/openid-connect/logout"
}

/** The endpoints for the current target; the redirect URI differs per platform. */
expect fun defaultAuthConfig(): AuthConfig
