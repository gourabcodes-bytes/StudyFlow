package com.studyflow.data

import com.studyflow.model.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

private const val SETTINGS_FILE = "settings.json"

/** Device-local settings (sync target, pairing codes, timer lengths). Never synced. */
class SettingsStore(
    private val storage: Storage,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(Settings())
    val state: StateFlow<Settings> = _state.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val writeLock = Mutex()

    init {
        scope.launch {
            val text = storage.read(SETTINGS_FILE)
            var saved = Settings()
            if (text != null) {
                try {
                    saved = AppJson.decodeFromString(Settings.serializer(), text)
                } catch (e: Exception) {
                    // fall back to defaults
                }
            }
            if (saved.hostCode.isBlank()) {
                saved = saved.copy(hostCode = Random.nextInt(100_000, 1_000_000).toString())
            }
            _state.value = saved
            _loaded.value = true
            persist()
        }
    }

    fun update(block: (Settings) -> Settings) {
        _state.update(block)
        persist()
    }

    private fun persist() {
        scope.launch {
            _loaded.first { it }
            flush()
        }
    }

    suspend fun flush() {
        writeLock.withLock {
            storage.write(SETTINGS_FILE, AppJson.encodeToString(Settings.serializer(), _state.value))
        }
    }
}
