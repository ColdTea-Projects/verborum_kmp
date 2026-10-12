plugins {
    id("verborum.kmp.feature")
    id("verborum.kmp.serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:network"))
            // Membership is the gate the whole feature sits behind, and the vault is where a
            // download is recorded.
            implementation(project(":core:profile"))
            // The contracts the library binds against — ratings out, downloaded ids in.
            api(project(":core:marketplace"))
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinxJson)
        }
    }
}
