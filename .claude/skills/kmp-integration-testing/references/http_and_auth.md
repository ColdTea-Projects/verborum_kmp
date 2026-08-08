# Testing the HTTP stack, the auth session, and repository → view model

Answers one decision: **what must a test cover when a change crosses the network or the auth seam?**

## Ktor `MockEngine` coverage

`platformHttpClient` is `internal expect`, so build the client from `MockEngine` directly in the test
and mirror the plugin set under test. Catalog entry:

```toml
ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }
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
