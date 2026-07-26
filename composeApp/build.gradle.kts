plugins {
    id("verborum.kmp.compose")
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    js {
        binaries.executable()
    }

    wasmJs {
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:designsystem"))
            implementation(project(":core:network"))
            implementation(project(":core:auth"))
            implementation(project(":core:database"))

            implementation(project(":feature:bibliotheca"))
            implementation(project(":feature:forum"))

            implementation(libs.navigation.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }
    }
}
