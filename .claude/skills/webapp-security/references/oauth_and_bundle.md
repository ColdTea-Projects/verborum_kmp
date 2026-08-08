# OAuth/PKCE on web, secrets in the bundle, and logging

Answers one decision: **is this sign-in change or this build output safe to ship to a browser?**

## OAuth / PKCE

The flow is implemented: `AuthorizationLauncher.web.kt` (top-level redirect, code stripped from the
URL with `history.replaceState` as soon as it is consumed), `AuthService` (verifies `state`,
exchanges the code), `AuthConfig.web.kt` (Keycloak expected at same-origin `/auth`, so
`connect-src 'self'` still holds). `Pkce` (RFC 7636, S256, 32 random bytes → 43-char verifier) is
correct and test-covered — keep using it and do not weaken to `plain`. When changing the flow:

- Generate the verifier per attempt; never reuse it, never log it.
- Send an unguessable `state` and verify it on the callback — this is the CSRF defence and PKCE does
  **not** replace it.
- Validate the `redirect_uri` against an exact allowlist; an open redirect on the callback leaks the
  authorization code.
- Verify the web RNG bridge (`secureRandomBytes`) is backed by `crypto.getRandomValues`, never
  `Math.random()`. Confirm this whenever the `jsMain`/`wasmJsMain` bridge changes.
- Never place tokens or codes in a URL fragment or query that is then left in browser history.

`TokenRefresher` is wired to the real endpoint (`KeycloakAuthClient.refresh`, bound in
`composeApp/di/AppModule.kt`). Keep it that way: never log the request or response, keep treating
any failure as sign-out (`AuthSession` clears storage and publishes `SignedOut`), and keep the
single-flight `Mutex` so a burst of 401s cannot fan out into parallel refreshes.

## Secrets and build output

- **No secrets in the client**: no API keys, client secrets, admin URLs or internal hostnames.
  Public OAuth client IDs are fine. Anything in `libs.versions.toml`, `ApiConfig` or a resource file
  ships to users.
- Do not publish source maps for production builds unless full source disclosure is acceptable;
  check `composeApp/build/dist/wasmJs/productionExecutable/` before deploying.
- Serve over HTTPS only.

## Logging

`createHttpClient` installs Ktor `Logging` only when `config.enableLogging` is true, and
`defaultApiConfig()` on web sets it to `false` — keep it that way for production. `LogLevel.ALL` or
`HEADERS` prints the `Authorization` header to the browser console; if it is needed while debugging,
use `LogLevel.INFO` and never commit an increase. Never log tokens, PII, or full request bodies.
