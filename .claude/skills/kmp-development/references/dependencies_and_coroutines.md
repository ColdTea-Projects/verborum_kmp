# Adding dependencies, and coroutines on these targets

Answers one decision: **how does this library or this concurrency get into shared code without
breaking iOS or web?**

## Adding a dependency

1. Add the version to `[versions]` and the artifact to `[libraries]` in
   `gradle/libs.versions.toml` — **never** a hardcoded coordinate in a module build file.
2. If it belongs to every module of a kind, add it to the matching convention plugin
   (`KmpComposeConventionPlugin`, `KmpFeatureConventionPlugin`) instead of each build file.
3. Convention-plugin code reads the catalog through `libs.library("alias")` (see `Catalogs.kt`) —
   note the **dash-separated** alias form there (`"androidx-lifecycle-viewmodel"`), while module
   build files use the accessor form (`libs.androidx.lifecycle.viewmodel`).
4. Use `api` only when the type appears in the module's own public signatures (as `core:common` does
   for `lifecycle-viewmodel`); otherwise `implementation`.

A dependency with no Kotlin/Native artifact will compile for web and fail at the iOS link step —
check the artifact's published targets before adding it to `commonMain`.

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
7. Compile both targets and run the tests.
