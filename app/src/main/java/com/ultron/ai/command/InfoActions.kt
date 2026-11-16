package com.ultron.ai.command

import com.ultron.ai.service.WeatherHere
import com.ultron.ai.service.WebResearch
import com.ultron.ai.service.messages.MessageAssistant
import com.ultron.ai.domain.model.CommandResult
import com.ultron.ai.service.ProactiveBriefService
import com.ultron.ai.service.mail.MailAssistant

/** Commands that look something up and tell the user. */
class InfoActions(
    private val weather: WeatherHere,
    private val proactive: ProactiveBriefService,
    private val mail: MailAssistant,
    private val web: WebResearch,
    private val messages: MessageAssistant,
    private val publicApis: com.ultron.ai.data.remote.PublicApisService = com.ultron.ai.data.remote.PublicApisService()
) {

    suspend fun run(c: CommandResult.Info, russian: Boolean): String = when (c) {
        is CommandResult.Weather -> weather(c, russian)
        is CommandResult.SetHomeCity -> homeCity(c.city, russian)
        is CommandResult.WhatDidIMiss -> proactive.whatDidIMiss(russian)
        is CommandResult.TellTime -> tellTime(russian)
        is CommandResult.LookUp -> web.answer(c.question, russian)
        is CommandResult.ReadMessages -> messages.read(c.from, russian)
        is CommandResult.MorningBrief -> proactive.morningBrief(russian)
        is CommandResult.Mail -> mail.handle(c.request, russian).text
        is CommandResult.SearchPublicApis -> searchApis(c.query, c.category)
    }

    private suspend fun searchApis(query: String, category: String?): String {
        val results = publicApis.search(query, category)
        return publicApis.formatSummary(results, query)
    }

    private suspend fun homeCity(said: String, russian: Boolean): String {
        val saved = weather.setHomeCity(said, russian)
        return when {
            saved == null -> "I couldn't find a city called $said."
            else -> "Got it: your city is set to $saved."
        }
    }

    private fun tellTime(russian: Boolean): String {
        val t = java.time.LocalTime.now()
        val hhmm = "%02d:%02d".format(t.hour, t.minute)
        return "It's $hhmm"
    }

    /**
     * Real weather from Open-Meteo. The model has no live data and will
     * happily invent a temperature if asked directly, so this never goes
     * through the LLM.
     */
    private suspend fun weather(c: CommandResult.Weather, russianSpeaker: Boolean): String {
        val where = c.place ?: "where you are"
        return weather.summary(c.place, c.dayOffset, false)
            ?: "Couldn't get the weather for $where"
    }
}
