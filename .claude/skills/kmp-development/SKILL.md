---
name: kmp-development
description: Day-to-day Kotlin Multiplatform implementation workflow for this repo — source-set layout, expect/actual mechanics and their pitfalls, adding dependencies through the version catalog, Koin wiring, and the compile/test commands that verify a change on iOS and web. Load before writing or editing any Kotlin in commonMain/iosMain/webMain, adding a dependency, or introducing a platform-specific implementation.
---

# KMP development workflow

## Source sets

`KmpLibraryConventionPlugin` declares `iosArm64`, `iosSimulatorArm64`, `js { browser(); nodejs() }`
and `wasmJs { browser(); nodejs() }`. The default hierarchy template gives you these intermediate
source sets — prefer the widest one that compiles:

```
commonMain ─┬─ iosMain      (iosArm64 + iosSimulatorArm64)
            └─ webMain      (jsMain + wasmJsMain)
                 ├─ jsMain
                 └─ wasmJsMain
```

`commonTest`, `iosTest`, `webTest` mirror this. **Write in `commonMain` by default.** Dropping to
`webMain`/`iosMain` needs a reason a comment can state in one line.

## expect / actual

An `expect` declaration needs an `actual` in **every** leaf target, so an `expect` in `commonMain`
must be satisfied by `iosMain` + `webMain` (or `jsMain` + `wasmJsMain`).

Patterns already in the repo, follow them:

```kotlin
// commonMain — factory function, not a platform class
expect fun createTokenStorage(): TokenStorage

// commonMain — internal seam so common code stays testable
internal expect fun currentEpochSeconds(): Long
internal expect fun platformHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient
```

Prefer **expect factory function returning a common interface** over `expect class`: it keeps the
shared API stable and lets tests substitute a plain fake (`InMemoryTokenStorage`).

### The `js(...)` constraint — the one trap that bites here

`js("…")` bodies are **not allowed in an intermediate source set** (`webMain`). So shared web logic
lives in `webMain` and delegates to a thin per-target bridge:

```kotlin
// webMain — shared logic
internal expect fun localStorageGet(key: String): String

// jsMain / wasmJsMain — the bridge only
internal actual fun localStorageGet(key: String): String = ...
```

Keep the bridge as small as physically possible; anything with logic belongs in `webMain`.

### iOS interop

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

## Adding a dependency

1. Add the version to `[versions]` and the artifact to `[libraries]` in
   `gradle/libs.versions.toml` — **never** a hardcoded coordinate in a module build file.
2. If it belongs to every module of a kind, add it to the matching convention plugin
   (`KmpComposeConventionPlugin`, `KmpFeatureConventionPlugin`) instead of each build file.
3. Convention-plugin code reads the catalog through `libs.library("alias")` (see `Catalogs.kt`) —
   note the **dash-separated** alias form there (`"androidx-lifecycle-viewmodel"`), while module
   build files use the accessor form (`libs.androidx.lifecycle.viewmodel`).
4. Use `api` only when the type appears in the module's own public signatures (as `core:common`
   does for `lifecycle-viewmodel`); otherwise `implementation`.

## Coroutines on these targets

- `kotlinx-coroutines-core` is on every module's `commonMain` via the base convention plugin.
- **JS and Wasm are single-threaded.** There is no `Dispatchers.IO`, and `runBlocking` does not
  exist. Never write blocking code in `commonMain`.
- Do not name a dispatcher in shared code; suspend and let the caller's scope decide. If a
  dispatcher is genuinely required, inject it as a `CoroutineContext` constructor parameter.
- Always rethrow `CancellationException` before a broad `catch` (`apiCall` shows the shape).
- Serialise shared mutable state with a `Mutex` (`AuthSession`), not with platform locks.

## Wiring a new piece of work end to end

1. `data/` — model, repository interface, implementation returning `Outcome`.
2. `ui/` — `State` data class, `Effect` sealed interface, `ViewModel : BaseViewModel<…>`.
3. `ui/` — stateful screen (`koinViewModel()`, `collectAsStateWithLifecycle()`) + stateless content.
4. `di/` — bind the repository and `viewModelOf(::Vm)` in the feature's module.
5. `navigation/` — `@Serializable` route, `composable<Route>`, navigation lambdas.
6. Register in `composeApp` if it is a new module or tab.
7. Verify (below).

## Verify a change

Compile both platforms — a change that compiles for web can still fail for Kotlin/Native:

```bash
./gradlew compileKotlinJs compileKotlinWasmJs        # web
./gradlew compileKotlinIosSimulatorArm64             # iOS
./gradlew jsNodeTest wasmJsNodeTest                  # web tests
./gradlew iosSimulatorArm64Test                      # iOS tests (needs Xcode CLT)
./gradlew build                                      # everything
```

Scope to a module while iterating: `./gradlew :feature:bibliotheca:compileKotlinJs`.

## Checklist

- [ ] Code sits in the widest source set that compiles
- [ ] Every `expect` has an actual for iOS **and** both web targets
- [ ] No `js(...)` in `webMain`; bridges are minimal
- [ ] Dependency added via the version catalog, `api` only where justified
- [ ] No blocking calls, no `Dispatchers.IO`, `CancellationException` rethrown
- [ ] Compiles for `Js`, `WasmJs` and `IosSimulatorArm64`
