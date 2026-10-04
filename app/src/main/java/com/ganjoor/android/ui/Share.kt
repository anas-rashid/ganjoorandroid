package com.ganjoor.android.ui

import android.content.Context
import android.content.Intent
import com.ganjoor.android.R

/**
 * Hands text to whatever the reader has installed: a messaging app, a notes app, a translator, or
 * an AI assistant.
 *
 * ACTION_SEND is the entire mechanism. The system draws the chooser, so the app needs no list of
 * apps, no per-vendor integration, no permission, and nothing to update when the reader installs
 * something new — which is also why this is the one way to reach a proprietary assistant that
 * stays within F-Droid's rules: the app ships no code belonging to it.
 */
fun Context.shareText(text: String, url: String? = null, subject: String? = null) {
    // The link matters for poetry: a couplet with no reference is a quote nobody can look up.
    val body = url?.let { "$text\n\n${webUrl(it)}" } ?: text
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, body)
        if (!subject.isNullOrBlank()) putExtra(Intent.EXTRA_SUBJECT, subject)
    }
    startActivity(Intent.createChooser(intent, getString(R.string.share)))
}

/** Ganjoor paths are stored as site-relative, the way the API gives them. */
fun webUrl(fullUrl: String): String =
    if (fullUrl.startsWith("http")) fullUrl
    else "https://ganjoor.net/" + fullUrl.trim('/')
