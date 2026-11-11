package com.studyflow.data

import kotlinx.serialization.json.Json

/** One JSON configuration for the files on disk and for the sync protocol. */
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
