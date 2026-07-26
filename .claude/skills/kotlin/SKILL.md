---
name: kotlin
description: Idiomatic Kotlin and clean-code conventions as practised in this repo — immutability, sealed hierarchies, error handling without exceptions, naming, visibility, comment style, and the anti-patterns to reject in review. Load before writing or reviewing any Kotlin file.
---

# Kotlin style & clean code

House style is `kotlin.code.style=official`: 4-space indent, 120-column soft limit, trailing commas
in multi-line argument lists, imports never wildcarded.

## Immutability first

- `val` everywhere; `var` needs a reason. State classes are `data class` with defaults, updated via
  `copy(...)`.
- Return read-only types (`List`, `Map`, `Set`) from public API; never leak a `MutableList`.
- Expose `StateFlow`/`SharedFlow`, keep the `Mutable*` backing field private:

```kotlin
private val _state = MutableStateFlow(initialState)
val state: StateFlow<State> = _state.asStateFlow()
```

## Model with the type system

- **`sealed interface`** for closed sets of alternatives (`Outcome`, `VerborumError`,
  `DictionaryEffect`) — prefer it to `sealed class` when no state is shared.
- `data object` for stateless cases (`Outcome.Loading`, `BibliothecaGraph`).
- `enum class` when each case carries the same shape (`TopLevelDestination`).
- `fun interface` for single-method seams (`BearerTokenProvider`, `TokenRefresher`) — it makes the
  lambda substitution in `AppModule` and in tests trivial.
- `value class` for identifiers you want the compiler to keep distinct, when the ObjC bridge is not
  in the way.
- `when` over a sealed type without an `else` — an added case then becomes a compile error, which is
  the whole point. Never add `else -> {}` to silence it.

## Errors are values, not exceptions

`Outcome<T>` + `VerborumError` is the app-wide contract:

- Repositories and use cases return `Outcome`, never throw and never signal failure with `null`.
- Exceptions are caught exactly once, at the boundary that owns the third-party library
  (`apiCall` in `core:network`), and mapped into `VerborumError`.
- Rethrow `CancellationException` before any broad `catch` — swallowing it breaks structured
  concurrency:

```kotlin
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    Outcome.Failure(throwable.toVerborumError())
}
```

- `runCatching { … }.getOrNull()` is fine for a genuinely optional local decode (see
  `TokenStorage`), not as a general error-handling strategy.
- **Never `!!`.** Use `?:`, `requireNotNull(value) { "why" }`, or restructure. `require`/`check`
  guard programmer errors only, never user or network input.

## Functions and classes

- A function does one thing and its name says which. If you need "and" to describe it, split it.
- Prefer expression bodies for one-liners: `fun retry() = search(currentState.query)`.
- Prefer extension functions to utility classes; keep them `private`/`internal` unless the whole app
  needs them (`Outcome.map`, `Throwable.toVerborumError`).
- Default arguments instead of overloads — but keep them off types crossing into Swift.
- Named arguments at any call site with more than two parameters, or any boolean parameter.
- Order composable parameters: required, then `modifier: Modifier = Modifier`, then other optionals,
  then trailing lambda.

## Visibility is a design decision

Default to the narrowest that works: `private` → `internal` → public. In a feature module, only the
nav graph entry and the Koin module are public. `internal` is the right level for something a
sibling file or a test in the same module needs (`DictionaryContent`, `isExpiring`).

## Naming

- Classes/objects `PascalCase`, functions/properties `camelCase`, constants `SCREAMING_SNAKE_CASE`
  in a `private const val`.
- `@Composable` functions are nouns in `PascalCase` (`WordRow`, `LoadingState`).
- Booleans read as assertions: `isLoading`, `isExpiring`, `enableLogging`.
- Say what a value *is*, with units when relevant: `expiresAtEpochSeconds`, `requestTimeoutMillis`.
- No Hungarian notation, no `Impl` suffix — name the implementation for what makes it different
  (`InMemoryWordRepository`, `LocalStorageTokenStorage`, `UserDefaultsTokenStorage`).

## Comments

Match the existing density: sparse, and always about **why**, never what.

```kotlin
/** Treated as expired a minute early so a token never dies mid-flight. */
// The web build is served next to the API, so requests stay same-origin and
// avoid a CORS preflight on every call.
```

KDoc on public API and on any non-obvious invariant. Delete commented-out code rather than shipping
it. No TODOs without a concrete follow-up sentence.

## Magic numbers and strings

Name them, close to use: `private const val VERIFIER_BYTES = 32`. Dimensions come from
`Spacing`, colours from `MaterialTheme.colorScheme` — never a literal `16.dp` or `Color(0xFF…)` in
a feature.

## Reject in review

- `!!`, `lateinit` in shared code, `Any` where a type is knowable
- `else` branch added to a sealed `when` purely to compile
- `GlobalScope`, `runBlocking`, a hardcoded `Dispatchers.*` in shared code
- Exceptions used for control flow across a module boundary
- A `data class` with `var` fields used as screen state
- Duplicated logic that a `core:*` module already provides (`Outcome.map`, `StateViews`, `Spacing`)
- Copy-pasted platform code in `jsMain` **and** `wasmJsMain` that belongs in `webMain`
