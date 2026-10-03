package com.ganjoor.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ganjoor.android.R
import com.ganjoor.android.data.Ganjoor
import com.ganjoor.android.data.PoemRef
import com.ganjoor.android.data.Verse
import com.ganjoor.android.data.couplets
import com.ganjoor.android.ui.theme.readingStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoemScreen(fullUrl: String, onBack: () -> Unit, onPoem: (String) -> Unit) {
    Load(key = fullUrl, block = { Ganjoor.poem(fullUrl) }) { poem ->
        val prefs = LocalSettings.current.value
        val style = readingStyle(prefs.font, prefs.fontSize)
        val couplets = remember(poem) { poem.verses.couplets() }

        // Siblings come from the parent category, which is a much bigger file than the poem, so
        // it loads after the poem is already on screen.
        var siblings by remember(fullUrl) { mutableStateOf<List<PoemRef>>(emptyList()) }
        LaunchedEffect(fullUrl) {
            siblings = runCatching {
                Ganjoor.category(fullUrl.substringBeforeLast('/')).poems
            }.getOrDefault(emptyList())
        }
        val here = siblings.indexOfFirst { it.fullUrl == fullUrl }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(poem.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    },
                    actions = { ReadingSettingsAction() },
                )
            }
        ) { insets ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = insets.calculateTopPadding() + 8.dp,
                    bottom = insets.calculateBottomPadding() + 32.dp,
                ),
            ) {
                item {
                    Column(Modifier.padding(bottom = 12.dp)) {
                        Text(
                            text = poem.fullTitle,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        poem.metre?.rhythm?.let { rhythm ->
                            Text(
                                text = rhythm,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                items(couplets) { couplet -> Couplet(couplet, style, prefs.showSummaries) }

                if (prefs.showSummaries) {
                    poem.poemSummary?.takeIf { it.isNotBlank() }?.let { summary ->
                        item { PoemSummary(summary) }
                    }
                }

                item {
                    SiblingNav(
                        previous = if (here > 0) siblings[here - 1] else null,
                        next = if (here >= 0) siblings.getOrNull(here + 1) else null,
                        onPoem = onPoem,
                    )
                }

                poem.sourceName?.takeIf { it.isNotBlank() }?.let { source ->
                    item {
                        Text(
                            text = stringResource(R.string.source, source),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * One line of poetry. A couplet is two hemistichs; on a phone they stack, with the first pushed
 * to the start of the line and the second to the end, which is how Ganjoor itself reads.
 */
@Composable
private fun Couplet(
    couplet: List<Verse>,
    style: androidx.compose.ui.text.TextStyle,
    showSummaries: Boolean,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        couplet.forEach { verse ->
            Text(
                text = verse.text,
                style = style,
                textAlign = when (verse.position) {
                    Verse.RIGHT -> TextAlign.Start
                    Verse.LEFT -> TextAlign.End
                    Verse.CENTERED_1, Verse.CENTERED_2 -> TextAlign.Center
                    // Single / Paragraph / Comment: prose, so let it fill the column.
                    else -> TextAlign.Justify
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (showSummaries) {
            couplet.firstNotNullOfOrNull { it.coupletSummary }
                ?.takeIf { it.isNotBlank() }
                ?.let { summary ->
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                    )
                }
        }
    }
}

@Composable
private fun PoemSummary(summary: String) {
    Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
        HorizontalDivider()
        Text(
            text = stringResource(R.string.summary_ai_note),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SiblingNav(previous: PoemRef?, next: PoemRef?, onPoem: (String) -> Unit) {
    if (previous == null && next == null) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        previous?.let {
            TextButton(onClick = { onPoem(it.fullUrl) }) {
                Text(stringResource(R.string.previous_poem, it.title))
            }
        }
        next?.let {
            TextButton(onClick = { onPoem(it.fullUrl) }) {
                Text(stringResource(R.string.next_poem, it.title))
            }
        }
    }
}
