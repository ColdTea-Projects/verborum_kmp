import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    id("verborum.kmp.compose")
    // The shell declares no routes of its own, but its navigation tests do: a route is a
    // `@Serializable` type, so the plugin that generates their serializers is needed here too.
    id("verborum.kmp.serialization")
}

// The dev server port below (8280) is part of the OAuth setup, not just a preference: this origin
// has to appear in the Keycloak client's valid redirect URIs and web origins. It is written as a
// literal in each block on purpose — a script-level `val` referenced inside commonWebpackConfig is
// captured as a Gradle script object reference, which the configuration cache cannot serialize.
kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    js {
        binaries.executable()
        browser {
            commonWebpackConfig {
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).copy(port = 8280)
            }
        }
    }

    wasmJs {
        binaries.executable()
        browser {
            commonWebpackConfig {
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).copy(port = 8280)
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:designsystem"))
            implementation(project(":core:localization"))
            implementation(project(":core:network"))
            implementation(project(":core:auth"))
            implementation(project(":core:database"))
            implementation(project(":core:profile"))

            implementation(project(":feature:auth"))
            implementation(project(":feature:bibliotheca"))
            implementation(project(":feature:forum"))
            implementation(project(":feature:onboarding"))
            implementation(project(":feature:options"))

            implementation(libs.navigation.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }
        commonTest.dependencies {
            // The Koin graph test builds the real wiring, so it needs an engine for the clients the
            // shell declares — the requests are never made, only the clients constructed.
            implementation(libs.ktor.client.mock)
        }
    }
}
