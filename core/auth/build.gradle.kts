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
    }
}
