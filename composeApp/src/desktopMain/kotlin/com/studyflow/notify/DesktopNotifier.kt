package com.studyflow.notify

import com.studyflow.logic.Reminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.Color
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.awt.image.BufferedImage

/**
 * Shows reminders from the system tray while the app is running (the window can be minimised).
 * A computer app cannot wake itself up when it is closed, so reminders only fire while it is open.
 */
class DesktopNotifier(scope: CoroutineScope) : Notifier {

    @Volatile
    private var pending: List<Reminder> = emptyList()
    private val fired = HashSet<String>()
    private var tray: TrayIcon? = null

    init {
        scope.launch {
            while (true) {
                delay(20_000)
                fireDue()
            }
        }
    }

    override fun sync(reminders: List<Reminder>) {
        pending = reminders
    }

    override fun showNow(title: String, text: String) {
        display(title, text)
    }

    private fun fireDue() {
        val now = System.currentTimeMillis()
        for (reminder in pending) {
            if (reminder.at <= now && fired.add("${reminder.id}:${reminder.at}")) {
                display(reminder.title, reminder.text)
            }
        }
    }

    private fun display(title: String, text: String) {
        try {
            if (!SystemTray.isSupported()) {
                Toolkit.getDefaultToolkit().beep()
                return
            }
            val icon = tray ?: createTrayIcon() ?: return
            icon.displayMessage(title, text, TrayIcon.MessageType.INFO)
        } catch (e: Exception) {
            Toolkit.getDefaultToolkit().beep()
        }
    }

    private fun createTrayIcon(): TrayIcon? {
        return try {
            val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
            val g = image.createGraphics()
            g.color = Color(0x4F, 0x46, 0xE5)
            g.fillOval(1, 1, 14, 14)
            g.dispose()
            val icon = TrayIcon(image, "StudyFlow")
            icon.isImageAutoSize = true
            SystemTray.getSystemTray().add(icon)
            tray = icon
            icon
        } catch (e: Exception) {
            null
        }
    }
}
