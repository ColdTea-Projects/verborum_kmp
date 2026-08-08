# expect/actual patterns and platform interop

Answers one decision: **how do I declare this platform-specific thing without breaking a target?**

An `expect` declaration needs an `actual` in **every** leaf target, so an `expect` in `commonMain`
must be satisfied by `iosMain` + `webMain` (or `jsMain` + `wasmJsMain`).

Patterns already in the repo — follow them:

```kotlin
// commonMain — factory function, not a platform class
expect fun createTokenStorage(): TokenStorage

// commonMain — internal seam so common code stays testable
internal expect fun currentEpochSeconds(): Long
internal expect fun platformHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient
```

Prefer an **expect factory function returning a common interface** over `expect class`: it keeps the
shared API stable and lets tests substitute a plain fake (`InMemoryTokenStorage`).

## The `js(...)` constraint — the one trap that bites here

`js("…")` bodies are **not allowed in an intermediate source set** (`webMain`). So shared web logic
lives in `webMain` and delegates to a thin per-target bridge:

```kotlin
// webMain — shared logic
internal expect fun localStorageGet(key: String): String

// jsMain / wasmJsMain — the bridge only
internal actual fun localStorageGet(key: String): String = ...
```

Keep the bridge as small as physically possible; anything with logic belongs in `webMain`.

## iOS interop

Apple APIs come from `platform.*` (`platform.Foundation.NSUserDefaults`,
`platform.Security.SecRandomCopyBytes`). C-interop needs opt-ins — annotate the narrowest scope:

```kotlin
@OptIn(ExperimentalForeignApi::class)
internal actual fun secureRandomBytes(size: Int): ByteArray = ByteArray(size).also { bytes ->
    bytes.usePinned { SecRandomCopyBytes(kSecRandomDefault, size.toULong(), it.addressOf(0)) }
}
```

Anything exposed to Swift must be a top-level function or a class — Kotlin `object`s, sealed
hierarchies and default arguments translate awkwardly across the ObjC bridge.
