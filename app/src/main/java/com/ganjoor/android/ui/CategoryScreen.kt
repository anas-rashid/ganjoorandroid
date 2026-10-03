package com.ganjoor.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ganjoor.android.R
import com.ganjoor.android.data.Downloads
import com.ganjoor.android.data.Ganjoor
import com.ganjoor.android.data.Offline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    fullUrl: String,
    onUp: () -> Unit,
    onHome: () -> Unit,
    onCategory: (String) -> Unit,
    onPoem: (String) -> Unit,
) {
    // System Back climbs the tree too, not the visit history.
    BackHandler(onBack = onUp)

    Load(key = fullUrl, block = { Ganjoor.category(fullUrl) }) { cat ->
        // First lines are a separate, optional call; the list shows up without waiting for it.
        var excerpts by remember(cat.id) { mutableStateOf(emptyMap<Int, String>()) }
        LaunchedEffect(cat.id) {
            if (cat.poems.isNotEmpty()) {
                excerpts = runCatching { Ganjoor.excerpts(cat.id) }.getOrDefault(emptyMap())
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(cat.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    navigationIcon = {
                        IconButton(onClick = onUp) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    },
                    actions = {
                        HomeAction(onHome)
                        // A whole poet can be saved for offline reading; a sub-collection can't,
                        // because the saved tree is keyed by poet.
                        poetSlug(fullUrl)?.let { PoetDownloadAction(it) }
                        ReadingSettingsAction()
                    },
                )
            }
        ) { insets ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = insets.calculateTopPadding(),
                    bottom = insets.calculateBottomPadding() + 16.dp,
                ),
            ) {
                cat.description?.takeIf { it.isNotBlank() }?.let { description ->
                    item { Description(description) }
                }
                items(cat.childCats, key = { "c${it.id}" }) { child ->
                    NavRow(child.title, isCategory = true) { onCategory(child.fullUrl) }
                }
                items(cat.poems, key = { "p${it.id}" }) { poem ->
                    NavRow(poem.title, isCategory = false, excerpt = excerpts[poem.id]) {
                        onPoem(poem.fullUrl)
                    }
                }
            }
        }
    }
}

/** A poet's root URL is a single segment (`/hafez`); anything deeper is one of their books. */
private fun poetSlug(fullUrl: String): String? =
    fullUrl.trim('/').takeIf { it.isNotEmpty() && !it.contains('/') }

@Composable
private fun PoetDownloadAction(slug: String) {
    val progress = Downloads.running[slug]
    val saved = remember(slug, Downloads.revision) { Offline.isSaved(slug) }

    when {
        progress != null -> IconButton(onClick = { Downloads.cancel(slug) }) {
            if (progress.total == 0) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                CircularProgressIndicator(
                    progress = { progress.done.toFloat() / progress.total },
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            }
        }

        saved -> IconButton(onClick = { Downloads.delete(slug) }) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.delete_download),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        // Core Material icons ship no download glyph, and the extended set is 4 MB for one icon.
        else -> IconButton(onClick = { Downloads.start(slug) }) {
            Icon(painterResource(R.drawable.ic_download), stringResource(R.string.download))
        }
    }
}

@Composable
private fun NavRow(
    title: String,
    isCategory: Boolean,
    excerpt: String? = null,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        // The opening line says what a poem is about far better than "Ghazal 237" does.
        supportingContent = excerpt?.takeIf { it.isNotBlank() }?.let {
            { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
        // A collection drills down into more lists; a poem is the leaf you read.
        leadingContent = if (!isCategory) null else {
            {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        trailingContent = if (!isCategory) null else {
            { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider()
}

/** Poet biographies run long, so start clamped and expand on tap. */
@Composable
private fun Description(text: String) {
    var expanded by remember(text) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(16.dp)
            .animateContentSize()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
