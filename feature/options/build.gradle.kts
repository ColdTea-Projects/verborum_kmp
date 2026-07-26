plugins {
    id("verborum.kmp.feature")
    id("verborum.kmp.serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Signing out ends the session this screen's only action owns.
            implementation(project(":core:auth"))
        }
        commonTest.dependencies {
            // Only to construct an AuthService; signing out never reaches the network here.
            implementation(libs.ktor.client.mock)
        }
    }
}
