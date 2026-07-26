package de.coldtea.verborum.core.auth

internal actual fun currentEpochSeconds(): Long = (nowMillis() / 1000.0).toLong()

internal expect fun nowMillis(): Double
