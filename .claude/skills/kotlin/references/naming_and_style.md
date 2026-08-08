# Functions, visibility, naming and comments

Answers one decision: **what should this declaration be called, how visible should it be, and does
it deserve a comment?**

## Functions and classes

- A function does one thing and its name says which. If describing it needs "and", split it.
- Prefer expression bodies for one-liners: `fun retry() = search(currentState.query)`.
- Prefer extension functions to utility classes; keep them `private`/`internal` unless the whole app
  needs them (`Outcome.map`, `Throwable.toVerborumError`).
- Default arguments instead of overloads — but keep them off types crossing into Swift.
- Named arguments at any call site with more than two parameters, or any boolean parameter.
- Order composable parameters: required, then `modifier: Modifier = Modifier`, then other optionals,
  then the trailing lambda.

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

Name them, close to use: `private const val VERIFIER_BYTES = 32`. Dimensions come from `Spacing`,
colours from `MaterialTheme.colorScheme` — never a literal `16.dp` or `Color(0xFF…)` in a feature.
