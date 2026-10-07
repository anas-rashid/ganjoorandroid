package com.ganjoor.android.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.Normalizer


/** One definition, and where it came from, so the credit stays attached to the text. */
data class Definition(val word: String, val gloss: String, val source: String)

/**
 * How a word sounds. [label] is the variety it belongs to — Classical Persian, Dari, Standard
 * Urdu — because a word in a 14th-century ghazal was not said the way Tehran says it now.
 */
data class Pronunciation(val text: String, val label: String)

/**
 * Word lookup over a bundled SQLite built from Wiktionary (CC BY-SA 3.0) and Daneshjoo.
 * See `tools/build_dictionary.py`; the asset ships gzipped and is unpacked once on first use.
 */
object Dictionary {
    /** Guards against a copy interrupted half-way leaving an unopenable file behind. */
    private const val ASSET_BYTES = 94384128L

    // Not a .gz: the build packager silently gunzips those and drops the extension, which left
    // the asset under a different name than the code was opening.
    private const val ASSET = "dictionary.db"
    private lateinit var appContext: Context

    @Volatile
    private var db: SQLiteDatabase? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun open(): SQLiteDatabase? {
        db?.let { return it }
        return synchronized(this) {
            db ?: runCatching {
                val file = File(appContext.filesDir, "dictionary.db")
                if (file.length() != ASSET_BYTES) {
                    appContext.assets.open(ASSET).use { input ->
                        file.outputStream().use { input.copyTo(it) }
                    }
                }
                SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
            }.getOrNull()?.also { db = it }
        }
    }

    /**
     * How [raw] is pronounced, Classical Persian first: this is an app for poetry written long
     * before modern Tehrani vowels, and the IPA's dots and stress marks are the syllable
     * breakdown that goes with it.
     */
    suspend fun pronunciations(raw: String, limit: Int = 5): List<Pronunciation> =
        withContext(Dispatchers.IO) {
            val database = open() ?: return@withContext emptyList()
            val word = normalise(raw).takeIf { it.isNotEmpty() } ?: return@withContext emptyList()
            database.rawQuery(
                """
                SELECT text, label FROM pron WHERE word = ?
                ORDER BY CASE
                    WHEN label LIKE 'Classical%' THEN 0
                    WHEN source = 'urwiktionary' THEN 1
                    WHEN source = 'wiktionary-fa' THEN 2
                    WHEN source = 'wiktionary-ur' THEN 3
                    ELSE 4
                END
                LIMIT ?
                """,
                arrayOf(word, limit.toString()),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(Pronunciation(cursor.getString(0), cursor.getString(1)))
                    }
                }
            }
        }

    /**
     * Looks a word up, widening the search until something matches:
     *
     *  1. the word as written;
     *  2. the lemma it inflects from — Persian verbs conjugate heavily, and `افتاد` is only
     *     findable as `افتادن`;
     *  3. the word with a common prefix or suffix removed;
     *  4. the parts of a ZWNJ compound, so `بی‌روزی` finds `روزی`.
     */
    suspend fun lookup(raw: String): List<Definition> = withContext(Dispatchers.IO) {
        val database = open() ?: return@withContext emptyList()
        val word = normalise(raw)
        if (word.isEmpty()) return@withContext emptyList()

        direct(database, word)
            .ifEmpty { lemmas(database, word).flatMap { direct(database, it) } }
            .ifEmpty { affixes(word).firstNotNullOfOrNull { direct(database, it).ifEmpty { null } }.orEmpty() }
            .ifEmpty {
                normalise(raw, keepZwnj = true).split(ZWNJ)
                    .filter { it.length > 1 }
                    .firstNotNullOfOrNull { direct(database, normalise(it)).ifEmpty { null } }
                    .orEmpty()
            }
            // A selection is usually a phrase rather than a word; fall back to its words.
            .ifEmpty {
                raw.split(' ', '\n', '\r')
                    .map { normalise(it) }
                    .filter { it.length > 1 && it != word }
                    .firstNotNullOfOrNull { part ->
                        direct(database, part).ifEmpty {
                            lemmas(database, part).flatMap { direct(database, it) }.ifEmpty { null }
                        }
                    }
                    .orEmpty()
            }
    }

    /**
     * Ordered the way a reader of this app wants to be answered: a definition written in Urdu
     * first, because it needs no translating at all, then the Persian sources, then the ones
     * keyed on another language. English is what the rest fall back to, so it comes last by
     * coming from the sources that sit last.
     *
     * Arabic is last outright: its forms index is larger than every other source combined,
     * which makes it the likeliest to match something by coincidence.
     */
    private fun direct(database: SQLiteDatabase, word: String): List<Definition> =
        database.rawQuery(
            """
            SELECT display, gloss, source FROM entry WHERE word = ?
            ORDER BY CASE source
                WHEN 'urwiktionary' THEN 0
                WHEN 'wiktionary-fa' THEN 1
                WHEN 'daneshjoo' THEN 2
                WHEN 'wiktionary-ur' THEN 3
                ELSE 4
            END
            LIMIT 12
            """,
            arrayOf(word),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(Definition(cursor.getString(0), cursor.getString(1), cursor.getString(2)))
                }
            }
        }

    /**
     * Headwords that look like [raw], for when nothing matched exactly — a misread letter, an
     * unusual spelling, or a word the dictionary simply spells differently.
     *
     * Candidates come from an index range scan on the first letters, then are ranked by how
     * many letters they share with the query. ponytail: shared letters rather than an edit
     * distance, which would need the whole table scanned to be worth the extra precision.
     */
    suspend fun suggest(raw: String, limit: Int = 6): List<String> = withContext(Dispatchers.IO) {
        val database = open() ?: return@withContext emptyList()
        val word = normalise(raw).takeIf { it.length > 1 } ?: return@withContext emptyList()

        // Widen the prefix until there is something to rank, but never scan the whole table.
        val candidates = generateSequence(minOf(3, word.length - 1)) { (it - 1).takeIf { n -> n >= 1 } }
            .map { prefixLength -> byPrefix(database, word.take(prefixLength)) }
            .firstOrNull { it.size >= 3 }
            ?: return@withContext emptyList()

        candidates
            .asSequence()
            .filter { it.first != word }
            .map { (normalised, display) -> display to letterOverlap(word, normalised) }
            .filter { it.second > 0.45f }
            .sortedByDescending { it.second }
            .map { it.first }
            .distinct()
            .take(limit)
            .toList()
    }

    /** Index range scan: everything whose normalised form starts with [prefix]. */
    private fun byPrefix(database: SQLiteDatabase, prefix: String): List<Pair<String, String>> =
        database.rawQuery(
            "SELECT DISTINCT word, display FROM entry WHERE word >= ? AND word < ? LIMIT 400",
            arrayOf(prefix, prefix + '\uFFFF'),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0) to cursor.getString(1))
            }
        }

    private fun lemmas(database: SQLiteDatabase, form: String): List<String> =
        database.rawQuery(
            "SELECT lemma FROM form WHERE form = ? LIMIT 6",
            arrayOf(form),
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }
}

private const val ZWNJ = '‌'

/**
 * Longest first, so تربتش strips شـ rather than matching nothing. These are the endings that
 * actually turn up in classical verse: plurals, the object marker, and the enclitic pronouns
 * that Persian glues onto a verb or noun — آیدت is آید + ت, باشدش is باشد + ش.
 */
private val SUFFIXES = listOf(
    "شان", "تان", "مان", "ها", "اش", "ست", "یم", "ید", "ند", "را", "ش", "م", "ت", "ی", "ان", "ه",
)

/** "ال" is the Arabic definite article, "ن"/"نمی" negation, "بی" privative. */
private val PREFIXES = listOf("نمی", "ال", "می", "بی", "بر", "ن", "ب")

/**
 * Candidate stems, one affix deep and then two — برنیاید is بر + ن + یاید, and a single pass
 * would never reach the verb. Ordered so the least mangled candidate is tried first.
 */
internal fun affixes(word: String): List<String> {
    fun oneStep(w: String) = buildList {
        SUFFIXES.forEach { if (w.endsWith(it) && w.length > it.length + 1) add(w.dropLast(it.length)) }
        PREFIXES.forEach { if (w.startsWith(it) && w.length > it.length + 1) add(w.drop(it.length)) }
    }
    val first = oneStep(word)
    return (first + first.flatMap(::oneStep)).distinct()
}

private val HARAKAT = (0x064B..0x0652) + listOf(0x0670, 0x0640) + (0x0610..0x0615)

/**
 * Folds a word to the form the dictionary is keyed by: diacritics dropped, and the Arabic
 * letters Persian writes differently (ي ك ة) folded to their Persian shapes, because poems and
 * dictionary headwords disagree about them constantly.
 */
internal fun normalise(text: String, keepZwnj: Boolean = false): String {
    val stripped = Normalizer.normalize(text, Normalizer.Form.NFD)
        .filter { it.code !in HARAKAT }
    val folded = Normalizer.normalize(stripped, Normalizer.Form.NFC).map { char ->
        when (char) {
            'ي', 'ى' -> 'ی'
            'ك' -> 'ک'
            'ة' -> 'ه'
            else -> char
        }
    }.joinToString("")
    return (if (keepZwnj) folded else folded.replace(ZWNJ.toString(), "")).trim()
}

/**
 * How much of two words' letters coincide — the overlapping letters counted against the longer
 * word, so مشکل and مشکلها score high while a word that merely starts the same does not.
 */
internal fun letterOverlap(a: String, b: String): Float {
    if (a.isEmpty() || b.isEmpty()) return 0f
    val remaining = b.toMutableList()
    val shared = a.count { remaining.remove(it) }
    return shared.toFloat() / maxOf(a.length, b.length)
}

/** The whole word surrounding [index], for turning a tap into something to look up. */
internal fun wordAt(text: String, index: Int): String? =
    wordRangeAt(text, index)?.let { text.substring(it) }

/** Where in [text] the word [wordAt] finds lies, so the reader can see which word was looked up. */
internal fun wordRangeAt(text: String, index: Int): IntRange? {
    if (text.isEmpty()) return null
    val at = index.coerceIn(0, text.length - 1)
    if (!text[at].isWordChar()) return null
    var start = at
    while (start > 0 && text[start - 1].isWordChar()) start--
    var end = at
    while (end < text.length - 1 && text[end + 1].isWordChar()) end++
    // A joiner at either edge belongs to the neighbour, not to the word.
    while (start <= end && text[start] == ZWNJ) start++
    while (end >= start && text[end] == ZWNJ) end--
    return (start..end).takeIf { end - start + 1 > 1 }
}

private fun Char.isWordChar() = this in '؀'..'ۿ' || this == ZWNJ
