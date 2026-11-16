package com.ultron.ai.core

/** A phone-book entry reduced to what matching needs. */
data class Contact(val name: String, val number: String)

/**
 * Resolves a spoken name to a phone-book entry.
 *
 * Two things make this non-trivial in practice:
 *  - Russian declension: you say "text *text*" but the contact is saved
 *    as "text". Comparing the raw strings never matches.
 *  - Relationship words: you say "text" but the contact may be saved as
 *    "text", "Dad", or "text".
 *
 * Kept free of Android APIs so the matching rules can be unit-tested.
 */
object ContactMatcher {

    /**
     * Words that mean the same person. Any term in a group is tried when the
     * user says any other term from that group.
     */
    private val RELATIONSHIP_GROUPS: List<Set<String>> = listOf(
        setOf(
            "dad", "daddy", "father", "papa", "pops", "appa", "thatha"
        ),
        setOf(
            "mom", "mum", "mommy", "mother", "mama", "madre", "amma", "paati"
        ),
        setOf("brother", "bro", "annan", "thambi"),
        setOf("sister", "sis", "akka", "thangachi"),
        setOf("wife", "wifey", "manaivi"),
        setOf("husband", "hubby", "purushan"),
        setOf("son", "makan"),
        setOf("daughter", "makal"),
        setOf("grandma", "grandmother", "paati"),
        setOf("grandpa", "grandfather", "thatha"),
        setOf("friend", "buddy", "machan", "nanba", "dost")
    )

    /**
     * What is left of a stem after a pet-name suffix.
     */
    private val DIMINUTIVE_TAILS = setOf(
        "ul", "ulk", "och", "ochk", "ichk", "ushk", "ik", "it", "is", "onk", "m", "d", "y", "ie", ""
    )

    /** Trailing letters that vowel endings are built from. */
    private const val CASE_ENDINGS = "aeiouy"

    /**
     * Multi-letter Latin spellings collapsed to standard forms.
     */
    private val LATIN_DIGRAPHS: List<Pair<String, String>> = listOf(
        "sch" to "sh", "kh" to "h", "zh" to "j", "ts" to "c", "ck" to "k"
    )

    /**
     * Brings a name to one comparable form.
     */
    fun canonical(word: String): String {
        val lowered = word.lowercase().trim()
        return LATIN_DIGRAPHS.fold(lowered) { acc, (from, to) -> acc.replace(from, to) }
    }

    /**
     * Reduces a word to a script-independent stem, so different cases of the
     * same name collapse together: "text"/"text"/"text" and "papa" all become
     * the same thing.
     */
    fun stem(word: String): String {
        var t = canonical(word)
        while (t.length > 2 && t.last() in CASE_ENDINGS) {
            t = t.dropLast(1)
        }
        return t
    }

    /**
     * Picks the contact that best matches [query], or null if nothing is a
     * credible match. Never guesses wildly — a wrong number is worse than
     * admitting the contact wasn't found.
     */
    fun findBest(query: String, contacts: List<Contact>): Contact? =
        ranked(query, contacts) { it.name }.firstOrNull()?.first

    /**
     * Every credible match for [query], best first, with its score — so a
     * caller can tell a clear winner from a tie.
     */
    fun <T> ranked(query: String, items: List<T>, name: (T) -> String): List<Pair<T, Int>> {
        val cleaned = query.lowercase().trim().trim('.', ',', '!', '?')
        if (cleaned.isBlank() || items.isEmpty()) return emptyList()

        val queryStems = expandToStems(cleaned)
        if (queryStems.isEmpty()) return emptyList()

        return items
            .mapNotNull { item ->
                val score = score(queryStems, cleaned, name(item))
                if (score > 0) item to score else null
            }
            .sortedByDescending { it.second }
    }

    /** The query's own stems plus stems of any relationship synonyms. */
    private fun expandToStems(query: String): Set<String> {
        val words = query.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }
        val stems = words.map { stem(it) }.toMutableSet()

        for (word in words) {
            val wordStem = stem(word)
            for (group in RELATIONSHIP_GROUPS) {
                val belongs = group.any { stem(it) == wordStem }
                if (belongs) stems += group.map { stem(it) }
            }
        }
        return stems.filter { it.isNotBlank() }.toSet()
    }

    /** Shorter stems collide with unrelated names when matched as prefixes. */
    private const val MIN_PREFIX = 3

    // How sure a match is; only the order matters.
    private const val EXACT = 100
    private const val WHOLE_NAME = 90

    /**
     * Every word said matches a word of this one name, some only spelled
     * alike: "text text" for "Firdavs Gettysburg". Name and surname
     * both fitting is not a guess, unlike one look-alike word ([FUZZY]),
     * which "Firdavs Yakubov" matches too.
     */
    private const val FULL_NAME_SPELLED = 85
    private const val PET_FORM = 80
    private const val ONE_WORD = 70
    private const val PREFIX = 50

    /**
     * Spelled a letter or two differently: "text" for text, "text"
     * for text — how speech recognition gets names it doesn't know. Ranked
     * below every real match, and a caller about to act on it alone should
     * ask first ("text" is one letter from "text").
     */
    const val FUZZY = 60

    /** Names shorter than this are too short to be told apart from a typo. */
    private const val FUZZY_MIN = 5
    private const val FUZZY_LONG = 8

    /** Higher is better; 0 means "not a match". */
    private fun score(queryStems: Set<String>, rawQuery: String, name: String): Int {
        val lowered = name.lowercase().trim()
        // Latin "x" is "text" in Alex but "text" in the Uzbek Shoxrux; both are tried.
        val readings =
            if ('x' in lowered) listOf(lowered.replace("x", "h"), lowered.replace("x", "ks")) else listOf(lowered)
        return readings.maxOf { scoreAs(queryStems, rawQuery, it) }
    }

    private fun scoreAs(queryStems: Set<String>, rawQuery: String, contactName: String): Int {
        val contactStems = contactName.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }.map { stem(it) }
        val prefixes = queryStems.filter { it.length >= MIN_PREFIX }

        // A pet form of what was said: "text", "Mamito", "text" — ranked
        // above a bare prefix so "text" prefers "text" to "text".
        val petForm = prefixes.any { q ->
            contactStems.any { it.startsWith(q) && it.removePrefix(q) in DIMINUTIVE_TAILS }
        }
        // A bare prefix catches nicknames and truncated forms ("text"/"text").
        val prefix = prefixes.any { q -> contactStems.any { it.startsWith(q) || q.startsWith(it) } }

        return when {
            contactName.isBlank() -> 0
            // Compared canonically so script differences don't block an exact hit.
            canonical(contactName) == canonical(rawQuery) -> EXACT
            // The whole contact name is one of the query's stems, e.g. "text".
            contactStems.size == 1 && contactStems.single() in queryStems -> WHOLE_NAME
            // Split or joined differently: "text text" for text.
            joined(contactName).let { it.length >= FUZZY_MIN && it == joined(rawQuery) } -> WHOLE_NAME
            everyWordFits(rawQuery, contactStems) -> FULL_NAME_SPELLED
            petForm -> PET_FORM
            // One word of a multi-word contact matches, e.g. "text text".
            contactStems.any { it in queryStems } -> ONE_WORD
            queryStems.any { q -> contactStems.any { c -> close(q, c) } } -> FUZZY
            prefix -> PREFIX
            else -> 0
        }
    }

    private fun joined(name: String): String = stem(name.filter { it.isLetter() })

    /** Two or more words said, each matching its own word of the name, exactly or one or two letters off. */
    private fun everyWordFits(rawQuery: String, contactStems: List<String>): Boolean {
        val said = rawQuery.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }.map { stem(it) }
        if (said.size < 2 || said.size > contactStems.size) return false
        val free = contactStems.toMutableList()
        return said.all { word ->
            val i = free.indexOfFirst { it == word || close(word, it) }
            if (i >= 0) free.removeAt(i)
            i >= 0
        }
    }

    /** One letter off for an ordinary name, two for a long one. */
    private fun close(a: String, b: String): Boolean {
        val shorter = minOf(a.length, b.length)
        if (shorter < FUZZY_MIN) return false
        val allowed = if (shorter >= FUZZY_LONG) 2 else 1
        return kotlin.math.abs(a.length - b.length) <= allowed &&
            com.ultron.ai.core.apps.Spelling.distance(a, b) <= allowed
    }

}
