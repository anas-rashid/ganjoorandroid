package com.ganjoor.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ganjoor.android.R
import com.ganjoor.android.data.LocalBookmarks

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(onUp: () -> Unit, onPoem: (String) -> Unit) {
    val bookmarks = LocalBookmarks.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bookmarks)) },
                navigationIcon = {
                    IconButton(onClick = onUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        }
    ) { insets ->
        if (bookmarks.items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(insets), Alignment.Center) {
                Text(
                    text = stringResource(R.string.no_bookmarks),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = insets) {
            items(bookmarks.items, key = { it.id }) { bookmark ->
                ListItem(
                    // A saved passage leads with its own words; a saved poem with its title.
                    headlineContent = {
                        Text(
                            text = bookmark.excerpt ?: bookmark.title,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = bookmark.subtitle.takeIf { it.isNotBlank() }?.let {
                        { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    },
                    trailingContent = {
                        IconButton(onClick = { bookmarks.toggle(bookmark) }) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = stringResource(R.string.bookmark_remove),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    modifier = Modifier.clickable { onPoem(bookmark.url) },
                )
                HorizontalDivider()
            }
        }
    }
}
