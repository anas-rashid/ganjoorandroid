package com.ganjoor.android

import com.ganjoor.android.data.SearchHit
import com.ganjoor.android.data.snippet
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Trimmed from a real api.ganjoor.net response: the live endpoint returns ~30 fields per poem,
 * so the decoder has to ignore almost all of them.
 */
private const val RESPONSE = """
[
  {
    "id": 2130,
    "title": "غزل شمارهٔ ۱",
    "fullTitle": "حافظ » غزلیات » غزل شمارهٔ ۱",
    "urlSlug": "sh1",
    "fullUrl": "/hafez/ghazal/sh1",
    "plainText": "الا یا ایها الساقی ادر کأسا و ناولها\nکه عشق آسان نمود اول ولی افتاد مشکل ها\nبه بوی نافه ای کآخر صبا زان طره بگشاید",
    "htmlText": "<div>…</div>",
    "sourceName": "ویکی‌درج",
    "published": true,
    "coupletsCount": 7,
    "verses": [],
    "recitations": []
  }
]
"""

class SearchHitTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a live search response decodes despite its many unused fields`() {
        val hits = json.decodeFromString<List<SearchHit>>(RESPONSE)

        assertEquals(1, hits.size)
        assertEquals("/hafez/ghazal/sh1", hits[0].fullUrl)
        assertEquals("حافظ » غزلیات » غزل شمارهٔ ۱", hits[0].fullTitle)
    }

    @Test
    fun `the snippet is the line the term is on, not the opening line`() {
        val hit = json.decodeFromString<List<SearchHit>>(RESPONSE).first()

        assertTrue(snippet(hit, "نافه").contains("نافه"))
        assertTrue(snippet(hit, "عشق").startsWith("که عشق"))
    }

    @Test
    fun `a term that isn't there falls back to the opening line`() {
        val hit = json.decodeFromString<List<SearchHit>>(RESPONSE).first()

        assertEquals("الا یا ایها الساقی ادر کأسا و ناولها", snippet(hit, "زرافه"))
    }

    @Test
    fun `a poem with no text yields an empty snippet rather than throwing`() {
        assertEquals("", snippet(SearchHit(id = 1), "عشق"))
    }
}
