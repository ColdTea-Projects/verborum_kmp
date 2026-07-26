This is a Kotlin Multiplatform project targeting iOS and Web.

## Module layout

```
verborum_kmp/
├── build-logic/                 # Gradle convention plugins (KMP / Compose / feature)
├── gradle/libs.versions.toml    # the single version catalog
│
├── composeApp/                  # the shell: theme, bottom bar, nav graph, Koin bootstrap
│   ├── commonMain/              #   App(), VerborumNavHost, TopLevelDestination, di/AppModule
│   ├── iosMain/                 #   MainViewController()
│   └── webMain/                 #   main() + index.html (compiles for both js and wasmJs)
├── iosApp/                      # Xcode wrapper (Swift @main → composeApp)
│
├── core/
│   ├── designsystem/            #   VerborumTheme, M3 colours, shared composables, icons
│   ├── common/                  #   BaseViewModel, Outcome, VerborumError, Envelope DTOs
│   ├── network/                 #   Ktor client, base URL per target, error mapping
│   ├── auth/                    #   token storage (expect/actual), PKCE, refresh
│   └── database/                #   optional local cache — real on iOS, no-op on web
│
└── feature/
    ├── bibliotheca/             # dictionary + word slices
    └── forum/                   # marketplace
```

Dependencies point one way only: `composeApp → feature/* → core/*`. The shell knows each
feature's nav graph entry point and nothing else; features never depend on each other.

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
