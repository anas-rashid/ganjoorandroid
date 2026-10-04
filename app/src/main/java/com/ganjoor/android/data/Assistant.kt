package com.ganjoor.android.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * How the app reaches a language model, if the reader wants one at all.
 *
 * Nothing here is required: every feature of the app works with this switched off, which is both
 * the point and what keeps it acceptable as free software — no account, no bundled vendor SDK,
 * no key shipped with the app, and no default endpoint.
 */
enum class AssistantMode { Off, Server, ShareToApp }

/** The language a translation or summary is asked for. */
enum class AssistantLanguage(val code: String, val englishName: String) {
    Urdu("ur", "Urdu"),
    English("en", "English"),
}

/**
 * A server the reader runs or subscribes to. Only the free, self-hosted ones are offered as
 * presets; anything else is typed in, so the app never steers anyone towards a paid service.
 */
data class AssistantPreset(val name: String, val baseUrl: String, val model: String)

val ASSISTANT_PRESETS = listOf(
    AssistantPreset("Ollama", "http://127.0.0.1:11434/v1", "llama3.2"),
    AssistantPreset("LM Studio", "http://127.0.0.1:1234/v1", "local-model"),
    AssistantPreset("llama.cpp", "http://127.0.0.1:8080/v1", "local-model"),
    AssistantPreset("LocalAI", "http://127.0.0.1:8080/v1", "local-model"),
)

/** Reader-supplied configuration, kept on the device and never sent anywhere but the endpoint. */
class AssistantSettings(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var mode by mutableStateOf(
        runCatching { AssistantMode.valueOf(prefs.getString(MODE, null)!!) }
            .getOrDefault(AssistantMode.Off)
    )
        private set

    var baseUrl by mutableStateOf(prefs.getString(BASE_URL, "").orEmpty())
        private set

    var model by mutableStateOf(prefs.getString(MODEL, "").orEmpty())
        private set

    /**
     * Optional, and only needed by services that demand one — a server on your own machine
     * usually doesn't. Stored in the app's private preferences, unencrypted, like any other
     * setting; don't put a key here you would mind someone with your unlocked phone reading.
     */
    var apiKey by mutableStateOf(prefs.getString(API_KEY, "").orEmpty())
        private set

    var language by mutableStateOf(
        runCatching { AssistantLanguage.valueOf(prefs.getString(LANGUAGE, null)!!) }
            .getOrDefault(AssistantLanguage.Urdu)
    )
        private set

    fun update(
        mode: AssistantMode = this.mode,
        baseUrl: String = this.baseUrl,
        model: String = this.model,
        apiKey: String = this.apiKey,
        language: AssistantLanguage = this.language,
    ) {
        this.mode = mode
        this.baseUrl = baseUrl.trim()
        this.model = model.trim()
        this.apiKey = apiKey.trim()
        this.language = language
        prefs.edit(commit = true) {
            putString(MODE, mode.name)
            putString(BASE_URL, this@AssistantSettings.baseUrl)
            putString(MODEL, this@AssistantSettings.model)
            putString(API_KEY, this@AssistantSettings.apiKey)
            putString(LANGUAGE, language.name)
        }
    }

    /** Whether a request can actually be made, as opposed to merely being switched on. */
    val serverReady: Boolean
        get() = mode == AssistantMode.Server && baseUrl.isNotBlank() && model.isNotBlank()

    private companion object {
        const val PREFS = "ganjoor"
        const val MODE = "assistantMode"
        const val BASE_URL = "assistantBaseUrl"
        const val MODEL = "assistantModel"
        const val API_KEY = "assistantApiKey"
        const val LANGUAGE = "assistantLanguage"
    }
}

val LocalAssistant = staticCompositionLocalOf<AssistantSettings> { error("No AssistantSettings") }

@Serializable
private data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false,
    val temperature: Double = 0.2,
)

@Serializable
private data class ChatMessage(val role: String, val content: String)

@Serializable
private data class ChatResponse(val choices: List<Choice> = emptyList())

@Serializable
private data class Choice(val message: ChatMessage? = null)

/**
 * Talks to anything that speaks the OpenAI chat-completions shape, which Ollama, LM Studio,
 * llama.cpp and LocalAI all do. That one shape is why no vendor library is needed: it is an
 * HTTP POST with a JSON body, and the app already has an HTTP client and a JSON parser.
 */
object Assistant {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val media = "application/json; charset=utf-8".toMediaType()

    // A model running on a phone or an old laptop can take a while to answer.
    private val http by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .build()
    }

    suspend fun ask(
        settings: AssistantSettings,
        system: String,
        user: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!settings.serverReady) {
            return@withContext Result.failure(IllegalStateException("not configured"))
        }
        val url = settings.baseUrl.trimEnd('/') + "/chat/completions"
        val body = json.encodeToString(
            ChatRequest(
                model = settings.model,
                messages = listOf(
                    ChatMessage("system", system),
                    ChatMessage("user", user),
                ),
            )
        ).toRequestBody(media)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .apply { if (settings.apiKey.isNotBlank()) header("Authorization", "Bearer ${settings.apiKey}") }
            .build()

        runCatching {
            http.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}: ${text.take(200)}")
                json.decodeFromString<ChatResponse>(text)
                    .choices.firstOrNull()?.message?.content?.trim()
                    .orEmpty()
                    .ifEmpty { throw IOException("empty reply") }
            }
        }
    }
}

/** The instruction given to the model, kept here so it can be read and argued with. */
object Prompts {
    private const val ROLE =
        "You are helping someone read classical Persian poetry from Ganjoor. Answer only with " +
            "what was asked, with no preamble and no notes about yourself."

    fun system(language: AssistantLanguage) =
        "$ROLE Reply in ${language.englishName}."

    fun translate(text: String) = "Translate this Persian poetry faithfully:\n\n$text"

    fun summarise(text: String) =
        "Summarise what this Persian poem says, in a short paragraph:\n\n$text"

    fun explain(couplet: String) =
        "Explain the meaning and the imagery of this couplet:\n\n$couplet"

    /** Ganjoor publishes its own AI summaries in Persian; this carries one across. */
    fun translateSummary(summary: String) =
        "Translate this Persian commentary on a poem:\n\n$summary"
}
