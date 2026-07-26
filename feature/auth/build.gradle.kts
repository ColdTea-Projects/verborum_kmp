plugins {
    id("verborum.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // The login screen drives AuthService; the OAuth flow itself lives in core:auth.
            implementation(project(":core:auth"))
        }
        commonTest.dependencies {
            // Only to construct an AuthService; the login failure paths never reach the network.
            implementation(libs.ktor.client.mock)
        }
    }
}
