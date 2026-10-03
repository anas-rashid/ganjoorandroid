package com.ganjoor.android.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.edit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A saved poem, or a saved passage from one. Either way [url] points back at the poem it came
 * from, so every entry in the list stays tappable.
 */
@Serializable
data class Bookmark(
    val url: String,
    val title: String,
    val subtitle: String = "",
    val excerpt: String? = null,
) {
    /** Several passages can be saved from one poem, so the url alone can't be the identity. */
    val id: String get() = if (excerpt == null) url else "$url#$excerpt"
}

/**
 * Bookmarked poems, kept on the device. A handful of lines of JSON in SharedPreferences —
 * ponytail: a database for a list someone scrolls by hand isn't worth the schema.
 */
class Bookmarks(context: Context) {
    private val prefs = context.getSharedPreferences("ganjoor", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    val items = mutableStateListOf<Bookmark>().apply {
        val stored = prefs.getString(KEY, null)
        if (stored != null) {
            addAll(runCatching { json.decodeFromString<List<Bookmark>>(stored) }.getOrDefault(emptyList()))
        }
    }

    /** Whether the whole poem is bookmarked — passages saved from it don't count. */
    fun contains(url: String) = items.any { it.url == url && it.excerpt == null }

    fun contains(bookmark: Bookmark) = items.any { it.id == bookmark.id }

    fun toggle(bookmark: Bookmark) {
        if (!items.removeAll { it.id == bookmark.id }) items.add(0, bookmark)
        // commit, so a bookmark survives the process being killed right after it's made.
        prefs.edit(commit = true) { putString(KEY, json.encodeToString(items.toList())) }
    }

    private companion object {
        const val KEY = "bookmarks"
    }
}

val LocalBookmarks = staticCompositionLocalOf<Bookmarks> { error("No Bookmarks provided") }
