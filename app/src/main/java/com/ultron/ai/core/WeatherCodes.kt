package com.ultron.ai.core

object WeatherCodes {

    fun describe(code: Int, russian: Boolean = false): String = description(code, russian)

    fun summarize(
        place: String,
        temperature: Double,
        feelsLike: Double,
        code: Int,
        russian: Boolean = false
    ): String = currentConditions(place, temperature, code, feelsLike, russian)

    fun description(code: Int, russian: Boolean = false): String = when (code) {
        0 -> "clear"
        1 -> "mostly clear"
        2 -> "partly cloudy"
        3 -> "overcast"
        45, 48 -> "fog"
        51, 53, 55 -> "drizzle"
        56, 57 -> "freezing drizzle"
        61 -> "light rain"
        63 -> "rain"
        65 -> "heavy rain"
        66, 67 -> "freezing rain"
        71 -> "light snow"
        73 -> "snow"
        75 -> "heavy snow"
        77 -> "snow grains"
        80, 81 -> "rain showers"
        82 -> "violent rain showers"
        85, 86 -> "snow showers"
        95 -> "thunderstorm"
        96, 99 -> "thunderstorm with hail"
        else -> "unclear conditions"
    }

    fun currentConditions(
        place: String,
        temperature: Double,
        code: Int,
        apparentTemperature: Double?,
        russian: Boolean = false
    ): String {
        val t = temperature.toInt()
        val condition = description(code, false)
        val mentionFeels = apparentTemperature != null && kotlin.math.abs(apparentTemperature - temperature) >= 3.0
        val feels = apparentTemperature?.toInt()

        return buildString {
            append("In $place it's currently $t°, $condition")
            if (mentionFeels) append(", feels like $feels°")
            append('.')
        }
    }
}
