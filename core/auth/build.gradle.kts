plugins {
    id("verborum.kmp.library")
    id("verborum.kmp.serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:common"))
            api(project(":core:network"))
        }
        commonTest.dependencies {
            // The Keycloak endpoints are exercised through MockEngine rather than a fake client, so
            // the request shape and the response mapping are both covered.
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinxJson)
        }
    }
}
