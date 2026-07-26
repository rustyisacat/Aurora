package com.rusty.aurora.service

import com.rusty.aurora.api.AuroraHttpServer
import com.rusty.aurora.model.ServerStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

/**
 * Owns the [AuroraHttpServer] lifecycle.
 *
 * Deliberately independent of any Activity/Service context - it only
 * needs a factory that builds a fresh server for a given port. That
 * means it can be started from a plain ViewModel today (v0.1) and moved
 * into a foreground Service later with no change beyond *where*
 * [start]/[stop] get called from.
 *
 * A new [AuroraHttpServer] instance is created on every [start], because
 * NanoHTTPD server instances aren't reliably restartable after [stop].
 */
class AuroraServerController(
    private val serverFactory: (port: Int) -> AuroraHttpServer
) {
    private var server: AuroraHttpServer? = null

    private val _status = MutableStateFlow(ServerStatus.STOPPED)
    val status: StateFlow<ServerStatus> = _status.asStateFlow()

    var port: Int = DEFAULT_PORT
        private set

    fun start(port: Int = DEFAULT_PORT) {
        if (_status.value == ServerStatus.RUNNING) return

        this.port = port
        val newServer = serverFactory(port)
        try {
            newServer.start(AuroraHttpServer.DEFAULT_SOCKET_TIMEOUT_MS, true)
            server = newServer
            _status.value = ServerStatus.RUNNING
        } catch (e: IOException) {
            _status.value = ServerStatus.ERROR
        }
    }

    fun stop() {
        server?.stop()
        server = null
        _status.value = ServerStatus.STOPPED
    }

    companion object {
        const val DEFAULT_PORT = 8080
    }
}
