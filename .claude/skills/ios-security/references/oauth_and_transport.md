# OAuth and transport security on iOS

Answers one decision: **is this sign-in or network change safe to ship on iOS?**

## Transport

- HTTPS only. **Never** add `NSAllowsArbitraryLoads` (or a per-domain ATS exception) to
  `iosApp/iosApp/Info.plist`; the current plist has no ATS exceptions and should stay that way.
- The Darwin engine gets a fresh `defaultApiConfig()` per target — the iOS base URL is absolute, so
  confirm it is `https://` and points at production before release. A localhost/staging URL shipping
  in a release build is both a security and a privacy incident.
- Certificate pinning is worth considering for the auth endpoint (Darwin engine `handleChallenge`),
  but only with a documented rotation plan — a pin without one bricks the app.

## OAuth

- The flow is implemented: `core/auth/.../AuthorizationLauncher.ios.kt` wraps
  **`ASWebAuthenticationSession`**, `AuthService` verifies `state` and exchanges the code, and
  `AuthConfig.ios.kt` holds the endpoints. Never replace it with an embedded `WKWebView` or
  `UIWebView`: an in-app web view can read the user's credentials, breaks federated sign-in, and is
  an App Store review risk.
- The redirect scheme `de.coldtea.verborum` is registered in `iosApp/iosApp/Info.plist`
  (`CFBundleURLTypes`) and must stay in sync with `redirectUri` in `AuthConfig.ios.kt`.
- PKCE `S256` from `core:auth` is already correct and its iOS RNG is properly seeded —
  `secureRandomBytes` uses `SecRandomCopyBytes(kSecRandomDefault, …)`. Keep it; never substitute
  `arc4random`/`Random.Default` for cryptographic material.
- Register the redirect URI's custom scheme / universal link and validate the callback: verify
  `state`, and prefer a **universal link** over a custom scheme (custom schemes can be claimed by
  another app).
- Handle `signOut()` by clearing the Keychain, `LocalCache`, the local database, and any in-memory
  state. `OptionsViewModel` calls `BibliothecaDatabase.clear()` after `AuthService.signOut()` — the
  library is one user's content, and the next person to sign in on the device must not find it
  waiting.

## Logging

`ApiConfig.enableLogging` must be `false` in release builds — Ktor `Logging` at `HEADERS`/`ALL`
writes the `Authorization` header into the device console, which any connected Mac can read. Never
`println` a token, a request body or PII. Prefer no logging of network traffic in release at all.
