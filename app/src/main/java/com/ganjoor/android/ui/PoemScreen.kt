package com.ganjoor.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ganjoor.android.R
import com.ganjoor.android.data.Bookmark
import com.ganjoor.android.data.breadcrumbs
import com.ganjoor.android.data.wordRangeAt
import com.ganjoor.android.data.Ganjoor
import com.ganjoor.android.data.LocalAssistant
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
    /** Large screens: the text keeps a centred reading measure and couplets may sit on one line. */
    wide: Boolean = false,
    /** Large screens: the button that hides or shows the columns, placed before the back arrow. */
    navigationToggle: (@Composable () -> Unit)? = null,
) {
    BackHandler(onBack = onUp)

    // The couplet travels with the word: the dictionary sheet is the one gesture every reader
    // finds, so the couplet's own actions live at its foot rather than behind a tap between words.
    var tapped by remember { mutableStateOf<WordTap?>(null) }

    // One side panel at a time. The dictionary and the reading settings both want the left of the
    // screen, and opening the second put two panels there at once — or, where there was no longer
    // room for two, left the dictionary as a sheet in the middle of the page while the settings
    // sat beside it. Either way the reader is asked to look in two places. The settings replace
    // the dictionary instead; closing them leaves the poem, which is where the reader was.
    val sidePanelOpen = LocalSidePanelOpen.current
    LaunchedEffect(sidePanelOpen) { if (sidePanelOpen) tapped = null }

    Load(
        key = fullUrl,
        block = { Ganjoor.poem(fullUrl) },
        // The bar stays up with Back, Home and the reading settings already working; only
        // the text waits. Sharing and bookmarking need a poem, so they arrive with it.
        placeholder = { LoadingPoem(wide, onUp, onHome, navigationToggle) },
    ) { poem ->
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

        // On a large screen the dictionary opens beside the poem, on the left, instead of as a
        // sheet over it: the text moves over to make room and nothing of it is covered.
        Row(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(poem.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        Row {
                            navigationToggle?.invoke()
                            IconButton(onClick = onUp) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                            }
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
            BoxWithConstraints {
            // On a large screen the text keeps a reading measure in the middle of whatever room
            // the columns leave it, and once that measure is wide enough the two hemistichs of a
            // couplet share a line, as ganjoor.net sets them on a desktop.
            val side = if (wide) maxOf(20.dp, (maxWidth - 680.dp) / 2) else 20.dp
            val sideBySide = wide && maxWidth - side * 2 >= 640.dp
            Column(Modifier.fillMaxSize().padding(top = insets.calculateTopPadding())) {
            // Above the text, not in it: as an item of the list the player was disposed the
            // moment it scrolled off, which released the MediaPlayer and cut the reading off
            // mid-line. Here it keeps playing, and stays in reach while you read further down.
            RecitationPlayer(poem.id, Modifier.padding(start = side, end = side, top = 8.dp))
            SelectionContainer(Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = side,
                    end = side,
                    top = 8.dp,
                    bottom = insets.calculateBottomPadding() + 32.dp,
                ),
            ) {
                item {
                    Column(Modifier.padding(bottom = 12.dp)) {
                        Breadcrumbs(poem.fullTitle, poem.fullUrl.ifBlank { fullUrl }, onCategory)
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
                        sideBySide = sideBySide,
                        showSummaries = prefs.showSummaries,
                        source = Bookmark(fullUrl, poem.title, poem.fullTitle),
                        onWord = { tapped = it },
                        tapped = tapped,
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
        }
        }
        if (wide) {
            AnimatedVisibility(
                visible = tapped != null,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut(),
            ) {
                tapped?.let { tap ->
                    WordPanel(
                        word = tap.word,
                        passage = tap.passage,
                        onDismiss = { tapped = null },
                    )
                }
            }
        }
        }
    }

    if (!wide) {
        tapped?.let { tap ->
            WordSheet(
                word = tap.word,
                passage = tap.passage,
                onDismiss = { tapped = null },
            )
        }
    }
}

/** The positions that make a line of verse; anything else (Single, Paragraph, Comment) is prose. */
private val VERSE_POSITIONS = setOf(Verse.RIGHT, Verse.LEFT, Verse.CENTERED_1, Verse.CENTERED_2)

/** A word someone tapped: what to look up, the couplet it came from, and where it sits in its verse. */
private data class WordTap(val word: String, val passage: Bookmark, val verse: Int, val range: IntRange)

/** The poem screen while its poem is on the way: the real top bar, and the text as a skeleton. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadingPoem(
    wide: Boolean,
    onUp: () -> Unit,
    onHome: () -> Unit,
    navigationToggle: (@Composable () -> Unit)?,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    Row {
                        navigationToggle?.invoke()
                        IconButton(onClick = onUp) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    }
                },
                // Going home and changing the reading settings do not depend on this poem, so
                // they do not wait for it. Share and bookmark do, and appear when it lands.
                actions = {
                    HomeAction(onHome)
                    ReadingSettingsAction()
                },
            )
        }
    ) { insets ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(insets)) {
            val side = if (wide) maxOf(20.dp, (maxWidth - 680.dp) / 2) else 20.dp
            SkeletonPoem(Modifier.padding(start = side, end = side, top = 16.dp))
        }
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
    onWord: (WordTap) -> Unit,
    tapped: WordTap?,
    sideBySide: Boolean = false,
) {
    // Tap, not long-press: long-press belongs to the text selection this sits inside.
    var actionsOpen by remember(couplet) { mutableStateOf(false) }
    val passage = source.copy(excerpt = couplet.joinToString("\n") { it.text })

    // Only a true Right+Left pair shares a line; centred verses and prose keep their own.
    val oneLine = sideBySide && couplet.size == 2 &&
        couplet[0].position == Verse.RIGHT && couplet[1].position == Verse.LEFT

    // Each line of verse sits in its own soft card, so the eye finds where one couplet ends and
    // the next begins, and the couplet's actions visibly belong to it. Prose (Golestan,
    // Nowruznameh) stays bare: a paragraph in a box reads as a quotation, not as the text.
    val isVerse = couplet.all { it.position in VERSE_POSITIONS }
    val colors = MaterialTheme.colorScheme
    // A step lighter than the page. On OLED black the usual step is all but black itself, so
    // the card takes the next one up: still dim, but there.
    val cardColor =
        if (colors.surface == Color.Black) colors.surfaceContainerHighest else colors.surfaceContainerHigh
    val card = if (isVerse) {
        Modifier
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cardColor)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    } else {
        Modifier.padding(vertical = 6.dp)
    }

    Column(modifier = Modifier.fillMaxWidth().then(card)) {
        if (oneLine) {
            Row(modifier = Modifier.fillMaxWidth()) {
                couplet.forEachIndexed { index, verse ->
                    if (index > 0) Spacer(Modifier.width(32.dp))
                    VerseText(
                        verse = verse,
                        style = style,
                        onWord = { range -> onWord(tapOf(verse, range, passage)) },
                        onElsewhere = { actionsOpen = !actionsOpen },
                        highlight = tapped?.takeIf { it.verse == verse.vOrder }?.range,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            couplet.forEach { verse ->
                VerseText(
                    verse = verse,
                    style = style,
                    onWord = { range -> onWord(tapOf(verse, range, passage)) },
                    // A tap that lands between words still opens the couplet's own actions.
                    onElsewhere = { actionsOpen = !actionsOpen },
                    highlight = tapped?.takeIf { it.verse == verse.vOrder }?.range,
                )
            }
        }
        if (showSummaries) {
            couplet.firstNotNullOfOrNull { it.coupletSummary }
                ?.takeIf { it.isNotBlank() }
                ?.let { summary ->
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    // Ganjoor writes these in Persian. Offered only once an assistant is set up:
                    // a button under every couplet earns its space only if it can answer, and the
                    // couplet's own actions are behind a tap between words that few will find.
                    if (LocalAssistant.current.serverReady) {
                        AssistantInline(
                            prompt = "summary",
                            text = summary,
                            label = R.string.assistant_translate,
                            instruction = R.string.assistant_translate_prompt,
                        )
                    }
                }
        }
        // A visible way in. The tap between words still works, but on a full line of poetry it
        // almost never lands there, so the actions were effectively unreachable without this.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(
                onClick = { actionsOpen = !actionsOpen },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = if (actionsOpen) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.couplet_options),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (actionsOpen) {
            PassageActions(passage)
            // Below the buttons rather than among them: the answer needs the full width.
            AssistantInline(
                prompt = "explain",
                text = passage.excerpt.orEmpty(),
                label = R.string.assistant_explain,
                instruction = R.string.assistant_ask_prompt,
            )
        }
    }
}

/**
 * One hemistich. Tapping a word looks it up; tapping between words falls through to the
 * couplet's save and copy actions, so both live on the same gesture without fighting.
 */
private fun tapOf(verse: Verse, range: IntRange, passage: Bookmark) =
    WordTap(verse.text.substring(range), passage, verse.vOrder, range)

@Composable
private fun VerseText(
    verse: Verse,
    style: androidx.compose.ui.text.TextStyle,
    onWord: (IntRange) -> Unit,
    onElsewhere: () -> Unit,
    /** The word being looked up, marked so the reader can see which one it was. */
    highlight: IntRange? = null,
    modifier: Modifier = Modifier,
) {
    var layout by remember(verse.text) { mutableStateOf<TextLayoutResult?>(null) }
    val fontSizePx = with(LocalDensity.current) { style.fontSize.toPx() }
    val mark = SpanStyle(
        background = MaterialTheme.colorScheme.secondaryContainer,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
    val text = remember(verse.text, highlight, mark) {
        buildAnnotatedString {
            append(verse.text)
            if (highlight != null && highlight.last < verse.text.length) {
                addStyle(mark, highlight.first, highlight.last + 1)
            }
        }
    }

    Text(
        text = text,
        style = style,
        textAlign = when (verse.position) {
            Verse.RIGHT -> TextAlign.Start
            Verse.LEFT -> TextAlign.End
            Verse.CENTERED_1, Verse.CENTERED_2 -> TextAlign.Center
            // Single / Paragraph / Comment: prose, so let it fill the column.
            else -> TextAlign.Justify
        },
        onTextLayout = { layout = it },
        modifier = modifier
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
 * Where the word actually under [position] lies, or null if the tap missed the glyphs.
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
): IntRange? {
    if (text.isEmpty() || fontSizePx <= 0f) return null
    val offset = layout.getOffsetForPosition(position).coerceIn(0, text.length - 1)
    val baseline = layout.getLineBaseline(layout.getLineForOffset(offset))
    // The band the ink actually occupies, measured from the baseline. Nastaliq hangs far above
    // it and dips a little below; these two multipliers are the knob to turn if a font is
    // swapped and taps start feeling off.
    if (position.y < baseline - fontSizePx * 1.4f) return null
    if (position.y > baseline + fontSizePx * 0.6f) return null
    return wordRangeAt(text, offset)
}

/** Save this passage, or copy it. Saving keeps the link back to the poem; copying doesn't. */
@Composable
internal fun PassageActions(passage: Bookmark) {
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
            // The same glyph as the top bar and as every other Android app: share is a shape
            // people recognise before they read the word next to it.
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(18.dp).padding(end = 4.dp),
            )
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
        AssistantInline(
            prompt = "summary",
            text = summary,
            label = R.string.assistant_translate,
            instruction = R.string.assistant_translate_prompt,
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
