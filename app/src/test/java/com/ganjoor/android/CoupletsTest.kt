package com.ganjoor.android

import com.ganjoor.android.data.Crumb
import com.ganjoor.android.data.Verse
import com.ganjoor.android.data.breadcrumbs
import com.ganjoor.android.data.parentUrl
import com.ganjoor.android.data.couplets
import org.junit.Assert.assertEquals
import org.junit.Test

class CoupletsTest {
    private fun verse(order: Int, position: String) =
        Verse(vOrder = order, position = position, text = "v$order")

    @Test
    fun `right and left hemistichs pair into one couplet`() {
        val grouped = listOf(
            verse(1, Verse.RIGHT),
            verse(2, Verse.LEFT),
            verse(3, Verse.RIGHT),
            verse(4, Verse.LEFT),
        ).couplets()

        assertEquals(2, grouped.size)
        assertEquals(listOf("v1", "v2"), grouped[0].map { it.text })
        assertEquals(listOf("v3", "v4"), grouped[1].map { it.text })
    }

    @Test
    fun `out of order verses are sorted before grouping`() {
        val grouped = listOf(verse(2, Verse.LEFT), verse(1, Verse.RIGHT)).couplets()

        assertEquals(1, grouped.size)
        assertEquals(listOf("v1", "v2"), grouped[0].map { it.text })
    }

    @Test
    fun `prose and single verses each stand alone`() {
        val grouped = listOf(
            verse(1, "Paragraph"),
            verse(2, "Single"),
            verse(3, Verse.RIGHT),
            verse(4, Verse.LEFT),
        ).couplets()

        assertEquals(listOf(1, 1, 2), grouped.map { it.size })
    }

    @Test
    fun `a left hemistich without its right half is not swallowed by the previous couplet`() {
        val grouped = listOf(
            verse(1, Verse.RIGHT),
            verse(2, Verse.LEFT),
            verse(3, Verse.LEFT),
        ).couplets()

        assertEquals(listOf(2, 1), grouped.map { it.size })
    }

    @Test
    fun `centered verses pair with each other, not with hemistichs`() {
        val grouped = listOf(
            verse(1, Verse.CENTERED_1),
            verse(2, Verse.CENTERED_2),
            verse(3, Verse.RIGHT),
            verse(4, Verse.CENTERED_2),
        ).couplets()

        assertEquals(listOf(2, 1, 1), grouped.map { it.size })
    }
}

class BreadcrumbsTest {
    @Test
    fun `every ancestor is linked and the poem itself is not`() {
        val crumbs = breadcrumbs("حافظ » غزلیات » غزل شمارهٔ ۱", "/hafez/ghazal/sh1")

        assertEquals(listOf("حافظ", "غزلیات", "غزل شمارهٔ ۱"), crumbs.map { it.label })
        assertEquals(listOf("/hafez", "/hafez/ghazal", null), crumbs.map { it.url })
    }

    @Test
    fun `deeply nested books keep the whole trail`() {
        val crumbs = breadcrumbs(
            "مولانا » مثنوی معنوی » دفتر اول » بخش ۱",
            "/moulavi/masnavi/daftar1/sh1",
        )

        assertEquals(4, crumbs.size)
        assertEquals("/moulavi/masnavi/daftar1", crumbs[2].url)
        assertEquals(null, crumbs.last().url)
    }

    @Test
    fun `a title and url that disagree produce nothing, so the caller falls back`() {
        assertEquals(emptyList<Crumb>(), breadcrumbs("حافظ » غزل ۱", "/hafez/ghazal/sh1"))
        assertEquals(emptyList<Crumb>(), breadcrumbs("", "/hafez/ghazal/sh1"))
    }

    @Test
    fun `a poem directly under a poet still links the poet`() {
        val crumbs = breadcrumbs("حافظ » ساقی‌نامه", "/hafez/saghinameh")

        assertEquals(listOf("/hafez", null), crumbs.map { it.url })
    }
}

class ParentUrlTest {
    @Test
    fun `each level climbs to the one above it`() {
        assertEquals("/hafez/ghazal", parentUrl("/hafez/ghazal/sh1"))
        assertEquals("/hafez", parentUrl("/hafez/ghazal"))
        assertEquals("/moulavi/masnavi", parentUrl("/moulavi/masnavi/daftar1"))
    }

    @Test
    fun `a poet root has no parent, so the caller sends you home`() {
        assertEquals(null, parentUrl("/hafez"))
        assertEquals(null, parentUrl("hafez"))
        assertEquals(null, parentUrl(""))
    }

    @Test
    fun `trailing slashes don't invent a level`() {
        assertEquals("/hafez", parentUrl("/hafez/ghazal/"))
    }
}
