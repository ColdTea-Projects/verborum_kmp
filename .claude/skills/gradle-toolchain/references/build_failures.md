# Diagnosing build failures

Answers one decision: **this build error just appeared — what is actually wrong?**

| Symptom | Cause / fix |
|---|---|
| `No library 'x' in the version catalog` | alias missing or mis-spelled in `libs.versions.toml`; convention plugins use the dash form |
| `Plugin [id: 'verborum.…'] was not found` | not registered in `build-logic/convention/build.gradle.kts`, or `includeBuild("build-logic")` disturbed |
| Config-cache error mentioning `Project` at execution time | a task captured `project`; capture the value at configuration time into a `Provider`/`val` |
| `yarn.lock` mismatch / `kotlinNpmInstall` failure | commit the regenerated `kotlin-js-store/yarn.lock`; do not delete it. Regenerate with `./gradlew kotlinUpgradeYarnLock` |
| iOS link fails, web compiles | a `commonMain` dependency has no Kotlin/Native artifact, or a JVM-only API leaked into shared code |
| Compose compiler / Kotlin version skew | `composeCompiler` is pinned to `version.ref = "kotlin"` — bump both together |
| Xcode build cannot find `ComposeApp` | run the framework task; the framework is `isStatic = true`, `baseName = "ComposeApp"` |

## Editing a convention plugin

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

## Diagnostic sequence

1. Re-run the failing task alone with `--console=plain` to get the real message.
2. If the error mentions the configuration cache, re-run once with `--no-configuration-cache` to
   confirm the cache is the cause — then fix the task, do not leave the flag in place.
3. If only one target fails, compare against the other: `compileKotlinJs` passing while
   `compileKotlinIosSimulatorArm64` fails almost always means a non-Native dependency.
