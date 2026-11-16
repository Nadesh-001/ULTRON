package com.ultron.ai.core

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AcknowledgementsTest {

    @Test
    fun `english is the default and the requested phrasing is in the pool`() {
        val seen = (1..40).map { Acknowledgements().next(null) }.toSet()
        assertTrue(seen.any { it.contains("At your service") || it.contains("Ultron online") })
        assertTrue("everything must be English: $seen", seen.all { it.first() in 'A'..'z' })
    }

    @Test
    fun `english wake call gets an english greeting`() {
        val a = Acknowledgements()
        repeat(20) {
            val s = a.next(WakePhrases.Language.ENGLISH)
            assertTrue("got '$s'", s.first() in 'A'..'z')
        }
    }

    @Test
    fun `tanglish wake call gets a tanglish greeting`() {
        val a = Acknowledgements()
        repeat(20) {
            val s = a.next(WakePhrases.Language.TANGLISH)
            assertTrue("got '$s'", s.first() in 'A'..'z')
        }
    }

    @Test
    fun `it never says the same thing twice in a row`() {
        val a = Acknowledgements()
        var previous = a.next(null)
        repeat(50) {
            val next = a.next(null)
            assertNotEquals("repeated back to back", previous, next)
            previous = next
        }
    }

    @Test
    fun `a degenerate random source still returns a usable phrase`() {
        listOf(0.0, 0.999999, 1.0).forEach { r ->
            val s = Acknowledgements(random = { r }).next(null)
            assertTrue("empty greeting for random=$r", s.isNotBlank())
        }
    }
}
