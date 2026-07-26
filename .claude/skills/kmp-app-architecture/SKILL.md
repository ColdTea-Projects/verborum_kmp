---
name: kmp-app-architecture
description: The module graph, layering rules and MVVM/MVI contracts of this Kotlin Multiplatform app. Load before adding or moving a module, adding a feature, deciding where a class belongs, wiring Koin, changing navigation, or reviewing whether a change respects layering. Triggers on "new feature module", "where should this live", "core vs feature", "dependency direction", "ViewModel", "navigation graph", "Koin module", "clean architecture".
---

# Verborum KMP architecture

Targets: **iOS** (`iosArm64`, `iosSimulatorArm64`) and **web** (`js`, `wasmJs`). There is no
Android target — never add `androidMain` code or Android-only APIs to shared source sets.

## Module graph

```
composeApp  ──▶ feature/*  ──▶ core/*
   (shell)        (screens)     (plumbing)
```

| Module               | Owns                                                                   |
|----------------------|------------------------------------------------------------------------|
| `composeApp`         | `App()`, `VerborumNavHost`, `TopLevelDestination`, `di/AppModule`, platform entry points |
| `core:common`        | `BaseViewModel`, `Outcome`, `VerborumError`, `Envelope`, `Platform`     |
| `core:designsystem`  | `VerborumTheme`, `VerborumColors`, `Typography`, `Spacing`, shared composables, `VerborumIcons` |
| `core:network`       | Ktor client factory, `ApiConfig` per target, `apiCall`, error mapping   |
| `core:auth`          | `AuthSession`, `TokenStorage` (expect/actual), PKCE, SHA-256            |
| `core:database`      | `LocalCache` — real on iOS, no-op on web                                |
| `feature:auth`        | the login wall (Keycloak, Authorization Code + PKCE)                    |
| `feature:bibliotheca`| the library — slice per screen (`dictionarylist`, `dictionarydetails`)   |
| `feature:forum`      | marketplace listings                                                    |

### Non-negotiable rules

1. **Dependencies point one way only.** `composeApp → feature → core`. Never `core → feature`,
   never `feature → composeApp`.
2. **Features never depend on features.** Shared behaviour is promoted to a `core:*` module.
3. **A feature exposes exactly two things**: its nav-graph entry (`BibliothecaGraph` +
   `bibliothecaGraph()`) and its Koin module (`bibliothecaModule`). Screens, view models,
   repositories and route classes stay `internal`/`private` to the feature.
4. **The shell owns no feature logic.** If `App.kt` or `VerborumNavHost` starts knowing about a
   screen, a repository or a piece of state, the logic belongs in the feature.
5. **`core:network` is the only module that knows Ktor.** Features see `Outcome` and
   `VerborumError`, never an `HttpResponse` or a Ktor exception.

## Feature module anatomy

A feature is a set of **screen slices**. Each slice is one screen (plus the pieces only it needs) and
carries its own `data` / `domain` / `di` / `ui` folders; anything two slices share moves up into the
feature's `common/`. This mirrors the Android app package-for-package, so a screen ported from there
lands in the same place here.

```
feature/<name>/src/commonMain/kotlin/de/coldtea/verborum/feature/<name>/
├── common/                     shared *inside* this feature only
│   ├── data/                     DTO/API pieces more than one slice uses
│   ├── domain/                   SyncService, cross-slice use cases
│   └── ui/model/                 shared UI enums/models (SupportedLanguage)
├── <slice>/                    one screen, e.g. dictionarylist/
│   ├── data/                     DTOs, Api, Store, Repository — while only this screen reads them
│   ├── di/                       <Slice>Module.kt — the slice's Koin module
│   ├── domain/                   domain model, <Slice>Service, usecase/ — same condition
│   └── ui/                       <Screen>.kt, <Screen>ViewModel.kt
│       ├── composables/          pieces of that screen only
│       └── model/                State, UI model, sort/filter enums
├── di/<Name>Module.kt          the feature's single Koin module: `includes(<slice>Module)`
└── navigation/<Name>Navigation.kt   @Serializable routes + NavGraphBuilder extension
```

`feature/bibliotheca/dictionarylist` is the reference slice. Rules that follow from this shape:

- **A slice owns its whole vertical.** Its repository, use cases and view model are `internal` to the
  module and never referenced from another slice.
- **Sharing goes up, never sideways.** Slice A does not import from slice B; the shared piece moves
  to `common/`. Needed by a second *feature*, it moves to a `core:*` module instead. This is not
  theoretical: the dictionary and word data/domain layers live in `common/` precisely because the list
  and the details screen both read them, so `dictionarylist/` and `dictionarydetails/` are `di` + `ui`
  only. A slice keeps its own `data`/`domain` only until a second screen needs them.
- **One Koin module per slice**, aggregated by the feature's module with `includes(...)` — the shell
  still sees exactly one module per feature.
- **The feature's public surface is unchanged**: the nav graph entry plus the Koin module. Slices add
  no new public API.
- A single-screen feature (`feature/forum`, `feature/auth`) keeps the flat `data`/`di`/`ui` layout
  until a second screen arrives; the slice folders are what a second screen introduces.

Then: `include(":feature:<name>")` in `settings.gradle.kts`, a build file with
`id("verborum.kmp.feature")`, and add the module + its Koin module to `composeApp`
(`build.gradle.kts` dependency, `appModules` in `di/AppModule.kt`, `VerborumNavHost`, and
`TopLevelDestination` if it is a tab).

## MVVM / MVI contract

`BaseViewModel<State, Effect>` in `core:common` is the only base class:

- **State** — one immutable `data class` per screen, defaults for every field, exposed as
  `StateFlow`. Mutated only through `setState { copy(...) }`.
- **Effect** — a `sealed interface` of one-shot events (navigation, snackbars) on a hot
  `SharedFlow`. Never model navigation as state.
- **Intents** — plain public methods on the view model (`search(query)`, `retry()`). No
  `dispatch(Action)` switchboard.
- Work runs in `viewModelScope`; suspending calls return `Outcome`, so `try/catch` never appears
  in a view model.

### Screen split

Every screen is two composables:

```kotlin
@Composable
internal fun DictionaryListScreen(         // stateful: injects the VM, collects state
    onDictionaryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DictionaryListViewModel = koinViewModel(),
) { ... }

@Composable
internal fun DictionaryListContent(        // stateless: pure state -> UI, previewable, testable
    state: DictionaryListUiState,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) { ... }
```

Navigation is passed **in** as lambdas from the nav graph; a screen never holds a `NavController`.

## Data layer

- Repository interface + implementation live in the slice's `data/` package; the interface is what
  the domain layer depends on. The domain model lives in `domain/`, the row the screen renders in
  `ui/model/` — three shapes, mapped at each boundary, none of them shared.
- Every repository method returns `Outcome<T>` — never throws, never returns `null` to mean failure.
- Fakes are legitimate production stand-ins while an endpoint is pending; swap the Koin binding, not
  the call sites.
- DTOs (`@Serializable`) stay in the data layer and are mapped to domain models before crossing into
  `ui/`. Do not let a DTO reach a composable.
- **Not every endpoint uses `Envelope`.** The dictionary service answers with the payload directly,
  so those calls go through `plainApiCall` / `statusApiCall`; `apiCall` is for enveloped endpoints.
- There is **no local database yet**. A slice that needs an observable local copy holds an in-memory
  store (`DictionaryStore`) as its single source of truth, populated by `SyncService`. `LocalCache` is
  not a substitute: it is plaintext `NSUserDefaults` on iOS and a no-op on web, so putting user
  content in it is a data-at-rest decision, not a detail.

## Dependency injection boundaries

- `coreModule` (in `composeApp/di/AppModule.kt`) wires `core:*` only.
- Each feature owns one `Module`; `appModules` is the single list, `initKoin()` the single
  entry point, called exactly once per platform launcher before first composition.
- View models are registered with `viewModelOf(::Vm)` and resolved with `koinViewModel()`.
- Interfaces are bound explicitly: `single<WordRepository> { InMemoryWordRepository() }`.
- No `GlobalContext.get()`, no service locators inside classes — constructor injection only.

## expect/actual placement

Declare the `expect` next to the interface it serves in `commonMain`, and put actuals in the
narrowest source set that works: `webMain` when js and wasmJs share the implementation,
`jsMain`/`wasmJsMain` only for the low-level bridge that cannot be shared (see
`kmp-development` for the `js(...)` constraint), `iosMain` for Apple.

## Review checklist

- [ ] No new edge in the module graph pointing the wrong way
- [ ] New public API on a feature limited to its graph + Koin module
- [ ] State immutable, effects one-shot, no navigation in state
- [ ] Repository returns `Outcome`, no Ktor type escapes `core:network`
- [ ] Screen split into stateful + stateless halves
- [ ] Koin binding added and reachable from `appModules`
