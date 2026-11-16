package com.ultron.ai.core

import com.ultron.ai.domain.model.CommandResult

/**
 * Weather questions and setting home city.
 */
object WeatherRequest {

    private val I = RegexOption.IGNORE_CASE

    private val englishHomeCity =
        Regex("""(?:my\s+(?:home\s+)?city\s+is|i\s+live\s+in|en\s+ooru)\s+([\p{L}][\p{L}\- ]{1,40}?)(?=[.,!?]|$)""", I)

    private val weatherWords = Regex(
        """(?:weather|rain|snow|umbrella|wind|climate|mazhai)""",
        I
    )

    private val asks = Regex(
        """\?\s*$|^(?:what|how|when|will|is\s+it|do\s+i|epdi)(?!\p{L})""",
        I
    )

    private val place = Regex("""(?:^|\s)(?:in|la)\s+([\p{Lu}][\p{L}\-]+(?:\s+[\p{Lu}][\p{L}\-]+)?)""")

    fun parse(text: String, dayOffset: Int): CommandResult? {
        val t = text.trim()
        val city = englishHomeCity.find(t)?.groupValues?.get(1)?.trim()
        val weather = CommandResult.Weather(placeIn(t, except = city), dayOffset)
            .takeIf { weatherWords.containsMatchIn(t) && asks.containsMatchIn(t) }
        return when {
            city != null && weather != null ->
                CommandResult.Sequence(listOf(CommandResult.SetHomeCity(city), weather.copy(place = null)))
            city != null -> CommandResult.SetHomeCity(city)
            else -> weather
        }
    }

    private fun placeIn(text: String, except: String?): String? =
        place.findAll(text).map { it.groupValues[1] }.firstOrNull { it != except }
}
