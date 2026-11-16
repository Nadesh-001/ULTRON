package com.ultron.ai.core.modes

import com.ultron.ai.domain.model.CommandResult

/**
 * How modes are parsed from speech in English and Tanglish.
 */
object ModePhrases {

    private const val MAX_NAME_WORDS = 4

    private const val EN_DESCRIPTION = 3

    private val wake = Regex("^(?:ultron|friday)[,!.\\s]+")

    private val createEn = Regex(
        "^(?:please\\s+)?(?:create|make|add|set up)\\s+(?:me\\s+)?(?:a\\s+|an\\s+|new\\s+)*" +
            "(?:mode\\s+(?:called\\s+)?(.+?)|(.+?)\\s+mode)" +
            "(?:\\s*[.:;,—–-]\\s*|\\s+(?=(?:where|when|that|which|it)\\s)|$)(.*)$",
        RegexOption.DOT_MATCHES_ALL
    )

    private val descriptionLead = Regex(
        "^(?:where\\s+|when\\s+|that\\s+|which\\s+|it\\s+)+"
    )
    private val nameLead = Regex("^(?:called|named)\\s+")

    private val exit = listOf(
        Regex("^(?:stop|deactivate|turn off|exit|end|disable|leave)\\s+(?:the\\s+)?(?:mode\\s+(.+)|(.+?)\\s+mode)$"),
        Regex("^(.+?)\\s+mode\\s+(?:off\\s+pannu|moodu|niruthu)$")
    )
    private val describe = listOf(
        Regex("^what does (?:the\\s+)?(.+?)\\s+mode do$"),
        Regex("^(?:show|tell me about)\\s+(?:the\\s+)?(.+?)\\s+mode$")
    )
    private val delete = listOf(
        Regex("^(?:delete|remove|forget)\\s+(?:the\\s+)?mode\\s+(.+)$"),
        Regex("^(?:delete|remove|forget)\\s+(?:the\\s+)?(.+?)\\s+mode$")
    )
    private val addTo = listOf(
        Regex("^add\\s+to\\s+(?:the\\s+)?mode\\s+(.+)$")
    )
    private val removeFrom = listOf(
        Regex("^(?:remove|take)\\s+(.+?)\\s+(?:out\\s+)?(?:of|from)\\s+(?:the\\s+)?(.+?)\\s+mode$")
    )

    private val list = Regex(
        "^(?:(?:list|show)\\s+(?:my\\s+)?modes|what modes(?: do i have)?|enna modes irukku)$"
    )
    private val run = listOf(
        Regex("^(?:(?:start|activate|turn on|enable|run|switch to)\\s+)?(?:the\\s+)?(.+?)\\s+mode$"),
        Regex("^(.+?)\\s+mode\\s+(?:on\\s+pannu|podu)$")
    )

    fun parse(text: String): CommandResult.Mode? {
        val t = clean(text)
        return parseCreate(t)
            ?: parseEdit(t)
            ?: CommandResult.Mode.ListAll.takeIf { list.containsMatchIn(t) }
            ?: firstName(t, describe)?.let { CommandResult.Mode.Describe(it) }
            ?: firstName(t, delete)?.let { CommandResult.Mode.Delete(it) }
            ?: firstName(t, exit)?.let { CommandResult.Mode.Exit(it) }
            ?: firstName(t, run)?.let { CommandResult.Mode.Run(it) }
    }

    private fun parseEdit(t: String): CommandResult.Mode? =
        addTo.firstNotNullOfOrNull { it.find(t) }?.let { CommandResult.Mode.AddTo(it.groupValues[1]) }
            ?: removeFrom.firstNotNullOfOrNull { it.find(t) }?.let { m ->
                name(m.groupValues[2])?.let { CommandResult.Mode.RemoveFrom(m.groupValues[1], it) }
            }

    private fun parseCreate(t: String): CommandResult.Mode.Create? =
        createEn.find(t)?.let { m ->
            name(m.groupValues[1].ifEmpty { m.groupValues[2] })
                ?.let { CommandResult.Mode.Create(it, description(m.groupValues[EN_DESCRIPTION])) }
        }

    private fun name(raw: String): String? {
        val n = raw.replace(nameLead, "").trim(' ', '«', '»', '"', '\'', '“', '”', '.', ',', '!', '?')
        val words = n.split(' ').filter { it.isNotBlank() }
        return n.takeIf { words.isNotEmpty() && words.size <= MAX_NAME_WORDS }
    }

    private fun description(raw: String): String? =
        raw.trim().replace(descriptionLead, "").trim(' ', '.', ',', '!', ';', ':').takeIf { it.length > 2 }

    private fun firstName(t: String, patterns: List<Regex>): String? = patterns.firstNotNullOfOrNull { p ->
        p.find(t)?.groupValues?.drop(1)?.firstOrNull { it.isNotBlank() }?.let(::name)
    }

    private fun clean(text: String): String =
        text.trim().lowercase().replace(wake, "").trim().trimEnd('.', '!', '?', ' ')
}
