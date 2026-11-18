package com.ultron.ai.core

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Turns spoken time expressions into an actual instant.
 *
 * Deliberately narrow: it handles the phrasings people actually use when
 * telling an assistant to remember something ("text text 15:00", "text text",
 * "in 2 hours", "text text text 10"), and returns null for anything it
 * isn't confident about, so a misread never silently books the wrong slot.
 */
object DateTimeParser {

    /**
     * @param instant when the event starts
     * @param hadExplicitTime false when only a day was given, so the caller
     *        can fall back to a sensible default hour instead of midnight.
     */
    data class Parsed(val instant: LocalDateTime, val hadExplicitTime: Boolean)

    /** Used when a day is named without a time ("text text"). */
    const val DEFAULT_HOUR = 9

    /**
     * How far ahead a reminder fires by default. Ten minutes is too late to
     * act on anything that needs travel or preparation, so an hour is the
     * more useful default for a personal assistant.
     */
    const val DEFAULT_REMINDER_MINUTES = 60

    private val LEAD_TIME = Regex(
        """(\d+)?\s*(hours?|minutes?|mins?)\s*(?:before)""",
        RegexOption.IGNORE_CASE
    )

    private val BARE_LEAD = Regex(
        """(?:(\d+)\s*)?(hours?|minutes?|mins?)\s+prior""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Reads how long before the event the reminder should fire:
     * "1 hour before" → 60, "2 hours before" → 120, "30 mins before" → 30.
     * Returns null when the user didn't say, so the caller keeps its default.
     */
    fun parseReminderLeadMinutes(text: String): Int? {
        val lower = text.lowercase()
        val match = LEAD_TIME.find(lower) ?: BARE_LEAD.find(lower) ?: return null
        val amount = match.groupValues[1].toIntOrNull() ?: 1
        val unit = match.groupValues[2]
        val minutes = when {
            unit.startsWith("hour") -> amount * 60
            else -> amount
        }
        return minutes.takeIf { it in 1..(14 * 24 * 60) }
    }

    private val WEEKDAYS: Map<String, DayOfWeek> = mapOf(
        "monday" to DayOfWeek.MONDAY, "tuesday" to DayOfWeek.TUESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY, "thursday" to DayOfWeek.THURSDAY,
        "friday" to DayOfWeek.FRIDAY, "saturday" to DayOfWeek.SATURDAY,
        "sunday" to DayOfWeek.SUNDAY
    )

    private val RELATIVE_UNITS = Regex(
        """(?:in)\s+(\d+)\s*(minutes?|mins?|hours?|days?|weeks?)""",
        RegexOption.IGNORE_CASE
    )

    private val EXPLICIT_TIME = Regex(
        """(?:at)\s*(\d{1,2})(?:[:.](\d{2}))?\s*(am|pm)?""",
        RegexOption.IGNORE_CASE
    )

    private val BARE_TIME = Regex("""\b(\d{1,2})[:.](\d{2})\b""")

    fun parse(text: String, now: LocalDateTime): Parsed? {
        val lower = text.lowercase()

        relativeOffset(lower, now)?.let { return Parsed(it, hadExplicitTime = true) }

        val day = resolveDay(lower, now)
        val time = resolveTime(lower)

        return when {
            time != null -> {
                val base = day ?: now.toLocalDate()
                var candidate = LocalDateTime.of(base, time)
                // "at 9" said at 22:00 with no day means tomorrow morning.
                if (day == null && candidate.isBefore(now)) candidate = candidate.plusDays(1)
                Parsed(candidate, hadExplicitTime = true)
            }
            day != null ->
                Parsed(LocalDateTime.of(day, LocalTime.of(DEFAULT_HOUR, 0)), hadExplicitTime = false)
            else -> null
        }
    }

    /** Convenience overload for callers that only have a zone. */
    fun parse(text: String, zone: ZoneId = ZoneId.systemDefault()): Parsed? =
        parse(text, LocalDateTime.now(zone))

    private fun relativeOffset(text: String, now: LocalDateTime): LocalDateTime? {
        val m = RELATIVE_UNITS.find(text) ?: return null
        val amount = m.groupValues[1].toLongOrNull() ?: return null
        val unit = m.groupValues[2]
        return when {
            unit.startsWith("min") -> now.plusMinutes(amount)
            unit.startsWith("hour") -> now.plusHours(amount)
            unit.startsWith("day") -> now.plusDays(amount)
            unit.startsWith("week") -> now.plusWeeks(amount)
            else -> null
        }
    }

    private fun resolveDay(text: String, now: LocalDateTime) = when {
        text.contains("day after tomorrow") ->
            now.toLocalDate().plusDays(2)
        text.contains("tomorrow") || text.contains("naalaiki") ->
            now.toLocalDate().plusDays(1)
        text.contains("today") || text.contains("innaiku") ->
            now.toLocalDate()
        else -> WEEKDAYS.entries
            .firstOrNull { (word, _) -> text.contains(word) }
            ?.let { (_, dow) -> now.toLocalDate().with(TemporalAdjusters.next(dow)) }
    }

    private fun resolveTime(text: String): LocalTime? {
        EXPLICIT_TIME.find(text)?.let { m ->
            val hour = m.groupValues[1].toIntOrNull() ?: return@let
            val minute = m.groupValues[2].toIntOrNull() ?: 0
            val qualifier = m.groupValues[3]
            val adjusted = applyDayPart(hour, qualifier)
            if (adjusted in 0..23 && minute in 0..59) return LocalTime.of(adjusted, minute)
        }
        BARE_TIME.find(text)?.let { m ->
            val hour = m.groupValues[1].toIntOrNull() ?: return@let
            val minute = m.groupValues[2].toIntOrNull() ?: return@let
            if (hour in 0..23 && minute in 0..59) return LocalTime.of(hour, minute)
        }
        return null
    }

    /** "at 3pm" is 15:00; "at 8am" is 08:00. */
    private fun applyDayPart(hour: Int, qualifier: String): Int = when {
        qualifier.isBlank() -> hour
        qualifier == "am" -> if (hour == 12) 0 else hour
        qualifier == "pm" -> if (hour < 12) hour + 12 else hour
        else -> hour
    }
}
