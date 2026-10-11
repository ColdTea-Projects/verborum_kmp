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
            // Membership decides whether downloads are shown at all, and the vault is what a
            // download is recorded in.
            implementation(project(":core:profile"))
            // The contracts the Forum binds against — ratings in, downloaded ids out.
            api(project(":core:marketplace"))
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinxJson)
        }
    }
}
