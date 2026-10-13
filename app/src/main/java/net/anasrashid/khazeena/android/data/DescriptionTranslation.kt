package net.anasrashid.khazeena.android.data

/** Who translates Ganjoor's Persian descriptions; [Off] keeps them as Ganjoor wrote them. */
enum class DescriptionTranslator { Google, Assistant, Off }

/**
 * Ganjoor's AI descriptions of couplets and poems, carried from Persian into the reader's
 * language by Google Translate or by the reader's own assistant.
 */
object DescriptionTranslation {
    /**
     * Translations already made, so a couplet scrolling back into view is not translated again —
     * a LazyColumn disposes what scrolls away. Access-ordered, so the oldest falls out first.
     */
    private val done = object : LinkedHashMap<String, String>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, String>) = size > 256
    }

    fun key(by: DescriptionTranslator, target: AssistantLanguage, text: String) =
        "${by.name}|${target.code}|$text"

    @Synchronized
    fun cached(key: String): String? = done[key]

    @Synchronized
    private fun remember(key: String, translation: String) {
        done[key] = translation
    }

    /** Whether [by] can be asked at all, so a reader with nothing set up just sees the Persian. */
    fun ready(by: DescriptionTranslator, settings: AssistantSettings, offline: Boolean) = when (by) {
        DescriptionTranslator.Google -> GoogleTranslate.ready(settings.googleKey, offline)
        DescriptionTranslator.Assistant -> settings.serverReady
        DescriptionTranslator.Off -> false
    }

    suspend fun translate(
        by: DescriptionTranslator,
        settings: AssistantSettings,
        target: AssistantLanguage,
        text: String,
        offline: Boolean,
    ): Result<String> {
        val key = key(by, target, text)
        cached(key)?.let { return Result.success(it) }
        val result = when (by) {
            DescriptionTranslator.Google ->
                GoogleTranslate.translate(text, target.code, settings.googleKey, offline)
            DescriptionTranslator.Assistant ->
                Assistant.ask(settings, Prompts.system(target), Prompts.translateSummary(text))
            DescriptionTranslator.Off -> Result.failure(IllegalStateException("off"))
        }
        result.getOrNull()?.let { remember(key, it) }
        return result
    }
}
