package com.studyflow.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Data lives in ~/.studyflow (e.g. C:\Users\you\.studyflow on Windows). */
actual fun createStorage(): Storage = FileStorage(File(System.getProperty("user.home"), ".studyflow"))

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
