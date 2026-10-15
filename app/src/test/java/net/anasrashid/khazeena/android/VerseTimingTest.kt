package net.anasrashid.khazeena.android

import net.anasrashid.khazeena.android.data.VerseTiming
import net.anasrashid.khazeena.android.data.verseAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VerseTimingTest {
    // Hafez, ghazal 2, as api.ganjoor.net times Farahandouz's reading: title and first line at 0.
    private val timings = listOf(
        VerseTiming(0, 0),
        VerseTiming(1, 0),
        VerseTiming(2, 7386),
        VerseTiming(3, 14736),
        VerseTiming(4, 18900),
    )

    @Test
    fun `the first line, not the title, is read from the start`() {
        assertEquals(1, timings.verseAt(0))
    }

    @Test
    fun `a verse stays current until the next one starts`() {
        assertEquals(1, timings.verseAt(7385))
        assertEquals(2, timings.verseAt(7386))
        assertEquals(3, timings.verseAt(18899))
        assertEquals(4, timings.verseAt(60_000))
    }

    @Test
    fun `nothing is current before the reading reaches its first verse`() {
        assertNull(listOf(VerseTiming(1, 2000)).verseAt(500))
        assertNull(emptyList<VerseTiming>().verseAt(500))
    }
}
