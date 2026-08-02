package de.coldtea.verborum.core.auth

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

/**
 * The redirect comes back through the app's custom scheme, registered in `iosApp/iosApp/Info.plist`
 * and matched by `ASWebAuthenticationSession`. A universal link would be preferable — a custom
 * scheme can be claimed by another installed app — and is the upgrade to make when the app has a
 * verified domain to host the association file on.
 *
 * A debug binary talks to the local Keycloak the web dev server proxies to (`:8180`), which is plain
 * http and so needs the `NSAllowsLocalNetworking` exception in `Info.plist`. The gate is on the
 * binary kind rather than a constant, so a release build can never ship the localhost issuer.
 */
@OptIn(ExperimentalNativeApi::class)
actual fun defaultAuthConfig(): AuthConfig = AuthConfig(
    issuer = if (Platform.isDebugBinary) {
        "http://localhost:8180/realms/verborum"
    } else {
        "https://auth.verborum.coldtea.de/realms/verborum"
    },
    clientId = "verborum-app",
    redirectUri = "de.coldtea.verborum://oauth2redirect/cb",
)
