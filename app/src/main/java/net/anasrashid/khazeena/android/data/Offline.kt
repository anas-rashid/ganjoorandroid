package net.anasrashid.khazeena.android.data

import android.content.Context
import java.io.File

/**
 * Poems saved to the device. Mirrors the data set's own layout under `filesDir/offline`, so a
 * saved file is found by the same path [Ganjoor] would fetch — which is what lets offline mode
 * be a single check rather than a parallel code path.
 */
object Offline {
    private lateinit var root: File

    fun init(context: Context) {
        if (::root.isInitialized) return
        root = File(context.filesDir, "offline")
    }

    fun read(path: String): String? = File(root, path).takeIf { it.isFile }?.readText()

    fun write(path: String, body: String) {
        val file = File(root, path)
        file.parentFile?.mkdirs()
        file.writeText(body)
    }

    private fun poetDir(slug: String) = File(root, "poets/$slug")

    // Written last, once the download has run to the end, and holding how many poems did not
    // arrive. Without it a download killed halfway, by a crash or by Android, left a folder that
    // looked exactly like a finished one.
    private fun marker(slug: String) = File(poetDir(slug), ".complete")

    fun isSaved(slug: String) = marker(slug).isFile

    /** Poems that failed in the last download of a saved poet; downloading again fetches them. */
    fun missing(slug: String): Int = runCatching { marker(slug).readText().toInt() }.getOrDefault(0)

    fun markSaved(slug: String, missing: Int) {
        marker(slug).writeText(missing.toString())
    }

    /** Poem files only — `_cat.json` and `poet.json` are structure, not something you read. */
    fun poemCount(slug: String): Int =
        poetDir(slug).walkTopDown()
            .count { it.isFile && it.extension == "json" && it.name != "_cat.json" && it.name != "poet.json" }

    fun savedSlugs(): List<String> =
        File(root, "poets").listFiles()?.map { it.name }?.filter(::isSaved)?.sorted()
            ?: emptyList()

    fun delete(slug: String) {
        poetDir(slug).deleteRecursively()
    }

    fun bytes(): Long = root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}

/** Thrown when offline mode is on and the requested page was never downloaded. */
class NotDownloaded(path: String) : Exception("Not downloaded: $path")
