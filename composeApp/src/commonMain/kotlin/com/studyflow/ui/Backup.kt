package com.studyflow.data

import com.studyflow.model.AppData
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The file written by "Save backup": the whole database plus a little information about it. */
@Serializable
data class BackupFile(
    val app: String = "StudyFlow",
    val version: Int = 1,
    val exportedAt: Long,
    val data: AppData,
)

private val BackupJson = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
}

fun createBackupText(data: AppData, now: Long): String =
    BackupJson.encodeToString(BackupFile.serializer(), BackupFile(exportedAt = now, data = data))

sealed interface RestoreResult {
    data class Ok(val data: AppData, val exportedAt: Long) : RestoreResult
    data class Bad(val message: String) : RestoreResult
}

fun parseBackup(text: String): RestoreResult {
    val file = try {
        BackupJson.decodeFromString(BackupFile.serializer(), text)
    } catch (e: Exception) {
        return RestoreResult.Bad("Could not read this file. Is it a StudyFlow backup?")
    }
    if (file.app != "StudyFlow") return RestoreResult.Bad("This file is not a StudyFlow backup.")
    if (file.version > 1) return RestoreResult.Bad("This backup was made by a newer version of StudyFlow. Update the app first.")
    return RestoreResult.Ok(file.data, file.exportedAt)
}
