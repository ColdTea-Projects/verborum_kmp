package de.coldtea.verborum.core.auth

import de.coldtea.verborum.core.common.browserFragment
import de.coldtea.verborum.core.common.browserOrigin
import io.ktor.http.parseQueryString
import kotlinx.coroutines.awaitCancellation

private const val RETURN_TO_KEY = "de.coldtea.verborum.auth.returnTo"

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
        // Keycloak's login page is a whole navigation away, which takes the fragment — the route the
        // user was trying to reach — with it. Parking it for the redirect to pick up is what lets a
        // shared link to a dictionary survive signing in.
        rememberReturnTo()

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
        // up in browser history, in the referrer of any later request, and in shared links. The
        // route the user was heading for goes back on in its place.
        browserReplaceUrl(browserOrigin() + "/" + consumeReturnTo())

        return when {
            code != null && state != null -> AuthorizationResult.Code(code = code, state = state)
            else -> AuthorizationResult.Failed(error)
        }
    }

    /**
     * Stores the current URL fragment for [consumeReturnTo], or clears the last one when there is
     * none — otherwise a second sign-in from the app's root would reopen wherever the first one came
     * from. `sessionStorage` because the value must survive exactly the same round trip as the PKCE
     * verifier beside it, and no longer.
     */
    private fun rememberReturnTo() {
        val fragment = browserFragment().takeIf { it.isFragment() }

        if (fragment != null) {
            sessionStorageSet(RETURN_TO_KEY, fragment)
        } else {
            sessionStorageRemove(RETURN_TO_KEY)
        }
    }

    /** Reads and removes it: one redirect returns to one route. */
    private fun consumeReturnTo(): String {
        val stored = sessionStorageGet(RETURN_TO_KEY)
        sessionStorageRemove(RETURN_TO_KEY)

        return stored.takeIf { it.isFragment() }.orEmpty()
    }
}

/**
 * A fragment comes from the address bar, so it is whatever a link the user followed put there. It is
 * only ever appended after the app's own origin — and a fragment cannot change which document a URL
 * addresses — but it is still checked for shape on the way in *and* on the way out, so nothing but a
 * fragment can be concatenated onto that origin.
 */
private fun String.isFragment(): Boolean = startsWith("#") && length > 1
