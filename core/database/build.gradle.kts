plugins {
    id("verborum.kmp.library")
    alias(libs.plugins.ksp)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:common"))
        }
        // Room and the bundled SQLite driver have no js/wasm artifacts, so the whole Room layer is
        // confined here and `createBibliothecaDatabase()` answers null on web.
        iosMain.dependencies {
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
        }
    }
}

// Room's processor runs per Kotlin/Native target; there is no common KSP configuration that would
// cover both, and adding one for a web target would fail to resolve.
dependencies {
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}
