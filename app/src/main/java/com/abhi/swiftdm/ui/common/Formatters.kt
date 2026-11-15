package com.abhi.swiftdm.ui.common

import java.util.Locale

/** Human-readable sizes and durations for the download UI. */
object Formatters {

    private val UsLocale = Locale.US

    fun bytes(value: Long): String {
        if (value < 0L) return "—"
        if (value < 1024L) return "$value B"
        val kilobytes = value / 1024.0
        if (kilobytes < 1024.0) return String.format(UsLocale, "%.0f KB", kilobytes)
        val megabytes = kilobytes / 1024.0
        if (megabytes < 1024.0) return String.format(UsLocale, "%.1f MB", megabytes)
        return String.format(UsLocale, "%.2f GB", megabytes / 1024.0)
    }

    fun speed(bytesPerSecond: Long): String =
        if (bytesPerSecond <= 0L) "—" else "${bytes(bytesPerSecond)}/s"

    fun eta(seconds: Long?): String {
        if (seconds == null || seconds <= 0L) return "—"
        val minutes = seconds / 60L
        val remainingSeconds = seconds % 60L
        return when {
            minutes >= 60L -> "${minutes / 60L} h ${minutes % 60L} min"
            minutes > 0L -> "$minutes min ${remainingSeconds} s"
            else -> "$remainingSeconds s"
        }
    }

    fun percent(fraction: Float): String =
        String.format(UsLocale, "%.0f%%", fraction.coerceIn(0f, 1f) * 100f)
}
