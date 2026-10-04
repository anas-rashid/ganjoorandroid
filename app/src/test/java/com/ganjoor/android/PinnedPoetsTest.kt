package com.ganjoor.android

import com.ganjoor.android.data.PoetRef
import com.ganjoor.android.ui.pinnedFirst
import org.junit.Assert.assertEquals
import org.junit.Test

class PinnedPoetsTest {

    private fun poets(vararg slugs: String) =
        slugs.mapIndexed { i, slug -> PoetRef(id = i, nickname = slug, fullUrl = "/$slug") }

    private val all = poets("hafez", "moulavi", "saadi", "ferdousi")

    /** The default view must never be empty, which is the whole reason it can be the default. */
    @Test
    fun `no pins leaves ganjoor's order untouched`() {
        assertEquals(all, pinnedFirst(all, emptyList()))
    }

    @Test
    fun `pinned poets come first, in the order they were pinned`() {
        val order = pinnedFirst(all, listOf("/saadi", "/hafez")).map { it.nickname }
        assertEquals(listOf("saadi", "hafez", "moulavi", "ferdousi"), order)
    }

    /** A pin for a poet who isn't in the list — filtered out by a search — must not crash. */
    @Test
    fun `a pin with no matching poet is ignored`() {
        val order = pinnedFirst(poets("hafez"), listOf("/saadi", "/hafez")).map { it.nickname }
        assertEquals(listOf("hafez"), order)
    }

    @Test
    fun `the unpinned keep their relative order`() {
        val order = pinnedFirst(all, listOf("/ferdousi")).map { it.nickname }
        assertEquals(listOf("ferdousi", "hafez", "moulavi", "saadi"), order)
    }
}
