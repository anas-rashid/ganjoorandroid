package com.ganjoor.android.ui

import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ganjoor.android.R
import com.ganjoor.android.data.Downloads
import com.ganjoor.android.data.Ganjoor
import com.ganjoor.android.data.Offline
import com.ganjoor.android.data.PoetRef

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(onBack: () -> Unit) {
    Load(key = Unit, block = { Ganjoor.manifest() }) { manifest ->
        val context = LocalContext.current
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
                        IconButton(onClick = onBack) {
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
                                selected.forEach { Downloads.start(it) }
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

                items(poets, key = { it.id }) { poet ->
                    PoetRow(
                        poet = poet,
                        progress = Downloads.running[poet.slug],
                        isSaved = poet.slug in saved,
                        isSelected = poet.slug in selected,
                        revision = revision,
                        onToggleSelected = {
                            if (!selected.remove(poet.slug)) selected.add(poet.slug)
                        },
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
                            manifest.poets.forEach { Downloads.start(it.slug) }
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

/**
 * One poet. The trailing control is whichever of the three states the poet is in: being fetched,
 * already on the device, or available to tick for the next batch.
 */
@Composable
private fun PoetRow(
    poet: PoetRef,
    progress: Downloads.Progress?,
    isSaved: Boolean,
    isSelected: Boolean,
    revision: Int,
    onToggleSelected: () -> Unit,
) {
    val poems = remember(poet.slug, revision) { if (isSaved) Offline.poemCount(poet.slug) else 0 }

    ListItem(
        headlineContent = { Text(poet.nickname) },
        supportingContent = when {
            progress != null && progress.total > 0 -> {
                { Text(stringResource(R.string.downloading, progress.done, progress.total)) }
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
                progress != null -> IconButton(onClick = { Downloads.cancel(poet.slug) }) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                }

                isSaved -> IconButton(onClick = { Downloads.delete(poet.slug) }) {
                    Icon(Icons.Default.Delete, stringResource(R.string.delete_download))
                }

                else -> Checkbox(checked = isSelected, onCheckedChange = { onToggleSelected() })
            }
        },
        modifier = if (progress != null || isSaved) Modifier
        else Modifier.clickable(onClick = onToggleSelected),
    )
}
