package com.ganjoor.android

import com.ganjoor.android.ui.webUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class ShareTest {

    @Test
    fun `builds a ganjoor link from a stored path`() {
        assertEquals("https://ganjoor.net/hafez/ghazal/sh1", webUrl("/hafez/ghazal/sh1"))
    }

    /** Paths arrive with a leading slash and sometimes a trailing one; neither may double up. */
    @Test
    fun `trims slashes rather than doubling them`() {
        assertEquals("https://ganjoor.net/saadi/golestan", webUrl("/saadi/golestan/"))
        assertEquals("https://ganjoor.net/saadi/golestan", webUrl("saadi/golestan"))
    }

    @Test
    fun `leaves an absolute url alone`() {
        assertEquals("https://ganjoor.net/x", webUrl("https://ganjoor.net/x"))
    }
}
