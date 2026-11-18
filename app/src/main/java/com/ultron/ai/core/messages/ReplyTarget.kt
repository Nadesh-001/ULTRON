package com.ultron.ai.core.messages

import com.ultron.ai.core.ContactMatcher

/**
 * Who "text …" is for, what to send, and whether to send it at once.
 *
 * Right after ULTRON reads or announces a message, "text text" is
 * plainly an answer to that chat: it is sent straight away, the way the
 * owner asked. Anything less certain — another chat, or nothing read just
 * now — is read back first ("text «text» text WhatsApp: «…»?").
 */
object ReplyTarget {

    data class Choice(val chat: Conversation, val text: String, val sendNow: Boolean)

    /** A word must match a chat this well to be taken as its name ("text" → text). */
    private const val NAME_SCORE = 70

    private val leadingThat = Regex("^(?:что|чтобы|that|saying)\\s+", RegexOption.IGNORE_CASE)

    /**
     * @param who      the person the phrase seemed to name ("text text: …"), if any
     * @param loose    everything after "text" when nothing separated a name from
     *                 the message, so [who] is only a guess ("text text text")
     * @param known    every chat ULTRON can still answer, read or not
     * @param justRead the chat ULTRON read or announced in the last few minutes
     */
    fun choose(
        who: String?,
        text: String,
        loose: String?,
        known: List<Conversation>,
        justRead: Conversation?
    ): Choice? {
        if (who == null || Conversations.isPronoun(who)) {
            return named(text, known)?.let { (chat, rest) -> Choice(chat, clean(rest), chat.key == justRead?.key) }
                ?: unnamed(text, known, justRead)
        }
        return Conversations.find(who, known)?.let { Choice(it, clean(text), sendNow = it.key == justRead?.key) }
            // "text" in "text text text" was never a name: the whole thing is the answer.
            ?: loose?.takeIf { wordsNotName(who, it) }?.let { unnamed(it, known, justRead) }
    }

    /** Whether a reply that matched no chat should go to e-mail instead. */
    fun isMailFor(who: String?, loose: String?): Boolean =
        who != null && !Conversations.isPronoun(who) && !wordsNotName(who, loose)

    private fun unnamed(text: String, known: List<Conversation>, justRead: Conversation?): Choice? =
        justRead?.let { Choice(it, clean(text), sendNow = true) }
            ?: Conversations.find(null, known)?.let { Choice(it, clean(text), sendNow = false) }

    /**
     * Speech recognition writes names with a capital and ordinary words
     * without: "text text text text text" names someone, "text text
     * text" does not.
     */
    private fun wordsNotName(who: String, loose: String?): Boolean =
        loose != null && who.firstOrNull()?.isLowerCase() == true

    /** "text text" → (text, "text"); null when the first word names no chat. */
    private fun named(text: String, known: List<Conversation>): Pair<Conversation, String>? {
        val words = text.trim().split(Regex("\\s+"))
        val first = words.first().trim(',', ':', '.', '—', '-')
        if (first.isEmpty()) return null
        val best = ContactMatcher.ranked(first, known) { it.title }.firstOrNull()
            ?.takeIf { it.second >= NAME_SCORE } ?: return null
        return best.first to words.drop(1).joinToString(" ").trimStart(',', ':', '—', '-', ' ')
    }

    /** What goes into the chat: no "text" in front, no full stop dictated at the end. */
    fun clean(text: String): String =
        text.trim().replace(leadingThat, "").trim().removeSuffix(".").trim()
}
