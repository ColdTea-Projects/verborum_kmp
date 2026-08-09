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
            // `logger(...)` hands back a Kermit Logger, so every module that logs sees the type.
            api(libs.kermit)
        }
        commonTest.dependencies {
            implementation(libs.kermit.test)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
    }
}
