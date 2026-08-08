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
        }
        commonTest.dependencies {
            // Only to construct an AuthService; signing out never reaches the network here.
            implementation(libs.ktor.client.mock)
        }
    }
}
