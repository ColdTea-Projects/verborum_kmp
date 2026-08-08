---
name: kotlin
description: Writes and reviews idiomatic Kotlin to this repo's clean-code conventions — immutability, sealed hierarchies, errors as values instead of exceptions, naming, visibility, comment style, and the anti-patterns to reject. Use when writing or reviewing any Kotlin file in this project.
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

## Naming, visibility and comments

Function shape, the narrowest-visibility rule, naming conventions, the sparse why-only comment
style and the no-magic-numbers rule are in
[references/naming_and_style.md](references/naming_and_style.md). Read it before naming a new
public declaration or adding a comment.

## Reject in review

- `!!`, `lateinit` in shared code, `Any` where a type is knowable
- `else` branch added to a sealed `when` purely to compile
- `GlobalScope`, `runBlocking`, a hardcoded `Dispatchers.*` in shared code
- Exceptions used for control flow across a module boundary
- A `data class` with `var` fields used as screen state
- Duplicated logic that a `core:*` module already provides (`Outcome.map`, `StateViews`, `Spacing`)
- Copy-pasted platform code in `jsMain` **and** `wasmJsMain` that belongs in `webMain`
- A local function whose name an import could also supply. **An explicit import outranks a same-file
  top-level declaration**, so adding one silently rebinds every call in the file to the imported
  version — and if the two differ only in defaulted parameters, it compiles and misbehaves at
  runtime. Either name the local one distinctly, or delete it and use the library's.
