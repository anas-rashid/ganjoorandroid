package com.ganjoor.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ganjoor.android.R
import com.ganjoor.android.data.CatEntry
import com.ganjoor.android.data.Category
import com.ganjoor.android.data.Ganjoor
import com.ganjoor.android.data.LocalPinnedPoets
import com.ganjoor.android.data.Manifest
import com.ganjoor.android.data.PoetRef
import com.ganjoor.android.data.orderedEntries
import com.ganjoor.android.data.parentUrl

/*
 * The tablet and unfolded-foldable layout. Right to left, as the app reads: a narrow column of
 * poets, then a column for each level of the open book (its books, a book's chapters, a
 * chapter's poems), then the open page in whatever room is left. Phones never see any of this;
 * GanjoorApp only switches to it at 600dp and wider.
 */

/**
 * What the columns have loaded and where each one is scrolled to.
 *
 * Every browse step is still a navigation destination, so Back and the phone layout keep working
 * unchanged; this lives above the NavHost so that moving from one destination to the next
 * doesn't throw away the columns the reader can still see.
 */
@Stable
class BrowserState {
    var manifest by mutableStateOf<Manifest?>(null)
    val categories = mutableStateMapOf<String, Category>()
    val excerpts = mutableStateMapOf<Int, Map<Int, String>>()
    val railState = LazyListState()
    private val listStates = HashMap<String, LazyListState>()
    private val widths = HashMap<String, Animatable<Float, AnimationVector1D>>()

    fun listState(url: String): LazyListState = listStates.getOrPut(url) { LazyListState() }

    /**
     * A column's width in dp, kept here rather than in the column: each tap is a new destination,
     * and a width remembered by the column would open it out from nothing again every time.
     */
    fun width(url: String): Animatable<Float, AnimationVector1D> = widths.getOrPut(url) { Animatable(0f) }

    suspend fun category(url: String): Category =
        categories[url] ?: Ganjoor.category(url).also { categories[url] = it }
}

/**
 * The categories that get a column on the way to [url], poet first:
 * `/saadi/golestan/bab1/sh1` → `/saadi`, `/saadi/golestan`, `/saadi/golestan/bab1`.
 * A poem is read rather than listed, so its own URL gets no column; a category's does.
 */
internal fun columnUrls(url: String, isPoem: Boolean): List<String> {
    val segments = url.trim('/').split('/').filter { it.isNotEmpty() }
    val depth = if (isPoem) segments.size - 1 else segments.size
    return (1..depth).map { "/" + segments.take(it).joinToString("/") }
}

/**
 * Newest column widest; older ones give their room to the page. While a poem is open every
 * column steps back further, so the reading gets most of the screen.
 */
private fun columnWidth(distance: Int, reading: Boolean): Dp = when {
    distance == 0 && !reading -> 224.dp
    distance == 0 || distance == 1 && !reading -> 168.dp
    else -> 132.dp
}

private fun same(a: String?, b: String?) = a != null && b != null && a.trimEnd('/') == b.trimEnd('/')

private val CatEntry.url
    get() = when (this) {
        is CatEntry.Chapter -> category.fullUrl
        is CatEntry.Poem -> poem.fullUrl
    }

/**
 * The columns beside [content]. [expanded] (840dp and up) shows up to three list columns;
 * narrower, only the newest one, with a way back up in its header.
 *
 * [content] gets the button that hides or shows the columns, to put in its own top bar.
 */
@Composable
fun ColumnBrowser(
    state: BrowserState,
    url: String,
    isPoem: Boolean,
    expanded: Boolean,
    onPoet: (String) -> Unit,
    onCategory: (String) -> Unit,
    onPoem: (String) -> Unit,
    content: @Composable (toggle: @Composable () -> Unit) -> Unit,
) {
    val settings = LocalSettings.current
    val hidden = settings.value.columnsHidden
    val columns = remember(url, isPoem) { columnUrls(url, isPoem) }
    // What is selected in each column: the next step of the path, down to the open poem.
    val path = if (isPoem) columns + url else columns
    val shown = columns.takeLast(if (expanded) 3 else 1)

    Row(Modifier.fillMaxSize()) {
        // Folding away slides the columns off to the right, where they live.
        AnimatedVisibility(
            visible = !hidden,
            enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
            exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(),
        ) {
            Row(Modifier.fillMaxHeight()) {
                PoetRail(
                    state = state,
                    selected = path.firstOrNull(),
                    compact = shown.size > 1 || isPoem,
                    onPoet = onPoet,
                )
                VerticalDivider()
                shown.forEachIndexed { index, columnUrl ->
                    key(columnUrl) {
                        // Only the first visible column can have hidden ancestors to climb back
                        // to, and a poet's own column climbs to the rail, which is already there.
                        val up = parentUrl(columnUrl)
                            ?.takeIf { index == 0 && columns.size > shown.size }
                        ListColumn(
                            state = state,
                            url = columnUrl,
                            selected = path.getOrNull(path.indexOf(columnUrl) + 1),
                            distance = shown.size - 1 - index,
                            reading = isPoem,
                            onUp = up?.let { parent -> { onCategory(parent) } },
                            onCategory = onCategory,
                            onPoem = onPoem,
                        )
                        VerticalDivider()
                    }
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            content {
                // Hiding is in the top bar; bringing them back is the floating button below,
                // which is there whenever they are hidden.
                if (!hidden) ColumnsToggle { settings.update { it.copy(columnsHidden = true) } }
            }
            // Reader view: the page has the whole screen, and one button, at the edge the
            // columns went to, brings them back as they were.
            ShowColumnsButton(
                visible = hidden,
                onShow = { settings.update { it.copy(columnsHidden = false) } },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(16.dp),
            )
        }
    }
}

/**
 * Brings the columns back from reader view.
 *
 * In its own composable because the Box it is placed in sits inside the browser's Row: with
 * RowScope still an implicit receiver there, `AnimatedVisibility` resolves to the row overload,
 * which takes no alignment and does not compile. A function of its own has only its own scope.
 */
@Composable
private fun ShowColumnsButton(visible: Boolean, onShow: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier,
    ) {
        SmallFloatingActionButton(onClick = onShow) {
            Icon(Icons.Default.Menu, stringResource(R.string.show_columns))
        }
    }
}

/** Folds the columns away so the page has the whole screen (reader view). */
@Composable
private fun ColumnsToggle(onHide: () -> Unit) {
    IconButton(onClick = onHide) {
        Icon(painterResource(R.drawable.ic_menu_open), stringResource(R.string.hide_columns))
    }
}

/** Every poet, portrait over name, in the same order the home screen uses. */
@Composable
private fun PoetRail(
    state: BrowserState,
    selected: String?,
    compact: Boolean,
    onPoet: (String) -> Unit,
) {
    var attempt by remember { mutableIntStateOf(0) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(attempt) {
        if (state.manifest == null) {
            failed = false
            runCatching { Ganjoor.manifest() }
                .onSuccess { state.manifest = it }
                .onFailure { failed = true }
        }
    }

    val sort = LocalSettings.current.value.poetSort
    val pinned = LocalPinnedPoets.current
    val manifest = state.manifest
    val poets = remember(manifest, sort, pinned.items.toList()) {
        manifest?.let { orderPoets(it.poets, sort, pinned.items) }.orEmpty()
    }
    val width by animateDpAsState(if (compact) 80.dp else 96.dp, label = "rail width")

    // Keep the open poet in view, without yanking the rail back while the reader scrolls it.
    LaunchedEffect(selected, poets.size) {
        val index = poets.indexOfFirst { same(it.fullUrl, selected) }
        if (index >= 0 && state.railState.layoutInfo.visibleItemsInfo.none { it.index == index }) {
            state.railState.scrollToItem(index)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.width(width).fillMaxHeight(),
    ) {
        when {
            manifest != null -> LazyColumn(
                state = state.railState,
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(poets, key = { it.id }) { poet ->
                    RailPoet(
                        poet = poet,
                        selected = same(poet.fullUrl, selected),
                        compact = compact,
                        onClick = { onPoet(poet.fullUrl) },
                    )
                }
            }

            failed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TextButton(onClick = { attempt++ }) { Text(stringResource(R.string.retry)) }
            }

            else -> SkeletonRail(Modifier.statusBarsPadding(), disc = if (compact) 44.dp else 56.dp)
        }
    }
}

@Composable
private fun RailPoet(poet: PoetRef, selected: Boolean, compact: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) colors.secondaryContainer else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // A ring round the open poet's portrait, so the choice doesn't rest on the tile alone.
        val ring = if (selected) {
            Modifier.border(2.dp, colors.primary, CircleShape).padding(4.dp)
        } else {
            Modifier
        }
        Box(
            modifier = Modifier
                .size(if (compact) 44.dp else 56.dp)
                .then(ring)
                .clip(CircleShape)
                .background(colors.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            // The initial sits underneath, as on the home screen, so a missing portrait isn't a hole.
            Text(poet.nickname.take(1), color = colors.onSecondaryContainer)
            AsyncImage(
                model = poet.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            text = poet.nickname,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.onSecondaryContainer else colors.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * One level of the open book: a category's chapters and poems, with [selected] marked.
 * [distance] counts the columns to its left; the further back, the narrower and terser it gets.
 */
@Composable
private fun ListColumn(
    state: BrowserState,
    url: String,
    selected: String?,
    distance: Int,
    reading: Boolean,
    onUp: (() -> Unit)?,
    onCategory: (String) -> Unit,
    onPoem: (String) -> Unit,
) {
    val target = columnWidth(distance, reading)
    // From nothing when the column first appears, so it opens out rather than popping in; after
    // that, from wherever it was, so an older column narrows as a new one opens.
    val width = state.width(url)
    LaunchedEffect(target) {
        width.animateTo(target.value, tween(300, easing = FastOutSlowInEasing))
    }

    var attempt by remember(url) { mutableIntStateOf(0) }
    var failed by remember(url) { mutableStateOf(false) }
    LaunchedEffect(url, attempt) {
        failed = false
        runCatching { state.category(url) }.onFailure { failed = true }
    }

    val cat = state.categories[url]
    // Older columns, and every column while a poem is open, drop first lines and wrap titles.
    val terse = distance > 0 || reading
    // First lines only where there's room to show them: the newest column.
    LaunchedEffect(cat?.id, terse) {
        if (cat != null && !terse && cat.poems.isNotEmpty() && cat.id !in state.excerpts) {
            state.excerpts[cat.id] = runCatching { Ganjoor.excerpts(cat.id) }.getOrDefault(emptyMap())
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.width(width.value.dp).fillMaxHeight().clipToBounds(),
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = if (terse) 8.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onUp != null) {
                    IconButton(onClick = onUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
                Text(
                    text = cat?.title.orEmpty(),
                    style = if (terse) MaterialTheme.typography.titleSmall
                    else MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            HorizontalDivider()

            // Only this column waits: a skeleton of its rows, fading into the list when it lands.
            Crossfade(targetState = cat, label = "column") { loaded ->
            when {
                loaded != null -> {
                    val cat = loaded
                    val entries = remember(cat) { orderedEntries(cat) }
                    val excerpts = if (terse) emptyMap() else state.excerpts[cat.id].orEmpty()
                    val listState = state.listState(url)
                    LaunchedEffect(selected, entries) {
                        val index = entries.indexOfFirst { same(it.url, selected) }
                        if (index >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.index == index }) {
                            listState.scrollToItem(index)
                        }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(
                            items = entries,
                            key = { entry ->
                                when (entry) {
                                    is CatEntry.Chapter -> "c${entry.category.id}"
                                    is CatEntry.Poem -> "p${entry.poem.id}"
                                }
                            },
                        ) { entry ->
                            ColumnRow(
                                entry = entry,
                                selected = same(entry.url, selected),
                                terse = terse,
                                excerpt = (entry as? CatEntry.Poem)?.let { excerpts[it.poem.id] },
                                onClick = {
                                    when (entry) {
                                        is CatEntry.Chapter -> onCategory(entry.category.fullUrl)
                                        is CatEntry.Poem -> onPoem(entry.poem.fullUrl)
                                    }
                                },
                            )
                        }
                    }
                }

                failed -> Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.load_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { attempt++ }) { Text(stringResource(R.string.retry)) }
                }

                else -> SkeletonList(rows = 10, twoLines = !terse)
            }
            }
        }
    }
}

@Composable
private fun ColumnRow(
    entry: CatEntry,
    selected: Boolean,
    terse: Boolean,
    excerpt: String?,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val title = when (entry) {
        is CatEntry.Chapter -> entry.category.title
        is CatEntry.Poem -> entry.poem.title
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) colors.secondaryContainer else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = if (terse) 8.dp else 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = if (terse) MaterialTheme.typography.bodyMedium
                else MaterialTheme.typography.bodyLarge,
                color = if (selected) colors.onSecondaryContainer else colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            // A chapter opens another column; a poem opens on the page.
            if (!terse && entry is CatEntry.Chapter) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        excerpt?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The page for an open poet, book or chapter: its description, then what it holds as cards.
 * The column beside it lists the same things; this is what fills the room the phone's list did.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryOverview(
    state: BrowserState,
    fullUrl: String,
    toggle: @Composable () -> Unit,
    onUp: () -> Unit,
    onHome: () -> Unit,
    onCategory: (String) -> Unit,
    onPoem: (String) -> Unit,
) {
    BackHandler(onBack = onUp)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.categories[fullUrl]?.title.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    Row {
                        toggle()
                        IconButton(onClick = onUp) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    HomeAction(onHome)
                    poetSlug(fullUrl)?.let { PoetDownloadAction(it) }
                    ReadingSettingsAction()
                },
            )
        }
    ) { insets ->
        Load(
            key = fullUrl,
            block = { state.category(fullUrl) },
            placeholder = {
                SkeletonCards(Modifier.padding(top = insets.calculateTopPadding()).padding(horizontal = 12.dp))
            },
        ) { cat ->
            LaunchedEffect(cat.id) {
                if (cat.poems.isNotEmpty() && cat.id !in state.excerpts) {
                    state.excerpts[cat.id] =
                        runCatching { Ganjoor.excerpts(cat.id) }.getOrDefault(emptyMap())
                }
            }
            val entries = remember(cat) { orderedEntries(cat) }
            val excerpts = state.excerpts[cat.id].orEmpty()

            BoxWithConstraints(Modifier.fillMaxSize().padding(top = insets.calculateTopPadding())) {
                // The same centred measure as a poem, a little wider for the cards.
                val side = maxOf(24.dp, (maxWidth - 760.dp) / 2)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(200.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = side,
                        end = side,
                        top = 8.dp,
                        bottom = insets.calculateBottomPadding() + 32.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    cat.description?.takeIf { it.isNotBlank() }?.let { description ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                    }
                    items(entries) { entry ->
                        Card(
                            onClick = {
                                when (entry) {
                                    is CatEntry.Chapter -> onCategory(entry.category.fullUrl)
                                    is CatEntry.Poem -> onPoem(entry.poem.fullUrl)
                                }
                            },
                        ) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(
                                    text = when (entry) {
                                        is CatEntry.Chapter -> entry.category.title
                                        is CatEntry.Poem -> entry.poem.title
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                (entry as? CatEntry.Poem)?.let { excerpts[it.poem.id] }
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                            }
                        }
                    }
                }
            }
        }
    }
}
