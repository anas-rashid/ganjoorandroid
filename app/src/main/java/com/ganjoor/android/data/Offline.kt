package com.ganjoor.android.data

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

    fun isSaved(slug: String) = poetDir(slug).isDirectory

    /** Poem files only — `_cat.json` is structure, not something you read. */
    fun poemCount(slug: String): Int =
        poetDir(slug).walkTopDown().count { it.isFile && it.name != "_cat.json" }

    fun savedSlugs(): List<String> =
        File(root, "poets").listFiles()?.filter { it.isDirectory }?.map { it.name }?.sorted()
            ?: emptyList()

    fun delete(slug: String) {
        poetDir(slug).deleteRecursively()
    }

    fun bytes(): Long = root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}

/** Thrown when offline mode is on and the requested page was never downloaded. */
class NotDownloaded(path: String) : Exception("Not downloaded: $path")
