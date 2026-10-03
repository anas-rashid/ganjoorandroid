package com.ganjoor.android.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ganjoor.android.R
import com.ganjoor.android.data.Ganjoor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    fullUrl: String,
    onBack: () -> Unit,
    onCategory: (String) -> Unit,
    onPoem: (String) -> Unit,
) {
    Load(key = fullUrl, block = { Ganjoor.category(fullUrl) }) { cat ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(cat.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
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
                    NavRow(poem.title, isCategory = false) { onPoem(poem.fullUrl) }
                }
            }
        }
    }
}

@Composable
private fun NavRow(title: String, isCategory: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
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
