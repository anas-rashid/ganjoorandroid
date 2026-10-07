package com.ganjoor.android.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.edit

/**
 * The poets someone keeps to hand, by the url that identifies them.
 *
 * Kept in the order they were pinned rather than sorted: a shelf someone arranges themselves
 * should stay where they put it, and a new pin appearing in the middle of the row is disorienting.
 *
 * ponytail: the same SharedPreferences and the same shape as [Bookmarks] — a list of a few strings
 * doesn't need a database or a schema, and the two have no reason to diverge.
 */
class PinnedPoets(context: Context) {
    private val prefs = context.getSharedPreferences("ganjoor", Context.MODE_PRIVATE)

    val items = mutableStateListOf<String>().apply {
        prefs.getString(KEY, null)
            ?.split(SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?.let(::addAll)
    }

    fun contains(fullUrl: String) = fullUrl in items

    fun toggle(fullUrl: String) {
        if (!items.remove(fullUrl)) items.add(fullUrl)
        // commit, so a pin survives the process being killed right after it's made.
        prefs.edit(commit = true) { putString(KEY, items.joinToString(SEPARATOR)) }
    }

    private companion object {
        const val KEY = "pinnedPoets"

        /** A newline cannot appear in a Ganjoor url, so it needs no escaping. */
        const val SEPARATOR = "\n"
    }
}

val LocalPinnedPoets = staticCompositionLocalOf<PinnedPoets> { error("No PinnedPoets provided") }
