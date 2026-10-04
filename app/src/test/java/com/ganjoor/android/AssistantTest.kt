package com.ganjoor.android

import com.ganjoor.android.data.Assistant
import com.ganjoor.android.data.AssistantLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class AssistantTest {

    @Test
    fun `recognises anthropic by its host`() {
        assertTrue(Assistant.isClaude("https://api.anthropic.com/v1"))
        assertFalse(Assistant.isClaude("https://api.openai.com/v1"))
        assertFalse(Assistant.isClaude("http://127.0.0.1:11434/v1"))
    }

    @Test
    fun `reads an openai reply`() {
        val body = """{"choices":[{"message":{"role":"assistant","content":" the rose "}}]}"""
        assertEquals("the rose", Assistant.reply(body, claude = false))
    }

    /** Anthropic answers with content blocks, and the first is not always the text one. */
    @Test
    fun `reads a claude reply past a non-text block`() {
        val body = """{"content":[{"type":"thinking","text":"x"},{"type":"text","text":" the rose "}]}"""
        assertEquals("the rose", Assistant.reply(body, claude = true))
    }

    @Test
    fun `the cache keeps answers apart by prompt, language and text`() {
        val a = Assistant.cacheKey("translate", AssistantLanguage.Urdu, "x")
        assertEquals(a, Assistant.cacheKey("translate", AssistantLanguage.Urdu, "x"))
        assertNotEquals(a, Assistant.cacheKey("explain", AssistantLanguage.Urdu, "x"))
        assertNotEquals(a, Assistant.cacheKey("translate", AssistantLanguage.English, "x"))
        assertNotEquals(a, Assistant.cacheKey("translate", AssistantLanguage.Urdu, "y"))
    }

    /** Scrolling a long poem must not re-ask for something already answered. */
    @Test
    fun `a remembered answer comes back`() {
        val key = Assistant.cacheKey("summary", AssistantLanguage.Urdu, "remembered")
        assertNull(Assistant.cached(key))
        Assistant.remember(key, "the answer")
        assertEquals("the answer", Assistant.cached(key))
    }

    @Test
    fun `the cache does not grow without bound`() {
        repeat(40) { Assistant.remember("bulk-$it", "reply $it") }
        assertNull(Assistant.cached("bulk-0"))
        assertEquals("reply 39", Assistant.cached("bulk-39"))
    }

    @Test
    fun `an empty reply is an error, not an empty answer`() {
        assertThrows(IOException::class.java) { Assistant.reply("""{"choices":[]}""", false) }
        assertThrows(IOException::class.java) { Assistant.reply("""{"content":[]}""", true) }
    }
}
