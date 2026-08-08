---
name: kmp-development
description: Guides day-to-day Kotlin Multiplatform implementation in this repo — source-set layout, expect/actual mechanics and their pitfalls, adding dependencies through the version catalog, coroutine constraints on JS/Wasm, and the compile/test commands that verify a change on iOS and web. Use when writing or editing any Kotlin in commonMain, iosMain or webMain, adding a dependency, or introducing a platform-specific implementation.
---

# KMP development workflow

## Source sets

`KmpLibraryConventionPlugin` declares `iosArm64`, `iosSimulatorArm64`, `js { browser(); nodejs() }`
and `wasmJs { browser(); nodejs() }`. The default hierarchy template provides these intermediate
source sets — prefer the widest one that compiles:

```
commonMain ─┬─ iosMain      (iosArm64 + iosSimulatorArm64)
            └─ webMain      (jsMain + wasmJsMain)
                 ├─ jsMain
                 └─ wasmJsMain
```

`commonTest`, `iosTest`, `webTest` mirror this. **Write in `commonMain` by default.** Dropping to
`webMain`/`iosMain` needs a reason a comment can state in one line, and `jsMain`/`wasmJsMain` is for
a minimal bridge only.

## Quick start — verify a change

Compile both platforms; a change that compiles for web can still fail for Kotlin/Native:

```bash
./gradlew compileKotlinJs compileKotlinWasmJs        # web
./gradlew compileKotlinIosSimulatorArm64             # iOS
./gradlew jsNodeTest wasmJsNodeTest                  # web tests
./gradlew iosSimulatorArm64Test                      # iOS tests (needs Xcode)
```

Scope to a module while iterating: `./gradlew :feature:bibliotheca:compileKotlinJs`.

## Platform-specific code

An `expect` in `commonMain` needs an `actual` in **every** leaf target. Prefer an expect factory
function returning a common interface over `expect class`:

```kotlin
// commonMain
expect fun createTokenStorage(): TokenStorage
```

The full patterns — the `js(...)`-not-allowed-in-`webMain` trap and its bridge shape, and iOS
`platform.*` interop with its opt-ins — are in
[references/expect_actual_patterns.md](references/expect_actual_patterns.md). Read that before
adding any `expect`/`actual`.

## Dependencies and concurrency

Version-catalog steps for a new library, the `api` vs `implementation` rule, the single-threaded
JS/Wasm constraints (no `Dispatchers.IO`, no `runBlocking`), and the end-to-end order for wiring a
new piece of work are in
[references/dependencies_and_coroutines.md](references/dependencies_and_coroutines.md).

## Checklist

- [ ] Code sits in the widest source set that compiles
- [ ] Every `expect` has an actual for iOS **and** both web targets
- [ ] No `js(...)` in `webMain`; bridges are minimal
- [ ] Dependency added via the version catalog, `api` only where justified
- [ ] No blocking calls, no `Dispatchers.IO`, `CancellationException` rethrown
- [ ] Compiles for `Js`, `WasmJs` and `IosSimulatorArm64`
