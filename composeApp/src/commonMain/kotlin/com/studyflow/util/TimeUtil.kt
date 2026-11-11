package com.studyflow.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

private fun zone(): TimeZone = TimeZone.currentSystemDefault()

fun Long.toLocal(): LocalDateTime = Instant.fromEpochMilliseconds(this).toLocalDateTime(zone())

fun today(): LocalDate = nowMillis().toLocal().date

fun minuteOfDay(dt: LocalDateTime): Int = dt.hour * 60 + dt.minute

/** 1 = Monday ... 7 = Sunday */
fun isoDay(dt: LocalDateTime): Int = dt.dayOfWeek.isoDayNumber

/** A date plus a minute-of-day in the device time zone -> epoch millis. */
fun toMillis(date: LocalDate, minuteOfDay: Int): Long =
    LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, minuteOfDay / 60, minuteOfDay % 60)
        .toInstant(zone())
        .toEpochMilliseconds()

/** Parses "2026-10-31". Returns null when the text is not a real date. */
fun parseDate(text: String): LocalDate? =
    try {
        LocalDate.parse(text.trim())
    } catch (e: IllegalArgumentException) {
        null
    }

/** "Thu 15 Oct, 18:00" */
fun formatDue(millis: Long): String {
    val dt = millis.toLocal()
    return "${DAY_SHORT[isoDay(dt) - 1]} ${dt.dayOfMonth} ${MONTH_SHORT[dt.monthNumber - 1]}, ${formatMinute(minuteOfDay(dt))}"
}

/** "Thursday, 15 October" */
fun formatDateLong(dt: LocalDateTime): String =
    "${DAY_LONG[isoDay(dt) - 1]}, ${dt.dayOfMonth} ${MONTH_LONG[dt.monthNumber - 1]}"

/** "in 2d 3h" or "overdue by 5h 10m" */
fun relativeDue(dueAt: Long, now: Long): String =
    if (dueAt < now) "overdue by ${humanDuration(now - dueAt)}" else "in ${humanDuration(dueAt - now)}"

/** Material date pickers work with "midnight UTC" milliseconds, whatever the device time zone is. */
fun LocalDate.toPickerMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

fun pickerMillisToDate(millis: Long): LocalDate =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date

/** "Thu 15 Oct 2026" */
fun formatDate(date: LocalDate): String =
    "${DAY_SHORT[date.dayOfWeek.isoDayNumber - 1]} ${date.dayOfMonth} ${MONTH_SHORT[date.monthNumber - 1]} ${date.year}"
