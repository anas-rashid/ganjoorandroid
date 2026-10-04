package com.ganjoor.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ganjoor.android.R
import com.ganjoor.android.data.Bookmark
import com.ganjoor.android.data.breadcrumbs
import com.ganjoor.android.data.wordAt
import com.ganjoor.android.data.Ganjoor
import com.ganjoor.android.data.LocalBookmarks
import com.ganjoor.android.data.Poem
import com.ganjoor.android.data.PoemRef
import com.ganjoor.android.data.Verse
import com.ganjoor.android.data.couplets
import com.ganjoor.android.ui.theme.readingStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoemScreen(
    fullUrl: String,
    onUp: () -> Unit,
    onHome: () -> Unit,
    onPoem: (String) -> Unit,
    onCategory: (String) -> Unit,
) {
    BackHandler(onBack = onUp)

    var tappedWord by remember { mutableStateOf<String?>(null) }

    Load(key = fullUrl, block = { Ganjoor.poem(fullUrl) }) { poem ->
        val prefs = LocalSettings.current.value
        val style = readingStyle(prefs.font, prefs.fontSize, prefs.fontWeight.weight)
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
                        IconButton(onClick = onUp) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    },
                    actions = {
                        HomeAction(onHome)
                        ShareAction(poem = poem, couplets = couplets)
                        BookmarkAction(
                            url = fullUrl,
                            title = poem.title,
                            subtitle = poem.fullTitle,
                        )
                        ReadingSettingsAction()
                    },
                )
            }
        ) { insets ->
            // Free-form selection for copying any span; tapping a word looks it up, and the
            // per-couplet actions save a passage with the reference attached, which a raw copy
            // would lose.
            //
            // The system selection menu gets its own entries from ProcessTextActivity, not from
            // here: Compose 1.10 stopped routing SelectionContainer through LocalTextToolbar, so
            // a custom TextToolbar is never asked to show. ACTION_PROCESS_TEXT goes round that,
            // and reaches every other app's selection menu as a side effect.
            SelectionContainer {
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
                        Breadcrumbs(poem.fullTitle, poem.fullUrl.ifBlank { fullUrl }, onCategory)
                        RecitationPlayer(poem.id)
                        poem.metre?.rhythm?.let { rhythm ->
                            Text(
                                text = rhythm,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                items(couplets) { couplet ->
                    Couplet(
                        couplet = couplet,
                        style = style,
                        showSummaries = prefs.showSummaries,
                        source = Bookmark(fullUrl, poem.title, poem.fullTitle),
                        onWord = { tappedWord = it },
                    )
                }

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

    tappedWord?.let { word ->
        WordSheet(word = word, onDismiss = { tappedWord = null })
    }
}

/** The poem's path, with every ancestor tappable: poet » book » section » this poem. */
@Composable
private fun Breadcrumbs(fullTitle: String, fullUrl: String, onCategory: (String) -> Unit) {
    val crumbs = remember(fullTitle, fullUrl) { breadcrumbs(fullTitle, fullUrl) }
    if (crumbs.isEmpty()) {
        Text(fullTitle, style = MaterialTheme.typography.titleMedium)
        return
    }

    FlowRow(verticalArrangement = Arrangement.Center) {
        crumbs.forEachIndexed { index, crumb ->
            if (index > 0) {
                Text(
                    text = " » ",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = crumb.label,
                style = MaterialTheme.typography.titleMedium,
                color = if (crumb.url == null) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.primary,
                modifier = if (crumb.url == null) Modifier
                else Modifier.clickable { onCategory(crumb.url) },
            )
        }
    }
}

/**
 * Shares the poem as text, with its title and link. The verses are joined couplet by couplet so
 * the shape survives in apps that know nothing about Persian prosody.
 */
@Composable
private fun ShareAction(poem: Poem, couplets: List<List<Verse>>) {
    val context = LocalContext.current
    IconButton(onClick = {
        val body = couplets.joinToString("\n\n") { couplet ->
            couplet.joinToString("\n") { it.text }
        }
        context.shareText(
            text = "${poem.fullTitle}\n\n$body",
            url = poem.fullUrl,
            subject = poem.fullTitle,
        )
    }) {
        Icon(Icons.Default.Share, stringResource(R.string.share_poem))
    }
}

@Composable
private fun BookmarkAction(url: String, title: String, subtitle: String) {
    val bookmarks = LocalBookmarks.current
    val saved = bookmarks.contains(url)
    IconButton(onClick = { bookmarks.toggle(Bookmark(url, title, subtitle)) }) {
        Icon(
            imageVector = if (saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = stringResource(
                if (saved) R.string.bookmark_remove else R.string.bookmark_add
            ),
            tint = if (saved) MaterialTheme.colorScheme.primary else LocalContentColor.current,
        )
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
    source: Bookmark,
    onWord: (String) -> Unit,
) {
    // Tap, not long-press: long-press belongs to the text selection this sits inside.
    var actionsOpen by remember(couplet) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        couplet.forEach { verse ->
            VerseText(
                verse = verse,
                style = style,
                onWord = onWord,
                // A tap that lands between words still opens the couplet's own actions.
                onElsewhere = { actionsOpen = !actionsOpen },
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
        if (actionsOpen) {
            PassageActions(source.copy(excerpt = couplet.joinToString("\n") { it.text }))
        }
    }
}

/**
 * One hemistich. Tapping a word looks it up; tapping between words falls through to the
 * couplet's save and copy actions, so both live on the same gesture without fighting.
 */
@Composable
private fun VerseText(
    verse: Verse,
    style: androidx.compose.ui.text.TextStyle,
    onWord: (String) -> Unit,
    onElsewhere: () -> Unit,
) {
    var layout by remember(verse.text) { mutableStateOf<TextLayoutResult?>(null) }
    val fontSizePx = with(LocalDensity.current) { style.fontSize.toPx() }

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
        onTextLayout = { layout = it },
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(verse.text) {
                detectTapGestures { position ->
                    val word = layout?.let { wordTappedAt(it, verse.text, position, fontSizePx) }
                    if (word != null) onWord(word) else onElsewhere()
                }
            },
    )
}

/**
 * The word actually under [position], or null if the tap missed the glyphs.
 *
 * getOffsetForPosition alone isn't enough: nastaliq is set with 2.4x leading, so most of a line
 * box is empty space above the glyphs, and a tap there clamps to the line's first character —
 * which made every tap return the opening word. Checking the character's own bounding box is
 * what distinguishes "on a word" from "in the gap between lines".
 */
internal fun wordTappedAt(
    layout: TextLayoutResult,
    text: String,
    position: Offset,
    fontSizePx: Float,
): String? {
    if (text.isEmpty() || fontSizePx <= 0f) return null
    val offset = layout.getOffsetForPosition(position).coerceIn(0, text.length - 1)
    val baseline = layout.getLineBaseline(layout.getLineForOffset(offset))
    // The band the ink actually occupies, measured from the baseline. Nastaliq hangs far above
    // it and dips a little below; these two multipliers are the knob to turn if a font is
    // swapped and taps start feeling off.
    if (position.y < baseline - fontSizePx * 1.4f) return null
    if (position.y > baseline + fontSizePx * 0.6f) return null
    return wordAt(text, offset)
}

/** Save this passage, or copy it. Saving keeps the link back to the poem; copying doesn't. */
@Composable
private fun PassageActions(passage: Bookmark) {
    val bookmarks = LocalBookmarks.current
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val saved = bookmarks.contains(passage)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { bookmarks.toggle(passage) }) {
            Text(
                text = stringResource(
                    if (saved) R.string.saved_passage else R.string.save_passage
                ),
                color = if (saved) MaterialTheme.colorScheme.primary
                else LocalContentColor.current,
            )
        }
        TextButton(onClick = {
            clipboard.setText(AnnotatedString(passage.excerpt.orEmpty()))
        }) {
            Text(stringResource(R.string.copy))
        }
        TextButton(onClick = {
            context.shareText(
                text = passage.excerpt.orEmpty(),
                url = passage.url,
                subject = passage.title,
            )
        }) {
            Text(stringResource(R.string.share))
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
