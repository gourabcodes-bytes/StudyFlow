package com.studyflow

import android.app.Application
import android.content.Context
import android.media.RingtoneManager
import com.studyflow.data.AppContextHolder
import com.studyflow.data.SettingsStore
import com.studyflow.data.StudyRepository
import com.studyflow.data.createStorage
import com.studyflow.notify.AndroidNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Created once per process. Holding the [AppContainer] here (instead of in the Activity)
 * means a running focus timer keeps going when the screen rotates.
 */
class StudyFlowApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        AppContextHolder.appContext = applicationContext

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val storage = createStorage()
        container = AppContainer(
            repository = StudyRepository(storage, scope),
            settings = SettingsStore(storage, scope),
            scope = scope,
            notifier = AndroidNotifier(applicationContext),
            syncHost = null,
            alert = { playAlert(applicationContext) },
        )
    }
}

private fun playAlert(context: Context) {
    try {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        RingtoneManager.getRingtone(context, uri)?.play()
    } catch (e: Exception) {
        // no sound available - ignore
    }
}
