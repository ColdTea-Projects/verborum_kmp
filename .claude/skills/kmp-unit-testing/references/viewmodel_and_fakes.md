# Testing view models, writing fakes, and injecting the non-deterministic parts

Answers one decision: **how do I get this view model or this clock-dependent logic under test without
a mocking framework?**

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

## Injecting the non-deterministic parts

This is why `AuthSession` takes `nowEpochSeconds: () -> Long = ::currentEpochSeconds` — a test
passes a fixed clock and asserts the 60-second refresh leeway exactly:

```kotlin
val session = AuthSession(
    storage = InMemoryTokenStorage(tokens),
    refresher = TokenRefresher { Outcome.Success(refreshed) },
    nowEpochSeconds = { 1_000L },
)
```

Do the same for randomness and IDs: default to the platform source, override in tests. Keep the seam
`internal` so it does not widen the public API. Verified cryptographic primitives get **known answer
tests** against published vectors, as `PkceTest` does with RFC 7636 Appendix B.
