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

// A Compose UI module links skiko, which Node cannot load ("both async and sync fetching of the
// wasm failed"), so this module's commonTest runs on js and iOS — the same code, one runtime fewer.
tasks.named("wasmJsNodeTest") { enabled = false }

compose.resources {
    // Consumed by feature modules, so the generated `Res` class must be public.
    publicResClass = true
    generateResClass = always
    packageOfResClass = "de.coldtea.verborum.core.designsystem.resources"
}
