plugins {
    id("verborum.kmp.feature")
    id("verborum.kmp.serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Remembering that the tour has been seen is all the persistence this needs.
            implementation(project(":core:database"))
        }
    }
}
