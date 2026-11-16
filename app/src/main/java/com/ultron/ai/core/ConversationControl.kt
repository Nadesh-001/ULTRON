package com.ultron.ai.core

/**
 * Decides when a spoken conversation is over.
 *
 * The voice loop keeps listening after each reply so you can talk naturally
 * without repeating the wake word. That only works if ending the conversation
 * is reliable — otherwise the overlay keeps popping back up and gets in the
 * way, which is exactly what an exact-match stop word list caused: "bye bye",
 * "ok bye" and "text, text" all slipped through.
 */
object ConversationControl {

    /** Single words that end a conversation when the utterance is short. */
    private val FAREWELL_WORDS = setOf(
        "bye", "goodbye", "stop", "enough", "thanks", "thank", "quit",
        "exit", "cancel", "dismiss", "nevermind", "pothum", "mudinjudhu"
    )

    /** Multi-word endings that close a short utterance. */
    private val FAREWELL_PHRASES = listOf(
        "that's all", "thats all", "that is all", "nothing else", "never mind", "that'll be all",
        "we're done", "i'm done", "all done", "see you later", "pothum ultron"
    )

    /**
     * A goodbye at the end of a longer utterance.
     */
    private val CLOSING_WORDS = setOf("bye", "goodbye", "later", "cya")
    private val CLOSING_PHRASES = listOf(
        "see you", "talk later", "good night", "catch you later"
    )

    /** Words that may stand around a closing word in its clause. */
    private val CLOSING_FILLER = setOf(
        "ok", "okay", "alright", "thanks", "then", "for", "now", "seri", "nandri"
    )

    /** Phrases in a longer sentence are a request that mentions them ("text text text…"). */
    private const val MAX_WORDS_FOR_PHRASE = 6

    private val clauseEnd = Regex("""[,.;!?…—]+""")

    /**
     * Beyond this many words we assume it's a real request that merely
     * contains a polite word, e.g. "thanks, now open YouTube".
     */
    private const val MAX_WORDS_FOR_SHORT_FAREWELL = 4

    /** True when [text] is the user (or the assistant) closing the conversation. */
    fun isFarewell(text: String): Boolean {
        val normalized = text.lowercase().trim()
        if (normalized.isEmpty()) return false

        val words = words(normalized)
        if (words.size <= MAX_WORDS_FOR_PHRASE && FAREWELL_PHRASES.any { normalized.contains(it) }) return true
        if (words.isEmpty() || words.size > MAX_WORDS_FOR_SHORT_FAREWELL) return false

        return words.any { it in FAREWELL_WORDS }
    }

    /**
     * What was said before a closing goodbye — "text text, text text text"
     * → "text text" — or null when [text] doesn't end with one. The
     * request is still carried out; the conversation then ends instead of
     * listening on and catching whatever is said in the room next.
     */
    fun beforeFarewell(text: String): String? {
        var (rest, last) = lastClause(text) ?: return null
        val lastWords = words(last)
        val closes = CLOSING_PHRASES.any { last == it || last.endsWith(" $it") || last.startsWith("$it ") } ||
            (lastWords.any { it in CLOSING_WORDS } && lastWords.all { it in CLOSING_WORDS || it in CLOSING_FILLER })
        if (!closes) return null
        // "…, text text, text": the words leading up to the goodbye go with it.
        while (true) {
            val (before, clause) = lastClause(rest) ?: break
            if (!words(clause).all { it in CLOSING_FILLER }) break
            rest = before
        }
        return rest.takeIf { it.isNotEmpty() }
    }

    /** ("text text", "text text text") from "text text, text text text."; null with no clause break. */
    private fun lastClause(text: String): Pair<String, String>? {
        val trimmed = text.trim().trimEnd { it in ",.;!?…—" || it.isWhitespace() }
        val cut = clauseEnd.findAll(trimmed).lastOrNull() ?: return null
        val before = trimmed.substring(0, cut.range.first).trim()
        return before to trimmed.substring(cut.range.last + 1).lowercase().trim()
    }

    private fun words(text: String): List<String> =
        text.lowercase().split(Regex("[^\\p{L}\\p{N}']+")).filter { it.isNotBlank() }
}
