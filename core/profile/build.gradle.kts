plugins {
    id("verborum.kmp.library")
    id("verborum.kmp.serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:common"))
            api(project(":core:network"))
            // The profile is keyed by the signed-in account: the subject, email and name all come
            // off the token, and `clear()` is registered as a post-logout hook.
            api(project(":core:auth"))

            // This module owns its own Koin module: the `ms_user` client, the APIs and the session
            // service. The feature convention plugin supplies Koin, a core one does not.
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinxJson)
        }
    }
}
