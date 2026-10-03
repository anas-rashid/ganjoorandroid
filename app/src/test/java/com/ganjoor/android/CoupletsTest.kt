package com.ganjoor.android

import com.ganjoor.android.data.Verse
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
