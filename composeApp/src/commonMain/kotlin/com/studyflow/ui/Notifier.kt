package com.studyflow.notify

import com.studyflow.logic.Reminder

/** Shows reminders as system notifications. Each platform provides its own implementation. */
interface Notifier {
    /** Replaces everything that is scheduled with exactly these reminders. */
    fun sync(reminders: List<Reminder>)

    /** Shows a notification right now (used by the "Send a test" button). */
    fun showNow(title: String, text: String)
}

/** Used where a platform cannot show notifications. */
object NoNotifier : Notifier {
    override fun sync(reminders: List<Reminder>) {}
    override fun showNow(title: String, text: String) {}
}
