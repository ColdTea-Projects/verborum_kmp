plugins {
    id("verborum.kmp.feature")
    id("verborum.kmp.serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Signing out ends the session this screen's only action owns.
            implementation(project(":core:auth"))
            // ...and empties the on-device library along with it.
            implementation(project(":core:database"))
            // The Profile screen edits the session profile: the display name and the Forum terms.
            implementation(project(":core:profile"))
        }
        commonTest.dependencies {
            // To construct an AuthService: signing out never reaches the network here, and the
            // delete-account tests answer the token exchange so the same-account check can run.
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinxJson)
        }
    }
}
