@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.studyflow.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

actual fun createStorage(): Storage = NsDocumentStorage()

private class NsDocumentStorage : Storage {
    private val fileManager = NSFileManager.defaultManager
    private val docsDirUrl: NSURL = fileManager.URLsForDirectory(
        directory = NSDocumentDirectory,
        inDomains = NSUserDomainMask
    ).first() as NSURL
    private val docsPath: String = docsDirUrl.path ?: ""

    override suspend fun read(name: String): String? = withContext(Dispatchers.IO) {
        val path = "$docsPath/$name"
        if (fileManager.fileExistsAtPath(path)) {
            NSString.stringWithContentsOfFile(path, encoding = NSUTF8StringEncoding, error = null)
        } else {
            null
        }
    }

    override suspend fun write(name: String, text: String) {
        withContext(Dispatchers.IO) {
            val path = "$docsPath/$name"
            @Suppress("CAST_NEVER_SUCCEEDS")
            (text as NSString).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        }
    }
}
