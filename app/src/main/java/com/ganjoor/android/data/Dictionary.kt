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
 * Word lookup over a bundled SQLite built from Wiktionary (CC BY-SA 3.0) and Daneshjoo.
 * See `tools/build_dictionary.py`; the asset ships gzipped and is unpacked once on first use.
 */
object Dictionary {
    /** Guards against a copy interrupted half-way leaving an unopenable file behind. */
    private const val ASSET_BYTES = 22_298_624L

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
    }

    private fun direct(database: SQLiteDatabase, word: String): List<Definition> =
        database.rawQuery(
            "SELECT display, gloss, source FROM entry WHERE word = ? LIMIT 12",
            arrayOf(word),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(Definition(cursor.getString(0), cursor.getString(1), cursor.getString(2)))
                }
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

private val SUFFIXES = listOf("ها", "اش", "ش", "م", "ت", "را", "ی", "ان")
// "ال" is the Arabic definite article: poems quote Arabic, so السّاقی has to reach ساقی.
private val PREFIXES = listOf("ال", "می", "بر", "ب")

/** Candidate stems after stripping one common affix. Order matters: longest affix first. */
internal fun affixes(word: String): List<String> = buildList {
    SUFFIXES.forEach { if (word.endsWith(it) && word.length > it.length + 1) add(word.dropLast(it.length)) }
    PREFIXES.forEach { if (word.startsWith(it) && word.length > it.length + 1) add(word.drop(it.length)) }
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

/** The whole word surrounding [index], for turning a tap into something to look up. */
internal fun wordAt(text: String, index: Int): String? {
    if (text.isEmpty()) return null
    val at = index.coerceIn(0, text.length - 1)
    if (!text[at].isWordChar()) return null
    var start = at
    while (start > 0 && text[start - 1].isWordChar()) start--
    var end = at
    while (end < text.length - 1 && text[end + 1].isWordChar()) end++
    return text.substring(start, end + 1).trim(ZWNJ).takeIf { it.length > 1 }
}

private fun Char.isWordChar() = this in '؀'..'ۿ' || this == ZWNJ
