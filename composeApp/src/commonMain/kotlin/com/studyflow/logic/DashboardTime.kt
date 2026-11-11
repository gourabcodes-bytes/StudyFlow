package com.studyflow.logic

import com.studyflow.util.isoDay
import com.studyflow.util.toLocal
import com.studyflow.util.toMillis
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Start (Monday 00:00) and end (next Monday 00:00) of the week containing [now], in epoch millis. */
fun weekBounds(now: Long): Pair<Long, Long> {
    val local = now.toLocal()
    val monday = local.date.minus(isoDay(local) - 1, DateTimeUnit.DAY)
    return toMillis(monday, 0) to toMillis(monday.plus(7, DateTimeUnit.DAY), 0)
}

/** Start of today and start of tomorrow (device time zone), in epoch millis. */
fun dayBounds(now: Long): Pair<Long, Long> {
    val date = now.toLocal().date
    return toMillis(date, 0) to toMillis(date.plus(1, DateTimeUnit.DAY), 0)
}

/** Which calendar day (in the device time zone) a moment falls on, as a day number. */
fun epochDay(millis: Long): Long = millis.toLocal().date.toEpochDays()
