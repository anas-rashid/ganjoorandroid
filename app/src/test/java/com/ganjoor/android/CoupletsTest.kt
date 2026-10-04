package com.ganjoor.android

import com.ganjoor.android.data.CatEntry
import com.ganjoor.android.data.Category
import com.ganjoor.android.data.Crumb
import com.ganjoor.android.data.PoemRef
import com.ganjoor.android.data.orderedEntries
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

class CategoryOrderTest {
    private fun cat(chapters: List<String>, poems: List<String>) = Category(
        id = 1,
        title = "book",
        childCats = chapters.mapIndexed { i, t -> Category(id = 100 + i, title = t) },
        poems = poems.mapIndexed { i, t -> PoemRef(id = 200 + i, title = t) },
    )

    private fun titles(category: Category) = orderedEntries(category).map {
        when (it) {
            is CatEntry.Chapter -> it.category.title
            is CatEntry.Poem -> it.poem.title
        }
    }

    @Test
    fun `a preface comes before the chapters, as on ganjoor net`() {
        // Golestan: دیباچه then the eight باب
        val golestan = cat(listOf("باب اول", "باب دوم"), listOf("دیباچه"))

        assertEquals(listOf("دیباچه", "باب اول", "باب دوم"), titles(golestan))
    }

    @Test
    fun `other poems stay after the chapters`() {
        // Hafez: مقدّمه, then the collections, then مثنوی and ساقی‌نامه
        val hafez = cat(
            chapters = listOf("غزلیات", "قطعات"),
            poems = listOf("مثنوی (الا ای آهوی وحشی)", "ساقی‌نامه", "مقدّمهٔ جمع‌آورندهٔ دیوان حافظ"),
        )

        assertEquals(
            listOf("مقدّمهٔ جمع‌آورندهٔ دیوان حافظ", "غزلیات", "قطعات", "مثنوی (الا ای آهوی وحشی)", "ساقی‌نامه"),
            titles(hafez),
        )
    }

    @Test
    fun `diacritics in a preface title don't hide it`() {
        // مقدّمه carries a shadda the plain spelling doesn't
        assertEquals(listOf("مقدّمه", "باب اول"), titles(cat(listOf("باب اول"), listOf("مقدّمه"))))
    }

    @Test
    fun `a category with no poems is left exactly as it is`() {
        val masnavi = cat(listOf("دفتر اول", "دفتر دوم", "دفتر سوم"), emptyList())

        assertEquals(listOf("دفتر اول", "دفتر دوم", "دفتر سوم"), titles(masnavi))
    }
}
