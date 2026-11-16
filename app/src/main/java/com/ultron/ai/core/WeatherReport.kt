package com.ultron.ai.core

import java.time.LocalDateTime
import kotlin.math.roundToInt

/**
 * Weather reports in plain English.
 */
object WeatherReport {

    data class Hour(
        val time: LocalDateTime,
        val temperature: Double,
        val feelsLike: Double,
        val precipitationChance: Int?,
        val precipitationMm: Double,
        val code: Int,
        val windKmh: Double
    )

    data class Day(
        val date: java.time.LocalDate,
        val min: Double,
        val max: Double,
        val code: Int,
        val precipitationMm: Double,
        val precipitationChance: Int?,
        val windMaxKmh: Double
    )

    data class Forecast(
        val place: String,
        val now: Hour,
        val hours: List<Hour>,
        val days: List<Day>
    )

    fun spoken(f: Forecast, dayOffset: Int = 0, russian: Boolean = false): String =
        if (dayOffset == 0) current(f, russian) else ahead(f, dayOffset, russian)

    private const val WET_CHANCE = 40
    private const val SOME_CHANCE = 25
    private const val WET_MM = 0.2

    fun current(f: Forecast, russian: Boolean = false): String {
        val n = f.now
        val rest = f.hours.filter { it.time.toLocalDate() == n.time.toLocalDate() && it.time > n.time }
        val next = rest.take(4)
        val condition = WeatherCodes.description(n.code, false)
        val feels = n.feelsLike.roundToInt().let { if (it != n.temperature.roundToInt()) it else null }
        return buildString {
            append("It's ${Say.deg(n.temperature)} in ${f.place}")
            feels?.let { append(", feels like ${Say.deg(it.toDouble())}") }
            append(", $condition, wind ${Say.wind(n.windKmh, false)}.")
            append(' ').append(precipitation(rest, false, later = true))
            if (next.isNotEmpty()) {
                val lo = next.minOf { it.temperature }
                val hi = next.maxOf { it.temperature }
                val span = Say.range(lo, hi, false)
                append(' ').append("Over the next few hours $span.")
            }
        }
    }

    private fun ahead(f: Forecast, dayOffset: Int, russian: Boolean = false): String {
        val date = f.now.time.toLocalDate().plusDays(dayOffset.toLong())
        val day = f.days.firstOrNull { it.date == date }
        val hours = f.hours.filter { it.time.toLocalDate() == date }
        val name = when (dayOffset) {
            1 -> "Tomorrow"
            else -> "The day after tomorrow"
        }
        val lo = day?.min ?: hours.minOfOrNull { it.temperature } ?: return noData(f.place, false)
        val hi = day?.max ?: hours.maxOf { it.temperature }
        val code = day?.code ?: hours.groupingBy { it.code }.eachCount().maxBy { it.value }.key
        val windMax = day?.windMaxKmh ?: hours.maxOfOrNull { it.windKmh } ?: 0.0
        val condition = WeatherCodes.description(code, false)
        return buildString {
            append("$name in ${f.place}: ${Say.range(lo, hi, false)}, $condition.")
            append(' ').append(precipitation(hours, false, later = false, dayTotal = day))
            val gusts = Say.wind(windMax, false)
            append(' ').append("Wind up to $gusts.")
        }
    }

    private fun precipitation(hours: List<Hour>, russian: Boolean = false, later: Boolean = false, dayTotal: Day? = null): String {
        val wet = hours.filter { isWet(it) }
        val chance = hours.mapNotNull { it.precipitationChance }.maxOrNull() ?: dayTotal?.precipitationChance
        if (wet.isEmpty()) return dry(chance, false, later)
        val mm = maxOf(hours.sumOf { it.precipitationMm }, dayTotal?.precipitationMm ?: 0.0)
        return wet(wet, mm, chance, false)
    }

    private fun dry(chance: Int?, russian: Boolean = false, later: Boolean = false): String = when {
        chance != null && chance >= SOME_CHANCE -> "Rain is unlikely, $chance% chance."
        later -> "Dry for the rest of the day."
        else -> "No rain expected."
    }

    private fun wet(wet: List<Hour>, mm: Double, chance: Int?, russian: Boolean = false): String {
        val from = Say.clock(wet.first().time, false)
        val to = Say.clock(wet.last().time.plusHours(1), false)
        val gaps = wet.zipWithNext().any { (a, b) -> b.time != a.time.plusHours(1) }
        val parts = mutableListOf(
            kind(wet, false) + " from $from to $to" + (if (gaps) " on and off" else "")
        )
        if (mm >= WET_MM) parts += "about ${Say.amount(mm)} mm"
        chance?.let { parts += "$it% chance" }
        return parts.joinToString(", ") + "."
    }

    private fun isWet(h: Hour): Boolean =
        (h.precipitationChance ?: 0) >= WET_CHANCE || h.precipitationMm >= WET_MM

    private fun kind(wet: List<Hour>, russian: Boolean = false): String {
        val snow = wet.count { it.code in SNOW_CODES }
        return when {
            snow == wet.size -> "Snow"
            snow > 0 -> "Rain and snow"
            wet.any { it.code in STORM_CODES } -> "Rain with thunderstorms"
            else -> "Rain"
        }
    }

    private val SNOW_CODES = setOf(71, 73, 75, 77, 85, 86)
    private val STORM_CODES = setOf(95, 96, 99)

    private fun noData(place: String, russian: Boolean = false) =
        "There's no forecast for $place that day."
}

internal object Say {
    fun deg(t: Double): String = t.roundToInt().let { if (it > 0) "+$it°" else "$it°" }

    fun range(lo: Double, hi: Double, russian: Boolean = false): String = when {
        lo.roundToInt() == hi.roundToInt() -> deg(lo)
        else -> "${deg(lo)} to ${deg(hi)}"
    }

    fun wind(kmh: Double, russian: Boolean = false): String =
        "${kmh.roundToInt()} km/h"

    fun amount(mm: Double): String = if (mm < 1) "%.1f".format(mm) else "${mm.roundToInt()}"

    fun clock(t: LocalDateTime, russian: Boolean = false): String = when {
        t.hour != 0 || t.minute != 0 -> "%d:%02d".format(t.hour, t.minute)
        else -> "midnight"
    }
}
