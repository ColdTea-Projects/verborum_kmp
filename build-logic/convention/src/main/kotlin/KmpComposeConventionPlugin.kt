import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A Compose Multiplatform UI module: the base KMP target set plus the Compose
 * runtime, Material 3, resources and lifecycle dependencies.
 */
class KmpComposeConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("verborum.kmp.library")
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        // A Compose UI module links skiko, which Node cannot load ("both async and sync fetching of
        // the wasm failed"), so a commonTest here runs on js and iOS — the same code, one runtime
        // fewer. Compose tests that must run on wasm need a browser test runner instead.
        tasks.matching { it.name == "wasmJsNodeTest" }.configureEach { enabled = false }

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain").dependencies {
                implementation(libs.library("compose-runtime"))
                implementation(libs.library("compose-foundation"))
                implementation(libs.library("compose-material3"))
                implementation(libs.library("compose-ui"))
                implementation(libs.library("compose-components-resources"))
                implementation(libs.library("compose-uiToolingPreview"))
                implementation(libs.library("androidx-lifecycle-viewmodel"))
                implementation(libs.library("androidx-lifecycle-viewmodelCompose"))
                implementation(libs.library("androidx-lifecycle-runtimeCompose"))
            }
        }
    }
}
