plugins {
    id("verborum.kmp.feature")
    id("verborum.kmp.serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:network"))
            // The sync needs to know who is signed in; the dictionary endpoint is scoped by user.
            implementation(project(":core:auth"))
            // The library's local copy, where the platform has one.
            implementation(project(":core:database"))
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinxJson)
        }
    }
}
