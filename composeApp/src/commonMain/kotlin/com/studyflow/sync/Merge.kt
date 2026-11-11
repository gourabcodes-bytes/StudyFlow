package com.studyflow.sync

import com.studyflow.model.AppData
import com.studyflow.model.Syncable

/**
 * Last-write-wins merge: for every id the copy with the newest [Syncable.updatedAt] survives.
 * On a tie the local copy is kept. The operation is idempotent, so syncing twice never
 * changes anything.
 */
fun <T : Syncable> mergeLists(local: List<T>, remote: List<T>): List<T> {
    val byId = LinkedHashMap<String, T>()
    for (item in local) byId[item.id] = item
    for (item in remote) {
        val existing = byId[item.id]
        if (existing == null || item.updatedAt > existing.updatedAt) byId[item.id] = item
    }
    return byId.values.toList()
}

fun mergeData(local: AppData, remote: AppData): AppData = AppData(
    classes = mergeLists(local.classes, remote.classes),
    tasks = mergeLists(local.tasks, remote.tasks),
    sessions = mergeLists(local.sessions, remote.sessions),
    xp = mergeLists(local.xp, remote.xp),
)

/** Forget deletions older than [cutoff] so the database does not grow forever. */
fun AppData.purgeTombstones(cutoff: Long): AppData = copy(
    classes = classes.filterNot { it.deleted && it.updatedAt < cutoff },
    tasks = tasks.filterNot { it.deleted && it.updatedAt < cutoff },
    sessions = sessions.filterNot { it.deleted && it.updatedAt < cutoff },
    xp = xp.filterNot { it.deleted && it.updatedAt < cutoff },
)
