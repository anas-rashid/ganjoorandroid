package com.ganjoor.android

import com.ganjoor.android.data.affixes
import com.ganjoor.android.data.normalise
import com.ganjoor.android.data.wordAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NormaliseTest {
    @Test
    fun `diacritics are dropped, since poems carry them and headwords don't`() {
        assertEquals("الا", normalise("اَلا"))
        assertEquals("ساقی", normalise("سّاقی"))
    }

    @Test
    fun `arabic letter shapes fold to their persian equivalents`() {
        assertEquals("کی", normalise("كي"))
        assertEquals("مکه", normalise("مكة"))
    }

    @Test
    fun `alef with madda survives, so آتش stays findable`() {
        assertEquals("آتش", normalise("آتش"))
    }

    @Test
    fun `zero-width non-joiners go unless asked for`() {
        assertEquals("بیروزی", normalise("بی‌روزی"))
        assertEquals("بی‌روزی", normalise("بی‌روزی", keepZwnj = true))
    }
}

class AffixTest {
    @Test
    fun `one affix is stripped at a time`() {
        assertTrue("دلها" .let { affixes(it) }.contains("دل"))
        assertTrue(affixes("میرود").contains("رود"))
    }

    @Test
    fun `a word that is barely longer than its affix is left alone`() {
        assertEquals(emptyList<String>(), affixes("ها"))
    }
}

class WordAtTest {
    private val line = "اگر آن ترک شیرازی به دست آرد دل ما را"

    @Test
    fun `a tap inside a word returns that whole word`() {
        assertEquals("شیرازی", wordAt(line, line.indexOf("شیرازی") + 2))
        assertEquals("اگر", wordAt(line, 0))
    }

    @Test
    fun `a tap on a space returns nothing, so the caller can do something else`() {
        assertNull(wordAt(line, line.indexOf(' ')))
    }

    @Test
    fun `a compound joined by a zero-width non-joiner counts as one word`() {
        assertEquals("بی‌خبر", wordAt("سالک بی‌خبر نبود", 8))
    }

    @Test
    fun `single letters and empty text are rejected rather than looked up`() {
        assertNull(wordAt("", 0))
        assertNull(wordAt("و دل", 0))
    }
}

class ArabicArticleTest {
    @Test
    fun `the arabic definite article is stripped, since poems quote arabic`() {
        assertTrue(affixes("الساقی").contains("ساقی"))
        assertTrue(affixes("الناس").contains("ناس"))
    }
}

class PersianMorphologyTest {
    @Test
    fun `enclitic pronouns glued onto a verb are stripped`() {
        assertTrue(affixes("آیدت").contains("آید"))
        assertTrue(affixes("باشدش").contains("باشد"))
        assertTrue(affixes("تربتش").contains("تربت"))
    }

    @Test
    fun `a prefix and a negation together still reach the verb`() {
        // برنیاید = بر + ن + یاید; one pass would stop at نیاید
        assertTrue(affixes("برنیاید").contains("یاید"))
    }

    @Test
    fun `plural and object markers still work`() {
        assertTrue(affixes("دلها").contains("دل"))
        assertTrue(affixes("مارا").contains("ما"))
    }

    @Test
    fun `stripping never produces a single letter`() {
        assertTrue(affixes("شان").none { it.length < 2 })
        assertTrue(affixes("بها").none { it.length < 2 })
    }
}
