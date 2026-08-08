# Feature module anatomy

Answers one decision: **which folder does this new class, screen or composable go in, and what does
adding a feature require?**

A feature is a set of **screen slices**. Each slice is one screen (plus the pieces only it needs)
and carries its own `data` / `domain` / `di` / `ui` folders; anything two slices share moves up into
the feature's `common/`. This mirrors the Android app package-for-package, so a screen ported from
there lands in the same place here.

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
  theoretical: the dictionary and word data/domain layers live in `common/` precisely because the
  list and the details screen both read them, so `dictionarylist/` and `dictionarydetails/` are
  `di` + `ui` only. A slice keeps its own `data`/`domain` only until a second screen needs them.
- **One Koin module per slice**, aggregated by the feature's module with `includes(...)` — the shell
  still sees exactly one module per feature.
- **The feature's public surface is unchanged**: the nav graph entry plus the Koin module. Slices
  add no new public API.
- A single-screen feature (`feature/forum`, `feature/auth`, `feature/options`) keeps the flat
  `data`/`di`/`ui` layout until a second screen arrives; the slice folders are what a second screen
  introduces.
- **One bottom-bar tab = one feature graph.** `TopLevelDestination` maps each tab onto a feature's
  `*Graph`, so a new tab is a new feature module rather than an entry in an existing one.

## The per-platform UI fork

**A slice forks its UI per platform** — `expect`/`actual` on the content composable, with
`iosMain`/`webMain` actuals. This is the norm in `feature:bibliotheca`, not the exception: the web
app is laid out as a desktop app and iOS keeps the Android design, so the two differ by intent. The
split is always the same:

```
ui/<Screen>Screen.kt          commonMain — the stateful half: view model, RegisterTopBar,
                                           then `expect fun <Screen>Content(…)`
ui/<Screen>Content.ios.kt     iosMain    — the Android design
ui/<Screen>Content.web.kt     webMain    — the desktop design
ui/composables/…              whichever source set actually draws them
```

The view model, the state and the callback signatures stay shared, so the fork is presentation only
and no behaviour can drift. A composable only one platform draws belongs in that platform's source
set — leaving it in `commonMain` is how the two designs start bleeding into each other.

Web pages take their furniture from `core:designsystem/webMain` (`WebPageTitle`, `WebChip`,
`WebPanel`, `WebSelect`, `WebTextField`, `WebPrimaryButton`) and their measure from `ContentPane` +
`ContentWidth.Web`; iOS screens keep `ContentColumn`.

**Every screen calls `RegisterTopBar`, on both platforms.** iOS renders it as the top bar; web
renders it as `WebTopBar`, the strip above the page holding the way back, and pages add a
`backLabel` naming where back leads. Register in the shared half where the title is the same on both
platforms, and in each actual where it is not.

**Web navigation follows the window.** Sidebar at ≥700dp, bottom bar below it; the shell decides
from the destination, and only the onboarding graph goes without. Do not gate navigation on what a
screen registered — a screen that forgets would silently lose it. `selfpractice` forked before the
web redesign and is unaffected by it.

## Registering a new feature

`include(":feature:<name>")` in `settings.gradle.kts`, a build file with
`id("verborum.kmp.feature")`, and add the module + its Koin module to `composeApp`
(`build.gradle.kts` dependency, `appModules` in `di/AppModule.kt`, `VerborumNavHost`, and
`TopLevelDestination` if it is a tab).
