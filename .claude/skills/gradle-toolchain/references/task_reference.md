# Gradle task reference

Answers one decision: **which task name runs the thing I want, for which target?**

Always `./gradlew`, never a system `gradle`.

```bash
# Web — dev server
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
./gradlew :composeApp:jsBrowserDevelopmentRun

# Web — production bundle
./gradlew :composeApp:wasmJsBrowserDistribution
./gradlew :composeApp:jsBrowserDistribution

# Compile checks — the both-platforms gate before any commit
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

Useful flags: `--console=plain` for parseable output, `-q` to suppress noise, `--rerun-tasks` to
defeat up-to-date checks, `--no-configuration-cache` **only** while diagnosing.

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
