plugins {
    id("verborum.kmp.compose")
}

compose.resources {
    // Consumed by feature modules, so the generated `Res` class must be public.
    publicResClass = true
    generateResClass = always
    packageOfResClass = "de.coldtea.verborum.core.designsystem.resources"
}
