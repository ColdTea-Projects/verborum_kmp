package de.coldtea.verborum.core.common

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue

/**
 * `NWPathMonitor` reports "satisfied" only for a path that can actually carry traffic, so a
 * captive-portal Wi-Fi reads as offline — which is what the network layer sees too.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun observeConnectivity(): Flow<Boolean> = callbackFlow {
    val monitor = nw_path_monitor_create()

    nw_path_monitor_set_update_handler(monitor) { path ->
        trySend(path != null && nw_path_get_status(path) == nw_path_status_satisfied)
    }
    // The handler runs on the main queue, which is where the UI collects this flow.
    nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
    nw_path_monitor_start(monitor)

    awaitClose { nw_path_monitor_cancel(monitor) }
}.distinctUntilChanged()
