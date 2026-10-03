package com.ganjoor.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.ganjoor.android.data.Ganjoor
import com.ganjoor.android.data.PoetRef

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoetsScreen(onPoet: (String) -> Unit, onBookmarks: () -> Unit, onDownloads: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
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
        }
    ) { insets ->
        Load(key = Unit, block = { Ganjoor.manifest() }) { manifest ->
            val settings = LocalSettings.current
            val sort = settings.value.poetSort
            var query by remember { mutableStateOf("") }
            val poets = remember(query, manifest, sort) {
                val matches =
                    if (query.isBlank()) manifest.poets
                    else manifest.poets.filter {
                        it.nickname.contains(query.trim(), ignoreCase = true)
                    }
                // Persian letters don't sort correctly by code point (آ vs ا, ی vs ي), so hand
                // the ordering to a collator rather than String.compareTo.
                if (sort == PoetSort.Name) {
                    val collator = Collator.getInstance(Locale.forLanguageTag("fa"))
                    matches.sortedWith { a, b -> collator.compare(a.nickname, b.nickname) }
                } else {
                    matches
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(132.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = insets.calculateTopPadding() + 8.dp,
                    bottom = insets.calculateBottomPadding() + 16.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PoetSort.entries.forEach { option ->
                            FilterChip(
                                selected = sort == option,
                                onClick = { settings.update { it.copy(poetSort = option) } },
                                label = { Text(stringResource(option.label)) },
                            )
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(stringResource(R.string.search_poets)) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    )
                }
                items(poets, key = { it.id }) { poet ->
                    PoetCard(poet) { onPoet(poet.fullUrl) }
                }
            }
        }
    }
}

@Composable
private fun PoetCard(poet: PoetRef, onClick: () -> Unit) {
    Card(modifier = Modifier.clickable(onClick = onClick)) {
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
