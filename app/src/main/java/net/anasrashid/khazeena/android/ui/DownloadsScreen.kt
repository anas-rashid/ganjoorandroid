package net.anasrashid.khazeena.android.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import net.anasrashid.khazeena.android.R
import net.anasrashid.khazeena.android.data.Downloads
import net.anasrashid.khazeena.android.data.Ganjoor
import net.anasrashid.khazeena.android.data.Offline
import net.anasrashid.khazeena.android.data.PoetRef

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(onUp: () -> Unit) {
    Load(key = Unit, block = { Ganjoor.manifest() }) { manifest ->
        val context = LocalContext.current
        val startDownload = rememberStartDownload()
        // Re-read the disk whenever a download finishes or a poet is removed.
        val revision = Downloads.revision
        val saved = remember(revision) { Offline.savedSlugs().toSet() }
        val bytes = remember(revision) { Offline.bytes() }

        val selected = remember { mutableStateListOf<String>() }
        var query by remember { mutableStateOf("") }
        var confirmAll by remember { mutableStateOf(false) }

        val poets = remember(query, manifest) {
            if (query.isBlank()) manifest.poets
            else manifest.poets.filter { it.nickname.contains(query.trim(), ignoreCase = true) }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.downloads)) },
                    navigationIcon = {
                        IconButton(onClick = onUp) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                stringResource(R.string.back),
                            )
                        }
                    },
                )
            },
            bottomBar = {
                // Only takes up room once something is ticked.
                if (selected.isNotEmpty()) {
                    Surface(tonalElevation = 3.dp) {
                        Button(
                            onClick = {
                                startDownload(selected.toList())
                                selected.clear()
                            },
                            modifier = Modifier
                                .navigationBarsPadding()
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            Text(stringResource(R.string.download_selected, selected.size))
                        }
                    }
                }
            },
        ) { insets ->
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = insets) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.storage_used,
                                Formatter.formatShortFileSize(context, bytes),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = { confirmAll = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.download_all))
                        }
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text(stringResource(R.string.search_poets)) },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    HorizontalDivider()
                }

                // What is downloading, and what is queued behind it, above the full list, so
                // progress can be followed without hunting for each poet.
                val active = Downloads.active
                val queue = manifest.poets.filter { it.slug in Downloads.running }
                    .sortedByDescending { it.slug == active }
                if (queue.isNotEmpty()) {
                    item {
                        ListItem(
                            headlineContent = {
                                Text(
                                    stringResource(R.string.downloads_now),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            },
                            trailingContent = {
                                TextButton(onClick = Downloads::cancelAll) {
                                    Text(stringResource(R.string.cancel_all))
                                }
                            },
                        )
                    }
                    items(queue, key = { "queued-${it.id}" }) { poet ->
                        QueuedRow(poet, Downloads.running[poet.slug], poet.slug == active)
                    }
                    item { HorizontalDivider() }
                }

                // Queued poets are listed above instead, so nobody appears twice.
                items(poets.filter { it.slug !in Downloads.running }, key = { it.id }) { poet ->
                    PoetRow(
                        poet = poet,
                        isSaved = poet.slug in saved,
                        isSelected = poet.slug in selected,
                        revision = revision,
                        onToggleSelected = {
                            if (!selected.remove(poet.slug)) selected.add(poet.slug)
                        },
                        onRetry = { startDownload(listOf(poet.slug)) },
                    )
                    HorizontalDivider()
                }
            }

            if (confirmAll) {
                AlertDialog(
                    onDismissRequest = { confirmAll = false },
                    title = { Text(stringResource(R.string.download_all)) },
                    text = { Text(stringResource(R.string.download_all_warning)) },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmAll = false
                            // Poets already complete would only be walked again for nothing.
                            startDownload(
                                manifest.poets.map { it.slug }
                                    .filter { it !in saved || Offline.missing(it) > 0 }
                            )
                        }) { Text(stringResource(R.string.download)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmAll = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                )
            }
        }
    }
}

/** A poet in the download queue: a progress bar while it is fetched, "waiting" until then. */
@Composable
private fun QueuedRow(poet: PoetRef, progress: Downloads.Progress?, isActive: Boolean) {
    ListItem(
        headlineContent = { Text(poet.nickname) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    when {
                        !isActive -> stringResource(R.string.download_waiting)
                        progress == null || progress.total == 0 ->
                            stringResource(R.string.download_preparing)
                        else -> stringResource(R.string.downloading, progress.done, progress.total)
                    }
                )
                if (isActive) {
                    if (progress == null || progress.total == 0) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(
                            progress = { progress.done.toFloat() / progress.total },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        leadingContent = {
            AsyncImage(
                model = poet.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(44.dp).clip(CircleShape),
            )
        },
        trailingContent = {
            IconButton(onClick = { Downloads.cancel(poet.slug) }) {
                Icon(Icons.Default.Close, stringResource(R.string.cancel))
            }
        },
    )
}

/**
 * Starts downloads, first asking (Android 13 and up) to show the notification that follows them
 * once the app is left. Refusing only hides the notification; the downloads run either way.
 */
@Composable
fun rememberStartDownload(): (List<String>) -> Unit {
    val context = LocalContext.current
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    return { slugs ->
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) ask.launch(Manifest.permission.POST_NOTIFICATIONS)
        slugs.forEach { Downloads.start(context, it) }
    }
}

/**
 * One poet not in the queue. The trailing control follows its state: on the device (with a retry
 * beside delete when some poems did not arrive), or available to tick for the next batch.
 */
@Composable
private fun PoetRow(
    poet: PoetRef,
    isSaved: Boolean,
    isSelected: Boolean,
    revision: Int,
    onToggleSelected: () -> Unit,
    onRetry: () -> Unit,
) {
    val poems = remember(poet.slug, revision) { if (isSaved) Offline.poemCount(poet.slug) else 0 }
    val missing = remember(poet.slug, revision) { if (isSaved) Offline.missing(poet.slug) else 0 }

    ListItem(
        headlineContent = { Text(poet.nickname) },
        supportingContent = when {
            missing > 0 -> {
                {
                    Text(
                        stringResource(R.string.downloaded_partial, poems, missing),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            isSaved -> {
                { Text(stringResource(R.string.downloaded, poems)) }
            }

            else -> null
        },
        leadingContent = {
            AsyncImage(
                model = poet.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(44.dp).clip(CircleShape),
            )
        },
        trailingContent = {
            when {
                isSaved -> Row {
                    if (missing > 0) IconButton(onClick = onRetry) {
                        Icon(painterResource(R.drawable.ic_download), stringResource(R.string.retry))
                    }
                    IconButton(onClick = { Downloads.delete(poet.slug) }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.delete_download))
                    }
                }

                else -> Checkbox(checked = isSelected, onCheckedChange = { onToggleSelected() })
            }
        },
        modifier = if (isSaved) Modifier
        else Modifier.clickable(onClick = onToggleSelected),
    )
}
