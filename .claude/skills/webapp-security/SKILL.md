---
name: webapp-security
description: Security review and hardening for the Kotlin/Wasm + Kotlin/JS web target — token storage and XSS exposure, OAuth/PKCE correctness, Content-Security-Policy for Wasm, CORS and same-origin assumptions, secrets in the client bundle, and logging leaks. Load before touching core:auth webMain, core:network config, the web host page, anything handling tokens or credentials, or when reviewing the web build for security.
---

# Web security

Threat model: the entire bundle is **public, attacker-readable, attacker-modifiable code running in a
hostile browser**. Nothing shipped to the client is a secret, and every request the client makes can
be forged. Security decisions therefore live at the API boundary; the client's job is to not *leak*
and to not *widen* the attack surface.

## Token storage — where it stands

`core/auth/src/webMain/.../TokenStorage.web.kt` uses **`localStorage`** (key
`de.coldtea.verborum.auth.tokens`). This is a **deliberate product decision**, taken knowingly: the app
requires a session that lasts until the user signs out, not one that dies with the tab.

What it costs, so nobody has to rediscover it: any script on the origin can read `localStorage`, so a
single XSS or compromised dependency exfiltrates the access **and** refresh token, and they persist
indefinitely. `sessionStorage`, `IndexedDB` and non-`HttpOnly` cookies are all equally readable — the
lifetime was the only lever, and it was spent on purpose.

**Do not "fix" this by switching back to `sessionStorage`** — that reintroduces the logout-on-tab-close
the change was made to remove. The one upgrade that actually helps:

1. Refresh token in an **`HttpOnly; Secure; SameSite=Strict` cookie** set by a backend endpoint; the
   client never sees it. Access token in memory only.
2. Failing that, access token in memory + short TTL, refresh via that cookie.

`TokenStorage` is the seam, so either is a `webMain`-only change and no feature is touched.

Because the tokens now outlive the tab, the surrounding defences carry more weight: the `js(...)`
bridges stay data-only, the CSP keeps third-party script out, and `PendingAuthorizationStore` keeps the
PKCE verifier in `sessionStorage` (per-attempt, short-lived — do not move that one).

**`TokenRefresher` is wired to the real endpoint** (`KeycloakAuthClient.refresh`, bound in
`composeApp/di/AppModule.kt`). Keep it that way: never log the request or response, keep treating any
failure as sign-out (`AuthSession` clears storage and publishes `SignedOut`), and keep the
single-flight `Mutex` so a burst of 401s cannot fan out into parallel refreshes.

## XSS is the whole game

Compose renders to a canvas, so there is no HTML templating to inject into — that removes the usual
XSS surface but does not remove it entirely:

- Never `innerHTML`, `eval`, `document.write` or `Function(...)` in a `js(...)` bridge. Bridges stay
  minimal and data-only (`localStorageGet/Set/Remove` is the right shape).
- Never build a URL for `window.location` / `window.open` from server or user data without
  validating scheme and host against an allowlist — `javascript:` and `data:` URLs are code
  execution.
- Treat every dependency added to the web bundle as running with full access to your tokens.
- Do not render backend-supplied markup or HTML anywhere.

## Content-Security-Policy

The static host must send a CSP. Kotlin/Wasm needs `wasm-unsafe-eval`; the JS target may need
`unsafe-eval` depending on the toolchain output — verify in the browser console rather than
pre-emptively loosening the policy. Starting point:

```
Content-Security-Policy:
  default-src 'none';
  script-src 'self' 'wasm-unsafe-eval';
  style-src 'self';
  img-src 'self' data:;
  font-src 'self';
  connect-src 'self';
  frame-ancestors 'none';
  base-uri 'none';
  form-action 'none'
```

`connect-src 'self'` works because every web request is same-origin by design: `defaultApiConfig()`
uses `<origin>/api` and `defaultAuthConfig()` uses `<origin>/auth`, served by a reverse proxy
deployed and by the dev server locally (`composeApp/webpack.config.d/devServerProxy.js`). Keep it that
way — pointing either at another origin in dev is what reintroduces CORS, and it hides the failure
behind a browser-only error the Node tests cannot catch.

**If the base URL ever becomes cross-origin**, that is a security change, not a config
tweak: it needs `connect-src` widened, a CORS policy on the API with an explicit origin allowlist
(never `Access-Control-Allow-Origin: *` together with credentials), and it re-exposes preflight and
cookie-scoping concerns.

Also send: `Strict-Transport-Security`, `X-Content-Type-Options: nosniff`,
`Referrer-Policy: strict-origin-when-cross-origin`, `Cross-Origin-Opener-Policy: same-origin`, and
`Permissions-Policy` denying what the app does not use.

## OAuth / PKCE

The flow is implemented: `AuthorizationLauncher.web.kt` (top-level redirect, code stripped from the
URL with `history.replaceState` as soon as it is consumed), `AuthService` (verifies `state`, exchanges
the code), `AuthConfig.web.kt` (Keycloak expected at same-origin `/auth`, so `connect-src 'self'`
still holds). `Pkce` (RFC 7636, S256, 32 random bytes → 43-char verifier) is correct and
test-covered — keep using it and do not weaken to `plain`. When changing the flow:

- Generate the verifier per attempt; never reuse, never log it.
- Send an unguessable `state` and verify it on the callback — this is the CSRF defence and PKCE does
  **not** replace it.
- Validate the `redirect_uri` against an exact allowlist; an open redirect on the callback leaks the
  authorization code.
- Verify the web RNG bridge (`secureRandomBytes`) is backed by `crypto.getRandomValues`, never
  `Math.random()`. Confirm this whenever the `jsMain`/`wasmJsMain` bridge changes.
- Never place tokens or codes in a URL fragment or query you then leave in browser history.

## Secrets and build output

- **No secrets in the client**: no API keys, client secrets, admin URLs or internal hostnames. Public
  OAuth client IDs are fine. Anything in `libs.versions.toml`, `ApiConfig` or a resource file ships
  to users.
- Do not publish source maps for production builds unless you accept full source disclosure; check
  `composeApp/build/dist/wasmJs/productionExecutable/` before deploying.
- Serve over HTTPS only.

## Logging

`createHttpClient` installs Ktor `Logging` only when `config.enableLogging` is true, and
`defaultApiConfig()` on web sets it to `false` — keep it that way for production. `LogLevel.ALL` or
`HEADERS` prints the `Authorization` header to the browser console; if you need it while debugging,
use `LogLevel.INFO` and never commit an increase. Never log tokens, PII, or full request bodies.

## Review checklist

- [ ] No credential written outside `TokenStorage`; the PKCE verifier stays in `sessionStorage`
- [ ] No `innerHTML`/`eval`/dynamic script in any `js(...)` bridge; bridges data-only
- [ ] Redirect/`window.open` targets validated against an allowlist
- [ ] CSP still valid for what the change adds; `connect-src` unchanged or deliberately widened
- [ ] API stays same-origin, or CORS + credential scoping explicitly reviewed
- [ ] PKCE `S256`, fresh verifier, `state` verified, CSRNG-backed RNG
- [ ] `enableLogging` false; no header/body logging; nothing sensitive logged
- [ ] No secrets in the bundle; source maps not shipped
- [ ] Auth failure paths clear stored tokens; refresh stays single-flight
