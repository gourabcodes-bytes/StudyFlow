package com.studyflow.data

/** Tiny file-like key/value store. Each platform provides its own implementation. */
interface Storage {
    suspend fun read(name: String): String?
    suspend fun write(name: String, text: String)
}

/** Creates the platform's storage (app-private folder on Android, ~/.studyflow on desktop). */
expect fun createStorage(): Storage
