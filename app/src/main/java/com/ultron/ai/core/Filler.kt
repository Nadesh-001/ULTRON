package com.ultron.ai.core

/**
 * The words people put in front of a request in conversation: "text, text
 * text text text text?", "text, text text text text". Left in, the
 * request was split at the comma, "text" matched nothing, and the whole
 * phrase went to the model instead of the command it was.
 */
object Filler {

    private val leading = Regex(
        """^(?:ok|okay|alright|thanks|great|seri|nandri)""" +
            """[,.!]?\s+(?:(?:so|and|but|then|appram)\s+)?(?:(?:now|ippo)\s+)?""",
        RegexOption.IGNORE_CASE
    )

    /** [text] without its lead-in; unchanged when nothing would be left. */
    fun strip(text: String): String {
        val rest = leading.replaceFirst(text.trim(), "").trim()
        return if (rest.isEmpty()) text.trim() else rest.replaceFirstChar { it.uppercase() }
    }
}
