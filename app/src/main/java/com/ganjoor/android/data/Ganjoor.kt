package com.ganjoor.android.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

/**
 * Ganjoor's poetry content as a static JSON "API": every endpoint below is a file in
 * github.com/anas-rashid/ganjoor-data served over jsDelivr's CDN. See that repo's API.md.
 *
 * Paths come straight from Ganjoor URLs, so nothing here needs the numeric id indexes:
 * /hafez/ghazal/sh1 -> poets/hafez/ghazal/sh1.json
 */
private const val DATA_BASE = "https://cdn.jsdelivr.net/gh/anas-rashid/ganjoor-data@main/"

// The data files use PascalCase keys; this maps them to idiomatic Kotlin names.
@OptIn(ExperimentalSerializationApi::class)
private val json = Json {
    ignoreUnknownKeys = true
    namingStrategy = JsonNamingStrategy { _, _, name -> name.replaceFirstChar(Char::uppercaseChar) }
}

@Serializable
data class Manifest(
    val poetsCount: Int = 0,
    val poemsCount: Int = 0,
    val generatedAtUtc: String? = null,
    val poets: List<PoetRef> = emptyList(),
)

@Serializable
data class PoetRef(val id: Int = 0, val nickname: String = "", val fullUrl: String = "") {
    val slug get() = fullUrl.trim('/')

    // ponytail: derived from the slug instead of fetching 240 poet.json files just for avatars.
    // Note the host: poet.json's own ImageUrl field points at ganjoor.net, which 404s for these.
    val imageUrl get() = "https://api.ganjoor.net/api/ganjoor/poet/image/$slug.gif"
}

@Serializable
data class Poet(
    val id: Int = 0,
    val name: String = "",
    val nickname: String = "",
    val description: String? = null,
    val fullUrl: String = "",
    val imageUrl: String? = null,
    val birthYearInLHijri: Int? = null,
    val deathYearInLHijri: Int? = null,
    val birthPlace: String? = null,
    val deathPlace: String? = null,
)

@Serializable
data class Category(
    val id: Int = 0,
    val poetId: Int = 0,
    val title: String = "",
    val fullUrl: String = "",
    val description: String? = null,
    val childCats: List<Category> = emptyList(),
    val poems: List<PoemRef> = emptyList(),
)

@Serializable
data class PoemRef(val id: Int = 0, val title: String = "", val fullUrl: String = "")

@Serializable
data class Metre(val rhythm: String? = null)

@Serializable
data class Poem(
    val id: Int = 0,
    val title: String = "",
    val fullTitle: String = "",
    val fullUrl: String = "",
    val rhymeLetters: String? = null,
    val sourceName: String? = null,
    val poemSummary: String? = null,
    val metre: Metre? = null,
    val verses: List<Verse> = emptyList(),
)

@Serializable
data class Verse(
    val vOrder: Int = 0,
    val position: String = RIGHT,
    val text: String = "",
    val coupletIndex: Int? = null,
    val coupletSummary: String? = null,
) {
    companion object {
        const val RIGHT = "Right"
        const val LEFT = "Left"
        const val CENTERED_1 = "CenteredVerse1"
        const val CENTERED_2 = "CenteredVerse2"
    }
}

/** The second half of a couplet, keyed by the first half it belongs to. */
private val coupletPairs = mapOf(
    Verse.LEFT to Verse.RIGHT,
    Verse.CENTERED_2 to Verse.CENTERED_1,
)

/**
 * Groups verses into couplets: Right+Left (and CenteredVerse1+2) are the two hemistichs of one
 * line of poetry; anything else (Single, Paragraph, Comment) stands on its own.
 */
fun List<Verse>.couplets(): List<List<Verse>> {
    val out = mutableListOf<MutableList<Verse>>()
    for (verse in sortedBy { it.vOrder }) {
        val open = out.lastOrNull()
        if (open != null && open.size == 1 && coupletPairs[verse.position] == open[0].position) {
            open += verse
        } else {
            out += mutableListOf(verse)
        }
    }
    return out
}

object Ganjoor {
    private val base = DATA_BASE.toHttpUrl()
    private lateinit var http: OkHttpClient

    fun init(context: Context) {
        if (::http.isInitialized) return
        // jsDelivr serves these with a long max-age, so this disk cache is what makes already
        // visited poems readable offline. ponytail: no Room mirror until "download a poet" exists.
        http = OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir, "ganjoor-data"), 64L * 1024 * 1024))
            .build()
    }

    private suspend inline fun <reified T> get(path: String): T = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(base.newBuilder().addPathSegments(path).build())
            .build()
        val response = try {
            http.newCall(request).execute()
        } catch (offline: IOException) {
            // Stale-while-offline: serve the cached copy rather than failing the screen.
            http.newCall(request.newBuilder().cacheControl(CacheControl.FORCE_CACHE).build())
                .execute()
        }
        response.use {
            if (!it.isSuccessful) throw IOException("HTTP ${it.code} for $path")
            json.decodeFromString<T>(it.body!!.string())
        }
    }

    suspend fun manifest(): Manifest = get("manifest.json")

    suspend fun poet(slug: String): Poet = get("poets/$slug/poet.json")

    /** @param fullUrl a Ganjoor category URL, e.g. `/hafez/ghazal` (or `/hafez` for a poet's root). */
    suspend fun category(fullUrl: String): Category = get("poets${fullUrl.trimEnd('/')}/_cat.json")

    /** @param fullUrl a Ganjoor poem URL, e.g. `/hafez/ghazal/sh1`. */
    suspend fun poem(fullUrl: String): Poem = get("poets${fullUrl.trimEnd('/')}.json")
}
