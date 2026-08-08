# Testing the Koin graph, Compose UI and platform actuals

Answers one decision: **how do I prove the wiring and the platform-specific halves actually work?**

## Verifying the Koin graph

A broken binding is a runtime crash on first composition, so it deserves a test. Without adding any
dependency, instantiate the modules and resolve every binding:

```kotlin
@Test
fun `every core binding resolves`() {
    val koin = koinApplication { modules(coreModule) }.koin

    assertNotNull(koin.get<ApiConfig>())
    assertNotNull(koin.get<AuthSession>())
    assertNotNull(koin.get<HttpClient>())

    koin.close()
}
```

This test belongs in `composeApp`'s `commonTest` (where `appModules` lives); it touches platform
actuals (`createTokenStorage`, `createLocalCache`, `defaultApiConfig`), which is exactly the point —
it proves the real graph builds on each target. Always `close()` so tests do not leak Koin state;
never call the app's `initKoin()` from a test, since `startKoin` is global. View models registered
with `viewModelOf` need a `ViewModelStoreOwner` to resolve, so assert on the repository and `core:*`
bindings and let UI tests cover view-model creation.

## Compose UI tests

```toml
compose-uiTest = { module = "org.jetbrains.compose.ui:ui-test", version.ref = "composeMultiplatform" }
```

```kotlin
@OptIn(ExperimentalTestApi::class)
@Test
fun `tapping a row reports the word id`() = runComposeUiTest {
    var clicked: String? = null

    setContent {
        VerborumTheme {
            DictionaryContent(
                state = DictionaryState(words = words, isLoading = false),
                onQueryChanged = {},
                onWordClicked = { clicked = it },
                onRetry = {},
            )
        }
    }

    onNodeWithText("liber").performClick()
    assertEquals("liber", clicked)
}
```

Test the **stateless** half (`DictionaryContent`) — this is why the screen split exists: no Koin, no
view model, no async. Target support for `runComposeUiTest` differs per platform; confirm it runs on
`iosSimulatorArm64` and the web targets before relying on it in CI, and keep the tests in
`commonTest` so a target can be added later. Query by text or `Modifier.testTag`, and always wrap in
`VerborumTheme` so nothing resolves against a default scheme.

## Platform actuals

`TokenStorage` and `LocalCache` actuals need target-specific tests, because their whole purpose is
platform behaviour. Put a shared contract test in the module's `commonTest` and run it against
`createTokenStorage()`:

- write → read returns the same tokens
- clear → read returns `null`
- a corrupt stored payload returns `null` rather than throwing (the `runCatching` path)

Each target then runs it against its real backing store (`localStorage` on web, the iOS store).
`core:database`'s web actual is a documented no-op, so assert against `localCacheIsPersistent`
instead of assuming persistence.
