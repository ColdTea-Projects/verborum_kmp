---
name: gradle-toolchain
description: Explains this repo's Gradle setup — the convention plugins in build-logic, the single version catalog, module registration, per-target task names, and how to diagnose build failures. Use when editing any *.gradle.kts, libs.versions.toml, settings.gradle.kts or convention plugin, when adding a module or dependency, or when a build or dependency-resolution failure needs diagnosing.
---

# Gradle toolchain

Gradle via the wrapper (always `./gradlew`, never a system `gradle`). Versions for Gradle, Kotlin
and Compose Multiplatform live in `gradle/libs.versions.toml`. Configuration cache and build cache
are **on** in `gradle.properties`.

## Quick start

The check that gates every change, on both targets:

```bash
./gradlew compileKotlinJs compileKotlinWasmJs compileKotlinIosSimulatorArm64
./gradlew jsNodeTest wasmJsNodeTest
```

Every other task name — dev server, distribution bundle, iOS framework, per-module build — is in
[references/task_reference.md](references/task_reference.md).

## The two sources of truth

**`gradle/libs.versions.toml`** — every version and coordinate in the build, including the Gradle
plugin artifacts that `build-logic` itself compiles against. `build-logic/settings.gradle.kts`
imports the same file, so there is exactly one catalog.

**`build-logic/convention/`** — the shared build logic. Module build files stay near-empty on
purpose; if the same block is about to be added to a second module, it belongs in a convention
plugin instead.

| Plugin id                    | Class                              | Adds                                                        |
|------------------------------|------------------------------------|-------------------------------------------------------------|
| `verborum.kmp.library`       | `KmpLibraryConventionPlugin`       | targets (iosArm64, iosSimulatorArm64, js, wasmJs), coroutines, `kotlin-test` |
| `verborum.kmp.compose`       | `KmpComposeConventionPlugin`       | the above + Compose runtime/foundation/M3/ui/resources/preview + lifecycle |
| `verborum.kmp.feature`       | `KmpFeatureConventionPlugin`       | the above + `api(core:common)`, `api(core:designsystem)`, Navigation, Koin |
| `verborum.kmp.serialization` | `KmpSerializationConventionPlugin` | kotlinx.serialization plugin + JSON runtime                 |

Pick the **most specific** plugin: a feature uses `verborum.kmp.feature` alone, never
`verborum.kmp.feature` plus a re-declaration of Compose dependencies.

## Adding a module

A module build file carries the plugin id and nothing else that a convention plugin already gives:

```kotlin
plugins {
    id("verborum.kmp.feature")
}
```

Then `include(":feature:<name>")` in the root `settings.gradle.kts`, add it to
`composeApp/build.gradle.kts`, and register its Koin module in `appModules` in `di/AppModule.kt`.
Full walkthrough, including when to use `api` vs `implementation`, in
[references/task_reference.md](references/task_reference.md).

## When a build fails

Symptom-to-cause table, the convention-plugin editing rules and the diagnostic sequence are in
[references/build_failures.md](references/build_failures.md). Start there rather than guessing at
flags — in particular, do not leave `--no-configuration-cache` in place to make a task work.

## Rules

- One version catalog. No version literals in build files, no `latest.release`.
- No repository declarations in module build files — `settings.gradle.kts` owns them, and the
  `google()` repo is deliberately content-filtered to androidx/com.android/com.google.
- Keep configuration cache compatible; do not disable it to make a task work.
- Do not add an Android target or a `kotlin("jvm")` module without an explicit decision — the whole
  toolchain assumes iOS + web.
- Commit `gradle/wrapper/gradle-wrapper.properties` changes (with the `distributionSha256Sum`) and
  `kotlin-js-store/` lockfiles.
