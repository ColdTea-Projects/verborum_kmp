This is a Kotlin Multiplatform project targeting iOS and Web.

## Module layout

```
verborum_kmp/
├── build-logic/                 # Gradle convention plugins (KMP / Compose / feature)
├── gradle/libs.versions.toml    # the single version catalog
│
├── composeApp/                  # the shell: theme, app chrome, nav graph, Koin bootstrap
│   ├── commonMain/              #   App(), NavigationCentral, VerborumNavHost, TopLevelDestination, di/AppModule
│   ├── iosMain/                 #   MainViewController()
│   └── webMain/                 #   main() + index.html (compiles for both js and wasmJs)
├── iosApp/                      # Xcode wrapper (Swift @main → composeApp)
│
├── core/
│   ├── designsystem/            #   VerborumTheme, M3 colours, shared composables, chrome, icons
│   ├── common/                  #   BaseViewModel, Outcome, VerborumError, Envelope DTOs, connectivity
│   ├── network/                 #   Ktor client, base URL per target, error mapping
│   ├── auth/                    #   token storage (expect/actual), PKCE, refresh
│   └── database/                #   optional local cache — real on iOS, no-op on web
│
└── feature/
    ├── auth/                    # the login wall (Keycloak, Authorization Code + PKCE)
    ├── bibliotheca/             # the library — one folder per screen slice
    │   ├── common/              #   shared inside the feature: SyncService, SupportedLanguage
    │   └── dictionarylist/      #   data / di / domain / ui for the dictionary list
    └── forum/                   # marketplace
```

Dependencies point one way only: `composeApp → feature/* → core/*`. The shell knows each
feature's nav graph entry point and nothing else; features never depend on each other.

### The navigation centre

`NavigationCentral` (in `composeApp/navigation/`) is the single place that knows the app's shape —
modelled on `NavigationCentral` in the Android app, with type-safe `@Serializable` routes instead of
string routes:

- **One nav host.** Each feature contributes a nested graph (`bibliothecaGraph()`); the shell never
  names a screen.
- **One header.** A screen declares its own with `RegisterTopBar(title, subtitle, showBackButton,
  action)` from `core:designsystem`; the shell renders it and owns the back button. Registering
  nothing means no chrome at all, which is what a full-screen destination such as onboarding wants.
- **One snackbar.** `LocalSnackbarHostState` is provided here; `ShowSnackbarMessages(flow)` pipes a
  view model's messages onto it.
- **One offline banner**, pinned under the header, driven by `observeConnectivity()` in `core:common`
  (`NWPathMonitor` on iOS, `online`/`offline` window events on web).
- **Tabs from `TopLevelDestination`**: a `NavigationBar` on phone widths, a `NavigationRail` at
  ≥ 840dp (desktop browser, iPad), and only on a tab root — deeper screens are left via the header's
  back button, so the tabs cannot swallow the back stack.

### Login

Authorization Code + PKCE against the Keycloak `verborum` realm, in `core:auth`, with the screen in
`feature:auth`. Signing in and creating an account are the same flow pointed at different endpoints —
there is no password field in the app.

| | iOS | Web |
|---|---|---|
| Browser leg | `ASWebAuthenticationSession` | top-level redirect; code stripped from the URL |
| Redirect | `de.coldtea.verborum://oauth2redirect/cb` (`Info.plist`) | the app's own origin |
| PKCE verifier | in memory | `sessionStorage` (must survive the reload) |
| Tokens | Keychain, `…AfterFirstUnlockThisDeviceOnly` | `localStorage` — the session lasts until sign-out |
| Endpoints | `https://auth.verborum.coldtea.de/realms/verborum` | same-origin `/auth/realms/verborum` |

**Web is same-origin everywhere.** Deployed, a reverse proxy serves `/auth` and `/api` next to the
app; in development the dev server does the same (`composeApp/webpack.config.d/devServerProxy.js`
→ Keycloak on `:8180`, `ms_dictionary` on `:8085`). So the token exchange and every API call are
same-origin: no CORS at any point, no **Web origins** entry to keep in step with the dev port, and
`connect-src 'self'` stays valid.

Local Keycloak (realm `verborum`, client `verborum-app`) still needs its **Valid redirect URIs** to
include `http://localhost:8280/*` — that is a browser navigation, not an XHR, so no proxy can cover
it. The dev port (**8280**) is set once in `composeApp/build.gradle.kts`.

iOS talks to the services directly and keeps its `https` issuer: pointing it at a local Keycloak over
plain http would need an ATS exception, which this repo does not ship.

`AuthService` is the only entry point the UI touches, `AuthSession.sessionState` is the gate the shell
watches (`Unknown` → neither wall nor app, so no login flash for a signed-in user), and a failed
refresh clears the session — so a signed-in user stays signed in across restarts for as long as the
`offline_access` refresh token lives. Both `*-security` skills describe the storage trade-offs: on web
the tokens are readable by any script on the origin, which is the price of not logging out when the tab
closes. The end state is a `HttpOnly` refresh-token cookie, and it needs backend work.

### The dictionary list

The bibliotheca tab's root screen, and where a user lands after signing in. Ported from the Android
app's `bibliotheca/dictionary` package, keeping its structure: search, From/To language filters, sort
order, pull-to-refresh, an overflow per row with edit/delete, and skeleton rows on first load.

`SyncService` (`bibliotheca/common/domain`) is the download half of the sync: it reads the signed-in
user from the auth session and pulls `GET dictionaries/{userId}` into `DictionaryStore`, the in-memory
single source of truth the screen observes. A delete tombstones its row immediately, then asks the
server, and puts the row back if the server refuses.

Two deliberate gaps, both visible in the UI: there is **no local database**, so the list is empty
again after a restart until the first sync completes, and the **word count is omitted** from each card
because the word endpoint is not wired up yet. Dictionary details and create/edit are the next
screens; until they exist those taps report themselves on the snackbar.

Local dev needs `ms_dictionary` running on `http://localhost:8085`; the dev server proxies `/api` to
it, so no CORS configuration is required on the service.

### Convention plugins

Module build files stay near-empty because `build-logic/convention` owns the shared setup:

| Plugin id                  | What it does                                                     |
|----------------------------|------------------------------------------------------------------|
| `verborum.kmp.library`     | targets (iOS arm64/sim, js, wasmJs), coroutines, `kotlin-test`     |
| `verborum.kmp.compose`     | the above + Compose Multiplatform, Material 3, lifecycle           |
| `verborum.kmp.feature`     | the above + `core:common`, `core:designsystem`, Koin, Navigation   |
| `verborum.kmp.serialization` | kotlinx.serialization plugin + JSON runtime                      |

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and
options:

- Web app:
    - Wasm target (faster, modern browsers): `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`
    - JS target (slower, supports older browsers): `./gradlew :composeApp:jsBrowserDevelopmentRun`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Web tests (Node): `./gradlew jsNodeTest wasmJsNodeTest`
- Web tests (browser): `./gradlew jsBrowserTest wasmJsBrowserTest`
- iOS tests: `./gradlew iosSimulatorArm64Test` (requires Xcode command line tools)

### License

MIT — see [LICENSE](./LICENSE). Copyright (c) 2026 Yasar Naci Gündüz and Seymur Mammadrza.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack
channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).
