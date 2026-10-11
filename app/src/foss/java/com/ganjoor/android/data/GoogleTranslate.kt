package com.ganjoor.android.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Google Translate for the FOSS build: the Cloud Translation API over plain HTTPS, with a key
 * the reader brings. No Google library ships in this build, which is what keeps it free software;
 * the Play build has the same object, translating on the device instead.
 */
object GoogleTranslate {
    /** This build cannot translate until the reader pastes in a Cloud API key. */
    const val NEEDS_KEY = true

    /** A key, and a network the reader has not switched off. */
    fun ready(key: String, offline: Boolean) = key.isNotBlank() && !offline

    private const val ENDPOINT = "https://translation.googleapis.com/language/translate/v2"
    private val json = Json { ignoreUnknownKeys = true }
    private val http by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Serializable
    private data class Response(val data: Data = Data())

    @Serializable
    private data class Data(val translations: List<Translated> = emptyList())

    @Serializable
    private data class Translated(val translatedText: String = "")

    internal fun parse(body: String): String =
        json.decodeFromString<Response>(body).data.translations.firstOrNull()
            ?.translatedText?.trim().orEmpty()
            .ifEmpty { throw IOException("empty reply") }

    /** [text] from Persian into [target], a language code such as `ur`. */
    suspend fun translate(text: String, target: String, key: String, offline: Boolean): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                check(ready(key, offline)) { "not configured" }
                val url = ENDPOINT.toHttpUrl().newBuilder().addQueryParameter("key", key).build()
                // format=text, or the reply comes back HTML-escaped.
                val body = FormBody.Builder()
                    .add("q", text)
                    .add("source", "fa")
                    .add("target", target)
                    .add("format", "text")
                    .build()
                http.newCall(Request.Builder().url(url).post(body).build()).execute().use { response ->
                    val reply = response.body?.string().orEmpty()
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}: ${reply.take(200)}")
                    parse(reply)
                }
            }
        }
}
