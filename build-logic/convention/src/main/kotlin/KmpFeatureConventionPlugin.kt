import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A feature module: Compose UI plus the shared plumbing every feature needs —
 * the design system, the common layer and Koin/Navigation wiring.
 */
class KmpFeatureConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("verborum.kmp.compose")

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain").dependencies {
                api(project(":core:common"))
                api(project(":core:designsystem"))

                implementation(libs.library("navigation-compose"))
                implementation(libs.library("koin-core"))
                implementation(libs.library("koin-compose"))
                implementation(libs.library("koin-compose-viewmodel"))
            }
        }
    }
}
