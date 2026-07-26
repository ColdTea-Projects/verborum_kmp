package de.coldtea.verborum.core.common

import kotlinx.coroutines.flow.Flow

/**
 * Emits whether the device currently has usable internet, starting with the state at
 * subscription time and then on every change. Feeds the app's offline banner.
 *
 * Errs towards `true`: if connectivity cannot be determined the app stays quiet, since wrongly
 * telling someone they are offline is worse than saying nothing.
 */
expect fun observeConnectivity(): Flow<Boolean>
