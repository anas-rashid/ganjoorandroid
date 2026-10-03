package com.ganjoor.android.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.edit
import com.ganjoor.android.R

const val PREFS_NAME = "ganjoor"

enum class ThemeMode(@StringRes val label: Int) {
    System(R.string.theme_system),
    Light(R.string.theme_light),
    Dark(R.string.theme_dark),
    Sepia(R.string.theme_sepia),
    SepiaDark(R.string.theme_sepia_dark),

    /** Pure black, so OLED panels can switch the pixels off entirely. */
    Black(R.string.theme_black),
}

enum class ReadingFont(@StringRes val label: Int) {
    Naskh(R.string.font_naskh),
    Nastaliq(R.string.font_nastaliq),
}

/** How heavy the poem text is drawn. Thin strokes wash out on a lit screen. */
enum class ReadingWeight(@StringRes val label: Int, val weight: FontWeight) {
    Regular(R.string.weight_regular, FontWeight.Normal),
    Medium(R.string.weight_medium, FontWeight.Medium),
    SemiBold(R.string.weight_semibold, FontWeight.SemiBold),
    Bold(R.string.weight_bold, FontWeight.Bold),
}

/** Each language is named in its own script, so the labels are the same whatever the UI locale. */
enum class Language(val tag: String, val label: String) {
    Fa("fa", "فارسی"),
    Ur("ur", "اردو"),
    En("en", "English"),
}

/** How the poet grid is ordered. */
enum class PoetSort(@StringRes val label: Int) {
    /** The data set's own order, which is Ganjoor's — best known poets first. */
    Default(R.string.sort_default),
    Name(R.string.sort_name),
}

data class Prefs(
    val theme: ThemeMode = ThemeMode.System,
    val font: ReadingFont = ReadingFont.Naskh,
    val fontSize: Float = 22f,
    val fontWeight: ReadingWeight = ReadingWeight.Regular,
    val showSummaries: Boolean = false,
    val language: Language = Language.Fa,
    val offline: Boolean = false,
    val poetSort: PoetSort = PoetSort.Default,
)

/** Reading preferences, kept in SharedPreferences and read once at startup. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var value by mutableStateOf(
        Prefs(
            theme = enumOrDefault(prefs.getString("theme", null), ThemeMode.System),
            font = enumOrDefault(prefs.getString("font", null), ReadingFont.Naskh),
            fontSize = prefs.getFloat("fontSize", 22f),
            fontWeight = enumOrDefault(prefs.getString("fontWeight", null), ReadingWeight.Regular),
            showSummaries = prefs.getBoolean("showSummaries", false),
            language = languageOrDefault(prefs.getString(KEY_LANGUAGE, null)),
            offline = prefs.getBoolean("offline", false),
            poetSort = enumOrDefault(prefs.getString("poetSort", null), PoetSort.Default),
        )
    )
        private set

    fun update(block: (Prefs) -> Prefs) {
        value = block(value).also { p ->
            // commit, not apply: these are a few hundred bytes written when someone taps a
            // setting, and an async write can be lost if the process is killed before it lands.
            prefs.edit(commit = true) {
                putString("theme", p.theme.name)
                putString("font", p.font.name)
                putFloat("fontSize", p.fontSize)
                putString("fontWeight", p.fontWeight.name)
                putBoolean("showSummaries", p.showSummaries)
                putString(KEY_LANGUAGE, p.language.tag)
                putBoolean("offline", p.offline)
                putString("poetSort", p.poetSort.name)
            }
        }
    }

    companion object {
        const val KEY_LANGUAGE = "language"

        /**
         * Read straight from prefs, for [com.ganjoor.android.MainActivity.attachBaseContext],
         * which runs long before any of the Compose world exists.
         */
        fun language(context: Context): Language = languageOrDefault(
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LANGUAGE, null)
        )
    }
}

private fun languageOrDefault(tag: String?): Language =
    Language.entries.firstOrNull { it.tag == tag } ?: Language.Fa

private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, fallback: T): T =
    runCatching { enumValueOf<T>(name!!) }.getOrDefault(fallback)

val LocalSettings = staticCompositionLocalOf<Settings> { error("No Settings provided") }
