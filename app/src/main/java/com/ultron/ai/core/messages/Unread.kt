package com.ultron.ai.core.messages

import com.ultron.ai.core.ContactMatcher

/**
 * The words of "text text?": who wrote, whose to read first, and
 * how one chat is told. Pure, so it is tested without a phone.
 */
object Unread {

    /** More than this many messages, or this many characters, and a chat is retold, not read. */
    private const val LONG_MESSAGES = 2
    private const val LONG_CHARS = 200

    /** A name said in answer to "text text?" is at most this many words. */
    private const val MAX_PICK_WORDS = 3
    /** Said in answer to a short list, a near-spelling is still clearly one of them. */
    private const val PICK_SCORE = ContactMatcher.FUZZY

    /** Words around a name in "text text text text". */
    private val PICK_FILLER = setOf(
        "давай", "сначала", "начни", "начнём", "начнем", "с", "от", "прочитай", "прочти", "читай", "его", "её",
        "ее", "сообщение", "сообщения", "пожалуйста", "first", "from", "read", "please", "start", "with"
    )

    /** "text text: text, text text text. text text text?" — newest first. */
    fun whoWrote(chats: List<Conversation>, russian: Boolean): String {
        val names = names(chats, russian)
        return "Messages from $names. Whose first?"
    }

    /** Said after one chat is read, while others wait. */
    fun stillUnread(chats: List<Conversation>, russian: Boolean): String =
        "Still unread: ${names(chats, false)}."

    /** One chat, as read out: word for word, or [summary] when it was long. */
    fun tell(chat: Conversation, summary: String?, russian: Boolean): String {
        if (summary == null) return Conversations.retell(listOf(chat), russian)
        val where = "on ${chat.app}"
        val brief = summary.trim().trimEnd('.')
        return "${chat.title} $where. In short: $brief."
    }

    /** Too much to read word for word. */
    fun isLong(chat: Conversation): Boolean =
        chat.messages.size > LONG_MESSAGES || chat.messages.sumOf { it.text.length } > LONG_CHARS

    /** The chat a name means, said on its own in answer to "text text?"; null if it is no name. */
    fun picked(text: String, chats: List<Conversation>): Conversation? {
        val words = text.lowercase().split(Regex("""[^\p{L}\p{N}]+"""))
            .filter { it.isNotBlank() && it !in PICK_FILLER }
        if (words.isEmpty() || words.size > MAX_PICK_WORDS) return null
        return ContactMatcher.ranked(words.joinToString(" "), chats) { it.title }.firstOrNull()
            ?.takeIf { it.second >= PICK_SCORE }?.first
    }

    private fun names(chats: List<Conversation>, russian: Boolean): String {
        val names = chats.sortedByDescending { it.updatedAt }.map { it.title }
        if (names.size == 1) return names.single()
        return names.dropLast(1).joinToString(", ") + (" and ") + names.last()
    }
}
