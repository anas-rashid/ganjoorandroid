package com.ganjoor.android.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.edit
import com.ganjoor.android.R

enum class ThemeMode(@StringRes val label: Int) {
    System(R.string.theme_system),
    Light(R.string.theme_light),
    Dark(R.string.theme_dark),
    Sepia(R.string.theme_sepia),
    SepiaDark(R.string.theme_sepia_dark),
}

enum class ReadingFont(@StringRes val label: Int) {
    Nastaliq(R.string.font_nastaliq),
    Naskh(R.string.font_naskh),
}

data class Prefs(
    val theme: ThemeMode = ThemeMode.System,
    val font: ReadingFont = ReadingFont.Nastaliq,
    val fontSize: Float = 22f,
    val showSummaries: Boolean = false,
)

/** Reading preferences, kept in SharedPreferences and read once at startup. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("ganjoor", Context.MODE_PRIVATE)

    var value by mutableStateOf(
        Prefs(
            theme = enumOrDefault(prefs.getString("theme", null), ThemeMode.System),
            font = enumOrDefault(prefs.getString("font", null), ReadingFont.Nastaliq),
            fontSize = prefs.getFloat("fontSize", 22f),
            showSummaries = prefs.getBoolean("showSummaries", false),
        )
    )
        private set

    fun update(block: (Prefs) -> Prefs) {
        value = block(value).also { p ->
            prefs.edit {
                putString("theme", p.theme.name)
                putString("font", p.font.name)
                putFloat("fontSize", p.fontSize)
                putBoolean("showSummaries", p.showSummaries)
            }
        }
    }
}

private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, fallback: T): T =
    runCatching { enumValueOf<T>(name!!) }.getOrDefault(fallback)

val LocalSettings = staticCompositionLocalOf<Settings> { error("No Settings provided") }
