package com.studyflow.sync

import kotlinx.coroutines.flow.StateFlow

const val SYNC_PORT = 8765
const val SYNC_CODE_HEADER = "X-Sync-Code"

/**
 * The desktop app can act as a tiny sync hub on the local network.
 * The phone sends its data to POST /sync, the hub merges it with its own and answers
 * with the merged result, so one request synchronises both directions.
 */
interface SyncHost {
    val running: StateFlow<Boolean>
    val addresses: StateFlow<List<String>>
    val lastError: StateFlow<String?>
    fun start()
    fun stop()
}
