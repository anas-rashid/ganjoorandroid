package net.anasrashid.khazeena.android.data

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Google Translate for the Play build: ML Kit, on the device. No key and no account; each
 * language pack (about 30 MB) downloads once, and from then on it works with no connection.
 * The FOSS build has the same object, calling the Cloud API instead.
 */
object GoogleTranslate {
    const val NEEDS_KEY = false

    /** Always: with offline mode on, a language pack already on the device still translates. */
    @Suppress("UNUSED_PARAMETER")
    fun ready(key: String, offline: Boolean) = true

    // One per target language, kept for the life of the process: opening one loads its model.
    private val translators = mutableMapOf<String, Translator>()

    /** [text] from Persian into [target], a language code such as `ur`. */
    suspend fun translate(text: String, target: String, key: String, offline: Boolean): Result<String> =
        runCatching {
            val language = TranslateLanguage.fromLanguageTag(target) ?: error("unsupported: $target")
            // Offline mode refuses the network entirely, and that includes a language pack.
            if (offline) {
                val onDevice = RemoteModelManager.getInstance()
                    .getDownloadedModels(TranslateRemoteModel::class.java).await()
                    .map { it.language }
                val needed = listOf(TranslateLanguage.PERSIAN, language) - TranslateLanguage.ENGLISH
                check(onDevice.containsAll(needed)) { "language pack not downloaded" }
            }
            val translator = synchronized(translators) {
                translators.getOrPut(language) {
                    Translation.getClient(
                        TranslatorOptions.Builder()
                            .setSourceLanguage(TranslateLanguage.PERSIAN)
                            .setTargetLanguage(language)
                            .build()
                    )
                }
            }
            translator.downloadModelIfNeeded().await()
            translator.translate(text).await().trim()
        }
}

// ponytail: five lines instead of kotlinx-coroutines-play-services for the one await we need.
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
}
