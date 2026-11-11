package com.studyflow.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Set once by the Application class before anything else touches storage. */
object AppContextHolder {
    lateinit var appContext: Context
}

actual fun createStorage(): Storage = FileStorage(File(AppContextHolder.appContext.filesDir, "studyflow"))

private class FileStorage(private val dir: File) : Storage {
    init {
        dir.mkdirs()
    }

    override suspend fun read(name: String): String? = withContext(Dispatchers.IO) {
        val file = File(dir, name)
        if (file.exists()) file.readText() else null
    }

    override suspend fun write(name: String, text: String) {
        withContext(Dispatchers.IO) {
            // write to a temp file first so a crash can never leave a half-written database
            val target = File(dir, name)
            val temp = File(dir, "$name.tmp")
            temp.writeText(text)
            if (!temp.renameTo(target)) {
                target.writeText(text)
                temp.delete()
            }
        }
    }
}
