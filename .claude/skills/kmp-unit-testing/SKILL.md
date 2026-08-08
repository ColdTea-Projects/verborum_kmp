---
name: kmp-unit-testing
description: Writes unit tests for this KMP project — commonTest layout, kotlin-test assertions, coroutine and StateFlow testing with runTest, hand-written fakes instead of mocks, injected clocks and randomness, and the per-target test tasks. Use when writing or changing any test, adding testable logic, or judging whether a change is covered.
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

## Quick start

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

`kotlinx-coroutines-test` belongs in `libs.versions.toml` and the base convention plugin's
`commonTest`, not in a single module's build file. Then:

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

## View models, fakes and injected clocks

Collecting a hot `SharedFlow` of effects before acting, the shape of a hand-written fake, and the
injected-clock seam (`AuthSession`'s `nowEpochSeconds`) are in
[references/viewmodel_and_fakes.md](references/viewmodel_and_fakes.md).

## What to test

Worth testing: `Outcome`/`Envelope` mapping, error translation, view-model state transitions and
effects, token expiry and refresh logic, PKCE/SHA-256 vectors, repository query/filter behaviour,
anything with a branch on `VerborumError`.

Not worth testing: Compose layout details, generated serializers, data-class `copy`, one-line
delegations, the framework itself.

## Run

```bash
./gradlew jsNodeTest wasmJsNodeTest              # fast — run these while iterating
./gradlew iosSimulatorArm64Test                  # needs Xcode
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
