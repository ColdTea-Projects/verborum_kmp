plugins {
    id("verborum.kmp.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // The offline banner reads connectivity from core:common. No core:common type appears
            // in this module's own API, so it stays an implementation dependency.
            implementation(project(":core:common"))
        }
    }
}

compose.resources {
    // Consumed by feature modules, so the generated `Res` class must be public.
    publicResClass = true
    generateResClass = always
    packageOfResClass = "de.coldtea.verborum.core.designsystem.resources"
}
