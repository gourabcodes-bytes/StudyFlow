package com.studyflow.sync

import com.studyflow.data.AppJson
import com.studyflow.data.SettingsStore
import com.studyflow.data.StudyRepository
import com.studyflow.model.AppData
import com.studyflow.util.normalizeHost
import com.studyflow.util.nowMillis
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

data class SyncStatus(
    val busy: Boolean = false,
    val message: String = "Not synced yet",
    val isError: Boolean = false,
)

/** Talks to a [SyncHost] running on another device and merges the answer into the local database. */
class SyncController(
    private val repository: StudyRepository,
    private val settings: SettingsStore,
) {
    private val client = HttpClient {
        install(ContentNegotiation) { json(AppJson) }
        install(HttpTimeout) {
            connectTimeoutMillis = 5_000
            requestTimeoutMillis = 15_000
        }
    }

    private val _status = MutableStateFlow(SyncStatus())
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val gate = Mutex()

    suspend fun syncNow() {
        if (!gate.tryLock()) return // a sync is already running
        try {
            val s = settings.state.value
            val base = normalizeHost(s.syncHost, SYNC_PORT)
            if (base == null) {
                _status.value = SyncStatus(message = "Enter the desktop's address first, e.g. 192.168.1.20", isError = true)
                return
            }
            _status.value = SyncStatus(busy = true, message = "Syncing…")
            try {
                val response = client.post("$base/sync") {
                    contentType(ContentType.Application.Json)
                    header(SYNC_CODE_HEADER, s.syncCode.trim())
                    setBody(repository.snapshot())
                }
                when {
                    response.status == HttpStatusCode.Unauthorized ->
                        _status.value = SyncStatus(message = "Wrong pairing code", isError = true)

                    !response.status.isSuccess() ->
                        _status.value = SyncStatus(message = "The desktop answered with error ${response.status.value}", isError = true)

                    else -> {
                        val remote = response.body<AppData>()
                        val merged = repository.mergeRemote(remote)
                        val now = nowMillis()
                        settings.update { it.copy(lastSyncAt = now) }
                        val tasks = merged.tasks.count { !it.deleted }
                        val classes = merged.classes.count { !it.deleted }
                        _status.value = SyncStatus(message = "Synced: $classes classes, $tasks deadlines")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _status.value = SyncStatus(
                    message = "Could not reach the desktop (${e.message ?: "unknown error"}). " +
                        "Check that both devices are on the same Wi-Fi and the hub is switched on.",
                    isError = true,
                )
            }
        } finally {
            gate.unlock()
        }
    }

    fun close() {
        client.close()
    }
}
