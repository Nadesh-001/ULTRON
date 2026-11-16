package com.ultron.ai.core

/**
 * Which music app the user meant, by name.
 *
 * Kept separate from the media control itself so the naming can be tested
 * without a device — the package list is the part that goes stale, and the
 * part most likely to be typed wrong.
 */
object MusicApps {

    data class Player(val packageName: String, val label: String, val spoken: List<String>)

    val KNOWN = listOf(
        Player(
            "com.spotify.music", "Spotify",
            listOf("spotify")
        ),
        Player(
            "com.sec.android.app.music", "Samsung Music",
            listOf("samsung music", "samsung")
        ),
        Player(
            "com.google.android.apps.youtube.music", "YouTube Music",
            listOf("youtube music", "yt music")
        ),
        Player("com.soundcloud.android", "SoundCloud", listOf("soundcloud")),
        Player("deezer.android.app", "Deezer", listOf("deezer")),
        Player("com.apple.android.music", "Apple Music", listOf("apple music")),
        Player("com.maxmpz.audioplayer", "Poweramp", listOf("poweramp"))
    )

    /**
     * The player named in [phrase], or null when the user just said "text"
     * and means whatever is already loaded.
     */
    fun match(phrase: String): Player? {
        val text = SpokenText.normalise(phrase)
        return KNOWN
            .flatMap { player -> player.spoken.map { player to SpokenText.normalise(it) } }
            // Longest first: "text text" must beat the bare "text".
            .sortedByDescending { it.second.length }
            .firstOrNull { (_, name) -> " $text ".contains(" $name ") }
            ?.first
    }
}
