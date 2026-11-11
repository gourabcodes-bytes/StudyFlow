package com.studyflow.data

import com.studyflow.logic.Celebration
import com.studyflow.logic.celebrationsFor
import com.studyflow.logic.missingEvents
import com.studyflow.logic.totalXp
import com.studyflow.logic.weekBounds
import com.studyflow.model.AppData
import com.studyflow.model.ClassSlot
import com.studyflow.model.FocusSession
import com.studyflow.model.Note
import com.studyflow.model.StudyTask
import com.studyflow.model.Syncable
import com.studyflow.model.XpEvent
import com.studyflow.sync.mergeData
import com.studyflow.sync.purgeTombstones
import com.studyflow.util.DAY_MS
import com.studyflow.util.newId
import com.studyflow.util.nowMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val DATA_FILE = "data.json"
private const val CORRUPT_BACKUP_FILE = "data.corrupt.json"
private const val TOMBSTONE_TTL_MS = 60 * DAY_MS

/**
 * The single source of truth. The UI observes [data]; every change is written to disk
 * (offline-first) and can be merged with another device's copy via [mergeRemote].
 */
class StudyRepository(
    private val storage: Storage,
    private val scope: CoroutineScope,
) {
    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _celebrations = MutableStateFlow<List<Celebration>>(emptyList())
    val celebrations: StateFlow<List<Celebration>> = _celebrations.asStateFlow()
    private var nextCelebrationId = 1L

    private val writeLock = Mutex()

    init {
        scope.launch { load() }
    }

    private suspend fun load() {
        val text = storage.read(DATA_FILE)
        var saved = AppData()
        if (text != null) {
            try {
                saved = AppJson.decodeFromString(AppData.serializer(), text)
            } catch (e: Exception) {
                // Never silently throw away a file we cannot read: keep a copy next to it.
                storage.write(CORRUPT_BACKUP_FILE, text)
            }
        }
        val cutoff = nowMillis() - TOMBSTONE_TTL_MS
        _data.update { current ->
            val merged = mergeData(saved, current).purgeTombstones(cutoff)
            val bounds = weekBounds(nowMillis())
            val missing = missingEvents(merged, nowMillis(), bounds.first, bounds.second) { it / DAY_MS }
            if (missing.isNotEmpty()) merged.copy(xp = merged.xp + missing) else merged
        }
        _loaded.value = true
        persist()
    }

    private fun persist() {
        scope.launch {
            // Never write before the file has been read, or we would overwrite the user's data.
            _loaded.first { it }
            flush()
        }
    }

    /** Writes the current state to disk right now (used when the desktop window closes). */
    suspend fun flush() {
        writeLock.withLock {
            storage.write(DATA_FILE, AppJson.encodeToString(AppData.serializer(), _data.value))
        }
    }

    private fun mutate(block: (AppData) -> AppData) {
        var xpBefore = 0
        var xpAfter = 0
        var created = emptyList<XpEvent>()
        _data.update { current ->
            xpBefore = totalXp(current.xp)
            val updated = block(current)
            val bounds = weekBounds(nowMillis())
            val missing = missingEvents(updated, nowMillis(), bounds.first, bounds.second) { it / DAY_MS }
            if (missing.isNotEmpty()) {
                val withMissing = updated.copy(xp = updated.xp + missing)
                xpAfter = totalXp(withMissing.xp)
                created = missing
                withMissing
            } else {
                xpAfter = totalXp(updated.xp)
                updated
            }
        }
        if (created.isNotEmpty() || xpAfter > xpBefore) {
            val cel = celebrationsFor(created, xpBefore, xpAfter, nextCelebrationId)
            if (cel.isNotEmpty()) {
                nextCelebrationId += cel.size
                _celebrations.update { it + cel }
            }
        }
        persist()
    }

    fun dismissCelebration(id: Long) {
        _celebrations.update { list -> list.filterNot { it.id == id } }
    }

    private fun <T : Syncable> List<T>.withItem(item: T): List<T> =
        if (any { it.id == item.id }) map { if (it.id == item.id) item else it } else this + item

    // ---- timetable ----

    fun saveClass(item: ClassSlot) {
        val stamped = item.copy(updatedAt = nowMillis(), deleted = false)
        mutate { it.copy(classes = it.classes.withItem(stamped)) }
    }

    fun deleteClass(id: String) {
        val stamp = nowMillis()
        mutate { d -> d.copy(classes = d.classes.map { if (it.id == id) it.copy(deleted = true, updatedAt = stamp) else it }) }
    }

    // ---- deadlines ----

    fun saveTask(item: StudyTask) {
        val stamped = item.copy(updatedAt = nowMillis(), deleted = false)
        mutate { it.copy(tasks = it.tasks.withItem(stamped)) }
    }

    fun setTaskDone(id: String, done: Boolean) {
        val stamp = nowMillis()
        mutate { d -> d.copy(tasks = d.tasks.map { if (it.id == id) it.copy(done = done, updatedAt = stamp) else it }) }
    }

    fun deleteTask(id: String) {
        val stamp = nowMillis()
        mutate { d -> d.copy(tasks = d.tasks.map { if (it.id == id) it.copy(deleted = true, updatedAt = stamp) else it }) }
    }

    // ---- focus ----

    fun addSession(label: String, startedAt: Long, durationSec: Int) {
        val session = FocusSession(
            id = newId(),
            label = label,
            startedAt = startedAt,
            durationSec = durationSec,
            updatedAt = nowMillis(),
        )
        mutate { it.copy(sessions = it.sessions + session) }
    }

    // ---- notes ----

    fun saveNote(item: Note) {
        val stamped = item.copy(updatedAt = nowMillis(), deleted = false)
        mutate { it.copy(notes = it.notes.withItem(stamped)) }
    }

    fun addNote(title: String, body: String) {
        val note = Note(
            id = newId(),
            title = title,
            body = body,
            createdAt = nowMillis(),
            updatedAt = nowMillis(),
        )
        saveNote(note)
    }

    fun deleteNote(id: String) {
        val stamp = nowMillis()
        mutate { d -> d.copy(notes = d.notes.map { if (it.id == id) it.copy(deleted = true, updatedAt = stamp) else it }) }
    }

    // ---- sync ----

    fun snapshot(): AppData = _data.value

    /** Merges another device's data into ours and returns the merged result. */
    fun mergeRemote(remote: AppData): AppData {
        var merged = AppData()
        _data.update { current ->
            merged = mergeData(current, remote)
            val bounds = weekBounds(nowMillis())
            val missing = missingEvents(merged, nowMillis(), bounds.first, bounds.second) { it / DAY_MS }
            if (missing.isNotEmpty()) merged.copy(xp = merged.xp + missing) else merged
        }
        persist()
        return merged
    }

    fun restore(remote: AppData): AppData = mergeRemote(remote)
}
