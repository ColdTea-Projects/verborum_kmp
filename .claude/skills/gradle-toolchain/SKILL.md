---
name: gradle-toolchain
description: This repo's Gradle setup — convention plugins in build-logic, the single version catalog, module registration, the task names for each target, and how to diagnose build failures (config cache, yarn.lock, Xcode framework tasks). Load before touching any *.gradle.kts, libs.versions.toml, settings.gradle.kts, or when a build/dependency-resolution failure needs diagnosing.
---

# Gradle toolchain

Gradle **9.1.0** via the wrapper (always `./gradlew`, never a system `gradle`). Kotlin **2.4.10**,
Compose Multiplatform **1.11.1**. Configuration cache and build cache are **on** in
`gradle.properties`.

## The two sources of truth

**`gradle/libs.versions.toml`** — every version and coordinate in the build, including the Gradle
plugin artifacts that `build-logic` itself compiles against. `build-logic/settings.gradle.kts`
imports the same file, so there is exactly one catalog.

**`build-logic/convention/`** — the shared build logic. Module build files stay near-empty on
purpose; if you are about to add the same block to a second module, it belongs in a convention
plugin instead.

| Plugin id                    | Class                              | Adds                                                        |
|------------------------------|------------------------------------|-------------------------------------------------------------|
| `verborum.kmp.library`       | `KmpLibraryConventionPlugin`       | targets (iosArm64, iosSimulatorArm64, js, wasmJs), coroutines, `kotlin-test` |
| `verborum.kmp.compose`       | `KmpComposeConventionPlugin`       | the above + Compose runtime/foundation/M3/ui/resources/preview + lifecycle |
| `verborum.kmp.feature`       | `KmpFeatureConventionPlugin`       | the above + `api(core:common)`, `api(core:designsystem)`, Navigation, Koin |
| `verborum.kmp.serialization` | `KmpSerializationConventionPlugin` | kotlinx.serialization plugin + JSON runtime                 |

Pick the **most specific** plugin: a feature uses `verborum.kmp.feature` alone, never
`verborum.kmp.feature` plus a re-declaration of Compose dependencies.

### Editing a convention plugin

Convention-plugin code resolves the catalog through the helpers in `Catalogs.kt`:

```kotlin
sourceSets.getByName("commonMain").dependencies {
    implementation(libs.library("compose-material3"))   // dash-separated alias
}
```

`library(alias)` throws with a clear message when the alias is missing — keep that behaviour rather
than falling back silently. After editing, register any new plugin id in
`build-logic/convention/build.gradle.kts` under `gradlePlugin { plugins { … } }`; the id and the
`implementationClass` must both be present or the id silently will not resolve.

## Adding a module

1. `include(":feature:<name>")` (or `":core:<name>"`) in the root `settings.gradle.kts`.
2. `feature/<name>/build.gradle.kts` with just the plugin id(s) and any module-specific deps:

```kotlin
plugins {
    id("verborum.kmp.feature")
    id("verborum.kmp.serialization")   // only if it defines @Serializable types
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:network"))
        }
    }
}
```

3. Add it to `composeApp/build.gradle.kts` and to `appModules` in `di/AppModule.kt`.

`project(":…")` for internal deps, `api` only when the dependency's types appear in this module's
public signatures (`core:common` exposes `lifecycle-viewmodel`; `core:network` exposes
`ktor-client-core` and `core:common`).

## Task reference

```bash
# Web — dev server
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
./gradlew :composeApp:jsBrowserDevelopmentRun

# Web — production bundle
./gradlew :composeApp:wasmJsBrowserDistribution
./gradlew :composeApp:jsBrowserDistribution

# Compile checks
./gradlew compileKotlinJs compileKotlinWasmJs compileKotlinIosSimulatorArm64

# Tests
./gradlew jsNodeTest wasmJsNodeTest          # fast, headless
./gradlew jsBrowserTest wasmJsBrowserTest    # needs a browser
./gradlew iosSimulatorArm64Test              # needs Xcode command line tools

# iOS framework (Xcode calls this itself via the build phase)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode

# Whole build
./gradlew build
./gradlew :feature:bibliotheca:build         # scope while iterating
```

Useful flags: `--console=plain` for parseable output, `-q` to suppress noise,
`--rerun-tasks` to defeat up-to-date checks, `--no-configuration-cache` **only** while diagnosing.

## Diagnosing failures

| Symptom | Cause / fix |
|---|---|
| `No library 'x' in the version catalog` | alias missing or mis-spelled in `libs.versions.toml`; convention plugins use the dash form |
| `Plugin [id: 'verborum.…'] was not found` | not registered in `build-logic/convention/build.gradle.kts`, or `includeBuild("build-logic")` disturbed |
| Config-cache error mentioning `Project` at execution time | a task captured `project`; capture the value at configuration time into a `Provider`/`val` |
| `yarn.lock` mismatch / `kotlinNpmInstall` failure | commit the regenerated `kotlin-js-store/yarn.lock`; do not delete it. Regenerate with `./gradlew kotlinUpgradeYarnLock` |
| iOS link fails, web compiles | a `commonMain` dependency has no Kotlin/Native artifact, or JVM-only API leaked into shared code |
| Compose compiler / Kotlin version skew | `composeCompiler` is pinned to `version.ref = "kotlin"` — bump both together |
| Xcode build cannot find `ComposeApp` | run the framework task above; the framework is `isStatic = true`, `baseName = "ComposeApp"` |

## Rules

- One version catalog. No version literals in build files, no `latest.release`.
- No repository declarations in module build files — `settings.gradle.kts` owns them, and the
  `google()` repo is deliberately content-filtered to androidx/com.android/com.google.
- Keep configuration cache compatible; do not disable it to make a task work.
- Do not add an Android target or `kotlin("jvm")` module without an explicit decision — the whole
  toolchain assumes iOS + web.
- Commit `gradle/wrapper/gradle-wrapper.properties` changes (with the `distributionSha256Sum`) and
  `kotlin-js-store/` lockfiles.
