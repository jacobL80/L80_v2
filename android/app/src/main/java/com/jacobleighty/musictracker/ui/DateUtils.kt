package com.jacobleighty.musictracker.ui

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object DateUtils {
    val MONTHS = listOf("JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC")

    fun expandYear(y: Int): Int = if (y < 100) (if (y < 50) 2000 + y else 1900 + y) else y

    fun hasFullDate(s: String): Boolean = s.trim().split("/").size == 3

    fun parseDate(s: String): LocalDate {
        val parts = s.trim().split("/")
        return try {
            when (parts.size) {
                3 -> LocalDate.of(expandYear(parts[2].toInt()), parts[0].toInt(), parts[1].toInt())
                2 -> LocalDate.of(expandYear(parts[1].toInt()), parts[0].toInt(), 1)
                else -> LocalDate.of(expandYear(parts[0].toInt()), 1, 1)
            }
        } catch (_: Exception) { LocalDate.now() }
    }

    fun getYear(s: String): String = expandYear(s.trim().split("/").last().toIntOrNull() ?: 0).toString()

    fun sortKey(name: String): String =
        name.replace(Regex("^(The|A)\\s+", RegexOption.IGNORE_CASE), "")

    data class DateParts(val month: String?, val day: Int?, val year: Int?)

    fun parseParts(s: String): DateParts {
        val parts = s.trim().split("/")
        return try {
            when (parts.size) {
                3 -> DateParts(MONTHS[parts[0].toInt() - 1], parts[1].toInt(), expandYear(parts[2].toInt()))
                2 -> DateParts(MONTHS[parts[0].toInt() - 1], null, expandYear(parts[1].toInt()))
                else -> DateParts(null, null, parts.firstOrNull()?.toIntOrNull()?.let { expandYear(it) })
            }
        } catch (_: Exception) { DateParts(null, null, null) }
    }

    fun hasDateDetail(s: String?): Boolean = !s.isNullOrEmpty() && s.split("/").size > 1

    fun formatLastDate(s: String): String {
        val (month, day, year) = parseParts(s)
        return when {
            month != null && day != null -> "$month $day, $year"
            month != null -> "$month $year"
            else -> year?.toString() ?: ""
        }
    }

    fun daysUntil(nextRelease: String): Long =
        ChronoUnit.DAYS.between(LocalDate.now(), parseDate(nextRelease))
}
