---
name: kmp-app-architecture
description: Reviews and implements against the module graph, layering rules and MVVM/MVI contracts of this Kotlin Multiplatform app — composeApp shell, feature slices, core plumbing, Koin wiring and navigation. Use when adding or moving a module, adding a feature, deciding where a class belongs, wiring Koin, changing the navigation graph, or reviewing whether a change respects layering; triggers on "new feature module", "where should this live", "core vs feature", "dependency direction", "ViewModel", "navigation graph", "Koin module".
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
| `core:database`      | `LocalCache` — real on iOS, no-op on web; `BibliothecaDatabase` — Room on iOS, absent on web |
| `feature:auth`       | the login wall (Keycloak, Authorization Code + PKCE)                    |
| `feature:bibliotheca`| the library — slice per screen (`dictionarylist`, `dictionarydetails`)   |
| `feature:forum`      | marketplace listings                                                    |
| `feature:onboarding` | the welcome tour; owns when it is shown per platform                     |
| `feature:options`    | the Options tab; owns sign-out                                          |

## Non-negotiable rules

1. **Dependencies point one way only.** `composeApp → feature → core`. Never `core → feature`,
   never `feature → composeApp`.
2. **Features never depend on features.** Shared behaviour is promoted to a `core:*` module. When
   one feature's screen must open another's, the graph builder takes a lambda and the **shell**
   supplies it (`optionsGraph(onHowToUseApp = …)` opens onboarding without Options knowing it
   exists).
3. **A feature exposes exactly two things**: its nav-graph entry (`BibliothecaGraph` +
   `bibliothecaGraph()`) and its Koin module (`bibliothecaModule`). Screens, view models,
   repositories and route classes stay `internal`/`private` to the feature.
4. **The shell owns no feature logic.** If `App.kt` or `VerborumNavHost` starts knowing about a
   screen, a repository or a piece of state, the logic belongs in the feature.
5. **`core:network` is the only module that knows Ktor.** Features see `Outcome` and
   `VerborumError`, never an `HttpResponse` or a Ktor exception.

## Quick start — where a new screen lands

```
feature/<name>/<slice>/ui/<Screen>Screen.kt       stateful half + expect <Screen>Content
feature/<name>/<slice>/ui/<Screen>Content.ios.kt  the Android design
feature/<name>/<slice>/ui/<Screen>Content.web.kt  the desktop design
feature/<name>/<slice>/di/<Slice>Module.kt        included by the feature's single Koin module
```

Full folder layout, the sharing rules between slices, the per-platform UI fork and the steps to
register a new feature: [references/feature_module_anatomy.md](references/feature_module_anatomy.md).

## The other two contracts

- View model, state/effect/intent shapes, the stateful-plus-stateless screen split, Koin binding
  rules and `expect`/`actual` placement: [references/mvvm_and_di.md](references/mvvm_and_di.md).
- Repository/domain/UI-model boundaries, the per-platform store, tombstone deletes, offline sync and
  what a failed write means: [references/data_layer.md](references/data_layer.md).

## Review checklist

- [ ] No new edge in the module graph pointing the wrong way
- [ ] New public API on a feature limited to its graph + Koin module
- [ ] State immutable, effects one-shot, no navigation in state
- [ ] Repository returns `Outcome`, no Ktor type escapes `core:network`
- [ ] Screen split into stateful + stateless halves
- [ ] Koin binding added and reachable from `appModules`
