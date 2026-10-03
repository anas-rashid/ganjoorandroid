package com.ganjoor.android.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.HttpUrl
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

/** First lines come from ganjoor.net's live API, which uses camelCase. */
@Serializable
private data class LiveCatPage(val cat: LiveCat? = null)

@Serializable
private data class LiveCat(val poems: List<LivePoem> = emptyList())

@Serializable
private data class LivePoem(val id: Int = 0, val excerpt: String? = null)

private val liveJson = Json { ignoreUnknownKeys = true }

fun catPath(fullUrl: String) = "poets${fullUrl.trimEnd('/')}/_cat.json"

fun poemPath(fullUrl: String) = "poets${fullUrl.trimEnd('/')}.json"

object Ganjoor {
    private val dataBase = DATA_BASE.toHttpUrl()
    private val liveBase = "https://api.ganjoor.net/".toHttpUrl()
    private lateinit var http: OkHttpClient

    /** When set, nothing leaves the device: only downloaded pages open. */
    @Volatile
    var offline = false

    fun init(context: Context) {
        Offline.init(context)
        if (::http.isInitialized) return
        // jsDelivr serves these with a long max-age, so this cache covers casual re-reading.
        // Anything that has to survive for certain goes through Offline instead.
        http = OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir, "ganjoor-data"), 64L * 1024 * 1024))
            .build()
    }

    private fun dataUrl(path: String) = dataBase.newBuilder().addPathSegments(path).build()

    private fun fetch(url: HttpUrl): String {
        val request = Request.Builder().url(url).build()
        val response = try {
            http.newCall(request).execute()
        } catch (offline: IOException) {
            // Stale-while-offline: serve the cached copy rather than failing the screen.
            http.newCall(request.newBuilder().cacheControl(CacheControl.FORCE_CACHE).build())
                .execute()
        }
        return response.use {
            if (!it.isSuccessful) throw IOException("HTTP ${it.code} for $url")
            it.body!!.string()
        }
    }

    /** Downloaded copy first, then the network — unless offline mode rules the network out. */
    private suspend inline fun <reified T> load(path: String): T = withContext(Dispatchers.IO) {
        Offline.read(path)?.let { return@withContext json.decodeFromString<T>(it) }
        if (offline) throw NotDownloaded(path)
        json.decodeFromString<T>(fetch(dataUrl(path)))
    }

    /** Like [load], but keeps the body on disk. Already-saved paths cost nothing. */
    private suspend fun grab(path: String): String = withContext(Dispatchers.IO) {
        Offline.read(path) ?: fetch(dataUrl(path)).also { Offline.write(path, it) }
    }

    // Kept on disk from the first fetch: without the poet list, offline mode has no way in.
    suspend fun manifest(): Manifest = json.decodeFromString(grab("manifest.json"))

    suspend fun poet(slug: String): Poet = load("poets/$slug/poet.json")

    /** @param fullUrl a Ganjoor category URL, e.g. `/hafez/ghazal` (or `/hafez` for a poet's root). */
    suspend fun category(fullUrl: String): Category = load(catPath(fullUrl))

    /** @param fullUrl a Ganjoor poem URL, e.g. `/hafez/ghazal/sh1`. */
    suspend fun poem(fullUrl: String): Poem = load(poemPath(fullUrl))

    /**
     * Poem id -> opening line, for a category listing.
     *
     * The data set's `_cat.json` carries only id, title and url, so this is a best-effort call
     * to ganjoor.net's live API; the list renders without it and fills in when it lands. The
     * result is kept on disk so downloaded poets still show their first lines offline.
     *
     * ponytail: delete this the day ganjoor-data's `_cat.json` gains an Excerpt field.
     */
    suspend fun excerpts(catId: Int): Map<Int, String> = withContext(Dispatchers.IO) {
        val path = "excerpts/$catId.json"
        Offline.read(path)?.let {
            return@withContext runCatching {
                liveJson.decodeFromString<Map<Int, String>>(it)
            }.getOrDefault(emptyMap())
        }
        if (offline) return@withContext emptyMap()

        val url = liveBase.newBuilder()
            .addPathSegments("api/ganjoor/cat/$catId")
            .addQueryParameter("poems", "true")
            .build()
        runCatching {
            liveJson.decodeFromString<LiveCatPage>(fetch(url)).cat?.poems.orEmpty()
                .mapNotNull { poem -> poem.excerpt?.takeIf { it.isNotBlank() }?.let { poem.id to it } }
                .toMap()
        }.onSuccess {
            if (it.isNotEmpty()) Offline.write(path, liveJson.encodeToString(it))
        }.getOrDefault(emptyMap())
    }

    /**
     * Saves a poet's whole tree — biography, every category and every poem — for offline reading.
     * Re-running it is cheap: anything already on disk is skipped, which is also how a download
     * interrupted by the process dying gets finished.
     */
    suspend fun downloadPoet(slug: String, onProgress: (done: Int, total: Int) -> Unit) {
        val wasOffline = offline
        offline = false
        try {
            runCatching { grab("poets/$slug/poet.json") }

            val poemPaths = mutableListOf<String>()
            val catIds = mutableListOf<Int>()

            suspend fun walk(fullUrl: String) {
                val cat = json.decodeFromString<Category>(grab(catPath(fullUrl)))
                catIds += cat.id
                cat.poems.forEach { poemPaths += poemPath(it.fullUrl) }
                cat.childCats.forEach { walk(it.fullUrl) }
            }
            walk("/$slug")

            onProgress(0, poemPaths.size)
            var done = 0
            // Six at a time: enough to keep the CDN busy without opening a connection per poem.
            poemPaths.chunked(6).forEach { chunk ->
                coroutineScope {
                    chunk.map { path -> async { runCatching { grab(path) } } }.awaitAll()
                }
                done += chunk.size
                onProgress(done, poemPaths.size)
            }

            catIds.forEach { runCatching { excerpts(it) } }
        } finally {
            offline = wasOffline
        }
    }
}
