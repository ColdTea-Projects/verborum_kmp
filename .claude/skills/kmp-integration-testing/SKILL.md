---
name: kmp-integration-testing
description: Integration testing across layers in this KMP app — Ktor MockEngine for the HTTP stack, repository-to-ViewModel flows, verifying the Koin graph, Compose UI tests, and platform actual (TokenStorage, LocalCache) verification. Load when testing more than one layer together, when touching core:network or core:auth wiring, when verifying DI, or when a UI flow needs an automated test.
---

# KMP integration testing

Unit-test conventions (source-set layout, `runTest`, fakes, naming) come from **`kmp-unit-testing`**
and apply here too. This skill covers the seams *between* layers.

Nothing beyond `kotlin-test` is wired up yet, so each section below states the catalog entry to add.
Add the artifact to `[libraries]` in `gradle/libs.versions.toml`, then to the module's `commonTest`
(or to `KmpLibraryConventionPlugin`'s `commonTest` if every module needs it) — never a hardcoded
coordinate.

## The HTTP stack — Ktor `MockEngine`

This is the highest-value integration test in the project: it exercises `createHttpClient`,
`ContentNegotiation` with `VerborumJson`, the bearer-auth plugin, `apiCall`, `Envelope` unwrapping
and `VerborumError` mapping in one pass.

```toml
ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }
```

`platformHttpClient` is `internal expect`, so build the client from `MockEngine` directly in the test
and mirror the plugin set under test:

```kotlin
@Test
fun `an error envelope becomes a http failure`() = runTest {
    val engine = MockEngine { request ->
        respond(
            content = """{"error":{"code":"word_not_found"}}""",
            status = HttpStatusCode.NotFound,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }
    val client = HttpClient(engine) {
        install(ContentNegotiation) { json(VerborumJson) }
        expectSuccess = false
    }

    val outcome: Outcome<Word> = apiCall { client.get("words/liber") }

    assertEquals(Outcome.Failure(VerborumError.Http(404, "word_not_found")), outcome)
}
```

Cover, at minimum:

- 2xx with a populated `data` → `Outcome.Success`
- 4xx with an `error` envelope → `VerborumError.Http` carrying `code`/`message`
- **401 → `VerborumError.Unauthorized`** (short-circuited in `apiCall` before body parsing)
- malformed/unexpected JSON → `VerborumError.Serialization`
- transport throw from the engine → `VerborumError.Network`
- an envelope with neither `data` nor `error` → `Serialization`
- `ignoreUnknownKeys` — an unknown backend field must not fail the decode
- the bearer plugin: a `BearerTokenProvider` returning a token stamps `Authorization: Bearer …`, and
  returning `null` stamps no header at all (assert on `request.headers` inside the `MockEngine`
  lambda)

## Auth session end to end

`AuthSession` + `InMemoryTokenStorage` + a `TokenRefresher` lambda + an injected clock is a full
integration test with no platform dependency:

- an unexpired token is returned without calling the refresher
- a token inside the 60s leeway triggers exactly **one** refresh and the new pair is persisted
- a failed refresh clears storage and returns `null`
- **concurrent `accessToken()` calls trigger a single refresh** — launch several coroutines in
  `runTest`, count refresher invocations, assert 1. This is the `Mutex` contract and the reason it
  exists.

## Repository → ViewModel

Drive a real view model through a real repository against `MockEngine` (or the in-memory
repository), and assert the observable state sequence rather than internals:

```kotlin
val viewModel = DictionaryViewModel(HttpWordRepository(client))
viewModel.search("verb")
advanceUntilIdle()

assertFalse(viewModel.state.value.isLoading)
assertEquals(listOf("verbum"), viewModel.state.value.words.map(Word::lemma))
```

Assert that a failure lands as `state.error` and that `retry()` re-issues the request.

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

Notes: this test belongs in `composeApp`'s `commonTest` (where `appModules` lives); it touches
platform actuals (`createTokenStorage`, `createLocalCache`, `defaultApiConfig`), which is exactly the
point — it proves the real graph builds on each target. Always `close()` so tests do not leak Koin
state; never call the app's `initKoin()` from a test, since `startKoin` is global. View models
registered with `viewModelOf` need a `ViewModelStoreOwner` to resolve, so assert on the repository
and `core:*` bindings and let UI tests cover view-model creation.

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
view model, no async. Target-support for `runComposeUiTest` differs per platform; confirm it runs on
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

## Run

```bash
./gradlew jsNodeTest wasmJsNodeTest          # headless, fast
./gradlew jsBrowserTest wasmJsBrowserTest    # required for real localStorage / DOM behaviour
./gradlew iosSimulatorArm64Test              # needs Xcode command line tools
./gradlew allTests
```

## Checklist

- [ ] New dependency added via the version catalog, `commonTest` scope
- [ ] HTTP behaviour tested through `MockEngine`, covering success, 401, HTTP error, malformed JSON, transport failure
- [ ] Bearer header asserted for both token-present and token-absent
- [ ] Single-flight refresh proven with concurrent callers
- [ ] Koin graph resolves and is `close()`d; `initKoin()` not called from tests
- [ ] UI tests target the stateless composable, wrapped in `VerborumTheme`
- [ ] Platform actuals covered by a shared contract test run on each target
- [ ] Green on a web target **and** iOS
