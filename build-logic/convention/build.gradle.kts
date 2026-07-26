plugins {
    `kotlin-dsl`
}

group = "de.coldtea.verborum.buildlogic"

dependencies {
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.composeCompiler.gradlePlugin)
    compileOnly(libs.kotlinSerialization.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "verborum.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "verborum.kmp.compose"
            implementationClass = "KmpComposeConventionPlugin"
        }
        register("kmpFeature") {
            id = "verborum.kmp.feature"
            implementationClass = "KmpFeatureConventionPlugin"
        }
        register("kmpSerialization") {
            id = "verborum.kmp.serialization"
            implementationClass = "KmpSerializationConventionPlugin"
        }
    }
}
