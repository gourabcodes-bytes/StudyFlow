package com.studyflow.util

val DAY_SHORT = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
val DAY_LONG = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
val MONTH_SHORT = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
val MONTH_LONG = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

const val MINUTE_MS = 60_000L
const val HOUR_MS = 60 * MINUTE_MS
const val DAY_MS = 24 * HOUR_MS

/** 570 -> "09:30" */
fun formatMinute(minute: Int): String {
    val h = (minute / 60).toString().padStart(2, '0')
    val m = (minute % 60).toString().padStart(2, '0')
    return "$h:$m"
}

/** "09:30" -> 570. Returns null for anything that is not a valid 24-hour time. */
fun parseMinute(text: String): Int? {
    val parts = text.trim().split(":")
    if (parts.size != 2) return null
    val h = parts[0].trim().toIntOrNull() ?: return null
    val m = parts[1].trim().toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

/** 90_000_000 ms -> "1d 1h", 3_900_000 ms -> "1h 5m", 120_000 ms -> "2m" */
fun humanDuration(ms: Long): String {
    val totalMinutes = (if (ms < 0) -ms else ms) / MINUTE_MS
    val days = totalMinutes / 1440
    val hours = (totalMinutes % 1440) / 60
    val minutes = totalMinutes % 60
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

/** 1500 -> "25:00" */
fun formatClock(totalSeconds: Int): String {
    val s = if (totalSeconds < 0) 0 else totalSeconds
    return "${(s / 60).toString().padStart(2, '0')}:${(s % 60).toString().padStart(2, '0')}"
}

/**
 * Turns whatever the user typed into a base URL.
 * "192.168.1.20" -> "http://192.168.1.20:8765"
 * "http://pc.local:9000/" -> "http://pc.local:9000"
 */
fun normalizeHost(input: String, defaultPort: Int): String? {
    val s = input.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
    if (s.isEmpty() || s.contains(' ')) return null
    val withPort = if (s.contains(':')) s else "$s:$defaultPort"
    return "http://$withPort"
}
