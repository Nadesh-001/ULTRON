package com.ultron.ai.core

/**
 * Guards what the extractor is allowed to write into long-term memory.
 *
 * The failure this exists for: asking "text text text?" made ULTRON store
 * `fact | name | text text?`, and from then on she claimed to remember the
 * user's name — quoting the question back as if it had been an answer. A
 * question is the user asking for a fact, never stating one, and the prompt
 * alone did not reliably keep the model from confusing the two.
 */
object FactFilter {

    private val INTERROGATIVES = listOf(
        "what", "who", "where", "when", "why", "how", "which", "whose", "enna", "yaaru", "enga", "eppo"
    )

    /** Words that mean the model is guessing rather than reporting. */
    private val HEDGES = listOf(
        "unknown", "maybe", "perhaps", "probably", "not sure", "n/a", "none", "null", "theriyadhu"
    )

    /**
     * True when [value] is worth saving as a fact about the user under [key].
     */
    fun isStorable(key: String, value: String): Boolean {
        val v = value.trim().lowercase()
        val k = key.trim().lowercase()

        if (v.length < 2 || k.isBlank()) return false
        if (v.contains('?')) return false

        val words = v.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return false
        if (words.first() in INTERROGATIVES) return false
        if (HEDGES.any { v == it || v.startsWith("$it ") }) return false

        // "name: name" carries nothing; it usually means the extractor echoed
        // the key because it had no answer.
        if (v == k) return false

        return true
    }
}
