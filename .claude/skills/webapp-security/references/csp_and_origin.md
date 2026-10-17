# Content-Security-Policy, headers and the same-origin assumption

Answers one decision: **what may the browser load and talk to, and what changes if the API stops
being same-origin?**

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

`connect-src 'self'` covers the login: `defaultAuthConfig()` uses `<origin>/auth`, served by a reverse proxy when
deployed and by the dev server locally (`composeApp/webpack.config.d/devServerProxy.js`). Keep it that way —
pointing Keycloak at another origin is what reintroduces CORS on the token exchange.

**The API is cross-origin in development since P5-10.** `defaultApiConfig()` on a local origin returns the gateway
on `:8080`, and the gateway sends the CORS headers for the dev origin (`CORS_ALLOWED_ORIGINS`; exact origins,
never `*`; no credentials — the token is a Bearer header). The dev server sends no CSP, so nothing else changes
locally. A static host that does send the CSP above must add the API origin to `connect-src` before a web build
is pointed at an `api.` origin; a deployed build still uses `<origin>/api` until the release decides (Phase 5R).

**If the base URL ever becomes cross-origin**, that is a security change, not a config tweak: it
needs `connect-src` widened, a CORS policy on the API with an explicit origin allowlist (never
`Access-Control-Allow-Origin: *` together with credentials), and it re-exposes preflight and
cookie-scoping concerns.

Also send: `Strict-Transport-Security`, `X-Content-Type-Options: nosniff`,
`Referrer-Policy: strict-origin-when-cross-origin`, `Cross-Origin-Opener-Policy: same-origin`, and
`Permissions-Policy` denying what the app does not use.
