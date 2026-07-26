package de.coldtea.verborum.core.common

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
