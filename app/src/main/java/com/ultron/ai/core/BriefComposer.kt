package com.ultron.ai.core

import java.time.LocalTime

import java.util.Locale

/**
 * Turns the raw pieces of a briefing into something worth listening to.
 *
 * Kept pure so the wording can be tested: a spoken summary that rambles is
 * useless, and the failure mode to avoid is reading out twenty notifications
 * one by one.
 */
object BriefComposer {

    /** One notification, reduced to what a summary needs. */
    data class NotificationSummary(val appName: String, val title: String, val text: String)

    /** One calendar entry, already formatted for speech. */
    data class AgendaItem(val title: String, val timeLabel: String)

    private const val MAX_APPS_NAMED = 3
    private const val MAX_AGENDA_ITEMS = 4

    /**
     * The "what did I miss" answer.
     *
     * Groups by app rather than listing every message, because the useful
     * information is *who* wants you, not the full text of each ping.
     */
    fun missedSummary(notifications: List<NotificationSummary>, russian: Boolean = false): String {
        if (notifications.isEmpty()) {
            return "Nothing new."
        }

        val byApp = notifications.groupBy { it.appName }
            .entries
            .sortedByDescending { it.value.size }

        val total = notifications.size
        val named = byApp.take(MAX_APPS_NAMED).joinToString(", ") { (app, items) ->
            if (items.size == 1) app else "$app (${items.size})"
        }
        val remaining = byApp.size - MAX_APPS_NAMED

        return buildString {
            append("$total notification${if (total == 1) "" else "s"}: $named")
            if (remaining > 0) {
                append(" and $remaining more")
            }
            append(".")

            // A single message is short enough to just read out.
            if (total == 1) {
                val only = notifications.first()
                val body = listOf(only.title, only.text).filter { it.isNotBlank() }.joinToString(" — ")
                if (body.isNotBlank()) append(" $body")
            }
        }
    }

    /**
     * The morning briefing: weather, then what's on, then what's waiting.
     * Ordered so the most actionable thing is heard first.
     */
    fun morningBrief(
        weather: String?,
        agenda: List<AgendaItem>,
        missedCount: Int,
        russian: Boolean = false,
        localTime: LocalTime
    ): String {
        val parts = mutableListOf<String>()

        parts += BriefTiming.greeting(localTime, russian)

        weather?.takeIf { it.isNotBlank() }?.let { parts += it }

        parts += when {
            agenda.isEmpty() ->
                "Nothing in the calendar today."
            else -> {
                val shownEn = agenda.take(MAX_AGENDA_ITEMS)
                    .joinToString(", ") { "${it.title} at ${it.timeLabel}" }
                val extra = agenda.size - MAX_AGENDA_ITEMS
                "Today: $shownEn" + if (extra > 0) " and $extra more." else "."
            }
        }

        if (missedCount > 0) {
            parts += "$missedCount unread."
        }

        return parts.joinToString(" ")
    }

    /** "9:05" style label used in the agenda, independent of locale quirks. */
    fun timeLabel(hour: Int, minute: Int): String =
        String.format(Locale.US, "%d:%02d", hour, minute)
}
