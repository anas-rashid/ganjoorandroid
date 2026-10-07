package com.ganjoor.android

import com.ganjoor.android.ui.columnUrls
import org.junit.Assert.assertEquals
import org.junit.Test

class ColumnUrlsTest {

    /** A poem four levels down gets a column for each category above it, but none of its own. */
    @Test
    fun `a poem lists every category above it`() {
        assertEquals(
            listOf("/saadi", "/saadi/golestan", "/saadi/golestan/bab1"),
            columnUrls("/saadi/golestan/bab1/sh1", isPoem = true),
        )
    }

    /** An open category is listed in its own column, with nothing in it chosen yet. */
    @Test
    fun `a category gets its own column`() {
        assertEquals(listOf("/hafez", "/hafez/ghazal"), columnUrls("/hafez/ghazal", isPoem = false))
    }

    @Test
    fun `a poet is one column`() {
        assertEquals(listOf("/hafez"), columnUrls("/hafez/", isPoem = false))
    }

    /** A poem straight under a poet has only the poet's column beside it. */
    @Test
    fun `a poem at a poet's root`() {
        assertEquals(listOf("/khayyam"), columnUrls("/khayyam/sh1", isPoem = true))
    }
}
