package com.ultron.ai.core.modes

import com.ultron.ai.core.SpokenText

/**
 * Whether two ways of saying a mode's name are the same name.
 */
object ModeNames {

    private const val MIN_STEM = 3
    private const val ENDING = 2

    private val stopWords = setOf("the", "my", "please", "konjam", "pa")

    /**
     * Whether [spoken] names [name] (or one of its aliases): every word of the
     * name matches a spoken word by stem, in order, with at most one spoken
     * word to spare.
     */
    fun same(spoken: String, name: String, spare: Int = 1): Boolean {
        val said = words(spoken)
        val wanted = words(name)
        if (wanted.isEmpty() || said.size - wanted.size !in 0..spare) return false
        var i = 0
        for (w in said) if (i < wanted.size && sameStem(w, wanted[i])) i++
        return i == wanted.size
    }

    private fun words(s: String) = SpokenText.normalise(s).split(' ').filter { it.isNotBlank() && it !in stopWords }

    fun overlap(phrase: String, text: String): Double {
        val wanted = words(phrase)
        val have = words(text)
        if (wanted.isEmpty()) return 0.0
        return wanted.count { w -> have.any { sameStem(w, it) } }.toDouble() / wanted.size
    }

    private fun sameStem(a: String, b: String): Boolean {
        if (a == b) return true
        val common = a.zip(b).takeWhile { (x, y) -> x == y }.size
        return common >= MIN_STEM && (a.length - common <= ENDING) && (b.length - common <= ENDING)
    }
}
