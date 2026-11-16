package com.ultron.ai.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeCallPositionTest {

    @Test
    fun `an unknown token before the name does not wake it`() {
        assertFalse(WakePhrases.isWakeCall("[unk] ultron"))
        assertFalse(WakePhrases.isWakeCall("[unk] [unk] ultron"))
    }

    @Test
    fun `a call opening the utterance works`() {
        assertTrue(WakePhrases.isWakeCall("ultron"))
        assertTrue(WakePhrases.isWakeCall("ultron what is the weather"))
    }

    @Test
    fun `one attention word in front is allowed`() {
        assertTrue(WakePhrases.isWakeCall("ok ultron"))
        assertTrue(WakePhrases.isWakeCall("hey ultron"))
        assertTrue(WakePhrases.isWakeCall("hi ultron"))
        assertTrue(WakePhrases.isWakeCall("dei ultron"))
        assertTrue(WakePhrases.isWakeCall("vanakkam ultron"))
    }

    @Test
    fun `two attention words are not`() {
        assertFalse(WakePhrases.isWakeCall("ok hey ultron"))
    }

    @Test
    fun `nothing at all is not a wake call`() {
        assertFalse(WakePhrases.isWakeCall(""))
        assertFalse(WakePhrases.isWakeCall("   "))
        assertFalse(WakePhrases.isWakeCall("what is the weather today"))
    }

    @Test
    fun `language follows the name that matched`() {
        assertEquals(WakePhrases.Language.ENGLISH, WakePhrases.detectLanguage("ultron"))
        assertEquals(WakePhrases.Language.ENGLISH, WakePhrases.detectLanguage("ok ultron"))
        assertEquals(WakePhrases.Language.TANGLISH, WakePhrases.detectLanguage("dei ultron"))
        assertEquals(WakePhrases.Language.TANGLISH, WakePhrases.detectLanguage("vanakkam ultron"))
        assertEquals(null, WakePhrases.detectLanguage("meeting tomorrow"))
    }
}
