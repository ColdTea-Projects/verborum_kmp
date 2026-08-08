---
name: webapp-security
description: Reviews and hardens the Kotlin/Wasm + Kotlin/JS web target — token storage and XSS exposure, OAuth/PKCE correctness, Content-Security-Policy for Wasm, CORS and same-origin assumptions, secrets in the client bundle, and logging leaks. Use when touching core:auth webMain, core:network config, the web host page, any js(...) bridge, anything handling tokens or credentials, or when reviewing the web build for security.
---

# Web security

Threat model: the entire bundle is **public, attacker-readable, attacker-modifiable code running in
a hostile browser**. Nothing shipped to the client is a secret, and every request the client makes
can be forged. Security decisions therefore live at the API boundary; the client's job is to not
*leak* and to not *widen* the attack surface.

## Quick start

Before changing anything under `core/auth` on web, confirm the bridges are still data-only:

```bash
grep -rn "innerHTML\|eval(\|document.write\|Function(" composeApp/src core/*/src
grep -rn "localStorage\|sessionStorage" core/auth/src/webMain
```

The first must return nothing. The second must show tokens in `localStorage` and the PKCE verifier
in `sessionStorage` — not the other way round.

## Token storage — where it stands

`core/auth/src/webMain/.../TokenStorage.web.kt` uses **`localStorage`** (key
`de.coldtea.verborum.auth.tokens`). This is a **deliberate product decision**, taken knowingly: the
app requires a session that lasts until the user signs out, not one that dies with the tab.

What it costs, so nobody has to rediscover it: any script on the origin can read `localStorage`, so
a single XSS or compromised dependency exfiltrates the access **and** refresh token, and they
persist indefinitely. `sessionStorage`, `IndexedDB` and non-`HttpOnly` cookies are all equally
readable — the lifetime was the only lever, and it was spent on purpose.

**Do not "fix" this by switching back to `sessionStorage`** — that reintroduces the
logout-on-tab-close the change was made to remove. The one upgrade that actually helps:

1. Refresh token in an **`HttpOnly; Secure; SameSite=Strict` cookie** set by a backend endpoint; the
   client never sees it. Access token in memory only.
2. Failing that, access token in memory + short TTL, refresh via that cookie.

`TokenStorage` is the seam, so either is a `webMain`-only change and no feature is touched.

Because the tokens outlive the tab, the surrounding defences carry more weight: the `js(...)`
bridges stay data-only, the CSP keeps third-party script out, and `PendingAuthorizationStore` keeps
the PKCE verifier in `sessionStorage` (per-attempt, short-lived — do not move that one).

## XSS is the whole game

Compose renders to a canvas, so there is no HTML templating to inject into — that removes the usual
XSS surface but does not remove it entirely:

- Never `innerHTML`, `eval`, `document.write` or `Function(...)` in a `js(...)` bridge. Bridges stay
  minimal and data-only (`localStorageGet/Set/Remove` is the right shape).
- Never build a URL for `window.location` / `window.open` from server or user data without
  validating scheme and host against an allowlist — `javascript:` and `data:` URLs are code
  execution.
- Treat every dependency added to the web bundle as running with full access to the user's tokens.
- Do not render backend-supplied markup or HTML anywhere.

## The rest of the surface

- The CSP header (including `wasm-unsafe-eval`), the other response headers, and why the API is
  same-origin — plus what changes if it stops being:
  [references/csp_and_origin.md](references/csp_and_origin.md).
- PKCE and `state` rules, the refresh endpoint contract, secrets in the bundle, source maps and
  logging: [references/oauth_and_bundle.md](references/oauth_and_bundle.md).

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
