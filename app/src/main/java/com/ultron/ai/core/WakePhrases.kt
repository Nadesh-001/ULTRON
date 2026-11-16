package com.ultron.ai.core

/**
 * Decides whether a chunk of recognised speech is the user addressing Ultron.
 * Supports English and Tanglish wake phrases.
 */
object WakePhrases {

    /** Accepted variants for Ultron. */
    private val ULTRON_NAMES = listOf(
        "ultron", "alltron", "altron", "oltron", "ul-tron", "ultronn"
    )

    /** Optional politeness / attention prefix before the name: "hey ultron", "ok ultron", "dei ultron". */
    private val ATTENTION_PREFIXES = listOf(
        "hey", "ok", "okay", "hi", "yo", "hello", "dei", "vanakkam"
    )

    /** Every accepted name, longest first. */
    private val ALL_NAMES: List<String> = ULTRON_NAMES.sortedByDescending { it.length }

    /**
     * Words handed to Vosk as a decoding grammar.
     */
    fun grammarVocabulary(): List<String> =
        (ALL_NAMES + ATTENTION_PREFIXES).map { it.lowercase() }.distinct()

    /**
     * True when [heard] is the user calling Ultron.
     */
    fun isWakeCall(heard: String): Boolean = nameIndex(normalize(heard)) != null

    /** Which of the decoder's [words] is the name, or null if they are not a wake call. */
    fun nameIndexIn(words: List<String>): Int? = nameIndex(words.map { it.lowercase().trim() })

    private fun nameIndex(words: List<String>): Int? {
        if (words.isEmpty()) return null
        if (isName(words[0])) return 0
        if (words.size >= 2 && words[0] in ATTENTION_PREFIXES && isName(words[1])) return 1
        return null
    }

    private fun isName(word: String): Boolean = ALL_NAMES.any { word.startsWith(it) }

    /**
     * Which language/mode the user addressed Ultron in, or null if it wasn't a wake call.
     */
    fun detectLanguage(heard: String): Language? {
        val words = normalize(heard)
        if (nameIndex(words) == null) return null
        val tanglishMarkers = setOf("dei", "vanakkam", "sollu", "solleenga", "enna")
        return if (words.any { it in tanglishMarkers }) Language.TANGLISH else Language.ENGLISH
    }

    enum class Language { ENGLISH, TANGLISH }

    private val leadingCall = Regex(
        "^\\s*(?:(?:hey|ok|okay|hi|yo|hello|dei|vanakkam)[\\s,!.]+)?" +
            "(?:ultron|altron|alltron|oltron)[\\s,!.:;—–-]*",
        RegexOption.IGNORE_CASE
    )

    /**
     * [transcript] without the call in front: "Ultron, play music" → "play music".
     */
    fun stripCall(transcript: String): String = transcript.replaceFirst(leadingCall, "").trim()

    private fun normalize(heard: String): List<String> =
        heard.lowercase()
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.isNotBlank() }
}
