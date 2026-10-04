package com.ganjoor.android

import com.ganjoor.android.data.Assistant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `an empty reply is an error, not an empty answer`() {
        assertThrows(IOException::class.java) { Assistant.reply("""{"choices":[]}""", false) }
        assertThrows(IOException::class.java) { Assistant.reply("""{"content":[]}""", true) }
    }
}
