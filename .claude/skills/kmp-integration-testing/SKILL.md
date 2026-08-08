---
name: kmp-integration-testing
description: Tests across layers in this KMP app — Ktor MockEngine for the HTTP stack, auth-session refresh behaviour, repository-to-ViewModel flows, resolving the Koin graph, Compose UI tests and platform actual (TokenStorage, LocalCache) verification. Use when testing more than one layer together, when touching core:network or core:auth wiring, when verifying DI, or when a UI flow needs an automated test.
---

# KMP integration testing

Unit-test conventions (source-set layout, `runTest`, fakes, naming) come from the
**`kmp-unit-testing`** skill and apply here too. This skill covers the seams *between* layers.

Nothing beyond `kotlin-test` is wired up yet, so each reference states the catalog entry to add. Add
the artifact to `[libraries]` in `gradle/libs.versions.toml`, then to the module's `commonTest` (or
to `KmpLibraryConventionPlugin`'s `commonTest` if every module needs it) — never a hardcoded
coordinate.

## Quick start — the highest-value test in the project

`MockEngine` exercises `createHttpClient`, `ContentNegotiation` with `VerborumJson`, the bearer-auth
plugin, `apiCall`, `Envelope` unwrapping and `VerborumError` mapping in one pass:

```kotlin
@Test
fun `an error envelope becomes a http failure`() = runTest {
    val engine = MockEngine {
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

## What each seam owes

- The full `MockEngine` case list (401, malformed JSON, transport failure, bearer header present and
  absent), the single-flight refresh contract on `AuthSession`, and driving a real repository into a
  real view model: [references/http_and_auth.md](references/http_and_auth.md).
- Resolving `coreModule` without `initKoin()`, `runComposeUiTest` against the stateless composable,
  and the shared contract test for `TokenStorage`/`LocalCache` actuals:
  [references/koin_ui_and_actuals.md](references/koin_ui_and_actuals.md).

## Run

```bash
./gradlew jsNodeTest wasmJsNodeTest          # headless, fast
./gradlew jsBrowserTest wasmJsBrowserTest    # required for real localStorage / DOM behaviour
./gradlew iosSimulatorArm64Test              # needs Xcode
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
