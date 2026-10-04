package com.ganjoor.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ganjoor.android.R
import com.ganjoor.android.data.Definition
import com.ganjoor.android.data.Dictionary
import com.ganjoor.android.ui.theme.readingStyle

/** English prose inside an otherwise right-to-left sheet. */
@Composable
private fun LeftToRight(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr, content = content)
}

/** What the dictionary knows about a tapped word. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordSheet(word: String, onDismiss: () -> Unit) {
    val prefs = LocalSettings.current.value
    val definitions by produceState<List<Definition>?>(null, word) { value = Dictionary.lookup(word) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // The headword in the reading font, at reading size: it is a line of poetry, after all.
            Text(
                text = word,
                style = readingStyle(prefs.font, prefs.fontSize, prefs.fontWeight.weight),
                modifier = Modifier.fillMaxWidth(),
            )
            HorizontalDivider()

            when {
                definitions == null -> CircularProgressIndicator(Modifier.padding(vertical = 16.dp))

                definitions!!.isEmpty() -> LeftToRight {
                    Text(
                        text = stringResource(R.string.no_definition),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                else -> definitions!!.forEach { definition ->
                    Column(Modifier.fillMaxWidth()) {
                        // The headword actually matched, which may be the lemma rather than the
                        // word as it appears in the line. Persian, so it stays right-to-left.
                        if (definition.word != word) {
                            Text(
                                text = definition.word,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        // The definitions are English; right-aligning them reads badly.
                        LeftToRight {
                            Column(Modifier.fillMaxWidth()) {
                                Text(definition.gloss, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = stringResource(
                                        if (definition.source == "wiktionary") {
                                            R.string.source_wiktionary
                                        } else {
                                            R.string.source_daneshjoo
                                        }
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
