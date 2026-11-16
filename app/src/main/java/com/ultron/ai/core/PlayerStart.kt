package com.ultron.ai.core

/**
 * What came of starting music, and how to say it.
 *
 * "text text text Spotify" with Spotify closed used to open Spotify and
 * stop there: there was no media session to press play on, so nothing
 * played, and the second attempt (Spotify now running) worked — which looked
 * like Friday not understanding the first time. Starting is now a checked
 * hand-off ([HandOff], [PlaybackStarter]), and only music a player's session
 * reports as playing is called "playing" — as only an alarm actually set is
 * called "set".
 */
object PlayerStart {

    enum class Outcome {
        /** The player's session reports playing. */
        PLAYING,

        /** The player came up but stayed paused even after "play". */
        OPENED_NOT_PLAYING,

        /** Opened, but it published no media session to check or control. */
        OPENED_UNVERIFIED,
        NOT_INSTALLED
    }

    /** [query] is what was asked for ("Believer"), or null for "whatever it plays". */
    fun reply(outcome: Outcome, player: String, russian: Boolean, query: String? = null): String {
        val what = query?.let { "\"$it\" " }.orEmpty()
        return when (outcome) {
            Outcome.PLAYING -> "Playing ${what}on $player"
            Outcome.OPENED_NOT_PLAYING ->
                "$player is open but didn't start — press play"
            Outcome.OPENED_UNVERIFIED ->
                "Opened $player — I can't tell whether it's playing"
            Outcome.NOT_INSTALLED -> "$player isn't installed"
        }
    }
}
