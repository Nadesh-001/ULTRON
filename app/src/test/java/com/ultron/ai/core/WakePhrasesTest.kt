package com.ultron.ai.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WakePhrasesTest {

    private fun wakes(text: String) = WakePhrases.isWakeCall(text)

    @Test
    fun `ultron name wakes the assistant`() {
        listOf("ultron", "Ultron", "ULTRON", "ultron!").forEach {
            assertTrue("'$it' should wake", wakes(it))
        }
    }

    @Test
    fun `wake word followed by a command still wakes`() {
        assertTrue(wakes("ultron open camera"))
        assertTrue(wakes("ultron what is the weather"))
        assertTrue(wakes("ultron camera open pannu"))
    }

    @Test
    fun `attention prefix is accepted`() {
        assertTrue(wakes("ok ultron"))
        assertTrue(wakes("hey ultron"))
        assertTrue(wakes("hi ultron turn on flashlight"))
        assertTrue(wakes("dei ultron"))
        assertTrue(wakes("vanakkam ultron"))
    }

    @Test
    fun `unrelated speech does not wake`() {
        assertFalse(wakes("hello how are you"))
        assertFalse(wakes("what is the weather today"))
        assertFalse(wakes("open camera"))
        assertFalse(wakes(""))
        assertFalse(wakes("   "))
    }

    @Test
    fun `language is reported so the reply can match`() {
        assertEquals(WakePhrases.Language.ENGLISH, WakePhrases.detectLanguage("ultron open camera"))
        assertEquals(WakePhrases.Language.ENGLISH, WakePhrases.detectLanguage("hey ultron"))
        assertEquals(WakePhrases.Language.TANGLISH, WakePhrases.detectLanguage("dei ultron open pannu"))
        assertEquals(WakePhrases.Language.TANGLISH, WakePhrases.detectLanguage("vanakkam ultron"))
    }

    @Test
    fun `language is null when it was not a wake call`() {
        assertNull(WakePhrases.detectLanguage("meeting tomorrow"))
        assertNull(WakePhrases.detectLanguage("hello there"))
    }

    @Test
    fun `an attention word alone does not wake`() {
        assertFalse(wakes("ok"))
        assertFalse(wakes("hey"))
        assertFalse(wakes("hello"))
    }

    @Test
    fun `grammar vocabulary covers ultron names and prefixes`() {
        val vocab = WakePhrases.grammarVocabulary()
        assertTrue("Ultron name missing from grammar", vocab.contains("ultron"))
        assertTrue("hey prefix missing from grammar", vocab.contains("hey"))
        assertTrue("ok prefix missing from grammar", vocab.contains("ok"))
    }

    @Test
    fun `out-of-vocabulary tokens do not wake`() {
        assertFalse(wakes("[unk]"))
        assertFalse(wakes("[unk] [unk] ultron"))
    }
}
