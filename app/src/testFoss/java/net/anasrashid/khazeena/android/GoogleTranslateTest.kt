package net.anasrashid.khazeena.android

import net.anasrashid.khazeena.android.data.GoogleTranslate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class GoogleTranslateTest {
    @Test
    fun `reads the translation out of the Cloud API reply`() {
        val body = """{"data":{"translations":[{"translatedText":" دل کا حال "}]}}"""
        assertEquals("دل کا حال", GoogleTranslate.parse(body))
    }

    @Test(expected = IOException::class)
    fun `an empty reply is an error, not a blank translation`() {
        GoogleTranslate.parse("""{"data":{"translations":[]}}""")
    }

    @Test
    fun `needs a key and the network`() {
        assertTrue(GoogleTranslate.ready("k", offline = false))
        assertFalse(GoogleTranslate.ready("", offline = false))
        assertFalse(GoogleTranslate.ready("k", offline = true))
    }
}
