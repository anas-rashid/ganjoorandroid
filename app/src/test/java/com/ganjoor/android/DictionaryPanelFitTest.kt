package com.ganjoor.android

import androidx.compose.ui.unit.dp
import com.ganjoor.android.ui.dictionaryFitsBeside
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rule that decides whether the dictionary opens beside the poem or as a sheet over it.
 *
 * Driving the gesture that reaches it — a tap landing on the glyphs of one Persian word — is not
 * something adb does reliably, so the arithmetic is tested directly instead.
 */
class DictionaryPanelFitTest {

    @Test
    fun `a tablet has room beside the poem`() {
        // 1280dp window, less the poets rail and two list columns, leaves the page about 900dp.
        assertTrue(dictionaryFitsBeside(900.dp, wide = true))
    }

    @Test
    fun `a foldable open in portrait does not`() {
        // 700dp window, less the rail and the newest column, leaves about 450dp — and 450 less
        // the 216dp panel is 234dp, which is a cramped phone column, not a measure for a verse.
        assertFalse(dictionaryFitsBeside(450.dp, wide = true))
    }

    @Test
    fun `a phone never gets the panel, however the page is measured`() {
        assertFalse(dictionaryFitsBeside(420.dp, wide = false))
        assertFalse(dictionaryFitsBeside(2000.dp, wide = false))
    }

    @Test
    fun `the boundary is the panel plus the minimum measure`() {
        assertTrue(dictionaryFitsBeside(616.dp, wide = true))   // 400 + 216, exactly
        assertFalse(dictionaryFitsBeside(615.dp, wide = true))
    }
}
