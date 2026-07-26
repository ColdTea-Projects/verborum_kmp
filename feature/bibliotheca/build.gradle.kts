plugins {
    id("verborum.kmp.feature")
    id("verborum.kmp.serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:network"))
        }
    }
}
