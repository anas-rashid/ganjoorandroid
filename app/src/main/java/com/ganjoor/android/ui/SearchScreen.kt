package com.ganjoor.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ganjoor.android.R
import com.ganjoor.android.data.Ganjoor
import com.ganjoor.android.data.PoetRef
import com.ganjoor.android.data.SearchHit
import com.ganjoor.android.data.snippet
import kotlinx.coroutines.delay

private const val PAGE_SIZE = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    initialTerm: String,
    onUp: () -> Unit,
    onHome: () -> Unit,
    onPoem: (String) -> Unit,
) {
    var term by remember { mutableStateOf(initialTerm) }
    // Empty means every poet; otherwise the search is scoped to the ones picked.
    val poets = remember { mutableStateListOf<PoetRef>() }
    var page by remember { mutableIntStateOf(1) }
    val hits = remember { mutableStateListOf<SearchHit>() }
    var loading by remember { mutableStateOf(false) }
    var exhausted by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    val offline = LocalSettings.current.value.offline

    // Re-run from the first page whenever the term or the scope changes.
    LaunchedEffect(term, poets.toList()) {
        page = 1; exhausted = false; hits.clear()
        if (term.isBlank()) return@LaunchedEffect
        delay(400)  // let typing settle before hitting the network
        loading = true
        hits += Ganjoor.search(term, poets.map { it.id }, page = 1, pageSize = PAGE_SIZE)
        exhausted = hits.size < PAGE_SIZE
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_poems)) },
                navigationIcon = {
                    IconButton(onClick = onUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = { HomeAction(onHome) },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                OutlinedTextField(
                    value = term,
                    onValueChange = { term = it },
                    label = { Text(stringResource(R.string.search)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = poets.isEmpty(),
                    onClick = { poets.clear() },
                    label = { Text(stringResource(R.string.all_poets)) },
                )
                poets.forEach { poet ->
                    FilterChip(
                        selected = true,
                        onClick = { poets.remove(poet) },
                        label = { Text(poet.nickname) },
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = { picking = true },
                    label = { Text(stringResource(R.string.choose_poets)) },
                )
            }

            when {
                offline -> Message(R.string.search_needs_connection)
                term.isBlank() -> Unit
                loading && hits.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    Alignment.Center,
                ) { CircularProgressIndicator() }

                hits.isEmpty() -> Message(R.string.no_results)

                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(hits, key = { "${it.id}-${it.fullUrl}" }) { hit ->
                        ListItem(
                            headlineContent = {
                                Text(hit.fullTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            },
                            supportingContent = {
                                Text(
                                    text = snippet(hit, term),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            modifier = Modifier.clickable { onPoem(hit.fullUrl) },
                        )
                        HorizontalDivider()
                    }
                    if (!exhausted) {
                        item {
                            TextButton(
                                onClick = { page++ },
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                            ) {
                                if (loading) CircularProgressIndicator(Modifier.heightIn(max = 20.dp))
                                else Text(stringResource(R.string.load_more))
                            }
                        }
                    }
                }
            }
        }
    }

    // Paging: only fires for pages after the first, which the term/scope effect handles.
    LaunchedEffect(page) {
        if (page == 1 || term.isBlank()) return@LaunchedEffect
        loading = true
        val more = Ganjoor.search(term, poets.map { it.id }, page = page, pageSize = PAGE_SIZE)
        hits += more
        exhausted = more.size < PAGE_SIZE
        loading = false
    }

    if (picking) {
        PoetPicker(
            selected = poets,
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun Message(resId: Int) {
    Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
        Text(stringResource(resId), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Tick any number of poets to narrow the search; ticking none means all of them. */
@Composable
private fun PoetPicker(selected: MutableList<PoetRef>, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_poets)) },
        text = {
            Load(key = Unit, block = { Ganjoor.manifest() }) { manifest ->
                val shown = remember(query, manifest) {
                    if (query.isBlank()) manifest.poets
                    else manifest.poets.filter { it.nickname.contains(query.trim(), true) }
                }
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(stringResource(R.string.search_poets)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    LazyColumn(Modifier.heightIn(max = 380.dp)) {
                        items(shown, key = { it.id }) { poet ->
                            val checked = selected.any { it.id == poet.id }
                            ListItem(
                                headlineContent = { Text(poet.nickname) },
                                leadingContent = {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = {
                                            if (checked) selected.removeAll { it.id == poet.id }
                                            else selected.add(poet)
                                        },
                                    )
                                },
                                modifier = Modifier.clickable {
                                    if (checked) selected.removeAll { it.id == poet.id }
                                    else selected.add(poet)
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
        },
    )
}
