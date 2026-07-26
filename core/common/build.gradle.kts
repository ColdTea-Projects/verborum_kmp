plugins {
    id("verborum.kmp.library")
    id("verborum.kmp.serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // BaseViewModel is part of this module's public surface.
            api(libs.androidx.lifecycle.viewmodel)
            api(libs.kotlinx.serialization.json)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
    }
}
