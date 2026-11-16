package com.ultron.ai.core

/**
 * What Ultron says the moment he is called, before the microphone opens.
 * Supports English and Tanglish responses.
 */
class Acknowledgements(private val random: () -> Double = Math::random) {

    companion object {
        private val ENGLISH = listOf(
            "At your service, sir",
            "Yes, sir?",
            "How can I help?",
            "Listening",
            "Ultron online, sir",
            "Standing by, sir"
        )
        private val TANGLISH = listOf(
            "Solleenga sir?",
            "Yes sir, enna pannanum?",
            "Ultron online sir, sollunga",
            "At your service sir, sollunga"
        )

        /** Every greeting, for synthesising them before they are needed. */
        val all: List<String> get() = ENGLISH + TANGLISH
    }

    private var lastSpoken: String? = null

    /**
     * Picks a greeting in the language the user used. Never returns the same
     * one twice running.
     */
    fun next(language: WakePhrases.Language?): String {
        val pool = if (language == WakePhrases.Language.TANGLISH) TANGLISH else ENGLISH
        val choices = pool.filter { it != lastSpoken }.ifEmpty { pool }
        val picked = choices[(random() * choices.size).toInt().coerceIn(0, choices.lastIndex)]
        lastSpoken = picked
        return picked
    }
}
