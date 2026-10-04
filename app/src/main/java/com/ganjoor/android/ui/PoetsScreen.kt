package com.ganjoor.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.ganjoor.android.data.LocalPinnedPoets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import java.text.Collator
import java.util.Locale
import com.ganjoor.android.R
import com.ganjoor.android.ui.theme.downloaded
import com.ganjoor.android.data.Offline
import com.ganjoor.android.data.Downloads
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.CheckCircle
import com.ganjoor.android.data.Ganjoor
import com.ganjoor.android.data.PoetRef

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoetsScreen(
    onPoet: (String) -> Unit,
    onSearchPoems: (String) -> Unit,
    onBookmarks: () -> Unit,
    onDownloads: () -> Unit,
) {
    // Hoisted so the search box can live in the bottom bar, within thumb reach.
    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    ViewAction()
                    LanguageAction()
                    IconButton(onClick = onBookmarks) {
                        Icon(Icons.Default.Favorite, stringResource(R.string.bookmarks))
                    }
                    IconButton(onClick = onDownloads) {
                        Icon(painterResource(R.drawable.ic_download), stringResource(R.string.downloads))
                    }
                    ReadingSettingsAction()
                },
            )
        },
        bottomBar = {
            SearchBar(
                query = query,
                onQueryChange = { query = it },
                onSearchPoems = { onSearchPoems(query.trim()) },
            )
        },
    ) { insets ->
        Load(key = Unit, block = { Ganjoor.manifest() }) { manifest ->
            val settings = LocalSettings.current
            val sort = settings.value.poetSort
            val pinned = LocalPinnedPoets.current
            val poets = remember(query, manifest, sort, pinned.items.toList()) {
                val matches =
                    if (query.isBlank()) manifest.poets
                    else manifest.poets.filter {
                        it.nickname.contains(query.trim(), ignoreCase = true)
                    }
                when (sort) {
                    // Persian letters don't sort correctly by code point (آ vs ا, ی vs ي), so hand
                    // the ordering to a collator rather than String.compareTo.
                    PoetSort.Name -> {
                        val collator = Collator.getInstance(Locale.forLanguageTag("fa"))
                        matches.sortedWith { a, b -> collator.compare(a.nickname, b.nickname) }
                    }

                    // Pinned first, in the order they were pinned, then the rest untouched. With
                    // nothing pinned this is Ganjoor's order, which is why it can be the default.
                    PoetSort.Pinned -> pinnedFirst(matches, pinned.items)

                    PoetSort.Default -> matches
                }
            }

            val padding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = insets.calculateTopPadding() + 8.dp,
                bottom = insets.calculateBottomPadding() + 16.dp,
            )
            // The chips and the hint are the same whichever shape the poets take.
            val header: @Composable () -> Unit = {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PoetSort.entries.forEach { option ->
                            FilterChip(
                                selected = sort == option,
                                onClick = { settings.update { it.copy(poetSort = option) } },
                                label = { Text(stringResource(option.label)) },
                            )
                        }
                    }
                    // Only while the shelf is empty: once there is something on it, it explains
                    // itself, and a standing instruction is just clutter.
                    if (sort == PoetSort.Pinned && pinned.items.isEmpty()) {
                        Text(
                            text = stringResource(R.string.pin_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            if (settings.value.poetGrid) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(132.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = padding,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) { header() }
                    items(poets, key = { it.id }) { poet ->
                        PoetCard(
                            poet = poet,
                            pinned = pinned.contains(poet.fullUrl),
                            onClick = { onPoet(poet.fullUrl) },
                            onPin = { pinned.toggle(poet.fullUrl) },
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = padding,
                ) {
                    item { header() }
                    listItems(poets, key = { it.id }) { poet ->
                        PoetRow(
                            poet = poet,
                            pinned = pinned.contains(poet.fullUrl),
                            onClick = { onPoet(poet.fullUrl) },
                            onPin = { pinned.toggle(poet.fullUrl) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Filters the poets as you type, and offers the same words to the poem search one tap further.
 * It sits at the bottom of the screen: a phone held one-handed reaches here, not the top.
 */
@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, onSearchPoems: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (query.isNotBlank()) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.search_poems_for, query.trim())) },
                    leadingContent = {
                        Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.clickable(onClick = onSearchPoems),
                )
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.search_poets)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Switches the poets between cards and a list, and remembers which. */
@Composable
private fun ViewAction() {
    val settings = LocalSettings.current
    val grid = settings.value.poetGrid
    IconButton(onClick = { settings.update { it.copy(poetGrid = !grid) } }) {
        Icon(
            painter = painterResource(
                if (grid) R.drawable.ic_view_list else R.drawable.ic_view_grid
            ),
            contentDescription = stringResource(if (grid) R.string.view_list else R.string.view_grid),
        )
    }
}

/** The pin marker, shown only on a poet someone chose to keep. */
@Composable
private fun PinMark(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_pin),
        contentDescription = stringResource(R.string.unpin_poet),
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier.size(16.dp),
    )
}

/**
 * Fetch a poet's poems, or say they are already here. Three states and no menu: downloading shows
 * its progress and cancels on a tap, downloaded is a green tick, and anything else offers the
 * download. Deleting stays on the downloads page, where the sizes are — a tap next to a poet's
 * name should never be the thing that throws their poems away.
 */
@Composable
private fun DownloadAction(slug: String) {
    val progress = Downloads.running[slug]
    val saved = remember(slug, Downloads.revision) { Offline.isSaved(slug) }

    when {
        progress != null -> IconButton(onClick = { Downloads.cancel(slug) }) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
        }

        // In a box the size of an IconButton, even though nothing here is tappable: a bare icon
        // sits where the button's padding would have put it, so the ticks and the arrows would
        // not line up down the column.
        saved -> Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.downloaded_poet),
                tint = MaterialTheme.colorScheme.downloaded,
            )
        }

        else -> IconButton(onClick = { Downloads.start(slug) }) {
            Icon(
                painter = painterResource(R.drawable.ic_download),
                contentDescription = stringResource(R.string.download),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One poet as a row, for readers who would rather scan names than faces. */
@Composable
private fun PoetRow(poet: PoetRef, pinned: Boolean, onClick: () -> Unit, onPin: () -> Unit) {
    ListItem(
        headlineContent = { Text(poet.nickname, style = MaterialTheme.typography.titleMedium) },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(poet.nickname.take(1), color = MaterialTheme.colorScheme.onSecondaryContainer)
                AsyncImage(
                    model = poet.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        },
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (pinned) PinMark()
                DownloadAction(poet.slug)
            }
        },
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onPin),
    )
}

@Composable
private fun PoetCard(poet: PoetRef, pinned: Boolean, onClick: () -> Unit, onPin: () -> Unit) {
    Card(modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onPin)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Not every poet has a portrait; the initial sits underneath so a miss isn't a hole.
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = poet.nickname.take(1),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                AsyncImage(
                    model = poet.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (pinned) PinMark()
                Text(
                    text = poet.nickname,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Pinned poets first, in the order they were pinned, then everyone else as Ganjoor has them.
 *
 * With nothing pinned this is Ganjoor's order untouched, which is what lets it be the default
 * without anyone opening the app to an empty screen.
 */
internal fun pinnedFirst(poets: List<PoetRef>, pins: List<String>): List<PoetRef> {
    if (pins.isEmpty()) return poets
    val (kept, rest) = poets.partition { it.fullUrl in pins }
    return kept.sortedBy { pins.indexOf(it.fullUrl) } + rest
}
