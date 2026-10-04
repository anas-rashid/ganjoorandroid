package com.ganjoor.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ganjoor.android.R

/**
 * One credited component. [licenseAsset] names a file under `assets/licenses`; the OFL in
 * particular requires the licence text to travel with the software, so it is bundled rather
 * than linked.
 */
private data class Credit(
    val name: String,
    val holder: String,
    val license: String,
    val licenseAsset: String? = null,
    val url: String,
)

private val CREDITS = listOf(
    Credit(
        "Ganjoor",
        "ganjoor.net — the poems themselves, and the search and first-line lookups",
        "Classical Persian verse, long out of copyright. Ganjoor's own site and backend " +
            "(GanjoorService) are GPL-3.0; this app uses none of their code, only data over HTTPS.",
        url = "https://ganjoor.net",
    ),
    Credit(
        "ganjoor-data",
        "The static export this app reads",
        "No licence stated by the publisher",
        url = "https://github.com/anas-rashid/ganjoor-data",
    ),
    Credit(
        "Wiktionary",
        "Wiktionary contributors — the word definitions, and the inflected-form index that " +
            "finds a conjugated verb's dictionary entry",
        "CC BY-SA 3.0. The bundled dictionary is therefore also CC BY-SA 3.0.",
        url = "https://en.wiktionary.org",
    ),
    Credit(
        "Daneshjoo Dictionary",
        "Layered under Wiktionary for the words it doesn't carry",
        "The source repository states MIT",
        url = "https://github.com/0xdolan/Daneshjoo",
    ),
    Credit(
        "Noto Naskh Arabic",
        "The Noto Project Authors",
        "SIL Open Font License 1.1",
        "ofl-noto-naskh-arabic.txt",
        "https://github.com/notofonts/arabic",
    ),
    Credit(
        "Noto Nastaliq Urdu",
        "The Noto Project Authors",
        "SIL Open Font License 1.1",
        "ofl-noto-nastaliq-urdu.txt",
        "https://github.com/notofonts/nastaliq",
    ),
    Credit(
        "Libron",
        "Nico Verbruggen, after Readerly and Newsreader",
        "SIL Open Font License 1.1",
        "ofl-libron.txt",
        "https://github.com/nicoverbruggen/libron",
    ),
    Credit(
        "Jetpack Compose, AndroidX",
        "The Android Open Source Project",
        "Apache License 2.0",
        "apache-2.0.txt",
        "https://developer.android.com/jetpack",
    ),
    Credit(
        "Kotlin, kotlinx.serialization",
        "JetBrains",
        "Apache License 2.0",
        "apache-2.0.txt",
        "https://kotlinlang.org",
    ),
    Credit(
        "OkHttp, Okio",
        "Square, Inc.",
        "Apache License 2.0",
        "apache-2.0.txt",
        "https://square.github.io/okhttp/",
    ),
    Credit(
        "Coil",
        "Coil Contributors",
        "Apache License 2.0",
        "apache-2.0.txt",
        "https://coil-kt.github.io/coil/",
    ),
    Credit(
        "Accompanist",
        "The Android Open Source Project",
        "Apache License 2.0",
        "apache-2.0.txt",
        "https://google.github.io/accompanist/",
    ),
    Credit(
        "Ganjoor for Android",
        "This app",
        "MIT License",
        "mit-ganjoor-android.txt",
        "https://github.com/anas-rashid/ganjoorandroid",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onUp: () -> Unit, onHome: () -> Unit) {
    var showing by remember { mutableStateOf<Credit?>(null) }

    // The rest of the app is right-to-left because the poetry is, but these credits and licence
    // texts are English prose, which is unreadable right-aligned.
    val direction =
        if (LocalSettings.current.value.language == Language.En) LayoutDirection.Ltr
        else LayoutDirection.Rtl

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about)) },
                navigationIcon = {
                    IconButton(onClick = onUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = { HomeAction(onHome) },
            )
        }
    ) { insets ->
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = insets) {
            item {
                Text(
                    text = stringResource(R.string.about_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
                HorizontalDivider()
            }
            items(CREDITS) { credit ->
                ListItem(
                    headlineContent = { Text(credit.name) },
                    supportingContent = {
                        Column {
                            Text(credit.holder, style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = credit.license,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(credit.url, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    modifier = Modifier.clickable(
                        enabled = credit.licenseAsset != null,
                    ) { showing = credit },
                )
                HorizontalDivider()
            }
        }
    }

    showing?.let { credit ->
        val context = LocalContext.current
        val text = remember(credit) {
            runCatching {
                context.assets.open("licenses/${credit.licenseAsset}").bufferedReader()
                    .use { it.readText() }
            }.getOrDefault(credit.license)
        }
        AlertDialog(
            onDismissRequest = { showing = null },
            title = { Text(credit.name) },
            text = {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(onClick = { showing = null }) { Text(stringResource(R.string.done)) }
            },
        )
    }
    }
}
