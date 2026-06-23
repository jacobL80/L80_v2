package com.jacobleighty.musictracker.ui

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object DateUtils {
    val MONTHS = listOf("JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC")

    fun expandYear(y: Int): Int = if (y < 100) (if (y < 50) 2000 + y else 1900 + y) else y

    fun hasFullDate(s: String): Boolean {
        val parts = s.trim().split("/")
        return when (parts.size) {
            3 -> true
            2 -> (parts[1].toIntOrNull() ?: 0) <= 31
            else -> false
        }
    }

    fun parseDate(s: String): LocalDate {
        val parts = s.trim().split("/")
        return try {
            when (parts.size) {
                3 -> LocalDate.of(expandYear(parts[2].toInt()), parts[0].toInt(), parts[1].toInt())
                2 -> {
                    val second = parts[1].toInt()
                    if (second <= 31) {
                        // M/D — resolve to next upcoming occurrence
                        val today = LocalDate.now()
                        val candidate = LocalDate.of(today.year, parts[0].toInt(), second)
                        if (!candidate.isBefore(today)) candidate else candidate.plusYears(1)
                    } else {
                        LocalDate.of(expandYear(second), parts[0].toInt(), 1)
                    }
                }
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
                2 -> {
                    val second = parts[1].toInt()
                    if (second <= 31) {
                        val month = parts[0].toInt()
                        val today = LocalDate.now()
                        val candidate = LocalDate.of(today.year, month, second)
                        val year = if (!candidate.isBefore(today)) today.year else today.year + 1
                        DateParts(MONTHS[month - 1], second, year)
                    } else {
                        DateParts(MONTHS[parts[0].toInt() - 1], null, expandYear(second))
                    }
                }
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

    fun isValidDate(s: String): Boolean {
        if (s.isBlank()) return true
        val parts = s.trim().split("/")
        return try {
            when (parts.size) {
                3 -> { LocalDate.of(expandYear(parts[2].toInt()), parts[0].toInt(), parts[1].toInt()); true }
                2 -> {
                    val second = parts[1].toInt()
                    if (second <= 31) {
                        val today = LocalDate.now()
                        LocalDate.of(today.year, parts[0].toInt(), second)
                    } else {
                        LocalDate.of(expandYear(second), parts[0].toInt(), 1)
                    }
                    true
                }
                1 -> parts[0].trim().toIntOrNull() != null
                else -> false
            }
        } catch (_: Exception) { false }
    }

    fun daysUntil(nextRelease: String): Long =
        ChronoUnit.DAYS.between(LocalDate.now(), parseDate(nextRelease))
}
