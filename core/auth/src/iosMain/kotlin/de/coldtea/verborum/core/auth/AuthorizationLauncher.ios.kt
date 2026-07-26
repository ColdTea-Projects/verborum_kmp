package de.coldtea.verborum.core.auth

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AuthenticationServices.ASPresentationAnchor
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject
import kotlin.coroutines.resume

/** The system browser session; never a `WKWebView`, which could read the user's credentials. */
actual fun createAuthorizationLauncher(): AuthorizationLauncher = WebAuthenticationLauncher()

/** iOS gets its redirect in the completion handler, so there is never a start-URL code to consume. */
private class WebAuthenticationLauncher : AuthorizationLauncher {

    // Held for the lifetime of the launcher: ASWebAuthenticationSession keeps only a weak
    // reference to its context provider, and a collected provider fails the presentation.
    private val presentationContext = PresentationContext()

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun authorize(url: String, redirectUri: String): AuthorizationResult =
        suspendCancellableCoroutine { continuation ->
            val callbackScheme = NSURL(string = redirectUri).scheme

            val session = ASWebAuthenticationSession(
                uRL = NSURL(string = url),
                callbackURLScheme = callbackScheme,
                completionHandler = { callbackUrl, error ->
                    val result = when {
                        callbackUrl != null -> callbackUrl.toAuthorizationResult()
                        // Dismissing the sheet is the only error worth treating as "no decision".
                        error?.code?.toInt() == CANCELLED_LOGIN -> AuthorizationResult.Cancelled
                        else -> AuthorizationResult.Failed(error?.localizedDescription)
                    }
                    if (continuation.isActive) continuation.resume(result)
                },
            )

            session.presentationContextProvider = presentationContext
            // Reuses the system cookie jar so an existing SSO session signs in without a prompt.
            session.prefersEphemeralWebBrowserSession = false

            continuation.invokeOnCancellation { session.cancel() }

            if (!session.start() && continuation.isActive) {
                continuation.resume(AuthorizationResult.Failed("The browser session did not start."))
            }
        }

    override fun consumeRedirect(): AuthorizationResult? = null
}

@OptIn(ExperimentalForeignApi::class)
private fun NSURL.toAuthorizationResult(): AuthorizationResult {
    val items = NSURLComponents(uRL = this, resolvingAgainstBaseURL = false)?.queryItems
        ?.filterIsInstance<NSURLQueryItem>()
        .orEmpty()

    fun value(name: String): String? = items.firstOrNull { it.name == name }?.value

    val code = value("code")
    val state = value("state")

    return when {
        code != null && state != null -> AuthorizationResult.Code(code = code, state = state)
        // Keycloak reports a refused authorization as `error` on the redirect, not as a failure.
        else -> AuthorizationResult.Failed(value("error"))
    }
}

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
private class PresentationContext :
    NSObject(),
    ASWebAuthenticationPresentationContextProvidingProtocol {

    override fun presentationAnchorForWebAuthenticationSession(
        session: ASWebAuthenticationSession,
    ): ASPresentationAnchor = activeWindow() ?: UIWindow()

    /** The scene's key window — the anchor the sheet is presented from. */
    private fun activeWindow(): UIWindow? =
        (UIApplication.sharedApplication.connectedScenes.firstOrNull() as? UIWindowScene)?.keyWindow
}

/** `ASWebAuthenticationSessionErrorCodeCanceledLogin` — the user dismissed the sheet. */
private const val CANCELLED_LOGIN = 1
