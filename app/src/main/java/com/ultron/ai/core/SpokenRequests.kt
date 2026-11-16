// Long lines on purpose: regexes, each kept on one line so it can be read whole.
@file:Suppress("MaxLineLength")

package com.ultron.ai.core

import com.ultron.ai.core.people.Channel
import com.ultron.ai.domain.model.CommandResult
import com.ultron.ai.domain.model.MediaKind

/**
 * "text text text text, text text text", "text text: text text text".
 *
 * Only phrasings that say it is a message — "text", "text", a messenger's
 * name — are read here. "text text, text text" could as well be "write
 * an essay that…", and goes to the model, which has the same tool.
 */
object MessageRequest {

    private val I = RegexOption.IGNORE_CASE

    private val messageWord = Regex("""(?:^|\s)(?:sms|message|text)(?=\s|$)""", I)
    private val channelPhrase = Regex("""\s+(?:on|via|in)\s+(?:whatsapp|telegram)""", I)

    private const val VERB = """(?:send|text|message)"""
    private const val SEPARATOR = """\s*(?::|,?\s+(?:saying|that)\s+|,|\.\s+)\s*"""
    private val withBody = Regex("""^$VERB\s+(\p{L}+(?:\s+\p{L}+)?)$SEPARATOR(.+)$""", I)
    private val withoutBody = Regex("""^$VERB\s+(\p{L}+(?:\s+\p{L}+)?)[.!]*$""", I)

    fun parse(text: String): CommandResult.SendMessage? {
        val t = text.trim()
        val via = Channel.named(t)
        if (via == null && !messageWord.containsMatchIn(t)) return null
        val core = t.replace(channelPhrase, "")
            .replace(Regex("""^($VERB)\s+(?:(?:sms|message|a\s+message)\s+)""", I), "$1 ")
            .trim()
        return withBody.find(core)?.let { m ->
            CommandResult.SendMessage(m.groupValues[1].trim(), m.groupValues[2].trim(), via)
        } ?: withoutBody.find(core)?.let { m -> CommandResult.SendMessage(m.groupValues[1].trim(), null, via) }
    }
}

/**
 * Chat request parser.
 */
object ChatRequest {

    private val I = RegexOption.IGNORE_CASE

    private val read = listOf(
        Regex("""^read\s+(?:me\s+)?(?:my\s+)?(?:new\s+|latest\s+)?(?:messages|texts)(?:\s+from\s+(.+?))?[.?!]*$""", I),
        Regex("""^what\s+did\s+(.+?)\s+(?:write|say|text)[.?!]*$""", I),
        Regex("""^(?:any\s+new\s+messages)[.?!]*$""", I),
        Regex("""^(?:do\s+i\s+have\s+)?(?:any\s+)?unread\s+messages[.?!]*$|^who\s+(?:texted|wrote|messaged)\s+me[.?!]*$""", I)
    )

    private val replyToLast = Regex("""^(?:reply)\s*(?::\s*|,?\s+(?:that|saying)\s+|,\s*)(.+)$""", I)

    /** Bare reply without separator */
    private val replyBare = Regex("""^(?:reply)[\s.!—–-]+(.+)$""", I)
    private val separator = Regex("""[:,]|\s(?:that|saying)\s""", I)

    fun parseBareReply(text: String): CommandResult.ReplyMessage? =
        looseReply(text)?.let { CommandResult.ReplyMessage(null, it, loose = it) }

    fun looseReply(text: String): String? =
        replyBare.find(text.trim())?.groupValues?.get(1)?.trim()?.takeUnless { separator.containsMatchIn(it) }

    fun parse(text: String): CommandResult? {
        val t = text.trim()
        replyToLast.find(t)?.let { return CommandResult.ReplyMessage(null, it.groupValues[1].trim()) }
        val m = read.firstNotNullOfOrNull { it.find(t) } ?: return null
        return CommandResult.ReadMessages(m.groupValues.getOrNull(1)?.trim()?.ifBlank { null })
    }
}

/**
 * Call request parser.
 */
object CallRequest {

    private val I = RegexOption.IGNORE_CASE
    private val call = Regex("""^(?:call|dial|ring)\s+(.+?)[.!]*$""", I)
    private val viaApp = Regex(
        """\s+(?:on|via|in|over)\s+(whatsapp|telegram)""", I
    )
    private val viaLine = Regex(
        """\s+(?:on\s+the\s+phone|by\s+phone|normally)""", I
    )

    fun parse(text: String): CommandResult.PhoneCall? {
        val target = call.find(text.trim())?.groupValues?.get(1) ?: return null
        val app = viaApp.find(target)?.let { Channel.named(it.groupValues[1]) }
        val line = viaLine.containsMatchIn(target)
        val who = target.replace(viaApp, "").replace(viaLine, "").trim()
        if (who.isBlank()) return null
        return CommandResult.PhoneCall(who, app ?: Channel.SMS.takeIf { line })
    }
}

/**
 * Media search and play request parser.
 */
object MediaRequest {

    private val I = RegexOption.IGNORE_CASE
    private const val VERB = """(?:play|put\s+on|find|search|show)"""
    private const val YOUTUBE = """(?:youtube)"""

    private val youtubeFirst = Regex("""^$VERB\s+(?:on|in)\s+$YOUTUBE\s+(.+)$""", I)
    private val youtubeLast = Regex("""^$VERB\s+(.+?)\s+(?:on|in)\s+$YOUTUBE[.!]*$""", I)
    private val inApp = Regex("""^$VERB\s+(.+?)\s+(?:on|in)\s+(.+?)[.!]*$""", I)
    private val namedSong = Regex("""^(?:play|put\s+on)\s+(?:song|track|album)\s+(.+?)[.!]*$""", I)

    private val nothingInParticular = Regex("""^(?:music|something|anything)$""", I)

    fun parse(text: String): CommandResult.PlayMedia? {
        val t = text.trim()
        return (youtubeFirst.find(t) ?: youtubeLast.find(t))?.let { video(it.groupValues[1]) }
            // Only a music app's name makes this a search ("text text" does not).
            ?: inApp.find(t)?.takeIf { MusicApps.match(it.groupValues[2]) != null }
                ?.let { music(it.groupValues[1], it.groupValues[2]) }
            ?: namedSong.find(t)?.let { music(it.groupValues[1], null) }
    }

    private fun video(query: String) = query.trim().takeUnless { it.isBlank() }
        ?.let { CommandResult.PlayMedia(it, MediaKind.VIDEO, "youtube") }

    private fun music(query: String, app: String?) = query.trim()
        .takeUnless { it.isBlank() || nothingInParticular.matches(it) }
        ?.let { CommandResult.PlayMedia(it, MediaKind.MUSIC, app?.trim()) }
}
