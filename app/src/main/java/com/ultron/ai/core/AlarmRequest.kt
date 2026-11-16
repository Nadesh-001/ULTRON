package com.ultron.ai.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Reads "text text text 8 text text text" and its many relatives.
 *
 * The old pattern only knew "text 7:30" — minutes required, "text" required. So
 * "text text 8 text" was not an alarm at all: it went to the model as
 * conversation, and the model answered "text" without anything having
 * been set. Times now go through [DateTimeParser], the same reading the
 * calendar uses, plus the two things only alarms say: "text 8" for the time,
 * and numbers spoken as words.
 */
object AlarmRequest {

    /**
     * @param date set only when the user's day differs from the next time the
     *   clock would ring anyway — the Clock app takes a time, not a date, and
     *   the reply has to say so rather than pretend.
     */
    data class Alarm(val time: LocalTime, val date: LocalDate?)

    /**
     * A request, not a mention: it has to start with an action or with the
     * alarm itself. "text text text 7 text text?" names a time too, and
     * must not set one. One leading word ("text", "text") is allowed.
     */
    private val trigger = Regex(
        """^(?:alarm)|^(?:\p{L}+[,\s]+)?""" +
            """(?:(?:set|make|start|create)[\s\S]*?(?:alarm)""" +
            """|wake\s+me)""",
        RegexOption.IGNORE_CASE
    )

    /** In an alarm request "at 8" is the time, not a duration. */
    private val naBeforeNumber = Regex("""(^|\s)(?:at|for)\s+(?=\d)""")

    private val numberWords = mapOf(
        "eleven" to 11, "twelve" to 12, "ten" to 10, "nine" to 9, "eight" to 8,
        "seven" to 7, "six" to 6, "five" to 5, "four" to 4, "three" to 3, "two" to 2,
        "one" to 1
    )
    private val spokenHour = Regex("""(^|\s)(at|for)\s+(${numberWords.keys.joinToString("|")})(?=\s|$)""")

    fun parse(text: String, now: LocalDateTime): Alarm? {
        if (!trigger.containsMatchIn(text.trim())) return null
        val normalised = digits(text.lowercase())
            .replace(naBeforeNumber) { "${it.groupValues[1]}at " }
        val parsed = DateTimeParser.parse(normalised, now) ?: return null
        // "alarm" without a time is a question, not 09:00.
        if (!parsed.hadExplicitTime) return null

        val time = parsed.instant.toLocalTime().withSecond(0).withNano(0)
        val clockWouldRing = nextOccurrence(time, now)
        val date = parsed.instant.toLocalDate().takeIf { it != clockWouldRing.toLocalDate() }
        return Alarm(time, date)
    }

    /** When an alarm at [time] actually goes off, set at [now]. */
    fun nextOccurrence(time: LocalTime, now: LocalDateTime): LocalDateTime {
        val today = now.toLocalDate().atTime(time)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    private fun digits(text: String): String =
        text.replace(spokenHour) { m ->
            val (lead, preposition, word) = m.destructured
            "$lead$preposition ${numberWords.getValue(word)}"
        }

    // --- after the request is sent ------------------------------------------

    enum class Check {
        /** The phone's next alarm is the one just asked for. */
        CONFIRMED,

        /** An earlier alarm is next, so this one cannot be seen — not proof of failure. */
        UNVERIFIABLE,

        /** Nothing at that time: the Clock app did not create it. */
        MISSING
    }

    private const val SAME_MINUTE_MS = 60_000L

    /**
     * Android reports only the *next* alarm across all apps. If that is ours,
     * it exists. If nothing is set, or the next one is later than ours, ours
     * was not created. If an earlier one is next, ours is hidden behind it.
     */
    fun check(expectedMillis: Long, nextAlarmMillis: Long?): Check = when {
        nextAlarmMillis == null -> Check.MISSING
        kotlin.math.abs(nextAlarmMillis - expectedMillis) < SAME_MINUTE_MS -> Check.CONFIRMED
        nextAlarmMillis < expectedMillis -> Check.UNVERIFIABLE
        else -> Check.MISSING
    }

    fun reply(time: LocalTime, check: Check, russian: Boolean): String {
        val t = "%02d:%02d".format(time.hour, time.minute)
        return when (check) {
            Check.CONFIRMED -> "Alarm set for $t."
            Check.UNVERIFIABLE ->
                "Alarm for $t sent to Clock. I can't confirm it: an earlier alarm is next."
            Check.MISSING ->
                "Clock didn't create the $t alarm on its own — I've opened it for you to save."
        }
    }

    /**
     * The Clock app takes a time, not a date: it rings at the next such time.
     * When that is not the day the user asked for, nothing is set — an alarm
     * that rings on the wrong morning is worse than none.
     */
    fun wrongDay(alarm: Alarm, now: LocalDateTime, russian: Boolean = false): String {
        val t = "%02d:%02d".format(alarm.time.hour, alarm.time.minute)
        val requested = alarm.date ?: return ""
        return when {
            requested.atTime(alarm.time).isBefore(now) ->
                "$t has already passed."
            else ->
                "Clock can only set the next $t, which isn't the day you asked for. " +
                    "Say “set an alarm for $t” if the next one is fine."
        }
    }
}
