package de.coldtea.verborum.core.auth

import io.ktor.http.parseQueryString
import kotlinx.coroutines.awaitCancellation

actual fun createAuthorizationLauncher(): AuthorizationLauncher = RedirectAuthorizationLauncher()

/**
 * The web flow is a top-level redirect rather than a popup: popups are blocked by default unless
 * they open in a direct gesture handler, and a redirect is what Keycloak's login page expects.
 *
 * The consequence is that [authorize] never returns — the page is gone. The code comes back to the
 * app's start URL and is picked up by [consumeRedirect] on the next launch.
 */
private class RedirectAuthorizationLauncher : AuthorizationLauncher {

    override suspend fun authorize(url: String, redirectUri: String): AuthorizationResult {
        // The URL is built from AuthConfig, never from server or user input, so there is no
        // untrusted scheme to validate here.
        browserNavigateTo(url)

        // Nothing after the navigation runs; suspending forever keeps that explicit rather than
        // letting a caller believe it got a result.
        awaitCancellation()
    }

    override fun consumeRedirect(): AuthorizationResult? {
        val query = browserSearch().removePrefix("?").takeIf { it.isNotEmpty() } ?: return null
        val parameters = parseQueryString(query)

        val code = parameters["code"]
        val state = parameters["state"]
        val error = parameters["error"]

        if (code == null && error == null) return null

        // Strip the query before doing anything with it: an authorization code left in the URL ends
        // up in browser history, in the referrer of any later request, and in shared links.
        browserReplaceUrl(browserOrigin() + "/")

        return when {
            code != null && state != null -> AuthorizationResult.Code(code = code, state = state)
            else -> AuthorizationResult.Failed(error)
        }
    }
}
