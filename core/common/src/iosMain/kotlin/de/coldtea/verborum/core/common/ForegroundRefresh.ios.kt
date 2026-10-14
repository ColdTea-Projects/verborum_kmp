package de.coldtea.verborum.core.common

/** iOS is nudged by a silent push (`P4N-12`), so it does not poll. */
actual val refreshesWhileVisible: Boolean = false
