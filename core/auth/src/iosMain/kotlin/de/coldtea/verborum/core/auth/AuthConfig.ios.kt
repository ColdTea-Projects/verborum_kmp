package de.coldtea.verborum.core.auth

/**
 * The redirect comes back through the app's custom scheme, registered in `iosApp/iosApp/Info.plist`
 * and matched by `ASWebAuthenticationSession`. A universal link would be preferable — a custom
 * scheme can be claimed by another installed app — and is the upgrade to make when the app has a
 * verified domain to host the association file on.
 *
 * The issuer is `https` on purpose: pointing this at a local Keycloak over plain http would need an
 * App Transport Security exception (`NSAllowsLocalNetworking`), which this repo deliberately does
 * not ship.
 */
actual fun defaultAuthConfig(): AuthConfig = AuthConfig(
    issuer = "https://auth.verborum.coldtea.de/realms/verborum",
    clientId = "verborum-app",
    redirectUri = "de.coldtea.verborum://oauth2redirect/cb",
)
