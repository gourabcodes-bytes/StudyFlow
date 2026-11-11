package com.studyflow.platform

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberNotificationPermission(): NotificationPermission {
    val context = LocalContext.current
    fun allowed(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    var granted by remember { mutableStateOf(allowed()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        granted = result
    }
    return NotificationPermission(granted) {
        if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
actual fun rememberFileSaver(onResult: (Boolean) -> Unit): (String, String) -> Unit {
    val context = LocalContext.current
    val latest by rememberUpdatedState(onResult)
    var pending by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        val text = pending
        pending = null
        if (uri == null || text == null) {
            latest(false)
        } else {
            val written = try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null
            } catch (e: Exception) {
                false
            }
            latest(written)
        }
    }

    return remember(launcher) {
        val save: (String, String) -> Unit = { name, content ->
            pending = content
            launcher.launch(name)
        }
        save
    }
}

@Composable
actual fun rememberTextFilePicker(onPicked: (String?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val latest by rememberUpdatedState(onPicked)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) {
            latest(null)
        } else {
            val text = try {
                context.contentResolver.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) }
            } catch (e: Exception) {
                null
            }
            latest(text)
        }
    }

    return remember(launcher) {
        val open: () -> Unit = { launcher.launch(arrayOf("*/*")) }
        open
    }
}
