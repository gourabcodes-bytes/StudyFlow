package com.studyflow.sync

import com.studyflow.data.AppJson
import com.studyflow.data.SettingsStore
import com.studyflow.data.StudyRepository
import com.studyflow.model.AppData
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

/**
 * A small HTTP server inside the desktop app. Phones sync against it.
 * POST /sync   body: AppData JSON, header X-Sync-Code: <pairing code>
 *              answer: the merged AppData (also merged into this computer's database)
 */
class DesktopSyncHost(
    private val repository: StudyRepository,
    private val settings: SettingsStore,
) : SyncHost {

    private val _running = MutableStateFlow(false)
    override val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _addresses = MutableStateFlow<List<String>>(emptyList())
    override val addresses: StateFlow<List<String>> = _addresses.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    override val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var server: EmbeddedServer<*, *>? = null

    override fun start() {
        if (_running.value) return
        try {
            server = embeddedServer(CIO, port = SYNC_PORT, host = "0.0.0.0") {
                install(ContentNegotiation) { json(AppJson) }
                routing {
                    get("/ping") {
                        call.respondText("StudyFlow hub")
                    }
                    post("/sync") {
                        val sent = call.request.headers[SYNC_CODE_HEADER]
                        if (sent == null || sent != settings.state.value.hostCode) {
                            call.respondText("Wrong pairing code", status = HttpStatusCode.Unauthorized)
                            return@post
                        }
                        val incoming = call.receive<AppData>()
                        val merged = repository.mergeRemote(incoming)
                        call.respond(merged)
                    }
                }
            }.start(wait = false)
            _addresses.value = lanAddresses()
            _lastError.value = null
            _running.value = true
        } catch (e: Exception) {
            server = null
            _running.value = false
            _lastError.value = "Could not start the hub on port $SYNC_PORT: ${e.message ?: "unknown error"}"
        }
    }

    override fun stop() {
        try {
            server?.stop(300, 1000)
        } catch (e: Exception) {
            // already stopped
        }
        server = null
        _running.value = false
    }

    private fun lanAddresses(): List<String> {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
            Collections.list(interfaces)
                .asSequence()
                .filter { it.isUp && !it.isLoopback && !it.isVirtual }
                .flatMap { Collections.list(it.inetAddresses) }
                .filterIsInstance<Inet4Address>()
                .filter { !it.isLoopbackAddress }
                .mapNotNull { it.hostAddress }
                .map { "$it:$SYNC_PORT" }
                .toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
