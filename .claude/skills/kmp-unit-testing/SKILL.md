---
name: kmp-unit-testing
description: Writing unit tests for this KMP project — commonTest layout, kotlin-test, coroutine and StateFlow testing, fakes instead of mocks, injecting clocks and randomness, and the per-target test tasks. Load before writing or changing any test, adding testable logic, or when asked whether a change is covered.
---

# KMP unit testing

`kotlin-test` is on every module's `commonTest` via `KmpLibraryConventionPlugin`. Existing examples:
`core/common/.../OutcomeTest.kt`, `core/auth/.../PkceTest.kt`.

## Where tests go

```
<module>/src/commonTest/kotlin/…   ← default; runs on iOS and both web targets
<module>/src/iosTest/kotlin/…      ← only for an iOS actual
<module>/src/webTest/kotlin/…      ← only for a web actual
```

Mirror the production package. **Write in `commonTest`** unless the thing under test is
platform-specific — a `commonTest` test is three tests for the price of one and catches
Native-vs-JS divergence.

## Conventions

Backticked descriptive names, one behaviour per test, arrange/act/assert with a blank line between:

```kotlin
@Test
fun `map leaves failures untouched`() {
    val failure = Outcome.Failure(VerborumError.Unauthorized)

    assertEquals(failure, failure.map { it })
    assertNull(failure.getOrNull())
}
```

Use `assertEquals`/`assertNull`/`assertTrue`/`assertFailsWith` from `kotlin.test`. Assert on values,
not on interactions — there is no mocking framework and that is a feature, not a gap.

## Coroutines

Add once to `libs.versions.toml` and to the base convention plugin's `commonTest`:

```toml
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }
```

Then:

```kotlin
@Test
fun `search publishes the repository result`() = runTest {
    val viewModel = DictionaryViewModel(FakeWordRepository(words))

    viewModel.search("verb")

    assertEquals(words, viewModel.state.value.words)
}
```

- `runTest` **must be the return value** of the test function (`= runTest { … }`) — on JS/Wasm it
  returns a promise, and a test that ignores it passes vacuously.
- Never `Thread.sleep`, never `runBlocking` — neither exists on web.
- Prefer `advanceUntilIdle()` / `runCurrent()` over real delays; use the injected
  `StandardTestDispatcher` rather than wall-clock time.

## Testing a `BaseViewModel`

- Construct the view model directly with fakes — no Koin in a unit test.
- Assert on `state.value` after the coroutine settles.
- For effects, collect before acting (`effects` is a hot `SharedFlow`, so a late collector misses
  emissions):

```kotlin
@Test
fun `clicking a word emits an open effect`() = runTest {
    val emitted = mutableListOf<DictionaryEffect>()
    val job = launch { viewModel.effects.toList(emitted) }

    viewModel.onWordClicked("liber")
    advanceUntilIdle()

    assertEquals(listOf(DictionaryEffect.OpenWord("liber")), emitted)
    job.cancel()
}
```

- `init { search("") }` in a view model runs at construction — account for the initial load in
  assertions.

## Fakes, not mocks

Hand-written fakes in `commonTest`, or the production stand-ins that already exist
(`InMemoryWordRepository`, `InMemoryTokenStorage`). Keep them small and deterministic:

```kotlin
private class FakeWordRepository(
    private val result: Outcome<List<Word>> = Outcome.Success(emptyList()),
) : WordRepository {
    var lastQuery: String? = null
        private set

    override suspend fun search(query: String): Outcome<List<Word>> =
        result.also { lastQuery = query }

    override suspend fun word(id: String): Outcome<Word> =
        Outcome.Failure(VerborumError.Http(404))
}
```

Cover the failure paths too: every `VerborumError` case a view model branches on deserves a test.

## Make code testable by injecting the non-deterministic parts

This is why `AuthSession` takes `nowEpochSeconds: () -> Long = ::currentEpochSeconds` — a test
passes a fixed clock and asserts the 60-second refresh leeway exactly:

```kotlin
val session = AuthSession(
    storage = InMemoryTokenStorage(tokens),
    refresher = TokenRefresher { Outcome.Success(refreshed) },
    nowEpochSeconds = { 1_000L },
)
```

Do the same for randomness and IDs: default to the platform source, override in tests. Keep the
seam `internal` so it does not widen the public API. Verified cryptographic primitives get **known
answer tests** against published vectors, as `PkceTest` does with RFC 7636 Appendix B.

## What to test

Worth testing: `Outcome`/`Envelope` mapping, error translation, view-model state transitions and
effects, token expiry and refresh logic, PKCE/SHA-256 vectors, repository query/filter behaviour,
anything with a branch on `VerborumError`.

Not worth testing: Compose layout details, generated serializers, data-class `copy`, one-line
delegations, the framework itself.

## Run

```bash
./gradlew jsNodeTest wasmJsNodeTest              # fast — run these while iterating
./gradlew iosSimulatorArm64Test                  # needs Xcode command line tools
./gradlew :core:auth:jsNodeTest                  # scope to a module
./gradlew allTests                               # everything
```

A change is not verified until it passes on **both** a web target and iOS.

## Checklist

- [ ] Test lives in `commonTest` unless platform-specific
- [ ] `= runTest { … }` used as the return value; no sleeps or `runBlocking`
- [ ] Fakes, not mocks; failure paths covered
- [ ] Effects collected before the action
- [ ] Clock/RNG injected rather than stubbed globally
- [ ] Passes on a web target and on iOS
